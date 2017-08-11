@directives.directive 'wizardDirective', [
	()->
		scope:
			steps: '='
			onBefore: '='
			onNext: '='
			onFinish: '='
			triggerNext: '='
		restrict: 'A'
		transclude: true
		template: ''+
			'<div class="wizard-directive">'+
				'<div class="steps">'+
					'<div class="steps-row">'+
						'<div class="step" '+
							'ng-repeat="step in steps" '+
							'ng-class="{\'active\': isActive(step), \'disabled\': isDisabled(step), \'completed\': isCompleted(step), \'next\': isNext(step)}">'+
							'<a href="" type="button" class="step-button" ng-click="next(step)">{{step.id}}</a>'+
							'<p class="step-title">{{step.title}}</p>'+
						'</div>'+
					'</div>'+
				'</div>'+
				'<div class="step-contents" ng-transclude>'+
				'</div>'+
			'</div>'
		link: (scope, element, attrs) ->
			scope.current = _.head scope.steps

			scope.isActive = (step)->
				scope.current.id == step.id

			scope.isDisabled = (step)->
				activeIndex = findActiveIndex()
				index = _.findIndex scope.steps, (s)->
					s.id == step.id

				activeIndex < index

			scope.isCompleted = (step) ->
				activeIndex = findActiveIndex()
				index = _.findIndex scope.steps, (s)->
					s.id == step.id

				activeIndex > index

			scope.isNext = (step)->
				activeIndex = findActiveIndex()
				index = _.findIndex scope.steps, (s)->
					s.id == step.id

				activeIndex + 1 == index

			scope.$on 'wizard-directive:next', ()=>
				scope.next()

			scope.$on 'wizard-directive:start', ()=>
				showStepContent (findActiveIndex())

			scope.next = (step)->
				activeIndex = findActiveIndex()
				if step?
					index = _.findIndex scope.steps, (s)->
						s.id == step.id
				else
					index = -1

				if index == -1 || activeIndex + 1 == index
					stepCompleted = scope.onNext scope.current

					if stepCompleted
						if activeIndex < scope.steps.length - 1
							scope.current = scope.steps[activeIndex+1]
							showStepContent (activeIndex+1)
						else
							scope.onFinish()

			findActiveIndex = ()->
				activeIndex = _.findIndex scope.steps, (s)->
					s.id == scope.current.id

				activeIndex

			showStepContent = (index)->
				stepContents = element.find '.step-contents .step-content'
				oldActiveContent = element.find '.step-contents .step-content.active'
				oldActiveContent.removeClass 'active'
				showStep = scope.onBefore scope.current

				if showStep
					newActiveContent = angular.element (stepContents[index])
					newActiveContent.addClass 'active'
				else
					scope.triggerNext()

				return

]
