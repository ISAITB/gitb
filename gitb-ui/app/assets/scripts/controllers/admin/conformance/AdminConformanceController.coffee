class AdminConformanceController

	@$inject = ['$log', '$scope', '$state', 'DataService', 'ConformanceService', 'ErrorService', 'Constants', '$q', 'CommunityService', 'OrganizationService', 'SystemService', 'ReportService', 'ConfirmationDialogService']
	constructor: (@$log, @$scope, @$state, @DataService, @ConformanceService, @ErrorService, @Constants, @$q, @CommunityService, @OrganizationService, @SystemService, @ReportService, @ConfirmationDialogService) ->
		@$log.debug "Constructing AdminConformanceController..."
		@showFilters = false
		@filters =
			community :
				all : []
				filter : []
				selection : []
			domain :
				all : []
				filter : []
				selection : []
			specification :
				all : []
				filter : []
				selection : []
			organization :
				all : []
				filter : []
				selection : []
			system :
				all : []
				filter : []
				selection : []
		@translation =
			selectAll       : ""
			selectNone      : ""
			reset           : ""
			search          : "Search..."
			nothingSelected : "All"
		d1 = @getAllCommunities()
		d2 = @getAllDomains()
		d3 = @getAllSpecifications()
		d6 = @getAllOrganizations()
		d7 = @getAllSystems()

		@$q.all([d1, d2, d3, d6, d7])
		.then () =>
			@resetFilters(false)
			@getConformanceStatements()
		
		tempColumns = []
		if (!@DataService.isCommunityAdmin)
			tempColumns.push {
				field: 'communityName',
				title: 'Community'
			}
		tempColumns.push {
			field: 'organizationName',
			title: 'Organization'
		}
		tempColumns.push {
			field: 'systemName',
			title: 'System'
		}
		if (!@DataService.community.domainId?)
			tempColumns.push {
				field: 'domainName',
				title: 'Domain'
			}
		tempColumns.push {
			field: 'specName',
			title: 'Specification'
		}
		tempColumns.push {
			field: 'actorName',
			title: 'Actor'
		}
		tempColumns.push {
			field: 'status',
			title: 'Status'
		}
		@tableColumns = tempColumns
		@expandedStatements = {}
		@expandedStatements.count = 0

	resetFilters: (keepTick) ->
		@setDomainFilter()
		@setSpecificationFilter(@filters.domain.filter, [], keepTick)
		@setCommunityFilter()
		@setOrganizationFilter(@filters.community.filter, [], keepTick)
		@setSystemFilter(@filters.organization.filter, [], keepTick)

	setDomainFilter: () ->
		if @DataService.isCommunityAdmin and @DataService.community.domainId?
			id = @DataService.community.domainId
			@filters.domain.filter = _.map(_.filter(@filters.domain.all, (d) => `d.id == id`), _.clone)
			@filters.domain.filter[0].ticked = true
			@filters.domain.selection = _.map(@filters.domain.filter, _.clone)
		else
			@filters.domain.filter = _.map(@filters.domain.all, _.clone)

	setSpecificationFilter: (selection1, selection2, keepTick) ->
		selection = if selection1? and selection1.length > 0 then selection1 else selection2
		copy = _.map(@filters.specification.filter, _.clone)
		@filters.specification.filter = _.map((_.filter @filters.specification.all, (s) => (_.contains (_.map selection, (d) => d.id), s.domain)), _.clone)
		@keepTickedProperty(copy, @filters.specification.filter) if keepTick

		for i in [@filters.specification.selection.length - 1..0] by -1
			some = @filters.specification.selection[i]
			found = _.find @filters.specification.filter, (s) => `s.id == some.id`
			if (!found?)
				@filters.specification.selection.splice(i, 1)

	setCommunityFilter: () ->
		if @DataService.isCommunityAdmin
			id = @DataService.community.id
			@filters.community.filter = _.map(_.filter(@filters.community.all, (c) => `c.id == id`), _.clone)
			@filters.community.filter[0].ticked = true
			@filters.community.selection = _.map(@filters.community.filter, _.clone)
		else
			@filters.community.filter = _.map(@filters.community.all, _.clone)

	setOrganizationFilter: (selection1, selection2, keepTick) ->
		selection = if selection1? and selection1.length > 0 then selection1 else selection2
		copy = _.map(@filters.organization.filter, _.clone)
		@filters.organization.filter = _.map((_.filter @filters.organization.all, (o) => (_.contains (_.map selection, (s) => s.id), o.community)), _.clone)
		@keepTickedProperty(copy, @filters.organization.filter) if keepTick

		for i in [@filters.organization.selection.length - 1..0] by -1
			some = @filters.organization.selection[i]
			found = _.find @filters.organization.filter, (s) => `s.id == some.id`
			if (!found?)
				@filters.organization.selection.splice(i, 1)

	setSystemFilter: (selection1, selection2, keepTick) ->
		selection = if selection1? and selection1.length > 0 then selection1 else selection2
		copy = _.map(@filters.system.filter, _.clone)
		@filters.system.filter = _.map((_.filter @filters.system.all, (o) => (_.contains (_.map selection, (s) => s.id), o.owner)), _.clone)
		@keepTickedProperty(copy, @filters.system.filter) if keepTick

		for i in [@filters.system.selection.length - 1..0] by -1
			some = @filters.system.selection[i]
			found = _.find @filters.system.filter, (s) => `s.id == some.id`
			if (!found?)
				@filters.system.selection.splice(i, 1)

	keepTickedProperty: (oldArr, newArr) ->
		if oldArr? and oldArr.length > 0
			for o, i in newArr
				n = _.find oldArr, (s) => `s.id == o.id`
				o.ticked = if n?.ticked? then n.ticked else false

	getAllCommunities: () ->
		d = @$q.defer()
		@CommunityService.getCommunities()
		.then (data) =>
				@filters.community.all = data
				d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllDomains: () ->
		d = @$q.defer()
		@ConformanceService.getDomains()
		.then (data) =>
			@filters.domain.all = data
			d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllSpecifications: () ->
		d = @$q.defer()
		@ConformanceService.getSpecificationsWithIds()
		.then (data) =>
				@filters.specification.all = data
				d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllOrganizations: () ->
		d = @$q.defer()
		@OrganizationService.getOrganizations()
		.then (data) =>
			@filters.organization.all = data
			d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllSystems: () ->
		d = @$q.defer()
		@SystemService.getSystems()
		.then (data) =>
			@filters.system.all = data
			d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	showFilter: () =>
		@showFilters = true

	clearFilter: () =>
		@showFilters = false
		@filters.domain.selection = []
		@filters.specification.selection = []
		@filters.community.selection = []
		@filters.organization.selection = []
		@filters.system.selection = []
		@resetFilters(false)
		@getConformanceStatements()

	domainClicked: (domain) =>
		@setSpecificationFilter(@filters.domain.selection, @filters.domain.filter, true)
		@getConformanceStatements()

	specificationClicked: (spec) =>
		@getConformanceStatements()

	communityClicked: (community) =>
		@setOrganizationFilter(@filters.community.selection, @filters.community.filter, true)
		@setSystemFilter(@filters.organization.selection, @filters.organization.filter, true)
		@getConformanceStatements()

	organizationClicked: (organization) =>
		@setSystemFilter(@filters.organization.selection, @filters.organization.filter, true)
		@getConformanceStatements()

	systemClicked: (system) =>
		@getConformanceStatements()

	isDomainDisabled: () =>
		@DataService.isCommunityAdmin


	getConformanceStatementsInternal: (fullResults) =>
		d = @$q.defer()
		if (@DataService.isCommunityAdmin)
			communityIds = [@DataService.community.id]
		else
			communityIds = _.map @filters.community.selection, (s) -> s.id
		if (@DataService.isCommunityAdmin && @DataService.community.domainId?)
			domainIds = [@DataService.community.domainId]
		else
			domainIds = _.map @filters.domain.selection, (s) -> s.id
		specIds = _.map @filters.specification.selection, (s) -> s.id
		organizationIds = _.map @filters.organization.selection, (s) -> s.id
		systemIds = _.map @filters.system.selection, (s) -> s.id

		@ConformanceService.getConformanceOverview(domainIds, specIds, communityIds, organizationIds, systemIds, fullResults)
		.then (data) =>
			for statement in data
				statement.status = "#{statement.completed}/#{statement.total} PASSED"
			d.resolve(data)
		d.promise

	getConformanceStatements: () =>
		searchPromise = @getConformanceStatementsInternal(false).then((data) => 
			@conformanceStatements = data
			@onCollapseAll()
		)

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
		@getConformanceStatementsInternal(true).then((data) =>
			if @DataService.isCommunityAdmin
				if @DataService.community.domain?
					headers = ["Organization", "System", "Specification", "Actor", "Test suite", "Test case", "Result"]
					columnMap = ["organizationName", "systemName", "specName", "actorName", "testSuiteName", "testCaseName", "result"]
				else
					headers = ["Organization", "System", "Domain", "Specification", "Actor", "Test suite", "Test case", "Result"]
					columnMap = ["organizationName", "systemName", "domainName", "specName", "actorName", "testSuiteName", "testCaseName", "result"]
			else
				headers = ["Community", "Organization", "System", "Domain", "Specification", "Actor", "Test suite", "Test case", "Result"]
				columnMap = ["communityName", "organizationName", "systemName", "domainName", "specName", "actorName", "testSuiteName", "testCaseName", "result"]
			@exportAsCsv(headers, columnMap, data)
		)

	exportAsCsv: (header, columnMap, data) ->
		if data.length > 0
			csv = header.toString() + "\n"
			for rowData, rowIndex in data
				line = ""
				for columnName, columnIndex in columnMap
					if columnIndex != 0
						line += ","
					if rowData[columnName]?
						line += rowData[columnName].replace /,/, " "
				csv += if rowIndex < data.length then line + "\n" else line
			blobData = new Blob([csv], {type: 'text/csv'});
			saveAs(blobData, "export.csv");

	onExportConformanceStatement: (statement) =>
		choice = @ConfirmationDialogService.confirm("Report options", "Would you like to include the detailed test step results per test session?", "Yes, include step results", "No, summary only", true)
		choice.then(() => 
			@ReportService.exportConformanceStatementReport(statement.actorId, statement.systemId, true)
			.then (data) =>
					blobData = new Blob([data], {type: 'application/pdf'});
					saveAs(blobData, "conformance_report.pdf");
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
		, () => 
			@ReportService.exportConformanceStatementReport(statement.actorId, statement.systemId, false)
			.then (data) =>
					blobData = new Blob([data], {type: 'application/pdf'});
					saveAs(blobData, "conformance_report.pdf");
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
		)

	onExportTestCase: (statement, testCase) =>
		@ReportService.exportTestCaseReport(testCase.sessionId, testCase.id)
		.then (stepResults) =>
				blobData = new Blob([stepResults], {type: 'application/pdf'});
				saveAs(blobData, "test_case_report.pdf");

@controllers.controller 'AdminConformanceController', AdminConformanceController