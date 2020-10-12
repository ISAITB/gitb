class TriggerManageController

  @$inject = ['$state', '$stateParams', '$q', '$uibModal', 'TriggerService', 'ConformanceService', 'CommunityService', 'ConfirmationDialogService', 'ErrorService', 'PopupService', 'DataService', 'Constants']
  constructor: (@$state, @$stateParams, @$q, @$uibModal, @TriggerService, @ConformanceService, @CommunityService, @ConfirmationDialogService, @ErrorService, @PopupService, @DataService, @Constants) ->

    @communityId = @$stateParams.community_id
    @triggerId = @$stateParams.trigger_id
    @update = @triggerId?
    @savePending = false
    @testPending = false
    @deletePending = false
    @previewPending = false
    @clearStatusPending = false
    @statusTextOk = {id: 1, msg: 'Success'}
    @statusTextError = {id: 2, msg: 'Error'}
    @statusTextUnknown = {id: 0, msg: 'None'}
    
    @alerts = []
    @trigger = {}
    @organisationParameters = []
    @systemParameters = []
    @eventTypes = @DataService.triggerEventTypes()
    @eventTypeMap = @DataService.idToLabelMap(@eventTypes)

    @dataTypes = @DataService.triggerDataTypes()
    @dataTypeMap = @DataService.idToLabelMap(@dataTypes)

    @triggerData = {
      community: {dataType: @Constants.TRIGGER_DATA_TYPE.COMMUNITY, visible: true, selected: false}
      organisation: {dataType: @Constants.TRIGGER_DATA_TYPE.ORGANISATION, visible: true, selected: false}
      system: {dataType: @Constants.TRIGGER_DATA_TYPE.SYSTEM, visible: true, selected: false}
      specification: {dataType: @Constants.TRIGGER_DATA_TYPE.SPECIFICATION, visible: true, selected: false}
      actor: {dataType: @Constants.TRIGGER_DATA_TYPE.ACTOR, visible: true, selected: false}
      organisationParameter: {dataType: @Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER, visible: true, selected: false}
      systemParameter: {dataType: @Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER, visible: true, selected: false}
      domainParameter: {dataType: @Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER, visible: true, selected: false}
    }

    @orgParamsLoaded = @$q.defer()
    @sysParamsLoaded = @$q.defer()
    @domainParamsLoaded = @$q.defer()

    @CommunityService.getOrganisationParameters(@communityId)
    .then (data) =>
      @organisationParameters = data
      @organisationParameterMap = {}
      for parameter in @organisationParameters
        parameter.selected = false
        @organisationParameterMap[parameter.id] = parameter
      if @organisationParameters.length == 0
        _.remove(@dataTypes, (current) => current.id == @Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER)
      @orgParamsLoaded.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    @CommunityService.getSystemParameters(@communityId)
    .then (data) =>
      @systemParameters = data
      @systemParameterMap = {}
      for parameter in @systemParameters
        parameter.selected = false
        @systemParameterMap[parameter.id] = parameter
      if @systemParameters.length == 0
        _.remove(@dataTypes, (current) => current.id == @Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER)
      @sysParamsLoaded.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    if @DataService.isCommunityAdmin && @DataService.community.domainId?
      @domainParameterFnResult = @ConformanceService.getDomainParameters(@domainId, false)
    else if @DataService.isSystemAdmin
      @domainParameterFnResult = @ConformanceService.getDomainParametersOfCommunity(@communityId)
    if @domainParameterFnResult?
      @domainParameterFnResult.then (data) =>
        @domainParameters = data
        @domainParameterMap = {}
        for parameter in @domainParameters
          parameter.selected = false
          @domainParameterMap[parameter.id] = parameter
        if @domainParameters.length == 0
          _.remove(@dataTypes, (current) => current.id == @Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER)
        @domainParamsLoaded.resolve()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @domainParamsLoaded.resolve()

    @$q.all([@orgParamsLoaded.promise, @sysParamsLoaded.promise, @domainParamsLoaded.promise]).then(() =>
      if @update
        @TriggerService.getTriggerById(@triggerId)
        .then (data) =>
          @trigger = data.trigger
          @trigger.event = {id: data.trigger.eventType}
          if @trigger.latestResultOk?
            if @trigger.latestResultOk
              @applyStatusValues(@statusTextOk)
            else
              @applyStatusValues(@statusTextError)
          else
            @applyStatusValues(@statusTextUnknown)
          for item in data.data
            if item.dataType == @Constants.TRIGGER_DATA_TYPE.COMMUNITY
              @triggerData.community.selected = true
            else if item.dataType == @Constants.TRIGGER_DATA_TYPE.ORGANISATION
              @triggerData.organisation.selected = true
            else if item.dataType == @Constants.TRIGGER_DATA_TYPE.SYSTEM
              @triggerData.system.selected = true
            else if item.dataType == @Constants.TRIGGER_DATA_TYPE.SPECIFICATION
              @triggerData.specification.selected = true
            else if item.dataType == @Constants.TRIGGER_DATA_TYPE.ACTOR
              @triggerData.actor.selected = true
            else if item.dataType == @Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER
              @triggerData.organisationParameter.selected = true
              if @domainParameterMap[item.dataId]?
                @organisationParameterMap[item.dataId].selected = true
            else if item.dataType == @Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER
              @triggerData.systemParameter.selected = true
              if @domainParameterMap[item.dataId]?
                @systemParameterMap[item.dataId].selected = true
            else if item.dataType == @Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER
              @triggerData.domainParameter.selected = true
              if @domainParameterMap[item.dataId]?
                @domainParameterMap[item.dataId].selected = true
          @eventTypeChanged()
        .catch (error) =>
          @ErrorService.showErrorMessage(error)
      else
        @eventTypeChanged()
    )
    @DataService.focus('name')

  saveDisabled: () =>
    !(
      !@savePending && !@deletePending && @trigger.name? && @trigger.name.trim() != '' && @trigger.url? && @trigger.url.trim() != '' && @trigger.event?
    )

  deleteDisabled: () =>
    !(
      !@savePending && !@deletePending
    )

  previewDisabled: () =>
    @saveDisabled() || @previewPending

  clearStatusDisabled: () =>
    @saveDisabled() || @clearStatusPending

  dataItemsToSave: () => 
    dataItems = []
    if @triggerData.community.visible && @triggerData.community.selected
      dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.COMMUNITY, dataId: -1})
    if @triggerData.organisation.visible && @triggerData.organisation.selected
      dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.ORGANISATION, dataId: -1})
    if @triggerData.system.visible && @triggerData.system.selected
      dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.SYSTEM, dataId: -1})
    if @triggerData.specification.visible && @triggerData.specification.selected
      dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.SPECIFICATION, dataId: -1})
    if @triggerData.actor.visible && @triggerData.actor.selected
      dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.ACTOR, dataId: -1})
    if @triggerData.organisationParameter.visible && @triggerData.organisationParameter.selected
      for parameter in @organisationParameters
        if parameter.selected
          dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER, dataId: parameter.id})
    if @triggerData.systemParameter.visible && @triggerData.systemParameter.selected
      for parameter in @systemParameters
        if parameter.selected
          dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER, dataId: parameter.id})
    if @triggerData.domainParameter.visible && @triggerData.domainParameter.selected
      for parameter in @domainParameters
        if parameter.selected
          dataItems.push({dataType: @Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER, dataId: parameter.id})
    dataItems

  save: () =>
    @clearAlerts()
    if @update
      callResult = @TriggerService.updateTrigger(@triggerId, @trigger.name, @trigger.description, @trigger.operation, @trigger.active, @trigger.url, @trigger.event.id, @communityId, @dataItemsToSave())
    else 
      callResult = @TriggerService.createTrigger(@trigger.name, @trigger.description, @trigger.operation, @trigger.active, @trigger.url, @trigger.event.id, @communityId, @dataItemsToSave())
    callResult
      .then (data) =>
        @savePending = false
        if data? && data.error_code
          @alerts.push({type:'danger', msg:data.error_description})
        else
          if @update
            @PopupService.success('Trigger updated.')
          else
            @back()
            @PopupService.success('Trigger created.')
      .catch (error) =>
        @savePending = false
        @ErrorService.showErrorMessage(error)

  delete: () =>
    @clearAlerts()
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this trigger?", "Yes", "No")
    .then () =>
      @deletePending = true
      @TriggerService.deleteTrigger(@triggerId)
      .then () =>
        @deletePending = false
        @back()
        @PopupService.success('Trigger deleted.')
      .catch (error) =>
        @deletePending = false
        @ErrorService.showErrorMessage(error)

  back: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  testEndpoint:() =>
    @testPending = true
    @TriggerService.testTriggerEndpoint(@trigger.url, @communityId)
    .then (result) =>
      if result.success
        modalOptions =
          templateUrl: 'assets/views/components/editor-modal.html'
          controller: 'EditorModalController as editorModalCtrl'
          resolve:
            name: () => 'Test success'
            editorOptions: () =>
              value: result.texts[0]
              readOnly: true
              copy: false
              lineNumbers: true
              smartIndent: false
              electricChars: false
            indicators: () => []
            lineNumber: () => []
          size: 'lg'
        @$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)
      else
        @popupErrorsArray(result.texts)
      @testPending = false
    .catch (error) =>
      @testPending = false
      @ErrorService.showErrorMessage(error)

  preview:() =>
    @previewPending = true
    @TriggerService.preview(@trigger.operation, @dataItemsToSave(), @communityId)
    .then (result) =>
      modalOptions =
        templateUrl: 'assets/views/components/editor-modal.html'
        controller: 'EditorModalController as editorModalCtrl'
        resolve:
          name: () => 'Sample service call'
          editorOptions: () =>
            value: result.message
            readOnly: true
            copy: true
            lineNumbers: true
            smartIndent: false
            electricChars: false
          indicators: () => []
          lineNumber: () => []
        size: 'lg'
      @$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)
      @previewPending = false
    .catch (error) =>
      @previewPending = false
      @ErrorService.showErrorMessage(error)

  clearStatus: () =>
    @clearStatusPending = true
    @TriggerService.clearStatus(@trigger.id)
    .then (result) =>
      @clearStatusPending = false
      @trigger.latestResultOk = undefined
      @applyStatusValues(@statusTextUnknown)
      @PopupService.success('Trigger status cleared.')
    .catch (error) =>
      @clearStatusPending = false
      @ErrorService.showErrorMessage(error)

  popupErrors: (errorJson) =>
    arrayToUse
    if errorJson?
      output = JSON.parse(errorJson)
      arrayToUse = output.texts
    @popupErrorsArray(arrayToUse)

  popupErrorsArray: (errorArray) =>
    content = ''
    if errorArray?
      counter = -1
      padding = 4
      for text in errorArray
        if counter == -1
          content += text
        else
          content += ('\n'+(' '.repeat(counter*padding))+'|\n')
          content += (' '.repeat(counter*padding)+'+-- ' + text)
        counter += 1
    modalOptions =
      templateUrl: 'assets/views/components/editor-modal.html'
      controller: 'EditorModalController as editorModalCtrl'
      resolve:
        name: () => 'Error messages'
        editorOptions: () =>
          value: content
          readOnly: true
          copy: true
          lineNumbers: false
          smartIndent: false
          electricChars: false
          styleClass: 'editor-short'
        indicators: () => []
        lineNumber: () => []
      size: 'lg'
    @$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)

  viewLatestErrors: () =>
    @popupErrors(@trigger.latestResultOutput)

  applyStatusValues: (statusToApply) =>
    @trigger.status = statusToApply.id
    @trigger.statusText = statusToApply.msg

  clearAlerts: () =>
    @alerts = []

  closeAlert: (index) =>
    @alerts.splice(index, 1)

  dataTypeChanged: (dataItem) =>
    dataItem.dataId = undefined

  eventTypeChanged: () =>
    if @trigger? && @trigger.event?
      eventType = @trigger.event.id
    @triggerData.community.visible = eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.community.dataType)
    @triggerData.organisation.visible = eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.organisation.dataType)
    @triggerData.system.visible = eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.system.dataType)
    @triggerData.specification.visible = eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.specification.dataType)
    @triggerData.actor.visible = eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.actor.dataType)
    @triggerData.organisationParameter.visible = @organisationParameters.length > 0 && (eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.organisationParameter.dataType))
    @triggerData.systemParameter.visible = @systemParameters.length > 0 && (eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.systemParameter.dataType))
    @triggerData.domainParameter.visible = @domainParameters.length > 0 && (eventType == undefined || @DataService.triggerDataTypeAllowedForEvent(eventType, @triggerData.domainParameter.dataType))

  parameterType: (parameter) =>
    parameter.kindLabel = if parameter.kind == 'SIMPLE' then 'Simple' else if parameter.kind == 'BINARY' then 'Binary' else 'Secret'

@controllers.controller 'TriggerManageController', TriggerManageController
