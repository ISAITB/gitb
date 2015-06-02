@directives.directive 'editorCodemirror', ['$timeout',
	($timeout) =>
		scope:
			options: '='
			indicators: '=' 	# a list of indicators to be shown
									# in the editor
									# available types are: info, warning, error
									# example format:
									# [{location: {line: 20, column: 11}, type: "error", description: "'test' variable is not defined"}]
			lineNumber: '=' 	# indicator index to navigate when the editor
											# is initialized
		template: ''
		restrict: 'A'
		link: (scope, element, attrs) =>
			cm = CodeMirror (_.head element.get()), scope.options
			doc = cm.getDoc()

			jumpToPosition = (line, ch) =>
				scroll = () =>
					pos =
						line: line
						ch: ch
					coordinates = cm.charCoords pos, 'local'
					top = coordinates.top

					middleHeight = cm.getScrollerElement().offsetHeight / 2

					cm.scrollTo null, top - middleHeight - 5

				$timeout scroll, 300

			if scope.indicators?
				for indicator, index in scope.indicators
					indicatorIcon = ''
					indicatorClass = ''
					switch indicator.type
						when "info"
							indicatorIcon = '<i class="fa fa-info-circle"></i>'
							indicatorClass = 'info-indicator-editor-widget'
						when "warning"
							indicatorIcon = '<i class="fa fa-warning"></i>'
							indicatorClass = 'warning-indicator-editor-widget'
						when "error"
							indicatorIcon = '<i class="fa fa-times-circle"></i>'
							indicatorClass = 'error-indicator-editor-widget'

					message = angular.element '' +
						'<div class="indicator-editor-widget '+ indicatorClass+'">' +
							'<span class="indicator-icon">' +
								indicatorIcon +
							'</span>'+
							'<span class="indicator-desc">' +
								indicator.description +
							'</span>' +
						'</div>'
					
					widgetOptions = 
						coverGutter: false
						noHScroll: true
						above: true
					doc.addLineClass indicator.location.line-1, 'background', 'indicator-widget-line'
					cm.addLineWidget indicator.location.line-1, message[0], widgetOptions

				if scope.lineNumber?

					jumpToPosition scope.lineNumber, 0



			refresh = () =>
				cm.refresh()
			$timeout refresh, 100
]
