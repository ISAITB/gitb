/*
 * Copyright (C) 2026 European Union
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
import {NavigationEnd, NavigationExtras, NavigationStart, Router, UrlTree} from '@angular/router';
import {Constants} from '../common/constants';
import {DataService} from './data.service';
import {MenuItem} from '../types/menu-item.enum';
import {BreadcrumbItem} from '../components/breadcrumb/breadcrumb-item';
import {BreadcrumbType} from '../types/breadcrumb-type';
import {NavigationTarget} from '../types/navigation-target';

@Injectable({
  providedIn: 'root'
})
export class RoutingService {

  constructor(
    private readonly router: Router,
    private readonly dataService: DataService
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
    } else if (url.startsWith('/admin/domains')) {
      this.dataService.changePage({ menuItem: MenuItem.domainManagement })
    } else if (url.startsWith('/admin/users')) {
      this.dataService.changePage({ menuItem: MenuItem.communityManagement })
    } else if (url.startsWith('/admin/data')) {
      this.dataService.changePage({ menuItem: MenuItem.dataManagement })
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

  toURL(url: string, extras?: NavigationExtras): Promise<boolean> {
    this.changePageForURL(url)
    return this.router.navigateByUrl(url, extras).catch((error) => {
      console.error("Unable to restore view at: "+url, error.stack)
      return this.toStartPage()
    })
  }

  /**
   * Records the current URL as the "return target" for a subsequent "View XYZ" navigation, so that
   * the target screen's Back/Cancel control can return here (via returnToSource()) instead of
   * following its default hierarchical navigation.
   */
  recordViewReturnTarget() {
    if (sessionStorage) {
      sessionStorage.setItem(Constants.SESSION_DATA.VIEW_RETURN, this.router.url)
    }
  }

  /**
   * Consumes (reads and clears) a previously recorded "View XYZ" return target. Meant to be called
   * once, on arrival at the target screen (typically in ngOnInit, caching the result on the
   * component), so the record only ever applies to this single navigation hop - a later Back click,
   * or navigating away some other way (e.g. via the menu), can never pick up a stale record left over
   * from an earlier, unrelated "View XYZ" action.
   */
  consumeViewReturnTarget(): string|undefined {
    if (sessionStorage) {
      const target = sessionStorage.getItem(Constants.SESSION_DATA.VIEW_RETURN)
      if (target != undefined) {
        sessionStorage.removeItem(Constants.SESSION_DATA.VIEW_RETURN)
        return target
      }
    }
    return undefined
  }

  /**
   * Used by a target screen's Back/Cancel control together with a target previously obtained via
   * consumeViewReturnTarget() (cached on the component from ngOnInit). Navigates back there if set,
   * flagging the navigation (via transient router state) to let the destination restore its own
   * saved display state, if any. Otherwise falls back to the provided default navigation.
   */
  returnToSource(target: string|undefined, defaultNavigation: () => void) {
    if (target != undefined) {
      this.toURL(target, { state: { restore: true } })
    } else {
      defaultNavigation()
    }
  }

  linkToHome(): NavigationTarget {
    return { commands: ['home'] }
  }
  toHome() {
    return this.navigate(this.linkToHome())
  }

  toStartPage(userId?: number): Promise<boolean> {
    if (userId != undefined) {
      const previousLocation = this.dataService.retrieveLocationData(userId)
      if (previousLocation) {
        return this.toURL(previousLocation)
      }
    }
    return this.router.navigate(['start'])
  }

  resolveStartPage(): UrlTree {
    if (this.dataService.homePageType === Constants.HOME_PAGE_TYPE.CONFORMANCE_DASHBOARD) {
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        return this.router.createUrlTree(['admin', 'conformance'])
      } else {
        return this.router.createUrlTree(['organisation', 'conformance', this.dataService.vendor?.id!])
      }
    } else {
      return this.router.createUrlTree(['home'])
    }
  }

  toLogin() {
    return this.navigate({ commands: ['login'] })
  }

  linkToCreateTestBedAdmin(): NavigationTarget {
    return { commands: ['admin', 'system', 'admin', 'create'] }
  }
  toCreateTestBedAdmin() {
    return this.navigate(this.linkToCreateTestBedAdmin())
  }

  linkToTestBedAdmin(adminId: number): NavigationTarget {
    return { commands: ['admin', 'system', 'admin', adminId] }
  }
  toTestBedAdmin(adminId: number) {
    return this.navigate(this.linkToTestBedAdmin(adminId))
  }

  linkToTestHistory(organisationId: number, sessionIdToShow?: string, systemToShow?: number, testCaseToShow?: number): NavigationTarget {
    if (sessionIdToShow != undefined || systemToShow != undefined || testCaseToShow != undefined) {
      return { commands: ['organisation', 'tests', organisationId], extras: {
        queryParams: this.createMultipleQueryParams([
          { name: Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID, value: sessionIdToShow },
          { name: Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, value: systemToShow },
          { name: Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID, value: testCaseToShow }
        ])
      }}
    } else {
      return { commands: ['organisation', 'tests', organisationId] }
    }
  }
  toTestHistory(organisationId: number, sessionIdToShow?: string, systemToShow?: number, testCaseToShow?: number) {
    return this.navigate(this.linkToTestHistory(organisationId, sessionIdToShow, systemToShow, testCaseToShow))
  }

  linkToCreateConformanceStatement(organisationId: number, systemId: number, communityId?: number): NavigationTarget {
    if (communityId == undefined) {
      return { commands: ['organisation', 'conformance', organisationId, 'system', systemId, 'create'] }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'create'] }
    }
  }
  toCreateConformanceStatement(organisationId: number, systemId: number, communityId?: number) {
    return this.navigate(this.linkToCreateConformanceStatement(organisationId, systemId, communityId))
  }

  linkToOwnConformanceStatement(organisationId: number, systemId: number, actorId: number, snapshotId?: number, snapshotLabel?: string, tab?: number): NavigationTarget {
    const pathParts = ['organisation', 'conformance', organisationId, 'system', systemId, 'actor', actorId]
    if (snapshotId != undefined) {
      pathParts.push('snapshot', snapshotId)
    }
    return { commands: pathParts, extras: this.addConformanceStatementExtras(tab, snapshotLabel) }
  }
  toOwnConformanceStatement(organisationId: number, systemId: number, actorId: number, snapshotId?: number, snapshotLabel?: string, tab?: number) {
    return this.navigate(this.linkToOwnConformanceStatement(organisationId, systemId, actorId, snapshotId, snapshotLabel, tab))
  }

  linkToConformanceStatement(organisationId: number, systemId: number, actorId: number, communityId: number, snapshotId?: number, snapshotLabel?: string, tab?: number): NavigationTarget {
    const pathParts = ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance', 'system', systemId, 'actor', actorId]
    if (snapshotId != undefined) {
      pathParts.push('snapshot', snapshotId)
    }
    return { commands: pathParts, extras: this.addConformanceStatementExtras(tab, snapshotLabel) }
  }
  toConformanceStatement(organisationId: number, systemId: number, actorId: number, communityId: number, snapshotId?: number, snapshotLabel?: string, tab?: number) {
    return this.navigate(this.linkToConformanceStatement(organisationId, systemId, actorId, communityId, snapshotId, snapshotLabel, tab))
  }

  private addConformanceStatementExtras(tab?: number, snapshotLabel?: string) {
    let extras: NavigationExtras|undefined = this.addTabExtras(tab)
    if (snapshotLabel != undefined) {
      if (extras == undefined) {
        extras = {}
        if (extras.state == undefined) {
          extras.state = {}
        }
      }
      if (extras.state != undefined) {
        extras.state[Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_LABEL] = snapshotLabel
      }
    }
    return extras
  }

  linkToConformanceStatements(communityId: number, organisationId: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean): NavigationTarget {
    // The replaceUrl flag causes the route path to be loaded but reusing the current controller (i.e. only the path gets updated), to retain state after refresh.
    if (systemId != undefined) {
      return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'], extras: {
        queryParams: this.createMultipleQueryParams([
          {name: Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, value: systemId},
          {name: Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, value: snapshotId}
        ]),
        replaceUrl: replaceUrl
      }}
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'conformance'], extras: {
        queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, snapshotId),
        replaceUrl: replaceUrl
      }}
    }
  }
  toConformanceStatements(communityId: number, organisationId: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean) {
    return this.navigate(this.linkToConformanceStatements(communityId, organisationId, systemId, snapshotId, replaceUrl))
  }

  linkToOwnConformanceStatements(organisationId: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean): NavigationTarget {
    // The replaceUrl flag causes the route path to be loaded but reusing the current controller (i.e. only the path gets updated), to retain state after refresh.
    if (systemId != undefined) {
      return { commands: ['organisation', 'conformance', organisationId], extras: {
        queryParams: this.createMultipleQueryParams([
          {name: Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, value: systemId},
          {name: Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, value: snapshotId}
        ]),
        replaceUrl: replaceUrl
      }}
    } else {
      return { commands: ['organisation', 'conformance', organisationId], extras: {
        queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, snapshotId),
        replaceUrl: replaceUrl
      }}
    }
  }
  toOwnConformanceStatements(organisationId: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean) {
    return this.navigate(this.linkToOwnConformanceStatements(organisationId, systemId, snapshotId, replaceUrl))
  }

  linkToCreateOrganisation(communityId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'organisation', 'create'] }
  }
  toCreateOrganisation(communityId: number) {
    return this.navigate(this.linkToCreateOrganisation(communityId))
  }

  linkToOwnOrganisationDetails(tab?: number, viewProperties?: boolean): NavigationTarget {
    const navigationPaths = ['settings', 'organisation']
    if (viewProperties == true) {
      if (tab != undefined) {
        const extras = this.addTabExtras(tab)!
        extras.queryParams = this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true)
        return { commands: navigationPaths, extras: extras }
      } else {
        return { commands: navigationPaths, extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) } }
      }
    } else {
      if (tab != undefined) {
        return { commands: navigationPaths, extras: this.addTabExtras(tab) }
      } else {
        return { commands: navigationPaths }
      }
    }
  }
  toOwnOrganisationDetails(tab?: number, viewProperties?: boolean) {
    return this.navigate(this.linkToOwnOrganisationDetails(tab, viewProperties))
  }

  linkToOrganisationDetails(communityId: number, organisationId: number, tab?: number, viewProperties?: boolean): NavigationTarget {
    let navigationPaths = ['admin', 'users', 'community', communityId, 'organisation', organisationId]
    if (viewProperties == true) {
      if (tab != undefined) {
        const extras = this.addTabExtras(tab)!
        extras.queryParams = this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true)
        return { commands: navigationPaths, extras: extras }
      } else {
        return { commands: navigationPaths, extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) } }
      }
    } else {
      if (tab != undefined) {
        return { commands: navigationPaths, extras: this.addTabExtras(tab) }
      } else {
        return { commands: navigationPaths }
      }
    }
  }
  toOrganisationDetails(communityId: number, organisationId: number, tab?: number, viewProperties?: boolean) {
    return this.navigate(this.linkToOrganisationDetails(communityId, organisationId, tab, viewProperties))
  }

  linkToCreateOwnSystem(): NavigationTarget {
    return { commands: ['settings', 'organisation', 'system', 'create'] }
  }
  toCreateOwnSystem() {
    return this.navigate(this.linkToCreateOwnSystem())
  }

  linkToCreateSystem(communityId: number, organisationId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', 'create'] }
  }
  toCreateSystem(communityId: number, organisationId: number) {
    return this.navigate(this.linkToCreateSystem(communityId, organisationId))
  }

  linkToOwnSystemDetails(systemId: number, viewProperties?: boolean): NavigationTarget {
    if (viewProperties == true) {
      return { commands: ['settings', 'organisation', 'system', systemId], extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) } }
    } else {
      return { commands: ['settings', 'organisation', 'system', systemId] }
    }
  }
  toOwnSystemDetails(systemId: number, viewProperties?: boolean) {
    return this.navigate(this.linkToOwnSystemDetails(systemId, viewProperties))
  }

  linkToSystemDetails(communityId: number, organisationId: number, systemId: number, viewProperties?: boolean): NavigationTarget {
    if (viewProperties == true) {
      return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', systemId], extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES, true) } }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'system', systemId] }
    }
  }
  toSystemDetails(communityId: number, organisationId: number, systemId: number, viewProperties?: boolean) {
    return this.navigate(this.linkToSystemDetails(communityId, organisationId, systemId, viewProperties))
  }

  linkToProfile(): NavigationTarget {
    return { commands: ['settings', 'profile'] }
  }
  toProfile() {
    return this.navigate(this.linkToProfile())
  }

  linkToChangePassword(): NavigationTarget {
    return { commands: ['settings', 'password'] }
  }
  toChangePassword() {
    return this.navigate(this.linkToChangePassword())
  }

  toTestCaseExecution(communityId: number, organisationId: number, systemId: number, actorId: number, testCaseId: number) {
    return this.navigate({ commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'], extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID, testCaseId) } })
  }

  toOwnTestCaseExecution(organisationId: number, systemId: number, actorId: number, testCaseId: number) {
    return this.navigate({ commands: ['organisation', 'test', organisationId, systemId, actorId, 'execute'], extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID, testCaseId)} })
  }

  toTestSuiteExecution(communityId: number, organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.navigate({ commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'], extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SUITE_ID, testSuiteId)} })
  }

  toOwnTestSuiteExecution(organisationId: number, systemId: number, actorId: number, testSuiteId: number) {
    return this.navigate({ commands: ['organisation', 'test', organisationId, systemId, actorId, 'execute'], extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TEST_SUITE_ID, testSuiteId)} })
  }

  toStatementExecution(communityId: number, organisationId: number, systemId: number, actorId: number) {
    return this.navigate({ commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'test', systemId, actorId, 'execute'] })
  }

  toOwnStatementExecution(organisationId: number, systemId: number, actorId: number) {
    return this.navigate({ commands: ['organisation', 'test', organisationId, systemId, actorId, 'execute'] })
  }

  linkToCreateDomain(): NavigationTarget {
    return { commands: ['admin', 'domains', 'create'] }
  }
  toCreateDomain() {
    return this.navigate(this.linkToCreateDomain())
  }

  linkToDomain(domainId: number, tab?: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId], extras: this.addTabExtras(tab) }
  }
  toDomain(domainId: number, tab?: number) {
    return this.navigate(this.linkToDomain(domainId, tab))
  }

  linkToDomains(): NavigationTarget {
    return { commands: ['admin', 'domains'] }
  }
  toDomains() {
    return this.navigate(this.linkToDomains())
  }

  linkToCreateSpecification(domainId: number, groupId?:number): NavigationTarget {
    if (groupId) {
      return { commands: ['admin', 'domains', domainId, 'specifications', 'create'], extras: { queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.SPECIFICATION_GROUP_ID, groupId) } }
    } else {
      return { commands: ['admin', 'domains', domainId, 'specifications', 'create'] }
    }
  }
  toCreateSpecification(domainId: number, groupId?:number) {
    return this.navigate(this.linkToCreateSpecification(domainId, groupId))
  }

  linkToCreateSpecificationGroup(domainId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', 'groups', 'create'] }
  }
  toCreateSpecificationGroup(domainId: number) {
    return this.navigate(this.linkToCreateSpecificationGroup(domainId))
  }

  linkToSpecification(domainId: number, specificationId: number, tab?: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', specificationId], extras: this.addTabExtras(tab) }
  }
  toSpecification(domainId: number, specificationId: number, tab?: number) {
    return this.navigate(this.linkToSpecification(domainId, specificationId, tab))
  }

  linkToSpecificationGroup(domainId: number, groupId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', 'groups', groupId] }
  }
  toSpecificationGroup(domainId: number, groupId: number) {
    return this.navigate(this.linkToSpecificationGroup(domainId, groupId))
  }

  linkToCreateEndpoint(domainId: number, specificationId: number, actorId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId, 'endpoints', 'create'] }
  }
  toCreateEndpoint(domainId: number, specificationId: number, actorId: number) {
    return this.navigate(this.linkToCreateEndpoint(domainId, specificationId, actorId))
  }

  linkToEndpoint(domainId: number, specificationId: number, actorId: number, endpointId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId, 'endpoints', endpointId] }
  }
  toEndpoint(domainId: number, specificationId: number, actorId: number, endpointId: number) {
    return this.navigate(this.linkToEndpoint(domainId, specificationId, actorId, endpointId))
  }

  linkToCreateActor(domainId: number, specificationId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', 'create'] }
  }
  toCreateActor(domainId: number, specificationId: number) {
    return this.navigate(this.linkToCreateActor(domainId, specificationId))
  }

  linkToActor(domainId: number, specificationId: number, actorId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', specificationId, 'actors', actorId] }
  }
  toActor(domainId: number, specificationId: number, actorId: number) {
    return this.navigate(this.linkToActor(domainId, specificationId, actorId))
  }

  linkToSharedTestSuite(domainId: number, testSuiteId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'testsuites', testSuiteId] }
  }
  toSharedTestSuite(domainId: number, testSuiteId: number) {
    return this.navigate(this.linkToSharedTestSuite(domainId, testSuiteId))
  }

  linkToTestSuite(domainId: number, specificationId: number, testSuiteId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId] }
  }
  toTestSuite(domainId: number, specificationId: number, testSuiteId: number) {
    return this.navigate(this.linkToTestSuite(domainId, specificationId, testSuiteId))
  }

  linkToSharedTestCase(domainId: number, testSuiteId: number, testCaseId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'testsuites', testSuiteId, 'testcases', testCaseId] }
  }
  toSharedTestCase(domainId: number, testSuiteId: number, testCaseId: number) {
    return this.navigate(this.linkToSharedTestCase(domainId, testSuiteId, testCaseId))
  }

  linkToTestCase(domainId: number, specificationId: number, testSuiteId: number, testCaseId: number): NavigationTarget {
    return { commands: ['admin', 'domains', domainId, 'specifications', specificationId, 'testsuites', testSuiteId, 'testcases', testCaseId] }
  }
  toTestCase(domainId: number, specificationId: number, testSuiteId: number, testCaseId: number) {
    return this.navigate(this.linkToTestCase(domainId, specificationId, testSuiteId, testCaseId))
  }

  linkToCreateCommunity(): NavigationTarget {
    return { commands: ['admin', 'users', 'community', 'create'] }
  }
  toCreateCommunity() {
    return this.navigate(this.linkToCreateCommunity())
  }

  linkToCommunity(communityId: number, tab?: number): NavigationTarget {
    if (tab != undefined) {
      return { commands: ['admin', 'users', 'community', communityId], extras: this.addTabExtras(tab) }
    } else {
      return { commands: ['admin', 'users', 'community', communityId] }
    }
  }
  toCommunity(communityId: number, tab?: number) {
    return this.navigate(this.linkToCommunity(communityId, tab))
  }

  linkToCommunityParameters(communityId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'parameters'] }
  }
  toCommunityParameters(communityId: number) {
    return this.navigate(this.linkToCommunityParameters(communityId))
  }

  linkToCommunityLabels(communityId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'labels'] }
  }
  toCommunityLabels(communityId: number) {
    return this.navigate(this.linkToCommunityLabels(communityId))
  }

  linkToCommunityReportSettings(communityId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'reports'] }
  }
  toCommunityReportSettings(communityId: number) {
    return this.navigate(this.linkToCommunityReportSettings(communityId))
  }

  linkToCreateCommunityAdmin(communityId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'admin', 'create'] }
  }
  toCreateCommunityAdmin(communityId: number) {
    return this.navigate(this.linkToCreateCommunityAdmin(communityId))
  }

  linkToCommunityAdmin(communityId: number, adminId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'admin', adminId] }
  }
  toCommunityAdmin(communityId: number, adminId: number) {
    return this.navigate(this.linkToCommunityAdmin(communityId, adminId))
  }

  linkToCreateTrigger(communityId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'triggers', 'create'] }
  }
  toCreateTrigger(communityId: number) {
    return this.navigate(this.linkToCreateTrigger(communityId))
  }

  linkToTrigger(communityId: number, triggerId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'triggers', triggerId] }
  }
  toTrigger(communityId: number, triggerId: number) {
    return this.navigate(this.linkToTrigger(communityId, triggerId))
  }

  linkToUserManagement(): NavigationTarget {
    return { commands: ['admin', 'users'] }
  }
  toUserManagement() {
    return this.navigate(this.linkToUserManagement())
  }

  linkToCreateErrorTemplate(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number): NavigationTarget {
    if (communityId == undefined) {
      return { commands: ['admin', 'system', 'errortemplates', 'create'], extras: this.addCommunityContentExtras(undefined, copySource) }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'errortemplates', 'create'], extras: this.addCommunityContentExtras(addCopyTestBedDefault, copySource) }
    }
  }
  toCreateErrorTemplate(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number) {
    return this.navigate(this.linkToCreateErrorTemplate(communityId, addCopyTestBedDefault, copySource))
  }

  linkToErrorTemplate(communityId: number|undefined, templateId: number): NavigationTarget {
    if (communityId == undefined) {
      return { commands: ['admin', 'system', 'errortemplates', templateId] }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'errortemplates', templateId] }
    }
  }
  toErrorTemplate(communityId: number|undefined, templateId: number) {
    return this.navigate(this.linkToErrorTemplate(communityId, templateId))
  }

  linkToCreateLegalNotice(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number): NavigationTarget {
    if (communityId == undefined) {
      return { commands: ['admin', 'system', 'notices', 'create'], extras: this.addCommunityContentExtras(undefined, copySource) }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'notices', 'create'], extras: this.addCommunityContentExtras(addCopyTestBedDefault, copySource) }
    }
  }
  toCreateLegalNotice(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number) {
    return this.navigate(this.linkToCreateLegalNotice(communityId, addCopyTestBedDefault, copySource))
  }

  linkToLegalNotice(communityId: number|undefined, noticeId: number): NavigationTarget {
    if (communityId == undefined) {
      return { commands: ['admin', 'system', 'notices', noticeId] }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'notices', noticeId] }
    }
  }
  toLegalNotice(communityId: number|undefined, noticeId: number) {
    return this.navigate(this.linkToLegalNotice(communityId, noticeId))
  }

  linkToCreateLandingPage(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number): NavigationTarget {
    if (communityId == undefined) {
      return { commands: ['admin', 'system', 'pages', 'create'], extras: this.addCommunityContentExtras(undefined, copySource) }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'pages', 'create'], extras: this.addCommunityContentExtras(addCopyTestBedDefault, copySource) }
    }
  }
  toCreateLandingPage(communityId?: number, addCopyTestBedDefault?: boolean, copySource?: number) {
    return this.navigate(this.linkToCreateLandingPage(communityId, addCopyTestBedDefault, copySource))
  }

  linkToLandingPage(communityId: number|undefined, pageId: number): NavigationTarget {
    if (communityId == undefined) {
      return { commands: ['admin', 'system', 'pages', pageId] }
    } else {
      return { commands: ['admin', 'users', 'community', communityId, 'pages', pageId] }
    }
  }
  toLandingPage(communityId: number|undefined, pageId: number) {
    return this.navigate(this.linkToLandingPage(communityId, pageId))
  }

  linkToCreateOwnOrganisationUser(): NavigationTarget {
    return { commands: ['settings', 'organisation', 'user', 'create'] }
  }
  toCreateOwnOrganisationUser() {
    return this.navigate(this.linkToCreateOwnOrganisationUser())
  }

  linkToCreateOrganisationUser(communityId: number, organisationId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'user', 'create'] }
  }
  toCreateOrganisationUser(communityId: number, organisationId: number) {
    return this.navigate(this.linkToCreateOrganisationUser(communityId, organisationId))
  }

  linkToOwnOrganisationUser(userId: number): NavigationTarget {
    return { commands: ['settings', 'organisation', 'user', userId] }
  }
  toOwnOrganisationUser(userId: number) {
    return this.navigate(this.linkToOwnOrganisationUser(userId))
  }

  linkToOrganisationUser(communityId: number, organisationId: number, userId: number): NavigationTarget {
    return { commands: ['admin', 'users', 'community', communityId, 'organisation', organisationId, 'user', userId] }
  }
  toOrganisationUser(communityId: number, organisationId: number, userId: number) {
    return this.navigate(this.linkToOrganisationUser(communityId, organisationId, userId))
  }

  linkToSessionDashboard(sessionIdToShow?: string, systemToShow?: number, testCaseToShow?: number): NavigationTarget {
    if (sessionIdToShow != undefined || systemToShow != undefined || testCaseToShow != undefined) {
      return { commands: ['admin', 'sessions'], extras: {
        queryParams: this.createMultipleQueryParams([
          { name: Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID, value: sessionIdToShow },
          { name: Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, value: systemToShow },
          { name: Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID, value: testCaseToShow }
        ])
      }}
    } else {
      return { commands: ['admin', 'sessions'] }
    }
  }
  toSessionDashboard(sessionIdToShow?: string, systemToShow?: number, testCaseToShow?: number) {
    return this.navigate(this.linkToSessionDashboard(sessionIdToShow, systemToShow, testCaseToShow))
  }

  linkToCommunitySessionDashboard(): NavigationTarget {
    return { commands: ['community', 'sessions'] }
  }
  toCommunitySessionDashboard() {
    return this.navigate(this.linkToCommunitySessionDashboard())
  }

  linkToServiceHealthDashboard(): NavigationTarget {
    return { commands: ['admin', 'health'] }
  }
  toServiceHealthDashboard() {
    return this.navigate(this.linkToServiceHealthDashboard())
  }

  linkToConformanceDashboard(communityId?: number, organisationId?: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean): NavigationTarget {
    return { commands: ['admin', 'conformance'], extras: {
      queryParams: this.createMultipleQueryParams([
        {name: Constants.NAVIGATION_QUERY_PARAM.COMMUNITY_ID, value: communityId},
        {name: Constants.NAVIGATION_QUERY_PARAM.ORGANISATION_ID, value: organisationId},
        {name: Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID, value: systemId},
        {name: Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID, value: snapshotId}
      ]),
      replaceUrl: replaceUrl
    }}
  }
  toConformanceDashboard(communityId?: number, organisationId?: number, systemId?: number, snapshotId?: number, replaceUrl?: boolean) {
    return this.navigate(this.linkToConformanceDashboard(communityId, organisationId, systemId, snapshotId, replaceUrl))
  }

  linkToDataManagement(): NavigationTarget {
    return { commands: ['admin', 'data'] }
  }
  toDataManagement() {
    return this.navigate(this.linkToDataManagement())
  }

  linkToSystemAdministration(tab?: number): NavigationTarget {
    if (tab != undefined) {
      return { commands: [ 'admin', 'system' ], extras: this.addTabExtras(tab) }
    } else {
      return { commands: [ 'admin', 'system' ] }
    }
  }
  toSystemAdministration(tab?: number) {
    return this.navigate(this.linkToSystemAdministration(tab))
  }

  linkToCreateTheme(referenceThemeId: number): NavigationTarget {
    return { commands: [ 'admin', 'system', 'themes', 'create', referenceThemeId ] }
  }
  toCreateTheme(referenceThemeId: number) {
    return this.navigate(this.linkToCreateTheme(referenceThemeId))
  }

  linkToTheme(themeId: number): NavigationTarget {
    return { commands: [ 'admin', 'system', 'themes', themeId ] }
  }
  toTheme(themeId: number) {
    return this.navigate(this.linkToTheme(themeId))
  }

  private addTabExtras(tabIndex?: number) {
    let extras: NavigationExtras|undefined = undefined
    if (tabIndex != undefined) {
      extras = {
        queryParams: this.createQueryParams(Constants.NAVIGATION_QUERY_PARAM.TAB, tabIndex),
        state: { tab: tabIndex },
        replaceUrl: true
      }
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

  private navigate(target: NavigationTarget) {
    return this.router.navigate(target.commands, target.extras)
  }

  /**
   * The entity-specific breadcrumb factories below (domainBreadcrumbs, specificationBreadcrumbs,
   * actorBreadcrumbs, communityBreadcrumbs, organisationBreadcrumbs, ...) only publish to the
   * breadcrumb display when called with a known label, since they're also used internally by
   * other factories purely to build an ancestor prefix (where publishing early would cause a
   * flash of a partial/incorrect breadcrumb chain). Pages that don't have that label loaded (e.g. a
   * "Create X" form nested under a named parent) can still show a correct breadcrumb by calling the
   * factory without a label and forcing the publish here - the breadcrumb component resolves the
   * missing label asynchronously itself (see BreadcrumbComponent.updateBreadcrumbs()).
   */
  private publishBreadcrumbs(crumbs: BreadcrumbItem[]): BreadcrumbItem[] {
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  /** For a page nested directly under a domain (e.g. "Create specification") whose own label isn't a variable entity name. */
  domainChildBreadcrumbs(domainId: number): BreadcrumbItem[] {
    return this.publishBreadcrumbs(this.domainBreadcrumbs(domainId))
  }

  /** For a page nested directly under a specification (e.g. "Create actor"). */
  specificationChildBreadcrumbs(domainId: number, specificationId: number): BreadcrumbItem[] {
    return this.publishBreadcrumbs(this.specificationBreadcrumbs(domainId, specificationId))
  }

  /** For a page nested directly under an actor (e.g. "Create endpoint"). */
  actorChildBreadcrumbs(domainId: number, specificationId: number, actorId: number): BreadcrumbItem[] {
    return this.publishBreadcrumbs(this.actorBreadcrumbs(domainId, specificationId, actorId))
  }

  /** For a page nested directly under a community (e.g. "Create organisation", "Create webhook", community-level "Create landing page"/"Create legal notice"/"Create error template", "Create community administrator"). */
  communityChildBreadcrumbs(communityId: number): BreadcrumbItem[] {
    return this.publishBreadcrumbs(this.communityBreadcrumbs(communityId))
  }

  /** For a page nested directly under an organisation (e.g. "Create system", "Create user" from community management). */
  organisationChildBreadcrumbs(communityId: number, organisationId: number): BreadcrumbItem[] {
    return this.publishBreadcrumbs(this.organisationBreadcrumbs(communityId, organisationId))
  }

  domainBreadcrumbs(domainId: number, label?: string): BreadcrumbItem[] {
    let crumbs: BreadcrumbItem[]
    if (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.vendor?.community == undefined)) {
      crumbs = this.domainsBreadcrumbs(true)
    } else {
      crumbs = []
    }
    crumbs.push({ type: BreadcrumbType.domain, typeId: domainId, label: label, action: (() => this.toDomain(domainId)), target: this.linkToDomain(domainId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  specificationBreadcrumbs(domainId: number, specificationId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.domainBreadcrumbs(domainId)
    crumbs.push({ type: BreadcrumbType.specification, typeId: specificationId, label: label, action: (() => this.toSpecification(domainId, specificationId)), target: this.linkToSpecification(domainId, specificationId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  specificationGroupBreadcrumbs(domainId: number, groupId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.domainBreadcrumbs(domainId)
    crumbs.push({ type: BreadcrumbType.specificationGroup, typeId: groupId, label: label, action: (() => this.toSpecificationGroup(domainId, groupId)), target: this.linkToSpecificationGroup(domainId, groupId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  actorBreadcrumbs(domainId: number, specificationId: number, actorId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.specificationBreadcrumbs(domainId, specificationId)
    crumbs.push({ type: BreadcrumbType.actor, typeId: actorId, label: label, action: (() => this.toActor(domainId, specificationId, actorId)), target: this.linkToActor(domainId, specificationId, actorId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  endpointBreadcrumbs(domainId: number, specificationId: number, actorId: number, endpointId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.actorBreadcrumbs(domainId, specificationId, actorId)
    crumbs.push({ type: BreadcrumbType.endpoint, typeId: endpointId, label: label, action: (() => this.toEndpoint(domainId, specificationId, actorId, endpointId)), target: this.linkToEndpoint(domainId, specificationId, actorId, endpointId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testSuiteBreadcrumbs(domainId: number, specificationId: number, testSuiteId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.specificationBreadcrumbs(domainId, specificationId)
    crumbs.push({ type: BreadcrumbType.testSuite, typeId: testSuiteId, label: label, action: (() => this.toTestSuite(domainId, specificationId, testSuiteId)), target: this.linkToTestSuite(domainId, specificationId, testSuiteId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testCaseBreadcrumbs(domainId: number, specificationId: number, testSuiteId: number, testCaseId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.testSuiteBreadcrumbs(domainId, specificationId, testSuiteId)
    crumbs.push({ type: BreadcrumbType.testCase, typeId: testCaseId, label: label, action: (() => this.toTestCase(domainId, specificationId, testSuiteId, testCaseId)), target: this.linkToTestCase(domainId, specificationId, testSuiteId, testCaseId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  sharedTestSuiteBreadcrumbs(domainId: number, testSuiteId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.domainBreadcrumbs(domainId)
    crumbs.push({ type: BreadcrumbType.sharedTestSuite, typeId: testSuiteId, label: label, action: (() => this.toSharedTestSuite(domainId, testSuiteId)), target: this.linkToSharedTestSuite(domainId, testSuiteId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  sharedTestCaseBreadcrumbs(domainId: number, testSuiteId: number, testCaseId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.sharedTestSuiteBreadcrumbs(domainId, testSuiteId)
    crumbs.push({ type: BreadcrumbType.sharedTestCase, typeId: testCaseId, label: label, action: (() => this.toSharedTestCase(domainId, testSuiteId, testCaseId)), target: this.linkToSharedTestCase(domainId, testSuiteId, testCaseId) })
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
    crumbs.push({ type: BreadcrumbType.community, typeId: communityId, label: label, action: (() => this.toCommunity(communityId)), target: this.linkToCommunity(communityId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  organisationBreadcrumbs(communityId: number, organisationId: number, label?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    let action: Function|undefined
    let target: NavigationTarget|undefined
    if (organisationId >= 0) {
      action = (() => this.toOrganisationDetails(communityId, organisationId))
      target = this.linkToOrganisationDetails(communityId, organisationId)
    }
    crumbs.push({ type: BreadcrumbType.organisation, typeId: organisationId, label: label, action: action, target: target })
    if (label && !skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemBreadcrumbs(communityId: number, organisationId: number, organisationLabel: string|undefined, systemId: number, label?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = this.organisationBreadcrumbs(communityId, organisationId, organisationLabel, true)
    let action: Function|undefined
    let target: NavigationTarget|undefined
    if (systemId >= 0) {
      action = (() => this.toSystemDetails(communityId, organisationId, systemId))
      target = this.linkToSystemDetails(communityId, organisationId, systemId)
    }
    crumbs.push({ type: BreadcrumbType.system, typeId: systemId, label: label, action: action, target: target })
    if (label && !skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityAdminBreadcrumbs(communityId: number, adminId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityAdmin, typeId: adminId, label: label, action: (() => this.toCommunityAdmin(communityId, adminId)), target: this.linkToCommunityAdmin(communityId, adminId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  organisationUserBreadcrumbs(communityId: number, organisationId: number, userId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.organisationBreadcrumbs(communityId, organisationId)
    crumbs.push({ type: BreadcrumbType.organisationUser, typeId: userId, label: label, action: (() => this.toOrganisationUser(communityId, organisationId, userId)), target: this.linkToOrganisationUser(communityId, organisationId, userId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  landingPageBreadcrumbs(communityId: number, pageId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.landingPage, typeId: pageId, label: label, action: (() => this.toLandingPage(communityId, pageId)), target: this.linkToLandingPage(communityId, pageId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  errorTemplateBreadcrumbs(communityId: number, templateId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.errorTemplate, typeId: templateId, label: label, action: (() => this.toErrorTemplate(communityId, templateId)), target: this.linkToErrorTemplate(communityId, templateId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  legalNoticeBreadcrumbs(communityId: number, noticeId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.legalNotice, typeId: noticeId, label: label, action: (() => this.toLegalNotice(communityId, noticeId)), target: this.linkToLegalNotice(communityId, noticeId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  triggerBreadcrumbs(communityId: number, triggerId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.trigger, typeId: triggerId, label: label, action: (() => this.toTrigger(communityId, triggerId)), target: this.linkToTrigger(communityId, triggerId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  conformanceStatementsBreadcrumbs(communityId: number, organisationId: number, organisationLabel?: string, systemId?: number, systemLabel?: string, snapshotId?:number, snapshotLabel?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    let crumbs: BreadcrumbItem[]
    if (systemId == undefined) {
      crumbs = this.organisationBreadcrumbs(communityId, organisationId, organisationLabel, true)
    } else {
      crumbs = this.systemBreadcrumbs(communityId, organisationId, organisationLabel, systemId, systemLabel, true)
    }
    crumbs.push({ type: BreadcrumbType.statements, label: 'Conformance statements', action: (() => this.toConformanceStatements(communityId, organisationId, systemId, snapshotId)), target: this.linkToConformanceStatements(communityId, organisationId, systemId, snapshotId) })
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
      crumbs.push({ type: BreadcrumbType.statement, typeId: systemId+'|'+actorId, label: statementLabel, action: (() => this.toConformanceStatement(organisationId, systemId, actorId, communityId, snapshotId, snapshotLabel)), target: this.linkToConformanceStatement(organisationId, systemId, actorId, communityId, snapshotId, snapshotLabel) })
    } else {
      crumbs = this.ownConformanceStatementsBreadcrumbs(organisationId, systemId, systemLabel, snapshotId, snapshotLabel, true)
      crumbs.push({ type: BreadcrumbType.statement, typeId: systemId+'|'+actorId, label: statementLabel, action: (() => this.toOwnConformanceStatement(organisationId, systemId, actorId, snapshotId, snapshotLabel)), target: this.linkToOwnConformanceStatement(organisationId, systemId, actorId, snapshotId, snapshotLabel) })
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
    crumbs.push({ type: BreadcrumbType.ownStatements, label: 'Conformance statements', action: (() => this.toOwnConformanceStatements(organisationId, systemId, snapshotId)), target: this.linkToOwnConformanceStatements(organisationId, systemId, snapshotId) })
    if (snapshotLabel) {
      crumbs.push({ type: BreadcrumbType.conformanceSnapshot, label: snapshotLabel })
    }
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  ownOrganisationBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.ownOrganisation, label: 'My '+this.dataService.labelOrganisationLower(), action: (() => this.toOwnOrganisationDetails()), target: this.linkToOwnOrganisationDetails() }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  ownSystemBreadcrumbs(systemId: number, label?: string, skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = this.ownOrganisationBreadcrumbs(true)
    let action: Function|undefined
    let target: NavigationTarget|undefined
    if (systemId >= 0) {
      action = (() => this.toOwnSystemDetails(systemId))
      target = this.linkToOwnSystemDetails(systemId)
    }
    crumbs.push({ type: BreadcrumbType.ownSystem, typeId: systemId, label: label, action: action, target: target })
    if (label && !skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testHistoryBreadcrumbs(organisationId: number): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.ownTestHistory, label: 'My test sessions', action: (() => this.toTestHistory(organisationId)), target: this.linkToTestHistory(organisationId) }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  conformanceDashboardBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.conformanceDashboard, label: 'Conformance dashboard', action: (() => this.toConformanceDashboard()), target: this.linkToConformanceDashboard() }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  sessionDashboardBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.sessionDashboard, label: 'Session dashboard', action: (() => this.toSessionDashboard()), target: this.linkToSessionDashboard() }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  serviceHealthDashboardBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.serviceHealthDashboard, label: 'Service health', action: (() => this.toServiceHealthDashboard()), target: this.linkToServiceHealthDashboard() }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communitySessionsBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.communitySessions, label: 'Community test sessions', action: (() => this.toCommunitySessionDashboard()), target: this.linkToCommunitySessionDashboard() }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  dataManagementBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.dataManagement, label: 'Data management', action: (() => this.toDataManagement()), target: this.linkToDataManagement() }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  domainsBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.domains, label: 'Domains', action: (() => this.toDomains()), target: this.linkToDomains() }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communitiesBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.communities, label: 'Communities', action: (() => this.toUserManagement()), target: this.linkToUserManagement() }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemConfigurationBreadcrumbs(skipUpdate?: boolean): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.systemConfiguration, label: 'System administration', action: (() => this.toSystemAdministration()), target: this.linkToSystemAdministration() }]
    if (!skipUpdate) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  testBedAdminBreadcrumbs(adminId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemAdmin, typeId: adminId, label: label, action: (() => this.toTestBedAdmin(adminId)), target: this.linkToTestBedAdmin(adminId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemLandingPageBreadcrumbs(pageId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemLandingPage, typeId: pageId, label: label, action: (() => this.toLandingPage(undefined, pageId)), target: this.linkToLandingPage(undefined, pageId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemLegalNoticeBreadcrumbs(noticeId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemLegalNotice, typeId: noticeId, label: label, action: (() => this.toLegalNotice(undefined, noticeId)), target: this.linkToLegalNotice(undefined, noticeId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemErrorTemplateBreadcrumbs(templateId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.systemErrorTemplate, typeId: templateId, label: label, action: (() => this.toErrorTemplate(undefined, templateId)), target: this.linkToErrorTemplate(undefined, templateId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  systemThemeBreadcrumbs(themeId: number, label?: string): BreadcrumbItem[] {
    const crumbs = this.systemConfigurationBreadcrumbs(true)
    crumbs.push({ type: BreadcrumbType.theme, typeId: themeId, label: label, action: (() => this.toTheme(themeId)), target: this.linkToTheme(themeId) })
    if (label) this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityLabelsBreadcrumbs(communityId: number): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityLabels, label: 'Labels', action: (() => this.toCommunityLabels(communityId)), target: this.linkToCommunityLabels(communityId) })
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityParametersBreadcrumbs(communityId: number): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityParameters, label: 'Properties', action: (() => this.toCommunityParameters(communityId)), target: this.linkToCommunityParameters(communityId) })
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  communityReportSettingsBreadcrumbs(communityId: number): BreadcrumbItem[] {
    const crumbs = this.communityBreadcrumbs(communityId)
    crumbs.push({ type: BreadcrumbType.communityParameters, label: 'Report settings', action: (() => this.toCommunityReportSettings(communityId)), target: this.linkToCommunityReportSettings(communityId) })
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  profileBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.profile, label: 'Profile', action: (() => this.toProfile()), target: this.linkToProfile() }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

  changePasswordBreadcrumbs(): BreadcrumbItem[] {
    const crumbs = [{ type: BreadcrumbType.changePassword, label: 'Change password', action: (() => this.toChangePassword()), target: this.linkToChangePassword() }]
    this.dataService.breadcrumbUpdate({ breadcrumbs: crumbs })
    return crumbs
  }

}
