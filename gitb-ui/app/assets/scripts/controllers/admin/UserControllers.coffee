class AdminUsersController
  name: 'AdminUsersController'

  @$inject = ['$log', '$scope', '$state', 'UserService', 'OrganizationService', 'ErrorService', 'Constants']
  constructor: (@$log, @$scope, @$state, @UserService, @OrganizationService, @ErrorService, @Constants) ->
    @$log.debug "Constructing #{@name}..."

    @adminTableColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'email',
        title: 'Email'
      }
    ]

    @userTableColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'email',
        title: 'Email'
      }
      {
        field: 'role',
        title: 'Role'
      }
    ]

    @administrators = []
    @organizations = []
    @users = []

    @UserService.getSystemAdministrators()
    .then (data) =>
      @administrators = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @OrganizationService.getOrganizations()
    .then (data) =>
      @organizations = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getUsersByOrganization: () =>
    @UserService.getUsersByOrganization(@$scope.organization.id)
    .then (data) =>
      @users = data
      labels = @Constants.USER_ROLE_LABEL
      @users.forEach((e) ->
        e.role = labels[e.role]
      )
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

@ControllerUtils.register @controllers, AdminUsersController