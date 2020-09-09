class UserGuideService

    @$inject = ['DataService', '$state']
    constructor: (@DataService, @$state) ->
        @paths = {}
        @initialise()

    initialise: () =>
        @paths['app.login'] = @setPath('login/index.html')
        @paths['app.home'] = @setPath('index.html')
        @paths['app.onetime'] = @setPath('login/index.html#replacing-a-one-time-password')
        @paths['app.settings.password'] = @setPath('profile/index.html#change-your-password')
        @paths['app.settings.organisation'] = @setPath('profile/index.html#view-your-organisation-s-details', 'profile/index.html#manage-your-organisation-s-details')
        @paths['app.tests.execution'] = @setPath('executeTests/index.html#step-3-test-execution', '', 'validateTestSetup/index.html#step-3-test-execution')
        @paths['app.reports.presentation'] = @setPath('testHistory/index.html#view-a-test-session-s-steps', '', 'validateTestSetup/index.html#view-a-test-session-s-steps')
        @paths['app.systems.detail.info'] = @setPath('manageConformanceStatements/index.html#view-selected-system-s-information', '', 'validateTestSetup/index.html#view-selected-system-s-information')
        @paths['app.systems.detail.conformance.list'] = @setPath('manageConformanceStatements/index.html#view-your-conformance-statements', '', 'validateTestSetup/index.html#view-your-conformance-statements')
        @paths['app.systems.detail.conformance.detail'] = @setPath('manageConformanceStatements/index.html#view-a-conformance-statement-s-details', '', 'validateTestSetup/index.html#view-a-conformance-statement-s-details')
        @paths['app.systems.detail.conformance.create'] = @setPath('', 'manageConformanceStatements/index.html#create-a-conformance-statement', 'validateTestSetup/index.html#create-a-conformance-statement')
        @paths['app.systems.detail.tests'] = @setPath('testHistory/index.html#search-test-history', '', 'validateTestSetup/index.html#search-test-history')
        @paths['app.systems.list'] = @setPath('manageSystems/index.html', '', 'validateTestSetup/index.html#manage-your-systems')
        @paths['app.admin'] = @setPath('', '', 'index.html')
        @paths['app.admin.dashboard.list'] = @setPath('', '', 'sessionDashboard/index.html')
        @paths['app.admin.domains.list'] = @setPath('', '', 'domainDashboard/index.html')
        @paths['app.admin.conformance.list'] = @setPath('', '', 'conformanceDashboard/index.html')
        @paths['app.admin.domains.create'] = @setPath('', '', '', 'domainDashboard/index.html#create-domain')
        @paths['app.admin.domains.detail.list'] = @setPath('', '', 'domainDashboard/index.html#manage-domain-details')
        @paths['app.admin.domains.detail.specifications.create'] = @setPath('', '', 'domainDashboard/index.html#create-specification')
        @paths['app.admin.domains.detail.specifications.detail.list'] = @setPath('', '', 'domainDashboard/index.html#manage-specification-details')
        @paths['app.admin.domains.detail.specifications.detail.actors.create'] = @setPath('', '', 'domainDashboard/index.html#create-actor')
        @paths['app.admin.domains.detail.specifications.detail.actors.detail.list'] = @setPath('', '', 'domainDashboard/index.html#manage-actor-details')
        @paths['app.admin.domains.detail.specifications.detail.actors.detail.endpoints.create'] = @setPath('', '', 'domainDashboard/index.html#create-endpoint')
        @paths['app.admin.domains.detail.specifications.detail.actors.detail.endpoints.detail'] = @setPath('', '', 'domainDashboard/index.html#manage-endpoint-details')
        @paths['app.admin.domains.detail.specifications.detail.testsuites.detail.list'] = @setPath('', '', 'domainDashboard/index.html#manage-test-suite-details')
        @paths['app.admin.domains.detail.specifications.detail.testsuites.detail.testcases.detail.list'] = @setPath('', '', 'domainDashboard/index.html#manage-test-case-details')
        @paths['app.admin.users.list'] = @setPath('', '', 'communityDashboard/index.html')
        @paths['app.admin.users.communities.create'] = @setPath('', '', '', 'communityDashboard/index.html#create-a-community')
        @paths['app.admin.users.communities.detail.list'] = @setPath('', '', 'communityDashboard/index.html', 'communityDashboard/index.html#manage-a-community-s-details')
        @paths['app.admin.users.communities.detail.certificate'] = @setPath('', '', 'communityDashboard/index.html#edit-conformance-certificate-settings')
        @paths['app.admin.users.communities.detail.parameters'] = @setPath('', '', 'communityDashboard/index.html#edit-custom-member-properties')
        @paths['app.admin.users.communities.detail.labels'] = @setPath('', '', 'communityDashboard/index.html#edit-labels')
        @paths['app.admin.users.communities.detail.organizations.create'] = @setPath('', '', 'communityDashboard/index.html#create-an-organisation')
        @paths['app.admin.users.communities.detail.organizations.detail.list'] = @setPath('', '', 'communityDashboard/index.html#manage-an-organisation-s-details')
        @paths['app.admin.users.communities.detail.landingpages.create'] = @setPath('', '', 'communityDashboard/index.html#create-landing-page')
        @paths['app.admin.users.communities.detail.landingpages.detail'] = @setPath('', '', 'communityDashboard/index.html#edit-landing-page')
        @paths['app.admin.users.communities.detail.legalnotices.create'] = @setPath('', '', 'communityDashboard/index.html#create-legal-notice')
        @paths['app.admin.users.communities.detail.legalnotices.detail'] = @setPath('', '', 'communityDashboard/index.html#edit-legal-notice')
        @paths['app.admin.users.communities.detail.errortemplates.create'] = @setPath('', '', 'communityDashboard/index.html#create-error-template')
        @paths['app.admin.users.communities.detail.errortemplates.detail'] = @setPath('', '', 'communityDashboard/index.html#edit-error-template')
        @paths['app.admin.users.communities.detail.triggers.create'] = @setPath('', '', 'communityDashboard/index.html#create-trigger')
        @paths['app.admin.users.communities.detail.triggers.detail'] = @setPath('', '', 'communityDashboard/index.html#edit-trigger')
        @paths['app.settings.profile'] = @setPath('profile/index.html')
        @paths['app.admin.users.admins.create'] = @setPath('', '', '', 'communityDashboard/index.html#manage-test-bed-administrators')
        @paths['app.admin.users.admins.detail'] = @setPath('', '', '', 'communityDashboard/index.html#manage-test-bed-administrators')
        @paths['app.admin.users.communities.detail.admins.create'] = @setPath('', '', 'communityDashboard/index.html#manage-administrators', 'communityDashboard/index.html#manage-community-administrators')
        @paths['app.admin.users.communities.detail.admins.detail'] = @setPath('', '', 'communityDashboard/index.html#manage-administrators', 'communityDashboard/index.html#manage-community-administrators')
        @paths['app.admin.users.communities.detail.organizations.detail.users.create'] = @setPath('', '', 'communityDashboard/index.html#manage-the-organisation-s-users')
        @paths['app.admin.users.communities.detail.organizations.detail.users.detail.list'] = @setPath('', '', 'communityDashboard/index.html#manage-the-organisation-s-users')
        @paths['app.admin.export'] = @setPath('', '', 'exportimport/index.html#export-data')
        @paths['app.admin.import'] = @setPath('', '', 'exportimport/index.html#import-data')

        # Validate to ensure no missing states
        states = @$state.get()
        for state in states
            if !state.abstract && @paths[state.name] == undefined
                console.log('Warning: missing user guide configuration for route ['+state.name+']')

    setPath: (path1, path2, path3, path4) =>
        if path4?
            if @DataService.isVendorUser
                path1
            else if @DataService.isVendorAdmin
                if path2 == ''
                    path1
                else
                    path2
            else if @DataService.isCommunityAdmin
                if path3 == ''
                    if path2 == ''
                       path1
                    else
                        path2
                else
                    path3
            else
                path4
        else if path3?
            if @DataService.isVendorUser
                path1
            else if @DataService.isVendorAdmin
                if path2 == ''
                    path1
                else
                    path2
            else
                if path3 == ''
                    if path2 == ''
                        path1
                    else 
                        path2
                else
                    path3
        else if path2?
            if @DataService.isVendorUser || @DataService.isVendorAdmin
                path1
            else
                if path2 == ''
                    path1
                else
                    path2
        else
            path1

    baseLink: () =>
        if (@DataService.configuration?)
            if @DataService.isVendorAdmin
                @DataService.configuration['userguide.oa']
            else if @DataService.isCommunityAdmin
                @DataService.configuration['userguide.ca']
            else if @DataService.isSystemAdmin
                @DataService.configuration['userguide.ta']
            else
                @DataService.configuration['userguide.ou']
        else
            @DataService.configuration['userguide.ou']

    userGuideLink: () =>
        link = @baseLink()
        if link.slice(-1) != '/'
            link += '/'
        if @$state.current? && @$state.current.name?
            if @paths[@$state.current.name]?
                link += @paths[@$state.current.name]
        link

services.service('UserGuideService', UserGuideService)

