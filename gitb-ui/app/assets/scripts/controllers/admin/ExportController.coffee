class ExportController

    @$inject = ['CommunityService', 'ConformanceService', 'ConfirmationDialogService', 'ErrorService', 'DataService', 'PopupService']
    constructor: (@CommunityService, @ConformanceService, @ConfirmationDialogService, @ErrorService, @DataService, @PopupService) ->
        @resetSettings()
        @pending = false
        if @DataService.isCommunityAdmin
            @community = @DataService.community
            @domain = @DataService.community.domain
        else if @DataService.isSystemAdmin
            # Get communities
            @CommunityService.getCommunities()
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

    resetSettings: () =>
        @settings = {
            landingPages: false,
            errorTemplates: false,
            legalNotices: false,
            certificateSettings: false,
            customLabels: false,
            customProperties: false,
            organisations: false,
            organisationPropertyValues: false,
            systems: false,
            systemPropertyValues: false,
            statements: false,
            statementConfigurations: false,
            domain: false,
            domainParameters: false,
            specifications: false,
            actors: false,
            endpoints: false,
            testSuites: false,
            encryptionKey: undefined
        }
        if @DataService.isSystemAdmin
            @domain = undefined
            @community = undefined
        if @exportType == 'domain'
            delay = 200
            if @exportType == 'community'
                delay = 0
            focusOn = 'domain'
            if @DataService.isCommunityAdmin
                focusOn = 'encryptionPassword'
            @DataService.focus(focusOn, delay)
        else if @exportType == 'community'
            delay = 200
            if @exportType == 'domain'
                delay = 0
            focusOn = 'community'
            if @DataService.isCommunityAdmin
                focusOn = 'encryptionPassword'
            @DataService.focus(focusOn, delay)

    allCommunityDataChanged: () =>
        if @allCommunityData
            @settings.landingPages = @allCommunityData
            @settings.legalNotices = @allCommunityData
            @settings.errorTemplates = @allCommunityData
            @settings.certificateSettings = @allCommunityData
            @settings.customLabels = @allCommunityData
            @settings.customProperties = @allCommunityData

    allOrganisationDataChanged: () =>
        if @allOrganisationData
            @settings.organisations = @allOrganisationData
            @settings.organisationPropertyValues = @allOrganisationData
            @settings.systems = @allOrganisationData
            @settings.systemPropertyValues = @allOrganisationData
            @settings.statements = @allOrganisationData
            @settings.statementConfigurations = @allOrganisationData
            # Prerequisites
            @statementConfigurationsChanged()
            @systemPropertyValuesChanged()

    allDomainDataChanged: () =>
        if @allDomainData
            @settings.domain = @allDomainData
            @settings.specifications = @allDomainData
            @settings.actors = @allDomainData
            @settings.endpoints = @allDomainData
            @settings.testSuites = @allDomainData
            @settings.domainParameters = @allDomainData

    organisationPropertyValuesChanged: () =>
        if @settings.organisationPropertyValues
            @settings.customProperties = true

    systemPropertyValuesChanged: () =>
        if @settings.systemPropertyValues
            @settings.customProperties = true

    statementConfigurationsChanged: () =>
        if @settings.statementConfigurations
            @settings.statements = true
            @statementsChanged()
            @settings.endpoints = true
            @endpointsChanged()

    statementsChanged: () =>
        if @settings.statements
            @settings.actors = true
            @actorsChanged()
            @settings.systems = true
            @systemsChanged()

    specificationsChanged: () =>
        if @settings.specifications
            @settings.domain = true

    testSuitesChanged: () =>
        if @settings.testSuites
            @settings.specifications = true
            @specificationsChanged()

    actorsChanged: () =>
        if @settings.actors
            @settings.specifications = true
            @specificationsChanged()

    endpointsChanged: () =>
        if @settings.endpoints
            @settings.actors = true
            @actorsChanged()

    domainParametersChanged: () =>
        if @settings.domainParameters
            @settings.domain = true

    systemsChanged: () =>
        if @settings.systems
            @settings.organisations = true

    isPrerequisiteDomain: () =>
        @settings.domainParameters || @settings.specifications || @isPrerequisiteSpecifications()

    isPrerequisiteSpecifications: () =>
        @settings.testSuites || @settings.actors || @isPrerequisiteActors()

    isPrerequisiteActors: () =>
        @settings.endpoints || @isPrerequisiteEndpoints() || @settings.statements || @isPrerequisiteStatements()

    isPrerequisiteEndpoints: () =>
        @settings.statementConfigurations

    isPrerequisiteOrganisations: () =>
        @settings.organisationPropertyValues || @settings.systems || @isPrerequisiteSystems()

    isPrerequisiteSystems: () =>
        @settings.systemPropertyValues || @settings.statements || @isPrerequisiteStatements()

    isPrerequisiteStatements: () =>
        @settings.statementConfigurations

    isPrerequisiteCustomProperties: () =>
        @settings.organisationPropertyValues || @settings.systemPropertyValues

    exportDisabled: () =>
        @pending || !((@exportType == 'domain' && @domain? || @exportType == 'community' && @community?) && @settings.encryptionKey? && @settings.encryptionKey.trim().length > 0)

    export: () =>
        @pending = true
        if @exportType == 'domain'
            exportId = @domain.id
            exportMethod = @ConformanceService.exportDomain
        else
            exportId = @community.id
            exportMethod = @CommunityService.exportCommunity
        exportMethod(exportId, JSON.stringify(@settings))
        .then (data) =>
            blobData = new Blob([data], {type: 'application/zip'});
            saveAs(blobData, "export.zip");
            @pending = false
        .catch (error) =>
            @ErrorService.showErrorMessage(error)
            @pending = false

@controllers.controller 'ExportController', ExportController
