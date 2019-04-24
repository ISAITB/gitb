class TestPresentationController

	@$inject = ['$log', '$scope', '$window', '$uibModal','$state', '$stateParams', 'Constants', 'ReportService', 'ErrorService']
	constructor: (@$log, @$scope, @$window, @$uibModal, @$state, @$stateParams, @Constants, @ReportService, @ErrorService) ->
		@$log.debug "Constructing TestPresentationController..."

		@selectedIndices = {}
		@sessionId = @$stateParams.session_id
		@iconClassForStatus(0)

	iconClassForStatus:(status) ->
		switch status
			when @Constants.TEST_STATUS.PROCESSING then "fa fa-cog fa-spin fa-fw fa-lg"
			when @Constants.TEST_STATUS.WAITING then "fa fa-clock-o fa-fw fa-lg"
			when @Constants.TEST_STATUS.ERROR then "fa fa-times fa-fw fa-lg"
			when @Constants.TEST_STATUS.COMPLETED then "fa fa-check fa-fw fa-lg"
			else "hidden"

	tooltipForStatus:(status) ->
		switch status
			when @Constants.TEST_STATUS.PROCESSING then "Processing..."
			when @Constants.TEST_STATUS.WAITING then "Waiting..."
			when @Constants.TEST_STATUS.ERROR then "Failed"
			when @Constants.TEST_STATUS.COMPLETED then "Success"
			when @Constants.TEST_STATUS.SKIPPED then "Skipped"
			else "Unknown"

	divClassForStatus:(status) =>
		switch status
			when @Constants.TEST_STATUS.PROCESSING then "row execution alert-info"
			when @Constants.TEST_STATUS.ERROR then "row execution alert-danger"
			when @Constants.TEST_STATUS.COMPLETED then "row execution alert-success"
			else "row execution alert-empty"

	labelClassForStatus:(status) ->
		switch status
			when @Constants.TEST_STATUS.PROCESSING then "label label-info font11"
			when @Constants.TEST_STATUS.ERROR then "label label-danger font11"
			when @Constants.TEST_STATUS.COMPLETED then "label label-success font11"
			else "label label-dark font11"

	showStepReport: (step) =>
		showTestStepReportModal = (report) =>
			modalOptions =
				templateUrl: 'assets/views/components/test-step-report-modal.html'
				controller: 'TestStepReportModalController as testStepReportModalCtrl'
				resolve:
					step: () => step
					report: () => report
					sessionId: () => @sessionId
				size: 'lg'

			@$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)

		if step.report?
			if step.report.path?
				@ReportService.getTestStepReport(@sessionId, step.report.path)
				.then (report) =>
					showTestStepReportModal report
				.catch (error) =>
					@ErrorService.showErrorMessage(error)
			else
				showTestStepReportModal step.report

	getSelection :(step, index) ->
		if @selectedIndices[step.id]?
			@selectedIndices[step.id] == index
		else if step.sequences?
			step.sequences.length-1 == index
		else if step.threads?
			step.threads.length-1 == index

	select: (stepId, index) ->
		@selectedIndices[stepId] = index

	back: () ->
		@$window.history.back();

controllers.controller('TestPresentationController', TestPresentationController)