class OrganizationController

    @$inject = ['$scope', '$uibModal', 'DataService', 'AccountService', 'ErrorService', 'Constants', 'UserService', 'ConfirmationDialogService', 'OrganizationService', '$stateParams', 'PopupService', 'canEditOwnOrganisation']
    constructor: (@$scope, @$uibModal, @DataService, @AccountService, @ErrorService, @Constants, @UserService, @ConfirmationDialogService, @OrganizationService, @$stateParams, @PopupService, @canEditOwnOrganisation) ->
        @ds = @DataService #shorten service name
        @users  = []       # users of the organization
        @alerts = []       # alerts to be displayed
        @$scope.udata = {} # bindings for new user
        @$scope.vdata = {} # bindings for vendor
        @$scope.vdata.fname = @ds.vendor.fname
        @$scope.vdata.sname = @ds.vendor.sname
        @dataStatus = {status: @Constants.STATUS.PENDING}
        @propertyData = {
            properties: []
            edit: @$stateParams['viewProperties']? && @$stateParams['viewProperties']
        }

        if @DataService.isVendorAdmin || @DataService.isSystemAdmin || @DataService.isCommunityAdmin
            @DataService.focus('shortName')

        @OrganizationService.getOwnOrganisationParameterValues()
        .then (data) =>
            @propertyData.properties = data
        .catch (error) =>
            @ErrorService.showErrorMessage(error)

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
        @DataService.userStatus(ssoStatus)

    deleteVisible: (member) =>
        # Don't allow deletion of own account or demo user account.
        member.id != @DataService.user.id && (!@DataService.configuration['demos.enabled'] || @DataService.configuration['demos.account'] != member.id)

    deleteMember: (member) =>
        @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this user?", "Yes", "No")
        .then () =>
            @UserService.deleteVendorUser(member.id)
            .then (data) =>
                if data.error_code?
                    @ErrorService.showErrorMessage(data.error_description)
                else
                    @getVendorUsers() #get users list again
                    @PopupService.success('User deleted.')
            .catch (error) =>
                @ErrorService.showErrorMessage(error)

    getVendorUsers:() =>
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
                @dataStatus.status = @Constants.STATUS.FINISHED
            ,
            (error) => #error handler
                @ErrorService.showErrorMessage(error)
                @dataStatus.status = @Constants.STATUS.FINISHED
        )

    valueDefined: (value) =>
        value? && value.trim().length > 0

    saveDisabled : () =>
        !@valueDefined(@$scope.vdata.fname) || !@valueDefined(@$scope.vdata.sname) || (@propertyData.edit && !@DataService.customPropertiesValid(@propertyData.properties))

    updateVendorProfile : () ->
        if @checkForm1()
            @AccountService.updateVendorProfile(@$scope.vdata.fname, @$scope.vdata.sname, @propertyData.edit, @propertyData.properties) #call service op.
            .then(
                (data) => #success handler
                    @ds.user.organization.fname = @$scope.vdata.fname
                    @ds.user.organization.sname = @$scope.vdata.sname
                    @ds.vendor.fname = @$scope.vdata.fname
                    @ds.vendor.sname = @$scope.vdata.sname
                    @PopupService.success(@DataService.labelOrganisation()+" information updated.")
                ,
                (error) => #error handler
                    @ErrorService.showErrorMessage(error)
                    #revert back the changes
                    @$scope.vdata.fname = @ds.vendor.fname
                    @$scope.vdata.sname = @ds.vendor.sname
            )
        else #revert back the changes
            @$scope.vdata.fname = @ds.vendor.fname
            @$scope.vdata.sname = @ds.vendor.sname

    checkForm1: () ->
        @removeAlerts()
        valid = true

        if @$scope.vdata.fname == undefined || @$scope.vdata.fname == ''
            @alerts.push({type:'danger', msg:"Full name of your "+@DataService.labelOrganisationLower()+" can not be empty."})
            valid = false
        else if @$scope.vdata.sname == undefined || @$scope.vdata.sname == ''
            @alerts.push({type:'danger', msg:"Short name of your "+@DataService.labelOrganisationLower()+" can not be empty."})
            valid = false

        valid

    closeAlert: (index) ->
        @alerts.splice(index, 1)

    popupMemberForm: () ->
        @removeAlerts()
        modalOptions =
            templateUrl: 'assets/views/settings/add-member-modal.html'
            controller: 'AddMemberController as controller'
            size: 'lg'
        modalInstance = @$uibModal.open(modalOptions)
        modalInstance.result
            .finally(angular.noop)
            .then(() => 
                @getVendorUsers() #get users list again
            , angular.noop)

    removeAlerts: () ->
        @alerts = []

    canUpdateOrganisation: () =>
        canUpdate = false
        if @DataService.isSystemAdmin || @DataService.isCommunityAdmin
            canUpdate = true
        else if @DataService.isVendorAdmin && (@DataService.community.allowPostTestOrganisationUpdates || !@hasTests)
            canUpdate = true
        canUpdate

controllers.controller('OrganizationController', OrganizationController)