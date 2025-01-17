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
import { ConformanceStatementItemInfo } from '../types/conformance-statement-item-info';
import { ConformanceSnapshot } from '../types/conformance-snapshot';
import { BadgesInfo } from '../components/manage-badges/badges-info';
import { HttpResponse } from '@angular/common/http';
import { ConformanceStatementItem } from '../types/conformance-statement-item';
import { ConformanceStatementWithResults } from '../types/conformance-statement-with-results';
import { ConformanceSnapshotList } from '../types/conformance-snapshot-list';
import { CommunityKeystore } from '../types/community-keystore';
import { ConformanceOverviewCertificateSettings } from '../types/conformance-overview-certificate-settings';
import { BadgePlaceholderInfo } from '../modals/conformance-certificate-modal/badge-placeholder-info';
import { Constants } from '../common/constants';
import { SystemParameter } from '../types/system-parameter';
import { OrganisationParameter } from '../types/organisation-parameter';
import { ErrorDescription } from '../types/error-description';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ConformanceService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  getDomain(domainId: number) {
    return this.restService.get<Domain>({
      path: ROUTES.controllers.ConformanceService.getDomain(domainId).url,
      authenticate: true
    })
  }

  getDomains(ids?: number[], withApiKeys?: boolean) {
    let params:any = {}
    if (ids !== undefined && ids.length > 0) {
      params['ids'] = ids.join(',')
    }
    if (withApiKeys != undefined) {
      params['keys'] = withApiKeys == true
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

  getDomainOfActor(actorId: number) {
    return this.restService.get<Domain>({
      path: ROUTES.controllers.ConformanceService.getDomainOfActor(actorId).url,
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

  getCommunityDomains(communityId: number) {
    return this.restService.get<{ linkedDomain: number|undefined, domains: Domain[] }>({
      path: ROUTES.controllers.ConformanceService.getCommunityDomains().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  getDomainSpecifications(domainId: number, withGroups?: boolean) {
    let params: any = {}
    if (withGroups != undefined) {
      params['groups'] = withGroups
    }
    return this.restService.get<Specification[]>({
      path: ROUTES.controllers.ConformanceService.getDomainSpecs(domainId).url,
      authenticate: true,
      params: params
    })
  }

  getSpecification(id: number) {
    return this.restService.get<Specification>({
      path: ROUTES.controllers.ConformanceService.getSpecification(id).url,
      authenticate: true
    })
  }

  getSpecificationsWithIds(ids?: number[], domainIds?: number[], groupIds?: number[]) {
    let params: any = {}
    if (ids != undefined && ids.length > 0) {
      params['ids'] = ids.join(',')
    }
    if (domainIds != undefined && domainIds.length > 0) {
      params['domain_ids'] = domainIds.join(',')
    }
    if (groupIds != undefined && groupIds.length > 0) {
      params['group_ids'] = groupIds.join(',')
    }
    return this.restService.post<Specification[]>({
      path: ROUTES.controllers.ConformanceService.getSpecs().url,
      authenticate: true,
      data: params
    })
  }

  searchActors(domainIds: number[]|undefined, specificationIds: number[]|undefined, specificationGroupIds: number[]|undefined) {
    const data: any = {}
    if (domainIds && domainIds.length > 0) {
      data["domain_ids"] = domainIds.join(',')
    }
    if (specificationIds && specificationIds.length > 0) {
      data["specification_ids"] = specificationIds.join(',')
    }
    if (specificationGroupIds != undefined && specificationGroupIds.length > 0) {
      data['group_ids'] = specificationGroupIds.join(',')
    }
    return this.restService.post<Actor[]>({
      path: ROUTES.controllers.ConformanceService.searchActors().url,
      authenticate: true,
      data: data
    })
  }

  searchActorsInDomain(domainId: number, specificationIds: number[]|undefined, specificationGroupIds: number[]|undefined) {
    const data: any = {
      domain_id: domainId
    }
    if (specificationIds && specificationIds.length > 0) {
      data["specification_ids"] = specificationIds.join(',')
    }
    if (specificationGroupIds != undefined && specificationGroupIds.length > 0) {
      data['group_ids'] = specificationGroupIds.join(',')
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

  getActor(actorId: number, specificationId: number) {
    return this.restService.get<Actor>({
      path: ROUTES.controllers.ConformanceService.getActor(actorId).url,
      authenticate: true,
      params: {
        spec: specificationId
      }
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

  deleteObsoleteTestResultsForOrganisation(organisationId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteObsoleteTestResultsForOrganisation().url,
      authenticate: true,
      params: {
        organization_id: organisationId
      }
    })
  }

  getConformanceOverview(criteria: TestResultSearchCriteria, snapshotId: number|undefined, fullResults: boolean, forExport: boolean, sortColumn: string, sortOrder: string, page: number, limit: number) {
    let params: any = {}
    params.full = fullResults
    params.page = page
    params.limit = limit
    if (snapshotId != undefined) {
      params.snapshot = snapshotId
    }
    if (criteria.domainIds != undefined && criteria.domainIds.length > 0) {
      params.domain_ids = criteria.domainIds.join(',')
    }
    if (criteria.specIds != undefined && criteria.specIds.length > 0) {
      params.specification_ids = criteria.specIds.join(',')
    }
    if (criteria.specGroupIds !== undefined && criteria.specGroupIds.length > 0) {
      params.group_ids = criteria.specGroupIds.join(',')
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

  getConformanceStatus(actorId: number, sutId: number, snapshotId?: number) {
    let params: any = undefined
    if (snapshotId != undefined) {
      params = {
        snapshot: snapshotId
      }
    }
    return this.restService.get<ConformanceStatus|undefined>({
      path: ROUTES.controllers.ConformanceService.getConformanceStatus(actorId, sutId).url,
      authenticate: true,
      params: params
    })
  }

  getConformanceStatusForTestSuiteExecution(actorId: number, sutId: number, testSuiteId: number) {
    return this.restService.get<ConformanceStatus>({
      path: ROUTES.controllers.ConformanceService.getConformanceStatusForTestSuiteExecution(actorId, sutId, testSuiteId).url,
      authenticate: true
    })
  }

  getCommunityKeystoreInfo(communityId: number) {
    return this.restService.get<CommunityKeystore|undefined>({
      path: ROUTES.controllers.ConformanceService.getCommunityKeystoreInfo(communityId).url,
      authenticate: true
    })
  }

  getConformanceCertificateSettings(communityId: number) {
    return this.restService.get<ConformanceCertificateSettings|undefined>({
      path: ROUTES.controllers.ConformanceService.getConformanceCertificateSettings(communityId).url,
      authenticate: true
    })
  }

  getConformanceOverviewCertificateSettings(communityId: number) {
    return this.restService.get<ConformanceOverviewCertificateSettings|undefined>({
      path: ROUTES.controllers.ConformanceService.getConformanceOverviewCertificateSettings(communityId).url,
      authenticate: true
    })
  }

  getConformanceOverviewCertificateSettingsWithApplicableMessage(communityId: number, level: 'all'|'domain'|'group'|'specification', identifier: number|undefined, snapshotId: number|undefined) {
    const params:any = {
      level: level
    }
    if (identifier != undefined) params.id = identifier
    if (snapshotId != undefined) params.snapshot = snapshotId
    return this.restService.get<ConformanceOverviewCertificateSettings|undefined>({
      path: ROUTES.controllers.ConformanceService.getConformanceOverviewCertificateSettingsWithApplicableMessage(communityId).url,
      authenticate: true,
      params: params
    })
  }

  getResolvedMessageForConformanceOverviewCertificate(communityId: number, messageId: number, systemId: number, domainId: number|undefined, groupId: number|undefined, specificationId: number|undefined, snapshotId: number|undefined) {
    const params:any = {
      id: messageId,
      system_id: systemId
    }
    if (domainId != undefined) params.domain_id = domainId
    if (groupId != undefined) params.group_id = groupId
    if (specificationId != undefined) params.spec_id = specificationId
    if (snapshotId != undefined) params.snapshot = snapshotId
    return this.restService.get<string|undefined>({
      path: ROUTES.controllers.ConformanceService.getResolvedMessageForConformanceOverviewCertificate(communityId).url,
      authenticate: true,
      text: true,
      params: params
    })
  }

  getResolvedMessageForConformanceStatementCertificate(communityId: number, systemId: number, actorId: number, snapshotId: number|undefined) {
    const params:any = {
      system_id: systemId,
      actor_id: actorId
    }
    if (snapshotId != undefined) params.snapshot = snapshotId
    return this.restService.get<string|undefined>({
      path: ROUTES.controllers.ConformanceService.getResolvedMessageForConformanceStatementCertificate(communityId).url,
      authenticate: true,
      text: true,
      params: params
    })
  }

  deleteCommunityKeystore(communityId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteCommunityKeystore(communityId).url,
      authenticate: true,
    })
  }

  downloadCommunityKeystore(communityId: number) {
		return this.restService.get<ArrayBuffer>({
			path: ROUTES.controllers.ConformanceService.downloadCommunityKeystore(communityId).url,
			authenticate: true,
			arrayBuffer: true
		})
  }

  conformanceOverviewCertificateEnabled(communityId: number, reportLevel: 'all'|'domain'|'specification'|'group') {
    return this.restService.get<{exists: boolean}>({
      path: ROUTES.controllers.ConformanceService.conformanceOverviewCertificateEnabled(communityId).url,
      authenticate: true,
      params: {
        level: reportLevel
      }
    })
  }

  testCommunityKeystore(communityId: number, settings: Partial<CommunityKeystore>) {
    let data: any
    let files: FileParam[]|undefined
    if (settings != undefined) {
      data = {}
      data.type = settings.keystoreType
      data.keystore_pass = settings.keystorePassword
      data.key_pass = settings.keyPassword
      if (settings.keystoreFile != undefined) {
        files = [{param: 'file', data: settings.keystoreFile}]
      }
    }
    return this.restService.post<{problem: string, level: string}|undefined>({
      path: ROUTES.controllers.ConformanceService.testCommunityKeystore(communityId).url,
      authenticate: true,
      files: files,
      data: data
    })
  }

  saveCommunityKeystore(communityId: number, settings: Partial<CommunityKeystore>) {
    let data: any
    let files: FileParam[]|undefined
    if (settings != undefined) {
      data = {}
      data.type = settings.keystoreType
      data.keystore_pass = settings.keystorePassword
      data.key_pass = settings.keyPassword
      if (settings.keystoreFile != undefined) {
        files = [{param: 'file', data: settings.keystoreFile}]
      }
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.saveCommunityKeystore(communityId).url,
      authenticate: true,
      files: files,
      data: data
    })
  }


  deleteDomain(domainId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteDomain(domainId).url,
      authenticate: true
    })
  }

  updateDomain(domainId: number, shortName: string, fullName: string, description?: string, reportMetadata?: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.updateDomain(domainId).url,
      data: {
        sname: shortName,
        fname: fullName,
        description: description,
        metadata: reportMetadata
      },
      authenticate: true
    })
  }

  createDomain(shortName: string, fullName: string, description?: string, reportMetadata?: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createDomain().url,
      authenticate: true,
      data: {
        sname: shortName,
        fname: fullName,
        description: description,
        metadata: reportMetadata
      }
    })
  }

  getDomainParameters(domainId: number, loadValues?: boolean, onlySimple?: boolean) {
    const params: any = {}
    if (loadValues != undefined) {
      params.values = loadValues
    }
    if (onlySimple != undefined) {
      params.simple = onlySimple
    }
    return this.restService.get<DomainParameter[]>({
      path: ROUTES.controllers.ConformanceService.getDomainParameters(domainId).url,
      authenticate: true,
      params: params
    })
  }

  getDomainParametersOfCommunity(communityId: number, loadValues?: boolean, onlySimple?: boolean) {
    const params: any = {}
    if (loadValues != undefined) {
      params.values = loadValues
    }
    if (onlySimple != undefined) {
      params.simple = onlySimple
    }
    return this.restService.get<DomainParameter[]>({
      path: ROUTES.controllers.ConformanceService.getDomainParametersOfCommunity(communityId).url,
      authenticate: true,
      params: params
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
    return this.restService.post<ErrorDescription|void>({
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
    return this.restService.post<ErrorDescription|void>({
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

  deployTestSuite(domainId: number, specificationIds: number[], sharedTestSuite: boolean, file: File) {
    return this.restService.post<TestSuiteUploadResult>({
      path: ROUTES.controllers.ConformanceService.deployTestSuiteToSpecifications().url,
      data: {
        specification_ids: specificationIds.join(','),
        domain_id: domainId,
        shared: sharedTestSuite
      },
      files: [{param: 'file', data: file}],
      authenticate: true
    })
  }

  resolvePendingTestSuite(pendingFolderId: string, overallAction: string, domainId: number, specificationIds: number[], specificationActions?: PendingTestSuiteUploadChoice[]) {
    const data: any = {
        pending_id: pendingFolderId,
        pending_action: overallAction,
        domain_id: domainId,
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

  createSpecification(shortName: string, fullName: string, description: string|undefined, reportMetadata: string|undefined, hidden: boolean|undefined, domainId: number, groupId: number|undefined, badges: BadgesInfo) {
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
    if (reportMetadata != undefined) {
      params.metadata = reportMetadata
    }
    if (groupId != undefined) {
      params.group_id = groupId
    }
    const files = this.dataService.parametersForBadgeUpdate(badges, params)
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createSpecification().url,
      authenticate: true,
      data: params,
      files: files
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

  getSharedTestSuites(domainId: number) {
    return this.restService.get<TestSuite[]>({
      path: ROUTES.controllers.ConformanceService.getSharedTestSuites(domainId).url,
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

  createActor(shortName: string, fullName: string, description: string|undefined, reportMetadata: string|undefined,defaultActor: boolean|undefined, hiddenActor: boolean|undefined, displayOrder: number|undefined, domainId: number, specificationId: number, badges: BadgesInfo) {
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
        metadata: reportMetadata,
        default: defaultActor,
        hidden: hiddenActor,
        domain_id: domainId,
        spec_id: specificationId
    }
    if (displayOrder != undefined) {
      data.displayOrder = Number(displayOrder)
    }
    const files = this.dataService.parametersForBadgeUpdate(badges, data)
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createActor().url,
      authenticate: true,
      data: data,
      files: files
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

  createParameter(name: string, testKey: string, description: string|undefined, use: string, kind: string, adminOnly: boolean, notForTests: boolean, hidden: boolean, allowedValues: string|undefined, dependsOn: string|undefined, dependsOnValue: string|undefined, defaultValue: string|undefined, endpointId: number|undefined, actorId: number) {
    const data:any = {
      name: name,
      test_key: testKey,
      description: description,
      use: use,
      kind: kind,
      admin_only: adminOnly,
      not_for_tests: notForTests,
      hidden: hidden,
      allowedValues: allowedValues,
      dependsOn: dependsOn,
      dependsOnValue: dependsOnValue,
      defaultValue: defaultValue,
      actor_id: actorId
    }
    if (endpointId) {
      data.endpoint_id = endpointId
    }
    return this.restService.post<{endpoint: number}>({
      path: ROUTES.controllers.ConformanceService.createParameter().url,
      authenticate: true,
      data: data
    })
  }

  exportDomain(domainId: number, settings: ExportSettings) {
    let path
    if (settings.themes) {
      path = ROUTES.controllers.RepositoryService.exportDomainAndSettings(domainId).url
    } else {
      path = ROUTES.controllers.RepositoryService.exportDomain(domainId).url
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

  exportDeletions(settings: ExportSettings) {
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportDeletions().url,
      data: {
        values: JSON.stringify(settings)
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  uploadDomainExport(domainId: number, settings: ImportSettings, archiveData: FileData) {
    let pathToUse
    if (this.dataService.isSystemAdmin) {
      pathToUse = ROUTES.controllers.RepositoryService.uploadDomainExportTestBedAdmin(domainId).url
    } else {
      pathToUse = ROUTES.controllers.RepositoryService.uploadDomainExportCommunityAdmin(domainId).url
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

  uploadDeletionsExport(settings: ImportSettings, archiveData: FileData) {
    return this.restService.post<ImportPreview|ErrorDescription>({
      path: ROUTES.controllers.RepositoryService.uploadDeletionsExport().url,
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

  confirmDeletionsImport(pendingImportId: string, settings: ImportSettings, items: ImportItem[]) {
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.confirmDeletionsImport().url,
      data: {
        settings: JSON.stringify(settings),
        pending_id: pendingImportId,
        items: JSON.stringify(items)
      },
      authenticate: true
    })
  }

  getStatementParameterValues(actorId: number, systemId: number) {
    return this.restService.get<EndpointParameter[]>({
      path: ROUTES.controllers.ConformanceService.getStatementParameterValues().url,
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

  getTestSuiteTestCaseForExecution(testCaseId: number) {
    return this.restService.get<TestCase>({
      path: ROUTES.controllers.ConformanceService.getTestSuiteTestCaseForExecution(testCaseId).url,
      authenticate: true
    })
  }

  getStatementParametersOfCommunity(communityId: number) {
    return this.restService.get<StatementParameterMinimal[]>({
      path: ROUTES.controllers.ConformanceService.getStatementParametersOfCommunity(communityId).url,
      authenticate: true
    })
  }

  linkSharedTestSuite(testSuiteId: number, specificationIds: number[]) {
    const data: any = {
      test_suite_id: testSuiteId
    }
    if (specificationIds.length > 0) {
      data["specification_ids"] = specificationIds.join(',')
    }
    return this.restService.post<TestSuiteUploadResult>({
      path: ROUTES.controllers.ConformanceService.linkSharedTestSuite().url,
      authenticate: true,
      data: data
    })
  }

  confirmLinkSharedTestSuite(testSuiteId: number, specificationIds: number[], specificationActions?: PendingTestSuiteUploadChoice[]) {
    const data: any = {
      test_suite_id: testSuiteId
    }
    if (specificationIds.length > 0) {
      data["specification_ids"] = specificationIds.join(',')
    }
    if (specificationActions != undefined) {
      data.actions = JSON.stringify(specificationActions)
    }
    return this.restService.post<TestSuiteUploadResult>({
      path: ROUTES.controllers.ConformanceService.confirmLinkSharedTestSuite().url,
      authenticate: true,
      data: data
    })
  }

  unlinkSharedTestSuite(testSuiteId: number, specificationIds: number[]) {
    const data: any = {
      test_suite_id: testSuiteId
    }
    if (specificationIds.length > 0) {
      data["specification_ids"] = specificationIds.join(',')
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.unlinkSharedTestSuite().url,
      authenticate: true,
      data: data
    })
  }

  getAvailableConformanceStatements(domainId: number|undefined, systemId: number) {
    let params:any = {}
    if (domainId) {
      params.domain_id = domainId
    }
    return this.restService.get<ConformanceStatementItemInfo>({
      path: ROUTES.controllers.ConformanceService.getAvailableConformanceStatements(systemId).url,
      authenticate: true,
      params: params
    })
  }

  createConformanceSnapshot(communityId: number, label: string, publicLabel: string|undefined, isPublic: boolean) {
    const data: any = {
      label: label,
      public: isPublic,
      community_id: communityId
    }
    if (publicLabel != undefined) {
      data.publicLabel = publicLabel
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.createConformanceSnapshot().url,
      authenticate: true,
      data: data
    })
  }

  getConformanceSnapshots(communityId: number, withApiKeys?: boolean) {
    return this.restService.get<ConformanceSnapshotList>({
      path: ROUTES.controllers.ConformanceService.getConformanceSnapshots().url,
      authenticate: true,
      params: {
        community_id: communityId,
        public: !this.dataService.isSystemAdmin && !this.dataService.isCommunityAdmin,
        keys: withApiKeys == true
      }
    })
  }

  getConformanceSnapshot(snapshotId: number) {
    return this.restService.get<ConformanceSnapshot>({
      path: ROUTES.controllers.ConformanceService.getConformanceSnapshot(snapshotId).url,
      authenticate: true,
      params: {
        public: !this.dataService.isSystemAdmin && !this.dataService.isCommunityAdmin
      }
    })
  }

  setLatestConformanceStatusLabel(communityId: number, label: string|undefined) {
    const data: any = {}
    if (label != undefined) {
      data.label = label
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.setLatestConformanceStatusLabel(communityId).url,
      authenticate: true,
      data: data
    })
  }

  editConformanceSnapshot(snapshotId: number, label: string, publicLabel: string|undefined, isPublic: boolean) {
    const data: any = {
      label: label,
      public: isPublic
    }
    if (publicLabel != undefined) {
      data.publicLabel = publicLabel
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.ConformanceService.editConformanceSnapshot(snapshotId).url,
      authenticate: true,
      data: data
    })
  }

  deleteConformanceSnapshot(snapshotId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ConformanceService.deleteConformanceSnapshot(snapshotId).url,
      authenticate: true
    })
  }

  getBadgeForStatus(specificationId: number, actorId: number|undefined, status: string, forReport?: boolean) {
    let params: any
    if (forReport != undefined) {
      params = {
        report: forReport
      }
    }
    let path: string
    if (actorId == undefined) {
      path = ROUTES.controllers.SpecificationService.getBadgeForStatus(specificationId, status).url
    } else {
      path = ROUTES.controllers.ActorService.getBadgeForStatus(specificationId, actorId, status).url
    }
    return this.restService.get<HttpResponse<ArrayBuffer>>({
      path: path,
      params: params,
      authenticate: true,
      arrayBuffer: true,
      httpResponse: true
    })
  }

  conformanceBadgeUrl(systemId: number, actorId: number, snapshotId?: number) {
    let params: any = undefined
    if (snapshotId != undefined) {
      params = {
        snapshot: snapshotId
      }
    }
    return this.restService.get<string|undefined>({
      path: ROUTES.controllers.ConformanceService.conformanceBadgeUrl(systemId, actorId).url,
      params: params,
      authenticate: true,
      text: true
    })
  }

  replaceBadgePlaceholdersInCertificateMessage(message: string) {
    // Find placeholders.
    const placeholders: BadgePlaceholderInfo[] = []
    const matches = message.match(Constants.BADGE_PLACEHOLDER_REGEX)
    if (matches) {
      matches.forEach((match) => {
        // $com.gitb.placeholder.BadgeUrl{RESULT|SYSTEM|SPECIFICATION|ACTOR|SNAPSHOT}
        const openBracket = match.indexOf("{")
        const closeBracket = match.indexOf("}", openBracket)
        const parts = match.substring(openBracket+1, closeBracket).split("|")
        const placeholder: BadgePlaceholderInfo = {
          placeholder: match,
          status: parts[0],
          systemId: parseInt(parts[1]),
          specificationId: parseInt(parts[2]),
          actorId: parseInt(parts[3])
        }
        if (parts[4] != "0") {
          placeholder.snapshotId = parseInt(parts[4])
        }
        placeholders.push(placeholder)
      })
    }
    // Replace with images.
    for (let placeholder of placeholders) {
      let imageUrl: string
      if (placeholder.snapshotId == undefined) {
        imageUrl = ROUTES.controllers.ConformanceService.conformanceBadgeReportPreview(placeholder.status, placeholder.systemId, placeholder.specificationId, placeholder.actorId).url
      } else {
        imageUrl = ROUTES.controllers.ConformanceService.conformanceBadgeReportPreviewForSnapshot(placeholder.status, placeholder.systemId, placeholder.specificationId, placeholder.actorId, placeholder.snapshotId).url
      }
      message = message.split(placeholder.placeholder).join(this.dataService.completePath(imageUrl))
    }
    return message
  }

  conformanceBadgeByIds(systemId: number, actorId: number, snapshotId?: number, forReport?: boolean) {
    let params: any = undefined
    if (snapshotId != undefined || forReport != undefined) {
      params = {}
      if (snapshotId != undefined) params.snapshot = snapshotId
      if (forReport != undefined) params.report = forReport
    }
    return this.restService.get<HttpResponse<ArrayBuffer>>({
      path: ROUTES.controllers.ConformanceService.conformanceBadgeByIds(systemId, actorId).url,
      params: params,
      authenticate: true,
      arrayBuffer: true,
      httpResponse: true
    })
  }

  getConformanceStatementsForSystem(system: number, snapshotId?: number) {
    let params: any = undefined
    if (snapshotId != undefined) {
      params = {
        snapshot: snapshotId
      }
    }
    return this.restService.get<ConformanceStatementItem[]>({
      path: ROUTES.controllers.ConformanceService.getConformanceStatementsForSystem(system).url,
      authenticate: true,
      params: params
    })
  }

  getConformanceStatement(system: number, actor: number, snapshotId?: number) {
    let params: any = undefined
    if (snapshotId != undefined) {
      params = {
        snapshot: snapshotId
      }
    }
    return this.restService.get<ConformanceStatementWithResults>({
      path: ROUTES.controllers.ConformanceService.getConformanceStatement(system, actor).url,
      authenticate: true,
      params: params
    })
  }

  updateStatementConfiguration(systemId: number, actorId: number, organisationProperties: OrganisationParameter[]|undefined, systemProperties: SystemParameter[]|undefined, statementProperties: EndpointParameter[]|undefined) {
    const data: any = {
      system_id: systemId,
      actor_id: actorId
    }
    let files: FileParam[] = []
    if (organisationProperties != undefined) {
      const orgProps = this.dataService.customPropertiesForPost(organisationProperties, "org")
      data.org_params = orgProps.parameterJson
      files.push(...orgProps.files)
    }
    if (systemProperties != undefined) {
      const sysProps = this.dataService.customPropertiesForPost(systemProperties, "sys")
      data.sys_params = sysProps.parameterJson
      files.push(...sysProps.files)
    }
    if (statementProperties != undefined) {
      const stmProps = this.dataService.customPropertiesForPost(statementProperties, "stm")
      data.stm_params = stmProps.parameterJson
      files.push(...stmProps.files)
    }
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.ConformanceService.updateStatementConfiguration().url,
      data: data,
      files: files,
      authenticate: true
    })
  }

}
