# A class for manipulating $httpProvider to change
# the default behavior of $http service.
class AuthProvider

	@$inject = ['$httpProvider']
	constructor: (@$httpProvider) ->
		@authenticated = false

	# Overriding $get method of $httpProvider
	$get: () ->
		@ # return this

	# Sets the Authorization header with access token
	authenticate: (accessToken) ->
		@authenticated = true
		@$httpProvider.defaults.headers.common.Authorization = 'Bearer ' + accessToken

	# Removes access token information from request headers
	deauthenticate: () ->
		@authenticated = false
		delete @$httpProvider.defaults.headers.common.Authorization

	# Checks if we are authenticated or not
	isAuthenticated: () ->
		@authenticated

providers.provider('AuthProvider', AuthProvider)
providers.provider('Auth', AuthProvider)

providers.run ['$log', '$rootScope', '$location', 'AuthProvider', 'Events', 'Constants', 'DataService'
	($log, $rootScope, $location, authProvider, Events, Constants, @DataService) ->
		# check if access token is set in cookies
		atKey = Constants.ACCESS_TOKEN_COOKIE_KEY
		accessToken = $.cookie(atKey)
		if accessToken?
			authProvider.authenticate(accessToken)

		# handle login event
		$rootScope.$on Events.onLogin, (event, data) ->
			accessToken  = data.tokens.access_token
			refreshToken = data.tokens.refresh_token
			expireTimeSec= data.tokens.expires_in  # in seconds
			expireTimeDay= (expireTimeSec / Constants.SECONDS_IN_DAY) # in days
			#TODO: access token expire time info should be retrieved from server
			$log.debug "expire time: #{expireTimeDay}"

			if data.remember
				$.cookie(atKey, accessToken, { expires: Constants.TOKEN_COOKIE_EXPIRE, path: '/' })
			else
				$.cookie(atKey, accessToken, { path: '/' })

			rtKey = Constants.REFRESH_TOKEN_COOKIE_KEY
			$.cookie(rtKey, refreshToken, { expires: Constants.TOKEN_COOKIE_EXPIRE, path: '/' })

			authProvider.authenticate(accessToken)
			$rootScope.$emit(Events.afterLogin)

		# handle logout event
		$rootScope.$on Events.onLogout, () ->
			$log.debug "handling logout event..."
			@DataService.destroy()
			$.removeCookie(atKey, { path: '/' })
			authProvider.deauthenticate()
			$location.path('/login')
]
