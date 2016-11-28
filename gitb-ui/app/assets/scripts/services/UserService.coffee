class UserService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  getSystemAdministrators: () ->
    @RestService.get({
      path: jsRoutes.controllers.UserService.getSystemAdministrators().url,
      authenticate: true
    })

  getUsersByOrganization: (orgId) ->
    @RestService.get({
      path: jsRoutes.controllers.UserService.getUsersByOrganization(orgId).url,
      authenticate: true
    })

  getUserById: (userId) ->
    @RestService.get({
      path: jsRoutes.controllers.UserService.getUserById(userId).url,
      authenticate: true
    })

  updateSystemAdminProfile: (userId, name) ->
    @RestService.post({
      path: jsRoutes.controllers.UserService.updateSystemAdminProfile(userId).url,
      authenticate: true
      data: {
        user_name: name
      }
    })

  updateUserProfile: (userId, name, role) ->
    @RestService.post({
      path: jsRoutes.controllers.UserService.updateUserProfile(userId).url,
      data: {
        user_name: name
        role_id: role
      }
      authenticate: true
    })

  createSystemAdmin: (userName, userEmail, userPassword) ->
    @RestService.post({
      path: jsRoutes.controllers.UserService.createSystemAdmin().url,
      data: {
        user_name: userName,
        user_email: userEmail,
        password: userPassword
      }
      authenticate: true
    })

  createVendorUser: (userName, userEmail, userPassword, orgId, roleId) ->
    @RestService.post({
      path: jsRoutes.controllers.UserService.createUser(orgId).url,
      data: {
        user_name: userName
        user_email: userEmail
        password: userPassword
        role_id: roleId
      }
      authenticate: true
    })

  deleteSystemAdmin: (userId) ->
    @RestService.delete
      path: jsRoutes.controllers.UserService.deleteSystemAdmin(userId).url

  deleteVendorUser: (userId) ->
    @RestService.delete
      path: jsRoutes.controllers.UserService.deleteVendorUser(userId).url

services.service('UserService', UserService)