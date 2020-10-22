class AdminConformanceController

	@$inject = ['$log', '$scope', '$state', 'DataService', 'ConformanceService', 'ErrorService', 'Constants', '$q', 'CommunityService', 'OrganizationService', 'SystemService', 'ReportService', 'ConfirmationDialogService', '$uibModal']
	constructor: (@$log, @$scope, @$state, @DataService, @ConformanceService, @ErrorService, @Constants, @$q, @CommunityService, @OrganizationService, @SystemService, @ReportService, @ConfirmationDialogService, @$uibModal) ->
		@$log.debug "Constructing AdminConformanceController..."
		@exportPending = false
		@dataStatus = {status: @Constants.STATUS.PENDING}
		@filterState = {
			updatePending: false
		}
		@filters = [@Constants.FILTER_TYPE.SPECIFICATION, @Constants.FILTER_TYPE.ACTOR, @Constants.FILTER_TYPE.ORGANISATION, @Constants.FILTER_TYPE.SYSTEM]
		if @DataService.isSystemAdmin
			@columnCount = 9
			@filters.push(@Constants.FILTER_TYPE.DOMAIN)
			@filters.push(@Constants.FILTER_TYPE.COMMUNITY)
		else if @DataService.isCommunityAdmin
			if @DataService.community.domain == null
				@columnCount = 8
				@filters.push(@Constants.FILTER_TYPE.DOMAIN)
			else
				@columnCount = 7

		tempColumns = []
		if (!@DataService.isCommunityAdmin)
			tempColumns.push {
				field: 'communityName',
				title: 'Community'
			}
		tempColumns.push {
			field: 'organizationName',
			title: @DataService.labelOrganisation()
		}
		tempColumns.push {
			field: 'systemName',
			title: @DataService.labelSystem()
		}
		if (!@DataService.community.domainId?)
			tempColumns.push {
				field: 'domainName',
				title: @DataService.labelDomain()
			}
		tempColumns.push {
			field: 'specName',
			title: @DataService.labelSpecification()
		}
		tempColumns.push {
			field: 'actorName',
			title: @DataService.labelActor()
		}
		tempColumns.push {
			field: 'status',
			title: 'Status'
		}
		@tableColumns = tempColumns
		@expandedStatements = {}
		@expandedStatements.count = 0
		@settingsLoaded = @$q.defer()

	getAllCommunities: () =>
		@CommunityService.getCommunities()

	getAllDomains: () =>
		@ConformanceService.getDomains()

	getAllSpecifications: () =>
		if @DataService.isCommunityAdmin && @DataService.community.domainId?
			callResult = @ConformanceService.getSpecifications(@DataService.community.domainId)
		else
			callResult = @ConformanceService.getSpecificationsWithIds()
		callResult

	getAllActors: () =>
		if @DataService.isCommunityAdmin && @DataService.community.domainId?
			callResult = @ConformanceService.getActorsForDomain(@DataService.community.domainId)
		else
			callResult = @ConformanceService.getActorsWithIds()
		callResult

	getAllOrganizations: () =>
		if @DataService.isCommunityAdmin
			callResult = @OrganizationService.getOrganizationsByCommunity(@DataService.community.id)
		else
			callResult = @OrganizationService.getOrganizations()
		callResult

	getAllSystems: () =>
		if @DataService.isSystemAdmin
			callResult = @SystemService.getSystems()
		else
			callResult = @SystemService.getSystemsByCommunity()
		callResult

	getCurrentSearchCriteria:() =>
		filters = @filterState.currentFilters()
		searchCriteria = {}
		if @DataService.isCommunityAdmin
			searchCriteria.communityIds = [@DataService.community.id]
			if @DataService.community.domain?
				searchCriteria.domainIds = [@DataService.community.domain.id]
			else
				searchCriteria.domainIds = filters[@Constants.FILTER_TYPE.DOMAIN]
		else
			searchCriteria.communityIds = filters[@Constants.FILTER_TYPE.COMMUNITY]
			searchCriteria.domainIds = filters[@Constants.FILTER_TYPE.DOMAIN]
		searchCriteria.specIds = filters[@Constants.FILTER_TYPE.SPECIFICATION]
		searchCriteria.actorIds = filters[@Constants.FILTER_TYPE.ACTOR]
		searchCriteria.organizationIds = filters[@Constants.FILTER_TYPE.ORGANISATION]
		searchCriteria.systemIds = filters[@Constants.FILTER_TYPE.SYSTEM]
		searchCriteria

	getConformanceStatementsInternal: (fullResults, forExport) =>
		d = @$q.defer()
		params = @getCurrentSearchCriteria()
		@ConformanceService.getConformanceOverview(params.domainIds, params.specIds, params.actorIds, params.communityIds, params.organizationIds, params.systemIds, fullResults, forExport)
		.then (data) =>
			for conformanceStatement in data.data
				completedCount = Number(conformanceStatement.completed)
				failedCount = Number(conformanceStatement.failed)
				undefinedCount = Number(conformanceStatement.undefined)
				conformanceStatement.status = @DataService.testStatusText(completedCount, failedCount, undefinedCount)
				conformanceStatement.overallStatus = @DataService.conformanceStatusForTests(completedCount, failedCount, undefinedCount)
			@dataStatus = {status: @Constants.STATUS.FINISHED}
			d.resolve(data)
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
			@dataStatus = {status: @Constants.STATUS.FINISHED}
		d.promise

	getConformanceStatements: () =>
		@filterState.updatePending = true
		@getConformanceStatementsInternal(false, false)
		.then (data) => 
			@conformanceStatements = data.data
			@filterState.updatePending = false
			@onCollapseAll()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
			@filterState.updatePending = false

	onExpand: (statement) =>
		if (@isExpanded(statement))
			@collapse(statement)
		else
			if (statement.testCases?)
				@expand(statement)
			else
				@ConformanceService.getConformanceStatus(statement.actorId, statement.systemId)
				.then (data) =>
					testCases = []
					for result in data
						testCase = {}
						testCase.id = result.testCaseId
						testCase.sessionId = result.sessionId
						testCase.testSuiteName = result.testSuiteName
						testCase.testCaseName = result.testCaseName
						testCase.result = result.result
						testCases.push(testCase)
					statement.testCases = testCases
					@expand(statement)
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	collapse: (statement) =>
		delete @expandedStatements[statement.systemId+"_"+statement.actorId]
		@expandedStatements.count -= 1

	expand: (statement) =>
		@expandedStatements[statement.systemId+"_"+statement.actorId] = true
		@expandedStatements.count += 1

	isExpanded: (statement) =>
		@expandedStatements[statement.systemId+"_"+statement.actorId]?

	showCollapseAll: () =>
		@expandedStatements.count > 0

	onCollapseAll: () =>
		@expandedStatements = {}
		@expandedStatements.count = 0

	showExportTestCase: (testCase) =>
		testCase.sessionId? && testCase.sessionId != ""

	onExportConformanceStatementsAsCsv: () =>
		@exportPending = true
		@getConformanceStatementsInternal(true, true)
		.then (data) =>
			headers = []
			columnMap = []
			if !@DataService.isCommunityAdmin
				headers.push("Community")
				columnMap.push("communityName")
			headers.push(@DataService.labelOrganisation())
			columnMap.push("organizationName")
			if data.orgParameters?
				for param in data.orgParameters
					headers.push(@DataService.labelOrganisation() + " ("+param+")")
					columnMap.push("orgparam_"+param)
			headers.push(@DataService.labelSystem())
			columnMap.push("systemName")
			if data.sysParameters?
				for param in data.sysParameters
					headers.push(@DataService.labelSystem() + " ("+param+")")
					columnMap.push("sysparam_"+param)
			if !@DataService.isCommunityAdmin || @DataService.isCommunityAdmin && !@DataService.community.domain?
				headers.push(@DataService.labelDomain())
				columnMap.push("domainName")
			headers = headers.concat([@DataService.labelSpecification(), @DataService.labelActor(), "Test suite", "Test case", "Result"])
			columnMap = columnMap.concat(["specName", "actorName", "testSuiteName", "testCaseName", "result"])
			@DataService.exportPropertiesAsCsv(headers, columnMap, data.data)
			@exportPending = false
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
			@exportPending = false		

	onExportTestCase: (statement, testCase) =>
		testCase.exportPending = true
		@ReportService.exportTestCaseReport(testCase.sessionId, testCase.id)
		.then (stepResults) =>
			blobData = new Blob([stepResults], {type: 'application/pdf'});
			saveAs(blobData, "test_case_report.pdf");
			testCase.exportPending = false
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
			testCase.exportPending = false

	onExportConformanceStatement: (statement) =>
		@statementToProcess = statement
		if !statement?
			@statementToProcess = @conformanceStatements[0]
		@statementToProcess.exportPending = true
		if !@settings?
			@ConformanceService.getConformanceCertificateSettings(@statementToProcess.communityId, false)
			.then (settings) => 
				if settings? && settings?.id?
					@settings = settings
				else
					@settings = {}
				@settingsLoaded.resolve()
			.catch (error) =>
				@statementToProcess.exportPending = false
				@ErrorService.showErrorMessage(error)
		@settingsLoaded.promise.then () =>
			@statementToProcess.exportPending = false
			modalOptions =
				templateUrl: 'assets/views/admin/conformance/generate-certificate-modal.html'
				controller: 'ConformanceCertificateModalController as controller'
				size: 'lg'
				resolve: 
					settings: () => JSON.parse(JSON.stringify(@settings))
					conformanceStatement: () => @statementToProcess
			@$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)

@controllers.controller 'AdminConformanceController', AdminConformanceController
