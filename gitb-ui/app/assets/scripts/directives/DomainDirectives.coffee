@directives.directive 'domainDetailsForm', [
	()->
		scope:
			domain: '='
		template: ''+
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
			'</div>'
		restrict: 'A'
]