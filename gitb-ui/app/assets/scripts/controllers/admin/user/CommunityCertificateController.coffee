class CommunityCertificateController

  @$inject = ['$state', '$scope', '$stateParams', 'WebEditorService', 'Constants', 'ConformanceService', 'ErrorService', '$q', 'DataService', 'ConfirmationDialogService']
  constructor: (@$state, @$scope, @$stateParams, @WebEditorService, @Constants, @ConformanceService, @ErrorService, @$q, @DataService, @ConfirmationDialogService) ->
    @communityId = @$stateParams.community_id
    @editorReady = @$q.defer()
    tinyMCE.remove('.mce-message')
    setTimeout(() => 
        @WebEditorService.editorForPdfInput(200, "", "mce-message").then () =>
          @editorReady.resolve()
    , 1);
    
    @placeholderDomain = @Constants.PLACEHOLDER__DOMAIN
    @placeholderSpecification = @Constants.PLACEHOLDER__SPECIFICATION
    @placeholderActor = @Constants.PLACEHOLDER__ACTOR
    @placeholderOrganisation = @Constants.PLACEHOLDER__ORGANISATION
    @placeholderSystem = @Constants.PLACEHOLDER__SYSTEM
    @updatePasswords = true
    @removeKeystore = false
    @exportPending = false
    @settings = {}

    @ConformanceService.getConformanceCertificateSettings(@communityId, true)
    .then (data) =>
      if data? && data?.id?
        @settings = data
        if !@settings.passwordsSet? || !@settings.passwordsSet
          @updatePasswords = true
        else 
          @updatePasswords = false
        if @settings.message?
          @editorReady.promise.then () =>
            tinymce.activeEditor.setContent(data.message)
      else
        @settings = {}
        @updatePasswords = true
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @$scope.attachKeystore = (files) =>
      file = _.head files
      if file?
        if file.size >= (Number(@DataService.configuration['savedFile.maxSize']) * 1024)
          @ErrorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for the keystore file is '+@DataService.configuration['savedFile.maxSize']+' KBs.')
        else
          reader = new FileReader()
          reader.readAsDataURL file
          reader.onload = (event) =>
            data = event.target.result
            @settings.keystoreFile = data
            @removeKeystore = false
            @$scope.$apply()

  downloadKeystore: () =>
    base64 = @DataService.base64FromDataURL(@settings.keystoreFile)
    blob = @DataService.b64toBlob(base64, 'application/octet-stream')
    fileName = 'keystore'
    if @settings.keystoreType == 'JKS'
      fileName += '.jks'
    else if @settings.keystoreType == 'JCEKS'
      fileName += '.jceks'
    else if @settings.keystoreType == 'PKCS12'
      fileName += '.p12'
    saveAs(blob, fileName)

  clearKeystore: () =>
    @settings.keystoreFile = undefined
    @settings.keystoreType = undefined
    @settings.keystorePassword = undefined
    @settings.keyPassword = undefined
    @settings.passwordsSet = false
    @removeKeystore = true
    @updatePasswords = true

  testKeystore: () =>
    @ConformanceService.testKeystoreSettings(@communityId, @settings, @updatePasswords)
    .then(
      (result) =>
        title = "Test "
        if result? && result.problem?
          message = result.problem
          if !message.endsWith('.')
            message += '.'
          if result.level != 'error'
            title += "warning"
          else
            title += "error"
        else
          title += "success"
          message = "The keystore configuration is correct."
        @ConfirmationDialogService.notify(title, message, 'Close')
    )
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  preview: () =>
    @exportPending = true
    @settings.message = tinymce.activeEditor.getContent()
    @ConformanceService.exportDemoConformanceCertificateReport(@communityId, @settings)
    .then (data) =>
      blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "conformance_certificate.pdf");
      @exportPending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @exportPending = false

  settingsOk: () =>
    !@settings.includeSignature || @keystoreSettingsOk()

  keystoreSettingsOk: () =>
    @settings.keystoreFile? && @settings.keystoreType? && (!@updatePasswords || (@settings.keystorePassword? && @settings.keyPassword?))

  update: () =>
    @settings.message = tinymce.activeEditor.getContent()
    @ConformanceService.updateConformanceCertificateSettings(@communityId, @settings, @updatePasswords, @removeKeystore)
    .then () =>
      @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  cancel: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

@controllers.controller 'CommunityCertificateController', CommunityCertificateController