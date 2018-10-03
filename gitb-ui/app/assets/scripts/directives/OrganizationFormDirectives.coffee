@directives.directive 'tbOrganizationForm', [
  ()->
    scope:
      tbOrganization: '='
      tbLandingPages: '='
      tbLegalNotices: '='
      tbOtherOrganisations: '='
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
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="role">Landing page:</label>'+
          '<div class="col-sm-8"><select class="form-control" ng-model="tbOrganization.landingPages" ng-options="page.name for page in tbLandingPages track by page.id"><option value="">--Default Page--</option></select></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="role">Legal notice:</label>'+
          '<div class="col-sm-8"><select class="form-control" ng-model="tbOrganization.legalNotices" ng-options="notice.name for notice in tbLegalNotices track by notice.id"><option value="">--Default Legal Notice--</option></select></div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbOtherOrganisations.length">'+
          '<label class="col-sm-3 control-label" for="role">Copy test setup from:</label>'+
          '<div class="col-sm-8"><select class="form-control" ng-model="tbOrganization.otherOrganisations" ng-options="organisation.sname for organisation in tbOtherOrganisations track by organisation.id"><option value=""></option></select></div>'+
        '</div>'+
      '</form>'
    restrict: 'A'
]