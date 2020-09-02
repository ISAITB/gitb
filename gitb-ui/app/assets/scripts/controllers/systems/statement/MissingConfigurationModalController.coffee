class MissingConfigurationModalController

    @$inject = ['DataService', '$uibModalInstance', 'organisationProperties', 'organisationConfigurationValid', 'systemProperties', 'systemConfigurationValid', 'endpointRepresentations', 'configurationValid', 'organisationPropertyVisibility', 'systemPropertyVisibility', 'statementPropertyVisibility']
    constructor:(@DataService, @$uibModalInstance, @organisationProperties, @organisationConfigurationValid, @systemProperties, @systemConfigurationValid, @endpointRepresentations, @configurationValid, @organisationPropertyVisibility, @systemPropertyVisibility, @statementPropertyVisibility) ->
        @showOrganisationProperties = @organisationPropertyVisibility.hasVisibleMissingRequiredProperties || @organisationPropertyVisibility.hasVisibleMissingOptionalProperties
        @showSystemProperties = @systemPropertyVisibility.hasVisibleMissingRequiredProperties || @systemPropertyVisibility.hasVisibleMissingOptionalProperties
        @showStatementProperties = @statementPropertyVisibility.hasVisibleMissingRequiredProperties || @statementPropertyVisibility.hasVisibleMissingOptionalProperties
        @somethingIsVisible = @showOrganisationProperties || @showSystemProperties || @showStatementProperties
        @requiredPropertiesAreHidden = @organisationPropertyVisibility.hasNonVisibleMissingRequiredProperties || @systemPropertyVisibility.hasNonVisibleMissingRequiredProperties || @statementPropertyVisibility.hasNonVisibleMissingRequiredProperties

    toOrganisationProperties: () =>
        @$uibModalInstance.close({action: 'organisation'})

    toSystemProperties: () =>
        @$uibModalInstance.close({action: 'system'})

    toConfigurationProperties: () =>
        @$uibModalInstance.close({action: 'statement'})

    close: () =>
        @$uibModalInstance.dismiss()

    controllers.controller('MissingConfigurationModalController', MissingConfigurationModalController)