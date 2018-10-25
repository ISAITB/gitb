class RegisterController

    @$inject = ['$log', '$scope', '$rootScope', 'AccountService', 'AuthService', 'ErrorService', 'Events', 'Constants']
    constructor: (@$log, @$scope, @$rootScope, @AccountService, @AuthService, @ErrorService, @Events, @Constants) ->
        @$log.debug "Constructing RegisterController..."

        @alerts = []     # alerts to be displayed
        @spinner = false # spinner to be display while waiting response from the server

    #call remote operation to check email availability
    checkEmail: () ->
        if @checkForm()
            @spinner = true #start spinner before calling service operation
            @AuthService.checkEmail(@$scope.email)
            .then(
                (data) =>
                    if data.available
                        @register()
                    else
                        @alerts.push({type:'danger', msg:"A user with email '#{@$scope.email}' has already been registered."})
                        @$scope.email = @$scope.password = ''
                        @spinner = false
                ,
                (error) =>
                    #no error supposed to happen here but check anyway
                    @ErrorService.showErrorMessage(error)
                    #stop spinner
                    @spinner = false
            )
        else
            @$scope.password = '' # clear password everytime the form is not valid

    #call remote operation to register vendor and its admin
    register: () ->
        @AccountService.registerVendor(@$scope.sname, @$scope.fname,
        @$scope.name, @$scope.email, @$scope.password)
        .then(
            (data) => #successful register handler
                #registration successful, now login
                @AuthService.access_token(@$scope.email, @$scope.password)
                .then(
                    (data) => #successful login handler
                        # login successful, fire onLogin event so that our authentication provider
                        # authenticaes user to the system
                        @$rootScope.$emit(@Events.onLogin, {
                            tokens  : data,
                            remember: false # false, since no "remember me" field
                        })
                        #stop spinner
                        @spinner = false
                    (error) => #unsuccessful login handler
                        @ErrorService.showErrorMessage(error)
                        #stop spinner
                        @spinner = false
                )
            ,
            (error) => #unsuccessful register handler
                #no error supposed to happen here but check anyway
                @ErrorService.showErrorMessage(error)
                #stop spinner
                @spinner = false
        )

    #checks form validity
    checkForm:() ->
        @alerts = []
        valid = true
        emailRegex = @Constants.EMAIL_REGEX

        #check for empty user name input
        if @$scope.name == undefined || @$scope.name == ''
            @alerts.push({type:'danger', msg:"Please enter your name."})
            valid = false
        #check for empty organisation short name input
        else if @$scope.sname == undefined || @$scope.sname == ''
            @alerts.push({type:'danger', msg:"Please enter short name of your organisation"})
            valid = false
        #check for empty organisation full name input
        else if @$scope.fname == undefined || @$scope.fname == ''
            @alerts.push({type:'danger', msg:"Please enter full name of your organisation"})
            valid = false
        #check for empty email input
        else if @$scope.email == undefined || @$scope.email == ""
            @alerts.push({type:'danger', msg:"Please enter your email address."})
            valid = false
        #check for invalid email input
        else if !emailRegex.test(@$scope.email)
            @alerts.push({type:'danger', msg:"Please enter a valid email address."})
            valid = false
        #check for emptry password input
        else if @$scope.password == undefined || @$scope.password == ''
            @alerts.push({type:'danger', msg:"Please enter your password."})
            valid = false

        valid

    #closes alert which is displayed due to an error
    closeAlert: (index) ->
        @alerts.splice(index, 1)

controllers.controller('RegisterController', RegisterController)