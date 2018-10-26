class ErrorTemplateCreateController

  @$inject = ['$log', '$state', '$stateParams', '$scope', 'WebEditorService', 'ErrorTemplateService', 'ValidationService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @$scope, @WebEditorService, @ErrorTemplateService, @ValidationService, @ConfirmationDialogService, @ErrorService) ->

    @communityId = @$stateParams.community_id

    @alerts = []
    @template = {}

    @initialize()

  initialize: () ->
    @template.name = @$stateParams.name
    @template.description = @$stateParams.description
    @template.default = false
    @WebEditorService.editor(300, @$stateParams.content ? "")

  saveDisabled: () =>
    !(@template?.name?)

  # create error template and cancel screen
  createErrorTemplate: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@template.name, "Please enter a name.")
      if @template.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default error template. Are you sure?", "Yes", "No")
        .then () =>
          @doCreate()
      else
        @doCreate()
    @alerts = @ValidationService.getAlerts()

  doCreate: () ->
    @ErrorTemplateService.createErrorTemplate(@template.name, @template.description, tinymce.activeEditor.getContent(), @template.default, @communityId)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        @cancelCreateErrorTemplate()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  cancelCreateErrorTemplate: () =>
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

@controllers.controller 'ErrorTemplateCreateController', ErrorTemplateCreateController