import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { PendingTestSuiteUploadChoice } from '../modals/test-suite-upload-modal/pending-test-suite-upload-choice';
import { TestSuiteUploadResult } from '../modals/test-suite-upload-modal/test-suite-upload-result';
import { ExportSettings } from '../types/export-settings';
import { Actor } from '../types/actor';
import { ConformanceCertificateSettings } from '../types/conformance-certificate-settings';
import { ConformanceResultFullList } from '../types/conformance-result-full-list';
import { Domain } from '../types/domain';
import { DomainParameter } from '../types/domain-parameter';
import { Endpoint } from '../types/endpoint';
import { EndpointParameter } from '../types/endpoint-parameter';
import { FileData } from '../types/file-data.type';
import { ImportItem } from '../types/import-item';
import { ImportPreview } from '../types/import-preview';
import { ImportSettings } from '../types/import-settings';
import { Specification } from '../types/specification';
import { TestResultSearchCriteria } from '../types/test-result-search-criteria';
import { TestSuite } from '../types/test-suite';
import { DataService } from './data.service';
import { RestService } from './rest.service';
import { SystemConfigurationEndpoint } from '../types/system-configuration-endpoint';
import { TestCase } from '../types/test-case';
import { ConformanceStatus } from '../types/conformance-status';
import { FileParam } from '../types/file-param.type';
import { StatementParameterMinimal } from '../types/statement-parameter-minimal';

