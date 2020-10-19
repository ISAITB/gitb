class AdminConformanceController

	@$inject = ['$log', '$scope', '$state', 'DataService', 'ConformanceService', 'ErrorService', 'Constants', '$q', 'CommunityService', 'OrganizationService', 'SystemService', 'ReportService', 'ConfirmationDialogService', '$uibModal']
	constructor: (@$log, @$scope, @$state, @DataService, @ConformanceService, @ErrorService, @Constants, @$q, @CommunityService, @OrganizationService, @SystemService, @ReportService, @ConfirmationDialogService, @$uibModal) ->
		@$log.debug "Constructing AdminConformanceController..."
		@showFilters = false
		@exportPending = false
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
			actor :
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
		d4 = @getAllActors()
		d6 = @getAllOrganizations()
		d7 = @getAllSystems()

		@$q.all([d1, d2, d3, d4, d6, d7])
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

	resetFilters: (keepTick) ->
		@setDomainFilter()
		@setSpecificationFilter(@filters.domain.filter, [], keepTick)
		@setActorFilter(@filters.specification.filter, [], keepTick)
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

	setActorFilter: (selection1, selection2, keepTick) ->
		selection = if selection1? and selection1.length > 0 then selection1 else selection2
		copy = _.map(@filters.actor.filter, _.clone)
		@filters.actor.filter = _.map((_.filter @filters.actor.all, (a) => (_.contains (_.map selection, (s) => s.id), a.specification)), _.clone)
		@keepTickedProperty(copy, @filters.actor.filter) if keepTick

		for i in [@filters.actor.selection.length - 1..0] by -1
			some = @filters.actor.selection[i]
			found = _.find @filters.actor.filter, (s) => `s.id == some.id`
			if (!found?)
				@filters.actor.selection.splice(i, 1)

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
		if !@DataService.isSystemAdmin
			communityIds = [@DataService.community.id]
		@CommunityService.getCommunities(communityIds)
		.then (data) =>
				@filters.community.all = data
				d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllDomains: () ->
		d = @$q.defer()
		if !@DataService.isSystemAdmin && @DataService.community.domainId?
			domainIds = [@DataService.community.domainId]
		@ConformanceService.getDomains(domainIds)
		.then (data) =>
			@filters.domain.all = data
			d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllSpecifications: () ->
		d = @$q.defer()
		if @DataService.isCommunityAdmin && @DataService.community.domainId?
			callResult = @ConformanceService.getSpecifications(@DataService.community.domainId)
		else
			callResult = @ConformanceService.getSpecificationsWithIds()
		callResult.then (data) =>
				@filters.specification.all = data
				d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllActors: () ->
		d = @$q.defer()
		if @DataService.isCommunityAdmin && @DataService.community.domainId?
			callResult = @ConformanceService.getActorsForDomain(@DataService.community.domainId)
		else
			callResult = @ConformanceService.getActorsWithIds()
		callResult.then (data) =>
				@filters.actor.all = data
				d.resolve()
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getAllOrganizations: () ->
		d = @$q.defer()
		if @DataService.isCommunityAdmin
			@OrganizationService.getOrganizationsByCommunity(@DataService.community.id)
			.then (data) =>
				@filters.organization.all = data
				d.resolve()
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
		else
			@OrganizationService.getOrganizations()
			.then (data) =>
				@filters.organization.all = data
				d.resolve()
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
		d.promise

	getAllSystems: () ->
		d = @$q.defer()
		if @DataService.isSystemAdmin
			sFunction = @SystemService.getSystems
		else
			sFunction = @SystemService.getSystemsByCommunity
		sFunction().then (data) =>
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
		@filters.actor.selection = []
		@filters.community.selection = []
		@filters.organization.selection = []
		@filters.system.selection = []
		@resetFilters(false)
		@getConformanceStatements()

	domainClicked: (domain) =>
		@setSpecificationFilter(@filters.domain.selection, @filters.domain.filter, true)
		@getConformanceStatements()

	specificationClicked: (spec) =>
		@setActorFilter(@filters.specification.selection, @filters.specification.filter, true)
		@getConformanceStatements()

	actorClicked: (spec) =>
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


	getConformanceStatementsInternal: (fullResults, forExport) =>
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
		actorIds = _.map @filters.actor.selection, (s) -> s.id
		organizationIds = _.map @filters.organization.selection, (s) -> s.id
		systemIds = _.map @filters.system.selection, (s) -> s.id

		@ConformanceService.getConformanceOverview(domainIds, specIds, actorIds, communityIds, organizationIds, systemIds, fullResults, forExport)
		.then (data) =>
			for conformanceStatement in data.data
				completedCount = Number(conformanceStatement.completed)
				failedCount = Number(conformanceStatement.failed)
				undefinedCount = Number(conformanceStatement.undefined)
				conformanceStatement.status = @DataService.testStatusText(completedCount, failedCount, undefinedCount)
				conformanceStatement.overallStatus = @DataService.conformanceStatusForTests(completedCount, failedCount, undefinedCount)
			d.resolve(data)
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		d.promise

	getConformanceStatements: () =>
		searchPromise = @getConformanceStatementsInternal(false, false).then((data) => 
			@conformanceStatements = data.data
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
