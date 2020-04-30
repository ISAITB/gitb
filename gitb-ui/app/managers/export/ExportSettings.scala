package managers.export

class ExportSettings {

  var communityAdministrators: Boolean = false
  var landingPages: Boolean = false
  var legalNotices: Boolean = false
  var errorTemplates: Boolean = false
  var certificateSettings: Boolean = false
  var customLabels: Boolean = false
  var customProperties: Boolean = false
  var organisations: Boolean = false
  var organisationUsers: Boolean = false
  var organisationPropertyValues: Boolean = false
  var systems: Boolean = false
  var systemPropertyValues: Boolean = false
  var statements: Boolean = false
  var statementConfigurations: Boolean = false
  var domain: Boolean = false
  var domainParameters: Boolean = false
  var specifications: Boolean = false
  var actors: Boolean = false
  var endpoints: Boolean = false
  var testSuites: Boolean = true
  var encryptionKey: Option[String] = None

}
