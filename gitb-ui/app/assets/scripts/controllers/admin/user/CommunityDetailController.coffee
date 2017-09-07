class CommunityDetailController

  @$inject = ['$log', '$state', '$window', '$stateParams', 'UserService', 'DataService', 'Constants', 'LandingPageService', 'LegalNoticeService', 'ValidationService', 'ConfirmationDialogService', 'OrganizationService', 'CommunityService', 'ConformanceService', 'ErrorService']
  constructor: (@$log, @$state, @$window, @$stateParams, @UserService, @DataService, @Constants, @LandingPageService, @LegalNoticeService, @ValidationService, @ConfirmationDialogService, @OrganizationService, @CommunityService, @ConformanceService, @ErrorService) ->

    @communityId = @$stateParams.community_id

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

    @organizationColumns = [
      {
        field: 'sname',
        title: 'Short name'
      }
      {
        field: 'fname',
        title: 'Full name'
      }
    ]

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

    @community = {}
    @domains = {}
    @admins = []
    @organizations = []
    @landingPages = []
    @legalNotices = []
    @alerts = []

    @LegalNoticeService.getCommunityDefaultLegalNotice(@Constants.DEFAULT_COMMUNITY_ID)
    .then (data) =>
      @testBedLegalNotice = data if data.exists
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LandingPageService.getCommunityDefaultLandingPage(@Constants.DEFAULT_COMMUNITY_ID)
    .then (data) =>
      @testBedLandingPage = data if data.exists
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @UserService.getCommunityAdministrators(@communityId)
    .then (data) =>
      @admins = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @CommunityService.getCommunityById(@communityId)
    .then (data) =>
      @community = data
      @$window.localStorage['community'] = angular.toJson data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ConformanceService.getDomains()
    .then (data) =>
      @domains = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LandingPageService.getLandingPagesByCommunity(@communityId)
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LegalNoticeService.getLegalNoticesByCommunity(@communityId)
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @OrganizationService.getOrganizationsByCommunity(@communityId)
    .then (data) =>
      @organizations = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  updateCommunity: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@community.sname, "Please enter short name of the community.") &
    @ValidationService.requireNonNull(@community.fname, "Please enter full name of the community.")
      @CommunityService.updateCommunity(@communityId, @community.sname, @community.fname, @community.domain?.id)
      .then () =>
        @cancelCommunityDetail()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  deleteCommunity: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this community?", "Yes", "No")
    .then () =>
      @CommunityService.deleteCommunity(@communityId)
      .then () =>
        @cancelCommunityDetail()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  organizationSelect: (organization) =>
    @$state.go 'app.admin.users.communities.detail.organizations.detail.list', { org_id : organization.id }

  isDefaultCommunity: () =>
    @communityId == @Constants.DEFAULT_COMMUNITY_ID

  landingPageSelect: (landingPage) =>
    @$state.go 'app.admin.users.communities.detail.landingpages.detail', { page_id : landingPage.id }

  legalNoticeSelect: (legalNotice) =>
    @$state.go 'app.admin.users.communities.detail.legalnotices.detail', { notice_id : legalNotice.id }

  adminSelect: (admin) =>
    @$state.go 'app.admin.users.communities.detail.admins.detail', { admin_id : admin.id }

  cancelCommunityDetail: () =>
    @$state.go 'app.admin.users.list'

  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

@controllers.controller 'CommunityDetailController', CommunityDetailController