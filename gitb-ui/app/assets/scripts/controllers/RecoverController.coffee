class RecoverController

    @$inject = ['$log', '$scope', '$location', 'Constants']
    constructor: (@$log, @$scope, @$location, @Constants) ->
        @$log.debug "Constructing RecoverController..."

        @alerts = []     # alerts to be displayed
        @spinner = false # spinner to be display while waiting response from the server

	#call remote reset operation to get access token to be authorized user operations
    reset: () ->
        if @checkForm()
            #@spinner = true #start spinner before calling service operation
            @alerts = []
            @alerts.push({type:'success', msg:"An email to reset your password has been sent."})

	#checks form validity
    checkForm: () ->
        @alerts = []
        valid = true
        emailRegex = @Constants.EMAIL_REGEX

		#check for empty email input
        if @$scope.email == undefined || @$scope.email == ''
            @alerts.push({type:'danger', msg:"Please enter your email address."})
            valid = false
        #check for invalid email input
        else if !emailRegex.test(@$scope.email)
            @alerts.push({type:'danger', msg:"Please enter a valid email address."})
            valid = false

        valid

	#closes alert which is displayed due to an error
    closeAlert: (index) ->
        @alerts.splice(index, 1)

	#redirects user to the specified path, i.e. /login, /register, etc.
    redirect: (address) ->
        @$location.path(address);

controllers.controller('RecoverController', RecoverController)