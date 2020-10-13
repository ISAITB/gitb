@directives.directive 'endpointDetailsForm', [
	()->
		scope:
			endpoint: '='
		template: ''+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="name">* Name:</label>'+
				'<div class="col-xs-7"><input id="name" ng-model="endpoint.name" class="form-control" type="text"></div>'+
				'<div tb-tooltip="A name used to refer to this set of configuration properties. This is displayed when viewing a conformance statement\'s details."></div>'+
			'</div>'+
			'<div class="form-group">'+
				'<label class="col-xs-3 control-label" for="description">Description:</label>'+
				'<div class="col-xs-7">'+
					'<textarea id="description" ng-model="endpoint.description" class="form-control"></textarea>'+
				'</div>'+
				'<div tb-tooltip="A descriptive text for the purpose of these configuration properties. This is displayed when viewing a conformance statement\'s details."></div'+
			'</div>'
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
			hideEndpointInfo: '='
		template: '
			<div ng-if="!hideEndpointInfo">
				<div class="row endpoint-row">
					<div class="col-md-1"><strong>{{DataService.labelEndpoint()}}</strong></div>
					<div class="col-md-11">{{endpoint.name}}</div>
				</div>
				<div class="row endpoint-row" ng-if="endpoint.description && endpoint.description.trim() != \'\'">
					<div class="col-md-1"><strong>Description</strong></div>
					<div class="col-md-11">{{endpoint.description}}</div>
				</div>
			</div>
			<div ng-class="{\'parameter-div\': !hideEndpointInfo}" parameter-display parameters="endpoint.parameters" only-missing="false" show-values="showValues" editable="editable" can-edit="canEdit" on-edit="onEdit">
			</div>
			'
		link: (scope, element, attrs) =>
			scope.DataService = DataService
]

@directives.directive 'parameterDisplay', ['DataService'
	(@DataService)->
		scope:
			parameters: '='
			showValues: '='
			onlyMissing: '='
			editable: '='
			canEdit: '='
			onEdit: '='
			parameterLabel: '@?'
		template: ''+
			'<div>
				<table class="table table-directive" ng-class="{\'with-actions\': showActions}" ng-if="parameters && parameters.length > 0">
					<thead>
						<tr>
							<th width="1%" style="padding-right:30px;" ng-if="!onlyMissing">Set?</th>
							<th width="20%">{{parameterLabel}}</th>
							<th ng-if="showValues">Configured value</th>
							<th>Description</th>
							<th style="text-align: center;" width="5%" ng-if="showActions">Action</th>
						</tr>
					</thead>
					<tbody>
						<tr ng-repeat="parameter in parameters | filter: showParameter">
							<td style="text-align: center;padding-right:30px;" ng-if="!onlyMissing"><i class="glyphicon" ng-class="{\'glyphicon-ok\': parameter.configured, \'glyphicon-remove\': !parameter.configured}"></i></td>
							<td style="font-weight: bold;" ng-if="isRequired(parameter)">*&nbsp;{{parameter.name}}</td>
							<td ng-if="!isRequired(parameter)">{{parameter.name}}</td>
							<td ng-if="showValues && parameter.kind == \'BINARY\'"><a ng-if="parameter.value" href="" ng-click="downloadBinaryParameter(parameter)">{{parameter.fileName}}</a></td>
							<td ng-if="showValues && parameter.kind != \'BINARY\'"><span ng-if="parameter.valueToShow != undefined">{{parameter.valueToShow}}</span><span ng-if="parameter.valueToShow == undefined">{{parameter.value}}</span></td>
							<td>{{parameter.desc}}</td>
							<td class="operations" style="text-align: center;" ng-if="showActions"><button type="button" class="btn btn-default" ng-if="canEdit(parameter)" ng-click="onEdit(parameter)"><i class="fa fa-pencil"></i></button></td>
						</tr>
					</tbody>
				</table>
			</div>'
		restrict: 'A'
		link: (scope, element, attrs) =>
			scope.isAdmin = @DataService.isSystemAdmin || @DataService.isCommunityAdmin
			if scope.onlyMissing == undefined
				scope.onlyMissing = false
			if scope.parameterLabel == undefined
				scope.parameterLabel = 'Parameter'
			scope.showActions = false
			if scope.editable && scope.parameters?
				for param in scope.parameters
					if scope.canEdit(param)
						scope.showActions = true
			scope.isRequired = (parameter) =>
				parameter.use == 'R'
			scope.showParameter = (parameter) =>
				(!scope.onlyMissing || !parameter.configured) && (scope.isAdmin || !parameter.hidden) && (!parameter.prerequisiteOk? || parameter.prerequisiteOk)
			scope.downloadBinaryParameter = (parameter) =>
				blob = @DataService.b64toBlob(@DataService.base64FromDataURL(parameter.value), parameter.mimeType)
				saveAs(blob, parameter.fileName)
]