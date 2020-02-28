class DisconnectRoleModalController

    @$inject = ['$uibModalInstance', 'Constants', 'AuthService', 'ErrorService', 'ConfirmationDialogService', 'DataService']
    constructor: (@$uibModalInstance, @Constants, @AuthService, @ErrorService, @ConfirmationDialogService, @DataService) ->
        @disconnectPending = false
        @choice = @Constants.DISCONNECT_ROLE_OPTION.CURRENT_PARTIAL

    disconnect: () =>
        if @choice == @Constants.DISCONNECT_ROLE_OPTION.CURRENT_PARTIAL
            message = "This action will also end your current session. Are you sure you want to proceed?"
        else 
            message = "This action will end your current session and cannot be undone. Are you sure you want to proceed?"
        @ConfirmationDialogService.confirm("Confirmation", message, "Yes", "No")
        .finally(angular.noop)
        .then () =>
            @AuthService.disconnectFunctionalAccount(@choice)
            .then(
                (data) => #success handler
                    @disconnectPending = false
                    @$uibModalInstance.close(@choice)
                ,
                (error) => #error handler
                    @disconnectPending = false
                    @ErrorService.showErrorMessage(error)
            )

    cancel: () =>
        @$uibModalInstance.dismiss()

@controllers.controller 'DisconnectRoleModalController', DisconnectRoleModalController
