import { Injectable } from '@angular/core';
import { NavigationExtras, Params, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class RoutingService {

  constructor(
    private router: Router
  ) { }

  toHome() {
    return this.router.navigate(['home'])
  }

  toLogin() {
    return this.router.navigate(['login'])
  }

  toCreateTestBedAdmin() {
    return this.router.navigate(['admin', 'users', 'admin', 'create'])
  }

  toTestBedAdmin(adminId: number) {
    return this.router.navigate(['admin', 'users', 'admin', adminId])
  }

  toSystems(organisationId: number, systemToView?: number, viewProperties?: boolean) {
    const params: Params = {}
    if (systemToView != undefined) {
      params['id'] = systemToView
    }
    if (viewProperties == true) {
      params['viewProperties'] = true
    }
    return this.router.navigate(['organisation', organisationId, 'systems'], { queryParams: params })
  }

  toSystemInfo(organisationId: number, systemId: number, viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'info'], { queryParams: { 'viewProperties': true } })
    } else {
      return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'info'])
    }
  }

  toTestHistory(organisationId: number, systemId: number) {
    return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'tests'])
  }

  toCreateConformanceStatement(organisationId: number, systemId: number) {
    return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'conformance', 'create'])
  }

  toConformanceStatement(organisationId: number, systemId: number, actorId: number, specificationId: number, viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'conformance', 'detail', actorId, specificationId], { queryParams: { 'viewProperties': true } })
    } else {
      return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'conformance', 'detail', actorId, specificationId])
    }
  }

  toConformanceStatements(organisationId: number, systemId: number, systemCount?: number) {
    if (systemCount != undefined) {
      return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'conformance'], { state: { systemCount: systemCount } })
    } else {
      return this.router.navigate(['organisation', organisationId, 'systems', systemId, 'conformance'])
    }
  }

  toCreateOrganisation(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', 'create'])
  }

  toOrganisationDetails(communityId: number, organisationId: number, viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId], { queryParams: { 'viewProperties': true } })
    } else {
      return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId])
    }
  }

  toOwnOrganisationDetails(viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.router.navigate(['settings', 'organisation'], { queryParams: { 'viewProperties': true } })
    } else {
      return this.router.navigate(['settings', 'organisation'])
    }
  }

  toProfile() {
    return this.router.navigate(['settings', 'profile'])
  }

  toChangePassword() {
    return this.router.navigate(['settings', 'password'])
  }

  toTestCaseExecution(organisationId: number, systemId: number, actorId: number, specificationId: number, testCaseId: number) {
    return this.router.navigate(['organisation', organisationId, 'test', systemId, actorId, specificationId, 'execute'], { queryParams: { tc: testCaseId }})
  }

  toTestSuiteExecution(organisationId: number, systemId: number, actorId: number, specificationId: number, testSuiteId: number) {
    return this.router.navigate(['organisation', organisationId, 'test', systemId, actorId, specificationId, 'execute'], { queryParams: { ts: testSuiteId }})
  }

  toCreateDomain() {
    return this.router.navigate(['admin', 'domains', 'create'])
  }

  toDomain(domainId: number) {
    return this.router.navigate(['admin', 'domains', domainId])
  }

  toDomains() {
    return this.router.navigate(['admin', 'domains'])
  }

  toCreateSpecification(domainId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', 'create'])
  }

  toSpecification(domainId: number, specificationId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId])
  }

  toCreateEndpoint(domainId: number, specificationId: number, actorId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId, 'endpoints', 'create'])
  }

  toEndpoint(domainId: number, specificationId: number, actorId: number, endpointId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId, 'endpoints', endpointId])
  }

  toCreateActor(domainId: number, specificationId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'actors', 'create'])
  }

  toActor(domainId: number, specificationId: number, actorId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId])
  }

  toTestSuite(domainId: number, specificationId: number, testSuiteId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId])
  }

  toTestCase(domainId: number, specificationId: number, testSuiteId: number, testCaseId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId, 'testcases', testCaseId])
  }

  toCreateCommunity() {
    return this.router.navigate(['admin', 'users', 'community', 'create'])
  }

  toCommunity(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId])
  }

  toCommunityParameters(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'parameters'])
  }

  toCommunityLabels(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'labels'])
  }

  toCommunityCertificateSettings(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'certificate'])
  }

  toCreateCommunityAdmin(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'admin', 'create'])
  }

  toCommunityAdmin(communityId: number, adminId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'admin', adminId])
  }

  toCreateTrigger(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'triggers', 'create'])
  }

  toTrigger(communityId: number, triggerId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'triggers', triggerId])
  }

  toUserManagement() {
    return this.router.navigate(['admin', 'users'])
  }

  toCreateErrorTemplate(communityId: number, addCopyTestBedDefault: boolean, copySource?: number) {
    this.router.navigate(['admin', 'users', 'community', communityId, 'errortemplates', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
  }

  toErrorTemplate(communityId: number, templateId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'errortemplates', templateId])
  }

  toCreateLegalNotice(communityId: number, addCopyTestBedDefault: boolean, copySource?: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'notices', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
  }

  toLegalNotice(communityId: number, noticeId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'notices', noticeId])
  }

  toCreateLandingPage(communityId: number, addCopyTestBedDefault: boolean, copySource?: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'pages', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
  }

  toLandingPage(communityId: number, pageId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'pages', pageId])
  }

  toCreateOrganisationUser(communityId: number, organisationId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'user', 'create'])
  }

  toOrganisationUser(communityId: number, organisationId: number, userId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'user', userId])
  }

  toSessionDashboard(sessionIdToShow?: string) {
    if (sessionIdToShow != undefined) {
      return this.router.navigate(['admin', 'sessions'], { queryParams: { sessionId: sessionIdToShow }})
    } else {
      return this.router.navigate(['admin', 'sessions'])
    }
  }

  toConformanceDashboard() {
    return this.router.navigate(['admin', 'conformance'])
  }

  toDataImport() {
    return this.router.navigate(['admin', 'import'])
  }

  toDataExport() {
    return this.router.navigate(['admin', 'export'])
  }

  private addCommunityContentExtras(copyTestBedDefault: boolean, copySourceId?: number) {
    let extras: NavigationExtras|undefined = undefined
    if (copyTestBedDefault || copySourceId != undefined) {
      extras = {}
      extras.queryParams = {}
      if (copyTestBedDefault) {
        extras.queryParams['copyDefault'] = true
      }
      if (copySourceId != undefined) {
        extras.queryParams['copy'] = copySourceId
      }
    }
    return extras
  }  
}
