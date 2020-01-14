@directives.directive 'endpointDetailsForm', [
	()->
		scope:
			endpoint: '='
			showSaveButton: '='
			showCancelButton: '='
			onSubmit: '='
			onCancel: '='
			saveDisabled: '='
		template: ''+
			'<form class="form-horizontal" ng-submit="submit()">'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="name">* Name:</label>'+
					'<div class="col-sm-8"><input id="name" ng-model="endpoint.name" class="form-control" type="text" required></div>'+
				'</div>'+
				'<div class="form-group">'+
					'<label class="col-sm-3 control-label" for="description">Description:</label>'+
					'<div class="col-sm-8">'+
						'<textarea id="description" ng-model="endpoint.description" class="form-control"></textarea>'+
					'</div>'+
				'</div>'+
				'<div class="form-group" ng-if="showSaveButton">'+
					'<div class="col-sm-offset-3 col-sm-8">'+
						'<div class="btn-toolbar">'+
							'<button class="btn btn-default" type="submit" ng-disabled="saveDisabled()">Save</button>'+
							'<button class="btn btn-default" ng-if="showCancelButton" ng-click="onCancel()" type="button">Cancel</button>'+
						'</div>'+
					'</div>'+
				'</div>'+
			'</form>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.submit = () ->
				scope.onSubmit?()
]

@directives.directive 'endpointDisplay', ['DataService'
	(DataService)->
		scope:
			endpoint: '='
			showValues: '='
			editable: '='
			canEdit: '='
			onEdit: '='
		template: ''+
			'<div class="row endpoint-row">
				<div class="col-md-1"><strong>{{DataService.labelEndpoint()}}</strong></div>
				<div class="col-md-11">{{endpoint.name}}</div>
			</div>
			<div class="row endpoint-row">
				<div class="col-md-1"><strong>Description</strong></div>
				<div class="col-md-11">{{endpoint.description}}</div>
			</div>
			<div class="parameter-div" parameter-display parameters="endpoint.parameters" show-values="showValues" editable="editable" can-edit="canEdit" on-edit="onEdit">
			</div>'
		link: (scope, element, attrs) =>
			scope.DataService = DataService
]

@directives.directive 'parameterDisplay', ['DataService'
	(@DataService)->
		scope:
			parameters: '='
			showValues: '='
			editable: '='
			canEdit: '='
			onEdit: '='
			parameterLabel: '@?'
		template: ''+
			'<div>
				<table id="parameter-table" class="table table-directive" ng-if="parameters && parameters.length > 0">
					<thead>
						<tr>
							<th width="1%" style="padding-right:30px;">Set?</th>
							<th width="20%">{{parameterLabel}}</th>
							<th ng-if="showValues">Configured value</th>
							<th>Description</th>
							<th style="text-align: center;" width="5%" ng-if="editable">Action</th>
							<th width="1%" ng-if="!editable"></th>
						</tr>
					</thead>
					<tbody>
						<tr ng-repeat="parameter in parameters">
							<td style="text-align: center;padding-right:30px;"><i class="glyphicon" ng-class="{\'glyphicon-ok\': parameter.configured, \'glyphicon-remove\': !parameter.configured}"></i></td>
							<td style="font-weight: bold;" ng-if="parameter.use == \'R\'">*&nbsp;{{parameter.name}}</td>
							<td ng-if="parameter.use != \'R\'">{{parameter.name}}</td>
							<td ng-if="showValues && parameter.kind == \'BINARY\'"><a ng-if="parameter.value" href="" ng-click="downloadBinaryParameter(parameter)">{{parameter.fileName}}</a></td>
							<td ng-if="showValues && parameter.kind != \'BINARY\'">{{parameter.value}}</td>
							<td>{{parameter.desc}}</td>
							<td style="text-align: center;" ng-if="editable && canEdit(parameter)"><button class="btn btn-default" ng-click="onEdit(parameter)"><i class="fa fa-pencil"></i></button></td>
							<td ng-if="!editable || !canEdit(parameter)"></td>
						</tr>
					</tbody>
				</table>
			</div>'
		restrict: 'A'
		link: (scope, element, attrs) =>
			if scope.parameterLabel == undefined
				scope.parameterLabel = 'Parameter'
			scope.downloadBinaryParameter = (parameter) =>
				blob = @DataService.b64toBlob(@DataService.base64FromDataURL(parameter.value), parameter.mimeType)
				saveAs(blob, parameter.fileName)
]