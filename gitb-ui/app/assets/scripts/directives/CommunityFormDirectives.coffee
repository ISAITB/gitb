@directives.directive 'tbCommunityForm', [
  ()->
    scope:
      tbCommunity: '='
      tbDomains: '='
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
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="role">Domain:</label>'+
          '<div class="col-sm-8"><select class="form-control" ng-model="tbCommunity.domain" ng-options="domain.sname for domain in tbDomains track by domain.id"><option value="">--Optional--</option></select></div>'+
        '</div>'+
      '</form>'
    restrict: 'A'
]