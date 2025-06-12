package managers.export

import models.Enums.ImportItemType

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.CollectionHasAsScala

object ImportTargets {

  private def updateForCommunity(community: com.gitb.xml.export.Community, result: ImportTargets): Unit = {
    if (community != null) {
      result.hasCommunity = true
      if (community.getDomain != null) {
        updateForDomain(community.getDomain, result)
      }
      if (community.getAdministrators != null && !community.getAdministrators.getAdministrator.isEmpty) {
        result.hasAdministrators = true
      }
      if (community.getConformanceCertificateSettings != null) {
        result.hasCommunityCertificateSettings = true
      }
      if (community.getCustomLabels != null && !community.getCustomLabels.getLabel.isEmpty) {
        result.hasCustomLabels = true
      }
      if (community.getOrganisationProperties != null && !community.getOrganisationProperties.getProperty.isEmpty) {
        result.hasOrganisationProperties = true
      }
      if (community.getSystemProperties != null && !community.getSystemProperties.getProperty.isEmpty) {
        result.hasSystemProperties = true
      }
      if (community.getLandingPages != null && !community.getLandingPages.getLandingPage.isEmpty) {
        result.hasLandingPages = true
      }
      if (community.getLegalNotices != null && !community.getLegalNotices.getLegalNotice.isEmpty) {
        result.hasLegalNotices = true
      }
      if (community.getErrorTemplates != null && !community.getErrorTemplates.getErrorTemplate.isEmpty) {
        result.hasErrorTemplates = true
      }
      if (community.getTriggers != null && !community.getTriggers.getTrigger.isEmpty) {
        result.hasTriggers = true
      }
      if (community.getResources != null && !community.getResources.getResource.isEmpty) {
        result.hasResources = true
      }
      if (community.getOrganisations != null && !community.getOrganisations.getOrganisation.isEmpty) {
        result.hasOrganisations = true
        community.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
          if (exportedOrganisation.getUsers != null && !exportedOrganisation.getUsers.getUser.isEmpty) {
            result.hasOrganisationUsers = true
          }
          if (exportedOrganisation.getPropertyValues != null && !exportedOrganisation.getPropertyValues.getProperty.isEmpty) {
            result.hasOrganisationPropertyValues = true
          }
          if (exportedOrganisation.getSystems != null && !exportedOrganisation.getSystems.getSystem.isEmpty) {
            result.hasSystems = true
            exportedOrganisation.getSystems.getSystem.asScala.foreach { exportedSystem =>
              if (exportedSystem.getPropertyValues != null && !exportedSystem.getPropertyValues.getProperty.isEmpty) {
                result.hasSystemPropertyValues = true
              }
              if (exportedSystem.getStatements != null && !exportedSystem.getStatements.getStatement.isEmpty) {
                result.hasStatements = true
                exportedSystem.getStatements.getStatement.asScala.foreach { exportedStatement =>
                  if (exportedStatement.getConfigurations != null && !exportedStatement.getConfigurations.getConfiguration.isEmpty) {
                    result.hasStatementConfigurations = true
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def fromImportItems(items: List[ImportItem]): ImportTargets = {
    val result = new ImportTargets()
    items.foreach { item =>
      updateForImportItem(result, item)
    }
    result
  }

  private def updateForImportItemList(targets: ImportTargets, items: ListBuffer[ImportItem]): Unit = {
    if (items != null && items.nonEmpty) {
      items.foreach { item =>
        updateForImportItem(targets, item)
      }
    }
  }

  private def updateForImportItem(targets: ImportTargets, item: ImportItem): Unit = {
    if (item != null) {
      item.itemType match {
        case ImportItemType.Domain => targets.hasDomain = true
        case ImportItemType.DomainParameter => targets.hasDomainParameters = true
        case ImportItemType.Specification => targets.hasSpecifications = true
        case ImportItemType.SpecificationGroup => targets.hasSpecifications = true
        case ImportItemType.TestSuite => targets.hasTestSuites = true
        case ImportItemType.Actor => targets.hasActors = true
        case ImportItemType.Endpoint => targets.hasEndpoints = true
        case ImportItemType.EndpointParameter => targets.hasEndpointParameters = true
        case ImportItemType.Community => targets.hasCommunity = true
        case ImportItemType.Administrator => targets.hasAdministrators = true
        case ImportItemType.CustomLabel => targets.hasCustomLabels = true
        case ImportItemType.OrganisationProperty => targets.hasOrganisationProperties = true
        case ImportItemType.SystemProperty => targets.hasSystemProperties = true
        case ImportItemType.LandingPage => targets.hasLandingPages = true
        case ImportItemType.LegalNotice => targets.hasLegalNotices = true
        case ImportItemType.ErrorTemplate => targets.hasErrorTemplates = true
        case ImportItemType.Trigger => targets.hasTriggers = true
        case ImportItemType.CommunityResource => targets.hasResources = true
        case ImportItemType.Organisation  => targets.hasOrganisations = true
        case ImportItemType.OrganisationUser => targets.hasOrganisationUsers = true
        case ImportItemType.OrganisationPropertyValue => targets.hasOrganisationPropertyValues = true
        case ImportItemType.System => targets.hasSystems = true
        case ImportItemType.SystemPropertyValue => targets.hasSystemPropertyValues = true
        case ImportItemType.Statement => targets.hasStatements = true
        case ImportItemType.StatementConfiguration => targets.hasStatementConfigurations = true
        case ImportItemType.Settings => targets.hasSettings = true
        case ImportItemType.SystemResource => targets.hasSystemResources = true
        case ImportItemType.Theme => targets.hasThemes = true
        case ImportItemType.DefaultLandingPage => targets.hasDefaultLandingPages = true
        case ImportItemType.DefaultLegalNotice => targets.hasDefaultLegalNotices = true
        case ImportItemType.DefaultErrorTemplate => targets.hasDefaultErrorTemplates = true
        case ImportItemType.SystemAdministrator => targets.hasSystemAdministrators = true
        case ImportItemType.SystemConfiguration => targets.hasSystemConfigurations = true
      }
      updateForImportItemList(targets, item.childrenItems)
    }
  }

  def fromCommunity(community: com.gitb.xml.export.Community): ImportTargets = {
    val result = new ImportTargets()
    updateForCommunity(community, result)
    result
  }

  private def updateForDomain(domain: com.gitb.xml.export.Domain, result: ImportTargets): Unit = {
    if (domain != null) {
      result.hasDomain = true
      if (domain.getSharedTestSuites != null && !domain.getSharedTestSuites.getTestSuite.isEmpty) {
        result.hasTestSuites = true
      }
      if (domain.getSpecifications != null && !domain.getSpecifications.getSpecification.isEmpty) {
        result.hasSpecifications = true
        domain.getSpecifications.getSpecification.asScala.foreach { specification =>
          if (specification.getTestSuites != null && !specification.getTestSuites.getTestSuite.isEmpty) {
            result.hasTestSuites = true
          }
          if (specification.getActors != null && !specification.getActors.getActor.isEmpty) {
            result.hasActors = true
            specification.getActors.getActor.asScala.foreach { actor =>
              if (actor.getEndpoints != null) {
                if (!actor.getEndpoints.getEndpoint.isEmpty) {
                  result.hasEndpoints = true
                  actor.getEndpoints.getEndpoint.asScala.foreach { endpoint =>
                    if (endpoint.getParameters != null && !endpoint.getParameters.getParameter.isEmpty) {
                      result.hasEndpointParameters = true
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (domain.getParameters != null && !domain.getParameters.getParameter.isEmpty) {
        result.hasDomainParameters = true
      }
    }
  }

  def fromDomain(domain: com.gitb.xml.export.Domain): ImportTargets = {
    val result = new ImportTargets()
    updateForDomain(domain, result)
    result
  }

  def fromSettings(settings: com.gitb.xml.export.Settings): ImportTargets = {
    val result = new ImportTargets()
    updateForSettings(settings, result)
    result
  }

  private def updateForSettings(settings: com.gitb.xml.export.Settings, result: ImportTargets):Unit = {
    if (settings != null) {
      result.hasSettings = true
      if (settings.getResources != null && !settings.getResources.getResource.isEmpty) {
        result.hasSystemResources = true
      }
      if (settings.getThemes != null && !settings.getThemes.getTheme.isEmpty) {
        result.hasThemes = true
      }
      if (settings.getLandingPages != null && !settings.getLandingPages.getLandingPage.isEmpty) {
        result.hasDefaultLandingPages = true
      }
      if (settings.getLegalNotices != null && !settings.getLegalNotices.getLegalNotice.isEmpty) {
        result.hasDefaultLegalNotices = true
      }
      if (settings.getErrorTemplates != null && !settings.getErrorTemplates.getErrorTemplate.isEmpty) {
        result.hasDefaultErrorTemplates = true
      }
      if (settings.getAdministrators != null && !settings.getAdministrators.getAdministrator.isEmpty) {
        result.hasSystemAdministrators = true
      }
      if (settings.getSystemConfigurations != null && !settings.getSystemConfigurations.getConfig.isEmpty) {
        result.hasSystemConfigurations = true
      }
    }
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
  var hasTriggers: Boolean = false
  var hasResources: Boolean = false
  var hasAdministrators: Boolean = false
  var hasOrganisations: Boolean = false
  var hasOrganisationUsers: Boolean = false
  var hasOrganisationPropertyValues: Boolean = false
  var hasSystems: Boolean = false
  var hasSystemPropertyValues: Boolean = false
  var hasStatements: Boolean = false
  var hasStatementConfigurations: Boolean = false

  var hasSettings: Boolean = false
  var hasSystemResources: Boolean = false
  var hasThemes: Boolean = false
  var hasDefaultLandingPages: Boolean = false
  var hasDefaultLegalNotices: Boolean = false
  var hasDefaultErrorTemplates: Boolean = false
  var hasSystemAdministrators: Boolean = false
  var hasSystemConfigurations: Boolean = false

}
