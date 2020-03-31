class ImportController

    @$inject = ['CommunityService', 'ConformanceService', 'ConfirmationDialogService', 'ErrorService', 'DataService', 'PopupService']
    constructor: (@CommunityService, @ConformanceService, @ConfirmationDialogService, @ErrorService, @DataService, @PopupService) ->
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
            testSuites: false,
            encryptionKey: undefined
        }
        if @DataService.isCommunityAdmin
            @community = @DataService.community
            # @domainId = @DataService.community.domain
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
            @settings.domain = true
            @settings.customProperties = true

    allDomainDataChanged: () =>
        if @allDomainData
            @settings.domain = @allDomainData
            @settings.testSuites = @allDomainData

    organisationPropertyValuesChanged: () =>
        if @settings.organisationPropertyValues
            @settings.customProperties = true

    systemPropertyValuesChanged: () =>
        if @settings.systemPropertyValues
            @settings.customProperties = true

    testSuitesChanged: () =>
        if @settings.testSuites
            @settings.domain = true

    statementConfigurationsChanged: () =>
        if @settings.statementConfigurations
            @settings.statements = true
            @statementsChanged()

    statementsChanged: () =>
        if @settings.statements
            @settings.domain = true
            @settings.systems = true
            @systemsChanged()

    systemsChanged: () =>
        if @settings.systems
            @settings.organisations = true

    isPrerequisiteDomain: () =>
        @settings.statements || @settings.statementConfigurations || @settings.testSuites

    isPrerequisiteOrganisations: () =>
        @settings.organisationPropertyValues || @settings.systems || @isPrerequisiteSystems()

    isPrerequisiteSystems: () =>
        @settings.systemPropertyValues || @settings.statements || @isPrerequisiteStatements()

    isPrerequisiteStatements: () =>
        @settings.statementConfigurations

    isPrerequisiteCustomProperties: () =>
        @settings.organisationPropertyValues || @settings.systemPropertyValues

    export: () =>
        @CommunityService.exportCommunity(@community.id, JSON.stringify(@settings))
        .then (data) =>
            blobData = new Blob([data], {type: 'application/zip'});
            saveAs(blobData, "export.zip");
        .catch (error) =>
            @ErrorService.showErrorMessage(error)

@controllers.controller 'ImportController', ImportController
