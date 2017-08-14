class LegalNoticeDetailController

  @$inject = ['$log', '$state', '$stateParams', 'WebEditorService', 'ValidationService', 'LegalNoticeService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @WebEditorService, @ValidationService, @LegalNoticeService, @ConfirmationDialogService, @ErrorService) ->

    @noticeId = @$stateParams.id
    @alerts = []
    @notice = {}
    @isDefault

    # get selected legal notice
    @LegalNoticeService.getLegalNoticeById(@noticeId)
    .then (data) =>
      @notice = data
      @isDefault = data.default
      @WebEditorService.editor(300, data.content)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateLegalNotice: (copy) =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@notice.name, "Please enter a name.")
      if !@isDefault && @notice.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default legal notice. Are you sure?", "Yes", "No")
        .then () =>
          @doUpdate(copy)
      else
        @doUpdate(copy)
    @alerts = @ValidationService.getAlerts()

  doUpdate: (copy) ->
    @LegalNoticeService.updateLegalNotice(@noticeId, @notice.name, @notice.description, tinymce.activeEditor.getContent(), @notice.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        if copy
          @copyLegalNotice()
        else
          @cancelDetailLegalNotice()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # copy legal notice info in create screen
  copyLegalNotice: () ->
    name = @notice.name + " COPY"
    description = @notice.description
    content = tinymce.activeEditor.getContent()
    @$state.go 'app.admin.users.legalnotices.create', { name : name, description : description, content : content }

  # delete and cancel detail
  deleteLegalNotice: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this legal notice?", "Yes", "No")
    .then () =>
      @LegalNoticeService.deleteLegalNotice(@noticeId)
      .then () =>
        @cancelDetailLegalNotice()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailLegalNotice: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'LegalNoticeDetailController', LegalNoticeDetailController
