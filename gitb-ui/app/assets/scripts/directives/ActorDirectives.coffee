@directives.directive 'actorDetailsForm', [
	()->
		scope:
			actor: '='
		template: ''+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="id">* ID:</label>'+
				'<div class="col-sm-8"><input id="id" ng-model="actor.actorId" class="form-control" type="text" required></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="name">* Name:</label>'+
				'<div class="col-sm-8"><input id="name" ng-model="actor.name" class="form-control" type="text" required></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="description">Description:</label>'+
				'<div class="col-sm-8">'+
					'<textarea id="description" ng-model="actor.description" class="form-control"></textarea>'+
				'</div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="displayOrder">Display order:</label>'+
				'<div class="col-sm-1"><input id="displayOrder" ng-model="actor.displayOrder" class="form-control" type="text"></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="default">Specification default:</label>'+
				'<div class="col-sm-8"><input id="default" ng-model="actor.default" type="checkbox" class="form-check"></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-sm-3 control-label" for="hidden">Hidden:</label>'+
				'<div class="col-sm-8"><input id="hidden" ng-model="actor.hidden" type="checkbox" class="form-check"></div>'+
			'</div>'
		restrict: 'A'
]