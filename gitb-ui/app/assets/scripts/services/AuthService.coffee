class AuthService

    @$inject = ['$log', 'RestService']
    constructor: (@$log, @RestService) ->
        @$log.debug "Constructing AuthService..."

    checkEmail: (email) =>
        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.checkEmail().url,
            params: {
                email: email
            }
        })

    checkEmailOfSystemAdmin: (email) =>
        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.checkEmailOfSystemAdmin().url,
            params: {
                email: email
            }
        })

    checkEmailOfCommunityAdmin: (email, communityId) =>
        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.checkEmailOfCommunityAdmin().url,
            params: {
                email: email
                community_id: communityId
            }
        })

    checkEmailOfOrganisationUser: (email, orgId, roleId) =>
        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.checkEmailOfOrganisationUser().url,
            params: {
                email: email
                organization_id: orgId
                role_id: roleId
            }
        })

    checkEmailOfOrganisationMember: (email) =>
        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.checkEmailOfOrganisationMember().url,
            params: {
                email: email
            }
        })

    logout: (fullLogout) =>
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.logout().url,
            data: {
                full: fullLogout
            }
        })

    loginToDemo: () =>
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.loginToDemo().url
        })

    getUserFunctionalAccounts: () =>
        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.getUserFunctionalAccounts().url
        })

    getUserUnlinkedFunctionalAccounts: () =>
        @RestService.get({
            path: jsRoutes.controllers.AuthenticationService.getUserUnlinkedFunctionalAccounts().url
        })

    disconnectFunctionalAccount: (option) =>
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.disconnectFunctionalAccount().url
            data: {
                type: option
            }
        })
    
    linkFunctionalAccount: (accountId) =>
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.linkFunctionalAccount().url,
            data : {
                id: accountId
            }
        })

    migrateFunctionalAccount: (email, password) =>
        @RestService.post({
            path: jsRoutes.controllers.AuthenticationService.migrateFunctionalAccount().url,
            data : {
                email: email,
                password: password
            }
        })

services.service('AuthService', AuthService)