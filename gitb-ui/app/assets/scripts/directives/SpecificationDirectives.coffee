@directives.directive 'specDetailsForm', ['DataService'
	(DataService)->
		scope:
			specification: '='
		template: ''+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="shortName">* Short name:</label>'+
				'<div class="col-sm-7"><input id="shortName" ng-model="specification.sname" class="form-control" type="text" required></div>'+
				'<div tb-tooltip="This is used to display the {{DataService.labelSpecificationLower()}} in selection lists (e.g. search filters) and tables (e.g. conformance statement creation wizard) where space is limited. Ensure this is short but still understandable by users."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="fullName">* Full name:</label>'+
				'<div class="col-sm-7"><input id="fullName" ng-model="specification.fname" class="form-control" type="text" required></div>'+
				'<div tb-tooltip="This is used to display the {{DataService.labelSpecificationLower()}} in detail forms and reports."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="description">Description:</label>'+
				'<div class="col-sm-7">'+
					'<textarea id="description" ng-model="specification.description" class="form-control"></textarea>'+
				'</div>'+
				'<div tb-tooltip="This is used to describe the {{DataService.labelSpecificationLower()}}. It is visible to users when managing conformance statements."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="hidden">Hidden:</label>'+
				'<div class="col-sm-8">'+
					'<input id="hidden" ng-model="specification.hidden" type="checkbox" class="form-check">'+
					'<div tb-tooltip="Check this to hide the {{DataService.labelSpecificationLower()}} as an available option for new conformance statements. Doing so effectively deprecates the {{DataService.labelSpecificationLower()}} but keeps testing history intact." tb-inline="true"></div>'+
				'</div>'+
			'</div>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.DataService = DataService
]