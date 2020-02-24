class ErrorTemplateDetailController

  @$inject = ['$log', '$state', '$stateParams', 'WebEditorService', 'ValidationService', 'ErrorTemplateService', 'ConfirmationDialogService', 'ErrorService', 'PopupService', 'DataService']
  constructor: (@$log, @$state, @$stateParams, @WebEditorService, @ValidationService, @ErrorTemplateService, @ConfirmationDialogService, @ErrorService, @PopupService, @DataService) ->

    @communityId = @$stateParams.community_id
    @templateId = @$stateParams.template_id
    @alerts = []
    @template = {}
    @isDefault
    @DataService.focus('name')

    @ErrorTemplateService.getErrorTemplateById(@templateId)
    .then (data) =>
      @template = data
      @isDefault = data.default
      @WebEditorService.editor(300, data.content)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    !(@template?.name?)

  updateErrorTemplate: (copy) =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@template.name, "Please enter a name.")
      if !@isDefault && @template.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default error template. Are you sure?", "Yes", "No")
        .then () =>
          @doUpdate(copy)
      else
        @doUpdate(copy)
    @alerts = @ValidationService.getAlerts()

  doUpdate: (copy) ->
    @ErrorTemplateService.updateErrorTemplate(@templateId, @template.name, @template.description, tinymce.activeEditor.getContent(), @template.default, @communityId)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        if copy
          @copyErrorTemplate()
        else
          @cancelDetailErrorTemplate()
          @PopupService.success('Error template updated.')
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  copyErrorTemplate: () ->
    name = @template.name + " COPY"
    description = @template.description
    content = tinymce.activeEditor.getContent()
    @$state.go 'app.admin.users.communities.detail.errortemplates.create', { name : name, description : description, content : content }

  deleteErrorTemplate: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this error template?", "Yes", "No")
    .then () =>
      @ErrorTemplateService.deleteErrorTemplate(@templateId)
      .then () =>
        @cancelDetailErrorTemplate()
        @PopupService.success('Error template deleted.')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  cancelDetailErrorTemplate: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

  preview: () =>
    error = {
      statusText: 'Internal server error',
      data: {
        error_description: 'This is a sample error description message.'
        error_id: '0123456789'
      }
      template: tinymce.activeEditor.getContent()
    }
    @ErrorService.showErrorMessage(error)

@controllers.controller 'ErrorTemplateDetailController', ErrorTemplateDetailController