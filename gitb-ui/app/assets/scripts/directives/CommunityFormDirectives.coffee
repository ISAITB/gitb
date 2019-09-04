@directives.directive 'tbCommunityForm', ['Constants'
  (@Constants)->
    scope:
      tbCommunity: '='
      tbDomains: '='
      tbAdmin: '='
    template: ''+
      '<form class="form-horizontal" ng-submit="submit()">'+
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="sname">* Short name:</label>'+
          '<div class="col-sm-8"><input id="sname" ng-model="tbCommunity.sname" class="form-control" type="text" required></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="fname">* Full name:</label>'+
          '<div class="col-sm-8"><input id="fname" ng-model="tbCommunity.fname" class="form-control" type="text" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbAdmin">'+
          '<label class="col-sm-3 control-label" for="domainChoice">Domain:</label>'+
          '<div class="col-sm-8">'+
            '<select id="domainChoice" class="form-control" ng-model="tbCommunity.domain" ng-options="domain.sname for domain in tbDomains track by domain.id"><option value="">--Optional--</option></select>'+
          '</div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="email">Support email:</label>'+
          '<div class="col-sm-8"><input id="email" ng-model="tbCommunity.email" class="form-control" type="text"></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="selfRegType">* Self-registration method:</label>'+
          '<div class="col-sm-8">'+
            '<select id="selfRegType" class="form-control" ng-model="tbCommunity.selfRegType" ng-options="+(type.id) as type.label for type in selfRegTypes"></select>'+
          '</div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbCommunity.selfRegType == '+@Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN+' || tbCommunity.selfRegType == '+@Constants.SELF_REGISTRATION_TYPE.TOKEN+'">'+
          '<label class="col-sm-3 control-label" for="selfRegToken">* Self-registration token:</label>'+
          '<div class="col-sm-8"><input id="selfRegToken" ng-model="tbCommunity.selfRegToken" class="form-control" type="text" required></div>'+
        '</div>'+
        '<input id="domain" ng-if="!tbAdmin" ng-model="tbCommunity.domain" type="hidden">'+
      '</form>'
    restrict: 'A'
    link: (scope, element, attrs) =>
      scope.selfRegTypes = [
        {id: @Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED, label: 'Not supported'}, 
        {id: @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING, label: 'Select from public communities'}, 
        {id: @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN, label: 'Select from public communities and provide token'}, 
        {id: @Constants.SELF_REGISTRATION_TYPE.TOKEN, label: 'Provide token'}
      ]
      scope.showToken = () =>
        scope.tbCommunity.selfRegType == @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN || scope.tbCommunity.selfRegType == @Constants.SELF_REGISTRATION_TYPE.TOKEN
]