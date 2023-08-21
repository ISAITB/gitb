import { Injectable } from '@angular/core';
import { NavigationEnd, NavigationExtras, Router } from '@angular/router';
import { CommunityTab } from '../pages/admin/user-management/community/community-details/community-tab.enum';
import { ConformanceStatementTab } from '../pages/organisation/conformance-statement/conformance-statement-tab';
import { OrganisationTab } from '../pages/admin/user-management/organisation/organisation-details/OrganisationTab';
import { Constants } from '../common/constants';
import { DataService } from './data.service';
import { MenuItem } from '../types/menu-item.enum';

@Injectable({
  providedIn: 'root'
})
export class RoutingService {

  private navigationMethodExecuted = false

  constructor(
    private router: Router,
    private dataService: DataService
  ) { 
    this.initialise()
  }

  private initialise() {
    this.router.events.subscribe((event) => {
      if (!this.navigationMethodExecuted && event instanceof NavigationEnd) {
        /*
         * We only need to do this matching if we are coming to a page without going through
         * one of the navigation methods (e.g. after a refresh). In other cases we skip this as
         * we always know what menu item applies through the navigate() method.
         */
        setTimeout(() => {
          if (event.url.startsWith('/home')) {
            this.dataService.changePage({ menuItem: MenuItem.home })
          } else if (event.url.startsWith('/login')) {
            this.dataService.changePage({ menuItem: MenuItem.login })
          } else if (event.url.startsWith('/settings/profile')) {
            this.dataService.changePage({ menuItem: MenuItem.myProfile })
          } else if (event.url.startsWith('/settings/organisation')) {
            this.dataService.changePage({ menuItem: MenuItem.myOrganisation })
          } else if (event.url.startsWith('/settings/password')) {
            this.dataService.changePage({ menuItem: MenuItem.changePassword })
          } else if (event.url.startsWith('/admin/sessions')) {
            this.dataService.changePage({ menuItem: MenuItem.sessionDashboard })
          } else if (event.url.startsWith('/admin/conformance')) {
            this.dataService.changePage({ menuItem: MenuItem.conformanceDashboard })
          } else if (event.url.startsWith('/admin/conformance')) {
            this.dataService.changePage({ menuItem: MenuItem.conformanceDashboard })
          } else if (event.url.startsWith('/admin/domains')) {
            this.dataService.changePage({ menuItem: MenuItem.domainManagement })
          } else if (event.url.startsWith('/admin/users')) {
            this.dataService.changePage({ menuItem: MenuItem.communityManagement })
          } else if (event.url.startsWith('/admin/export')) {
            this.dataService.changePage({ menuItem: MenuItem.dataExport })
          } else if (event.url.startsWith('/admin/import')) {
            this.dataService.changePage({ menuItem: MenuItem.dataImport })
          } else if (event.url.startsWith('/organisation/conformance')) {
            this.dataService.changePage({ menuItem: MenuItem.myConformanceStatements })
          } else if (event.url.startsWith('/organisation/tests')) {
            this.dataService.changePage({ menuItem: MenuItem.myTestSessions })
          } else if (event.url.startsWith('/organisation/test')) {
            this.dataService.changePage({ menuItem: MenuItem.myConformanceStatements })
          } else if (event.url.startsWith('/organisation')) {
            this.dataService.changePage({ menuItem: MenuItem.myOrganisation })
          }
        }, 1)
      }
    })
  }

  toHome() {
    return this.navigate(MenuItem.home, ['home'])
  }

  toLogin() {
    return this.navigate(MenuItem.login, ['login'])
  }

