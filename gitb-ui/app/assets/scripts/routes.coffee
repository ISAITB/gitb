app.config ['$stateProvider', '$urlRouterProvider',
	($stateProvider, $urlRouterProvider) ->
		profile = [
			'$q', '$log', '$state', 'AuthProvider', 'AccountService', 'DataService', 'CommunityService', 'ErrorService', 
			($q, $log, $state, AuthProvider, AccountService, DataService, CommunityService, ErrorService)->
				deferred = $q.defer()
				$log.debug 'Resolving user profile..'
				authenticated = AuthProvider.isAuthenticated()
				errorService = ErrorService
				configDeferred = $q.defer()
				getConfigurationProperties = () ->
					AccountService.getConfiguration()
					.then (data) ->
						DataService.setConfiguration(data)
						configDeferred.resolve()
					.catch (error) ->
						errorService.showErrorMessage(error)
				if !DataService.configuration?
					getConfigurationProperties()
				else
					configDeferred.resolve()
				if authenticated
					userDeferred = $q.defer()
					vendorDeferred = $q.defer()
					communityDeferred = $q.defer()
					getUserProfile = () ->
						$log.debug 'Getting user profile from the server...'
						AccountService.getUserProfile()
							.then (data)->
								DataService.setUser(data)
								$log.debug 'Got user profile from the server...'
								userDeferred.resolve()
							.catch (error) ->
								errorService.showErrorMessage(error)
					getVendorProfile = () ->
						AccountService.getVendorProfile()
						.then (data) ->
							DataService.setVendor(data)
							vendorDeferred.resolve()
						.catch (error) ->
							errorService.showErrorMessage(error)

					getUserCommunity = () ->
						CommunityService.getUserCommunity()
						.then (data) ->
							DataService.setCommunity(data)
							communityDeferred.resolve()
						.catch (error) ->
							errorService.showErrorMessage(error)

					if !DataService.user?
						getUserProfile()
					else
						userDeferred.resolve()
					if !DataService.vendor?
						getVendorProfile()
					else
						vendorDeferred.resolve()
					if !DataService.community?
						getUserCommunity()
					else
						communityDeferred.resolve()
					$q.all([userDeferred.promise, vendorDeferred.promise, communityDeferred.promise, configDeferred.promise]).then(() ->
						deferred.resolve()
					)
				else
					$q.all([configDeferred.promise]).then(() ->
						if DataService.configuration['sso.enabled'] && !DataService.actualUser?
							AuthService.getUserFunctionalAccounts()
							.then (data) =>
								DataService.setActualUser(data)
								deferred.resolve()
							.catch (error) =>
								errorService.showErrorMessage(error)
						else
							deferred.resolve()
					)
				deferred.promise
		]

		system = [
			'$q', 'DataService', 'SystemService', 'ErrorService',
			($q, DataService, SystemService, ErrorService)->
				deferred = $q.defer()
				errorService = ErrorService

				if DataService.isVendorUser
					SystemService.getSystemsByOrganization(DataService.vendor.id)
					.then (data) ->
						if data.length == 1
							deferred.reject {redirectTo: 'app.systems.detail.conformance.list', params: {id: data[0].id}}
						else
							deferred.resolve()
					.catch (error) ->
						errorService.showErrorMessage(error)
				else
					deferred.resolve()

				deferred.promise
		]

		conformance = [
			'$q', '$stateParams', 'DataService', 'SystemService', 'ErrorService',
			($q, $stateParams, DataService, SystemService, Errorservice)->
				deferred = $q.defer()
				errorService = Errorservice

				if DataService.isVendorUser
					SystemService.getConformanceStatements $stateParams.id
					.then (data) ->
						if data.length == 1
							deferred.reject {redirectTo: 'app.systems.detail.conformance.detail', params: {id: $stateParams.id, actor_id: data[0].actorId, specId: data[0].specificationId}}
						else
							deferred.resolve()
					.catch (error) ->
						errorService.showErrorMessage(error)
				else
					deferred.resolve()

				deferred.promise
		]

		community = [
			'$q', '$window', 'CommunityService', 'ErrorService', '$stateParams'
			($q, $window, CommunityService, Errorservice, $stateParams) ->
				deferred = $q.defer()
				CommunityService.getCommunityById($stateParams.community_id)
				.then (data) =>
					$window.localStorage['community'] = angular.toJson data
					deferred.resolve(data)
				.catch (error) =>
					ErrorService.showErrorMessage(error)
				deferred.promise
		]

		conformanceCertificate = [
			'$q', 'ConformanceService', 'ErrorService', '$stateParams'
			($q, ConformanceService, Errorservice, $stateParams) ->
				deferred = $q.defer()
				ConformanceService.getConformanceCertificateSettings($stateParams.community_id, true)
				.then (data) =>
					if data? && data?.id?
						deferred.resolve(data)
					else
						deferred.resolve({})
				.catch (error) =>
					ErrorService.showErrorMessage(error)
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
				resolve: 
					profile: profile
			'app.login':
				url: '/login'
				templateUrl: 'assets/views/login.html'
				controller: 'LoginController'
				controllerAs: 'lc'
				resolve:
					profile: profile
			'app.onetime':
				url: '/onetime'
				templateUrl: 'assets/views/onetime-password.html'
				controller: 'PasswordController'
				controllerAs: 'pc'
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
				params: {
					id: undefined
					viewProperties: false
				}
				resolve: 
					system: system
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
				params: {
					viewProperties: false
				}
			'app.systems.detail.conformance':
				url: '/conformance'
				template: '<div ui-view/>'
				abstract: true
			'app.systems.detail.conformance.list':
				url: ''
				templateUrl: 'assets/views/systems/conformance/index.html'
				controller: 'ConformanceStatementController'
				controllerAs: 'conformanceStatementCtrl'
				resolve: 
					conformance: conformance
			'app.systems.detail.conformance.detail':
				url: '/detail/:actor_id?specId'
				templateUrl: 'assets/views/systems/conformance/detail.html'
				controller: 'ConformanceStatementDetailController'
				controllerAs: 'conformanceStatementDetailCtrl'
				params: {
					editEndpoints: false
				}
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
			'app.settings':
				url: '/settings'
				templateUrl: 'assets/views/settings/settings.html'
				controller: 'SettingsController'
				controllerAs: 'sc'
				abstract: true				
			'app.settings.profile':
				url: '/profile'
				templateUrl: 'assets/views/settings/profile.html'
				controller: 'UserProfileController'
				controllerAs: 'pc'
			'app.settings.organisation':
				url: '/organisation'
				templateUrl: 'assets/views/settings/organisation.html'
				controller: 'OrganizationController'
				controllerAs: 'oc'
				params: {
					viewProperties: false
				}
			'app.settings.password':
				url: '/password'
				templateUrl: 'assets/views/settings/password.html'
				controller: 'PasswordController'
				controllerAs: 'pc'
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
			'app.admin.export':
				url: '/export'
				templateUrl: 'assets/views/admin/importexport/export.html'
				controller: 'ExportController'
				controllerAs: 'controller'
			'app.admin.import':
				url: '/import'
				templateUrl: 'assets/views/admin/importexport/import.html'
				controller: 'ImportController'
				controllerAs: 'controller'
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
			'app.admin.domains.detail.specifications.detail.testsuites':
				url: '/testsuites'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.detail.testsuites.detail':
				url: '/:testsuite_id'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.detail.testsuites.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/domains/detail-testsuite.html'
				controller: 'TestSuiteDetailsController'
				controllerAs: 'testSuiteDetailsCtrl'
			'app.admin.domains.detail.specifications.detail.testsuites.detail.testcases':
				url: '/testcases'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.detail.testsuites.detail.testcases.detail':
				url: '/:testcase_id'
				template: '<div ui-view/>'
				abstract: true
			'app.admin.domains.detail.specifications.detail.testsuites.detail.testcases.detail.list':
				url: ''
				templateUrl: 'assets/views/admin/domains/detail-testcase.html'
				controller: 'TestCaseDetailsController'
				controllerAs: 'testCaseDetailsCtrl'
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
				resolve: {
					community: community
				}
			'app.admin.users.communities.detail.certificate':
				url: '/cert'
				templateUrl: 'assets/views/admin/users/community-detail-certificate.html'
				controller: 'CommunityCertificateController'
				controllerAs: 'communityCertificateCtrl'
				resolve: {
					settings: conformanceCertificate
				}
			'app.admin.users.communities.detail.parameters':
				url: '/params'
				templateUrl: 'assets/views/admin/users/community-detail-parameters.html'
				controller: 'CommunityParametersController'
				controllerAs: 'controller'
			'app.admin.users.communities.detail.labels':
				url: '/labels'
				templateUrl: 'assets/views/admin/users/community-detail-labels.html'
				controller: 'CommunityLabelsController'
				controllerAs: 'controller'
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
				params: {
					viewProperties: false
				}
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
			'app.admin.users.communities.detail.errortemplates':
				url: '/errortemplates'
				abstract: true
				template: '<div ui-view/>'
			'app.admin.users.communities.detail.errortemplates.create':
				url: '/create?name&description&content'
				templateUrl: 'assets/views/admin/users/error-template-create.html'
				controller: 'ErrorTemplateCreateController'
				controllerAs: 'errorTemplateCreateCtrl'
			'app.admin.users.communities.detail.errortemplates.detail':
				url: '/:template_id'
				templateUrl: 'assets/views/admin/users/error-template-detail.html'
				controller: 'ErrorTemplateDetailController'
				controllerAs: 'errorTemplateDetailCtrl'

		for state, value of states
			$stateProvider.state state, value
		return
]

app.run ['$log', '$transitions', 'AuthProvider', '$state'
	($log, $transitions, AuthProvider, $state) ->

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

		$transitions.onStart({to: requiresLogin}, (trans) -> 
			toState = trans.$to()
			$log.debug 'Starting state', toState
			authenticated = AuthProvider.isAuthenticated()
			if not authenticated
				$log.debug 'State requires login, redirecting...'
				trans.abort()
				trans.router.stateService.go 'app.login'
		)

		$transitions.onError({to: (state) -> true}, (trans) ->
			error = trans.error()
			if (error? && error.detail? && error.detail.redirectTo?)
				if error.detail.params?
					trans.router.stateService.go error.detail.redirectTo, error.detail.params
				else
					trans.router.stateService.go error.detail.redirectTo
		)

		$state.defaultErrorHandler((error) =>
			# Do not log transitionTo errors.
		)

		return
]
