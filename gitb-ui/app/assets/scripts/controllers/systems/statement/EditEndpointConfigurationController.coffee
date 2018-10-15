class EditEndpointConfigurationController

  @$inject = ['$log', '$window', 'ErrorService', 'ConfirmationDialogService', 'SystemService', 'DataService', 'Constants', '$modalInstance', 'systemId', 'endpoint', 'parameter', 'configuration']
  constructor: (@$log, @$window, @ErrorService, @ConfirmationDialogService, @SystemService, @DataService, @Constants, @$modalInstance, @systemId, @endpoint, @parameter, @oldConfiguration) ->
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

  showFileName: () =>
    @file? || @configuration?.value?

  fileName:() =>
    name = ''
    if (@file?)
      name = @file.name
    else
      if (@configuration?)
        mimeType = @DataService.mimeTypeFromDataURL(@configuration.value)
        extension = @DataService.extensionFromMimeType(mimeType)
        name = @parameter.name + extension
    name

  download:() =>
    mimeType = @DataService.mimeTypeFromDataURL(@configuration.value)
    blob = @DataService.b64toBlob(@DataService.base64FromDataURL(@configuration.value), mimeType)
    extension = @DataService.extensionFromMimeType(mimeType)
    saveAs(blob, @parameter.name+extension)

  closeDialog: () =>
    if !@oldConfiguration?
      @$modalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.ADD
    else if @configuration?.deleted
      @$modalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.DELETE
    else
      @$modalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.UPDATE

  delete: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete the value for this parameter?", "Yes", "No")
    .then () =>
      @SystemService.deleteEndpointConfiguration(@systemId, @parameter.id, @endpoint.id)
      .then () => 
        @configuration.deleted = true
        @closeDialog()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  cancel: () =>
    @$modalInstance.dismiss()
 
  save: () =>
    if @parameter.kind == "SIMPLE"
      if !@configuration.value?
        @configuration.value = ''
      @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
      .then () => @closeDialog()
      .catch (error) => @ErrorService.showErrorMessage(error)
    else if @parameter.kind == "BINARY"
      if @file?
        if @file.size >= (1024 * 64)
          error = {
            statusText: 'File upload problem',
            data: {
              error_description: 'The maximum allowed size for binary parameters is 65 KBs.'
            }
          }
          @ErrorService.showErrorMessage(error)
        else 
          reader = new FileReader()
          reader.readAsDataURL @file
          reader.onload = (event) =>
            result = event.target.result
            @configuration.value = result
            @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
            .then (metadata) => 
              if metadata?
                @configuration.extension = metadata.extension
                @configuration.mimeType = metadata.mimeType
              @closeDialog()
            .catch (error) => 
              @ErrorService.showErrorMessage(error)
          reader.onerror = (event) =>
            @$log.error "An error occurred while reading the file: ", @file, event
            @ErrorService.showErrorMessage(error)

@controllers.controller 'EditEndpointConfigurationController', EditEndpointConfigurationController
