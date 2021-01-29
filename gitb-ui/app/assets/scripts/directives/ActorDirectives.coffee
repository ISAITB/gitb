@directives.directive 'actorDetailsForm', ['DataService'
	(DataService)->
		scope:
			actor: '='
		template: ''+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="id">* ID:</label>'+
				'<div class="col-xs-7"><input id="id" ng-model="actor.actorId" class="form-control" type="text" autocomplete="off"></div>'+
				'<div tb-tooltip="This identifier is used within test suites and test cases to refer to the {{DataService.labelActorLower()}}."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="name">* Name:</label>'+
				'<div class="col-xs-7"><input id="name" ng-model="actor.name" class="form-control" type="text" autocomplete="off"></div>'+
				'<div tb-tooltip="This is used to refer to the {{DataService.labelActorLower()}} in all screens and reports."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="description">Description:</label>'+
				'<div class="col-xs-7">'+
					'<textarea id="description" ng-model="actor.description" class="form-control"></textarea>'+
				'</div>'+
				'<div tb-tooltip="This description is presented to users to explain the purpose of the {{DataService.labelActorLower()}} when managing conformance statements."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="displayOrder">Display order:</label>'+
				'<div class="col-xs-1"><input id="displayOrder" ng-model="actor.displayOrder" class="form-control" type="text" autocomplete="off"></div>'+
				'<div tb-tooltip="A number to be considered in relation to other {{DataService.labelActorsLower()}} that is used to determine the display positioning in test execution diagrams. If unspecified a default positioning is applied based on the test case\'s steps."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="default">Specification default:</label>'+
				'<div class="col-xs-7">'+
					'<input id="default" ng-model="actor.default" type="checkbox" class="form-check">'+
					'<div tb-inline="true" tb-tooltip="The default {{DataService.labelActorLower()}} of the {{DataService.labelSpecificationLower()}} will be automatically selected when defining new conformance statements. Other {{DataService.labelActorsLower()}} will not be presented as available options."></div>'+
				'</div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="hidden">Hidden:</label>'+
				'<div class="col-xs-7">'+
					'<input id="hidden" ng-model="actor.hidden" type="checkbox" class="form-check">'+
					'<div tb-inline="true" tb-tooltip="Check this to remove this {{DataService.labelActorLower()}} as an available option when defining new conformance statements. This can be used to hide simulated {{DataService.labelActorsLower()}} when there is no clear default actor or to deprecate an {{DataService.labelActorLower()}} that should not be used anymore. In any case checking this has no effect on the existing testing history."></div>'+
				'</div>'+
			'</div>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.DataService = DataService		
]