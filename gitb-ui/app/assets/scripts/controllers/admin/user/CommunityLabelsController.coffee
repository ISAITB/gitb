class CommunityLabelsController

  @$inject = ['$state', '$stateParams', 'CommunityService', 'ErrorService', 'DataService', 'Constants', 'PopupService']
  constructor: (@$state, @$stateParams, @CommunityService, @ErrorService, @DataService, @Constants, @PopupService) ->
    @communityId = @$stateParams.community_id
    @busy = false
    @labelTypeDescription = {}
    @labelTypeDescription[@Constants.LABEL_TYPE.DOMAIN] = "The set of related specifications and test suites the community will be using for testing."
    @labelTypeDescription[@Constants.LABEL_TYPE.SPECIFICATION] = "The specific named or versioned requirements that community members will be selecting to test for."
    @labelTypeDescription[@Constants.LABEL_TYPE.ACTOR] = "The specification role that community members' systems will be assigned with during testing."
    @labelTypeDescription[@Constants.LABEL_TYPE.ENDPOINT] = "The set of actor-specific configuration parameters applicable when testing against a specification."
    @labelTypeDescription[@Constants.LABEL_TYPE.ORGANISATION] = "The entity corresponding to a member of the current community."
    @labelTypeDescription[@Constants.LABEL_TYPE.SYSTEM] = "A software component, service or abstract entity that will be the subject of conformance testing."

    @CommunityService.getCommunityLabels(@communityId)
    .then (data) =>
      labelMap = @DataService.createLabels(data)
      @labels = []
      @labels.push(labelMap[@Constants.LABEL_TYPE.DOMAIN])
      @labels.push(labelMap[@Constants.LABEL_TYPE.SPECIFICATION])
      @labels.push(labelMap[@Constants.LABEL_TYPE.ACTOR])
      @labels.push(labelMap[@Constants.LABEL_TYPE.ENDPOINT])
      @labels.push(labelMap[@Constants.LABEL_TYPE.ORGANISATION])
      @labels.push(labelMap[@Constants.LABEL_TYPE.SYSTEM])
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  labelTypeLabel: (labelType) =>
    @Constants.LABEL_TYPE_LABEL[labelType]

  customChecked: (labelType) =>
    @DataService.focus('label-singular-'+labelType)

  save: () =>
    @busy = true
    @labelsToSave = []
    for label in @labels
      if label.custom
        @labelsToSave.push({
          labelType: label.labelType,
          singularForm: label.singularForm,
          pluralForm: label.pluralForm,
          fixedCase: label.fixedCase
        })
    @CommunityService.setCommunityLabels(@communityId, JSON.stringify(@labelsToSave))
    .then (data) =>
      @busy = false
      if @DataService.isCommunityAdmin || (@DataService.isSystemAdmin && Number(@communityId) == Number(@Constants.DEFAULT_COMMUNITY_ID))
        @DataService.setupLabels(@labelsToSave)
      @cancel()
      @PopupService.success('Community labels updated.')
    .catch (error) =>
      @busy = false
      @ErrorService.showErrorMessage(error)

  cancel: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  saveDisabled: () =>
    if @busy
      true
    else
      if @labels?
        for label in @labels
          if label.custom && !label.singularForm? || !label.pluralForm? || label.singularForm.trim().length == 0 || label.pluralForm.trim().length == 0
            return true
      false

@controllers.controller 'CommunityLabelsController', CommunityLabelsController