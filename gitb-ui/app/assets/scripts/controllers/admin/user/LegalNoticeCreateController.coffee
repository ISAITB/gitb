class LegalNoticeCreateController

  @$inject = ['$log', '$state', '$stateParams', '$scope', 'WebEditorService', 'LegalNoticeService', 'ValidationService', 'ConfirmationDialogService', 'ErrorService', 'PopupService', 'DataService']
  constructor: (@$log, @$state, @$stateParams, @$scope, @WebEditorService, @LegalNoticeService, @ValidationService, @ConfirmationDialogService, @ErrorService, @PopupService, @DataService) ->

    @communityId = @$stateParams.community_id

    @alerts = []
    @notice = {}

    @initialize()
    @DataService.focus('name')

  initialize: () ->
    @notice.name = @$stateParams.name
    @notice.description = @$stateParams.description
    @notice.default = false
    @WebEditorService.editor(300, @$stateParams.content ? "")

  saveDisabled: () =>
    !(@notice?.name?)

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
    @LegalNoticeService.createLegalNotice(@notice.name, @notice.description, tinymce.activeEditor.getContent(), @notice.default, @communityId)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        @cancelCreateLegalNotice()
        @PopupService.success('Legal notice created.')
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  cancelCreateLegalNotice: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'LegalNoticeCreateController', LegalNoticeCreateController