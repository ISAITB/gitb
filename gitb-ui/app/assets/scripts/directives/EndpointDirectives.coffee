@directives.directive 'endpointDetailsForm', [
	()->
		scope:
			endpoint: '='
			showSaveButton: '='
			onSubmit: '='
		template: ''+
			'<form class="form-horizontal" ng-submit="submit()">'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="name">*Name:</label>'+
					'<div class="col-sm-8"><input id="name" ng-model="endpoint.name" class="form-control" type="text" required></div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="description">Description:</label>'+
					'<div class="col-sm-8">'+
						'<textarea id="description" ng-model="endpoint.desc" class="form-control"></textarea>'+
					'</div>'+
				'</div>'+
				'<div class="form-group" ng-if="showSaveButton">'+
					'<div class="col-sm-offset-3 col-sm-8">'+
						'<button class="btn btn-default" type="submit">Save endpoint</button>'+
					'</div>'+
				'</div>'+
			'</form>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.submit = () ->
				scope.onSubmit?()
]