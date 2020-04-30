class ExportController

    @$inject = ['CommunityService', 'ConformanceService', 'ErrorService', 'DataService']
    constructor: (@CommunityService, @ConformanceService, @ErrorService, @DataService) ->
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
            @domain = @DataService.community.domain
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

    resetIncludes: () =>
        @allCommunityData = false
        @allDomainData = false
        @allOrganisationData = false
        @settings.landingPages = false
        @settings.errorTemplates = false
        @settings.legalNotices = false
        @settings.certificateSettings = false
        @settings.customLabels = false
        @settings.customProperties = false
        @settings.organisations = false
        @settings.organisationPropertyValues = false
        @settings.systems = false
        @settings.systemPropertyValues = false
        @settings.statements = false
        @settings.statementConfigurations = false
        @settings.domain = false
        @settings.domainParameters = false
        @settings.specifications = false
        @settings.actors = false
        @settings.endpoints = false
        @settings.testSuites = false
        @settings.communityAdministrators = false
        @settings.organisationUsers = false

    resetSettings: () =>
        @settings = {}
        @resetIncludes()
        @settings.encryptionKey = undefined
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
            @settings.communityAdministrators = !@DataService.configuration['sso.enabled'] && @allCommunityData
            @settings.landingPages = @allCommunityData
            @settings.legalNotices = @allCommunityData
            @settings.errorTemplates = @allCommunityData
            @settings.certificateSettings = @allCommunityData
            @settings.customLabels = @allCommunityData
            @settings.customProperties = @allCommunityData

    allOrganisationDataChanged: () =>
        if @allOrganisationData
            @settings.organisationUsers = !@DataService.configuration['sso.enabled'] && @allOrganisationData
            @settings.organisations = @allOrganisationData
            @settings.organisationPropertyValues = @allOrganisationData
            @settings.systems = @allOrganisationData
            @settings.systemPropertyValues = @allOrganisationData
            if @showDomainOption
                @settings.statements = @allOrganisationData
                @settings.statementConfigurations = @allOrganisationData
                # Prerequisites
                @statementConfigurationsChanged()
            @systemPropertyValuesChanged()

    allDomainDataChanged: () =>
        if @allDomainData && @showDomainOption
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
            @settings.actors = true
            @actorsChanged()
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

    organisationUsersChanged: () =>
        if @settings.organisationUsers
            @settings.organisations = true

    systemsChanged: () =>
        if @settings.systems
            @settings.organisations = true

    isPrerequisiteDomain: () =>
        @settings.domainParameters || @settings.specifications || @isPrerequisiteSpecifications()

    isPrerequisiteSpecifications: () =>
        @settings.testSuites || @settings.actors || @isPrerequisiteActors()

    isPrerequisiteActors: () =>
        @settings.endpoints || @isPrerequisiteEndpoints() || @settings.statements || @isPrerequisiteStatements() || @settings.testSuites

    isPrerequisiteEndpoints: () =>
        @settings.statementConfigurations

    isPrerequisiteOrganisations: () =>
        @settings.organisationPropertyValues || @settings.systems || @isPrerequisiteSystems() || @settings.organisationUsers

    isPrerequisiteSystems: () =>
        @settings.systemPropertyValues || @settings.statements || @isPrerequisiteStatements()

    isPrerequisiteStatements: () =>
        @settings.statementConfigurations

    isPrerequisiteCustomProperties: () =>
        @settings.organisationPropertyValues || @settings.systemPropertyValues

    clearIncludes: () =>
        @resetIncludes()

    allIncludes: () =>
        @allOrganisationData = true
        @allCommunityData = true
        @allCommunityDataChanged()
        @allOrganisationDataChanged()
        if @showDomainOption
            @allDomainData = true
            @allDomainDataChanged()

    disableAllIncludes: () =>
        if @exportType == 'community'
            @allCommunityData && @allOrganisationData && (!@showDomainOption || @allDomainData)
        else
            @allDomainData

    disableClearIncludes: () =>
        !(@allCommunityData ||
        @allDomainData ||
        @allOrganisationData ||
        @settings.landingPages ||
        @settings.errorTemplates ||
        @settings.legalNotices ||
        @settings.certificateSettings ||
        @settings.customLabels ||
        @settings.customProperties ||
        @settings.organisations ||
        @settings.organisationPropertyValues ||
        @settings.systems ||
        @settings.systemPropertyValues ||
        @settings.statements ||
        @settings.statementConfigurations ||
        @settings.domain ||
        @settings.domainParameters ||
        @settings.specifications ||
        @settings.actors ||
        @settings.endpoints ||
        @settings.testSuites ||
        @settings.communityAdministrators ||
        @settings.organisationUsers)

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
