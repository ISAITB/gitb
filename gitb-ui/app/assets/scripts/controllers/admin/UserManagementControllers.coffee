class UserManagementController
  name: 'UserManagementController'

  @$inject = ['$log', '$state', 'UserService', 'LandingPageService', 'OrganizationService', 'LegalNoticeService', 'ErrorService']
  constructor: (@$log, @$state, @UserService, @LandingPageService, @OrganizationService, @LegalNoticeService, @ErrorService) ->

    # admin table
    @adminColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'email',
        title: 'Email'
      }
    ]

    # organization table
    @organizationColums = [
      {
        field: 'sname',
        title: 'Short name'
      }
      {
        field: 'fname',
        title: 'Full name'
      }
    ]

    # landing page table
    @landingPagesColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'default',
        title: 'Default'
      }
    ]

    # legal notice table
    @legalNoticesColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'default',
        title: 'Default'
      }
    ]

    @admins = []
    @organizations = []
    @landingPages = []
    @legalNotices = []

    # get all system administrators
    @UserService.getSystemAdministrators()
    .then (data) =>
      @admins = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get all organizations
    @OrganizationService.getOrganizations()
    .then (data) =>
      @organizations = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get all landing pages
    @LandingPageService.getLandingPages()
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get all legal notices
    @LegalNoticeService.getLegalNotices()
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # detail of selected admin
  adminSelect: (admin) =>
    @$state.go 'app.admin.users.admins.detail', { id : admin.id }

  # detail of selected organization
  organizationSelect: (organization) =>
    @$state.go 'app.admin.users.organizations.detail.list', { id : organization.id }

  # detail of selected landing page
  landingPageSelect: (landingPage) =>
    @$state.go 'app.admin.users.landingpages.detail', { id : landingPage.id }

  # detail of selected legal notice
  legalNoticeSelect: (ln) =>
    @$state.go 'app.admin.users.legalnotices.detail', { id : ln.id }

####################################################################################

