class CommunityParametersController

  @$inject = ['$state', '$stateParams', 'CommunityService', 'ErrorService', '$q', '$uibModal', 'DataService', 'PopupService']
  constructor: (@$state, @$stateParams, @CommunityService, @ErrorService, @$q, @$uibModal, @DataService, @PopupService) ->
    @communityId = @$stateParams.community_id
    @orderOrganisationParametersDisabled = {value: true}
    @orderSystemParametersDisabled = {value: true}
    @organisationReservedKeys = ['fullName', 'shortName']
    @systemReservedKeys = ['fullName', 'shortName', 'version']

    @loadParameters(@CommunityService.getOrganisationParameters)
    .then((data) =>
      @organisationParameterValues = []
      for item in data
        itemRef = {id: item.id, name: item.name, key: item.testKey, kind: item.kind}
        itemRef.hasPresetValues = false
        if item.allowedValues?
          itemRef.presetValues = JSON.parse(item.allowedValues)
          itemRef.hasPresetValues = itemRef.presetValues?.length > 0
        @organisationParameterValues.push itemRef
      @organisationParameters = data
    )
    @loadParameters(@CommunityService.getSystemParameters)
    .then((data) =>
      @systemParameterValues = []
      for item in data
        itemRef = {id: item.id, name: item.name, key: item.testKey, kind: item.kind}
        itemRef.hasPresetValues = false
        if item.allowedValues?
          itemRef.presetValues = JSON.parse(item.allowedValues)
          itemRef.hasPresetValues = itemRef.presetValues?.length > 0
        @systemParameterValues.push itemRef
      @systemParameters = data
    )

  previewParameters: (title, parameters, hasRegistrationCase) =>
    modalOptions =
      templateUrl: 'assets/views/admin/users/preview-parameters-modal.html'
      controller: 'PreviewParametersModalController as controller'
      size: 'lg'
      resolve:
        modalTitle: () => title
        parameters: () => parameters
        hasRegistrationCase: () => hasRegistrationCase
    modalInstance = @$uibModal.open(modalOptions)
    modalInstance.result
      .finally(angular.noop)
      .then(angular.noop, angular.noop)

  previewOrganisationParameters: () =>
    @previewParameters(@DataService.labelOrganisation()+" property form preview", @organisationParameters, true)

  previewSystemParameters: () =>
    @previewParameters(@DataService.labelSystem()+" property form preview", @systemParameters, false)

  orderOrganisationParameters: () =>
    ids = []
    for param in @organisationParameters
      ids.push param.id
    @CommunityService.orderOrganisationParameters(@communityId, ids)
    .then () =>
      @PopupService.success('Property ordering saved.')
      @orderOrganisationParametersDisabled.value = true
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @orderOrganisationParametersDisabled.value = true

  orderSystemParameters: () =>
    ids = []
    for param in @systemParameters
      ids.push param.id
    @CommunityService.orderSystemParameters(@communityId, ids)
    .then () =>
      @PopupService.success('Property ordering saved.')
      @orderSystemParametersDisabled.value = true
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @orderSystemParametersDisabled.value = true

  movePropertyUp: (properties, disabledFlag, index) =>
    item = properties.splice(index, 1)[0]
    disabledFlag.value = false
    properties.splice(index-1, 0, item)

  movePropertyDown: (properties, disabledFlag, index) =>
    item = properties.splice(index, 1)[0]
    disabledFlag.value = false
    properties.splice(index+1, 0, item)

  loadParameters: (serviceMethod) =>
    resultDeferred = @$q.defer()
    serviceMethod(@communityId)
    .then (data) =>
      for item in data
        item.kindLabel = if item.kind == 'SIMPLE' then 'Simple' else if item.kind == 'BINARY' then 'Binary' else 'Secret'
        item.useLabel = item.use == 'R'
        item.adminOnlyLabel = !item.adminOnly
        item.notForTestsLabel = !item.notForTests
      resultDeferred.resolve(data)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      resultDeferred.reject()
    resultDeferred.promise

  addOrganisationParameter: () =>
    @addParameter('Create '+@DataService.labelOrganisationLower()+' property', @organisationParameterValues, @organisationReservedKeys, @CommunityService.createOrganisationParameter, @DataService.labelOrganisation(), false)

  addSystemParameter: () =>
    @addParameter('Create '+@DataService.labelSystemLower()+' property', @systemParameterValues, @systemReservedKeys, @CommunityService.createSystemParameter, @DataService.labelSystem(), true)

  addParameter: (modalTitle, existingValues, reservedKeys, createMethod, propertyLabel, hideInRegistration) =>
    options = {
      nameLabel: 'Label'
      notForTests: true
      adminOnly: false
      hasKey: true
      hideInRegistration: hideInRegistration
      modalTitle: modalTitle
      confirmMessage: 'Are you sure you want to delete this property?'
      existingValues: existingValues
      reservedKeys: reservedKeys
    }
    modalOptions =
      templateUrl: 'assets/views/admin/domains/create-parameter-modal.html'
      controller: 'CreateParameterController as CreateParameterController'
      size: 'lg'
      resolve:
        options: () => options
    modalInstance = @$uibModal.open(modalOptions)
    modalInstance.result
      .finally(angular.noop)
      .then((parameter) => 
        parameter.testKey = parameter.key
        parameter.desc = parameter.description
        parameter.community = @communityId
        @preparePresetValues(parameter)
        createMethod(parameter)
        .then () =>
          @$state.go(@$state.$current, null, { reload: true });
          @PopupService.success(propertyLabel + ' property created.')
        .catch (error) =>
          @ErrorService.showErrorMessage(error)

    , angular.noop)

  preparePresetValues: (parameter) =>
    parameter.allowedValues = undefined
    if parameter.kind == 'SIMPLE' && parameter.hasPresetValues
      checkedValues = []
      for value in parameter.presetValues
        existingValue = _.find(checkedValues, (v) => v.value == value.value)
        if existingValue == undefined
          checkedValues.push({value: value.value, label: value.label})
      if checkedValues.length > 0
        parameter.allowedValues = JSON.stringify(checkedValues)

  onOrganisationParameterSelect: (parameter) =>
    @onParameterSelect(parameter, @organisationParameterValues, @organisationReservedKeys, @CommunityService.updateOrganisationParameter, @CommunityService.deleteOrganisationParameter, @DataService.labelOrganisation(), false)

  onSystemParameterSelect: (parameter) =>
    @onParameterSelect(parameter, @systemParameterValues, @systemReservedKeys, @CommunityService.updateSystemParameter, @CommunityService.deleteSystemParameter, @DataService.labelSystem(), true)

  onParameterSelect: (parameter, existingValues, reservedKeys, updateMethod, deleteMethod, propertyLabel, hideInRegistration) =>
    options = {
      nameLabel: 'Label'
      hasKey: true
      hideInRegistration: hideInRegistration
      modalTitle: propertyLabel + ' property details'
      confirmMessage: 'Are you sure you want to delete this property?'
      existingValues: existingValues
      reservedKeys: reservedKeys
    }
    parameter.key = parameter.testKey
    modalOptions =
      templateUrl: 'assets/views/admin/domains/detail-parameter-modal.html'
      controller: 'ParameterDetailsController as ParameterDetailsController'
      resolve:
        parameter: () => parameter
        options: () => options
      size: 'lg'
    modalInstance = @$uibModal.open(modalOptions)
    modalInstance.result
      .finally(angular.noop)
      .then((data) => 
        if data.action == 'update'
          data.parameter.testKey = data.parameter.key
          data.parameter.description = data.parameter.desc
          data.parameter.community = @communityId
          @preparePresetValues(data.parameter)
          updateMethod(data.parameter)
          .then () =>
            @$state.go(@$state.$current, null, { reload: true });
            @PopupService.success(propertyLabel + ' property updated.')
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          deleteMethod(data.parameter.id)
          .then () =>
            @$state.go(@$state.$current, null, { reload: true });
            @PopupService.success(propertyLabel + ' property deleted.')
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
      , angular.noop)

  cancel: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

@controllers.controller 'CommunityParametersController', CommunityParametersController