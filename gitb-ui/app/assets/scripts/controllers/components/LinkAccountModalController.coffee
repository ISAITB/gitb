class LinkAccountModalController

    @$inject = ['$scope', '$uibModalInstance', 'DataService', 'linkedAccounts', 'Constants', 'AuthService', 'ErrorService', 'createOption', 'CommunityService', 'selfRegOptions']

    constructor: (@$scope, @$uibModalInstance, @DataService, @linkedAccounts, @Constants, @AuthService, @ErrorService, createOption, @CommunityService, @selfRegOptions) ->
        @choice = -1
        @selectedAccountId = -1
        @createPending = false
        @selfRegData = {}

        if createOption == @Constants.LOGIN_OPTION.REGISTER
            @choice = @Constants.CREATE_ACCOUNT_OPTION.SELF_REGISTER
        else if createOption == @Constants.LOGIN_OPTION.MIGRATE
            @choice = @Constants.CREATE_ACCOUNT_OPTION.MIGRATE
        else if createOption == @Constants.LOGIN_OPTION.LINK_ACCOUNT || (!@DataService.configuration['registration.enabled'] && !@DataService.configuration['sso.inMigration'])
            @choice = @Constants.CREATE_ACCOUNT_OPTION.LINK

    resetSelfRegOptions: () =>
        @selfRegData = {}

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
                @textProvided(@email) && @textProvided(@password)
            else if @choice == @Constants.CREATE_ACCOUNT_OPTION.SELF_REGISTER
                @selfRegData.selfRegOption?.communityId? && 
                    (@selfRegData.selfRegOption.selfRegType != @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN || @textProvided(@selfRegData.selfRegToken)) && 
                    (!@selfRegData.selfRegOption.forceTemplateSelection || @selfRegData.selfRegOption.templates?.length == 0 || @selfRegData.template?) &&
                    (!@selfRegData.selfRegOption.forceRequiredProperties || @DataService.customPropertiesValid(@selfRegData.selfRegOption.organisationProperties, true)) &&
                    @textProvided(@selfRegData.orgShortName) && @textProvided(@selfRegData.orgFullName)
            else
                false

    textProvided: (value) =>
        value? && value.trim().length > 0

    choiceChanged: () =>
        @selectedAccountId = -1
        @email = undefined
        @password = undefined
        @resetSelfRegOptions()
        if @DataService.configuration['sso.inMigration'] && @choice == @Constants.CREATE_ACCOUNT_OPTION.MIGRATE
            @DataService.focus('username')

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
        else if @choice == @Constants.CREATE_ACCOUNT_OPTION.SELF_REGISTER
            if @selfRegData.selfRegOption.selfRegType == @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN
                token = @selfRegData.selfRegToken
            if @selfRegData.template?
                templateId = @selfRegData.template.id
            @CommunityService.selfRegister(@selfRegData.selfRegOption.communityId, token, @selfRegData.orgShortName, @selfRegData.orgFullName, templateId, @selfRegData.selfRegOption.organisationProperties, undefined, undefined, undefined)
            .then((data) => 
                @createPending = false
                if !@handleError('Unable to register', data)
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
