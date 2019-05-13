# A class for manipulating $httpProvider to change
# the default behavior of $http service.
class AuthProvider

	@$inject = ['$httpProvider']
	constructor: (@$httpProvider) ->
		@authenticated = false
		@logoutOngoing = false

	# Overriding $get method of $httpProvider
	$get: () ->
		@ # return this

	# Sets the Authorization header with access token
	authenticate: (accessToken) =>
		@authenticated = true
		@$httpProvider.defaults.headers.common.Authorization = 'Bearer ' + accessToken

	# Removes access token information from request headers
	deauthenticate: () =>
		@authenticated = false
		@logoutOngoing = false
		delete @$httpProvider.defaults.headers.common.Authorization

	# Checks if we are authenticated or not
	isAuthenticated: () =>
		@authenticated

providers.provider('AuthProvider', AuthProvider)
providers.provider('Auth', AuthProvider)

providers.run ['$log', '$rootScope', '$location', '$cookies', 'AuthProvider', 'Events', 'Constants', 'DataService', 'AuthService'
	($log, $rootScope, $location, $cookies, authProvider, Events, Constants, @DataService, @AuthService) =>
		# check if access token is set in cookies
		atKey = Constants.ACCESS_TOKEN_COOKIE_KEY
		accessToken = $cookies.get(atKey)
		if accessToken?
			authProvider.authenticate(accessToken)

		# handle login event
		$rootScope.$on Events.onLogin, (event, data) =>
			accessToken  = data.tokens.access_token
			cookieOptions = {}
			cookieOptions.path = data.path
			cookieOptions.samesite = 'strict'
			protocol = $location.protocol()
			if protocol? && (protocol.toLowerCase() == 'https')
				cookieOptions.secure = true
			if data.remember
				expiryDate = new Date(Date.now() + Constants.TOKEN_COOKIE_EXPIRE)
				cookieOptions.expires = expiryDate

			$cookies.put(atKey, accessToken, cookieOptions)

			authProvider.authenticate(accessToken)
			$rootScope.$emit(Events.afterLogin)

		# handle logout event
		$rootScope.$on Events.onLogout, () =>
			if !authProvider.logoutOngoing && authProvider.isAuthenticated()
				authProvider.logoutOngoing = true
				@AuthService.logout().then((data) ->
					$log.debug "Successfully signalled logout"
				)
				.catch((data) ->
					$log.debug "Failed to signal logout"
				)
				.finally(() ->
					@DataService.destroy()
					$cookies.remove(atKey)
					authProvider.deauthenticate()
					$location.path('/login')
				)
]
