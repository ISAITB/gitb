/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import {Injectable} from '@angular/core';
import {RestService} from './rest.service';
import {DataService} from './data.service';
import {ROUTES} from '../common/global';
import {SelfRegistrationOption} from '../types/self-registration-option.type';
import {Community} from '../types/community';
import {OrganisationParameter} from '../types/organisation-parameter';
import {SystemParameter} from '../types/system-parameter';
import {Parameter} from '../types/parameter';
import {TypedLabelConfig} from '../types/typed-label-config.type';
import {ExportSettings} from '../types/export-settings';
import {ImportSettings} from '../types/import-settings';
import {FileData} from '../types/file-data.type';
import {ImportPreview} from '../types/import-preview';
import {ImportItem} from '../types/import-item';
import {ErrorDescription} from '../types/error-description';
import {ActualUserInfo} from '../types/actual-user-info';
import {CustomProperty} from '../types/custom-property.type';
import {FileParam} from '../types/file-param.type';
import {CommunitySearchResult} from '../types/community-search-result';

@Injectable({
  providedIn: 'root'
})
export class CommunityService {

  constructor(private readonly restService: RestService, private readonly dataService: DataService) { }

  getCommunities(communityIds?: number[], skipDefault?: boolean) {
    let params: any = {}
    if (skipDefault !== undefined) {
      params.skipDefault = skipDefault
    }
    if (communityIds !== undefined && communityIds.length > 0) {
      params.ids = communityIds.join(',')
    }
    return this.restService.get<Community[]>({
      path: ROUTES.controllers.CommunityService.getCommunities().url,
      authenticate: true,
      params: params
    })
  }

  getUserCommunities() {
    return this.restService.get<Community[]>({
      path: ROUTES.controllers.CommunityService.getUserCommunities().url,
      authenticate: true
    })
  }

  searchCommunities(filter: string|undefined, page: number|undefined, limit: number|undefined) {
    return this.restService.get<CommunitySearchResult>({
      path: ROUTES.controllers.CommunityService.searchCommunities().url,
      authenticate: true,
      params: {
        filter: filter,
        page: page,
        limit: limit,
      }
    })
  }

  getUserCommunity() {
    return this.restService.get<Community>({
      path: ROUTES.controllers.CommunityService.getUserCommunity().url,
      authenticate: true
    })
  }

  getSelfRegistrationOptions() {
    return this.restService.get<SelfRegistrationOption[]>({
      path: ROUTES.controllers.CommunityService.getSelfRegistrationOptions().url
    })
  }

  selfRegister(communityId: number, token:string|undefined, organisationShortName: string, organisationFullName: string, templateId: number|undefined, organisationProperties: CustomProperty[]|undefined, userName?: string, userEmail?: string, userPassword?: string) {
    let data:any = {
      community_id: communityId,
      vendor_sname: organisationShortName,
      vendor_fname: organisationFullName,
      user_name: userName,
      user_email: userEmail,
      password: userPassword,
    }
    let files: FileParam[]|undefined
    const props = this.dataService.customPropertiesForPost(organisationProperties)
    data.properties = props.parameterJson
    files = props.files
    if (token != undefined) {
      data.community_selfreg_token = token
    }
    if (templateId != undefined) {
      data.template_id = templateId
    }
    return this.restService.post<ErrorDescription|{id: number}|ActualUserInfo>({
      path: ROUTES.controllers.CommunityService.selfRegister().url,
      files: files,
      data: data
    })
  }

  getOrganisationParameters(communityId: number, forFiltering?: boolean, onlyPublic?: boolean) {
    let params: any = {}
    if (forFiltering !== undefined) {
      params.filtering = forFiltering
    }
    if (onlyPublic !== undefined) {
      params.public = onlyPublic
    }
    return this.restService.get<OrganisationParameter[]>({
      path: ROUTES.controllers.CommunityService.getOrganisationParameters(communityId).url,
      authenticate: true,
      params: params
    })
  }

