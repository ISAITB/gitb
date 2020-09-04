class CommunityCreateController

  @$inject = ['$log', '$state', 'ValidationService', 'CommunityService', 'ConformanceService', 'ErrorService', 'Constants', 'DataService', 'PopupService', 'WebEditorService', '$timeout']
  constructor: (@$log, @$state, @ValidationService, @CommunityService, @ConformanceService, @ErrorService, @Constants, @DataService, @PopupService, @WebEditorService, @$timeout) ->

    @alerts = []
    @community = {}
    @community.selfRegType = @Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED
    @community.selfRegRestriction = @Constants.SELF_REGISTRATION_RESTRICTION.NO_RESTRICTION
    @community.allowCertificateDownload = false
    @community.allowSystemManagement = true
    @community.allowStatementManagement = true
    @domains = []
    @DataService.focus('sname')

    @$timeout(() =>
      tinymce.remove('.mce-message')
      @WebEditorService.editorForSingleLineInput("", "mce-message")
    )

    @ConformanceService.getDomains()
    .then (data) =>
      @domains = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    !(@community?.sname? && @community?.fname? && 
    (!@DataService.configuration?['registration.enabled'] || 
      ((@community?.selfRegType == @Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED || 
        @community?.selfRegType == @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING || 
        (@community?.selfRegToken?.trim().length > 0)) && 
        (!@DataService.configuration?['email.enabled'] || 
        (!@community?.selfRegNotification || @community?.email? && @community.email.trim() != '')))
    ))

  createCommunity: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@community.sname, "Please enter short name of the community.") &
    @ValidationService.requireNonNull(@community.fname, "Please enter full name of the community.") &
    (!(@community.email? && @community.email.trim() != '') || @ValidationService.validateEmail(@community.email, "Please enter a valid support email.")) &
    (!@community.selfRegNotification || @ValidationService.requireNonNull(@community.email, "A support email needs to be defined to support notifications."))
      if !@community.sameDescriptionAsDomain
        descriptionToUse = @community.activeDescription
      @CommunityService.createCommunity @community.sname, @community.fname, @community.email, @community.selfRegType, @community.selfRegRestriction, @community.selfRegToken, tinymce.activeEditor.getContent(), @community.selfRegNotification, descriptionToUse, @community.selfRegForceTemplateSelection, @community.selfRegForceRequiredProperties, @community.allowCertificateDownload, @community.allowStatementManagement, @community.allowSystemManagement, @community.domain?.id
      .then (data) =>
        if data? && data.error_code?
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
          @alerts = @ValidationService.getAlerts()          
        else
          @cancelCreateCommunity()
          @PopupService.success('Community created.')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  cancelCreateCommunity: () =>
    @$state.go 'app.admin.users.list'

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'CommunityCreateController', CommunityCreateController