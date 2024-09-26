package managers.export

class ExportSettings {

  var communityAdministrators: Boolean = false
  var landingPages: Boolean = false
  var legalNotices: Boolean = false
  var errorTemplates: Boolean = false
  var triggers: Boolean = false
  var resources: Boolean = false
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
  var themes: Boolean = false
  var systemAdministrators: Boolean = false
  var defaultLandingPages: Boolean = false
  var defaultLegalNotices: Boolean = false
  var defaultErrorTemplates: Boolean = false
  var systemConfigurations: Boolean = false
  var encryptionKey: Option[String] = None
  var communitiesToDelete: Option[List[String]] = None
  var domainsToDelete: Option[List[String]] = None

  def hasSystemSettings(): Boolean = {
    themes || systemAdministrators || defaultLandingPages || defaultLegalNotices || defaultErrorTemplates || systemConfigurations
  }

  def withoutSystemSettings(): ExportSettings = {
    if (this.themes) {
      val copy = new ExportSettings
      copy.communityAdministrators = this.communityAdministrators
      copy.landingPages = this.landingPages
      copy.legalNotices = this.legalNotices
      copy.errorTemplates = this.errorTemplates
      copy.triggers = this.triggers
      copy.resources = this.resources
      copy.certificateSettings = this.certificateSettings
      copy.customLabels = this.customLabels
      copy.customProperties = this.customProperties
      copy.organisations = this.organisations
      copy.organisationUsers = this.organisationUsers
      copy.organisationPropertyValues = this.organisationPropertyValues
      copy.systems = this.systems
      copy.systemPropertyValues = this.systemPropertyValues
      copy.statements = this.statements
      copy.statementConfigurations = this.statementConfigurations
      copy.domain = this.domain
      copy.domainParameters = this.domainParameters
      copy.specifications = this.specifications
      copy.actors = this.actors
      copy.endpoints = this.endpoints
      copy.testSuites = this.testSuites
      copy.themes = false
      copy.systemAdministrators = false
      copy.defaultLandingPages = false
      copy.defaultLegalNotices = false
      copy.defaultErrorTemplates = false
      copy.systemConfigurations = false
      copy.encryptionKey = this.encryptionKey
      copy.communitiesToDelete = this.communitiesToDelete
      copy.domainsToDelete = this.domainsToDelete
      copy
    } else {
      this
    }
  }
}
