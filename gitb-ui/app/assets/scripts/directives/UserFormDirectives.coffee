@directives.directive 'tbUserForm', ['DataService'
  (DataService)->
    scope:
      tbUser: '='
      tbNew: '='
      tbAdmin: '='
      tbRoles: '='
      sso: '='
    template: ''+
      '<div class="form-group" ng-if="sso && !tbNew">'+
        '<label class="col-xs-3 control-label" for="name">Name:</label>'+
        '<div class="col-xs-6"><input id="name" ng-readOnly="true" ng-model="tbUser.name" class="form-control" type="text"></div>'+
        '<div tb-tooltip="The name of the user retrieved from her EU Login account."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!sso">'+
        '<label class="col-xs-4 control-label" for="name">* Name:</label>'+
        '<div class="col-xs-6"><input id="name" ng-model="tbUser.name" class="form-control" type="text"></div>'+
        '<div tb-tooltip="The name of the user. This is used for display purposes when listing users."></div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="email"><span ng-if="tbNew">* </span>Email:</label>'+
        '<div class="col-xs-6"><input id="email" ng-model="tbUser.email" class="form-control" type="text" ng-readOnly="!tbNew"></div>'+
        '<div ng-if="sso" tb-tooltip="The email address linked to the user\'s EU Login account."></div>'+
        '<div ng-if="!sso" tb-tooltip="An email-formatted username for this user that is used for logging in. No emails are ever sent to this."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="tbAdmin && !tbNew">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="role"><span ng-if="tbNew">* </span>Role:</label>'+
        '<div class="col-xs-6"><input id="role" ng-model="tbUser.role.label" class="form-control" type="text" ng-readOnly="!tbNew"></div>'+
        '<div tb-tooltip="The user\'s assigned role."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbAdmin">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="role">* Role:</label>'+
        '<div class="col-xs-6"><select id="role" class="form-control" ng-model="tbUser.role" ng-options="role.label for role in tbRoles track by role.id"></select></div>'+
        '<div tb-tooltip="The user\'s assigned role. A \'user\' can view information and execute test sessions whereas an \'administrator\' can also manage users and edit test configuration."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbNew">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="status">Status:</label>'+
        '<div class="col-xs-6"><input id="status" ng-model="tbUser.ssoStatusText" class="form-control" type="text" ng-readOnly="true"></div>'+
        '<div tb-tooltip="The activation status of the user\'s account."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbAdmin && !tbNew">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="organization"><span ng-if="!sso">* </span>{{DataService.labelOrganisation()}}:</label>'+
        '<div class="col-xs-6"><input id="organization" ng-model="tbUser.organization.fname" class="form-control" type="text" ng-readOnly="true"></div>'+
        '<div tb-tooltip="The {{DataService.labelOrganisationLower()}} this user is a member of."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbNew && !sso">'+
        '<label class="col-xs-4 control-label" for="changePassword">Set one-time password?</label>'+
        '<div class="col-xs-6"><input id="changePassword" ng-model="tbUser.changePassword" class="form-check" type="checkbox" class="form-check" ng-change="setPasswordClicked()"></div>'+
      '</div>'+
      '<div ng-if="!sso" uib-collapse="!tbNew && !tbUser.changePassword">' +
        '<div class="form-group">'+
          '<label class="col-xs-4 control-label" for="password">* One-time password:</label>'+
          '<div class="col-xs-6"><input id="password" ng-model="tbUser.password" class="form-control" type="password"></div>'+
          '<div tb-tooltip="This password can only be used once at first login and will then need to be changed."></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label class="col-xs-4 control-label" for="cpassword">* Confirm one-time password:</label>'+
          '<div class="col-xs-6"><input id="cpassword" ng-model="tbUser.cpassword" class="form-control" type="password"></div>'+
          '<div tb-tooltip="Repeat the password to ensure it is correctly provided."></div>'+
        '</div>'+
      '</div>'
    restrict: 'A'
    link: (scope, element, attrs) ->
      scope.DataService = DataService
      scope.setPasswordClicked = () ->
        if scope.tbUser.changePassword
          scope.DataService.focus('password', 200)

]

