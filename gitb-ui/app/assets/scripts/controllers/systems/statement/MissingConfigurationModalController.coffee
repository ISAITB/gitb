class MissingConfigurationModalController

    @$inject = ['DataService', '$uibModalInstance', 'organisationProperties', 'organisationConfigurationValid', 'systemProperties', 'systemConfigurationValid', 'endpointRepresentations', 'configurationValid', 'missingOrganisationConfigurationVisible', 'missingSystemConfigurationVisible', 'missingStatementConfigurationVisible']
    constructor:(@DataService, @$uibModalInstance, @organisationProperties, @organisationConfigurationValid, @systemProperties, @systemConfigurationValid, @endpointRepresentations, @configurationValid, @missingOrganisationConfigurationVisible, @missingSystemConfigurationVisible, @missingStatementConfigurationVisible) ->
        @isAdmin = @DataService.isCommunityAdmin || @DataService.isSystemAdmin

    toOrganisationProperties: () =>
        @$uibModalInstance.close({action: 'organisation'})

    toSystemProperties: () =>
        @$uibModalInstance.close({action: 'system'})

    toConfigurationProperties: () =>
        @$uibModalInstance.close({action: 'statement'})

    close: () =>
        @$uibModalInstance.dismiss()

    controllers.controller('MissingConfigurationModalController', MissingConfigurationModalController)