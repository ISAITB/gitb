@directives.directive 'errorMessage', ['$log',
	(@$log)->
		scope:
			error: '='
		template: ''+
			'<div ng-bind-html="error.messageToShow">'+
			'</div>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			# @$log.debug scope.error
]

@directives.directive 'errorTemplateEditor', ['Constants',
	(@Constants)->
		scope:
			error: '='
		template: ''+
			'<div>'+
				'<div class="pull-left">'+
					'<textarea class="mce-editor"></textarea>'+
				'</div>'+
				'<div style="padding-left: 20px; padding-right:20px; display: table;">'+
					'<h4>Placeholders:</h4>'+
					'<p><b>{{errorDescriptionLabel}}</b>: The error message text (a text value - may be empty).</p>'+
					'<p><b>{{errorIdLabel}}</b>: The error identifier (used to trace error in logs).</p>'+
				'</div>'+
			'</div>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			scope.errorDescriptionLabel = @Constants.PLACEHOLDER__ERROR_DESCRIPTION
			scope.errorIdLabel = @Constants.PLACEHOLDER__ERROR_ID
]