  toCreateTestBedAdmin() {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'admin', 'create'])
  }

  toTestBedAdmin(adminId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'admin', adminId])
  }

  toTestHistory(organisationId: number, sessionIdToShow?: string) {
    if (sessionIdToShow != undefined) {
      return this.navigate(MenuItem.myTestSessions, ['organisation', organisationId, 'tests'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID, sessionIdToShow) })
    } else {
      return this.navigate(MenuItem.myTestSessions, ['organisation', organisationId, 'tests'])
    }
  }

  toCreateConformanceStatement(organisationId: number, systemId: number, communityId?: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.myOrganisation, ['organisation', organisationId, 'conformance', 'system', systemId, 'create'])
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'create'])
    }
  }

  toOwnConformanceStatement(organisationId: number, systemId: number, actorId: number, tab?: ConformanceStatementTab) {
    const pathParts = ['organisation', organisationId, 'conformance', 'system', systemId, 'actor', actorId]
    if (tab != undefined) {
      return this.navigate(MenuItem.myConformanceStatements, pathParts, { state: { tab: ConformanceStatementTab[tab] } })
    } else {
      return this.navigate(MenuItem.myConformanceStatements, pathParts)
    }
  }

  toConformanceStatement(organisationId: number, systemId: number, actorId: number, communityId: number, tab?: ConformanceStatementTab) {
    let pathParts = ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'actor', actorId]
    if (tab != undefined) {
      return this.navigate(MenuItem.communityManagement, pathParts, { state: { tab: ConformanceStatementTab[tab] } })
    } else {
      return this.navigate(MenuItem.communityManagement, pathParts)
    }
  }

  toConformanceStatements(communityId: number, organisationId: number, systemId?: number) {
    if (systemId != undefined) {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, systemId) })
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'])      
    }
  }

  toOwnConformanceStatements(organisationId: number, systemId?: number) {
    if (systemId != undefined) {
      return this.navigate(MenuItem.myConformanceStatements, ['organisation', organisationId, 'conformance'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, systemId) })
    } else {
      return this.navigate(MenuItem.myConformanceStatements, ['organisation', organisationId, 'conformance'])
    }
  }

  toCreateOrganisation(communityId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', 'create'])
  }

  toOwnOrganisationDetails(tab?: OrganisationTab, viewProperties?: boolean) {
    const navigationPaths = ['settings', 'organisation']
    if (viewProperties == true) {
      if (tab != undefined) {
        return this.navigate(MenuItem.myOrganisation, navigationPaths, { state: { tab: OrganisationTab[tab] }, queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) })
      } else {
        return this.navigate(MenuItem.myOrganisation, navigationPaths, { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) })
      }
    } else {
      if (tab != undefined) {
        return this.navigate(MenuItem.myOrganisation, navigationPaths, { state: { tab: OrganisationTab[tab] } })
      } else {
        return this.navigate(MenuItem.myOrganisation, navigationPaths)
      }
    }
  }

  toOrganisationDetails(communityId: number, organisationId: number, tab?: OrganisationTab, viewProperties?: boolean) {
    let navigationPaths = ['admin', 'users', 'community', communityId, 'organisation', organisationId]
    if (viewProperties == true) {
      if (tab != undefined) {
        return this.navigate(MenuItem.communityManagement, navigationPaths, { state: { tab: OrganisationTab[tab] }, queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) })
      } else {
        return this.navigate(MenuItem.communityManagement, navigationPaths, { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) })
      }
    } else {
      if (tab != undefined) {
        return this.navigate(MenuItem.communityManagement, navigationPaths, { state: { tab: OrganisationTab[tab] } })
      } else {
        return this.navigate(MenuItem.communityManagement, navigationPaths)
      }
    }
  }

  toCreateOwnSystem() {
    return this.navigate(MenuItem.myOrganisation, ['settings', 'organisation', 'system', 'create'])
  }

  toCreateSystem(communityId: number, organisationId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', 'create'])
  }

  toOwnSystemDetails(systemId: number, viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.navigate(MenuItem.myOrganisation, ['settings', 'organisation', 'system', systemId], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) })
    } else {
      return this.navigate(MenuItem.myOrganisation, ['settings', 'organisation', 'system', systemId])
    }
  }

  toSystemDetails(communityId: number, organisationId: number, systemId: number, viewProperties?: boolean) {
    if (viewProperties == true) {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', systemId], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) })
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', systemId])
    }
  }

  toProfile() {
    return this.navigate(MenuItem.myProfile, ['settings', 'profile'])
  }

  toChangePassword() {
    return this.navigate(MenuItem.changePassword, ['settings', 'password'])
  }

  toTestCaseExecution(communityId: number, organisationId: number, systemId: number, actorId: number, testCaseId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID, testCaseId) })
  }

  toOwnTestCaseExecution(organisationId: number, systemId: number, actorId: number, testCaseId: number) {
    return this.navigate(MenuItem.myConformanceStatements, ['organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID, testCaseId)})
  }

  toTestSuiteExecution(communityId: number, organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SUITE_ID, testSuiteId)})
  }

  toOwnTestSuiteExecution(organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.navigate(MenuItem.myConformanceStatements, ['organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SUITE_ID, testSuiteId)})
  }

  toCreateDomain() {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', 'create'])
  }

  toDomain(domainId: number, tab?: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId], this.addTabExtras(tab))
  }

  toDomains() {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains'])
  }

  toCreateSpecification(domainId: number, groupId?:number) {
    if (groupId) {
      return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', 'create'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SPECIFICATION_GROUP_ID, groupId) })
    } else {
      return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', 'create'])
    }
  }

  toCreateSpecificationGroup(domainId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', 'groups', 'create'])
  }

  toSpecification(domainId: number, specificationId: number, tab?: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', specificationId], this.addTabExtras(tab))
  }

  toSpecificationGroup(domainId: number, groupId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', 'groups', groupId])
  }

  toCreateEndpoint(domainId: number, specificationId: number, actorId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId, 'endpoints', 'create'])
  }

  toEndpoint(domainId: number, specificationId: number, actorId: number, endpointId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId, 'endpoints', endpointId])
  }

  toCreateActor(domainId: number, specificationId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', 'create'])
  }

  toActor(domainId: number, specificationId: number, actorId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId])
  }

  toSharedTestSuite(domainId: number, testSuiteId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'testsuites', testSuiteId])
  }

  toTestSuite(domainId: number, specificationId: number, testSuiteId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId])
  }

  toSharedTestCase(domainId: number, testSuiteId: number, testCaseId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'testsuites', testSuiteId, 'testcases', testCaseId])
  }

  toTestCase(domainId: number, specificationId: number, testSuiteId: number, testCaseId: number) {
    return this.navigate(MenuItem.domainManagement, ['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId, 'testcases', testCaseId])
  }

  toCreateCommunity() {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', 'create'])
  }

  toCommunity(communityId: number, tab?: CommunityTab) {
    if (tab != undefined) {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId], { state: { tab: CommunityTab[tab] } })
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId])
    }
  }

  toCommunityParameters(communityId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'parameters'])
  }

  toCommunityLabels(communityId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'labels'])
  }

  toCommunityCertificateSettings(communityId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'certificate'])
  }

  toCreateCommunityAdmin(communityId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'admin', 'create'])
  }

  toCommunityAdmin(communityId: number, adminId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'admin', adminId])
  }

  toCreateTrigger(communityId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'triggers', 'create'])
  }

  toTrigger(communityId: number, triggerId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'triggers', triggerId])
  }

  toUserManagement() {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users'])
  }

  toCreateErrorTemplate(communityId: number, addCopyTestBedDefault: boolean, copySource?: number) {
    this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'errortemplates', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
  }

  toErrorTemplate(communityId: number, templateId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'errortemplates', templateId])
  }

  toCreateLegalNotice(communityId: number, addCopyTestBedDefault: boolean, copySource?: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'notices', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
  }

  toLegalNotice(communityId: number, noticeId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'notices', noticeId])
  }

  toCreateLandingPage(communityId: number, addCopyTestBedDefault: boolean, copySource?: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'pages', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
  }

  toLandingPage(communityId: number, pageId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'pages', pageId])
  }

  toCreateOwnOrganisationUser() {
    return this.navigate(MenuItem.myOrganisation, ['settings', 'organisation', 'user', 'create'])
  }

  toCreateOrganisationUser(communityId: number, organisationId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'user', 'create'])
  }

  toOwnOrganisationUser(userId: number) {
    return this.navigate(MenuItem.myOrganisation, ['settings', 'organisation', 'user', userId])
  }

  toOrganisationUser(communityId: number, organisationId: number, userId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'user', userId])
  }

  toSessionDashboard(sessionIdToShow?: string) {
    if (sessionIdToShow != undefined) {
      return this.navigate(MenuItem.sessionDashboard, ['admin', 'sessions'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID, sessionIdToShow) })
    } else {
      return this.navigate(MenuItem.sessionDashboard, ['admin', 'sessions'])
    }
  }

  toConformanceDashboard() {
    return this.navigate(MenuItem.conformanceDashboard, ['admin', 'conformance'])
  }

  toDataImport() {
    return this.navigate(MenuItem.dataImport, ['admin', 'import'])
  }

  toDataExport() {
    return this.navigate(MenuItem.dataExport, ['admin', 'export'])
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
        extras.queryParams[Constants.NAVIGATION_QUERY_PARAM.COPY_DEFAULT] = true
      }
      if (copySourceId != undefined) {
        extras.queryParams[Constants.NAVIGATION_QUERY_PARAM.COPY] = copySourceId
      }
    }
    return extras
  }  

  private createQueryParams(name: string, value: any) {
    let params: {[key: string]: any} = {}
    params[name] = value
    return params
  }

  private navigate(menuItem: MenuItem, commands: any[], extras?: NavigationExtras|undefined) {
    this.dataService.changePage({ menuItem: menuItem })
    this.navigationMethodExecuted = true    
    return this.router.navigate(commands, extras)
  }

}
