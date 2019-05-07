class AuthService

    @$inject = ['$log', 'RestService']
    constructor: (@$log, @RestService) ->
        @$log.debug "Constructing AuthService..."

    access_token: (email, password) ->
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.access_token().url,
            data: {
                email: email,
                password: password
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

    logout: () ->
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.logout().url,
            authenticate: true
        })

services.service('AuthService', AuthService)