  getOrganisationParameterValues(orgId: number) {
    return this.restService.get<OrganisationParameter[]>({
      path: ROUTES.controllers.OrganizationService.getOrganisationParameterValues(orgId).url,
      authenticate: true
    })
  }

  getSystemParameters(communityId: number, forFiltering?: boolean, onlyPublic?: boolean) {
    let params: any = {}
    if (forFiltering !== undefined) {
      params.filtering = forFiltering
    }
    if (onlyPublic !== undefined) {
      params.public = onlyPublic
    }
    return this.restService.get<SystemParameter[]>({
      path: ROUTES.controllers.CommunityService.getSystemParameters(communityId).url,
      authenticate: true,
      params: params
    })
  }

  createCommunity(shortName: string, fullName: string, email: string|undefined,
    selfRegType: number, selfRegRestriction: number, selfRegToken: string|undefined, selfRegTokenHelpText: string|undefined, selfRegNotification: boolean|undefined,
    interactionNotification: boolean, description: string|undefined, selfRegForceTemplate: boolean|undefined, selfRegForceProperties: boolean|undefined,
    allowCertificateDownload: boolean, allowStatementManagement: boolean, allowSystemManagement: boolean, allowPostTestOrganisationUpdate: boolean,
    allowPostTestSystemUpdate: boolean, allowPostTestStatementUpdate: boolean, allowAutomationApi: boolean|undefined, allowCommunityView: boolean,
    domainId: number|undefined) {
    const data: any = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email,
      description: description,
      allow_certificate_download: allowCertificateDownload,
      allow_statement_management: allowStatementManagement,
      allow_system_management: allowSystemManagement,
      allow_post_test_org_update: allowPostTestOrganisationUpdate,
      allow_post_test_sys_update: allowPostTestSystemUpdate,
      allow_post_test_stm_update: allowPostTestStatementUpdate,
      allow_community_view: allowCommunityView,
      interaction_notification: interactionNotification
    }
    if (this.dataService.configuration.registrationEnabled) {
      if (selfRegNotification == undefined) selfRegNotification = false
      if (selfRegForceTemplate == undefined) selfRegForceTemplate = false
      if (selfRegForceProperties == undefined) selfRegForceProperties = false
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken
      data.community_selfreg_token_help_text = selfRegTokenHelpText
      data.community_selfreg_notification = selfRegNotification
      data.community_selfreg_force_template = selfRegForceTemplate
      data.community_selfreg_force_properties = selfRegForceProperties
      if (this.dataService.configuration.ssoEnabled) {
        data.community_selfreg_restriction = selfRegRestriction
      }
    }
    if (this.dataService.configuration.automationApiEnabled) {
      if (allowAutomationApi == undefined) allowAutomationApi = false
      data.allow_automation_api = allowAutomationApi
    }
    if (domainId != undefined) data.domain_id = domainId
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.createCommunity().url,
      data: data,
      authenticate: true
    })
  }

  updateCommunity(communityId: number, shortName: string, fullName: string, email: string|undefined,
    selfRegType: number, selfRegRestriction: number, selfRegToken: string|undefined, selfRegTokenHelpText: string|undefined, selfRegNotification: boolean|undefined,
    interactionNotification: boolean, description: string|undefined, selfRegForceTemplate: boolean|undefined, selfRegForceProperties: boolean|undefined,
    allowCertificateDownload: boolean, allowStatementManagement: boolean, allowSystemManagement: boolean, allowPostTestOrganisationUpdate: boolean,
    allowPostTestSystemUpdate: boolean, allowPostTestStatementUpdate: boolean, allowAutomationApi: boolean|undefined, allowCommunityView: boolean,
    domainId: number|undefined) {
    const data: any = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email,
      description: description,
      allow_certificate_download: allowCertificateDownload,
      allow_statement_management: allowStatementManagement,
      allow_system_management: allowSystemManagement,
      allow_post_test_org_update: allowPostTestOrganisationUpdate,
      allow_post_test_sys_update: allowPostTestSystemUpdate,
      allow_post_test_stm_update: allowPostTestStatementUpdate,
      allow_community_view: allowCommunityView,
      interaction_notification: interactionNotification
    }
    if (this.dataService.configuration.registrationEnabled) {
      if (selfRegNotification == undefined) selfRegNotification = false
      if (selfRegForceTemplate == undefined) selfRegForceTemplate = false
      if (selfRegForceProperties == undefined) selfRegForceProperties = false
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken
      data.community_selfreg_token_help_text = selfRegTokenHelpText
      data.community_selfreg_notification = selfRegNotification
      data.community_selfreg_force_template = selfRegForceTemplate
      data.community_selfreg_force_properties = selfRegForceProperties
      if (this.dataService.configuration.ssoEnabled) {
        data.community_selfreg_restriction = selfRegRestriction
      }
    }
    if (this.dataService.configuration.automationApiEnabled) {
      if (allowAutomationApi == undefined) allowAutomationApi = false
      data.allow_automation_api = allowAutomationApi
    }
    if (domainId != undefined) data.domain_id = domainId
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.updateCommunity(communityId).url,
      data: data,
      authenticate: true
    })
  }

  deleteCommunity(communityId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.CommunityService.deleteCommunity(communityId).url,
      authenticate: true
    })
  }

  getCommunityById(communityId: number) {
    return this.restService.get<Community>({
      path: ROUTES.controllers.CommunityService.getCommunityById(communityId).url,
      authenticate: true
    })
  }

  orderOrganisationParameters(community: number, orderedIds: number[]) {
    const data = {
      ids: orderedIds.join(',')
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.orderOrganisationParameters(community).url,
      data: data,
      authenticate: true
    })
  }

  orderSystemParameters(community: number, orderedIds: number[]) {
    const data = {
      ids: orderedIds.join(',')
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.orderSystemParameters(community).url,
      data: data,
      authenticate: true
    })
  }

  deleteOrganisationParameter(parameterId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.CommunityService.deleteOrganisationParameter(parameterId).url,
      authenticate: true
    })
  }

  deleteSystemParameter(parameterId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.CommunityService.deleteSystemParameter(parameterId).url
    })
  }

  createOrganisationParameter(parameter: Parameter, communityId: number) {
    const data = {
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      in_selfreg: parameter.inSelfRegistration,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      defaultValue: parameter.defaultValue,
      community_id: communityId
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.createOrganisationParameter().url,
      data: data,
      authenticate: true
    })
  }

  createSystemParameter(parameter: Parameter, communityId: number) {
    const data = {
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      defaultValue: parameter.defaultValue,
      community_id: communityId
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.createSystemParameter().url,
      data: data,
      authenticate: true
    })
  }

  updateOrganisationParameter(parameter: Parameter, communityId: number) {
    const data = {
      id: parameter.id,
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      in_selfreg: parameter.inSelfRegistration,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      defaultValue: parameter.defaultValue,
      community_id: communityId
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.updateOrganisationParameter(parameter.id).url,
      data: data,
      authenticate: true
    })
  }

  updateSystemParameter(parameter: Parameter, communityId: number) {
    const data = {
      id: parameter.id,
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      defaultValue: parameter.defaultValue,
      community_id: communityId
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.updateSystemParameter(parameter.id).url,
      data: data,
      authenticate: true
    })
  }

  getCommunityLabels(communityId: number) {
    return this.restService.get<TypedLabelConfig[]>({
      path: ROUTES.controllers.CommunityService.getCommunityLabels(communityId).url,
      authenticate: true
    })
  }

  setCommunityLabels(communityId: number, labels: TypedLabelConfig[]) {
    return this.restService.post<void>({
      path: ROUTES.controllers.CommunityService.setCommunityLabels(communityId).url,
      data: {
        values: JSON.stringify(labels)
      },
      authenticate: true
    })
  }

  exportCommunity(communityId: number, settings: ExportSettings) {
    let path
    if (settings.themes) {
      path = ROUTES.controllers.RepositoryService.exportCommunityAndSettings(communityId).url
    } else {
      path = ROUTES.controllers.RepositoryService.exportCommunity(communityId).url
    }
    return this.restService.post<ArrayBuffer>({
      path: path,
      data: {
        values: JSON.stringify(settings)
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportSystemSettings(settings: ExportSettings) {
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportSystemSettings().url,
      data: {
        values: JSON.stringify(settings)
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  uploadSystemSettingsExport(settings: ImportSettings, archiveData: FileData) {
    return this.restService.post<ImportPreview|ErrorDescription>({
      path: ROUTES.controllers.RepositoryService.uploadSystemSettingsExport().url,
      files: [{param: 'file', data: archiveData.file!}],
      data: {
        settings: JSON.stringify(settings)
      },
      authenticate: true
    })
  }

  uploadCommunityExport(communityId: number, settings: ImportSettings, archiveData: FileData) {
    let pathToUse
    if (this.dataService.isSystemAdmin) {
      pathToUse = ROUTES.controllers.RepositoryService.uploadCommunityExportTestBedAdmin(communityId).url
    } else {
      pathToUse = ROUTES.controllers.RepositoryService.uploadCommunityExportCommunityAdmin(communityId).url
    }
    return this.restService.post<ImportPreview|ErrorDescription>({
      path: pathToUse,
      files: [{param: 'file', data: archiveData.file!}],
      data: {
        settings: JSON.stringify(settings)
      },
      authenticate: true
    })
  }

  cancelSystemSettingsImport(pendingImportId: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.cancelSystemSettingsImport().url,
      data: {
        pending_id: pendingImportId
      },
      authenticate: true
    })
  }

  cancelCommunityImport(communityId: number, pendingImportId: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.cancelCommunityImport(communityId).url,
      data: {
        pending_id: pendingImportId
      },
      authenticate: true
    })
  }

  confirmSystemSettingsImport(pendingImportId: string, settings: ImportSettings, items: ImportItem[]) {
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.confirmSystemSettingsImport().url,
      data: {
        settings: JSON.stringify(settings),
        pending_id: pendingImportId,
        items: JSON.stringify(items)
      },
      authenticate: true
    })
  }

  confirmCommunityImport(communityId: number, pendingImportId: string, settings: ImportSettings, items: ImportItem[]) {
    let path: string
    if (this.dataService.isCommunityAdmin) {
      path = ROUTES.controllers.RepositoryService.confirmCommunityImportCommunityAdmin(communityId).url
    } else {
      path = ROUTES.controllers.RepositoryService.confirmCommunityImportTestBedAdmin(communityId).url
    }
    return this.restService.post<void>({
      path: path,
      data: {
        settings: JSON.stringify(settings),
        pending_id: pendingImportId,
        items: JSON.stringify(items)
      },
      authenticate: true
    })
  }

  getCommunityIdOfDomain(domainId: number) {
    return this.restService.get<{id: number}|undefined>({
      path: ROUTES.controllers.CommunityService.getCommunityIdOfDomain().url,
      authenticate: true,
      params: {
        domain_id: domainId
      }
    })
  }

  getCommunityIdOfActor(actorId: number) {
    return this.restService.get<{id: number}|undefined>({
      path: ROUTES.controllers.CommunityService.getCommunityIdOfActor().url,
      authenticate: true,
      params: {
        actor_id: actorId
      }
    })
  }

  getCommunityIdOfSnapshot(snapshotId: number) {
    return this.restService.get<{id: number}|undefined>({
      path: ROUTES.controllers.CommunityService.getCommunityIdOfSnapshot().url,
      authenticate: true,
      params: {
        snapshot: snapshotId
      }
    })
  }

}
