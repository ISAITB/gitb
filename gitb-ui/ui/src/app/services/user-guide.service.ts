import { Injectable } from '@angular/core';
import { DataService } from './data.service'
import { ActivationEnd, NavigationStart, Router, Routes } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class UserGuideService {

  private paths:any = {}
  private currentPath: string|undefined

  constructor(
    private dataService: DataService, private router: Router) {
    this.initialise()
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.currentPath = undefined
      }
      if (event instanceof ActivationEnd) {
        if (this.currentPath === undefined) {
          let tempPath = '/'
          if (event.snapshot != undefined && event.snapshot.pathFromRoot != undefined) {
            let withExtras = false
            for (let routePart of event.snapshot.pathFromRoot) {
              if (routePart.routeConfig != undefined && routePart.routeConfig.path != undefined && routePart.routeConfig.path.trim().length > 0) {
                if (withExtras) {
                  tempPath += '/'
                }
                tempPath += routePart.routeConfig.path.trim()
                withExtras = true
              }
            }
          }
          this.currentPath = tempPath
        }
      }
    })
  }

  initialise() {
    this.paths['/'] = this.setPath('')
    this.paths['/login'] = this.setPath('login/index.html')
    this.paths['/home'] = this.setPath('index.html')
    this.paths['/settings/profile'] = this.setPath('profile/index.html')
    this.paths['/settings/password'] = this.setPath('profile/index.html#change-your-password')
    this.paths['/settings/organisation'] = this.setPath('profile/index.html#view-your-organisation-s-details', 'profile/index.html#manage-your-organisation-s-details')
    this.paths['/organisation/:org_id/test/:system_id/:actor_id/:spec_id/execute'] = this.setPath('executeTests/index.html#execute_tests_interactive', '', 'validateTestSetup/index.html#execute_tests_interactive')
    this.paths['/organisation/:org_id/systems/:id/info'] = this.setPath('manageConformanceStatements/index.html#view-selected-system-s-information', '', 'validateTestSetup/index.html#view-selected-system-s-information')
    this.paths['/organisation/:org_id/systems/:id/conformance'] = this.setPath('manageConformanceStatements/index.html#view-your-conformance-statements', '', 'validateTestSetup/index.html#view-your-conformance-statements')
    this.paths['/organisation/:org_id/systems/:id/conformance/detail/:actor_id/:spec_id'] = this.setPath('manageConformanceStatements/index.html#view-a-conformance-statement-s-details', '', 'validateTestSetup/index.html#view-a-conformance-statement-s-details')
    this.paths['/organisation/:org_id/systems/:id/conformance/create'] = this.setPath('', 'manageConformanceStatements/index.html#create-a-conformance-statement', 'validateTestSetup/index.html#create-a-conformance-statement')
    this.paths['/organisation/:org_id/systems/:id/tests'] = this.setPath('testHistory/index.html#search-test-history', '', 'validateTestSetup/index.html#search-test-history')
    this.paths['/organisation/:org_id/systems'] = this.setPath('manageSystems/index.html', '', 'validateTestSetup/index.html#manage-your-systems')
    this.paths['/admin'] = this.setPath('', '', 'index.html')
    this.paths['/admin/sessions'] = this.setPath('', '', 'sessionDashboard/index.html')
    this.paths['/admin/conformance'] = this.setPath('', '', 'conformanceDashboard/index.html')
    this.paths['/admin/domains'] = this.setPath('', '', 'domainDashboard/index.html')
    this.paths['/admin/domains/create'] = this.setPath('', '', '', 'domainDashboard/index.html#create-domain')
    this.paths['/admin/domains/:id'] = this.setPath('', '', 'domainDashboard/index.html#manage-domain-details')
    this.paths['/admin/domains/:id/specifications/create'] = this.setPath('', '', 'domainDashboard/index.html#create-specification')
    this.paths['/admin/domains/:id/specifications/:spec_id'] = this.setPath('', '', 'domainDashboard/index.html#manage-specification-details')
    this.paths['/admin/domains/:id/specifications/:spec_id/actors/create'] = this.setPath('', '', 'domainDashboard/index.html#create-actor')
    this.paths['/admin/domains/:id/specifications/:spec_id/actors/:actor_id'] = this.setPath('', '', 'domainDashboard/index.html#manage-actor-details')
    this.paths['/admin/domains/:id/specifications/:spec_id/testsuites/:testsuite_id'] = this.setPath('', '', 'domainDashboard/index.html#manage-test-suite-details')
    this.paths['/admin/domains/:id/specifications/:spec_id/testsuites/:testsuite_id/testcases/:testcase_id'] = this.setPath('', '', 'domainDashboard/index.html#manage-test-case-details')
    this.paths['/admin/domains/:id/specifications/:spec_id/actors/:actor_id/endpoints/create'] = this.setPath('', '', 'domainDashboard/index.html#create-endpoint')
    this.paths['/admin/domains/:id/specifications/:spec_id/actors/:actor_id/endpoints/:endpoint_id'] = this.setPath('', '', 'domainDashboard/index.html#manage-endpoint-details')
    this.paths['/admin/users'] = this.setPath('', '', 'communityDashboard/index.html')
    this.paths['/admin/users/admin/create'] = this.setPath('', '', '', 'communityDashboard/index.html#manage-test-bed-administrators')
    this.paths['/admin/users/admin/:id'] = this.setPath('', '', '', 'communityDashboard/index.html#manage-test-bed-administrators')
    this.paths['/admin/users/community/create'] = this.setPath('', '', '', 'communityDashboard/index.html#create-a-community')
    this.paths['/admin/users/community/:community_id'] = this.setPath('', '', 'communityDashboard/index.html', 'communityDashboard/index.html#manage-a-community-s-details')
    this.paths['/admin/users/community/:community_id/admin/create'] = this.setPath('', '', 'communityDashboard/index.html#manage-administrators', 'communityDashboard/index.html#manage-community-administrators')
    this.paths['/admin/users/community/:community_id/admin/:admin_id'] = this.setPath('', '', 'communityDashboard/index.html#manage-administrators', 'communityDashboard/index.html#manage-community-administrators')
    this.paths['/admin/users/community/:community_id/pages/create'] = this.setPath('', '', 'communityDashboard/index.html#create-landing-page')
    this.paths['/admin/users/community/:community_id/pages/:page_id'] = this.setPath('', '', 'communityDashboard/index.html#edit-landing-page')
    this.paths['/admin/users/community/:community_id/notices/create'] = this.setPath('', '', 'communityDashboard/index.html#create-legal-notice')
    this.paths['/admin/users/community/:community_id/notices/:notice_id'] = this.setPath('', '', 'communityDashboard/index.html#edit-legal-notice')
    this.paths['/admin/users/community/:community_id/errortemplates/create'] = this.setPath('', '', 'communityDashboard/index.html#create-error-template')
    this.paths['/admin/users/community/:community_id/errortemplates/:template_id'] = this.setPath('', '', 'communityDashboard/index.html#edit-error-template')
    this.paths['/admin/users/community/:community_id/triggers/create'] = this.setPath('', '', 'communityDashboard/index.html#create-trigger')
    this.paths['/admin/users/community/:community_id/triggers/:trigger_id'] = this.setPath('', '', 'communityDashboard/index.html#edit-trigger')
    this.paths['/admin/users/community/:community_id/organisation/create'] = this.setPath('', '', 'communityDashboard/index.html#create-an-organisation')
    this.paths['/admin/users/community/:community_id/organisation/:org_id'] = this.setPath('', '', 'communityDashboard/index.html#manage-an-organisation-s-details')
    this.paths['/admin/users/community/:community_id/organisation/:org_id/user/create'] = this.setPath('', '', 'communityDashboard/index.html#manage-the-organisation-s-users')
    this.paths['/admin/users/community/:community_id/organisation/:org_id/user/:user_id'] = this.setPath('', '', 'communityDashboard/index.html#manage-the-organisation-s-users')
    this.paths['/admin/users/community/:community_id/certificate'] = this.setPath('', '', 'communityDashboard/index.html#edit-conformance-certificate-settings')
    this.paths['/admin/users/community/:community_id/parameters'] = this.setPath('', '', 'communityDashboard/index.html#edit-custom-member-properties')
    this.paths['/admin/users/community/:community_id/labels'] = this.setPath('', '', 'communityDashboard/index.html#edit-labels')
    this.paths['/admin/export'] = this.setPath('', '', 'exportimport/index.html#export-data')
    this.paths['/admin/import'] = this.setPath('', '', 'exportimport/index.html#import-data')

    // Validate to ensure no missing states
    this.reportMissingRouteLinks(this.router.config, [])
  }

  private reportMissingRouteLinks(routes: Routes, parentPaths: string[]) {
    if (routes !== undefined) {
      for (let route of routes) {
        if (route.path !== undefined) {
          if (route.children === undefined) {
            let absolutePath = '/'
            if (parentPaths.length > 0) {
              absolutePath += parentPaths.join('/') + '/'
            }
            absolutePath += route.path
            if (this.paths[absolutePath] === undefined) {
              console.warn('Warning: missing user guide configuration for route ['+absolutePath+']')
            }
          } else {
            if (route.path.length > 0) {
              parentPaths.push(route.path)
            }
            this.reportMissingRouteLinks(route.children, parentPaths)
            parentPaths.pop()
          }
        }
      }
    }
  }

  private setPath(path1: string, path2?: string, path3?: string, path4?: string): string|undefined {
    if (path4) {
        if (this.dataService.isVendorUser) {
            return path1
        } else if (this.dataService.isVendorAdmin) {
            if (path2 == '') {
                return path1
            } else {
                return path2
            }
        } else if (this.dataService.isCommunityAdmin) {
            if (path3 == '') {
                if (path2 == '') {
                  return path1
                } else {
                  return path2
                }
            } else {
                return path3
            }
        } else {
          return path4
        }
    } else if (path3) {
        if (this.dataService.isVendorUser) {
          return path1
        } else if (this.dataService.isVendorAdmin) {
            if (path2 == '') {
              return path1
            } else {
              return path2
            }
        } else {
          if (path3 == '') {
            if (path2 == '') {
              return path1
            } else {
              return path2
            }
          } else {
            return path3
          }
        }
    } else if (path2) {
        if (this.dataService.isVendorUser || this.dataService.isVendorAdmin) {
          return path1
        } else {
          if (path2 == '') {
            return path1
          } else {
            return path2
          }
        }
    } else {
      return path1
    }
  }

  baseLink(): string {
    if (this.dataService.configuration != undefined) {
      if (this.dataService.isVendorAdmin) {
        return this.dataService.configuration.userGuideOA
      } else if (this.dataService.isCommunityAdmin) {
        return this.dataService.configuration.userGuideCA
      } else if (this.dataService.isSystemAdmin) {
        return this.dataService.configuration.userGuideTA
      } else {
        return this.dataService.configuration.userGuideOU
      }
    } else {
      return ''
    }
  }

  userGuideLink() {
    let link = this.baseLink()
    if (link.slice(-1) != '/') {
      link += '/'
    }
    let pathToUse = this.currentPath
    if (pathToUse == undefined) {
      pathToUse = '/'
    }
    if (this.paths[pathToUse]) {
      link += this.paths[pathToUse]
    }
    return link
  }

}
