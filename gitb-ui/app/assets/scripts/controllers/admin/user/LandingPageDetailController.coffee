class LandingPageDetailController

  @$inject = ['$log', '$state', '$stateParams', 'WebEditorService', 'ValidationService', 'LandingPageService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @WebEditorService, @ValidationService, @LandingPageService, @ConfirmationDialogService, @ErrorService) ->

    @pageId = @$stateParams.id
    @alerts = []
    @page = {}
    @isDefault

    # get selected landing page
    @LandingPageService.getLandingPageById(@pageId)
    .then (data) =>
      @page = data
      @isDefault = data.default
      @WebEditorService.editor(300, data.content)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateLandingPage: (copy) =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@page.name, "Please enter a name.")
      if !@isDefault && @page.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default landing page. Are you sure?", "Yes", "No")
        .then () =>
          @doUpdate(copy)
      else
        @doUpdate(copy)
    @alerts = @ValidationService.getAlerts()

  doUpdate: (copy) ->
    @LandingPageService.updateLandingPage(@pageId, @page.name, @page.description, tinymce.activeEditor.getContent(), @page.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        if copy
          @copyLandingPage()
        else
          @cancelDetailLandingPage()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # copy landing page info in create screen
  copyLandingPage: () ->
    name = @page.name + " COPY"
    description = @page.description
    content = tinymce.activeEditor.getContent()
    @$state.go 'app.admin.users.landingpages.create', { name : name, description : description, content : content }

  # delete and cancel detail
  deleteLandingPage: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this landing page?", "Yes", "No")
    .then () =>
      @LandingPageService.deleteLandingPage(@pageId)
      .then () =>
        @cancelDetailLandingPage()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailLandingPage: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'LandingPageDetailController', LandingPageDetailController
