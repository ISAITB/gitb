class ImportController

    @$inject = ['$scope', 'CommunityService', 'ConformanceService', 'ErrorService', 'DataService', 'PopupService', 'Constants', '$state']
    constructor: (@$scope, @CommunityService, @ConformanceService, @ErrorService, @DataService, @PopupService, @Constants, @$state) ->
        @resetSettings()
        @showDomainOption = true
        @pending = false
        if @DataService.isCommunityAdmin
            @community = @DataService.community
            if @DataService.community.domain?
                @domain = @DataService.community.domain
            else
                @exportType = 'community'
                @showDomainOption = false
        else if @DataService.isSystemAdmin
            # Get communities
            @CommunityService.getCommunities([], true)
            .then (data) =>
                @communities = data
            .catch (error) =>
                @ErrorService.showErrorMessage(error)
            # Get domains
            @ConformanceService.getDomains()
            .then (data) =>
                @domains = data
            .catch (error) =>
                @ErrorService.showErrorMessage(error)

        @$scope.$on '$destroy', () =>
            if @pendingImportId?
                @cancel()

    resetSettings: () =>
        @importStep1 = true
        @importStep2 = false
        # Clear data
        @archiveData = {}
        @pendingImportId = undefined
        @importItems = undefined
        @settings = {}
        @settings.encryptionKey = undefined
        @settings.createNewData = true
        @settings.deleteUnmatchedData = true
        @settings.updateMatchingData = true
        if @DataService.isSystemAdmin
            @domain = undefined
            @community = undefined
        if @exportType == 'domain'
            delay = 200
            if @exportType == 'community'
                delay = 0
            if @DataService.isSystemAdmin
                @DataService.focus('domain', delay)
        else if @exportType == 'community'
            delay = 200
            if @exportType == 'domain'
                delay = 0
            if @DataService.isSystemAdmin
                @DataService.focus('community', delay)

    uploadArchive: (files) =>
        file = _.head files
        if file?
            reader = new FileReader()
            reader.onload = (event) =>
                @archiveData = {
                    name: file.name
                    size: file.size
                    type: file.type
                    data: event.target.result
                }
                @$scope.$apply()
            reader.readAsDataURL file

    setupImportItemLabels: () =>
        @importItemActionLabels = {}
        @importItemActionLabels[@Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY] = 'New'
        @importItemActionLabels[@Constants.IMPORT_ITEM_MATCH.BOTH] = 'Update'
        @importItemActionLabels[@Constants.IMPORT_ITEM_MATCH.DB_ONLY] = 'Delete'
        @importItemTypeLabels = {}
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.DOMAIN] = @DataService.labelDomains()
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.DOMAIN_PARAMETER] = @DataService.labelDomain() + ' parameters'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.SPECIFICATION] = @DataService.labelSpecifications()
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ACTOR] = @DataService.labelActors()
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ENDPOINT] = @DataService.labelEndpoints()
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ENDPOINT_PARAMETER] = @DataService.labelEndpoint() + ' parameters'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.TEST_SUITE] = 'Test suites'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.COMMUNITY] = 'Communities'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ADMINISTRATOR] = 'Administrators'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.CUSTOM_LABEL] = 'Custom labels'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ORGANISATION_PROPERTY] = 'Custom ' + @DataService.labelOrganisationLower() + ' properties'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.SYSTEM_PROPERTY] = 'Custom ' + @DataService.labelSystemLower() + ' properties'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.LANDING_PAGE] = 'Landing pages'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.LEGAL_NOTICE] = 'Legal notices'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ERROR_TEMPLATE] = 'Error templates'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.TRIGGER] = 'Triggers'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ORGANISATION] = @DataService.labelOrganisations()
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ORGANISATION_USER] = 'Users'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.ORGANISATION_PROPERTY_VALUE] = 'Custom ' + @DataService.labelOrganisationLower() + ' property values'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.SYSTEM] = @DataService.labelSystems()
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.SYSTEM_PROPERTY_VALUE] = 'Custom ' + @DataService.labelSystemLower() + ' property values'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.STATEMENT] = 'Conformance statements'
        @importItemTypeLabels[@Constants.IMPORT_ITEM_TYPE.STATEMENT_CONFIGURATION] = 'Conformance statement configurations'

    typeDescription: (type) =>
        @importItemTypeLabels[type]

    defaultAction: (matchType) =>
      if matchType == @Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY
        if @settings.createNewData
            @Constants.IMPORT_ITEM_CHOICE.PROCEED
        else
            @Constants.IMPORT_ITEM_CHOICE.SKIP
      else if matchType == @Constants.IMPORT_ITEM_MATCH.BOTH
        if @settings.updateMatchingData
            @Constants.IMPORT_ITEM_CHOICE.PROCEED
        else
            @Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN
      else 
        if @settings.deleteUnmatchedData
            @Constants.IMPORT_ITEM_CHOICE.PROCEED
        else
            @Constants.IMPORT_ITEM_CHOICE.SKIP

    importDisabled: () =>
        @pending || (@importStep1 && !(@archiveData?.data? && @settings.encryptionKey? && @settings.encryptionKey.trim().length > 0 && (@exportType == 'domain' && @domain? || @exportType == 'community' && @community?)))

    import: () =>
        @pending = true
        if @exportType == 'domain'
            importId = @domain?.id
            importMethod = @ConformanceService.uploadDomainExport
        else 
            importId = @community?.id
            importMethod = @CommunityService.uploadCommunityExport
        importMethod(importId, JSON.stringify(@settings), @archiveData.data)
        .then (data) =>
            @importStep1 = false
            @importStep2 = true
            @setupImportItemLabels()
            @pendingImportId = data.pendingImportId
            @importItems = data.importItems
            @archiveData = {}
            @pending = false
        .catch (error) =>
            @ErrorService.showErrorMessage(error)
            @pending = false

    cancel: () =>
        if @exportType == 'domain'
            importId = @domain?.id
            importMethod = @ConformanceService.cancelDomainImport
        else 
            importId = @community?.id
            importMethod = @CommunityService.cancelCommunityImport
        importMethod(importId, @pendingImportId)
        .then (data) =>
            @$state.go @$state.current, {}, {reload: true}
        .catch (error) =>
            @ErrorService.showErrorMessage(error)

    cleanImportItems: () =>
        cleanItems = []
        for item in @importItems
            cleanItems.push(@cleanImportItem(item))
        cleanItems
    
    cleanImportItem: (item) =>
        cleanItem = {}
        cleanItem.name = item.name
        cleanItem.type = item.type
        cleanItem.match = item.match
        cleanItem.target = item.target
        cleanItem.source = item.source
        cleanItem.process = item.process
        if item.children?
            cleanItem.children = []
            for childItem in item.children
                cleanItem.children.push(@cleanImportItem(childItem))
        cleanItem

    confirm: () =>
        @pending = true
        if @exportType == 'domain'
            importId = @domain?.id
            importMethod = @ConformanceService.confirmDomainImport
        else 
            importId = @community?.id
            importMethod = @CommunityService.confirmCommunityImport
        importMethod(importId, @pendingImportId, JSON.stringify(@settings), JSON.stringify(@cleanImportItems()))
        .then (data) =>
            @$state.go @$state.current, {}, {reload: true}
            @PopupService.success("Import successful.")
            @pending = false
        .catch (error) =>
            @ErrorService.showErrorMessage(error)
            @$state.go @$state.current, {}, {reload: true}
            @pending = false

@controllers.controller 'ImportController', ImportController
