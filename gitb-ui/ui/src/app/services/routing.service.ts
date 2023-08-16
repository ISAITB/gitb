import { Injectable } from '@angular/core';
import { NavigationExtras, Params, Router } from '@angular/router';
import { CommunityTab } from '../pages/admin/user-management/community/community-details/community-tab.enum';
import { ConformanceStatementTab } from '../pages/organisation/conformance-statement/conformance-statement-tab';
import { OrganisationTab } from '../pages/admin/user-management/organisation/organisation-details/OrganisationTab';

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

  toTestHistory(organisationId: number, sessionIdToShow?: string) {
    if (sessionIdToShow != undefined) {
      return this.router.navigate(['organisation', organisationId, 'tests'], { queryParams: { sessionId: sessionIdToShow }})
    } else {
      return this.router.navigate(['organisation', organisationId, 'tests'])
    }
  }

  toCreateConformanceStatement(organisationId: number, systemId: number, communityId?: number) {
    if (communityId == undefined) {
      return this.router.navigate(['organisation', organisationId, 'conformance', 'system', systemId, 'create'])
    } else {
      return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'create'])
    }
  }

  toOwnConformanceStatement(organisationId: number, systemId: number, actorId: number, tab?: ConformanceStatementTab) {
    const pathParts = ['organisation', organisationId, 'conformance', 'system', systemId, 'actor', actorId]
    if (tab != undefined) {
      return this.router.navigate(pathParts, { state: { tab: ConformanceStatementTab[tab] } })
    } else {
      return this.router.navigate(pathParts)
    }
  }

  toConformanceStatement(organisationId: number, systemId: number, actorId: number, communityId: number, tab?: ConformanceStatementTab) {
    let pathParts = ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'actor', actorId]
    if (tab != undefined) {
      return this.router.navigate(pathParts, { state: { tab: ConformanceStatementTab[tab] } })
    } else {
      return this.router.navigate(pathParts)
    }
  }

  toConformanceStatements(communityId: number, organisationId: number, systemId?: number) {
    if (systemId != undefined) {
      return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'], { queryParams: { 'system': systemId } })      
    } else {
      return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'])      
    }
  }

  toOwnConformanceStatements(organisationId: number, systemId?: number) {
    if (systemId != undefined) {
      return this.router.navigate(['organisation', organisationId, 'conformance'], { queryParams: { 'system': systemId } })
    } else {
      return this.router.navigate(['organisation', organisationId, 'conformance'])
    }
  }

  toCreateOrganisation(communityId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', 'create'])
  }

  toOwnOrganisationDetails(tab?: OrganisationTab, viewProperties?: boolean) {
    const navigationPaths = ['settings', 'organisation']
    if (viewProperties == true) {
      if (tab != undefined) {
        return this.router.navigate(navigationPaths, { state: { tab: OrganisationTab[tab] }, queryParams: { 'viewProperties': true } })
      } else {
        return this.router.navigate(navigationPaths, { queryParams: { 'viewProperties': true } })
      }
    } else {
      if (tab != undefined) {
        return this.router.navigate(navigationPaths, { state: { tab: OrganisationTab[tab] } })
      } else {
        return this.router.navigate(navigationPaths)
      }
    }
  }

  toOrganisationDetails(communityId: number, organisationId: number, tab?: OrganisationTab, viewProperties?: boolean) {
    let navigationPaths = ['admin', 'users', 'community', communityId, 'organisation', organisationId]
    if (viewProperties == true) {
      if (tab != undefined) {
        return this.router.navigate(navigationPaths, { state: { tab: OrganisationTab[tab] }, queryParams: { 'viewProperties': true } })
      } else {
        return this.router.navigate(navigationPaths, { queryParams: { 'viewProperties': true } })
      }
    } else {
      if (tab != undefined) {
        return this.router.navigate(navigationPaths, { state: { tab: OrganisationTab[tab] } })
      } else {
        return this.router.navigate(navigationPaths)
      }
    }
  }

  toCreateOwnSystem() {
    return this.router.navigate(['settings', 'organisation', 'system', 'create'])
  }

  toCreateSystem(communityId: number, organisationId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', 'create'])
  }

  toOwnSystemDetails(systemId: number, viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.router.navigate(['settings', 'organisation', 'system', systemId], { queryParams: { 'viewProperties': true } })
    } else {
      return this.router.navigate(['settings', 'organisation', 'system', systemId])
    }
  }

  toSystemDetails(communityId: number, organisationId: number, systemId: number, viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', systemId], { queryParams: { 'viewProperties': true } })
    } else {
      return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', systemId])
    }
  }

  toProfile() {
    return this.router.navigate(['settings', 'profile'])
  }

  toChangePassword() {
    return this.router.navigate(['settings', 'password'])
  }

  toTestCaseExecution(communityId: number, organisationId: number, systemId: number, actorId: number, testCaseId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: { tc: testCaseId }})
  }

  toOwnTestCaseExecution(organisationId: number, systemId: number, actorId: number, testCaseId: number) {
    return this.router.navigate(['organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: { tc: testCaseId }})
  }

  toTestSuiteExecution(communityId: number, organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: { ts: testSuiteId }})
  }

  toOwnTestSuiteExecution(organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.router.navigate(['organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: { ts: testSuiteId }})
  }

  toCreateDomain() {
    return this.router.navigate(['admin', 'domains', 'create'])
  }

  toDomain(domainId: number, tab?: number) {
    return this.router.navigate(['admin', 'domains', domainId], this.addTabExtras(tab))
  }

  toDomains() {
    return this.router.navigate(['admin', 'domains'])
  }

  toCreateSpecification(domainId: number, groupId?:number) {
    if (groupId) {
      return this.router.navigate(['admin', 'domains', domainId, 'specifications', 'create'], { queryParams: {
        group: groupId
      }})
    } else {
      return this.router.navigate(['admin', 'domains', domainId, 'specifications', 'create'])
    }
  }

  toCreateSpecificationGroup(domainId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', 'groups', 'create'])
  }

  toSpecification(domainId: number, specificationId: number, tab?: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId], this.addTabExtras(tab))
  }

  toSpecificationGroup(domainId: number, groupId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', 'groups', groupId])
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

  toSharedTestSuite(domainId: number, testSuiteId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'testsuites', testSuiteId])
  }

  toTestSuite(domainId: number, specificationId: number, testSuiteId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId])
  }

  toSharedTestCase(domainId: number, testSuiteId: number, testCaseId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'testsuites', testSuiteId, 'testcases', testCaseId])
  }

  toTestCase(domainId: number, specificationId: number, testSuiteId: number, testCaseId: number) {
    return this.router.navigate(['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId, 'testcases', testCaseId])
  }

  toCreateCommunity() {
    return this.router.navigate(['admin', 'users', 'community', 'create'])
  }

  toCommunity(communityId: number, tab?: CommunityTab) {
    if (tab != undefined) {
      return this.router.navigate(['admin', 'users', 'community', communityId], { state: { tab: CommunityTab[tab] } })
    } else {
      return this.router.navigate(['admin', 'users', 'community', communityId])
    }
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

  toCreateOwnOrganisationUser() {
    return this.router.navigate(['settings', 'organisation', 'user', 'create'])
  }

  toCreateOrganisationUser(communityId: number, organisationId: number) {
    return this.router.navigate(['admin', 'users', 'community', communityId, 'organisation', organisationId, 'user', 'create'])
  }

  toOwnOrganisationUser(userId: number) {
    return this.router.navigate(['settings', 'organisation', 'user', userId])
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

  private addTabExtras(tabIndex?: number) {
    let extras: NavigationExtras|undefined = undefined
    if (tabIndex != undefined) {
      extras = { state: { tab: tabIndex }}
    }
    return extras
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
