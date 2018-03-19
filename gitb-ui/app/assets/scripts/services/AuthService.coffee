class AuthService

    @$inject = ['$log', 'RestService']
    constructor: (@$log, @RestService) ->
        @$log.debug "Constructing AuthService..."

    access_token: (email, password) ->
        @$log.debug "Getting access_token for credentials => email: #{email}, password: #{password}"

        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.access_token().url,
            data: {
                email: email,
                password: password,
                grant_type: 'password'
            }
        })

    checkEmail: (email) ->
        @$log.debug "Checking email availability: #{email}"

        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.checkEmail().url,
            params: {
                email: email
            }
        })

services.service('AuthService', AuthService)