class ContactSupportController

  @$inject = ['$scope', '$modalInstance', 'WebEditorService', '$timeout', 'DataService', 'AccountService', 'ErrorService', 'ValidationService']
  constructor: (@$scope, @$modalInstance, @WebEditorService, @$timeout, @DataService, @AccountService, @ErrorService, @ValidationService) ->
    @$modalInstance.opened.then(
        @$timeout(() =>
            @WebEditorService.editorMinimal(200, "", "mce-editor-contact")
            @$scope.editorReady = true
        , 1)
    )
    @$scope.contactAddress = @DataService.user.email
    @$scope.surveyAddress = @DataService.configuration?["survey.address"]
    @$scope.attachments = []
    @$scope.feedbackTypes = [
        {id: 0, description: "Technical issue"},
        {id: 1, description: "Feature request"},
        {id: 2, description: "Question on usage"},
        {id: 3, description: "Other"}
    ]

    @$scope.resetState = () =>
        @$scope.feedback = {}
        @$scope.sendPending = false
        @$scope.alerts = []
        @$scope.attachments = []

        editor = @$scope.getRichTextEditor()
        if (editor?)
            editor.setContent("")

    @$scope.getRichTextEditor = () =>
        tinymce.get('editor-contact')

    @$scope.resetState()

    @$scope.sendDisabled = () =>
        !(@$scope.contactAddress? && @$scope.feedback? && @$scope.feedback?.id? && !@$scope.sendPending)

    @$scope.showSurveyLink = () =>
        @DataService.configuration?["survey.enabled"] == 'true'

    @$scope.validateAttachments = () =>
        valid = true
        if @$scope.attachments?
            maxCount = 5
            if @DataService.configuration?["email.attachments.maxCount"]?
                maxCount = parseInt(@DataService.configuration["email.attachments.maxCount"])
            if @$scope.attachments.length > maxCount
                @ValidationService.alerts.push({type:'danger', msg:'A maximum of '+maxCount+' attachments can be provided'})
                valid = false
            totalSize = 0
            for attachment in @$scope.attachments
                totalSize += attachment.size
            maxSizeMBs = 5
            if @DataService.configuration?["email.attachments.maxSize"]?
                maxSizeMBs = parseInt(@DataService.configuration["email.attachments.maxSize"])
            maxSize = maxSizeMBs * 1024 * 1024
            if totalSize > maxSize
                @ValidationService.alerts.push({type:'danger', msg:'The total size of attachments cannot exceed '+maxSizeMBs+' MBs.'})
                valid = false
        valid

    @$scope.send = () =>
        @$scope.clearSuccess()
        @ValidationService.clearAll()
        if (!@$scope.sendDisabled() & 
            @ValidationService.validateEmail(@$scope.contactAddress, "Please enter a valid email address.") &
            @ValidationService.objectNonNull(@$scope.feedback.id, "Please select the feedback type.") &
            @ValidationService.requireNonNull(@$scope.getRichTextEditor().getContent(), "Please provide a message.") &
            @$scope.validateAttachments()
        ) 
            @$scope.sendPending = true
            @AccountService.submitFeedback(@DataService.user.id, @$scope.contactAddress, @$scope.feedback.id, @$scope.feedback.description, @$scope.getRichTextEditor().getContent(), @$scope.attachments)
            .then (data) =>
                if (data? && data.error_code?)
                    @ValidationService.alerts.push({type:'danger', msg:data.error_description})
                    @$scope.sendPending = false
                    @$scope.alerts = @ValidationService.getAlerts()
                else
                    @$scope.successMessage = "Your feedback was submitted successfully."
                    @$scope.resetState()                
            .catch (error) =>
                @$scope.sendPending = false
                @ErrorService.showErrorMessage(error)
        else
            @$scope.alerts = @ValidationService.getAlerts()

    @$scope.cancel = () =>
        @$modalInstance.dismiss()

    @$scope.closeAlert = (index) =>
        @ValidationService.clearAlert(index)
        @$scope.alerts = @ValidationService.getAlerts()

    @$scope.clearSuccess = () =>
        @$scope.successMessage = undefined

    @$scope.attachFile = (files) =>
        file = _.head files
        if file?
            reader = new FileReader()
            reader.readAsDataURL file
            reader.onload = (event) =>
                data = event.target.result
                @$scope.attachments.push({
                    name: file.name
                    size: file.size
                    type: file.type
                    data: data
                })
                $scope.$apply()

    @$scope.removeAttachment = (index) =>
        @$scope.attachments.splice(index, 1)
    
    @$scope.showUpload = () =>
        maxCount = -1
        if @DataService.configuration?["email.attachments.maxCount"]?
            maxCount = parseInt(@DataService.configuration["email.attachments.maxCount"])
        maxCount > 0



@controllers.controller 'ContactSupportController', ContactSupportController
