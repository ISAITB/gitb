class CommunityDetailController

  @$inject = ['$log', '$state', '$window', '$stateParams', 'UserService', 'DataService', 'Constants', 'LandingPageService', 'LegalNoticeService', 'ErrorTemplateService', 'TriggerService', 'ValidationService', 'ConfirmationDialogService', 'OrganizationService', 'CommunityService', 'ConformanceService', 'ErrorService', 'PopupService', 'community', 'WebEditorService', '$timeout']
  constructor: (@$log, @$state, @$window, @$stateParams, @UserService, @DataService, @Constants, @LandingPageService, @LegalNoticeService, @ErrorTemplateService, @TriggerService, @ValidationService, @ConfirmationDialogService, @OrganizationService, @CommunityService, @ConformanceService, @ErrorService, @PopupService, @community, @WebEditorService, @$timeout) ->

    @communityId = @community.id
    @originalDomainId = @community.domain
    @$timeout(() =>
      tinymce.remove('.mce-message')
      if @community.selfRegTokenHelpText
        @WebEditorService.editorForSingleLineInput(@community.selfRegTokenHelpText, "mce-message")
      else
        @WebEditorService.editorForSingleLineInput("", "mce-message")
    )

    @adminColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'email',
        title: 'Email'
      }
      {
        field: 'ssoStatusText',
        title: 'Status'
      }
    ]

    @organizationColumns = [
      {
        field: 'sname',
        title: 'Short name'
      }
      {
        field: 'fname',
        title: 'Full name'
      }
    ]
    if @DataService.configuration['registration.enabled']
      @organizationColumns.push({
        field: 'templateName',
        title: 'Set as template'
      })

    @landingPagesColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'default',
        title: 'Default'
      }
    ]

    @legalNoticesColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'default',
        title: 'Default'
      }
    ]

    @errorTemplatesColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'default',
        title: 'Default'
      }
    ]

    @triggerColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'eventTypeLabel',
        title: 'Event type'
      }
      {
        field: 'active',
        title: 'Active'
      }
      {
        field: 'status',
        title: 'Status'
      }
    ]


    @domains = {}
    @admins = []
    @organizations = []
    @landingPages = []
    @legalNotices = []
    @errorTemplates = []
    @triggers = []
    @alerts = []
    @triggerEventTypeMap = @DataService.idToLabelMap(@DataService.triggerEventTypes())

    @LegalNoticeService.getTestBedDefaultLegalNotice()
    .then (data) =>
      @testBedLegalNotice = data if data.exists
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LandingPageService.getCommunityDefaultLandingPage(@Constants.DEFAULT_COMMUNITY_ID)
    .then (data) =>
      @testBedLandingPage = data if data.exists
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ErrorTemplateService.getCommunityDefaultErrorTemplate(@Constants.DEFAULT_COMMUNITY_ID)
    .then (data) =>
      @testBedErrorTemplate = data if data.exists
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @UserService.getCommunityAdministrators(@communityId)
    .then (data) =>
      for admin in data
        admin.ssoStatusText = @DataService.userStatus(admin.ssoStatus)
      @admins = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    if @DataService.isSystemAdmin
      @ConformanceService.getDomains()
      .then (data) =>
        @domains = data
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

    @LandingPageService.getLandingPagesByCommunity(@communityId)
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LegalNoticeService.getLegalNoticesByCommunity(@communityId)
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ErrorTemplateService.getErrorTemplatesByCommunity(@communityId)
    .then (data) =>
      @errorTemplates = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @TriggerService.getTriggersByCommunity(@communityId)
    .then (data) =>
      for trigger in data
        trigger.eventTypeLabel = @triggerEventTypeMap[trigger.eventType]
        if trigger.latestResultOk?
          if trigger.latestResultOk
            trigger.status = 'Success'
          else
            trigger.status = 'Error'
        else
          trigger.status = '-'

      @triggers = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @OrganizationService.getOrganizationsByCommunity(@communityId)
    .then (data) =>
      @organizations = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @DataService.focus('sname')

  saveDisabled: () =>
    !(@community?.sname? && @community?.fname? && @community.sname.trim() != '' && @community.fname.trim() != '' &&
      (!@DataService.configuration?['registration.enabled'] || 
        (@community?.selfRegType == @Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED || 
          ((@community?.selfRegType == @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING || (@community?.selfRegToken?.trim().length > 0)) && 
          (!@DataService.configuration?['email.enabled'] || (!@community?.selfRegNotification || (@community?.email? && @community.email.trim() != '')))))
    ))  

  updateCommunity: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@community.sname, "Please enter the short name of the community.") &
    @ValidationService.requireNonNull(@community.fname, "Please enter the full name of the community.") &
    (!(@community.email? && @community.email.trim() != '') || @ValidationService.validateEmail(@community.email, "Please enter a valid support email.")) &
    (!@community.selfRegNotification || @ValidationService.requireNonNull(@community.email, "A support email needs to be defined to support notifications."))
      if !@community.sameDescriptionAsDomain
        descriptionToUse = @community.activeDescription
      updateCall = () => 
        @CommunityService.updateCommunity(@communityId, @community.sname, @community.fname, @community.email, @community.selfRegType, @community.selfRegRestriction, @community.selfRegToken, tinymce.activeEditor.getContent(), @community.selfRegNotification, descriptionToUse, @community.selfRegForceTemplateSelection, @community.selfRegForceRequiredProperties, @community.allowCertificateDownload, @community.allowStatementManagement, @community.allowSystemManagement, @community.allowPostTestOrganisationUpdates, @community.allowPostTestSystemUpdates, @community.allowPostTestStatementUpdates, @community.domain?.id)
          .then (data) =>
            if data? && data.error_code?
              @ValidationService.pushAlert({type:'danger', msg:data.error_description})
              @alerts = @ValidationService.getAlerts()          
            else
              @originalDomainId = @community.domain
              @PopupService.success('Community updated.')
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
      if (!@originalDomainId? && @community.domain?) || (@originalDomainId? && @community.domain? && @originalDomainId != @community.domain)
        if !@originalDomainId?
          confirmationMessage = "Setting the "+@DataService.labelDomainLower()+" will remove existing conformance statements linked to other "+@DataService.labelDomainsLower()+". Are you sure you want to proceed?"
        else
          confirmationMessage = "Changing the "+@DataService.labelDomainLower()+" will remove all existing conformance statements. Are you sure you want to proceed?"
        @ConfirmationDialogService.confirm("Confirm "+@DataService.labelDomainLower()+" change", confirmationMessage, "Yes", "No")
        .then () =>
          updateCall()
      else
        updateCall()
    else
      @alerts = @ValidationService.getAlerts()

  deleteCommunity: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this community?", "Yes", "No")
    .then () =>
      @CommunityService.deleteCommunity(@communityId)
      .then () =>
        @cancelCommunityDetail()
        @PopupService.success('Community deleted.')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  organizationSelect: (organization) =>
    @$state.go 'app.admin.users.communities.detail.organizations.detail.list', { org_id : organization.id }

  isDefaultCommunity: () =>
    (@communityId+'' == @Constants.DEFAULT_COMMUNITY_ID+'')

  landingPageSelect: (landingPage) =>
    @$state.go 'app.admin.users.communities.detail.landingpages.detail', { page_id : landingPage.id }

  legalNoticeSelect: (legalNotice) =>
    @$state.go 'app.admin.users.communities.detail.legalnotices.detail', { notice_id : legalNotice.id }

  errorTemplateSelect: (errorTemplate) =>
    @$state.go 'app.admin.users.communities.detail.errortemplates.detail', { template_id : errorTemplate.id }

  createTrigger: () =>
    @$state.go 'app.admin.users.communities.detail.triggers.create'

  triggerSelect: (trigger) =>
    @$state.go 'app.admin.users.communities.detail.triggers.detail', { trigger_id : trigger.id }

  adminSelect: (admin) =>
    @$state.go 'app.admin.users.communities.detail.admins.detail', { admin_id : admin.id }

  cancelCommunityDetail: () =>
    @$state.go 'app.admin.users.list'

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

  updateConformanceCertificateSettings: () =>
    @$state.go 'app.admin.users.communities.detail.certificate'

  updateParameters: () =>
    @$state.go 'app.admin.users.communities.detail.parameters'

  editLabels: () =>
    @$state.go 'app.admin.users.communities.detail.labels'

@controllers.controller 'CommunityDetailController', CommunityDetailController