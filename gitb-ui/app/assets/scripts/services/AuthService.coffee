class AuthService

    @$inject = ['$log', 'RestService']
    constructor: (@$log, @RestService) ->
        @$log.debug "Constructing AuthService..."

    checkEmail: (email) =>
        @$log.debug "Checking email availability: #{email}"

        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.checkEmail().url,
            params: {
                email: email
            }
        })

    logout: () =>
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.logout().url,
        })

services.service('AuthService', AuthService)