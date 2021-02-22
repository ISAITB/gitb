class ContactSupportController

    @$inject = ['$scope', '$uibModalInstance', 'WebEditorService', '$timeout', 'DataService', 'AccountService', 'ErrorService', 'ValidationService', 'PopupService']
    constructor: (@$scope, @$uibModalInstance, @WebEditorService, @$timeout, @DataService, @AccountService, @ErrorService, @ValidationService, @PopupService) ->
        @$uibModalInstance.opened.then(
            @$timeout(() =>
                tinymce.remove('.mce-editor-contact')
                @WebEditorService.editorMinimal(200, "", "mce-editor-contact")
                @editorReady = true
            , 1)
        )

        @maxCount = 5
        if @DataService.configuration?["email.attachments.maxCount"]?
            @maxCount = parseInt(@DataService.configuration["email.attachments.maxCount"])

        if @DataService.user?
            @contactAddress = @DataService.user.email
        @surveyAddress = @DataService.configuration?["survey.address"]
        @attachments = []
        @feedbackTypes = [
            {id: undefined, description: "--Select one--"},
            {id: 0, description: "Technical issue"},
            {id: 1, description: "Feature request"},
            {id: 2, description: "Question on usage"},
            {id: 3, description: "Other"}
        ]
        @resetState()
        @$uibModalInstance.rendered.then @DataService.focus('contact')

    resetState: () =>
        @feedback = {}
        @sendPending = false
        @alerts = []
        @attachments = []
        editor = @getRichTextEditor()
        if (editor?)
            editor.setContent("")

    getRichTextEditor:() =>
        tinymce.get('mce-editor-contact')

    sendDisabled: () =>
        !(@contactAddress? && @contactAddress.trim() != '' && @feedback? && @feedback?.id? && !@sendPending)

    showSurveyLink: () =>
        @DataService.configuration?["survey.enabled"] == true

    validateAttachments: () =>
        valid = true
        if @attachments?
            if @attachments.length > @maxCount
                @ValidationService.alerts.push({type:'danger', msg:'A maximum of '+@maxCount+' attachments can be provided'})
                valid = false
            totalSize = 0
            for attachment in @attachments
                totalSize += attachment.size
            maxSizeMBs = 5
            if @DataService.configuration?["email.attachments.maxSize"]?
                maxSizeMBs = parseInt(@DataService.configuration["email.attachments.maxSize"])
            maxSize = maxSizeMBs * 1024 * 1024
            if totalSize > maxSize
                @ValidationService.alerts.push({type:'danger', msg:'The total size of attachments cannot exceed '+maxSizeMBs+' MBs.'})
                valid = false
        valid

    send: () =>
        @ValidationService.clearAll()
        if (!@sendDisabled() & 
            @ValidationService.validateEmail(@contactAddress, "Please enter a valid email address.") &
            @ValidationService.objectNonNull(@feedback.id, "Please select the feedback type.") &
            @ValidationService.requireNonNull(@getRichTextEditor().getContent(), "Please provide a message.") &
            @validateAttachments()
        ) 
            @sendPending = true
            @AccountService.submitFeedback(@contactAddress, @feedback.id, @feedback.description, @getRichTextEditor().getContent(), @attachments)
            .then (data) =>
                if (data? && data.error_code?)
                    @ValidationService.alerts.push({type:'danger', msg:data.error_description})
                    @sendPending = false
                    @alerts = @ValidationService.getAlerts()
                else
                    @cancel()                
                    @PopupService.success("Your feedback was submitted successfully.")
            .catch (error) =>
                @sendPending = false
                @ErrorService.showErrorMessage(error)
        else
            @alerts = @ValidationService.getAlerts()

    cancel: () =>
        @$uibModalInstance.dismiss()

    closeAlert: (index) =>
        @ValidationService.clearAlert(index)
        @alerts = @ValidationService.getAlerts()

    attachFile: (files) =>
        file = _.head files
        if file?
            reader = new FileReader()
            reader.onload = (event) =>
                data = event.target.result
                @attachments.push({
                    name: file.name
                    size: file.size
                    type: file.type
                    data: data
                })
                @$scope.$apply()
            reader.readAsDataURL file

    removeAttachment: (index) =>
        @attachments.splice(index, 1)
    
    showUpload: () =>
        @maxCount > 0

@controllers.controller 'ContactSupportController', ContactSupportController
