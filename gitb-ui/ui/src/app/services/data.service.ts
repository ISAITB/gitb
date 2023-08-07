import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { Observable } from 'rxjs';
import { Constants } from '../common/constants'
import { ObjectWithId } from '../components/test-filter/object-with-id';
import { ConformanceTestCase } from '../pages/organisation/conformance-statement/conformance-test-case';
import { ActualUserInfo } from '../types/actual-user-info';
import { AppConfigurationProperties } from '../types/app-configuration-properties';
import { Community } from '../types/community';
import { ConfigurationPropertyVisibility } from '../types/configuration-property-visibility';
import { CustomPropertySubmissionInfo } from '../types/custom-property-submission-info.type';
import { CustomProperty } from '../types/custom-property.type';
import { FileParam } from '../types/file-param.type';
import { IdLabel } from '../types/id-label';
import { NumberSet } from '../types/number-set';
import { Organisation } from '../types/organisation.type';
import { Parameter } from '../types/parameter';
import { SystemConfigurationEndpoint } from '../types/system-configuration-endpoint';
import { SystemConfigurationParameter } from '../types/system-configuration-parameter';
import { TypedLabelConfig } from '../types/typed-label-config.type'
import { UserAccount } from '../types/user-account';
import { User } from '../types/user.type';
import { saveAs } from 'file-saver'
import { LogLevel } from '../types/log-level';
import { SpecificationGroup } from '../types/specification-group';
import { Specification } from '../types/specification';
import { DomainSpecification } from '../types/domain-specification';
import { find, sortBy } from 'lodash';

@Injectable({
  providedIn: 'root'
})
export class DataService {

  public configuration!: AppConfigurationProperties
  public configurationLoaded = false
  public acceptedEmailAttachmentTypes?: {[key: string]: boolean}
  public actualUser?: ActualUserInfo
  public user?: User
  public vendor?: Organisation
  public community?: Community
  public labels?: {[key: number]: TypedLabelConfig}
  public isSystemAdmin = false
  public isVendorUser = false
  public isVendorAdmin = false
  public isCommunityAdmin = false
  public isDomainUser = false
  public tests?: ConformanceTestCase[]
  public currentLandingPageContent?: string
  private apiRoot?: string

