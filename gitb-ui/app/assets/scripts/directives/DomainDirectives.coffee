@directives.directive 'domainDetailsForm', ['DataService'
	(DataService)->
		scope:
			domain: '='
		template: ''+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="shortName">* Short name:</label>'+
				'<div class="col-xs-7"><input id="shortName" ng-model="domain.sname" class="form-control" type="text" autocomplete="off"></div>'+
				'<div tb-tooltip="This is used to display the {{DataService.labelDomainLower()}} in selection lists (e.g. search filters) and tables (e.g. conformance statement creation wizard) where space is limited. Ensure this is short but still understandable by users."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="fullName">* Full name:</label>'+
				'<div class="col-xs-7"><input id="fullName" ng-model="domain.fname" class="form-control" type="text" autocomplete="off"></div>'+
				'<div tb-tooltip="This is used to display the {{DataService.labelDomainLower()}} in detail forms and reports."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="description">Description:</label>'+
				'<div class="col-xs-7">'+
					'<textarea id="description" ng-model="domain.description" class="form-control"></textarea>'+
				'</div>'+
				'<div tb-tooltip="This is used to describe and contextualise the {{DataService.labelSpecificationsLower()}} included in the {{DataService.labelDomainLower()}}. It is visible to users when managing conformance statements and may also be used as a community\'s description during self-registration (if enabled)."></div>'+
			'</div>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.DataService = DataService
]