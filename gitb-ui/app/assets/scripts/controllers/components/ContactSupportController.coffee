class ContactSupportController

  @$inject = ['$scope', '$modalInstance', 'WebEditorService', '$timeout', 'DataService', 'AccountService', 'ErrorService', 'ValidationService']
  constructor: (@$scope, @$modalInstance, @WebEditorService, @$timeout, @DataService, @AccountService, @ErrorService, @ValidationService) ->
    @$modalInstance.opened.then(
        @$timeout(() =>
            @WebEditorService.editorMinimal(200, "")
            @$scope.editorReady = true
        , 1)
    )
    @$scope.contactAddress = @DataService.user.email
    @$scope.surveyAddress = @DataService.configuration?["survey.address"]
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
        if (tinymce.activeEditor?)
            tinymce.activeEditor.setContent("")

    @$scope.resetState()

    @$scope.sendDisabled = () =>
        !(@$scope.contactAddress? && @$scope.feedback? && @$scope.feedback?.id? && !@$scope.sendPending)

    @$scope.showSurveyLink = () =>
        @DataService.configuration?["survey.enabled"] == 'true'

    @$scope.send = () =>
        @$scope.clearSuccess()
        @ValidationService.clearAll()
        if (!@$scope.sendDisabled() & 
            @ValidationService.validateEmail(@$scope.contactAddress, "Please enter a valid email address.") &
            @ValidationService.objectNonNull(@$scope.feedback.id, "Please select the feedback type.") &
            @ValidationService.requireNonNull(tinymce.activeEditor.getContent(), "Please provide a message.")
        ) 
            @$scope.sendPending = true
            @AccountService.submitFeedback(@DataService.user.id, @$scope.contactAddress, @$scope.feedback.id, @$scope.feedback.description, tinymce.activeEditor.getContent())
            .then () =>
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

    @$scope.clearSuccess = () =>
        @$scope.successMessage = undefined

@controllers.controller 'ContactSupportController', ContactSupportController
