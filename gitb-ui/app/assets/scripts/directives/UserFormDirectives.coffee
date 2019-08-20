@directives.directive 'tbUserForm', [
  ()->
    scope:
      tbUser: '='
      tbNew: '='
      tbAdmin: '='
      tbRoles: '='
      sso: '='
    template: ''+
      '<form class="form-horizontal">'+
        '<div class="form-group" ng-if="sso && !tbNew">'+
          '<label class="col-sm-2 control-label" for="name">Name:</label>'+
          '<div class="col-sm-8"><input id="name" ng-readOnly="true" ng-model="tbUser.name" class="form-control" type="text"></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!sso">'+
          '<label class="col-sm-3 control-label" for="name">* Name:</label>'+
          '<div class="col-sm-8"><input id="name" ng-model="tbUser.name" class="form-control" type="text" required></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label ng-class="{\'col-sm-2\': sso, \'col-sm-3\': !sso}" class="control-label" for="email"><span ng-if="tbNew">* </span>Email:</label>'+
          '<div class="col-sm-8"><input id="email" ng-model="tbUser.email" class="form-control" type="text" ng-readOnly="!tbNew" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbAdmin && !tbNew">'+
          '<label ng-class="{\'col-sm-2\': sso, \'col-sm-3\': !sso}" class="control-label" for="role"><span ng-if="tbNew">* </span>Role:</label>'+
          '<div class="col-sm-8"><input id="role" ng-model="tbUser.role.label" class="form-control" type="text" ng-readOnly="!tbNew" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbAdmin">'+
          '<label ng-class="{\'col-sm-2\': sso, \'col-sm-3\': !sso}" class="control-label" for="role">* Role:</label>'+
          '<div class="col-sm-8"><select class="form-control" ng-model="tbUser.role" ng-options="role.label for role in tbRoles track by role.id" required></select></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbNew">'+
          '<label ng-class="{\'col-sm-2\': sso, \'col-sm-3\': !sso}" class="control-label" for="status">Status:</label>'+
          '<div class="col-sm-8"><input id="status" ng-model="tbUser.ssoStatusText" class="form-control" type="text" ng-readOnly="true"></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbAdmin && !tbNew">'+
          '<label ng-class="{\'col-sm-2\': sso, \'col-sm-3\': !sso}" class="control-label" for="organization"><span ng-if="!sso">* </span>Organisation:</label>'+
          '<div class="col-sm-8"><input id="organization" ng-model="tbUser.organization.fname" class="form-control" type="text" ng-readOnly="true" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!tbNew && !sso">'+
          '<label class="col-sm-3 control-label" for="changePassword">Set one-time password?</label>'+
          '<div class="col-sm-8"><input id="changePassword" ng-model="tbUser.changePassword" class="form-check" type="checkbox" class="form-check"></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!sso && (tbNew || tbUser.changePassword)">'+
          '<label class="col-sm-3 control-label" for="password">* One-time password:</label>'+
          '<div class="col-sm-8"><input id="password" ng-model="tbUser.password" class="form-control" type="password" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="!sso && (tbNew || tbUser.changePassword)">'+
          '<label class="col-sm-3 control-label" for="cpassword">* Confirm one-time password:</label>'+
          '<div class="col-sm-8"><input id="cpassword" ng-model="tbUser.cpassword" class="form-control" type="password" required></div>'+
        '</div>'+
      '</form>'
    restrict: 'A'
]