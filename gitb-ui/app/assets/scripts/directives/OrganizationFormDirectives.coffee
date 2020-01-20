@directives.directive 'tbOrganizationForm', ['DataService'
  (@DataService)->
    scope:
      tbOrganization: '='
      tbLandingPages: '='
      tbLegalNotices: '='
      tbErrorTemplates: '='
      tbOtherOrganisations: '='
      tbCopyChanged: '='
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
        '<div class="form-group">'+
          '<label class="col-sm-3 control-label" for="template">Error template:</label>'+
          '<div class="col-sm-8"><select class="form-control" ng-model="tbOrganization.errorTemplates" ng-options="template.name for template in tbErrorTemplates track by template.id"><option value="">--Default Error Template--</option></select></div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbOtherOrganisations.length">'+
          '<label class="col-sm-3 control-label" for="role">Copy test setup from:</label>'+
          '<div class="col-sm-8"><select ng-change="tbCopyChanged()" class="form-control" ng-model="tbOrganization.otherOrganisations" ng-options="organisation.sname for organisation in tbOtherOrganisations track by organisation.id"><option value=""></option></select></div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbOrganization.otherOrganisations">'+
          '<label class="col-sm-3 control-label">Copy also:</label>'+
          '<div class="col-sm-9">'+
            '<label class="checkbox-inline">'+
              '<input type="checkbox" ng-change="tbCopyChanged()" ng-model="tbOrganization.copyOrganisationParameters">{{DataService.labelOrganisation()}} properties'+
            '</label>'+
            '<label class="checkbox-inline">'+
              '<input type="checkbox" ng-change="tbCopyChanged()" ng-model="tbOrganization.copySystemParameters">{{DataService.labelSystem()}} properties'+
            '</label>'+
            '<label class="checkbox-inline">'+
              '<input type="checkbox" ng-change="tbCopyChanged()" ng-model="tbOrganization.copyStatementParameters">Conformance statement configurations'+
            '</label>'+
          '</div>'+
        '</div>'+
        '<div ng-if="selfRegEnabled">'+
          '<div class="form-group">'+
            '<label class="col-sm-3 control-label" for="template">Publish as template:</label>'+
            '<div class="col-sm-8"><input id="template" ng-model="tbOrganization.template" type="checkbox" class="form-check"></div>'+
          '</div>'+
          '<div class="form-group" ng-if="tbOrganization.template">'+
            '<label class="col-sm-3 control-label" for="templateName">* Template name:</label>'+
            '<div class="col-sm-8"><input id="templateName" ng-model="tbOrganization.templateName" class="form-control" type="text" required></div>'+
          '</div>'+
        '</div>'+
      '</form>'
    restrict: 'A'
    link: (scope, element, attrs) =>
      scope.selfRegEnabled = @DataService.configuration['registration.enabled']
      scope.DataService = @DataService
]