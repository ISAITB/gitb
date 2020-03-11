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
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="sname">* Short name:</label>'+
        '<div class="col-xs-7"><input id="sname" ng-model="tbOrganization.sname" class="form-control" type="text"></div>'+
        '<div tb-tooltip="A short name to refer to the {{DataService.labelOrganisationLower()}} used in selection lists and tabular displays where space is limited."></div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="fname">* Full name:</label>'+
        '<div class="col-xs-7"><input id="fname" ng-model="tbOrganization.fname" class="form-control" type="text"></div>'+
        '<div tb-tooltip="The full name of the {{DataService.labelOrganisationLower()}} used in reports and detail screens."></div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="role">Landing page:</label>'+
        '<div class="col-xs-7"><select class="form-control" ng-model="tbOrganization.landingPages" ng-options="page.name for page in tbLandingPages track by page.id"><option value="">--Default Page--</option></select></div>'+
        '<div tb-tooltip="You can provide a specific landing page for the {{DataService.labelOrganisationLower()}} with custom information. If unspecified the community default applies."></div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="role">Legal notice:</label>'+
        '<div class="col-xs-7"><select class="form-control" ng-model="tbOrganization.legalNotices" ng-options="notice.name for notice in tbLegalNotices track by notice.id"><option value="">--Default Legal Notice--</option></select></div>'+
        '<div tb-tooltip="You can provide a specific legal notice for the {{DataService.labelOrganisationLower()}} with custom information. If unspecified the community default applies."></div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="template">Error template:</label>'+
        '<div class="col-xs-7"><select class="form-control" ng-model="tbOrganization.errorTemplates" ng-options="template.name for template in tbErrorTemplates track by template.id"><option value="">--Default Error Template--</option></select></div>'+
        '<div tb-tooltip="You can provide a specific error template for the {{DataService.labelOrganisationLower()}} with custom information. If unspecified the community default applies."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="tbOtherOrganisations.length">'+
        '<label class="col-xs-3 control-label" for="role">Copy test setup from:</label>'+
        '<div class="col-xs-7"><select ng-change="tbCopyChanged()" class="form-control" ng-model="tbOrganization.otherOrganisations" ng-options="organisation.sname for organisation in tbOtherOrganisations track by organisation.id"><option value=""></option></select></div>'+
        '<div tb-tooltip="Select another {{DataService.labelOrganisationLower()}} to serve as a template for this one. The configuration from the selected {{DataService.labelOrganisationLower()}} will be copied to the current one."></div>'+
      '</div>'+
      '<div class="form-group" uib-collapse="!tbOrganization.otherOrganisations">'+
        '<label class="col-xs-3 control-label">Copy also:</label>'+
        '<div class="col-xs-8">'+
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
          '<label class="col-xs-3 control-label" for="template">Publish as template:</label>'+
          '<div class="col-xs-7">'+
            '<input id="template" ng-model="tbOrganization.template" type="checkbox" class="form-check" ng-change="templateChoiceChanged()">'+
            '<div tb-inline="true" tb-tooltip="You can define the configuration of this {{DataService.labelOrganisationLower()}} as a template that will be presented to users self-registering for the community (if self-registration is enabled). Do this to offer new users preconfigured {{DataService.labelSystemsLower()}} and conformance statements."></div>'+
          '</div>'+
        '</div>'+
        '<div class="form-group" uib-collapse="!tbOrganization.template">'+
          '<label class="col-xs-3 control-label" for="templateName">* Template name:</label>'+
          '<div class="col-xs-7"><input id="templateName" ng-model="tbOrganization.templateName" class="form-control" type="text"></div>'+
          '<div tb-tooltip="The name to display in the self-registration screen for this template configuration."></div>'+
        '</div>'+
      '</div>'
    restrict: 'A'
    link: (scope, element, attrs) =>
      scope.DataService = @DataService
      scope.selfRegEnabled = @DataService.configuration['registration.enabled']
      scope.DataService = @DataService
      scope.templateChoiceChanged = () =>
        if scope.tbOrganization.template
          scope.DataService.focus('templateName', 200)
]