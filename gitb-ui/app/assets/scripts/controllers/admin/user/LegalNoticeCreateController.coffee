class LegalNoticeCreateController

  @$inject = ['$log', '$state', '$stateParams', '$scope', 'WebEditorService', 'LegalNoticeService', 'ValidationService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @$scope, @WebEditorService, @LegalNoticeService, @ValidationService, @ConfirmationDialogService, @ErrorService) ->

    @alerts = []
    @notice = {}

    @initNotice()

  initNotice: () ->
    @notice.name = @$stateParams.name
    @notice.description = @$stateParams.description
    @notice.default = false
    @WebEditorService.editor(300, @$stateParams.content ? "")

  # create legal notice and cancel screen
  createLegalNotice: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@notice.name, "Please enter a name.")
      if @notice.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default legal notice. Are you sure?", "Yes", "No")
        .then () =>
          @doCreate()
      else
        @doCreate()
    @alerts = @ValidationService.getAlerts()

  doCreate: () ->
    @LegalNoticeService.createLegalNotice(@notice.name, @notice.description, tinymce.activeEditor.getContent(), @notice.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        @cancelCreateLegalNotice()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelCreateLegalNotice: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'LegalNoticeCreateController', LegalNoticeCreateController
