app.config ['$stateProvider', '$urlRouterProvider',
	($stateProvider, $urlRouterProvider) ->
		profile = [
			'$q', '$log', '$state', 'AuthProvider', 'AccountService', 'DataService', 'CommunityService',
			($q, $log, $state, AuthProvider, AccountService, DataService, CommunityService)->
				deferred = $q.defer()

				getUserProfile = ()->
					$log.debug 'Getting user profile from the server...'
					AccountService.getUserProfile()
						.then (data)->
							DataService.setUser(data)
							$log.debug 'Got user profile from the server...'
							deferred.resolve()

				getVendorProfile = () ->
					AccountService.getVendorProfile()
					.then (data) ->
						DataService.setVendor(data)
						deferred.resolve()

				getUserCommunity = () ->
					CommunityService.getUserCommunity()
					.then (data) ->
						DataService.setCommunity(data)
						deferred.resolve()

				$log.debug 'Resolving user profile..'
				authenticated = AuthProvider.isAuthenticated()

				if authenticated
					if !DataService.user?
						getUserProfile()
					if !DataService.vendor?
						getVendorProfile()
					if !DataService.community?
						getUserCommunity()
				else
					$log.debug 'No need for user profile, user is not authenticated...'
					deferred.resolve()
		]

		system = [
			'$q', 'DataService', 'SystemService'
			($q, DataService, SystemService)->
				deferred = $q.defer()

				if DataService.isVendorUser
					SystemService.getSystemsByOrganization(DataService.vendor.id)
					.then (data) ->
						if data.length == 1
							deferred.reject {redirectTo: 'app.systems.detail.conformance.list', params: {id: data[0].id}}
						else
							deferred.resolve()
				else
					deferred.resolve()

				deferred.promise
		]

		conformance = [
			'$q', '$stateParams', 'DataService', 'SystemService'
			($q, $stateParams, DataService, SystemService)->
				deferred = $q.defer()

				if DataService.isVendorUser
					SystemService.getConformanceStatements $stateParams.id
					.then (data) ->
						if data.length == 1
							deferred.reject {redirectTo: 'app.systems.detail.conformance.detail', params: {id: $stateParams.id, actor_id: data[0].actorId, specId: data[0].specificationId}}
						else
							deferred.resolve()
				else
					deferred.resolve()

				deferred.promise
		]

		$urlRouterProvider.when('', '/')
		$urlRouterProvider.otherwise('/')

		states =
			'app':
				url: ''
				templateUrl: 'assets/views/index.html'
				controller: 'IndexController'
				controllerAs: 'indexCtrl'
				abstract: true
				resolve:
					profile: profile
			'app.home':
				url: '/'
				templateUrl: 'assets/views/home.html'
				controller: 'HomeController'
				controllerAs: 'homeCtrl'
				resolve: profile
			'app.register':
				url: '/register'
				templateUrl: 'assets/views/register.html'
			'app.login':
				url: '/login'
				templateUrl: 'assets/views/login.html'
			'app.recover':
				url: '/recover'
				templateUrl: 'assets/views/recover.html'
			'app.tests':
				url: '/tests'
				abstract: true
				templateUrl: 'assets/views/tests/index.html'
			'app.tests.execution':
				url: '/:systemId?actorId&specId&testCaseId&testSuiteId'
				templateUrl: 'assets/views/tests/execution-v2.html'
				controller: 'TestExecutionControllerV2'
				controllerAs: 'testExecutionCtrl'
			'app.reports':
				url: '/reports/:session_id'
				templateUrl: 'assets/views/result.html'
				abstract: true
				controller: 'TestResultController'
				controllerAs: 'testResultCtrl'
			'app.reports.presentation':
				url: ''
				templateUrl: 'assets/views/test-presentation.html'
				controller: 'TestPresentationController'
				controllerAs: 'testPresentationCtrl'
			'app.systems':
				url: '/systems'
				abstract: true
				templateUrl: 'assets/views/systems/index.html'
				controller: 'SystemsTitleController'
				controllerAs: 'systemsTitleCtrl'
			'app.systems.list':
				url: ''
				templateUrl: 'assets/views/systems/list.html'
				controller: 'SystemsController'
				controllerAs: 'systemsCtrl'
				resolve: system
			'app.systems.detail':
				url: '/:id'
				templateUrl: 'assets/views/systems/detail.html'
				controller: 'SystemNavigationController'
				controllerAs: 'systemNavigationCtrl'
				abstract: true
			'app.systems.detail.info':
				url: ''
				templateUrl: 'assets/views/systems/info.html'
				controller: 'SystemController'
				controllerAs: 'systemCtrl'
			'app.systems.detail.conformance':
				url: '/conformance'
				template: '<div ui-view/>'
				abstract: true
			'app.systems.detail.conformance.list':
				url: ''
				templateUrl: 'assets/views/systems/conformance/index.html'
				controller: 'ConformanceStatementController'
				controllerAs: 'conformanceStatementCtrl'
				resolve: conformance
			'app.systems.detail.conformance.detail':
				url: '/detail/:actor_id?specId'
				templateUrl: 'assets/views/systems/conformance/detail.html'
				controller: 'ConformanceStatementDetailController'
				controllerAs: 'conformanceStatementDetailCtrl'
			'app.systems.detail.conformance.create':
				url: '/create'
				templateUrl: 'assets/views/systems/conformance/create.html'
				controller: 'CreateConformanceStatementController'
				controllerAs: 'createConformanceStatementCtrl'
			'app.systems.detail.tests':
				url: '/tests'
				templateUrl: 'assets/views/systems/tests.html'
				controller: 'SystemTestsController'
				controllerAs: 'systemTestsCtrl'
			'app.page1':
				url: '/page1'
				controller: 'SystemsController'
				controllerAs: 'sc'
				templateUrl: 'assets/views/page1.html'
			'app.page2':
				url: '/page2'
				templateUrl: 'assets/views/page2.html'
			'app.users':
				url: '/users'
				templateUrl: 'assets/views/users.html'
			'app.profile':
				url: '/profile'
				templateUrl: 'assets/views/profile.html'
			'app.settings':
				url: '/settings'
				templateUrl: 'assets/views/settings.html'
			'app.support':
				url: '/support'
				templateUrl: 'assets/views/help.html'
			'app.contact':
				url: '/contact'
				templateUrl: 'assets/views/contact.html'
			'app.about':
				url: '/about'
				templateUrl: 'assets/views/about.html'
			'app.admin':
				url: '/admin'
				templateUrl: 'assets/views/admin/index.html'
				controller: 'AdminController'
				controllerAs: 'adminCtrl'
			'app.admin.dashboard':
				url: '/dashboard'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.dashboard.list':
				url: ''
				templateUrl: 'assets/views/admin/dashboard/index.html'
				controller: 'DashboardController'
				controllerAs: 'dashboardCtrl'
			'app.admin.domains':
				url: '/domains'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.domains.list':
				url: ''
				templateUrl: 'assets/views/admin/domains/index.html'
				controller: 'AdminDomainsController'
				controllerAs: 'adminDomainsCtrl'
			'app.admin.conformance':
				url: '/conformance'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.conformance.list':
				url: ''
				templateUrl: 'assets/views/admin/conformance/index.html'
				controller: 'AdminConformanceController'
				controllerAs: 'adminConformanceCtrl'
			'app.admin.domains.create':
				url: '/create'
				templateUrl: 'assets/views/admin/domains/create.html'
				controller: 'CreateDomainController'
				controllerAs: 'createDomainCtrl'
			'app.admin.domains.detail':
				url: '/:id'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/domains/detail.html'
				controller: 'DomainDetailsController'
				controllerAs: 'domainDetailsCtrl'
			'app.admin.domains.detail.specifications':
				url: '/specifications'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.create':
				url: '/create'
				templateUrl: 'assets/views/admin/domains/create-spec.html'
				controller: 'CreateSpecificationController'
				controllerAs: 'createSpecCtrl'
			'app.admin.domains.detail.specifications.detail':
				url: '/{spec_id:[0-9]+}'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.domains.detail.specifications.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/domains/detail-spec.html'
				controller: 'SpecificationDetailsController'
				controllerAs: 'specDetailsCtrl'
			'app.admin.domains.detail.specifications.detail.actors':
				url: '/actors'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.detail.actors.create':
				url: '/create'
				templateUrl: 'assets/views/admin/domains/create-actor.html'
				controller: 'CreateActorController'
				controllerAs: 'createActorCtrl'
			'app.admin.domains.detail.specifications.detail.actors.detail':
				url: '/:actor_id'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.detail.actors.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/domains/detail-actor.html'
				controller: 'ActorDetailsController'
				controllerAs: 'actorDetailsCtrl'
			'app.admin.domains.detail.specifications.detail.actors.detail.endpoints':
				url: '/endpoints'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.detail.actors.detail.endpoints.create':
				url: '/create'
				templateUrl: 'assets/views/admin/domains/create-endpoint.html'
				controller: 'CreateEndpointController'
				controllerAs: 'createEndpointCtrl'
			'app.admin.domains.detail.specifications.detail.actors.detail.endpoints.detail':
				url: '/:endpoint_id'
				templateUrl: 'assets/views/admin/domains/detail-endpoint.html'
				controller: 'EndpointDetailsController'
				controllerAs: 'endpointDetailsCtrl'
			'app.admin.domains.detail.actors.detail.options':
				url: '/options'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.actors.detail.options.create':
				url: '/create'
				templateUrl: 'assets/views/admin/domains/create-option.html'
				controller: 'CreateOptionController'
				controllerAs: 'createOptionCtrl'
			'app.admin.domains.detail.actors.detail.options.detail':
				url: '/:option_id'
				templateUrl: 'assets/views/admin/domains/detail-option.html'
				controller: 'OptionDetailsController'
				controllerAs: 'optionDetailsCtrl'
			'app.admin.suites':
				url: '/suites'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.suites.list':
				url: ''
				templateUrl: 'assets/views/admin/suites/index.html'
				controller: 'AdminTestSuitesController'
				controllerAs: 'adminTestSuitesCtrl'
			'app.admin.suites.detail':
				url: '/:id'
				templateUrl: 'assets/views/admin/suites/detail.html'
				controller: 'AdminTestSuiteDetailsController'
				controllerAs: 'adminTestSuiteDetailsCtrl'
			'app.admin.users':
				url: '/users'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.list':
				url: ''
				templateUrl: 'assets/views/admin/users/index.html'
				controller: 'UserManagementController'
				controllerAs: 'userManagementCtrl'
			'app.admin.users.admins':
				url: '/admin'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.admins.create':
				url: '/create'
				templateUrl: 'assets/views/admin/users/admin-create.html'
				controller: 'AdminCreateController'
				controllerAs: 'adminCreateCtrl'
			'app.admin.users.admins.detail':
				url: '/:id'
				templateUrl: 'assets/views/admin/users/admin-detail.html'
				controller: 'AdminDetailController'
				controllerAs: 'adminDetailCtrl'
			'app.admin.users.communities':
				url: '/community'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.communities.create':
				url: '/create'
				templateUrl: 'assets/views/admin/users/community-create.html'
				controller: 'CommunityCreateController'
				controllerAs: 'communityCreateCtrl'
			'app.admin.users.communities.detail':
				url: '/:community_id'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.communities.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/users/community-detail.html'
				controller: 'CommunityDetailController'
				controllerAs: 'communityDetailCtrl'
			'app.admin.users.communities.detail.admins':
				url: '/admin'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.communities.detail.admins.create':
				url: '/create'
				templateUrl: 'assets/views/admin/users/community-admin-create.html'
				controller: 'CommunityAdminCreateController'
				controllerAs: 'communityAdminCreateCtrl'
			'app.admin.users.communities.detail.admins.detail':
				url: '/:admin_id'
				templateUrl: 'assets/views/admin/users/community-admin-detail.html'
				controller: 'CommunityAdminDetailController'
				controllerAs: 'communityAdminDetailCtrl'
			'app.admin.users.communities.detail.organizations':
				url: '/organization'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.communities.detail.organizations.create':
				url: '/create'
				templateUrl: 'assets/views/admin/users/organization-create.html'
				controller: 'OrganizationCreateController'
				controllerAs: 'orgCreateCtrl'
			'app.admin.users.communities.detail.organizations.detail':
				url: '/:org_id'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.users.communities.detail.organizations.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/users/organization-detail.html'
				controller: 'OrganizationDetailController'
				controllerAs: 'orgDetailCtrl'
			'app.admin.users.communities.detail.organizations.detail.users':
				url: '/users'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.users.communities.detail.organizations.detail.users.create':
				url: '/create'
				templateUrl: 'assets/views/admin/users/user-create.html'
				controller: 'UserCreateController'
				controllerAs: 'userCreateCtrl'
			'app.admin.users.communities.detail.organizations.detail.users.detail':
				url: '/:user_id'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.users.communities.detail.organizations.detail.users.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/users/user-detail.html'
				controller: 'UserDetailController'
				controllerAs: 'userDetailCtrl'
			'app.admin.users.communities.detail.landingpages':
				url: '/pages'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.communities.detail.landingpages.create':
				url: '/create?name&description&content'
				templateUrl: 'assets/views/admin/users/landing-page-create.html'
				controller: 'LandingPageCreateController'
				controllerAs: 'landingPageCreateCtrl'
			'app.admin.users.communities.detail.landingpages.detail':
				url: '/:page_id'
				templateUrl: 'assets/views/admin/users/landing-page-detail.html'
				controller: 'LandingPageDetailController'
				controllerAs: 'landingPageDetailCtrl'
			'app.admin.users.communities.detail.legalnotices':
				url: '/notices'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.communities.detail.legalnotices.create':
				url: '/create?name&description&content'
				templateUrl: 'assets/views/admin/users/legal-notice-create.html'
				controller: 'LegalNoticeCreateController'
				controllerAs: 'LegalNoticeCreateCtrl'
			'app.admin.users.communities.detail.legalnotices.detail':
				url: '/:notice_id'
				templateUrl: 'assets/views/admin/users/legal-notice-detail.html'
				controller: 'LegalNoticeDetailController'
				controllerAs: 'legalNoticeDetailCtrl'

		for state, value of states
			$stateProvider.state state, value
		return
]

app.run ['$log', '$rootScope', '$state', 'AuthProvider',
	($log, $rootScope, $state, AuthProvider) ->

		startsWith = (str, prefix) ->
			(str.indexOf prefix) == 0

		requiresLogin = (state) ->
			(state.name == 'app.settings') or
			(state.name == 'app.profile') or
			(state.name == 'app.users') or
			(state.name == 'app.home') or
			(startsWith state.name, 'app.tests') or
			(startsWith state.name, 'app.reports') or
			(startsWith state.name, 'app.systems') or
			(startsWith state.name, 'app.page') or
			(startsWith state.name, 'app.admin')

		$rootScope.$on '$stateChangeStart', (event, toState, toParams, fromState, fromParams)->
			authenticated = AuthProvider.isAuthenticated()

			$log.debug 'Starting state', toState

			if (requiresLogin toState) and not authenticated
				$log.debug 'State requires login, redirecting...'
				event.preventDefault()
				$state.go 'app.login'

		$rootScope.$on '$stateChangeError', (event, toState, toParams, fromState, fromParams, error) ->
			if error.redirectTo?
				if error.params?
					$state.go error.redirectTo, error.params
				else
					$state.go error.redirectTo

		return
]
