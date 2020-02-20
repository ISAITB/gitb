@directives.directive 'tbUserForm', ['DataService'
  (DataService)->
    scope:
      tbUser: '='
      tbNew: '='
      tbAdmin: '='
      tbRoles: '='
      sso: '='
    template: ''+
      '<form class="form-horizontal">'+
        '<div class="form-group" ng-if="sso && !tbNew">'+
          '<label class="col-sm-3 control-label" for="name">Name:</label>'+
          '<div class="col-sm-7"><input id="name" ng-readOnly="true" ng-model="tbUser.name" class="form-control" type="text"></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!sso">'+
          '<label class="col-sm-4 control-label" for="name">* Name:</label>'+
          '<div class="col-sm-7"><input id="name" ng-model="tbUser.name" class="form-control" type="text" required></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label ng-class="{\'col-sm-3\': sso, \'col-sm-4\': !sso}" class="control-label" for="email"><span ng-if="tbNew">* </span>Email:</label>'+
          '<div class="col-sm-7"><input id="email" ng-model="tbUser.email" class="form-control" type="text" ng-readOnly="!tbNew" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbAdmin && !tbNew">'+
          '<label ng-class="{\'col-sm-3\': sso, \'col-sm-4\': !sso}" class="control-label" for="role"><span ng-if="tbNew">* </span>Role:</label>'+
          '<div class="col-sm-7"><input id="role" ng-model="tbUser.role.label" class="form-control" type="text" ng-readOnly="!tbNew" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbAdmin">'+
          '<label ng-class="{\'col-sm-3\': sso, \'col-sm-4\': !sso}" class="control-label" for="role">* Role:</label>'+
          '<div class="col-sm-7"><select class="form-control" ng-model="tbUser.role" ng-options="role.label for role in tbRoles track by role.id" required></select></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbNew">'+
          '<label ng-class="{\'col-sm-3\': sso, \'col-sm-4\': !sso}" class="control-label" for="status">Status:</label>'+
          '<div class="col-sm-7"><input id="status" ng-model="tbUser.ssoStatusText" class="form-control" type="text" ng-readOnly="true"></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbAdmin && !tbNew">'+
          '<label ng-class="{\'col-sm-3\': sso, \'col-sm-4\': !sso}" class="control-label" for="organization"><span ng-if="!sso">* </span>{{DataService.labelOrganisation()}}:</label>'+
          '<div class="col-sm-7"><input id="organization" ng-model="tbUser.organization.fname" class="form-control" type="text" ng-readOnly="true" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbNew && !sso">'+
          '<label class="col-sm-4 control-label" for="changePassword">Set one-time password?</label>'+
          '<div class="col-sm-7"><input id="changePassword" ng-model="tbUser.changePassword" class="form-check" type="checkbox" class="form-check"></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!sso && (tbNew || tbUser.changePassword)">'+
          '<label class="col-sm-4 control-label" for="password">* One-time password:</label>'+
          '<div class="col-sm-7"><input id="password" ng-model="tbUser.password" class="form-control" type="password" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!sso && (tbNew || tbUser.changePassword)">'+
          '<label class="col-sm-4 control-label" for="cpassword">* Confirm one-time password:</label>'+
          '<div class="col-sm-7"><input id="cpassword" ng-model="tbUser.cpassword" class="form-control" type="password" required></div>'+
        '</div>'+
      '</form>'
    restrict: 'A'
    link: (scope, element, attrs) ->
      scope.DataService = DataService

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
          '<div class="form-group">'+
              '<label class="col-xs-3 control-label" for="community">* Community:</label>'+
              '<div class="col-xs-7">'+
                  '<select ng-if="selfRegOptions.length > 1" ng-change="communityChanged()" id="community" class="form-control" ng-model="model.selfRegOption" ng-options="option as option.communityName for option in selfRegOptions"><option value=""></option></select>'+
                  '<div ng-if="selfRegOptions.length == 1">'+
                      '<input type="text" id="community" class="form-control" ng-model="model.selfRegOption.communityName" readonly="true"/>'+
                      '<input type="hidden" id="communityId" ng-model="model.selfRegOption"/>'+
                  '</div>'+
              '</div>'+
          '</div>'+
          '<div ng-if="model.selfRegOption.communityId">' +
            '<div class="form-group" ng-if="model.selfRegOption.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN">'+
                '<label class="col-xs-3 control-label" for="token">* Token:</label>'+
                '<div class="col-xs-7">'+
                    '<input id="token" ng-model="model.selfRegToken" class="form-control" type="text"/>'+
                '</div>'+
                '<div class="form-control-static"><span uib-tooltip="A community-specific token needs to be provided to allow registration. You need to request this from the community\'s administrator."><i class="fa fa-question-circle"></i></span></div>'+
            '</div>'+
            '<div ng-class="{\'form-separator-popup\': sso, \'form-separator\': !sso}">'+
                '<h4 class="title">{{DataService.labelOrganisation()}} details <span uib-tooltip="This information defines the member of the selected community through which will you be testing for conformance"><i class="fa fa-question-circle"></i></span></h4>'+
            '</div>'+
            '<div class="form-group">'+
                '<label class="col-xs-3 control-label" for="orgShortName">* Short name:</label>'+
                '<div class="col-xs-7">'+
                    '<input id="orgShortName" ng-model="model.orgShortName" class="form-control" type="text"/>'+
                '</div>'+
                '<div class="form-control-static"><span uib-tooltip="A short name for your {{DataService.labelOrganisationLower()}} to appear in reports"><i class="fa fa-question-circle"></i></span></div>'+
            '</div>'+
            '<div class="form-group">'+
                '<label class="col-xs-3 control-label" for="orgFullName">* Full name:</label>'+
                '<div class="col-xs-7">'+
                    '<input id="orgFullName" ng-model="model.orgFullName" class="form-control" type="text"/>'+
                '</div>'+
                '<div class="form-control-static"><span uib-tooltip="The complete name of your {{DataService.labelOrganisationLower()}}"><i class="fa fa-question-circle"></i></span></div>'+
            '</div>'+
            '<div class="form-group" ng-if="model.selfRegOption.templates.length > 0">'+
                '<label class="col-xs-3 control-label" for="template">Configuration:</label>'+
                '<div class="col-xs-7">'+
                    '<select id="template" class="form-control" ng-model="model.template" ng-options="template as template.name for template in model.selfRegOption.templates"><option value=""></option></select>'+
                '</div>'+
                '<div class="form-control-static"><span uib-tooltip="Predefined test configuration(s) curated by the community administrator"><i class="fa fa-question-circle"></i></span></div>'+
            '</div>'+
            '<div ng-if="!sso">'+
              '<div class="form-separator">'+
                  '<h4 class="title">Administrator account details <span uib-tooltip="Through your administrator account you can manage your {{DataService.labelOrganisationLower()}}  users, configure your conformance statements and execute tests"><i class="fa fa-question-circle"></i></span></h4>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminName">* Name:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminName" ng-model="model.adminName" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div class="form-control-static"><span uib-tooltip="This may be your name or a description in case this account will be shared by multiple people"><i class="fa fa-question-circle"></i></span></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminEmail">* Email:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminEmail" ng-model="model.adminEmail" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div class="form-control-static"><span uib-tooltip="This is used only during login as an email-formatted username (no emails will ever be sent to this address)"><i class="fa fa-question-circle"></i></span></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminPassword">* Password:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminPassword" ng-model="model.adminPassword" class="form-control" type="password"/>'+
                  '</div>'+
                  '<div class="form-control-static"><span uib-tooltip="The account\'s password, to be provided with the email when logging in"><i class="fa fa-question-circle"></i></span></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminPasswordConfirm">* Confirm password:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminPasswordConfirm" ng-model="model.adminPasswordConfirm" class="form-control" type="password"/>'+
                  '</div>'+
                  '<div class="form-control-static"><span uib-tooltip="Provide again the account\'s password to ensure it is correct"><i class="fa fa-question-circle"></i></span></div>'+
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
      if scope.selfRegOptions == undefined
        CommunityService.getSelfRegistrationOptions()
        .then((data) =>
          if data.length == 1
              scope.model.selfRegOption = data[0]
          scope.selfRegOptions = data
        )
      else
        if scope.selfRegOptions.length == 1
            scope.model.selfRegOption = scope.selfRegOptions[0]
      
      scope.communityChanged = () =>
        if scope.model?.selfRegOption?
          scope.DataService.setupLabels(scope.model.selfRegOption.labels)
]