class AdminCreateController
  name: 'AdminCreateController'

  @$inject = ['$log', '$state', 'UserService', 'ValidationService', 'AuthService', 'ErrorService']
  constructor: (@$log, @$state, @UserService, @ValidationService, @AuthService, @ErrorService) ->

    @alerts = []
    @user = {}

  # create system administrator and cancel screen
  createAdmin: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
    @ValidationService.validatePasswords(@user.password, @user.cpassword, "Please enter equal passwords.") &
    @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      @AuthService.checkEmail(@user.email)
      .then (data) =>
        if (data.available)
          @UserService.createSystemAdmin(@user.name, @user.email, @user.password)
          .then () =>
           @cancelCreateAdmin()
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered."})
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create
  cancelCreateAdmin: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class AdminDetailController
  name: 'AdminDetailController'

  @$inject = ['$log', '$state', '$stateParams','UserManagementService', 'UserService', 'ValidationService', 'DataService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @UserManagementService, @UserService, @ValidationService, @DataService, @ConfirmationDialogService, @ErrorService) ->

    @alerts = []
    @userId = @$stateParams.id
    @user = {}

    @disableDeleteButton = Number(@DataService.user.id) == Number(@userId)

    # get selected user
    @UserService.getUserById(@userId)
    .then (data) =>
      @user = data
      @UserManagementService.mapUser(@user)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateAdmin: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.")
      @UserService.updateSystemAdminProfile(@userId, @user.name)
      .then () =>
        @cancelDetailAdmin()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # delete and cancel detail
  deleteAdmin: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this administrator?", "Yes", "No")
    .then () =>
      @UserService.deleteSystemAdmin(@userId)
      .then (data) =>
        @cancelDetailAdmin()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailAdmin: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class OrganizationCreateController
  name: 'OrganizationCreateController'

  @$inject = ['$log', '$state', 'LandingPageService', 'LegalNoticeService', 'ValidationService', 'OrganizationService', 'ErrorService']
  constructor: (@$log, @$state, @LandingPageService, @LegalNoticeService, @ValidationService, @OrganizationService, @ErrorService) ->

    @alerts = []
    @organization = {}
    @landingPages = []
    @legalNotices = []

    @LandingPageService.getLandingPages()
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LegalNoticeService.getLegalNotices()
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # create organization and cancel screen
  createOrganization: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@organization.sname, "Please enter short name of the organization.") &
    @ValidationService.requireNonNull(@organization.fname, "Please enter full name of the organization.")
      @OrganizationService.createOrganization @organization.sname, @organization.fname, @organization.landingPages, @organization.legalNotices
      .then () =>
        @cancelCreateOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create organization
  cancelCreateOrganization: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class OrganizationDetailController
  name: 'OrganizationDetailController'

  @$inject = ['$log', '$state', '$stateParams', 'LandingPageService', 'LegalNoticeService', 'UserManagementService', 'ValidationService', 'ConfirmationDialogService', 'OrganizationService', 'UserService', 'Constants', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @LandingPageService, @LegalNoticeService, @UserManagementService, @ValidationService, @ConfirmationDialogService, @OrganizationService, @UserService, @Constants, @ErrorService) ->

    @userColumns = [
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

    @orgId = @$stateParams.id
    @organization = {}
    @landingPages = []
    @legalNotices = []
    @users = []
    @alerts = []

    # get selected organization
    @OrganizationService.getOrganizationById(@orgId)
    .then (data) =>
      @organization = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get users of the organization
    @UserService.getUsersByOrganization(@orgId)
    .then (data) =>
      @users = data
      @UserManagementService.mapUsers(@users)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LandingPageService.getLandingPages()
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LegalNoticeService.getLegalNotices()
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # delete and cancel detail
  deleteOrganization: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this organization?", "Yes", "No")
    .then () =>
      @OrganizationService.deleteOrganization(@orgId)
      .then () =>
        @cancelDetailOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateOrganization: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@organization.sname, "Please enter short name of the organization.") &
    @ValidationService.requireNonNull(@organization.fname, "Please enter full name of the organization.")
      @OrganizationService.updateOrganization(@orgId, @organization.sname, @organization.fname, @organization.landingPages, @organization.legalNotices)
      .then () =>
        @cancelDetailOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # detail of selected organization
  userSelect: (user) =>
    @$state.go 'app.admin.users.organizations.detail.users.detail.list', { user_id : user.id }

  # cancel detail
  cancelDetailOrganization: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class UserCreateController
  name: 'UserCreateController'

  @$inject = ['$log', '$state', '$stateParams', 'ValidationService', 'UserService', 'Constants', 'AuthService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @ValidationService, @UserService, @Constants, @AuthService, @ErrorService) ->

    @orgId = @$stateParams.id
    @alerts = []
    @user = {}

    @roleCreateChoices = @Constants.VENDOR_USER_ROLES

  # create user and cancel screen
  createUser: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
    @ValidationService.objectNonNull(@user.role, "Please enter a role.") &
    @ValidationService.validatePasswords(@user.password, @user.cpassword, "Please enter equal passwords.") &
    @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      @AuthService.checkEmail(@user.email)
      .then (data) =>
        if (data.available)
          @UserService.createVendorUser @user.name, @user.email, @user.password, @orgId, @user.role.id
          .then () =>
            @cancelCreateUser()
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered."})
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create
  cancelCreateUser: () =>
    @$state.go 'app.admin.users.organizations.detail.list', { id : @orgId }

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class UserDetailController
  name: 'UserDetailController'

  @$inject = ['$log', '$state', '$stateParams', 'ValidationService', 'UserManagementService', 'ConfirmationDialogService', 'UserService', 'Constants', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams,  @ValidationService, @UserManagementService, @ConfirmationDialogService, @UserService, @Constants, @ErrorService) ->

    @orgId = @$stateParams.id
    @userId = @$stateParams.user_id
    @alerts = []
    @user = {}

    @roleChoices = @Constants.VENDOR_USER_ROLES

    # get selected user
    @UserService.getUserById(@userId)
    .then (data) =>
      @user = data
      @UserManagementService.mapUser(@user)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateUser: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.")
      @UserService.updateUserProfile(@userId, @user.name, @user.role.id)
      .then (data) =>
        if (!data)
          @cancelDetailUser()
        else
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    @alerts = @ValidationService.getAlerts()

  # delete and cancel detail
  deleteUser: () =>
    @ValidationService.clearAll()
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this user?", "Yes", "No")
    .then () =>
      @UserService.deleteVendorUser(@userId)
      .then (data) =>
        if (!data)
          @cancelDetailUser()
        else
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
          @alerts = @ValidationService.getAlerts()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailUser: () =>
    @$state.go 'app.admin.users.organizations.detail.list', { id : @orgId }

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class LandingPageCreateController
  name: 'LandingPageCreateController'

  @$inject = ['$log', '$state', '$stateParams', '$scope', 'WebEditorService', 'LandingPageService', 'ValidationService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @$scope, @WebEditorService, @LandingPageService, @ValidationService, @ConfirmationDialogService, @ErrorService) ->

    @alerts = []
    @page = {}

    @initPage()

  initPage: () ->
    @page.name = @$stateParams.name
    @page.description = @$stateParams.description
    @page.default = false
    @WebEditorService.editor(300, @$stateParams.content ? "")

  # create landing page and cancel screen
  createLandingPage: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@page.name, "Please enter a name.")
      if @page.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default landing page. Are you sure?", "Yes", "No")
        .then () =>
          @doCreate()
      else
        @doCreate()
    @alerts = @ValidationService.getAlerts()

  doCreate: () ->
    @LandingPageService.createLandingPage(@page.name, @page.description, tinymce.activeEditor.getContent(), @page.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        @cancelCreateLandingPage()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelCreateLandingPage: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class LandingPageDetailController
  name: 'LandingPageDetailController'

  @$inject = ['$log', '$state', '$stateParams', 'WebEditorService', 'ValidationService', 'LandingPageService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @WebEditorService, @ValidationService, @LandingPageService, @ConfirmationDialogService, @ErrorService) ->

    @pageId = @$stateParams.id
    @alerts = []
    @page = {}
    @isDefault

    # get selected landing page
    @LandingPageService.getLandingPageById(@pageId)
    .then (data) =>
      @page = data
      @isDefault = data.default
      @WebEditorService.editor(300, data.content)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateLandingPage: (copy) =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@page.name, "Please enter a name.")
      if !@isDefault && @page.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default landing page. Are you sure?", "Yes", "No")
        .then () =>
          @doUpdate(copy)
      else
        @doUpdate(copy)
    @alerts = @ValidationService.getAlerts()

  doUpdate: (copy) ->
    @LandingPageService.updateLandingPage(@pageId, @page.name, @page.description, tinymce.activeEditor.getContent(), @page.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        if copy
          @copyLandingPage()
        else
          @cancelDetailLandingPage()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # copy landing page info in create screen
  copyLandingPage: () ->
    name = @page.name + " COPY"
    description = @page.description
    content = tinymce.activeEditor.getContent()
    @$state.go 'app.admin.users.landingpages.create', { name : name, description : description, content : content }

  # delete and cancel detail
  deleteLandingPage: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this landing page?", "Yes", "No")
    .then () =>
      @LandingPageService.deleteLandingPage(@pageId)
      .then () =>
        @cancelDetailLandingPage()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailLandingPage: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class LegalNoticeCreateController
  name: 'LegalNoticeCreateController'

  @$inject = ['$log', '$state', '$stateParams', '$scope', 'WebEditorService', 'LegalNoticeService', 'ValidationService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @$scope, @WebEditorService, @LegalNoticeService, @ValidationService, @ConfirmationDialogService, @ErrorService) ->

    @alerts = []
    @notice = {}

    @initNotice()

  initNotice: () ->
    @notice.name = @$stateParams.name
    @notice.description = @$stateParams.description
    @notice.default = false
    @WebEditorService.editor(300, @$stateParams.content ? "")

  # create legal notice and cancel screen
  createLegalNotice: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@notice.name, "Please enter a name.")
      if @notice.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default legal notice. Are you sure?", "Yes", "No")
        .then () =>
          @doCreate()
      else
        @doCreate()
    @alerts = @ValidationService.getAlerts()

  doCreate: () ->
    @LegalNoticeService.createLegalNotice(@notice.name, @notice.description, tinymce.activeEditor.getContent(), @notice.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        @cancelCreateLegalNotice()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelCreateLegalNotice: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

class LegalNoticeDetailController
  name: 'LegalNoticeDetailController'

  @$inject = ['$log', '$state', '$stateParams', 'WebEditorService', 'ValidationService', 'LegalNoticeService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @WebEditorService, @ValidationService, @LegalNoticeService, @ConfirmationDialogService, @ErrorService) ->

    @noticeId = @$stateParams.id
    @alerts = []
    @notice = {}
    @isDefault

    # get selected legal notice
    @LegalNoticeService.getLegalNoticeById(@noticeId)
    .then (data) =>
      @notice = data
      @isDefault = data.default
      @WebEditorService.editor(300, data.content)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateLegalNotice: (copy) =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@notice.name, "Please enter a name.")
      if !@isDefault && @notice.default
        @ConfirmationDialogService.confirm("Confirm default", "You are about to change the default legal notice. Are you sure?", "Yes", "No")
        .then () =>
          @doUpdate(copy)
      else
        @doUpdate(copy)
    @alerts = @ValidationService.getAlerts()

  doUpdate: (copy) ->
    @LegalNoticeService.updateLegalNotice(@noticeId, @notice.name, @notice.description, tinymce.activeEditor.getContent(), @notice.default)
    .then (data) =>
      if (data)
        @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      else
        if copy
          @copyLegalNotice()
        else
          @cancelDetailLegalNotice()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # copy legal notice info in create screen
  copyLegalNotice: () ->
    name = @notice.name + " COPY"
    description = @notice.description
    content = tinymce.activeEditor.getContent()
    @$state.go 'app.admin.users.legalnotices.create', { name : name, description : description, content : content }

  # delete and cancel detail
  deleteLegalNotice: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this legal notice?", "Yes", "No")
    .then () =>
      @LegalNoticeService.deleteLegalNotice(@noticeId)
      .then () =>
        @cancelDetailLegalNotice()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailLegalNotice: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

####################################################################################

@ControllerUtils.register @controllers, UserManagementController
@ControllerUtils.register @controllers, AdminCreateController
@ControllerUtils.register @controllers, AdminDetailController
@ControllerUtils.register @controllers, OrganizationCreateController
@ControllerUtils.register @controllers, OrganizationDetailController
@ControllerUtils.register @controllers, UserCreateController
@ControllerUtils.register @controllers, UserDetailController
@ControllerUtils.register @controllers, LandingPageCreateController
@ControllerUtils.register @controllers, LandingPageDetailController
@ControllerUtils.register @controllers, LegalNoticeCreateController
@ControllerUtils.register @controllers, LegalNoticeDetailController