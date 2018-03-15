@directives.directive 'domainDetailsForm', [
	()->
		scope:
			domain: '='
			showSaveButton: '='
			showCancelButton: '='
			onSubmit: '='
			onCancel: '='
		template: ''+
			'<form class="form-horizontal" ng-submit="submit()">'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="shortName">* Short name:</label>'+
					'<div class="col-sm-8"><input id="shortName" ng-model="domain.sname" class="form-control" type="text" required></div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="fullName">* Full name:</label>'+
					'<div class="col-sm-8"><input id="fullName" ng-model="domain.fname" class="form-control" type="text" required></div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="description">Description:</label>'+
					'<div class="col-sm-8">'+
						'<textarea id="description" ng-model="domain.description" class="form-control"></textarea>'+
					'</div>'+
				'</div>'+
				'<div class="form-group" ng-if="showSaveButton">'+
					'<div class="col-sm-offset-3 col-sm-1">'+
						'<button class="btn btn-default" type="submit">Save</button>'+
					'</div>'+
					'<div class="col-sm-7" ng-if="showCancelButton">'+
						'<button class="btn btn-default" ng-click="onCancel()" type="button">Cancel</button>'+
					'</div>'+
				'</div>'+
			'</form>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.submit = () ->
				scope.onSubmit?()
]