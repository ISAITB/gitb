class CommunityParametersController

  @$inject = ['$state', '$stateParams', 'CommunityService', 'ErrorService', '$q', '$uibModal']
  constructor: (@$state, @$stateParams, @CommunityService, @ErrorService, @$q, @$uibModal) ->
    @communityId = @$stateParams.community_id
    @parameterTableColumns = [
      {
        field: 'name'
        title: 'Label'
      }
      {
        field: 'testKey'
        title: 'Key'
      }
      {
        field: 'desc'
        title: 'Description'
      }
      {
        field: 'kindLabel'
        title: 'Type'
      }
      {
        field: 'useLabel'
        title: 'Required'
      }
      {
        field: 'adminOnlyLabel'
        title: 'Editable'
      }
      {
        field: 'notForTestsLabel'
        title: 'In tests'
      }
      {
        field: 'inExports'
        title: 'In exports'
      }
    ]

    @organisationReservedKeys = ['fullName', 'shortName']
    @systemReservedKeys = ['fullName', 'shortName', 'version']

    @loadParameters(@CommunityService.getOrganisationParameters)
    .then((data) =>
      @organisationParameterValues = []
      for item in data
        @organisationParameterValues.push({id: item.id, name: item.name, key: item.testKey})
      @organisationParameters = data
    )
    @loadParameters(@CommunityService.getSystemParameters)
    .then((data) =>
      @systemParameterValues = []
      for item in data
        @systemParameterValues.push({id: item.id, name: item.name, key: item.testKey})
      @systemParameters = data
    )

  loadParameters: (serviceMethod) =>
    resultDeferred = @$q.defer()
    serviceMethod(@communityId)
    .then (data) =>
      for item in data
        item.kindLabel = if item.kind == 'SIMPLE' then 'Simple' else if item.kind == 'BINARY' then 'Binary' else 'Hidden'
        item.useLabel = item.use == 'R'
        item.adminOnlyLabel = !item.adminOnly
        item.notForTestsLabel = !item.notForTests
      resultDeferred.resolve(data)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      resultDeferred.reject()
    resultDeferred.promise

  addOrganisationParameter: () =>
    @addParameter('Create organisation property', @organisationParameterValues, @organisationReservedKeys, @CommunityService.createOrganisationParameter)

  addSystemParameter: () =>
    @addParameter('Create system property', @systemParameterValues, @systemReservedKeys, @CommunityService.createSystemParameter)

  addParameter: (modalTitle, existingValues, reservedKeys, createMethod) =>
    options = {
      nameLabel: 'Label'
      notForTests: true
      adminOnly: false
      hasKey: true
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
        createMethod(parameter)
        .then () =>
          @$state.go(@$state.$current, null, { reload: true });
        .catch (error) =>
          @ErrorService.showErrorMessage(error)

    , angular.noop)

  onOrganisationParameterSelect: (parameter) =>
    @onParameterSelect(parameter, 'Organisation property details', @organisationParameterValues, @organisationReservedKeys, @CommunityService.updateOrganisationParameter, @CommunityService.deleteOrganisationParameter)

  onSystemParameterSelect: (parameter) =>
    @onParameterSelect(parameter, 'System property details', @systemParameterValues, @systemReservedKeys, @CommunityService.updateSystemParameter, @CommunityService.deleteSystemParameter)

  onParameterSelect: (parameter, modalTitle, existingValues, reservedKeys, updateMethod, deleteMethod) =>
    options = {
      nameLabel: 'Label'
      hasKey: true
      modalTitle: modalTitle
      confirmMessage: 'Are you sure you want to delete this property?'
      existingValues: existingValues
      reservedKeys: reservedKeys
    }
    parameter.key = parameter.testKey
    parameter.desc = parameter.description
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
          updateMethod(data.parameter)
          .then () =>
            @$state.go(@$state.$current, null, { reload: true });
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          deleteMethod(data.parameter.id)
          .then () =>
            @$state.go(@$state.$current, null, { reload: true });
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
      , angular.noop)

  cancel: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

@controllers.controller 'CommunityParametersController', CommunityParametersController