@Injectable({
  providedIn: 'root'
})
export class ConformanceService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  getDomains(ids?: number[]) {
    let params:any = {}
    if (ids !== undefined && ids.length > 0) {
      params['ids'] = ids.join(',')
    }
    return this.restService.get<Domain[]>({
      path: ROUTES.controllers.ConformanceService.getDomains().url,
      authenticate: true,
      params: params
    })
  }

  getDomainForSpecification(specId: number) {
    return this.restService.get<Domain>({
      path: ROUTES.controllers.ConformanceService.getDomainOfSpecification(specId).url,
      authenticate: true
    })
  }

  getCommunityDomain(communityId: number) {
    return this.restService.get<Domain|undefined>({
      path: ROUTES.controllers.ConformanceService.getCommunityDomain().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  getSpecifications(domainId: number) {
    return this.restService.get<Specification[]>({
      path: ROUTES.controllers.ConformanceService.getDomainSpecs(domainId).url,
      authenticate: true
    })
  }

  getSpecificationsWithIds(ids?: number[], domainIds?: number[], withApiKeys?: boolean) {
    let params: any = {}
    if (ids != undefined && ids.length > 0) {
      params['ids'] = ids.join(',')
    }
    if (domainIds != undefined && domainIds.length > 0) {
      params['domain_ids'] = domainIds.join(',')
    }
    if (withApiKeys != undefined && withApiKeys) {
      params['with_api_keys'] = withApiKeys
    }
    return this.restService.post<Specification[]>({
      path: ROUTES.controllers.ConformanceService.getSpecs().url,
      authenticate: true,
      data: params
    })
  }

  searchActors(domainIds: number[]|undefined, specificationIds: number[]|undefined) {
    const data: any = {}
    if (domainIds && domainIds.length > 0) {
      data["domain_ids"] = domainIds.join(',')
    }
    if (specificationIds && specificationIds.length > 0) {
      data["specification_ids"] = specificationIds.join(',')
    }
    return this.restService.post<Actor[]>({
      path: ROUTES.controllers.ConformanceService.searchActors().url,
      authenticate: true,
      data: data
    })
  }

  searchActorsInDomain(domainId: number, specificationIds: number[]|undefined) {
    const data: any = {
      domain_id: domainId
    }
    if (specificationIds && specificationIds.length > 0) {
      data["specification_ids"] = specificationIds.join(',')
    }
    return this.restService.post<Actor[]>({
      path: ROUTES.controllers.ConformanceService.searchActorsInDomain().url,
      authenticate: true,
      data: data
    })
  }

  getActorsWithIds(ids?: number[], specificationIds?: number[]) {
    let params: any = {}
    if (ids != undefined && ids.length > 0) {
      params['ids'] = ids.join(',')
    }
    if (specificationIds != undefined && specificationIds.length > 0) {
      params['specification_ids'] = specificationIds.join(',')
    }
    return this.restService.post<Actor[]>({
      path: ROUTES.controllers.ConformanceService.getActors().url,
      authenticate: true,
      data: params
    })
  }

  deleteTestResults(sessionIds: string[]) {
    const data: any = {
        session_ids: JSON.stringify(sessionIds)
    }
    if (this.dataService.isCommunityAdmin) {
      data.community_id = this.dataService.community!.id
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.deleteTestResults().url,
      authenticate: true,
      data: data
    })
  }

  deleteObsoleteTestResultsForCommunity(communityId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteObsoleteTestResultsForCommunity().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  deleteObsoleteTestResults() {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteAllObsoleteTestResults().url,
      authenticate: true
    })
  }

  deleteObsoleteTestResultsForSystem(systemId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteObsoleteTestResultsForSystem().url,
      authenticate: true,
      params: {
        system_id: systemId
      }
    })
  }

  getConformanceOverview(criteria: TestResultSearchCriteria, fullResults: boolean, forExport: boolean, sortColumn: string, sortOrder: string) {
    let params: any = {}
    params.full = fullResults
    if (criteria.domainIds != undefined && criteria.domainIds.length > 0) {
      params.domain_ids = criteria.domainIds.join(',')
    }
    if (criteria.specIds != undefined && criteria.specIds.length > 0) {
      params.specification_ids = criteria.specIds.join(',')
    }
    if (criteria.actorIds != undefined && criteria.actorIds.length > 0) {
      params.actor_ids = criteria.actorIds.join(',')
    }
    if (criteria.communityIds != undefined && criteria.communityIds.length > 0) {
      params.community_ids = criteria.communityIds.join(',')
    }
    if (criteria.organisationIds != undefined && criteria.organisationIds.length > 0) {
      params.organization_ids = criteria.organisationIds.join(',')
    }
    if (criteria.systemIds != undefined && criteria.systemIds.length > 0) {
      params.system_ids = criteria.systemIds.join(',')
    }
    if (criteria.organisationProperties && criteria.organisationProperties.length > 0) {
      params.org_params = JSON.stringify(criteria.organisationProperties)
    }
    if (criteria.systemProperties && criteria.systemProperties.length > 0) {
      params.sys_params = JSON.stringify(criteria.systemProperties)
    }
    if (criteria.results !== undefined && criteria.results.length > 0) {
      params.status = criteria.results.join(',')
    }
    if (criteria.endTimeBeginStr !== undefined) {
      params.update_time_begin = criteria.endTimeBeginStr
    }
    if (criteria.endTimeEndStr !== undefined) {
      params.update_time_end = criteria.endTimeEndStr
    }
    params.export = forExport != undefined && forExport
    params.sort_column = sortColumn
    params.sort_order = sortOrder
    return this.restService.post<ConformanceResultFullList>({
      path: ROUTES.controllers.ConformanceService.getConformanceOverview().url,
      authenticate: true,
      data: params
    })
  }

  getConformanceStatus(actorId: number, sutId: number) {
    return this.restService.get<ConformanceStatus>({
      path: ROUTES.controllers.ConformanceService.getConformanceStatus(actorId, sutId).url,
      authenticate: true
    })
  }

  getConformanceStatusForTestSuite(actorId: number, sutId: number, testSuiteId: number) {
    return this.restService.get<ConformanceStatus>({
      path: ROUTES.controllers.ConformanceService.getConformanceStatusForTestSuite(actorId, sutId, testSuiteId).url,
      authenticate: true
    })
  }

  getConformanceCertificateSettings(communityId: number, includeKeystoreData: boolean) {
    return this.restService.get<ConformanceCertificateSettings|undefined>({
      path: ROUTES.controllers.ConformanceService.getConformanceCertificateSettings(communityId).url,
      authenticate: true,
      params: {
        keystore: includeKeystoreData
      }
    })
  }

  downloadConformanceCertificateKeystore(communityId: number) {
		return this.restService.get<ArrayBuffer>({
			path: ROUTES.controllers.ConformanceService.downloadConformanceCertificateKeystore(communityId).url,
			authenticate: true,
			arrayBuffer: true
		})
  }

  updateConformanceCertificateSettings(communityId: number, settings: ConformanceCertificateSettings, updatePasswords: boolean, removeKeystore: boolean) {
    const data:any = {}
    if (settings != undefined) {
      data.title = settings.title
      data.message = settings.message
      data.includeMessage = settings.includeMessage != undefined && settings.includeMessage
      data.includeTestStatus = settings.includeTestStatus != undefined && settings.includeTestStatus
      data.includeTestCases = settings.includeTestCases != undefined && settings.includeTestCases
      data.includeDetails = settings.includeDetails != undefined && settings.includeDetails
      data.includeSignature = settings.includeSignature != undefined && settings.includeSignature
      data.keystoreType = settings.keystoreType
      data.keystorePassword = settings.keystorePassword
      data.keyPassword = settings.keyPassword
    }
    let files: FileParam[]|undefined
    if (settings.keystoreFile != undefined) {
      files = [{param: 'file', data: settings.keystoreFile}]
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.updateConformanceCertificateSettings(communityId).url,
      authenticate: true,
      files: files,
      data: {
        settings: JSON.stringify(data),
        updatePasswords: updatePasswords,
        removeKeystore: removeKeystore
      }
    })
  }

  testKeystoreSettings(communityId: number, settings: ConformanceCertificateSettings|undefined, updatePasswords: boolean) {
    const data: any = {}
    let files: FileParam[]|undefined
    if (settings != undefined) {
      data.keystoreType = settings.keystoreType
      data.keystorePassword = settings.keystorePassword
      data.keyPassword = settings.keyPassword
      if (settings.keystoreFile != undefined) {
        files = [{param: 'file', data: settings.keystoreFile}]
      }
    }
    return this.restService.post<{problem: string, level: string}|undefined>({
      path: ROUTES.controllers.ConformanceService.testKeystoreSettings(communityId).url,
      authenticate: true,
      files: files,
      data: {
        settings: JSON.stringify(data),
        updatePasswords: updatePasswords
      }
    })
  }

  exportDemoConformanceCertificateReport(communityId: number, settings: ConformanceCertificateSettings) {
    let files: FileParam[]|undefined
    const data: any = {}
    if (settings != undefined) {
      data.title = settings.title
      data.includeMessage = settings.includeMessage != undefined && settings.includeMessage
      data.includeTestStatus = settings.includeTestStatus != undefined && settings.includeTestStatus
      data.includeTestCases = settings.includeTestCases != undefined && settings.includeTestCases
      data.includeDetails = settings.includeDetails != undefined && settings.includeDetails
      data.includeSignature = settings.includeSignature != undefined && settings.includeSignature
      if (data.includeMessage) {
        data.message = settings.message
      }
      if (data.includeSignature) {
        data.keystoreType = settings.keystoreType
        data.keystorePassword = settings.keystorePassword
        data.keyPassword = settings.keyPassword
      }
      if (settings.keystoreFile != undefined) {
        files = [{param: 'file', data: settings.keystoreFile}]
      }
    }
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportDemoConformanceCertificateReport(communityId).url,
      files: files,
      data: {
        settings: JSON.stringify(data)
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportOwnConformanceCertificateReport(actorId: number, systemId: number) {
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportOwnConformanceCertificateReport().url,
      data: {
        actor_id: actorId,
        system_id: systemId
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportConformanceCertificateReport(communityId: number, actorId: number, systemId: number, settings: ConformanceCertificateSettings) {
    let data: any = {}
    if (settings != undefined) {
      data.title = settings.title
      data.includeMessage = settings.includeMessage != undefined && settings.includeMessage
      data.includeTestStatus = settings.includeTestStatus != undefined && settings.includeTestStatus
      data.includeTestCases = settings.includeTestCases != undefined && settings.includeTestCases
      data.includeDetails = settings.includeDetails != undefined && settings.includeDetails
      data.includeSignature = settings.includeSignature != undefined && settings.includeSignature
      if (data.includeMessage) {
        data.message = settings.message
      }
    }
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceCertificateReport().url,
      data: {
        settings: JSON.stringify(data),
        community_id: communityId,
        actor_id: actorId,
        system_id: systemId
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  deleteDomain(domainId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteDomain(domainId).url,
      authenticate: true
    })
  }

  updateDomain(domainId: number, shortName: string, fullName: string, description?: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.updateDomain(domainId).url,
      data: {
        sname: shortName,
        fname: fullName,
        description: description
      },
      authenticate: true
    })
  }
  
  createDomain(shortName: string, fullName: string, description?: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createDomain().url,
      authenticate: true,
      data: {
        sname: shortName,
        fname: fullName,
        description: description
      }
    })
  }

  getDomainParameters(domainId: number, loadValues?: boolean) {
    const params: any = {}
    if (loadValues != undefined) {
      params.values = loadValues
    }
    return this.restService.get<DomainParameter[]>({
      path: ROUTES.controllers.ConformanceService.getDomainParameters(domainId).url,
      authenticate: true,
      params: params
    })
  }

  getDomainParametersOfCommunity(communityId: number) {
    return this.restService.get<DomainParameter[]>({
      path: ROUTES.controllers.ConformanceService.getDomainParametersOfCommunity(communityId).url,
      authenticate: true
    })
  }

  downloadDomainParameterFile(domainId: number, domainParameterId: number) {
		return this.restService.get<ArrayBuffer>({
			path: ROUTES.controllers.ConformanceService.downloadDomainParameterFile(domainId, domainParameterId).url,
			authenticate: true,
			arrayBuffer: true
		})
  }

  updateDomainParameter(domainParameterId: number, domainParameterName: string, domainParameterDescription: string|undefined, domainParameterValue: string|File|undefined, domainParameterKind: string, inTests: boolean|undefined, domainId: number) {
    const params: any = {
      name: domainParameterName,
      kind: domainParameterKind
    }
    if (domainParameterDescription != undefined) {
      params.desc = domainParameterDescription
    }
    if (inTests != undefined) {
      params.inTests = inTests
    } else {
      params.inTests = false
    }
    let files: FileParam[]|undefined
    if (domainParameterKind == 'BINARY') {
      if (domainParameterValue != undefined) {
        params.contentType = (domainParameterValue as File).type
        files = [{param: 'file', data: domainParameterValue as File}]
      }
    } else {
      params.value = domainParameterValue
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.updateDomainParameter(domainId, domainParameterId).url,
      authenticate: true,
      data: {
        config: JSON.stringify(params)
      },
      files: files
    })
  }

  createDomainParameter(domainParameterName: string, domainParameterDescription: string|undefined, domainParameterValue: string|File, domainParameterKind: string, inTests: boolean|undefined, domainId: number) {
    const params: any = {
      name: domainParameterName,
      kind: domainParameterKind
    }
    if (domainParameterDescription != undefined) {
      params.desc = domainParameterDescription
    }
    if (inTests != undefined) {
      params.inTests = inTests
    } else {
      params.inTests = false
    }
    let files: FileParam[]|undefined
    if (domainParameterKind == 'BINARY') {
      params.contentType = (domainParameterValue as File).type
      files = [{param: 'file', data: domainParameterValue as File}]
    } else {
      params.value = domainParameterValue as string
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createDomainParameter(domainId).url,
      authenticate: true,
      data: {
        config: JSON.stringify(params)
      },
      files: files
    })
  }

  deleteDomainParameter(domainParameterId: number, domainId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteDomainParameter(domainId, domainParameterId).url,
      authenticate: true
    })
  }

  deployTestSuite(specificationIds: number[], file: File) {
    return this.restService.post<TestSuiteUploadResult>({
      path: ROUTES.controllers.ConformanceService.deployTestSuiteToSpecifications().url,
      data: {
        specification_ids: specificationIds.join(',')
      },
      files: [{param: 'file', data: file}],
      authenticate: true
    })
  }

  resolvePendingTestSuite(pendingFolderId: string, overallAction: string, specificationIds: number[], specificationActions?: PendingTestSuiteUploadChoice[]) {
    const data: any = {
        pending_id: pendingFolderId,
        pending_action: overallAction,
        specification_ids: specificationIds.join(',')
    }
    if (specificationActions != undefined) {
      data.actions = JSON.stringify(specificationActions)
    }
    return this.restService.post<TestSuiteUploadResult>({
      path: ROUTES.controllers.ConformanceService.resolvePendingTestSuites().url,
      authenticate: true,
      data: data
    })
  }

  createSpecification(shortName: string, fullName: string, description: string|undefined, hidden: boolean|undefined, domainId: number) {
    const params:any = {
      sname: shortName,
      fname: fullName,
      hidden: false,
      domain_id: domainId
    }
    if (hidden != undefined) {
      params.hidden = hidden
    }
    if (description != undefined) {
      params.description = description
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createSpecification().url,
      authenticate: true,
      data: params
    })
  }

  getActorsWithSpecificationId(specId: number) {
    return this.restService.get<Actor[]>({
      path: ROUTES.controllers.ConformanceService.getSpecActors(specId).url,
      authenticate: true
    })
  }

  getTestSuites(specificationId: number) {
    return this.restService.get<TestSuite[]>({
      path: ROUTES.controllers.ConformanceService.getSpecTestSuites(specificationId).url,
      authenticate: true
    })
  }

  getDocumentationForPreview(content: string) {
    return this.restService.post<string>({
      path: ROUTES.controllers.ConformanceService.getDocumentationForPreview().url,
      authenticate: true,
      data: {
        content: content
      },
      text: true
    })
  }

  createActor(shortName: string, fullName: string, description: string|undefined, defaultActor: boolean|undefined, hiddenActor: boolean|undefined, displayOrder: number|undefined, domainId: number, specificationId: number) {
    if (hiddenActor == undefined) {
      hiddenActor = false
    }
    if (defaultActor == undefined) {
      defaultActor = false
    }
    const data: any = {
        actor_id: shortName,
        name: fullName,
        description: description,
        default: defaultActor,
        hidden: hiddenActor,
        domain_id: domainId,
        spec_id: specificationId
    }
    if (displayOrder != undefined) {
      data.displayOrder = Number(displayOrder)
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createActor().url,
      authenticate: true,
      data: data
    })
  }

  getEndpointsForActor(actorId: number) {
    return this.restService.get<Endpoint[]>({
      path: ROUTES.controllers.ConformanceService.getEndpointsForActor(actorId).url,
      authenticate: true
    })
  }

  createEndpoint(name: string, description: string|undefined, actor: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createEndpoint().url,
      authenticate: true,
      data: {
        name: name,
        description: description,
        actor_id: actor
      }
    })
  }

  getEndpoints(endpointIds: number[]) {
    const params: any = {}
    if (endpointIds != undefined) params["ids"] = endpointIds.join(",")
    return this.restService.get<Endpoint[]>({
      path: ROUTES.controllers.ConformanceService.getEndpoints().url,
      authenticate: true,
      params: params
    })
  }

  createParameter(name: string, description: string|undefined, use: string, kind: string, adminOnly: boolean, notForTests: boolean, hidden: boolean, allowedValues: string|undefined, dependsOn: string|undefined, dependsOnValue: string|undefined, endpointId: number) {
    return this.restService.post<EndpointParameter>({
      path: ROUTES.controllers.ConformanceService.createParameter().url,
      authenticate: true,
      data: {
        name: name,
        description: description,
        use: use,
        kind: kind,
        admin_only: adminOnly,
        not_for_tests: notForTests,
        hidden: hidden,
        allowedValues: allowedValues,
        dependsOn: dependsOn,
        dependsOnValue: dependsOnValue,
        endpoint_id: endpointId
      }
    })
  }

  exportDomain(domainId: number, settings: ExportSettings) {
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportDomain(domainId).url,
      data: {
        values: JSON.stringify(settings)
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  uploadDomainExport(domainId: number, settings: ImportSettings, archiveData: FileData) {
    return this.restService.post<ImportPreview>({
      path: ROUTES.controllers.RepositoryService.uploadDomainExport(domainId).url,
      files: [{param: 'file', data: archiveData.file!}],
      data: {
        settings: JSON.stringify(settings)
      },
      authenticate: true
    })
  }

  cancelDomainImport(domainId: number, pendingImportId: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.cancelDomainImport(domainId).url,
      data: {
        pending_id: pendingImportId        
      },
      authenticate: true
    })
  }

  confirmDomainImport(domainId: number, pendingImportId: string, settings: ImportSettings, items: ImportItem[]) {
    let path: string
    if (this.dataService.isCommunityAdmin) {
      path = ROUTES.controllers.RepositoryService.confirmDomainImportCommunityAdmin(domainId).url
    } else {
      path = ROUTES.controllers.RepositoryService.confirmDomainImportTestBedAdmin(domainId).url
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

  getSystemConfigurations(actorId: number, systemId: number) {
    return this.restService.get<SystemConfigurationEndpoint[]>({
      path: ROUTES.controllers.ConformanceService.getSystemConfigurations().url,
      authenticate: true,
      params: {
        actor_id: actorId,
        system_id: systemId
      }
    })
  }

  checkConfigurations(actorId: number, systemId: number) {
    return this.restService.get<SystemConfigurationEndpoint[]>({
      path: ROUTES.controllers.ConformanceService.checkConfigurations().url,
      authenticate: true,
      params: {
        actor_id: actorId,
        system_id: systemId
      }
    })
  }

  getTestCaseDocumentation(id: number) {
    return this.restService.get<string>({
      path: ROUTES.controllers.ConformanceService.getTestCaseDocumentation(id).url,
      authenticate: true,
      text: true
    })
  }

  getTestSuiteDocumentation(id: number) {
    return this.restService.get<string>({
      path: ROUTES.controllers.ConformanceService.getTestSuiteDocumentation(id).url,
      authenticate: true,
      text: true
    })
  }

  getTestSuiteTestCase(testCaseId: number) {
    return this.restService.get<TestCase>({
      path: ROUTES.controllers.ConformanceService.getTestSuiteTestCase(testCaseId).url,
      authenticate: true
    })
  }

  getStatementParametersOfCommunity(communityId: number) {
    return this.restService.get<StatementParameterMinimal[]>({
      path: ROUTES.controllers.ConformanceService.getStatementParametersOfCommunity(communityId).url,
      authenticate: true
    })
  }

}
