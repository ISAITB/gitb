class LandingPageCreateController

  @$inject = ['$log', '$state', '$stateParams', '$scope', 'WebEditorService', 'LandingPageService', 'ValidationService', 'ConfirmationDialogService', 'ErrorService', 'PopupService', 'DataService']
  constructor: (@$log, @$state, @$stateParams, @$scope, @WebEditorService, @LandingPageService, @ValidationService, @ConfirmationDialogService, @ErrorService, @PopupService, @DataService) ->

    @communityId = @$stateParams.community_id

    @alerts = []
    @page = {}

    @initialize()
    @DataService.focus('name')

  initialize: () ->
    @page.name = @$stateParams.name
    @page.description = @$stateParams.description
    @page.default = false
    @WebEditorService.editor(300, @$stateParams.content ? "")

  saveDisabled: () =>
    !(@page?.name? && @page.name.trim() != '')

  # create landing page and cancel screen
  createLandingPage: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@page.name, "Please enter a name.")
      if @page.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default landing page. Are you sure?", "Yes", "No")
        .then () =>
          @doCreate()
      else
        @doCreate()
    @alerts = @ValidationService.getAlerts()

  doCreate: () ->
    @LandingPageService.createLandingPage(@page.name, @page.description, tinymce.activeEditor.getContent(), @page.default, @communityId)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        @cancelCreateLandingPage()
        @PopupService.success('Landing page created.')
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  cancelCreateLandingPage: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'LandingPageCreateController', LandingPageCreateController