class EditEndpointConfigurationController

  @$inject = ['$log', '$window', 'ErrorService', 'ConfirmationDialogService', 'SystemService', 'DataService', 'Constants', '$uibModalInstance', 'systemId', 'endpoint', 'parameter', 'configuration', 'PopupService']
  constructor: (@$log, @$window, @ErrorService, @ConfirmationDialogService, @SystemService, @DataService, @Constants, @$uibModalInstance, @systemId, @endpoint, @parameter, @oldConfiguration, @PopupService) ->
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
    @isConfigurationSet = @configuration.value?

    if !@isBinary
      @$uibModalInstance.rendered.then () => @DataService.focus('value')

  onFileSelect: (files) =>
    tempFile = _.head files
    if tempFile?
      if tempFile.size >= (Number(@DataService.configuration['savedFile.maxSize']) * 1024)
        @ErrorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for files is '+@DataService.configuration['savedFile.maxSize']+' KBs.')
      else
        @file = tempFile

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
    if @configuration.deleted
      @$uibModalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.DELETE
    else
      if !@oldConfiguration?
        @$uibModalInstance.close
          configuration: @configuration
          operation: @Constants.OPERATION.ADD
      else
        @$uibModalInstance.close
          configuration: @configuration
          operation: @Constants.OPERATION.UPDATE

  delete: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete the value for this parameter?", "Yes", "No")
    .then () =>
      @SystemService.deleteEndpointConfiguration(@systemId, @parameter.id, @endpoint.id)
      .then () => 
        @configuration.deleted = true
        @closeDialog()
        @PopupService.success('Parameter deleted.')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  cancel: () =>
    @$uibModalInstance.dismiss()
 
  save: () =>
    @configuration.configured = true
    if @parameter.kind == "SIMPLE"
      if !@configuration.value?
        @configuration.value = ''
      @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
      .then () => 
        @closeDialog()
        @PopupService.success('Parameter updated.')
      .catch (error) => @ErrorService.showErrorMessage(error)
    else if @parameter.kind == "SECRET"
      if !@configuration.value?
        configurationValue = ''
      else
        configurationValue = @configuration.value
      if !@configuration.valueConfirm?
        configurationValueConfirm = ''
      else
        configurationValueConfirm = @configuration.valueConfirm
      if configurationValue != configurationValueConfirm
        @ErrorService.showSimpleErrorMessage('Invalid value', 'The provided value and its confirmation don\'t match.')
      else
        @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
        .then () => 
          @configuration.value = undefined
          @configuration.valueConfirm = undefined
          @closeDialog()
          @PopupService.success('Parameter updated.')
        .catch (error) => @ErrorService.showErrorMessage(error)
    else if @parameter.kind == "BINARY"
      if @file?
          reader = new FileReader()
          reader.readAsDataURL @file
          reader.onload = (event) =>
            result = event.target.result
            @configuration.value = result
            @SystemService.saveEndpointConfiguration @endpoint.id, @configuration, true
            .then (metadata) => 
              if metadata?
                @configuration.extension = metadata.extension
                @configuration.mimeType = metadata.mimeType
              @closeDialog()
              @PopupService.success('Parameter updated.')
            .catch (error) => 
              @ErrorService.showErrorMessage(error)
          reader.onerror = (event) =>
            @$log.error "An error occurred while reading the file: ", @file, event
            @ErrorService.showErrorMessage(error)

@controllers.controller 'EditEndpointConfigurationController', EditEndpointConfigurationController
