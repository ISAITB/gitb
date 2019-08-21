class PasswordController

    @$inject = ['$log', '$scope', '$location', 'AccountService', 'ErrorService', 'ErrorCodes', 'DataService', '$state', '$rootScope', 'Events']
    constructor: (@$log, @$scope, @$location, @AccountService, @ErrorService, @ErrorCodes, @DataService, @$state, @$rootScope, @Events) ->
        @$log.debug 'Constructing PasswordController'

        @ds = @DataService #shorten service name
        @users  = []       # users of the organization
        @alerts = []       # alerts to be displayed
        @$scope.data = {}  # holds scope bindings
        @passwordSpinner = false # spinner to be displayed for password operations

    saveDisabled : () ->
        @$scope.data.currentPassword == undefined || @$scope.data.currentPassword == '' || 
        @$scope.data.password1 == undefined || @$scope.data.password1 == '' ||
        @$scope.data.password2 == undefined || @$scope.data.password2 == ''

    cancelOneTimePasswordUpdate: () ->
        @$rootScope.$emit(@Events.onLogout, {full: true})

    updatePassword: () ->
        if @checkForm()
            @passwordSpinner = true #start spinner
            @AccountService.updateUserProfile(null, @$scope.data.password1, @$scope.data.currentPassword)
            .then(
                (data) => #success handler
                    @alerts.push({type:'success', msg:"Your password has been updated."})
                    @passwordSpinner = false #stop spinner
                    if @DataService.user.onetime
                        @DataService.user.onetime = false
                        @$state.go 'app.home'
                ,
                (error) => #error handler
                    if error.data.error_code == @ErrorCodes.INVALID_CREDENTIALS
                        @alerts.push({type:'danger', msg:"You entered a wrong password."})
                    else #unidentified error
                        @ErrorService.showErrorMessage(error)
                    #stop spinner
                    @passwordSpinner = false
            )
            @$scope.data = {}

    checkForm: () ->
        @alerts = []
        valid = true

        if @$scope.data.currentPassword == undefined || @$scope.data.currentPassword == ''
            @alerts.push({type:'danger', msg:"You have to enter your current password."})
            valid = false
        else if @$scope.data.password1 == undefined || @$scope.data.password1 == ''
            @alerts.push({type:'danger', msg:"You have to enter a new password."})
            valid = false
        else if @$scope.data.password2 == undefined || @$scope.data.password2 == ''
            @alerts.push({type:'danger', msg:"You have to confirm your new password."})
            valid = false
        else if @$scope.data.password1 != @$scope.data.password2
            @alerts.push({type:'danger', msg:"The new password does not match the confirmation."})
            valid = false
        else if @$scope.data.currentPassword == @$scope.data.password1
            @alerts.push({type:'danger', msg:"The password you provided is the same as the current one."})
            valid = false

        valid

    closeAlert: (index) ->
        @alerts.splice(index, 1)


controllers.controller('PasswordController', PasswordController)