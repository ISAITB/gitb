class ConformanceCertificateModalController

    @$inject = ['$scope', '$q', '$modalInstance', 'WebEditorService', 'settings', 'conformanceStatement', 'ConformanceService', 'ErrorService', 'Constants']
    constructor: (@$scope, @$q, @$modalInstance, @WebEditorService, @settings, @conformanceStatement, @ConformanceService, @ErrorService, @Constants) ->
        @exportPending = false
        if @settings.message? 
            # Replace the placeholders for the preview.
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__DOMAIN).join(@conformanceStatement.domainName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__SPECIFICATION).join(@conformanceStatement.specName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__ACTOR).join(@conformanceStatement.actorName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__ORGANISATION).join(@conformanceStatement.organizationName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__SYSTEM).join(@conformanceStatement.systemName)
        else 
            @settings.message = ''

        @editorReady = @$q.defer()
        tinyMCE.execCommand('mceRemoveEditor', false, 'message');
        setTimeout(() => 
            @WebEditorService.editorForPdfInput(200, @settings.message).then () =>
            @editorReady.resolve()
        , 1);
        @editorReady.promise.then () =>
                # console.log(tinymce.activeEditor)
                # tinymce.activeEditor.setContent(@settings.message)
                # console.log(tinymce.activeEditor.getContent())

    generate: () =>
        @exportPending = true
        @settings.message = tinymce.activeEditor.getContent()
        @ConformanceService.exportConformanceCertificateReport(@conformanceStatement.communityId, @conformanceStatement.actorId, @conformanceStatement.systemId, @settings)
        .then (data) =>
            blobData = new Blob([data], {type: 'application/pdf'});
            saveAs(blobData, "conformance_certificate.pdf");
            @exportPending = false
            @$modalInstance.dismiss()
        .catch (error) =>
            @ErrorService.showErrorMessage(error)
            @exportPending = false

    cancel: () =>
        @$modalInstance.dismiss()

@controllers.controller 'ConformanceCertificateModalController', ConformanceCertificateModalController
