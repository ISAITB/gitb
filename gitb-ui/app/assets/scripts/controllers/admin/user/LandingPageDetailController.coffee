class LandingPageDetailController

  @$inject = ['$log', '$state', '$stateParams', 'WebEditorService', 'ValidationService', 'LandingPageService', 'ConfirmationDialogService', 'ErrorService', 'PopupService', 'DataService']
  constructor: (@$log, @$state, @$stateParams, @WebEditorService, @ValidationService, @LandingPageService, @ConfirmationDialogService, @ErrorService, @PopupService, @DataService) ->

    @communityId = @$stateParams.community_id
    @pageId = @$stateParams.page_id
    @alerts = []
    @page = {}
    @isDefault

    @LandingPageService.getLandingPageById(@pageId)
    .then (data) =>
      @page = data
      @isDefault = data.default
      @WebEditorService.editor(300, data.content)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @DataService.focus('name')

  saveDisabled: () =>
    !(@page?.name?)

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
    @LandingPageService.updateLandingPage(@pageId, @page.name, @page.description, tinymce.activeEditor.getContent(), @page.default, @communityId)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        if copy
          @copyLandingPage()
        else
          @cancelDetailLandingPage()
          @PopupService.success('Landing page updated.')
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  copyLandingPage: () ->
    name = @page.name + " COPY"
    description = @page.description
    content = tinymce.activeEditor.getContent()
    @$state.go 'app.admin.users.communities.detail.landingpages.create', { name : name, description : description, content : content }

  deleteLandingPage: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this landing page?", "Yes", "No")
    .then () =>
      @LandingPageService.deleteLandingPage(@pageId)
      .then () =>
        @cancelDetailLandingPage()
        @PopupService.success('Landing page deleted.')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  cancelDetailLandingPage: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'LandingPageDetailController', LandingPageDetailController