class UserManagementService

  @$inject = ['Constants']
  constructor: (@Constants) ->

  # maps user role as object of id and label
  mapUser: (user) ->
    user.role =
      id: user.role
      label: @Constants.USER_ROLE_LABEL[user.role]

  # maps array of users role text
  mapUsers: (users) ->
    labels = @Constants.USER_ROLE_LABEL;
    users.forEach((user) -> user.role = labels[user.role])

services.service('UserManagementService', UserManagementService)