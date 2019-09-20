class OrganizationDetailController

  @$inject = ['$log', '$state', '$stateParams', '$window', 'LandingPageService', 'LegalNoticeService', 'ErrorTemplateService', 'UserManagementService', 'ValidationService', 'ConfirmationDialogService', 'OrganizationService', 'UserService', 'ErrorService', '$q', 'DataService']
  constructor: (@$log, @$state, @$stateParams, @$window, @LandingPageService, @LegalNoticeService, @ErrorTemplateService, @UserManagementService, @ValidationService, @ConfirmationDialogService, @OrganizationService, @UserService, @ErrorService, @$q, @DataService) ->

    @userColumns = [
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
      {
        field: 'ssoStatusText',
        title: 'Status'
      }
    ]

    @orgId = @$stateParams.org_id
    @communityId = @$stateParams.community_id
    @organization = {}
    @landingPages = []
    @legalNotices = []
    @errorTemplates = []
    @otherOrganisations = []
    @propertyData = {
      properties: []
      edit: false
    }
    @users = []
    @alerts = []

    # get selected organization
    @OrganizationService.getOrganizationById(@orgId)
    .then (data) =>
      @organization = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @OrganizationService.getOrganisationParameterValues(@orgId)
    .then (data) =>
      @propertyData.properties = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get users of the organization
    @UserService.getUsersByOrganization(@orgId)
    .then (data) =>
      for user in data
        user.ssoStatusText = @DataService.userStatus(user.ssoStatus)
      @users = data
      @UserManagementService.mapUsers(@users)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LandingPageService.getLandingPagesByCommunity(@communityId)
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LegalNoticeService.getLegalNoticesByCommunity(@communityId)
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ErrorTemplateService.getErrorTemplatesByCommunity(@communityId)
    .then (data) =>
      @errorTemplates = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @OrganizationService.getOrganizationsByCommunity(@communityId)
    .then (data) =>
      for org in data
        if (org.id+'' != @orgId+'')
          @otherOrganisations.push(org)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # delete and cancel detail
  deleteOrganization: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this organisation?", "Yes", "No")
    .then () =>
      @OrganizationService.deleteOrganization(@orgId)
      .then () =>
        @cancelDetailOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    !(@valueDefined(@organization?.sname) && @valueDefined(@organization?.fname) && (!@DataService.configuration?['registration.enabled'] || (!@organization?.template || @valueDefined(@organization?.templateName))) && (!@propertyData.edit || @DataService.customPropertiesValid(@propertyData.properties)))

  valueDefined: (value) =>
    value? && value.trim().length > 0

  doUpdate: () =>
    @OrganizationService.updateOrganization(@orgId, @organization.sname, @organization.fname, @organization.landingPages, @organization.legalNotices, @organization.errorTemplates, @organization.otherOrganisations, @organization.template, @organization.templateName, @propertyData.edit, @propertyData.properties)
    .then (data) =>
      if data? && data.error_code?
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
        @alerts = @ValidationService.getAlerts()          
      else
        @cancelDetailOrganization()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)


  # update and cancel detail
  updateOrganization: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@organization.sname, "Please enter short name of the organisation.") &
    @ValidationService.requireNonNull(@organization.fname, "Please enter full name of the organisation.")
      if @organization.otherOrganisations? && @organization.otherOrganisations.id?
        @ConfirmationDialogService.confirm("Confirm test setup copy", "Copying the test setup from another organisation will remove current systems, conformance statements and test results. Are you sure you want to proceed?", "Yes", "No")
        .then(() =>
          @doUpdate()
        )
      else 
        @doUpdate()
    else
      @alerts = @ValidationService.getAlerts()

  # detail of selected organization
  userSelect: (user) =>
    @$state.go 'app.admin.users.communities.detail.organizations.detail.users.detail.list', { user_id : user.id }

  # cancel detail
  cancelDetailOrganization: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  # closes alert which is displayed due to an error
  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

  manageOrganizationTests: () =>
    @$window.localStorage['organization'] = angular.toJson @organization
    @$state.go 'app.systems.list'

@controllers.controller 'OrganizationDetailController', OrganizationDetailController
