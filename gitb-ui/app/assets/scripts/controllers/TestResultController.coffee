class TestResultController

	@$inject = ['$log', '$scope', '$state', '$stateParams', 'Constants', 'ReportService', 'ErrorService']
	constructor: (@$log, @$scope, @$state, @$stateParams, @Constants, @ReportService, @ErrorService) ->
		@$log.debug "Constructing TestResultController..."

		@sessionId = @$stateParams["session_id"]
		@testResultFlat = {}

		@getTestResultOfSession(@sessionId)

	getTestResultOfSession:(sessionId) ->
		@ReportService.getTestStepResults(sessionId)
		.then (stepResults) =>
			@stepResults = stepResults
			@flattenTestStepResults(@stepResults)
			@ReportService.getTestResultOfSession(sessionId)
		.then (result) =>
			@testResult   = result
			@testcase     = angular.fromJson(@testResult.tpl)
			@$scope.steps = @testcase.steps
			@traverseTestCaseSteps(@$scope.steps)
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	flattenTestStepResults: (results) ->
		for result in results
			@testResultFlat[result.stepId] = result

	visibleStepContents: (step) ->
		visible = false
		if Array.isArray(step)
			visible = @visibleSequenceContents(step)
		else
			if step.type == "decision"
				visible = @visibleSequenceContents(step.then) || @visibleSequenceContents(step.else)
			else if step.type == "loop"
				visible = @visibleSequenceContents(step.sequences)
			else
				visible = true
		visible

	visibleSequenceContents: (steps) ->
		if steps? && steps.length > 0
			for step in steps
				if @visibleStepContents(step)
					return true
		return false

	traverseTestCaseSteps: (steps) ->
		if steps?
			for step in steps
				if @testResultFlat[step.id]?
					step.status =  @testResultFlat[step.id].result
					step.report = {}
					step.report.path = @testResultFlat[step.id].path

					if step.type == "decision"
						if @testResultFlat[step.id+"[T]"]?
							@testResultFlat[step.id].condition = true
							step.condition = true
						else if @testResultFlat[step.id+"[F]"]?
							@testResultFlat[step.id].condition = false
							step.condition = false
						@traverseTestCaseSteps(step.then)
						@traverseTestCaseSteps(step.else)
					else if step.type == "flow"
						for thread in step.threads
							@traverseTestCaseSteps(thread)
					else if step.type == "loop"
						@traverseLoops(step)
						for sequence in step.sequences
							@traverseTestCaseSteps(sequence)
					step.hide = !@visibleStepContents(step)

	traverseLoops : (loopStep) ->
		loopStep.sequences = []
		i = 1

		while @testResultFlat[loopStep.id + "[" + i + "]"]?
			sequence = angular.toJson(loopStep.steps)
			if i > 1
				sequence = sequence.split(loopStep.id+"[1]").join(loopStep.id + "[" + i + "]")
			sequence = angular.fromJson(sequence)
			if sequence.length > 0
				loopStep.sequences.push(sequence)
			i = i+1
		loopStep.currentIndex = loopStep.sequences.length - 1

controllers.controller('TestResultController', TestResultController)