@directives.directive 'tbOrganizationForm', [
  ()->
    scope:
      tbOrganization: '='
    template: ''+
      '<form class="form-horizontal" ng-submit="submit()">'+
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="sname">* Short name:</label>'+
          '<div class="col-sm-8"><input id="sname" ng-model="tbOrganization.sname" class="form-control" type="text" required></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="fname">* Full name:</label>'+
          '<div class="col-sm-8"><input id="fname" ng-model="tbOrganization.fname" class="form-control" type="text" required></div>'+
        '</div>'+
      '</form>'
    restrict: 'A'
]