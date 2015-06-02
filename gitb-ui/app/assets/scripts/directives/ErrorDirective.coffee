@directives.directive 'errorMessage', ['$log',
	(@$log)->
		scope:
			error: '='
		template: ''+
			'<div>'+
				'{{error.data.error_description}}' +
			'</div>'
		restrict: 'A'
		link: (scope, element, attrs) ->
			@$log.debug scope.error
]