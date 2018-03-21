class EditEndpointConfigurationController

  @$inject = ['$log', '$window', 'SystemService', 'DataService', 'Constants', '$modalInstance', 'systemId', 'endpoint', 'parameter', 'configuration']
  constructor: (@$log, @$window, @SystemService, @DataService, @Constants, @$modalInstance, @systemId, @endpoint, @parameter, @oldConfiguration) ->
    @$log.debug "Constructing EditEndpointConfigurationController"
    @file = null

    if !@oldConfiguration?
      @configuration =
        system: @systemId
        endpoint: @endpoint.id
        parameter: @parameter.id
    else
      @configuration = _.cloneDeep @oldConfiguration

    @isBinary = @parameter.kind == "BINARY"
    @isConfiugrationSet = @configuration.value?

  onFileSelect: (files) =>
    @file = _.head files

  closeDialog: () =>
    if !@oldConfiguration?
      @$modalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.ADD
    else
      @$modalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.UPDATE

  cancel: () =>
    @$modalInstance.dismiss()

  save: () =>
    if @parameter.kind == "SIMPLE"
      if @configuration.value?
        @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
        .then () => @closeDialog()
        .catch (error) => @$modalInstance.dismiss error
    else if @parameter.kind == "BINARY"
      if @file?
        reader = new FileReader()
        reader.readAsDataURL @file
        reader.onload = (event) =>
          result = event.target.result

          @configuration.value = result
          @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
          .then () => @closeDialog()
          .catch (error) => @$modalInstance.dismiss error
        reader.onerror = (event) =>
          @$log.error "An error occurred while reading the file: ", @file, event
          @$modalInstance.dismiss event

  canEdit: () =>
    !@DataService.isVendorUser

@controllers.controller 'EditEndpointConfigurationController', EditEndpointConfigurationController
