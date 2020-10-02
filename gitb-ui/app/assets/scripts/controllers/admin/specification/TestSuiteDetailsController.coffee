class TestSuiteDetailsController

	@$inject = ['$scope', 'TestSuiteService', 'ConfirmationDialogService', '$state', '$stateParams', 'ErrorService', 'DataService', 'PopupService', 'WebEditorService', 'HtmlService', '$sce', 'ConformanceService']
	constructor: (@$scope, @TestSuiteService, @ConfirmationDialogService, @$state, @$stateParams, @ErrorService, @DataService, @PopupService, @WebEditorService, @HtmlService, @$sce, @ConformanceService) ->
		@testSuite = {}
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@testSuiteId = @$stateParams.testsuite_id
		@testCaseTableColumns = [
			{
				field: 'identifier'
				title: 'ID'
			}
			{
				field: 'sname'
				title: 'Name'
			}
			{
				field: 'description'
				title: 'Description'
			}
		]

		@TestSuiteService.getTestSuiteWithTestCases(@testSuiteId)
		.then (data) =>
			@testSuite = data
			valueToSet = @testSuite.documentation
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
			@HtmlService.showHtml('Test suite documentation', @$sce.trustAsHtml(html))
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	download: () =>
		@TestSuiteService.downloadTestSuite @testSuite.id
		.then (data) =>
			blobData = new Blob([data], {type: 'application/zip'});
			saveAs(blobData, "test_suite.zip");
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	delete: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this test suite?", "Yes", "No")
		.then(() =>
			@TestSuiteService.undeployTestSuite @testSuite.id
			.then () =>
				@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
				@PopupService.success('Test suite deleted.')
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
		, angular.noop)

	saveChanges: () =>
		@TestSuiteService.updateTestSuiteMetadata(@testSuite.id, @testSuite.sname, @testSuite.description, tinymce.activeEditor.getContent())
		.then () =>
			@PopupService.success('Test suite updated.')
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	back: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}

	saveDisabled: () =>
		!(@testSuite?.sname? && @testSuite.sname.trim() != '')

	onTestCaseSelect: (testCase) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.testsuites.detail.testcases.detail.list', {id: @domainId, spec_id: @specificationId, testsuite_id: @testSuite.id, testcase_id: testCase.id}


@controllers.controller 'TestSuiteDetailsController', TestSuiteDetailsController
