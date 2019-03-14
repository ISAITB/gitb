class ConformanceCertificateModalController

    @$inject = ['$scope', '$timeout', '$uibModalInstance', 'WebEditorService', 'settings', 'conformanceStatement', 'ConformanceService', 'ErrorService', 'Constants', 'ReportService']
    constructor: (@$scope, @$timeout, @$uibModalInstance, @WebEditorService, @settings, @conformanceStatement, @ConformanceService, @ErrorService, @Constants, @ReportService) ->
        @exportPending = false
        @choice = @Constants.REPORT_OPTION_CHOICE.REPORT
        if @settings.message? 
            # Replace the placeholders for the preview.
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__DOMAIN).join(@conformanceStatement.domainName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__SPECIFICATION).join(@conformanceStatement.specName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__ACTOR).join(@conformanceStatement.actorName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__ORGANISATION).join(@conformanceStatement.organizationName)
            @settings.message = @settings.message.split(@Constants.PLACEHOLDER__SYSTEM).join(@conformanceStatement.systemName)
        else 
            @settings.message = ''
        @$uibModalInstance.opened.then(
            @$timeout(() =>
                tinymce.remove('.mce-message')
                @WebEditorService.editorForPdfInput(200, @settings.message, "mce-message")
            , 1)
        )

    generate: () =>
        @exportPending = true
        if @choice == @Constants.REPORT_OPTION_CHOICE.CERTIFICATE
            @settings.message = tinymce.activeEditor.getContent()
            @ConformanceService.exportConformanceCertificateReport(@conformanceStatement.communityId, @conformanceStatement.actorId, @conformanceStatement.systemId, @settings)
            .then (data) =>
                blobData = new Blob([data], {type: 'application/pdf'});
                saveAs(blobData, "conformance_certificate.pdf");
                @exportPending = false
                @$uibModalInstance.dismiss()
            .catch (error) =>
                @ErrorService.showErrorMessage(error)
                @exportPending = false
        else
            includeDetails = @choice == @Constants.REPORT_OPTION_CHOICE.DETAILED_REPORT
            @ReportService.exportConformanceStatementReport(@conformanceStatement.actorId, @conformanceStatement.systemId, includeDetails)
            .then (data) =>
                blobData = new Blob([data], {type: 'application/pdf'});
                saveAs(blobData, "conformance_report.pdf");
                @exportPending = false
                @$uibModalInstance.dismiss()
            .catch (error) =>
                @ErrorService.showErrorMessage(error)
                @exportPending = false

    cancel: () =>
        @$uibModalInstance.dismiss()

@controllers.controller 'ConformanceCertificateModalController', ConformanceCertificateModalController
