package managers.export

object ImportTargets {

  import scala.collection.JavaConversions._

  private def updateForCommunity(community: com.gitb.xml.export.Community, result: ImportTargets) = {
    result.hasCommunity = true
    if (community.getDomain != null) {
      updateForDomain(community.getDomain, result)
    }
    if (community.getAdministrators != null && community.getAdministrators.getAdministrator.nonEmpty) {
      result.hasAdministrators = true
    }
    if (community.getConformanceCertificateSettings != null) {
      result.hasCommunityCertificateSettings = true
    }
    if (community.getCustomLabels != null && community.getCustomLabels.getLabel.nonEmpty) {
      result.hasCustomLabels = true
    }
    if (community.getOrganisationProperties != null && community.getOrganisationProperties.getProperty.nonEmpty) {
      result.hasOrganisationProperties = true
    }
    if (community.getSystemProperties != null && community.getSystemProperties.getProperty.nonEmpty) {
      result.hasSystemProperties = true
    }
    if (community.getLandingPages != null && community.getLandingPages.getLandingPage.nonEmpty) {
      result.hasLandingPages = true
    }
    if (community.getLegalNotices != null && community.getLegalNotices.getLegalNotice.nonEmpty) {
      result.hasLegalNotices = true
    }
    if (community.getErrorTemplates != null && community.getErrorTemplates.getErrorTemplate.nonEmpty) {
      result.hasErrorTemplates = true
    }
    if (community.getOrganisations != null && community.getOrganisations.getOrganisation.nonEmpty) {
      result.hasOrganisations = true
      community.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
        if (exportedOrganisation.getUsers != null && exportedOrganisation.getUsers.getUser.nonEmpty) {
          result.hasOrganisationUsers = true
        }
        if (exportedOrganisation.getPropertyValues != null && exportedOrganisation.getPropertyValues.getProperty.nonEmpty) {
          result.hasOrganisationPropertyValues = true
        }
        if (exportedOrganisation.getSystems != null && exportedOrganisation.getSystems.getSystem.nonEmpty) {
          result.hasSystems = true
          exportedOrganisation.getSystems.getSystem.foreach { exportedSystem =>
            if (exportedSystem.getPropertyValues != null && exportedSystem.getPropertyValues.getProperty.nonEmpty) {
              result.hasSystemPropertyValues = true
            }
            if (exportedSystem.getStatements != null && exportedSystem.getStatements.getStatement.nonEmpty) {
              result.hasStatements = true
              exportedSystem.getStatements.getStatement.foreach { exportedStatement =>
                if (exportedStatement.getConfigurations != null && exportedStatement.getConfigurations.getConfiguration.nonEmpty) {
                  result.hasStatementConfigurations = true
                }
              }
            }
          }
        }
      }
    }
  }

  def fromCommunity(community: com.gitb.xml.export.Community): ImportTargets = {
    val result = new ImportTargets()
    updateForCommunity(community, result)
    result
  }

  private def updateForDomain(domain: com.gitb.xml.export.Domain, result: ImportTargets) = {
    result.hasDomain = true
    if (domain.getSpecifications != null && domain.getSpecifications.getSpecification.nonEmpty) {
      result.hasSpecifications = true
      domain.getSpecifications.getSpecification.foreach { specification =>
        if (specification.getTestSuites != null && specification.getTestSuites.getTestSuite.nonEmpty) {
          result.hasTestSuites = true
        }
        if (specification.getActors != null && specification.getActors.getActor.nonEmpty) {
          result.hasActors = true
          specification.getActors.getActor.foreach { actor =>
            if (actor.getEndpoints != null) {
              if (actor.getEndpoints.getEndpoint.nonEmpty) {
                result.hasEndpoints = true
                actor.getEndpoints.getEndpoint.foreach { endpoint =>
                  if (endpoint.getParameters != null && endpoint.getParameters.getParameter.nonEmpty) {
                    result.hasEndpointParameters = true
                  }
                }
              }
            }
          }
        }
      }
    }
    if (domain.getParameters != null && domain.getParameters.getParameter.nonEmpty) {
      result.hasDomainParameters = true
    }
  }

  def fromDomain(domain: com.gitb.xml.export.Domain): ImportTargets = {
    val result = new ImportTargets()
    updateForDomain(domain, result)
    result
  }

}

class ImportTargets {
  var hasDomain: Boolean = false
  var hasSpecifications: Boolean = false
  var hasTestSuites: Boolean = false
  var hasActors: Boolean = false
  var hasEndpoints: Boolean = false
  var hasEndpointParameters: Boolean = false
  var hasDomainParameters: Boolean = false

  var hasCommunity: Boolean = false
  var hasCommunityCertificateSettings: Boolean = false
  var hasCustomLabels: Boolean = false
  var hasOrganisationProperties: Boolean = false
  var hasSystemProperties: Boolean = false
  var hasLandingPages: Boolean = false
  var hasLegalNotices: Boolean = false
  var hasErrorTemplates: Boolean = false
  var hasAdministrators: Boolean = false
  var hasOrganisations: Boolean = false
  var hasOrganisationUsers: Boolean = false
  var hasOrganisationPropertyValues: Boolean = false
  var hasSystems: Boolean = false
  var hasSystemPropertyValues: Boolean = false
  var hasStatements: Boolean = false
  var hasStatementConfigurations: Boolean = false

}
