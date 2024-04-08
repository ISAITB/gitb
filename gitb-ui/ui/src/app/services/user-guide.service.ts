import { Injectable } from '@angular/core';
import { DataService } from './data.service'
import { ActivationEnd, NavigationStart, Router, Routes } from '@angular/router';
import { Constants } from '../common/constants';

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
    this.paths['/home'] = this.setPath('index.html')
    this.paths['/login'] = this.setPath('login/index.html')
    this.paths['/settings/profile'] = this.setPath('profile/index.html')
    this.paths['/settings/organisation'] = this.setPath('manageOrganisation/index.html', '', 'validateTestSetup/index.html#manage-your-organisation')
    this.paths['/settings/organisation/user/create'] = this.setPath('', 'manageOrganisation/index.html#create-a-new-user')
    this.paths['/settings/organisation/user/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID] = this.setPath('', 'manageOrganisation/index.html#edit-an-existing-user')
    this.paths['/settings/organisation/system/create'] = this.setPath('', 'manageOrganisation/index.html#create-a-new-system', 'validateTestSetup/index.html#create-a-new-system')
    this.paths['/settings/organisation/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID] = this.setPath('manageOrganisation/index.html#manage-your-systems', 'manageOrganisation/index.html#edit-an-existing-system', 'validateTestSetup/index.html#edit-an-existing-system')
    this.paths['/settings/password'] = this.setPath('profile/index.html#change-your-password')
    this.paths['/admin/sessions'] = this.setPath('', '', 'sessionDashboard/index.html')
    this.paths['/admin/conformance'] = this.setPath('', '', 'conformanceDashboard/index.html')
    this.paths['/admin/domains'] = this.setPath('', '', 'domainDashboard/index.html')
    this.paths['/admin/domains/create'] = this.setPath('', '', '', 'domainDashboard/index.html#create-domain')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-domain-details')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-test-suite-details')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID+'/testcases/:'+Constants.NAVIGATION_PATH_PARAM.TEST_CASE_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-test-case-details')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/groups/create'] = this.setPath('', '', 'domainDashboard/index.html#specification-groups')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/groups/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_GROUP_ID] = this.setPath('', '', 'domainDashboard/index.html#specification-groups')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/create'] = this.setPath('', '', 'domainDashboard/index.html#create-specification')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-specification-details')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/create'] = this.setPath('', '', 'domainDashboard/index.html#create-actor')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-actor-details')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-test-suite-details')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID+'/testcases/:'+Constants.NAVIGATION_PATH_PARAM.TEST_CASE_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-test-case-details')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/endpoints/create'] = this.setPath('', '', 'domainDashboard/index.html#create-endpoint')
    this.paths['/admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/endpoints/:'+Constants.NAVIGATION_PATH_PARAM.ENDPOINT_ID] = this.setPath('', '', 'domainDashboard/index.html#manage-endpoint-details')
    this.paths['/admin/users'] = this.setPath('', '', 'communityDashboard/index.html')
    this.paths['/admin/users/community/create'] = this.setPath('', '', '', 'communityDashboard/index.html#create-a-community')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID] = this.setPath('', '', 'communityDashboard/index.html', 'communityDashboard/index.html#manage-community-details')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/admin/create'] = this.setPath('', '', 'communityDashboard/index.html#manage-administrators')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/admin/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID] = this.setPath('', '', 'communityDashboard/index.html#manage-administrators')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/pages/create'] = this.setPath('', '', 'communityDashboard/index.html#create-landing-page')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/pages/:'+Constants.NAVIGATION_PATH_PARAM.LANDING_PAGE_ID] = this.setPath('', '', 'communityDashboard/index.html#edit-landing-page')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/notices/create'] = this.setPath('', '', 'communityDashboard/index.html#create-legal-notice')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/notices/:'+Constants.NAVIGATION_PATH_PARAM.LEGAL_NOTICE_ID] = this.setPath('', '', 'communityDashboard/index.html#edit-legal-notice')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/errortemplates/create'] = this.setPath('', '', 'communityDashboard/index.html#create-error-template')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/errortemplates/:'+Constants.NAVIGATION_PATH_PARAM.ERROR_TEMPLATE_ID] = this.setPath('', '', 'communityDashboard/index.html#edit-error-template')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/triggers/create'] = this.setPath('', '', 'communityDashboard/index.html#create-trigger')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/triggers/:'+Constants.NAVIGATION_PATH_PARAM.TRIGGER_ID] = this.setPath('', '', 'communityDashboard/index.html#edit-trigger')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/reports'] = this.setPath('', '', 'communityDashboard/index.html#edit-report-settings')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/parameters'] = this.setPath('', '', 'communityDashboard/index.html#edit-custom-member-properties')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/labels'] = this.setPath('', '', 'communityDashboard/index.html#edit-labels')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/create'] = this.setPath('', '', 'communityDashboard/index.html#create-an-organisation')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID] = this.setPath('', '', 'communityDashboard/index.html#manage-an-organisation-s-details')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/conformance'] = this.setPath('', '', 'validateTestSetup/index.html#manage-your-conformance-statements')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/conformance/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/create'] = this.setPath('', '', 'validateTestSetup/index.html#create-a-conformance-statement')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/conformance/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/actor/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID] = this.setPath('', '', 'validateTestSetup/index.html#view-a-conformance-statement-s-details')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/conformance/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/actor/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/snapshot/:'+Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_ID] = this.setPath('', '', 'validateTestSetup/index.html#view-a-conformance-statement-s-details')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/user/create'] = this.setPath('', '', 'communityDashboard/index.html#manage-the-organisation-s-users')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/user/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID] = this.setPath('', '', 'communityDashboard/index.html#manage-the-organisation-s-users')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/create'] = this.setPath('', '', 'communityDashboard/index.html#create-a-new-system')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID] = this.setPath('', '', 'communityDashboard/index.html#edit-an-existing-system')
    this.paths['/admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/test/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/:actor_id/execute'] = this.setPath('', '', 'validateTestSetup/index.html#execute-tests')
    this.paths['/admin/export'] = this.setPath('', '', 'exportimport/index.html#export-data')
    this.paths['/admin/import'] = this.setPath('', '', 'exportimport/index.html#import-data')
    this.paths['/admin/system'] = this.setPath('', '', '', 'systemAdministration/index.html')
    this.paths['/admin/system/admin/create'] = this.setPath('', '', '', 'systemAdministration/index.html#manage-system-administrators')
    this.paths['/admin/system/admin/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID] = this.setPath('', '', '', 'systemAdministration/index.html#manage-system-administrators')
    this.paths['/admin/system/pages/create'] = this.setPath('', '', '', 'systemAdministration/index.html#create-landing-page')
    this.paths['/admin/system/pages/:'+Constants.NAVIGATION_PATH_PARAM.LANDING_PAGE_ID] = this.setPath('', '', '', 'systemAdministration/index.html#edit-landing-page')
    this.paths['/admin/system/notices/create'] = this.setPath('', '', '', 'systemAdministration/index.html#create-legal-notice')
    this.paths['/admin/system/notices/:'+Constants.NAVIGATION_PATH_PARAM.LEGAL_NOTICE_ID] = this.setPath('', '', '', 'systemAdministration/index.html#edit-legal-notice')
    this.paths['/admin/system/errortemplates/create'] = this.setPath('', '', '', 'systemAdministration/index.html#create-error-template')
    this.paths['/admin/system/errortemplates/:'+Constants.NAVIGATION_PATH_PARAM.ERROR_TEMPLATE_ID] = this.setPath('', '', '', 'systemAdministration/index.html#edit-error-template')
    this.paths['/admin/system/themes/create'] = this.setPath('', '', '', 'systemAdministration/index.html#create-theme')
    this.paths['/admin/system/themes/create/:'+Constants.NAVIGATION_PATH_PARAM.THEME_ID] = this.setPath('', '', '', 'systemAdministration/index.html#create-theme')
    this.paths['/admin/system/themes/:'+Constants.NAVIGATION_PATH_PARAM.THEME_ID] = this.setPath('', '', '', 'systemAdministration/index.html#edit-theme')
    this.paths['/organisation/conformance/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID] = this.setPath('manageConformanceStatements/index.html#view-your-conformance-statements', '', 'validateTestSetup/index.html#view-your-conformance-statements')
    this.paths['/organisation/conformance/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/create'] = this.setPath('', 'manageConformanceStatements/index.html#create-a-conformance-statement', 'validateTestSetup/index.html#create-a-conformance-statement')
    this.paths['/organisation/conformance/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/actor/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID] = this.setPath('manageConformanceStatements/index.html#view-a-conformance-statement-s-details', '', 'validateTestSetup/index.html#view-a-conformance-statement-s-details')
    this.paths['/organisation/conformance/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/actor/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/snapshot/:'+Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_ID] = this.setPath('manageConformanceStatements/index.html#view-a-conformance-statement-s-details', '', 'validateTestSetup/index.html#view-a-conformance-statement-s-details')
    this.paths['/organisation/tests/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID] = this.setPath('testHistory/index.html', '', 'validateTestSetup/index.html#view-your-test-history')
    this.paths['/organisation/test/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/execute'] = this.setPath('executeTests/index.html', '', 'validateTestSetup/index.html#execute-tests')
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
