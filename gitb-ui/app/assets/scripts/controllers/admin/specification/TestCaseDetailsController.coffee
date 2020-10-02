class TestCaseDetailsController

	@$inject = ['$scope', 'TestSuiteService', 'ConfirmationDialogService', '$state', '$stateParams', 'ErrorService', 'DataService', 'PopupService', 'WebEditorService', 'HtmlService', '$sce', 'ConformanceService']
	constructor: (@$scope, @TestSuiteService, @ConfirmationDialogService, @$state, @$stateParams, @ErrorService, @DataService, @PopupService, @WebEditorService, @HtmlService, @$sce, @ConformanceService) ->
		@testCase = {}
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@testSuiteId = @$stateParams.testsuite_id
		@testCaseId = @$stateParams.testcase_id

		@TestSuiteService.getTestCase(@testCaseId)
		.then (data) =>
			@testCase = data
			valueToSet = @testCase.documentation
			if !valueToSet?
				valueToSet = ''
			tinymce.remove('.mce-editor')
			@WebEditorService.editor(300, valueToSet)
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@DataService.focus('name')

	previewDocumentation: () =>
		@ConformanceService.getDocumentationForPreview(tinymce.activeEditor.getContent())
		.then (html) =>
			@HtmlService.showHtml('Test case documentation', @$sce.trustAsHtml(html))
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	saveChanges: () =>
		@TestSuiteService.updateTestCaseMetadata(@testCase.id, @testCase.sname, @testCase.description, tinymce.activeEditor.getContent())
		.then () =>
			@PopupService.success('Test case updated.')
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	back: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.testsuites.detail.list', {id: @domainId, spec_id: @specificationId, testsuite_id: @testSuiteId}

	saveDisabled: () =>
		!(@testCase?.sname? && @testCase.sname.trim() != '')

@controllers.controller 'TestCaseDetailsController', TestCaseDetailsController
