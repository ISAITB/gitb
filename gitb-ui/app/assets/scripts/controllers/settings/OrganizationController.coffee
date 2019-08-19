class OrganizationController

    @$inject = ['$log', '$scope', '$location', '$uibModal', 'DataService', 'AccountService', 'AuthService', 'ErrorService', 'Constants']
    constructor: (@$log, @$scope, @$location, @$uibModal, @DataService, @AccountService, @AuthService, @ErrorService, @Constants) ->
        @$log.debug 'Constructing OrganizationController'

        @ds = @DataService #shorten service name
        @users  = []       # users of the organization
        @alerts = []       # alerts to be displayed
        @modalAlerts = []  # alerts to be displayed within modals
        @$scope.udata = {} # bindings for new user
        @$scope.vdata = {} # bindings for vendor
        @$scope.vdata.fname = @ds.vendor.fname
        @$scope.vdata.sname = @ds.vendor.sname
        @organizationSpinner = false # spinner to be displayed for organization operations
        @memberSpinner = false       # spinner to be displayed for new member operations

        @tableColumns = [
            {
                field: 'name',
                title: 'Name'
            }
            {
                field: 'email',
                title: 'Email'
            }
            {
                field: 'role',
                title: 'Role'
            }
        ]
        if !@DataService.isVendorUser
            @tableColumns.push({
                field: 'ssoStatusText',
                title: 'Status'
            })

        @getVendorUsers()  # get users of the organization

    userStatus: (ssoStatus) =>
        if @DataService.configuration['sso.enabled']
            if ssoStatus == 1
                'Not migrated'
            else if ssoStatus == 2
                'Inactive'
            else
                'Active'
        else
            'Active'

    getVendorUsers:() =>
        @memberSpinner = true #start spinner
        @AccountService.getVendorUsers() #call service operation
        .then(
            (data) => #success handler
                labels = @Constants.USER_ROLE_LABEL
                @users = data.map((user) =>
                    if user.id == @DataService.user.id
                        user.name = user.name + ' (You)'
                    user.role = labels[user.role]
                    user.ssoStatusText = @userStatus(user.ssoStatus)
                    user
                )
                #stop spinner
                @memberSpinner = false
            ,
            (error) => #error handler
                @ErrorService.showErrorMessage(error)
                #stop spinner
                @memberSpinner = false
        )

    saveDisabled : () ->
        @$scope.vdata.fname == undefined || @$scope.vdata.fname == '' || @$scope.vdata.sname == undefined || @$scope.vdata.sname == ''

    saveMemberDisabled : () ->
        if @DataService.configuration?
            isSSO = @DataService.configuration['sso.enabled']
            if isSSO
                disabled = @$scope.udata.email == undefined || @$scope.udata.email == ''
            else
                disabled = @$scope.udata.email == undefined || @$scope.udata.email == '' || 
                    @$scope.udata.name == undefined || @$scope.udata.name == '' || 
                    @$scope.udata.password == undefined || @$scope.udata.password == '' || 
                    @$scope.udata.cpassword == undefined || @$scope.udata.cpassword == ''
            disabled
        else
            true

    updateVendorProfile : () ->
        if @checkForm1()
            @organizationSpinner = true #start spinner
            @AccountService.updateVendorProfile(@$scope.vdata.fname, @$scope.vdata.sname) #call service op.
            .then(
                (data) => #success handler
                    @alerts.push({type:'success', msg:"Organisation information updated."})
                    @ds.user.organization.fname = @$scope.vdata.fname
                    @ds.user.organization.sname = @$scope.vdata.sname
                    @ds.vendor.fname = @$scope.vdata.fname
                    @ds.vendor.sname = @$scope.vdata.sname
                    #stop spinner
                    @organizationSpinner = false
                ,
                (error) => #error handler
                    @ErrorService.showErrorMessage(error)
                    #revert back the changes
                    @$scope.vdata.fname = @ds.vendor.fname
                    @$scope.vdata.sname = @ds.vendor.sname
                    #stop spinner
                    @organizationSpinner = false
            )
        else #revert back the changes
            @$scope.vdata.fname = @ds.vendor.fname
            @$scope.vdata.sname = @ds.vendor.sname

    checkForm1: () ->
        @removeAlerts()
        valid = true

        if @$scope.vdata.fname == undefined || @$scope.vdata.fname == ''
            @alerts.push({type:'danger', msg:"Full name of your organisation can not be empty."})
            valid = false
        else if @$scope.vdata.sname == undefined || @$scope.vdata.sname == ''
            @alerts.push({type:'danger', msg:"Short name of your organisation can not be empty."})
            valid = false

        valid

    #call remote operation to check email availability
    checkEmail: () ->
        if @checkForm2()
            @memberSpinner = true #start spinner
            @AuthService.checkEmailOfOrganisationMember(@$scope.udata.email)
            .then(
                (data) =>  #success handler
                    if data.available
                        @addMember()
                    else #error handler
                        @modalAlerts.push({type:'danger', msg:"A user with email '#{@$scope.udata.email}' has already been registered."})
                        @$scope.udata.email = @$scope.udata.password = @$scope.udata.cpassword = ''
                        @memberSpinner = false                        
                        @spinner = false
                ,
                (error) =>
                    #no error supposed to happen here but check anyway
                    @ErrorService.showErrorMessage(error)
                    #stop spinner
                    @spinner = false
            )
        else
            @$scope.udata.password = @$scope.udata.cpassword = '' # clear password every time the form is not valid

	#call remote operation to register user
    addMember: () ->
        @AccountService.registerUser(@$scope.udata.name, @$scope.udata.email, @$scope.udata.password)
        .then(
            (data) => #success handler
                @getVendorUsers() #get users list again
                @memberSpinner = false # stop spinner
            ,
            (error) => #error handler
                @ErrorService.showErrorMessage(error)
                @memberSpinner = false # stop spinner
        )
        #close modal when we are done
        $('#addMemberModal').modal('hide')


    checkForm2: () ->
        @removeAlerts()
        valid = true
        emailRegex = @Constants.EMAIL_REGEX
        isSSO = @DataService.configuration['sso.enabled']

        if !isSSO && (@$scope.udata.name == undefined || @$scope.udata.name == '')
            @modalAlerts.push({type:'danger', msg:"You have to enter the user's name."})
            valid = false
        else if @$scope.udata.email == undefined || @$scope.udata.email == ''
            @modalAlerts.push({type:'danger', msg:"You have to enter the user's email."})
            valid = false
        else if !emailRegex.test(@$scope.udata.email)
            @modalAlerts.push({type:'danger', msg:"You have to enter a valid email address."})
            valid = false
        else if !isSSO && (@$scope.udata.password == undefined || @$scope.udata.password == '')
            @modalAlerts.push({type:'danger', msg:"You have to enter the user's password."})
            valid = false
        else if !isSSO && (@$scope.udata.cpassword == undefined || @$scope.udata.cpassword == '')
            @modalAlerts.push({type:'danger', msg:"You have to confirm the user's password."})
            valid = false
        else if !isSSO && (@$scope.udata.password != @$scope.udata.cpassword)
            @modalAlerts.push({type:'danger', msg:"The provided passwords don't match."})
            valid = false
        valid

    closeAlert: (index) ->
        @alerts.splice(index, 1)

    closeModalAlert: (index) ->
        @modalAlerts.splice(index, 1)

    openModel : () ->
        @$scope.udata = {}
        @removeAlerts()

    removeAlerts: () ->
        @alerts = []
        @modalAlerts = []

controllers.controller('OrganizationController', OrganizationController)