@directives.directive 'tbSelfRegForm', ['Constants', 'CommunityService', 'DataService'
  (Constants, CommunityService, DataService)->
    scope:
      model: '='
      selfRegOptions: '='
    template: ''+
      '<div>'+
        '<div ng-if="selfRegOptions.length == 0">'+
            '<div ng-class="{\'link-account-separator\': sso}">'+
                '<div class="bg-info div-rounded div-padded">'+
                    '<span>There are currently no communities available that allow self-registration.</span>'+
                '</div>'+
            '</div>'+
        '</div>'+
        '<div ng-if="selfRegOptions.length > 0">'+
          '<div ng-class="{\'link-account-separator\': sso, \'bottom-margin\': !sso}">'+
              '<div class="bg-info div-rounded div-padded">'+
                  '<p>Select the community you want to register for and provide the requested information. If your community is not listed you need to request from an administrator to register you on your behalf.</p>'+
                  '<span ng-if="sso"><b>Privacy note:</b> By registering you grant your consent to link your name, email and EU Login user ID to your new administrator account.</span>'+
                  '<span ng-if="!sso"><b>Privacy note:</b> By registering you grant your consent to link the provided information to your new administrator account.</span>'+
              '</div>'+
          '</div>'+
          '<div ng-class="{\'form-separator-popup\': sso, \'form-separator form-separator-top-padding\': !sso}">'+
              '<h4 class="title">Community</h4>'+
          '</div>'+
          '<div table-directive row-style="optionRowStyle" ng-class="{\'self-reg-option-table-popup\': sso}" class="self-reg-option-table" columns="communityColumns" data="selfRegOptions" on-select="communitySelected"/>'+
          '<div uib-collapse="!model.selfRegOption.communityId">' +
            '<div ng-if="model.selfRegOption.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN">'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="token">* Registration token:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="token" ng-model="model.selfRegToken" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div tb-tooltip="A community-specific token needs to be provided to allow registration. You need to request this from the community\'s administrator."></div>'+
              '</div>'+
              '<div class="form-group" ng-if="model.selfRegOption.communityEmail" style="margin-bottom: -20px;">'+
                  '<div class="col-xs-offset-3 col-xs-9">'+
                      '<div class="form-control-static inline-form-text">You can request this from the community\'s support team at <a ng-href="mailto:{{model.selfRegOption.communityEmail}}">{{model.selfRegOption.communityEmail}}</a>.</div>'+
                  '</div>'+
              '</div>'+
            '</div>'+
            '<div ng-class="{\'form-separator-popup\': sso, \'form-separator\': !sso}" class="form-separator-top-padding">'+
                '<h4 class="title">{{DataService.labelOrganisation()}} details <span uib-tooltip="This information defines the member of the selected community through which will you be testing for conformance"><i class="fa fa-question-circle"></i></span></h4>'+
            '</div>'+
            '<div class="form-group">'+
                '<label class="col-xs-3 control-label" for="orgShortName">* Short name:</label>'+
                '<div class="col-xs-7">'+
                    '<input id="orgShortName" ng-model="model.orgShortName" class="form-control" type="text"/>'+
                '</div>'+
                '<div tb-tooltip="A short name for your {{DataService.labelOrganisationLower()}} to appear in reports."></div>'+
            '</div>'+
            '<div class="form-group">'+
                '<label class="col-xs-3 control-label" for="orgFullName">* Full name:</label>'+
                '<div class="col-xs-7">'+
                    '<input id="orgFullName" ng-model="model.orgFullName" class="form-control" type="text"/>'+
                '</div>'+
                '<div tb-tooltip="The complete name of your {{DataService.labelOrganisationLower()}}."></div>'+
            '</div>'+
            '<div class="form-group" ng-if="model.selfRegOption.templates.length > 0">'+
                '<label class="col-xs-3 control-label" for="template">Configuration:</label>'+
                '<div class="col-xs-7">'+
                    '<select id="template" class="form-control" ng-model="model.template" ng-options="template as template.name for template in model.selfRegOption.templates"><option value=""></option></select>'+
                '</div>'+
                '<div tb-tooltip="Predefined test configuration(s) curated by the community administrator."></div>'+
            '</div>'+

            '<div class="row" ng-if="model.selfRegOption.organisationProperties.length > 0">'+
              '<div class="col-xs-12">'+
                '<div ng-class="{\'form-separator-popup selfreg\': sso, \'form-separator\': !sso}"/>'+
              '</div>'+
            '</div>'+

            '<div tb-custom-properties-form tb-properties="model.selfRegOption.organisationProperties" tb-show-form-header="false" tb-form-padded="false" tb-col-input-less="1"/>'+
            '<div ng-if="!sso">'+
              '<div class="form-separator form-separator-top-padding">'+
                  '<h4 class="title">Administrator account details <span uib-tooltip="Through your administrator account you can manage your {{DataService.labelOrganisationLower()}}  users, configure your conformance statements and execute tests"><i class="fa fa-question-circle"></i></span></h4>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminName">* Name:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminName" ng-model="model.adminName" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div tb-tooltip="This may be your name or a description in case this account will be shared by multiple people."></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminEmail">* Email:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminEmail" ng-model="model.adminEmail" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div tb-tooltip="This is used only during login as an email-formatted username (no emails will ever be sent to this address)."></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminPassword">* Password:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminPassword" ng-model="model.adminPassword" class="form-control" type="password"/>'+
                  '</div>'+
                  '<div tb-tooltip="The account\'s password, to be provided with the email when logging in."></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminPasswordConfirm">* Confirm password:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminPasswordConfirm" ng-model="model.adminPasswordConfirm" class="form-control" type="password"/>'+
                  '</div>'+
                  '<div tb-tooltip="Provide again the account\'s password to ensure it is correct."></div>'+
              '</div>'+
            '</div>'+
          '</div>'+
        '</div>'+
      '</div>'
    restrict: 'A'
    link: (scope, element, attrs) ->
      scope.Constants = Constants
      scope.DataService = DataService
      scope.sso = DataService.configuration['sso.enabled']
      scope.communityColumns = [
        {
          field: 'communityName',
          title: 'Name'
        }
        {
          field: 'communityDescription',
          title: 'Description'
        }
      ]
      scope.DataService.setupLabels()
      scope.communitySelected = (option) =>
        scope.model.selfRegOption = option
        scope.communityChanged()

      scope.setFormFocus = () =>
        if scope.model?.selfRegOption?
          if scope.model.selfRegOption.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN
            scope.DataService.focus('token', 200)
          else 
            scope.DataService.focus('orgShortName', 200)
      if scope.selfRegOptions == undefined
        CommunityService.getSelfRegistrationOptions()
        .then((data) =>
          scope.selfRegOptions = data
          if data.length == 1
            scope.model.selfRegOption = data[0]
            scope.DataService.setupLabels(scope.model.selfRegOption.labels)
            scope.setFormFocus()
        )
      else
        if scope.selfRegOptions.length == 1
          scope.model.selfRegOption = scope.selfRegOptions[0]
          scope.DataService.setupLabels(scope.model.selfRegOption.labels)
          scope.setFormFocus()
      
      scope.communityChanged = () =>
        if scope.model?.selfRegOption?
          scope.DataService.setupLabels(scope.model.selfRegOption.labels)
          scope.setFormFocus()

      scope.optionRowStyle = () =>
        if scope.selfRegOptions.length > 1
          ""
        else
          "selected"

]

@directives.directive 'tbTooltip', [
  () ->
    scope:
      tbTooltip: '@'
      tbInline: '<?'
      tbInlineType: '@?'
    restrict: 'A'
    template: '<div ng-class="{\'form-control-static\': (!tbInline || tbInlineType == \'button\'), \'checkLabel\': (tbInline && tbInlineType == \'checkLabel\'), \'check\': (tbInline && tbInlineType == \'check\'), \'radio\': (tbInline && tbInlineType == \'radio\')}"><span uib-tooltip="{{tbTooltip}}"><i class="fa fa-question-circle"></i></span></div>'
    link: (scope, element, attrs) ->
      if scope.tbInline == undefined
        scope.tbInline = false
      if scope.tbInlineType == undefined
        scope.tbInlineType = 'check'
      if scope.tbInline
        angular.element(element).addClass('inline-tooltip');
      
]
