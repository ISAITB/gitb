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
import {NavigationEnd, NavigationExtras, NavigationStart, Router} from '@angular/router';
import {CommunityTab} from '../pages/admin/user-management/community/community-details/community-tab.enum';
import {ConformanceStatementTab} from '../pages/organisation/conformance-statement/conformance-statement-tab';
import {OrganisationTab} from '../pages/admin/user-management/organisation/organisation-details/OrganisationTab';
import {Constants} from '../common/constants';
import {DataService} from './data.service';
import {MenuItem} from '../types/menu-item.enum';
import {SystemAdministrationTab} from '../pages/admin/system-administration/system-administration-tab.enum';
import {BreadcrumbItem} from '../components/breadcrumb/breadcrumb-item';
import {BreadcrumbType} from '../types/breadcrumb-type';

@Injectable({
  providedIn: 'root'
})
export class RoutingService {

  constructor(
    private router: Router,
    private dataService: DataService
  ) {
    this.initialise()
  }

  private initialise() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        // For all navigation except to the login page record it as the last location.
        if (!event.url.startsWith('/login')) {
          this.dataService.recordLocationData(event.url)
        }
        // Clear cookies to be defined by resolvers.
        this.dataService.clearImplicitCommunity()
      } else if (event instanceof NavigationEnd) {
        /*
         * We only need to do this matching if we are coming to a page without going through
         * one of the navigation methods (e.g. after a refresh). In other cases we skip this as
         * we always know what menu item applies through the navigate() method.
         */
        setTimeout(() => {
          this.changePageForURL(event.url)
        }, 1)
      }
    })
  }

  private changePageForURL(url: string) {
    this.dataService.showNavigationControls = true
    if (url.startsWith('/home')) {
      this.dataService.changePage({ menuItem: MenuItem.home })
    } else if (url.startsWith('/login')) {
      this.dataService.showNavigationControls = false
      this.dataService.changePage({ menuItem: MenuItem.login })
    } else if (url.startsWith('/settings/profile')) {
      this.dataService.changePage({ menuItem: MenuItem.myProfile })
    } else if (url.startsWith('/settings/organisation')) {
      this.dataService.changePage({ menuItem: MenuItem.myOrganisation })
    } else if (url.startsWith('/settings/password')) {
      this.dataService.changePage({ menuItem: MenuItem.changePassword })
    } else if (url.startsWith('/admin/sessions')) {
      this.dataService.changePage({ menuItem: MenuItem.sessionDashboard })
    } else if (url.startsWith('/admin/health')) {
      this.dataService.changePage({ menuItem: MenuItem.serviceHealthDashboard })
    } else if (url.startsWith('/admin/conformance')) {
      this.dataService.changePage({ menuItem: MenuItem.conformanceDashboard })
    } else if (url.startsWith('/admin/conformance')) {
      this.dataService.changePage({ menuItem: MenuItem.conformanceDashboard })
    } else if (url.startsWith('/admin/domains')) {
      this.dataService.changePage({ menuItem: MenuItem.domainManagement })
    } else if (url.startsWith('/admin/users')) {
      this.dataService.changePage({ menuItem: MenuItem.communityManagement })
    } else if (url.startsWith('/admin/export')) {
      this.dataService.changePage({ menuItem: MenuItem.dataExport })
    } else if (url.startsWith('/admin/import')) {
      this.dataService.changePage({ menuItem: MenuItem.dataImport })
    } else if (url.startsWith('/admin/system')) {
      this.dataService.changePage({ menuItem: MenuItem.systemAdministration })
    } else if (url.startsWith('/organisation/conformance')) {
      this.dataService.changePage({ menuItem: MenuItem.myConformanceStatements })
    } else if (url.startsWith('/organisation/tests')) {
      this.dataService.changePage({ menuItem: MenuItem.myTestSessions })
    } else if (url.startsWith('/organisation/test')) {
      this.dataService.changePage({ menuItem: MenuItem.myConformanceStatements })
    } else if (url.startsWith('/organisation')) {
      this.dataService.changePage({ menuItem: MenuItem.myOrganisation })
    } else if (url.startsWith('/community/sessions')) {
      this.dataService.changePage({ menuItem: MenuItem.communitySessionDashboard })
    }
  }

  toURL(url: string) {
    this.changePageForURL(url)
    return this.router.navigateByUrl(url).catch((error) => {
      console.error("Unable to restore view at: "+url, error.stack)
      return this.toHome()
    })
  }

  toHome() {
    return this.navigate(MenuItem.home, ['home'])
  }

  toStartPage(userId: number) {
    const previousLocation = this.dataService.retrieveLocationData(userId)
    if (previousLocation) {
      return this.toURL(previousLocation)
    } else {
      return this.toHome()
    }
  }

  toLogin() {
    return this.navigate(MenuItem.login, ['login'])
  }

  toCreateTestBedAdmin() {
    return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'admin', 'create'])
  }

  toTestBedAdmin(adminId: number) {
    return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'admin', adminId])
  }

  toTestHistory(organisationId: number, sessionIdToShow?: string) {
    if (sessionIdToShow != undefined) {
      return this.navigate(MenuItem.myTestSessions, ['organisation', 'tests', organisationId], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID, sessionIdToShow) })
    } else {
      return this.navigate(MenuItem.myTestSessions, ['organisation', 'tests', organisationId])
    }
  }

  toCreateConformanceStatement(organisationId: number, systemId: number, communityId?: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.myOrganisation, ['organisation', 'conformance', organisationId, 'system', systemId, 'create'])
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'create'])
    }
  }

  toOwnConformanceStatement(organisationId: number, systemId: number, actorId: number, snapshotId?: number, snapshotLabel?: string, tab?: ConformanceStatementTab) {
    const pathParts = ['organisation', 'conformance', organisationId, 'system', systemId, 'actor', actorId]
    if (snapshotId != undefined) {
      pathParts.push('snapshot', snapshotId)
    }
    return this.navigate(MenuItem.myConformanceStatements, pathParts, this.addConformanceStatementExtras(tab, snapshotLabel))
  }

  toConformanceStatement(organisationId: number, systemId: number, actorId: number, communityId: number, snapshotId?: number, snapshotLabel?: string, tab?: ConformanceStatementTab) {
    const pathParts = ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'actor', actorId]
    if (snapshotId != undefined) {
      pathParts.push('snapshot', snapshotId)
    }
    return this.navigate(MenuItem.communityManagement, pathParts, this.addConformanceStatementExtras(tab, snapshotLabel))
  }

  private addConformanceStatementExtras(tab?: ConformanceStatementTab, snapshotLabel?: string) {
    let extras: NavigationExtras|undefined = undefined
    if (tab != undefined || snapshotLabel != undefined) {
      extras = {}
      extras.state = {}
      if (tab != undefined) {
        extras.state[Constants.NAVIGATION_PATH_PARAM.TAB] = ConformanceStatementTab[tab]
      }
      if (snapshotLabel != undefined) {
        extras.state[Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_LABEL] = snapshotLabel
      }
    }
    return extras
  }

  toConformanceStatements(communityId: number, organisationId: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean) {
    // The replaceUrl flag causes the route path to be loaded but reusing the current controller (i.e. only the path gets updated), to retain state after refresh.
    if (systemId != undefined) {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'], {
        queryParams: this.createMultipleQueryParams([
          {name: Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, value: systemId},
          {name: Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, value: snapshotId}
        ]),
        replaceUrl: replaceUrl
      })
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'], {
        queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, snapshotId),
        replaceUrl: replaceUrl
      })
    }
  }

  toOwnConformanceStatements(organisationId: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean) {
    // The replaceUrl flag causes the route path to be loaded but reusing the current controller (i.e. only the path gets updated), to retain state after refresh.
    if (systemId != undefined) {
      return this.navigate(MenuItem.myConformanceStatements, ['organisation', 'conformance', organisationId], {
        queryParams: this.createMultipleQueryParams([
          {name: Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, value: systemId},
          {name: Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, value: snapshotId}
        ]),
        replaceUrl: replaceUrl
      }
    )
    } else {
      return this.navigate(MenuItem.myConformanceStatements, ['organisation', 'conformance', organisationId], {
        queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, snapshotId),
        replaceUrl: replaceUrl
      })
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
    return this.navigate(MenuItem.myConformanceStatements, ['organisation', 'test', organisationId, systemId, actorId, 'execute'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID, testCaseId)})
  }

  toTestSuiteExecution(communityId: number, organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SUITE_ID, testSuiteId)})
  }

  toOwnTestSuiteExecution(organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.navigate(MenuItem.myConformanceStatements, ['organisation', 'test', organisationId, systemId, actorId, 'execute'], { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SUITE_ID, testSuiteId)})
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

  toCommunityReportSettings(communityId: number) {
    return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'reports'])
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

  toCreateErrorTemplate(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'errortemplates', 'create'], this.addCommunityContentExtras(undefined, copySource))
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'errortemplates', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
    }
  }

  toErrorTemplate(communityId: number|undefined, templateId: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'errortemplates', templateId])
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'errortemplates', templateId])
    }
  }

  toCreateLegalNotice(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'notices', 'create'], this.addCommunityContentExtras(undefined, copySource))
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'notices', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
    }
  }

  toLegalNotice(communityId: number|undefined, noticeId: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'notices', noticeId])
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'notices', noticeId])
    }
  }

  toCreateLandingPage(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'pages', 'create'], this.addCommunityContentExtras(undefined, copySource))
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'pages', 'create'], this.addCommunityContentExtras(addCopyTestBedDefault, copySource))
    }
  }

  toLandingPage(communityId: number|undefined, pageId: number) {
    if (communityId == undefined) {
      return this.navigate(MenuItem.systemAdministration, ['admin', 'system', 'pages', pageId])
    } else {
      return this.navigate(MenuItem.communityManagement, ['admin', 'users', 'community', communityId, 'pages', pageId])
    }
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

  toCommunitySessionDashboard() {
    return this.navigate(MenuItem.communitySessionDashboard, ['community', 'sessions'])
  }

  toServiceHealthDashboard() {
    return this.navigate(MenuItem.serviceHealthDashboard, ['admin', 'health'])
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

  toSystemAdministration(tab?: SystemAdministrationTab) {
    if (tab != undefined) {
      return this.navigate(MenuItem.systemAdministration, [ 'admin', 'system' ], { state: { tab: SystemAdministrationTab[tab] } })
    } else {
      return this.navigate(MenuItem.systemAdministration, [ 'admin', 'system' ])
    }
  }

  toCreateTheme(referenceThemeId: number) {
    return this.navigate(MenuItem.systemAdministration, [ 'admin', 'system', 'themes', 'create', referenceThemeId ])
  }

  toTheme(themeId: number) {
    return this.navigate(MenuItem.systemAdministration, [ 'admin', 'system', 'themes', themeId ])
  }

  private addTabExtras(tabIndex?: number) {
    let extras: NavigationExtras|undefined = undefined
    if (tabIndex != undefined) {
      extras = { state: { tab: tabIndex }}
    }
    return extras
  }

  private addCommunityContentExtras(copyTestBedDefault: boolean|undefined, copySourceId?: number) {
    let extras: NavigationExtras|undefined = undefined
    if ((copyTestBedDefault != undefined && copyTestBedDefault) || copySourceId != undefined) {
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
    return this.createMultipleQueryParams([{name: name, value: value}])
  }

  private createMultipleQueryParams(parameters: {name: string, value: any}[]) {
    let params: {[key: string]: any} = {}
    for (let p of parameters) {
      if (p.value !== undefined) {
        params[p.name] = p.value
      }
    }
    return params
  }

  private navigate(menuItem: MenuItem, commands: any[], extras?: NavigationExtras|undefined) {
    return this.router.navigate(commands, extras)
  }

  domainBreadcrumbs(domainId: number, label?: string): BreadcrumbItem[] {
    let crumbs: BreadcrumbItem[]
    if (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.vendor?.community == undefined)) {
      crumbs = this.domainsBreadcrumbs(true)
    } else {
      crumbs = []
    }
    crumbs.push({ type: BreadcrumbType.domain, typeId: domainId, label: label, action: (() => this.toDomain(domainId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  specificationBreadcrumbs(domainId: number, specificationId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.domainBreadcrumbs(domainId)
    crumbs.push({ type: BreadcrumbType.specification, typeId: specificationId, label: label, action: (() => this.toSpecification(domainId, specificationId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  specificationGroupBreadcrumbs(domainId: number, groupId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.domainBreadcrumbs(domainId)
    crumbs.push({ type: BreadcrumbType.specificationGroup, typeId: groupId, label: label, action: (() => this.toSpecificationGroup(domainId, groupId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  actorBreadcrumbs(domainId: number, specificationId: number, actorId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.specificationBreadcrumbs(domainId, specificationId)
    crumbs.push({ type: BreadcrumbType.actor, typeId: actorId, label: label, action: (() => this.toActor(domainId, specificationId, actorId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  endpointBreadcrumbs(domainId: number, specificationId: number, actorId: number, endpointId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.actorBreadcrumbs(domainId, specificationId, actorId)
    crumbs.push({ type: BreadcrumbType.endpoint, typeId: endpointId, label: label, action: (() => this.toEndpoint(domainId, specificationId, actorId, endpointId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testSuiteBreadcrumbs(domainId: number, specificationId: number, testSuiteId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.specificationBreadcrumbs(domainId, specificationId)
    crumbs.push({ type: BreadcrumbType.testSuite, typeId: testSuiteId, label: label, action: (() => this.toTestSuite(domainId, specificationId, testSuiteId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testCaseBreadcrumbs(domainId: number, specificationId: number, testSuiteId: number, testCaseId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.testSuiteBreadcrumbs(domainId, specificationId, testSuiteId)
    crumbs.push({ type: BreadcrumbType.testCase, typeId: testCaseId, label: label, action: (() => this.toTestCase(domainId, specificationId, testSuiteId, testCaseId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  sharedTestSuiteBreadcrumbs(domainId: number, testSuiteId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.domainBreadcrumbs(domainId)
    crumbs.push({ type: BreadcrumbType.sharedTestSuite, typeId: testSuiteId, label: label, action: (() => this.toSharedTestSuite(domainId, testSuiteId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  sharedTestCaseBreadcrumbs(domainId: number, testSuiteId: number, testCaseId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.sharedTestSuiteBreadcrumbs(domainId, testSuiteId)
    crumbs.push({ type: BreadcrumbType.sharedTestCase, typeId: testCaseId, label: label, action: (() => this.toSharedTestCase(domainId, testSuiteId, testCaseId))})
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityBreadcrumbs(communityId: number, label?: string): BreadcrumbItem[] {
    let crumbs: BreadcrumbItem[] = []
    if (this.dataService.isSystemAdmin) {
      crumbs = this.communitiesBreadcrumbs(true)
    } else {
      crumbs = []
    }
    crumbs.push({ type: BreadcrumbType.community, typeId: communityId, label: label, action: (() => this.toCommunity(communityId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  organisationBreadcrumbs(communityId: number, organisationId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    let action: Function|undefined
    if (organisationId >= 0) {
      action = (() => this.toOrganisationDetails(communityId, organisationId))
    }
    crumbs.push({ type: BreadcrumbType.organisation, typeId: organisationId, label: label, action: action })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemBreadcrumbs(communityId: number, organisationId: number, organisationLabel: string|undefined, systemId: number, label?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = this.organisationBreadcrumbs(communityId, organisationId, organisationLabel)
    let action: Function|undefined
    if (systemId >= 0) {
      action = (() => this.toSystemDetails(communityId, organisationId, systemId))
    }
    crumbs.push({ type: BreadcrumbType.system, typeId: systemId, label: label, action: action })
    if (label && !skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityAdminBreadcrumbs(communityId: number, adminId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityAdmin, typeId: adminId, label: label, action: (() => this.toCommunityAdmin(communityId, adminId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  organisationUserBreadcrumbs(communityId: number, organisationId: number, userId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.organisationBreadcrumbs(communityId, organisationId)
    crumbs.push({ type: BreadcrumbType.organisationUser, typeId: userId, label: label, action: (() => this.toOrganisationUser(communityId, organisationId, userId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  landingPageBreadcrumbs(communityId: number, pageId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.landingPage, typeId: pageId, label: label, action: (() => this.toLandingPage(communityId, pageId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  errorTemplateBreadcrumbs(communityId: number, templateId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.errorTemplate, typeId: templateId, label: label, action: (() => this.toErrorTemplate(communityId, templateId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  legalNoticeBreadcrumbs(communityId: number, noticeId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.legalNotice, typeId: noticeId, label: label, action: (() => this.toLegalNotice(communityId, noticeId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  triggerBreadcrumbs(communityId: number, triggerId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.trigger, typeId: triggerId, label: label, action: (() => this.toTrigger(communityId, triggerId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  conformanceStatementsBreadcrumbs(communityId: number, organisationId: number, organisationLabel?: string, systemId?: number, systemLabel?: string, snapshotId?:number, snapshotLabel?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    let crumbs: BreadcrumbItem[]
    if (systemId == undefined) {
      crumbs = this.organisationBreadcrumbs(communityId, organisationId, organisationLabel)
    } else {
      crumbs = this.systemBreadcrumbs(communityId, organisationId, organisationLabel, systemId, systemLabel, true)
    }
    crumbs.push({ type: BreadcrumbType.statements, label: 'Conformance statements', action: (() => this.toConformanceStatements(communityId, organisationId, systemId, snapshotId)) })
    if (snapshotLabel) {
      crumbs.push({ type: BreadcrumbType.conformanceSnapshot, label: snapshotLabel })
    }
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  conformanceStatementBreadcrumbs(organisationId: number, systemId: number, actorId: number, communityId: number|undefined, statementLabel: string, organisationLabel?: string, systemLabel?: string, snapshotId?: number, snapshotLabel?: string): BreadcrumbItem[] {
    let crumbs: BreadcrumbItem[]
    if (communityId) {
      crumbs = this.conformanceStatementsBreadcrumbs(communityId, organisationId, organisationLabel, systemId, systemLabel, snapshotId, snapshotLabel, true)
      crumbs.push({ type: BreadcrumbType.statement, typeId: systemId+'|'+actorId, label: statementLabel, action: (() => this.toConformanceStatement(organisationId, systemId, actorId, communityId, snapshotId, snapshotLabel)) })
    } else {
      crumbs = this.ownConformanceStatementsBreadcrumbs(organisationId, systemId, systemLabel, snapshotId, snapshotLabel, true)
      crumbs.push({ type: BreadcrumbType.statement, typeId: systemId+'|'+actorId, label: statementLabel, action: (() => this.toOwnConformanceStatement(organisationId, systemId, actorId, snapshotId, snapshotLabel)) })
    }
    if (statementLabel) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  ownConformanceStatementsBreadcrumbs(organisationId: number, systemId?: number, systemLabel?: string, snapshotId?: number, snapshotLabel?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    let crumbs: BreadcrumbItem[]
    if (systemId == undefined) {
      crumbs = this.ownOrganisationBreadcrumbs()
    } else {
      crumbs = this.ownSystemBreadcrumbs(systemId, systemLabel, true)
    }
    crumbs.push({ type: BreadcrumbType.ownStatements, label: 'Conformance statements', action: (() => this.toOwnConformanceStatements(organisationId, systemId, snapshotId)) })
    if (snapshotLabel) {
      crumbs.push({ type: BreadcrumbType.conformanceSnapshot, label: snapshotLabel })
    }
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  ownOrganisationBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.ownOrganisation, label: 'My '+this.dataService.labelOrganisationLower(), action: (() => this.toOwnOrganisationDetails()) }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  ownSystemBreadcrumbs(systemId: number, label?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = this.ownOrganisationBreadcrumbs(true)
    let action: Function|undefined
    if (systemId >= 0) {
      action = (() => this.toOwnSystemDetails(systemId))
    }
    crumbs.push({ type: BreadcrumbType.ownSystem, typeId: systemId, label: label, action: action })
    if (label && !skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testHistoryBreadcrumbs(organisationId: number): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.ownTestHistory, label: 'My test sessions', action: (() => this.toTestHistory(organisationId)) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  conformanceDashboardBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.conformanceDashboard, label: 'Conformance dashboard', action: (() => this.toConformanceDashboard()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  sessionDashboardBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.sessionDashboard, label: 'Session dashboard', action: (() => this.toSessionDashboard()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  serviceHealthDashboardBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.serviceHealthDashboard, label: 'Service health', action: (() => this.toServiceHealthDashboard()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communitySessionsBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.communitySessions, label: 'Community test sessions', action: (() => this.toCommunitySessionDashboard()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  exportBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.export, label: 'Data export', action: (() => this.toDataExport()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  importBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.import, label: 'Data import', action: (() => this.toDataImport()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  domainsBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.domains, label: 'Domains', action: (() => this.toDomains()) }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communitiesBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.communities, label: 'Communities', action: (() => this.toUserManagement()) }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemConfigurationBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.systemConfiguration, label: 'System administration', action: (() => this.toSystemAdministration()) }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testBedAdminBreadcrumbs(adminId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemAdmin, typeId: adminId, label: label, action: (() => this.toTestBedAdmin(adminId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemLandingPageBreadcrumbs(pageId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemLandingPage, typeId: pageId, label: label, action: (() => this.toLandingPage(undefined, pageId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemLegalNoticeBreadcrumbs(noticeId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemLegalNotice, typeId: noticeId, label: label, action: (() => this.toLegalNotice(undefined, noticeId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemErrorTemplateBreadcrumbs(templateId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemErrorTemplate, typeId: templateId, label: label, action: (() => this.toErrorTemplate(undefined, templateId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemThemeBreadcrumbs(themeId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.theme, typeId: themeId, label: label, action: (() => this.toTheme(themeId)) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityLabelsBreadcrumbs(communityId: number): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityLabels, label: 'Labels', action: (() => this.toCommunityLabels(communityId)) })
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityParametersBreadcrumbs(communityId: number): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityParameters, label: 'Properties', action: (() => this.toCommunityParameters(communityId)) })
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityReportSettingsBreadcrumbs(communityId: number): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityParameters, label: 'Report settings', action: (() => this.toCommunityReportSettings(communityId)) })
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  profileBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.profile, label: 'Profile', action: (() => this.toProfile()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  changePasswordBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.changePassword, label: 'Change password', action: (() => this.toChangePassword()) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

}
