dependencies = [
	'ngCookies'
	'ngResource'
	'ui.bootstrap'
	'ui.bootstrap.modal'
	'ngSanitize'
	'ngAnimate'
	'ui.bootstrap.dropdown'
	'app.common'
	'app.models'
	'app.providers'
	'app.services'
	'app.controllers'
	'app.directives'
	'app.filters'
	'ui.router'
	'angularFileUpload'
	'isteven-multi-select'
	'daterangepicker'
]

@app = angular.module 'app', dependencies
@common = angular.module 'app.common', []
@models = angular.module 'app.models', []
@providers = angular.module 'app.providers', []
@services = angular.module 'app.services', []
@controllers = angular.module 'app.controllers', []
@directives = angular.module 'app.directives', []
@filters = angular.module 'app.filters', []