  private renderer: Renderer2
  triggerEventToDataTypeMap?: {[key: number]: { [key: number]: boolean } }

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null)
    this.configuration = this.emptyAppConfiguration()
    this.destroy()
  }

  destroy() {
    this.actualUser = undefined
    this.user = undefined
    this.vendor = undefined
    this.community = undefined
    this.configuration = this.emptyAppConfiguration()
    this.isSystemAdmin = false
    this.isVendorUser = false
    this.isVendorAdmin = false
    this.isCommunityAdmin = false
    this.isDomainUser = false
    this.acceptedEmailAttachmentTypes = undefined
    this.tests = undefined
    this.currentLandingPageContent = undefined
  }

  private emptyAppConfiguration(): AppConfigurationProperties {
    this.configurationLoaded = false
    return {
      emailEnabled: (this.configuration?.emailEnabled != undefined)?this.configuration!.emailEnabled:false,
      emailAttachmentsMaxCount: (this.configuration?.emailAttachmentsMaxCount != undefined)?this.configuration!.emailAttachmentsMaxCount:5,
      emailAttachmentsMaxSize: (this.configuration?.emailAttachmentsMaxSize != undefined)?this.configuration!.emailAttachmentsMaxSize:5,
      emailAttachmentsAllowedTypes: (this.configuration?.emailAttachmentsAllowedTypes != undefined)?this.configuration!.emailAttachmentsAllowedTypes:'',
      surveyEnabled: (this.configuration?.surveyEnabled != undefined)?this.configuration!.surveyEnabled:false,
      surveyAddress: (this.configuration?.surveyAddress != undefined)?this.configuration!.surveyAddress:'',
      moreInfoEnabled: (this.configuration?.moreInfoEnabled != undefined)?this.configuration!.moreInfoEnabled:false,
      moreInfoAddress: (this.configuration?.moreInfoAddress != undefined)?this.configuration!.moreInfoAddress:'',
      releaseInfoEnabled: (this.configuration?.releaseInfoEnabled != undefined)?this.configuration!.releaseInfoEnabled:false,
      releaseInfoAddress: (this.configuration?.releaseInfoAddress != undefined)?this.configuration!.releaseInfoAddress:'',
      userGuideOU: (this.configuration?.userGuideOU != undefined)?this.configuration!.userGuideOU:'',
      userGuideOA: (this.configuration?.userGuideOA != undefined)?this.configuration!.userGuideOA:'',
      userGuideCA: (this.configuration?.userGuideCA != undefined)?this.configuration!.userGuideCA:'',
      userGuideTA: (this.configuration?.userGuideTA != undefined)?this.configuration!.userGuideTA:'',
      ssoEnabled: (this.configuration?.ssoEnabled != undefined)?this.configuration!.ssoEnabled:false,
      ssoInMigration: (this.configuration?.ssoInMigration != undefined)?this.configuration!.ssoInMigration:false,
      demosEnabled: (this.configuration?.demosEnabled != undefined)?this.configuration!.demosEnabled:false,
      demosAccount: (this.configuration?.demosAccount != undefined)?this.configuration!.demosAccount:-1,
      registrationEnabled: (this.configuration?.registrationEnabled != undefined)?this.configuration!.registrationEnabled:false,
      savedFileMaxSize: (this.configuration?.savedFileMaxSize != undefined)?this.configuration!.savedFileMaxSize:5,
      mode: (this.configuration?.mode != undefined)?this.configuration!.mode:'development',
      automationApiEnabled: (this.configuration?.automationApiEnabled != undefined)?this.configuration!.automationApiEnabled:false,
      versionNumber: (this.configuration?.versionNumber != undefined)?this.configuration!.versionNumber:''
    }
  }

  setActualUser(actualUser: ActualUserInfo) {
    this.actualUser = actualUser
    if (!this.user) {
      this.user = {}
    }
    this.user.name = actualUser.firstName + ' ' + actualUser.lastName
    this.user.email = actualUser.email
  }

  setUser(user: User) {
    this.user = user
    if (this.actualUser) {
      this.setActualUser(this.actualUser)
    }
    this.isVendorAdmin = (user.role == Constants.USER_ROLE.VENDOR_ADMIN)
    this.isVendorUser = (user.role == Constants.USER_ROLE.VENDOR_USER)
    this.isDomainUser = (user.role == Constants.USER_ROLE.DOMAIN_USER)
    this.isSystemAdmin = (user.role == Constants.USER_ROLE.SYSTEM_ADMIN)
    this.isCommunityAdmin = (user.role == Constants.USER_ROLE.COMMUNITY_ADMIN)
  }

  isDevelopmentMode(): boolean {
    return this.configuration != undefined && this.configuration.mode == 'development'
  }

  isDemoAccount(): boolean {
    return this.user != undefined && this.configuration.demosEnabled && this.configuration.demosAccount == this.user.id
  }

  setConfiguration(config: AppConfigurationProperties) {
    this.configuration = config
    this.acceptedEmailAttachmentTypes = {}
    let acceptedTypes = config.emailAttachmentsAllowedTypes.split(',')
    for (let acceptedType of acceptedTypes) {
      this.acceptedEmailAttachmentTypes[acceptedType] = true
    }
    this.configurationLoaded = true
  }

  getRoleDescription(full: boolean, account?: UserAccount): string {
    let role: number | undefined
    let organisation = ''
    let community = ''
    if (account == undefined) {
      if (this.user && this.vendor && this.community) {
        role = this.user.role
        if (full) {
          organisation = this.vendor.fname
          community = this.community.fname
        } else {
          organisation = this.vendor.sname
          community = this.community.sname
        }
      }
    } else {
      role = account.role
      if (full) {
        organisation = account.organisationFullName
        community = account.communityFullName
      } else {
        organisation = account.organisationShortName
        community = account.communityShortName
      }
    }
    let description = ''
    if (role == Constants.USER_ROLE.SYSTEM_ADMIN) {
      description = 'Test bed administrator'
    } else if (role == Constants.USER_ROLE.COMMUNITY_ADMIN) {
      description = 'Community administrator (' + community + ')'
    } else if (role == Constants.USER_ROLE.VENDOR_ADMIN) {
      description = 'Administrator of '+organisation+' ('+community+')'
    } else {
      description = 'User of '+organisation+' ('+community+')'
    }
    return description
  }

  setVendor(vendor: Organisation) {
    this.vendor = vendor
  }

  setCommunity(community: Community) {
    this.community = community
    if (community?.labels) {
      this.setupLabels(community.labels)
      delete community.labels
    }
  }

  createLabels(customLabels?: TypedLabelConfig[]): {[key: number]: TypedLabelConfig} {
    let labels:{[key: number]: TypedLabelConfig} = {}
    if (customLabels) {
      for (let label of customLabels)
        labels[label.labelType] = {
          labelType: label.labelType,
          singularForm: label.singularForm,
          pluralForm: label.pluralForm,
          fixedCase: label.fixedCase,
          custom: true
        }
    }
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.DOMAIN)
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.SPECIFICATION)
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.ACTOR)
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.ENDPOINT)
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.ORGANISATION)
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.SYSTEM)
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP)
    this.setDefaultLabel(labels, Constants.LABEL_TYPE.SPECIFICATION_GROUP)
    return labels
  }

  setupLabels(customLabels?: TypedLabelConfig[]) {
    this.labels = this.createLabels(customLabels)
  }

  setDefaultLabel(labels: {[key:number]: TypedLabelConfig}, labelType: number) {
    if (labels[labelType] == undefined)
      labels[labelType] = {
        labelType: labelType,
        singularForm: Constants.LABEL_DEFAULT[labelType].singularForm,
        pluralForm: Constants.LABEL_DEFAULT[labelType].pluralForm,
        fixedCase: Constants.LABEL_DEFAULT[labelType].fixedCase,
        custom: false
      }
  }

  labelDomain() {
    return this.labels![Constants.LABEL_TYPE.DOMAIN].singularForm
  }

  labelDomainLower() {
    if (this.labels![Constants.LABEL_TYPE.DOMAIN].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.DOMAIN].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.DOMAIN].singularForm.toLowerCase()
    }
  }

  labelDomains() {
    return this.labels![Constants.LABEL_TYPE.DOMAIN].pluralForm
  }

  labelDomainsLower() {
    if (this.labels![Constants.LABEL_TYPE.DOMAIN].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.DOMAIN].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.DOMAIN].pluralForm.toLowerCase()
    }
  }

  labelSpecification() {
    return this.labels![Constants.LABEL_TYPE.SPECIFICATION].singularForm
  }

  labelSpecificationLower() {
    if (this.labels![Constants.LABEL_TYPE.SPECIFICATION].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION].singularForm.toLowerCase()
    }
  }

  labelSpecifications() {
    return this.labels![Constants.LABEL_TYPE.SPECIFICATION].pluralForm
  }

  labelSpecificationsLower() {
    if (this.labels![Constants.LABEL_TYPE.SPECIFICATION].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION].pluralForm.toLowerCase()
    }
  }

  labelSpecificationGroup() {
    return this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].singularForm
  }

  labelSpecificationGroupLower() {
    if (this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].singularForm.toLowerCase()
    }
  }

  labelSpecificationGroups() {
    return this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].pluralForm
  }

  labelSpecificationGroupsLower() {
    if (this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_GROUP].pluralForm.toLowerCase()
    }
  }

  labelSpecificationInGroup() {
    return this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].singularForm
  }

  labelSpecificationInGroupLower() {
    if (this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].singularForm.toLowerCase()
    }
  }

  labelSpecificationInGroups() {
    return this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].pluralForm
  }

  labelSpecificationInGroupsLower() {
    if (this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP].pluralForm.toLowerCase()
    }
  }

  labelActor() {
    return this.labels![Constants.LABEL_TYPE.ACTOR].singularForm
  }

  labelActorLower() {
    if (this.labels![Constants.LABEL_TYPE.ACTOR].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.ACTOR].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.ACTOR].singularForm.toLowerCase()
    }
  }

  labelActors() {
    return this.labels![Constants.LABEL_TYPE.ACTOR].pluralForm
  }

  labelActorsLower() {
    if (this.labels![Constants.LABEL_TYPE.ACTOR].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.ACTOR].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.ACTOR].pluralForm.toLowerCase()
    }
  }

  labelEndpoint() {
    return this.labels![Constants.LABEL_TYPE.ENDPOINT].singularForm
  }

  labelEndpointLower() {
    if (this.labels![Constants.LABEL_TYPE.ENDPOINT].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.ENDPOINT].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.ENDPOINT].singularForm.toLowerCase()
    }
  }

  labelEndpoints() {
    return this.labels![Constants.LABEL_TYPE.ENDPOINT].pluralForm
  }

  labelEndpointsLower() {
    if (this.labels![Constants.LABEL_TYPE.ENDPOINT].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.ENDPOINT].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.ENDPOINT].pluralForm.toLowerCase()
    }
  }

  labelOrganisation() {
    return this.labels![Constants.LABEL_TYPE.ORGANISATION].singularForm
  }

  labelOrganisationLower() {
    if (this.labels![Constants.LABEL_TYPE.ORGANISATION].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.ORGANISATION].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.ORGANISATION].singularForm.toLowerCase()
    }
  }

  labelOrganisations() {
    return this.labels![Constants.LABEL_TYPE.ORGANISATION].pluralForm
  }

  labelOrganisationsLower() {
    if (this.labels![Constants.LABEL_TYPE.ORGANISATION].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.ORGANISATION].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.ORGANISATION].pluralForm.toLowerCase()
    }
  }

  labelSystem() {
    return this.labels![Constants.LABEL_TYPE.SYSTEM].singularForm
  }

  labelSystemLower() {
    if (this.labels![Constants.LABEL_TYPE.SYSTEM].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SYSTEM].singularForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SYSTEM].singularForm.toLowerCase()
    }
  }

  labelSystems() {
    return this.labels![Constants.LABEL_TYPE.SYSTEM].pluralForm
  }

  labelSystemsLower() {
    if (this.labels![Constants.LABEL_TYPE.SYSTEM].fixedCase) {
      return this.labels![Constants.LABEL_TYPE.SYSTEM].pluralForm
    } else {
      return this.labels![Constants.LABEL_TYPE.SYSTEM].pluralForm.toLowerCase()
    }
  }

  focus(inputId: string|undefined, delay?: number) {
    if (inputId) {
      if (!inputId.startsWith('#')) {
        inputId = '#' + inputId
      }
      let timeToWait = 1
      if (delay) {
        timeToWait = Number(delay)
      }
      setTimeout(() => {
        const element = this.renderer.selectRootElement(inputId)
        element.focus()
      }, timeToWait)
    }
  }

  async(fn: () => any) {
    setTimeout(() => {
      fn()
    }, 1)
  }

  userStatus(ssoStatus?: number): string {
    if (this.configuration.ssoEnabled) {
      if (ssoStatus == 1) {
        return 'Not migrated'
      } else if (ssoStatus == 2) {
        return 'Inactive'
      } else {
        return 'Active'
      }
    } else {
      return 'Active'
    }
  }

  customPropertiesValid(properties: CustomProperty[]|undefined, forceRequiredValues?: boolean) {
    let valid = true
    if (forceRequiredValues) {
      if (properties != undefined) {
        for (let property of properties) {
          if (property.use == 'R' ) {
            if (property.prerequisiteOk && !(property.value && property.value.trim().length > 0)) {
              valid = false
            }
          }
        }
      }
    }
    return valid
  }

  customPropertiesForPost(properties: CustomProperty[]|undefined): CustomPropertySubmissionInfo {
    let propValues = []
    let files: FileParam[] = []
    if (properties) {
      for (let property of properties) {
        let propValue:any = {}
        propValue.parameter = Number(property.id)
        if (property.kind == 'SECRET') {
          if (property.changeValue && property.value && property.value.trim().length > 0) {
            propValue.value = property.value.trim()
          } else if (!property.changeValue && property.configured) {
            propValue.value = ''
          }
        } else if (property.kind == 'BINARY') {
          if (property.file?.file != undefined) {
            propValue.value = ''
            files.push({
              param: 'file_'+propValue.parameter,
              data: property.file.file
            })
          } else if (property.configured) {
            propValue.value = ''
          }
        } else {
          if (property.value && property.value.trim().length > 0) {
            propValue.value = property.value.trim()
          }
        }
        if (propValue.value != undefined) {
          propValues.push(propValue)
        }
      }
    }
    return {
      parameterJson: JSON.stringify(propValues),
      files: files
    }
  }

  base64FromDataURL(dataURL: string) {
    // DEPRECATED - To be removed
    return dataURL.substring(dataURL.indexOf(',')+1)
  }

  mimeTypeFromDataURL(dataURL: string) {
    // DEPRECATED - This should be handled server-side
    return dataURL.substring(dataURL.indexOf(':')+1, dataURL.indexOf(';'))
  }

  extensionFromMimeType(mimeType: string|undefined) {
    let result = ""
    if (mimeType == "text/xml" || mimeType == "application/xml") {
      result = ".xml"
    } else if (mimeType == "application/zip" || mimeType == "application/x-zip-compressed") {
      result = ".zip"
    } else if (mimeType == "application/pkix-cert") {
      result = ".cer"
    } else if (mimeType == "application/pdf") {
      result = ".pdf"
    } else if (mimeType == "application/json") {
      result = ".json"
    } else if (mimeType == "text/plain") {
      result = ".txt"
    } else if (mimeType == "image/png") {
      result = ".png"
    } else if (mimeType == "image/gif") {
      result = ".gif"
    } else if (mimeType == "image/gif") {
      result = ".gif"
    } else if (mimeType == "image/jpeg") {
      result = ".jpeg"
    }
    return result
  }

  b64toBlob(b64Data: string, contentType = '', sliceSize = 512): Blob {
    const byteCharacters = atob(b64Data)
    let byteArrays: Uint8Array[] = []
    let offset = 0
    while (offset < byteCharacters.length) {
      let slice = byteCharacters.slice(offset, offset + sliceSize)
      let byteNumbers = new Array(slice.length)
      for (let i = 0; i < slice.length; i++) {
        byteNumbers[i] = slice.charCodeAt(i)
      }
      let byteArray = new Uint8Array(byteNumbers)
      byteArrays.push(byteArray);
      offset += sliceSize
    }
    if (contentType !== undefined) {
      return new Blob(byteArrays, {type: contentType})
    } else {
      return new Blob(byteArrays)
    }
  }

  iconForTestResult(result?: string): string {
    let icon: string
    if (result == Constants.TEST_CASE_RESULT.SUCCESS) {
      icon = "fa testsuite-progress-icon fa-check-circle test-case-success"
    } else if (result == Constants.TEST_CASE_RESULT.FAILURE) {
      icon = "fa testsuite-progress-icon fa-times-circle test-case-error"
    } else {
      icon = "fa testsuite-progress-icon fa-ban test-case-undefined"
    }
    return icon
  }

  tooltipForTestResult(result?: string): string {
    let text: string
    if (result == Constants.TEST_CASE_RESULT.SUCCESS) {
      text = "Success"
    } else if (result == Constants.TEST_CASE_RESULT.FAILURE) {
      text = "Failure"
    } else {
      text = "Incomplete"
    }
    return text
  }

  asCsvString(text: any) {
    let textStr = ''
    if (text !== undefined) {
      textStr = String(text)
      if (textStr.length > 0) {
        // Replace values that can break the CSV format
        textStr = textStr.replace(/(,|\s+)/g, ' ')
        // Prevent CSV formula injection attacks
        const charsToEscape = ['=','@','+','-']
        if (charsToEscape.indexOf(textStr.charAt(0)) != -1) {
          textStr = '\'' + textStr
        }
      }
    }
    return textStr
  }

  exportAllAsCsv(header: string[], data: any[]) {
    if (data.length > 0) {
      let csv = header.toString() + '\n'
      data.forEach((item, i) => {
        let line = ''
        let idx = 0
        for (let key in item) {
          if (idx++ != 0) {
            line += ','
          }
          line += this.asCsvString(item[key])
        }
        csv += line
        if (i < data.length) {
          csv += '\n'
        }
      })
      const blobData = new Blob([csv], {type: 'text/csv'});
      saveAs(blobData, 'export.csv');
    }
  }

  exportPropertiesAsCsv(header: string[], columnMap: string[], data: any[]) {
    if (data.length > 0) {
      let csv = header.toString() + '\n'
      data.forEach((rowData, rowIndex) => {
        let line = ''
        columnMap.forEach((columnName, columnIndex) => {
          if (columnIndex != 0) {
            line += ','
          }
          line += this.asCsvString(rowData[columnName])
        })
        csv += line
        if (rowIndex < data.length) {
          csv += '\n'
        }
      })
      const blobData = new Blob([csv], {type: 'text/csv'})
      saveAs(blobData, 'export.csv')
    }
  }

  conformanceStatusForTests(completedCount: number, failedCount: number, undefinedCount: number) {
    if (failedCount > 0) {
      return Constants.TEST_CASE_RESULT.FAILURE
    } else if (undefinedCount > 0) {
      return Constants.TEST_CASE_RESULT.UNDEFINED
    } else if (completedCount > 0) {
      return Constants.TEST_CASE_RESULT.SUCCESS
    } else {
      return Constants.TEST_CASE_RESULT.UNDEFINED
    }
  }

  idToLabelMap(items: IdLabel[]) {
    const map: {[key: number]: string} = {}
    for (let item of items) {
      map[item.id] = item.label
    }
    return map
  }

  errorArrayToString(errorArray: string[]|undefined): string {
    let content = ''
    if (errorArray != undefined) {
      let counter = -1
      let padding = 4
      for (let text of errorArray) {
        if (counter == -1) {
          content += text
        } else {
          content += ('\n'+(' '.repeat(counter*padding))+'|\n')
          content += (' '.repeat(counter*padding)+'+-- ' + text)
        }
        counter += 1
      }
    }
    return content;
  }

  triggerEventTypeLabel(eventType: number): string {
    switch (eventType) {
      case Constants.TRIGGER_EVENT_TYPE.ORGANISATION_CREATED: return this.labelOrganisation() + ' created'
      case Constants.TRIGGER_EVENT_TYPE.ORGANISATION_UPDATED: return this.labelOrganisation() + ' updated'
      case Constants.TRIGGER_EVENT_TYPE.SYSTEM_CREATED: return this.labelSystem() + ' created'
      case Constants.TRIGGER_EVENT_TYPE.SYSTEM_UPDATED: return this.labelSystem() + ' updated'
      case Constants.TRIGGER_EVENT_TYPE.CONFORMANCE_STATEMENT_CREATED: return 'Conformance statement created'
      case Constants.TRIGGER_EVENT_TYPE.CONFORMANCE_STATEMENT_UPDATED: return 'Conformance statement updated'
      case Constants.TRIGGER_EVENT_TYPE.CONFORMANCE_STATEMENT_SUCCEEDED: return 'Conformance statement succeeded'
      case Constants.TRIGGER_EVENT_TYPE.TEST_SESSION_STARTED: return 'Test session started'
      case Constants.TRIGGER_EVENT_TYPE.TEST_SESSION_SUCCEEDED: return 'Test session succeeded'
      case Constants.TRIGGER_EVENT_TYPE.TEST_SESSION_FAILED: return 'Test session failed'
      default: throw new Error('Unknown trigger event type ['+eventType+']')
    }
  }

  triggerDataTypes(): IdLabel[] {
    return [
      {id: Constants.TRIGGER_DATA_TYPE.COMMUNITY, label: 'Community'},
      {id: Constants.TRIGGER_DATA_TYPE.ORGANISATION, label: this.labelOrganisation()},
      {id: Constants.TRIGGER_DATA_TYPE.SYSTEM, label: this.labelSystem()},
      {id: Constants.TRIGGER_DATA_TYPE.SPECIFICATION, label: this.labelSpecification()},
      {id: Constants.TRIGGER_DATA_TYPE.ACTOR, label: this.labelActor()},
      {id: Constants.TRIGGER_DATA_TYPE.TEST_SESSION, label: 'Test session'},
      {id: Constants.TRIGGER_DATA_TYPE.TEST_REPORT, label: 'Test report'},
      {id: Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER, label: this.labelOrganisation() + ' properties'},
      {id: Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER, label: this.labelSystem() + ' properties'},
      {id: Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER, label: this.labelDomain() + ' properties'},
      {id: Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER, label: 'Conformance statement properties'}
    ]
  }

  private addIdMapEntry(map: {[key: number]: { [key: number]: boolean } }, id: number, ids: number[]) {
    map[id] = {}
    for (let otherId of ids) {
      map[id][otherId] = true
    }
  }

  triggerDataTypeAllowedForEvent(eventType: number, dataType: number) {
    if (this.triggerEventToDataTypeMap == undefined) {
      let tempMap: {[key: number]: { [key: number]: boolean }} = {}
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.ORGANISATION_CREATED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.ORGANISATION_UPDATED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.SYSTEM_CREATED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.SYSTEM_UPDATED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.CONFORMANCE_STATEMENT_CREATED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SPECIFICATION,
        Constants.TRIGGER_DATA_TYPE.ACTOR,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.CONFORMANCE_STATEMENT_UPDATED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SPECIFICATION,
        Constants.TRIGGER_DATA_TYPE.ACTOR,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.TEST_SESSION_SUCCEEDED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SPECIFICATION,
        Constants.TRIGGER_DATA_TYPE.ACTOR,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.TEST_SESSION,
        Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.TEST_REPORT
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.TEST_SESSION_FAILED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SPECIFICATION,
        Constants.TRIGGER_DATA_TYPE.ACTOR,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.TEST_SESSION,
        Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.TEST_REPORT
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.TEST_SESSION_STARTED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SPECIFICATION,
        Constants.TRIGGER_DATA_TYPE.ACTOR,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.TEST_SESSION,
        Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER
      ])
      this.addIdMapEntry(tempMap, Constants.TRIGGER_EVENT_TYPE.CONFORMANCE_STATEMENT_SUCCEEDED, [
        Constants.TRIGGER_DATA_TYPE.COMMUNITY,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION,
        Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SYSTEM,
        Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.SPECIFICATION,
        Constants.TRIGGER_DATA_TYPE.ACTOR,
        Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER,
        Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER
      ])
      this.triggerEventToDataTypeMap = tempMap
    }
    return this.triggerEventToDataTypeMap[eventType][dataType] === true
  }

	checkPropertyVisibility(properties: Parameter[]) {
		const results: ConfigurationPropertyVisibility = {
			hasProperties: false,
			hasMissingProperties: false,
			hasVisibleMissingRequiredProperties: false,
			hasVisibleMissingOptionalProperties: false,
			hasNonVisibleMissingRequiredProperties: false,
			hasNonVisibleMissingOptionalProperties: false
		}
		if (properties != undefined && properties.length > 0) {
			results.hasProperties = true
			for (let prop of properties) {
				if (!prop.configured) {
					results.hasMissingProperties = true
					if (prop.hidden) {
						if (this.isSystemAdmin || this.isCommunityAdmin) {
							if (prop.use == 'O') {
								results.hasVisibleMissingOptionalProperties = true
              } else {
								results.hasVisibleMissingRequiredProperties = true
              }
            } else {
							if (prop.use == 'O') {
								results.hasNonVisibleMissingOptionalProperties = true
              } else {
								results.hasNonVisibleMissingRequiredProperties = true
              }
            }
          } else {
						if (prop.use == 'O') {
							results.hasVisibleMissingOptionalProperties = true
            } else {
							results.hasVisibleMissingRequiredProperties = true
            }
          }
        }
      }
    }
		return results
  }

	isMemberConfigurationValid(properties: CustomProperty[]) {
		let valid = true
		if (properties != undefined) {
			for (let property of properties) {
				if (property.use == 'R' && !property.configured) {
					return false
        }
      }
    }
		return valid
  }

	isConfigurationValid(endpointRepresentations: SystemConfigurationEndpoint[]) {
		if (endpointRepresentations != undefined) {
			for (let endpoint of endpointRepresentations) {
        let endpointValid = this.isConfigurationOfEndpointValid(endpoint)
        if (!endpointValid) {
          return false
        }
      }
    }
		return true
  }

  getEndpointParametersToDisplay(endpoints: SystemConfigurationEndpoint[]): SystemConfigurationParameter[] {
    if (endpoints.length > 0) {
			for (let endpoint of endpoints) {
        let endpointValid = this.isConfigurationOfEndpointValid(endpoint)
        if (!endpointValid) {
          return endpoint.parameters
        }
      }
      return endpoints[0].parameters
    }
    return []
  }

	isConfigurationOfEndpointValid(endpoint: SystemConfigurationEndpoint) {
    for (let parameter of endpoint.parameters) {
      if (!parameter.configured && parameter.use == "R") {
        return false
      }
    }
		return true
  }

	setTestsToExecute(tests: ConformanceTestCase[]) {
		this.tests = tests
  }

  isDataURL(configuration: string) {
   return Constants.DATA_URL_REGEX.test(configuration)
  }

	getFileInfo(blob: Blob, filename?: string): Observable<{type: string, extension: string, filename: string}> {
    return new Observable((subscriber) => {
      const fileReader = new FileReader()
      fileReader.onloadend = (e) => {
        const result = e.target!.result as ArrayBufferLike
        const byteArray = new Uint8Array(result)
        let arr: Uint8Array
        if (byteArray.length >= 8) {
          arr = (new Uint8Array(result)).subarray(0, 8)
        } else { // if (byteArray.length >= 4) {
          arr = (new Uint8Array(result)).subarray(0, 4)
        }
        let header = ""
        for (let i=0; i <= arr.length-1; i++) {
          header += arr[i].toString(16)
        }
        let type: string|undefined
        let extension: string|undefined = undefined
        if (header.startsWith('89504e47')) {
          type = "image/png"
          extension = "png"
        } else if (header.startsWith('47494638')) {
          type = "image/gif"
          extension = "gif"
        } else if (header.startsWith('ffd8ffe0') || header.startsWith('ffd8ffe1') || header.startsWith('ffd8ffe2') || header.startsWith('ffd8ffe3') || header.startsWith('ffd8ffe8')) {
          type = "image/jpeg"
          extension = "jpeg"
        } else if (header.startsWith('49492a00') || header.startsWith('4d4d002d')) {
          type = "image/tiff"
          extension = "tiff"
        } else if (header.startsWith('25504446')) {
          type = "application/pdf"
          extension = "pdf"
        } else if (header.startsWith('504b0304') || header.startsWith('504b0506') || header.startsWith('504b0708')) {
          type = "application/zip"
          extension = "zip"
        } else if (header.startsWith('efbbbf') || header.startsWith('fffe')) {
          type = "text/plain"
          extension = "txt"
        } else if (header.startsWith('3c3f786d6c20')) {
          type = "text/xml"
          extension = "xml"
        } else {
          type = "application/octet-stream"
          extension = "bin"
        }
        if (filename == undefined) {
          filename = "file"
        }
        filename += '.'+extension
        subscriber.next({
          type: type,
          extension: extension,
          filename: filename
        })
        subscriber.complete()
      }
      fileReader.readAsArrayBuffer(blob)
    })
  }

  copyToClipboard(text: string|undefined): Observable<string|undefined> {
    return new Observable<string|undefined>((observer) => {
      if (text != undefined) {
        if (navigator.clipboard) {
          // Normal scenario.
          navigator.clipboard.writeText(text).then(() => {
            observer.next(text)
            observer.complete()
          })
        } else {
          // IE11 support.
          const clipboard = (window as any).clipboardData
          if (clipboard != undefined) {
            clipboard.setData("text", text)
            observer.next(text)
            observer.complete()
          } else {
            // Final fallback solution.
            let listener = (e: ClipboardEvent) => {
              let clipboard = e.clipboardData
              if (clipboard) {
                clipboard.setData("text", text)
              }
              e.preventDefault()
              observer.next(text)
              observer.complete()
            }
            document.addEventListener("copy", listener, false)
            document.execCommand("copy");
            document.removeEventListener("copy", listener, false);            
          }
        }
      } else {
        observer.next()
        observer.complete()
      }
    })
  }

  asSet(items: number[]|undefined): NumberSet {
    const numberSet: NumberSet = {}
    if (items) {
      for (let item of items) {
        numberSet[item] = true
      }
    }
    return numberSet
  }

  asIdSet(items: ObjectWithId[]|undefined): NumberSet {
    const idSet: NumberSet = {}
    if (items) {
      for (let item of items) {
        idSet[item.id] = true
      }
    }
    return idSet
  }

  prettifyJSON(content: string) {
    return JSON.stringify(JSON.parse(content), null, 3)
  }

  logMessageLevel(message: string, defaultLevel: LogLevel): LogLevel {
    let logLevel = defaultLevel
    let match = Constants.LOG_LEVEL_REGEX.exec(message)
    if (match != null) {
      const logLevelStr = match[1]
      if (logLevelStr == 'DEBUG') {
        logLevel = LogLevel.DEBUG
      } else if (logLevelStr == 'INFO') {
        logLevel = LogLevel.INFO
      } else if (logLevelStr == 'WARN') {
        logLevel = LogLevel.WARN
      } else if (logLevelStr == 'ERROR') {
        logLevel = LogLevel.ERROR
      }
    }
    return logLevel
  }

  completePath(path: string): string {
    if (this.apiRoot == undefined) {
      const apiRootElement = document.getElementById('cp-div')
      if (apiRootElement) {
        this.apiRoot = apiRootElement.innerText
      } else {
        this.apiRoot = '/'
      }
    }
    if (this.apiRoot != '/') {
      return this.apiRoot + path
    }
    return path
  }

  private specToDomainSpecification(specification: Specification): DomainSpecification {
    return {
      id: specification.id,
      sname: specification.sname,
      fname: specification.fname,
      description: specification.description,
      hidden: specification.hidden,
      groupId: specification.group,
      option: specification.group != undefined,
      collapsed: false,
      group: false,
      order: specification.order,
      domain: specification.domain,
    }
  }

  private specGroupToDomainSpecification(group: SpecificationGroup): DomainSpecification {
    return {
      id: group.id,
      sname: group.sname,
      fname: group.fname,
      description: group.description,
      hidden: false,
      group: true,
      option: false,
      collapsed: true,
      domain: group.domain,
      order: group.order,
      options: []
    }
  }

  toDomainSpecifications(groups: SpecificationGroup[], specs: Specification[]): DomainSpecification[] {
    const groupMap: {[id: number]: DomainSpecification} = {}
    let results: DomainSpecification[] = []
    for (let group of groups) {
      const groupAsDomainSpecification = this.specGroupToDomainSpecification(group)
      results.push(groupAsDomainSpecification)
      groupMap[group.id] = groupAsDomainSpecification
    }
    for (let spec of specs) {
      if (spec.group == undefined) {
        results.push(this.specToDomainSpecification(spec))
      } else {
        if (groupMap[spec.group]) {
          groupMap[spec.group].options!.push(this.specToDomainSpecification(spec))
        }
      }
    }
    for (let key in groupMap) {
      this.setSpecificationGroupVisibility(groupMap[key])
    }
    return this.sortDomainSpecifications(results)
  }

  setSpecificationGroupVisibility(group: DomainSpecification) {
    if (group.group) {
      const hasVisible = find(group.options, (s) => !s.hidden)
      group.hidden = !hasVisible
    }
  }

  sortDomainSpecifications(specs: DomainSpecification[]) {
    // Apply sorting.
    specs.sort((a, b) => a.order - b.order || a.fname.localeCompare(b.fname))
    // Sort also options.
    specs.forEach(spec => {
      if (spec.options) {
        spec.options.sort((a, b) => a.order - b.order || a.fname.localeCompare(b.fname))
      }
    })
    return specs
  }

  toSpecifications(domainSpecifications: DomainSpecification[]) {
    const specs: Specification[] = []
    for (let domainSpec of domainSpecifications) {
      if (domainSpec.group && domainSpec.options) {
        for (let option of domainSpec.options) {
          specs.push({
            id: option.id,
            sname: domainSpec.sname + ' - ' +option.sname,
            fname: domainSpec.fname + ' - ' +option.fname,
            domain: option.domain,
            order: option.order,
            hidden: false
          })
        }
      } else if (!domainSpec.group) {
        specs.push({
          id: domainSpec.id,
          sname: domainSpec.sname,
          fname: domainSpec.fname,
          domain: domainSpec.domain,
          order: domainSpec.order,
          hidden: false
        })
      }
    }
    return specs
  }
}
