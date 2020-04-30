class AddMemberController

    @$inject = ['$uibModalInstance', 'DataService', 'AccountService', 'AuthService', 'ErrorService', 'Constants', 'PopupService']
    constructor: (@$uibModalInstance, @DataService, @AccountService, @AuthService, @ErrorService, @Constants, @PopupService) ->
        @udata = {}
        @modalAlerts = []
        @$uibModalInstance.rendered.then () => 
            if @DataService.configuration['sso.enabled']
                @DataService.focus('email')
            else
                @DataService.focus('name')

    saveMemberDisabled : () ->
        if @DataService.configuration?
            isSSO = @DataService.configuration['sso.enabled']
            if isSSO
                disabled = @udata.email == undefined || @udata.email == ''
            else
                disabled = @udata.email == undefined || @udata.email == '' || 
                    @udata.name == undefined || @udata.name == '' || 
                    @udata.password == undefined || @udata.password == '' || 
                    @udata.cpassword == undefined || @udata.cpassword == ''
            disabled
        else
            true

    #call remote operation to check email availability
    checkEmail: () ->
        if @checkForm2()
            @memberSpinner = true
            @AuthService.checkEmailOfOrganisationMember(@udata.email)
            .then(
                (data) =>  #success handler
                    if data.available
                        @addMember()
                    else #error handler
                        @modalAlerts.push({type:'danger', msg:"A user with email '#{@udata.email}' has already been registered."})
                        @udata.email = @udata.password = @udata.cpassword = ''
                        @memberSpinner = false                        
                ,
                (error) =>
                    @ErrorService.showErrorMessage(error)
                    @memberSpinner = true
            )
        else
            @udata.password = @udata.cpassword = ''

    addMember: () ->
        @AccountService.registerUser(@udata.name, @udata.email, @udata.password)
        .then(
            (data) => #success handler
                @$uibModalInstance.close()
                @PopupService.success("User created.")
            ,
            (error) => #error handler
                @ErrorService.showErrorMessage(error)
        )

    close: () ->
        @$uibModalInstance.dismiss()

    closeModalAlert: (index) ->
        @modalAlerts.splice(index, 1)

    removeAlerts: () ->
        @modalAlerts = []

    checkForm2: () ->
        @removeAlerts()
        valid = true
        emailRegex = @Constants.EMAIL_REGEX
        isSSO = @DataService.configuration['sso.enabled']

        if !isSSO && (@udata.name == undefined || @udata.name == '')
            @modalAlerts.push({type:'danger', msg:"You have to enter the user's name."})
            valid = false
        else if @udata.email == undefined || @udata.email == ''
            @modalAlerts.push({type:'danger', msg:"You have to enter the user's email."})
            valid = false
        else if !emailRegex.test(@udata.email)
            @modalAlerts.push({type:'danger', msg:"You have to enter a valid email address."})
            valid = false
        else if !isSSO && (@udata.password == undefined || @udata.password == '')
            @modalAlerts.push({type:'danger', msg:"You have to enter the user's password."})
            valid = false
        else if !isSSO && (@udata.cpassword == undefined || @udata.cpassword == '')
            @modalAlerts.push({type:'danger', msg:"You have to confirm the user's password."})
            valid = false
        else if !isSSO && (@udata.password != @udata.cpassword)
            @modalAlerts.push({type:'danger', msg:"The provided passwords don't match."})
            valid = false
        valid

controllers.controller('AddMemberController', AddMemberController)