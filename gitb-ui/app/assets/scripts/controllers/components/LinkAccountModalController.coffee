class LinkAccountModalController

    @$inject = ['$scope', '$uibModalInstance', 'DataService', 'linkedAccounts', 'Constants', 'AuthService', 'ErrorService', 'createOption']

    constructor: (@$scope, @$uibModalInstance, @DataService, @linkedAccounts, @Constants, @AuthService, @ErrorService, createOption) ->
        @choice = -1
        @selectedAccountId = -1
        @createPending = false

        if createOption == @Constants.LOGIN_OPTION.REGISTER
            @choice = @Constants.CREATE_ACCOUNT_OPTION.SELF_REGISTER
        else if createOption == @Constants.LOGIN_OPTION.MIGRATE
            @choice = @Constants.CREATE_ACCOUNT_OPTION.MIGRATE

    selectAccount: (accountId) =>
        if @selectedAccountId == accountId
            @selectedAccountId = -1
        else
            @selectedAccountId = accountId

    createEnabled: () =>
        if @createPending
            false
        else
            if @choice == @Constants.CREATE_ACCOUNT_OPTION.LINK
                @selectedAccountId != -1
            else if @choice == @Constants.CREATE_ACCOUNT_OPTION.MIGRATE
                @email? && @password? && @email.trim().length > 0 && @password.trim().length > 0
            else
                false

    choiceChanged: () =>
        @selectedAccountId = -1
        @email = undefined
        @password = undefined

    handleError: (title, response) =>
        handled = false
        if response.error_code?
            error = { 
                statusText: title,
                data: {error_description: response.error_description}
            }
            @ErrorService.showErrorMessage(error)
            handled = true
        handled

    create: () =>
        @createPending = true
        if @choice == @Constants.CREATE_ACCOUNT_OPTION.LINK
            @AuthService.linkFunctionalAccount(@selectedAccountId)
            .then((data) =>
                @createPending = false
                if !@handleError('Unable to confirm role', data)
                    @DataService.setActualUser(data)
                    @$uibModalInstance.close()
            ,
            (error) =>
                @createPending = false
                @ErrorService.showErrorMessage(error)
            )
        else if @choice == @Constants.CREATE_ACCOUNT_OPTION.MIGRATE
            @AuthService.migrateFunctionalAccount(@email, @password)
            .then((data) => 
                @createPending = false
                if !@handleError('Unable to migrate account', data)
                    @DataService.setActualUser(data)
                    @$uibModalInstance.close()
            ,
            (error) =>
                @createPending = false
                @ErrorService.showErrorMessage(error)
            )

    cancel: () =>
        @$uibModalInstance.dismiss()

@controllers.controller 'LinkAccountModalController', LinkAccountModalController
