@directives.directive 'specDetailsForm', [
	()->
		scope:
			specification: '='
			showSaveButton: '='
			onSubmit: '='
		template: ''+
			'<form class="form-horizontal" ng-submit="submit()">'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="shortName">* Short name:</label>'+
					'<div class="col-sm-8"><input id="shortName" ng-model="specification.sname" class="form-control" type="text" required></div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="fullName">* Full name:</label>'+
					'<div class="col-sm-8"><input id="fullName" ng-model="specification.fname" class="form-control" type="text" required></div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="urls">Related URLs:</label>'+
					'<div class="col-sm-8">'+
						'<textarea id="urls" ng-model="specification.urls" class="form-control"></textarea>'+
						'<span class="help-block">URLs should be seperated with "," without spaces</span>'+
					'</div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="diagram">Diagram:</label>'+
					'<div class="col-sm-8">'+
						'<input id="diagram" ng-model="specification.diagram" class="form-control" type="text">'+
						'<span class="help-block">URL of the diagram describing the specification</span>'+
					'</div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="description">Description:</label>'+
					'<div class="col-sm-8">'+
						'<textarea id="description" ng-model="specification.description" class="form-control"></textarea>'+
					'</div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="specificationType">Specification Type:</label>'+
					'<div class="col-sm-8">'+
						'<select id="specificationType" ng-model="specification.spec_type" class="form-control">'+
							'<option value="1">Integration Profile</option>'+
							'<option value="2">Content Specification</option>'+
						'</select>'+
					'</div>'+
				'</div>'+
				'<div class="form-group" ng-if="showSaveButton">'+
					'<div class="col-sm-offset-3 col-sm-8">'+
						'<button class="btn btn-default" type="submit">Create specification</button>'+
					'</div>'+
				'</div>'+
			'</form>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.submit = () ->
				scope.onSubmit?()
]