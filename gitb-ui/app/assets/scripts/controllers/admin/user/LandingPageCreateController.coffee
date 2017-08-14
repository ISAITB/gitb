class LandingPageCreateController

  @$inject = ['$log', '$state', '$stateParams', '$scope', 'WebEditorService', 'LandingPageService', 'ValidationService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @$scope, @WebEditorService, @LandingPageService, @ValidationService, @ConfirmationDialogService, @ErrorService) ->

    @alerts = []
    @page = {}

    @initPage()

  initPage: () ->
    @page.name = @$stateParams.name
    @page.description = @$stateParams.description
    @page.default = false
    @WebEditorService.editor(300, @$stateParams.content ? "")

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
    @LandingPageService.createLandingPage(@page.name, @page.description, tinymce.activeEditor.getContent(), @page.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        @cancelCreateLandingPage()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelCreateLandingPage: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'LandingPageCreateController', LandingPageCreateController
