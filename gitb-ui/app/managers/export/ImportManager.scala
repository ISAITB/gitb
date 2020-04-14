package managers.export

import java.io.File
import java.nio.file.{Files, Paths}

import com.gitb.xml.export.{CustomLabelType, OrganisationRoleType, PropertyType, SelfRegistrationMethod, SelfRegistrationRestriction, TestSuite}
import config.Configurations
import javax.inject.{Inject, Singleton}
import managers._
import models.Enums.ImportItemType.ImportItemType
import models.Enums.LabelType.LabelType
import models.Enums.{ImportItemChoice, ImportItemMatch, ImportItemType, LabelType, OrganizationType, SelfRegistrationType}
import models.{CommunityLabels, Constants, Enums, OrganisationParameters, Organizations, SystemParameters}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{ClamAVClient, MimeUtil, RepositoryUtils}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ImportManager @Inject() (exportManager: ExportManager, communityManager: CommunityManager, conformanceManager: ConformanceManager, specificationManager: SpecificationManager, actorManager: ActorManager, endpointManager: EndPointManager, parameterManager: ParameterManager, testSuiteManager: TestSuiteManager, landingPageManager: LandingPageManager, legalNoticeManager: LegalNoticeManager, errorTemplateManager: ErrorTemplateManager, organisationManager: OrganizationManager, systemManager: SystemManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  import scala.collection.JavaConversions._

  def getTempFolder(): File = {
    new File("/tmp")
  }

  def getPendingFolder(): File = {
    new File(getTempFolder(), "pending")
  }

  private def listBufferToNameMap[A](input: ListBuffer[A], nameFn: A => String): mutable.Map[String, A] = {
    val map = mutable.Map[String, A]()
    input.foreach { x =>
      map += (nameFn.apply(x) -> x)
    }
    map
  }

  private def listBufferToNonUniqueNameMap[A](input: ListBuffer[A], nameFn: A => String): mutable.Map[String, ListBuffer[A]] = {
    val map = mutable.Map[String, ListBuffer[A]]()
    input.foreach { x =>
      val name = nameFn.apply(x)
      if (!map.containsKey(name)) {
        map += (name -> new ListBuffer[A]())
      }
      map(name) += x
    }
    map
  }

  private def previewDomainImportInternal(exportedDomain: com.gitb.xml.export.Domain, targetDomainId: Option[Long]): (DomainImportInfo, ImportItem) = {
    var targetDomain: Option[models.Domain] = None
    if (targetDomainId.isDefined) {
      targetDomain = exec(PersistenceSchema.domains
        .filter(_.id === targetDomainId.get)
        .result
        .headOption)
    }
    val importTargets = ImportTargets.fromDomain(exportedDomain)
    /*
    Important:
    ----------
    Maps named target* are checked within the processing of the domain and will have elements removed when matched. The
    remaining values from these maps will be flagged for deletion.
    Maps named reference* are populated but never have elements removed from them. These are used to pass reference
    information back to the community preview processing.
     */
    var targetSpecificationMap = mutable.Map[String, models.Specifications]()
    var targetSpecificationIdMap = mutable.Map[Long, models.Specifications]()
    var targetSpecificationTestSuiteMap = mutable.Map[Long, mutable.Map[String, models.TestSuites]]()
    var targetSpecificationActorMap = mutable.Map[Long, mutable.Map[String, models.Actors]]()
    var targetActorEndpointMap = mutable.Map[Long, mutable.Map[String, models.Endpoints]]()
    var referenceActorEndpointMap = mutable.Map[Long, mutable.Map[String, models.Endpoints]]()
    var referenceActorToSpecificationMap = mutable.Map[Long, models.Specifications]()
    var targetEndpointParameterMap = mutable.Map[Long, mutable.Map[String, models.Parameters]]()
    var referenceEndpointParameterMap = mutable.Map[Long, mutable.Map[String, models.Parameters]]()
    var referenceEndpointParameterIdMap = mutable.Map[Long, models.Parameters]()
    var targetDomainParametersMap = mutable.Map[String, models.DomainParameter]()

    var importItemMapSpecification = mutable.Map[String, ImportItem]()
    var importItemMapActor = mutable.Map[String, ImportItem]()
    var importItemMapEndpoint = mutable.Map[String, ImportItem]()
    var importItemDomain: ImportItem = null
    if (targetDomain.isDefined) {
      importItemDomain = new ImportItem(Some(targetDomain.get.fullname), ImportItemType.Domain, ImportItemMatch.Both, Some(targetDomain.get.id.toString), Some(exportedDomain.getId))
      // Load data.
      if (importTargets.hasSpecifications) {
        exec(PersistenceSchema.specifications
          .filter(_.domain === targetDomain.get.id)
          .result
        ).map(x => {
          targetSpecificationMap += (x.shortname -> x)
          targetSpecificationIdMap += (x.id -> x)
        })
        if (importTargets.hasTestSuites) {
          exportManager.loadSpecificationTestSuiteMap(targetDomain.get.id).map { x =>
            targetSpecificationTestSuiteMap += (x._1 -> listBufferToNameMap(x._2, { t => t.shortname}))
          }
        }
        if (importTargets.hasActors) {
          exportManager.loadSpecificationActorMap(targetDomain.get.id).map { x =>
            targetSpecificationActorMap += (x._1 -> listBufferToNameMap(x._2, { a => a.actorId }))
            x._2.foreach { a =>
              referenceActorToSpecificationMap += (a.id -> targetSpecificationIdMap(x._1))
            }
            true
          }
          if (importTargets.hasEndpoints) {
            exportManager.loadActorEndpointMap(targetDomain.get.id).map { x =>
              targetActorEndpointMap += (x._1 -> listBufferToNameMap(x._2, { e => e.name }))
              referenceActorEndpointMap += (x._1 -> listBufferToNameMap(x._2, { e => e.name }))
            }
            if (importTargets.hasEndpointParameters) {
              exportManager.loadEndpointParameterMap(targetDomain.get.id).map { x =>
                targetEndpointParameterMap += (x._1 -> listBufferToNameMap(x._2, { p => p.name }))
                referenceEndpointParameterMap += (x._1 -> listBufferToNameMap(x._2, { p => p.name }))
                x._2.foreach { p =>
                  referenceEndpointParameterIdMap += (p.id -> p)
                }
                true
              }
            }
          }
        }
      }
      if (importTargets.hasDomainParameters) {
        exec(PersistenceSchema.domainParameters
          .filter(_.domain === targetDomain.get.id)
          .result
        ).map(x => targetDomainParametersMap += (x.name -> x))
      }
    } else {
      importItemDomain = new ImportItem(Some(exportedDomain.getFullName), ImportItemType.Domain, ImportItemMatch.ArchiveOnly, None, Some(exportedDomain.getId))
    }
    // Domain parameters.
    if (importTargets.hasDomainParameters) {
      exportedDomain.getParameters.getParameter.foreach { exportedDomainParameter =>
        var targetParameter: Option[models.DomainParameter] = None
        if (targetDomain.isDefined) {
          targetParameter = targetDomainParametersMap.remove(exportedDomainParameter.getName)
        }
        if (targetParameter.isDefined) {
          new ImportItem(Some(targetParameter.get.name), ImportItemType.DomainParameter, ImportItemMatch.Both, Some(targetParameter.get.id.toString), Some(exportedDomainParameter.getId), importItemDomain)
        } else {
          new ImportItem(Some(exportedDomainParameter.getName), ImportItemType.DomainParameter, ImportItemMatch.ArchiveOnly, None, Some(exportedDomainParameter.getId), importItemDomain)
        }
      }
    }
    // Specifications.
    if (importTargets.hasSpecifications) {
      exportedDomain.getSpecifications.getSpecification.foreach { exportedSpecification =>
        var targetSpecification: Option[models.Specifications] = None
        var importItemSpecification: ImportItem = null
        if (targetDomain.isDefined) {
          targetSpecification = targetSpecificationMap.remove(exportedSpecification.getShortName)
        }
        if (targetSpecification.isDefined) {
          importItemSpecification = new ImportItem(Some(targetSpecification.get.fullname), ImportItemType.Specification, ImportItemMatch.Both, Some(targetSpecification.get.id.toString), Some(exportedSpecification.getId), importItemDomain)
          importItemMapSpecification += (targetSpecification.get.id.toString -> importItemSpecification)
        } else {
          importItemSpecification = new ImportItem(Some(exportedSpecification.getFullName), ImportItemType.Specification, ImportItemMatch.ArchiveOnly, None, Some(exportedSpecification.getId), importItemDomain)
        }
        // Test suites.
        if (exportedSpecification.getTestSuites != null) {
          exportedSpecification.getTestSuites.getTestSuite.foreach { exportedTestSuite =>
            var targetTestSuite: Option[models.TestSuites] = None
            if (targetSpecification.isDefined && targetSpecificationTestSuiteMap.contains(targetSpecification.get.id)) {
              targetTestSuite = targetSpecificationTestSuiteMap(targetSpecification.get.id).remove(exportedTestSuite.getShortName)
            }
            if (targetTestSuite.isDefined) {
              new ImportItem(Some(targetTestSuite.get.fullname), ImportItemType.TestSuite, ImportItemMatch.Both, Some(targetTestSuite.get.id.toString), Some(exportedTestSuite.getId), importItemSpecification)
            } else {
              new ImportItem(Some(exportedTestSuite.getShortName), ImportItemType.TestSuite, ImportItemMatch.ArchiveOnly, None, Some(exportedTestSuite.getId), importItemSpecification)
            }
          }
        }
        // Actors
        if (exportedSpecification.getActors != null) {
          exportedSpecification.getActors.getActor.foreach { exportedActor =>
            var targetActor: Option[models.Actors] = None
            var importItemActor: ImportItem = null
            if (targetSpecification.isDefined && targetSpecificationActorMap.contains(targetSpecification.get.id)) {
              targetActor = targetSpecificationActorMap(targetSpecification.get.id).remove(exportedActor.getActorId)
            }
            if (targetActor.isDefined) {
              importItemActor = new ImportItem(Some(targetActor.get.name), ImportItemType.Actor, ImportItemMatch.Both, Some(targetActor.get.id.toString), Some(exportedActor.getId), importItemSpecification)
              importItemMapActor += (targetActor.get.id.toString -> importItemActor)
            } else {
              importItemActor = new ImportItem(Some(exportedActor.getName), ImportItemType.Actor, ImportItemMatch.ArchiveOnly, None, Some(exportedActor.getId), importItemSpecification)
            }
            // Endpoints
            if (exportedActor.getEndpoints != null) {
              exportedActor.getEndpoints.getEndpoint.foreach { exportedEndpoint =>
                var targetEndpoint: Option[models.Endpoints] = None
                var importItemEndpoint: ImportItem = null
                if (targetActor.isDefined &&  targetActorEndpointMap.contains(targetActor.get.id)) {
                  targetEndpoint = targetActorEndpointMap(targetActor.get.id).remove(exportedEndpoint.getName)
                }
                if (targetEndpoint.isDefined) {
                  importItemEndpoint = new ImportItem(Some(targetEndpoint.get.name), ImportItemType.Endpoint, ImportItemMatch.Both, Some(targetEndpoint.get.id.toString), Some(exportedEndpoint.getId), importItemActor)
                  importItemMapEndpoint += (targetEndpoint.get.id.toString -> importItemEndpoint)
                } else {
                  importItemEndpoint = new ImportItem(Some(exportedEndpoint.getName), ImportItemType.Endpoint, ImportItemMatch.ArchiveOnly, None, Some(exportedEndpoint.getId), importItemActor)
                }
                // Endpoint parameters
                if (exportedEndpoint.getParameters != null) {
                  exportedEndpoint.getParameters.getParameter.foreach { exportedEndpointParameter =>
                    var targetEndpointParameter: Option[models.Parameters] = None
                    if (targetEndpoint.isDefined && targetEndpointParameterMap.contains(targetEndpoint.get.id)) {
                      targetEndpointParameter = targetEndpointParameterMap(targetEndpoint.get.id).remove(exportedEndpointParameter.getName)
                    }
                    if (targetEndpointParameter.isDefined) {
                      new ImportItem(Some(targetEndpointParameter.get.name), ImportItemType.EndpointParameter, ImportItemMatch.Both, Some(targetEndpointParameter.get.id.toString), Some(exportedEndpointParameter.getId), importItemEndpoint)
                    } else {
                      new ImportItem(Some(exportedEndpointParameter.getName), ImportItemType.EndpointParameter, ImportItemMatch.ArchiveOnly, None, Some(exportedEndpointParameter.getId), importItemEndpoint)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    // Mark items not found for deletion.
    targetDomainParametersMap.values.foreach { parameter =>
      new ImportItem(Some(parameter.name), ImportItemType.DomainParameter, ImportItemMatch.DBOnly, Some(parameter.id.toString), None, importItemDomain)
    }
    targetSpecificationMap.values.foreach { specification =>
      val item = new ImportItem(Some(specification.fullname), ImportItemType.Specification, ImportItemMatch.DBOnly, Some(specification.id.toString), None, importItemDomain)
      importItemMapSpecification += (specification.id.toString -> item)
    }
    targetSpecificationTestSuiteMap.values.foreach { x =>
      x.values.foreach { testSuite =>
        new ImportItem(Some(testSuite.fullname), ImportItemType.TestSuite, ImportItemMatch.DBOnly, Some(testSuite.id.toString), None, importItemMapSpecification(testSuite.specification.toString))
      }
    }
    targetSpecificationActorMap.entrySet.foreach { entry =>
      entry.getValue.values.foreach { actor =>
        val item = new ImportItem(Some(actor.name), ImportItemType.Actor, ImportItemMatch.DBOnly, Some(actor.id.toString), None, importItemMapSpecification(entry.getKey.toString))
        importItemMapActor += (actor.id.toString -> item)
      }
    }
    targetActorEndpointMap.values.foreach { x =>
      x.values.foreach { endpoint =>
        val item = new ImportItem(Some(endpoint.name), ImportItemType.Endpoint, ImportItemMatch.DBOnly, Some(endpoint.id.toString), None, importItemMapActor(endpoint.actor.toString))
        importItemMapEndpoint += (endpoint.id.toString -> item)
      }
    }
    targetEndpointParameterMap.values.foreach { x =>
      x.values.foreach { parameter =>
        new ImportItem(Some(parameter.name), ImportItemType.EndpointParameter, ImportItemMatch.DBOnly, Some(parameter.id.toString), None, importItemMapEndpoint(parameter.endpoint.toString))
      }
    }
    (DomainImportInfo(referenceActorEndpointMap, referenceEndpointParameterMap, referenceActorToSpecificationMap, referenceEndpointParameterIdMap), importItemDomain)
  }


  def previewDomainImport(exportedDomain: com.gitb.xml.export.Domain, targetDomainId: Option[Long]): ImportItem = {
    previewDomainImportInternal(exportedDomain, targetDomainId)._2
  }

  def previewCommunityImport(exportedCommunity: com.gitb.xml.export.Community, targetCommunityId: Option[Long]): (ImportItem, Option[ImportItem]) = {
    var importItemCommunity: ImportItem = null
    var targetCommunity: Option[models.Communities] = None
    if (targetCommunityId.isDefined) {
      require(targetCommunityId.get != Constants.DefaultCommunityId, "The Test Bed's default community cannot be the target of an import")
      targetCommunity = exec(PersistenceSchema.communities
        .filter(_.id === targetCommunityId.get)
        .result
        .headOption)
    }
    val importTargets = ImportTargets.fromCommunity(exportedCommunity)
    // Process first domain and get back reference maps.
    var domainImportInfo: DomainImportInfo = null
    var importItemDomain: Option[ImportItem] = None
    if (importTargets.hasDomain && targetCommunity.isDefined) {
      val domainImportResult = previewDomainImportInternal(exportedCommunity.getDomain, targetCommunity.get.domain)
      domainImportInfo = domainImportResult._1
      importItemDomain = Some(domainImportResult._2)
    }
    var targetAdministratorsMap = mutable.Map[String, models.Users]()
    var targetOrganisationPropertyMap = mutable.Map[String, models.OrganisationParameters]()
    var targetOrganisationPropertyIdMap = mutable.Map[Long, models.OrganisationParameters]()
    var targetSystemPropertyMap = mutable.Map[String, models.SystemParameters]()
    var targetSystemPropertyIdMap = mutable.Map[Long, models.SystemParameters]()
    var targetCustomLabelMap = mutable.Map[LabelType, models.CommunityLabels]()
    var targetLandingPageMap = mutable.Map[String, ListBuffer[Long]]()
    var targetLegalNoticeMap = mutable.Map[String, ListBuffer[Long]]()
    var targetErrorTemplateMap = mutable.Map[String, ListBuffer[Long]]()
    var targetOrganisationMap = mutable.Map[String, mutable.ListBuffer[models.Organizations]]()
    var targetOrganisationPropertyValueMap = mutable.Map[Long, mutable.Map[String, models.OrganisationParameterValues]]()
    var targetOrganisationUserMap = mutable.Map[Long, mutable.Map[String, models.Users]]()
    var targetSystemMap = mutable.Map[Long, mutable.Map[String, ListBuffer[models.Systems]]]()
    var targetSystemIdMap = mutable.Map[Long, models.Systems]()
    var targetSystemPropertyValueMap = mutable.Map[Long, mutable.Map[String, models.SystemParameterValues]]()
    var targetStatementMap = mutable.Map[Long, mutable.Map[String, (String, models.Actors)]]() // System to [actor name to (specification name, actor)]
    var targetStatementConfigurationMap = mutable.Map[String, mutable.Map[String, models.Configs]]() // [Actor ID]_[System ID]_[Parameter ID] to Parameter name to Configs
    var importItemMapOrganisation = mutable.Map[String, ImportItem]()
    var importItemMapSystem = mutable.Map[String, ImportItem]()
    var importItemMapStatement = mutable.Map[String, ImportItem]()
    if (targetCommunity.isDefined) {
      importItemCommunity = new ImportItem(Some(targetCommunity.get.fullname), ImportItemType.Community, ImportItemMatch.Both, Some(targetCommunity.get.id.toString), Some(exportedCommunity.getId))
      // Load data.
      // Administrators.
      if (importTargets.hasAdministrators) {
        exportManager.loadAdministrators(targetCommunity.get.id).map { x =>
          targetAdministratorsMap += (x.email -> x)
        }
      }
      // Organisation properties.
      if (importTargets.hasOrganisationProperties) {
        exportManager.loadOrganisationProperties(targetCommunity.get.id).map { x =>
          targetOrganisationPropertyMap += (x.testKey -> x)
          targetOrganisationPropertyIdMap += (x.id -> x)
        }
      }
      // System properties.
      if (importTargets.hasSystemProperties) {
        exportManager.loadSystemProperties(targetCommunity.get.id).map { x =>
          targetSystemPropertyMap += (x.testKey -> x)
          targetSystemPropertyIdMap += (x.id -> x)
        }
      }
      // Custom labels.
      if (importTargets.hasCustomLabels) {
        communityManager.getCommunityLabels(targetCommunity.get.id).foreach { x =>
          targetCustomLabelMap += (LabelType.apply(x.labelType) -> x)
        }
      }
      // Landing pages.
      if (importTargets.hasLandingPages) {
        exec(PersistenceSchema.landingPages.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
          if (!targetLandingPageMap.containsKey(x._1)) {
            targetLandingPageMap += (x._1 -> new ListBuffer[Long]())
          }
          targetLandingPageMap(x._1) += x._2
        }
      }
      // Legal notices.
      if (importTargets.hasLegalNotices) {
        exec(PersistenceSchema.legalNotices.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
          if (!targetLegalNoticeMap.containsKey(x._1)) {
            targetLegalNoticeMap += (x._1 -> new ListBuffer[Long]())
          }
          targetLegalNoticeMap(x._1) += x._2
        }
      }
      // Error templates.
      if (importTargets.hasErrorTemplates) {
        exec(PersistenceSchema.errorTemplates.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
          if (!targetErrorTemplateMap.containsKey(x._1)) {
            targetErrorTemplateMap += (x._1 -> new ListBuffer[Long]())
          }
          targetErrorTemplateMap(x._1) += x._2
        }
      }
      // Organisations.
      if (importTargets.hasOrganisations) {
        exportManager.loadOrganisations(targetCommunity.get.id).foreach { x =>
          if (!targetOrganisationMap.containsKey(x.shortname)) {
            targetOrganisationMap += (x.shortname -> new ListBuffer[Organizations]())
          }
          targetOrganisationMap(x.shortname) += x
        }
        // Users.
        if (importTargets.hasOrganisationUsers) {
          exportManager.loadOrganisationUserMap(targetCommunity.get.id).map { x =>
            targetOrganisationUserMap += (x._1 -> listBufferToNameMap(x._2,  { u => u.email }))
          }
        }
        // Organisation property values.
        if (importTargets.hasOrganisationPropertyValues) {
          exportManager.loadOrganisationParameterValueMap(targetCommunity.get.id).map { x =>
            targetOrganisationPropertyValueMap += (x._1 -> listBufferToNameMap(x._2, { v => targetOrganisationPropertyIdMap(v.parameter).testKey }))
          }
        }
        // Systems.
        if (importTargets.hasSystems) {
          exportManager.loadOrganisationSystemMap(targetCommunity.get.id).map { x =>
            targetSystemMap += (x._1 -> listBufferToNonUniqueNameMap(x._2, { s => s.shortname }))
            x._2.foreach { system =>
              targetSystemIdMap += (system.id -> system)
            }
            true
          }
          // System property values.
          if (importTargets.hasSystemPropertyValues) {
            exportManager.loadSystemParameterValues(targetCommunity.get.id).map { x =>
              targetSystemPropertyValueMap += (x._1 -> listBufferToNameMap(x._2, { v => targetSystemPropertyIdMap(v.parameter).testKey }))
            }
          }
          // Statements.
          if (importTargets.hasStatements) {
            exportManager.loadSystemStatementsMap(targetCommunity.get.id, targetCommunity.get.domain).map { x =>
              targetStatementMap += (x._1 -> listBufferToNameMap(x._2, { a => a._2.actorId }))
            }
            // Statement configurations.
            if (importTargets.hasStatementConfigurations) {
              exportManager.loadSystemConfigurationsMap(targetCommunity.get).map { x =>
                // System to [Actor ID]_[System ID]_[Parameter ID])
                targetStatementConfigurationMap += (x._1 -> listBufferToNameMap(x._2, { v => domainImportInfo.targetEndpointParameterIdMap(v.parameter).name }))
              }
            }
          }
        }
      }
    } else {
      importItemCommunity = new ImportItem(Some(exportedCommunity.getFullName), ImportItemType.Community, ImportItemMatch.ArchiveOnly, None, Some(exportedCommunity.getId))
    }
    // Administrators.
    if (importTargets.hasAdministrators) {
      exportedCommunity.getAdministrators.getAdministrator.foreach { exportedAdministrator =>
        var targetAdministrator: Option[models.Users] = None
        if (targetCommunity.isDefined) {
          targetAdministrator = targetAdministratorsMap.remove(exportedAdministrator.getEmail)
        }
        if (targetAdministrator.isDefined) {
          new ImportItem(Some(targetAdministrator.get.name), ImportItemType.Administrator, ImportItemMatch.Both, Some(targetAdministrator.get.id.toString), Some(exportedAdministrator.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedAdministrator.getName), ImportItemType.Administrator, ImportItemMatch.ArchiveOnly, None, Some(exportedAdministrator.getId), importItemCommunity)
        }
      }
    }
    // Organisation properties.
    if (importTargets.hasOrganisationProperties) {
      exportedCommunity.getOrganisationProperties.getProperty.foreach { exportedProperty =>
        var targetProperty: Option[OrganisationParameters] = None
        if (targetCommunity.isDefined) {
          targetProperty = targetOrganisationPropertyMap.remove(exportedProperty.getName)
        }
        if (targetProperty.isDefined) {
          new ImportItem(Some(targetProperty.get.name), ImportItemType.OrganisationProperty, ImportItemMatch.Both, Some(targetProperty.get.id.toString), Some(exportedProperty.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedProperty.getName), ImportItemType.OrganisationProperty, ImportItemMatch.ArchiveOnly, None, Some(exportedProperty.getId), importItemCommunity)
        }
      }
    }
    // System properties.
    if (importTargets.hasSystemProperties) {
      exportedCommunity.getSystemProperties.getProperty.foreach { exportedProperty =>
        var targetProperty: Option[SystemParameters] = None
        if (targetCommunity.isDefined) {
          targetProperty = targetSystemPropertyMap.remove(exportedProperty.getName)
        }
        if (targetProperty.isDefined) {
          new ImportItem(Some(targetProperty.get.name), ImportItemType.SystemProperty, ImportItemMatch.Both, Some(targetProperty.get.id.toString), Some(exportedProperty.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedProperty.getName), ImportItemType.SystemProperty, ImportItemMatch.ArchiveOnly, None, Some(exportedProperty.getId), importItemCommunity)
        }
      }
    }
    // Custom labels.
    if (importTargets.hasCustomLabels) {
      exportedCommunity.getCustomLabels.getLabel.foreach { exportedLabel =>
        var targetLabel: Option[CommunityLabels] = None
        var labelType: LabelType = null
        exportedLabel.getLabelType match {
          case com.gitb.xml.export.CustomLabelType.DOMAIN => labelType = LabelType.Domain
          case com.gitb.xml.export.CustomLabelType.SPECIFICATION => labelType = LabelType.Specification
          case com.gitb.xml.export.CustomLabelType.ACTOR => labelType = LabelType.Actor
          case com.gitb.xml.export.CustomLabelType.ENDPOINT => labelType = LabelType.Endpoint
          case com.gitb.xml.export.CustomLabelType.ORGANISATION => labelType = LabelType.Organisation
          case com.gitb.xml.export.CustomLabelType.SYSTEM => labelType = LabelType.System
          case _ => throw new IllegalArgumentException("Unknown enum value ["+exportedLabel.getLabelType+"]")
        }
        if (targetCommunity.isDefined) {
          targetLabel = targetCustomLabelMap.remove(labelType)
        }
        if (targetLabel.isDefined) {
          new ImportItem(Some(targetLabel.get.labelType.toString), ImportItemType.CustomLabel, ImportItemMatch.Both, Some(targetCommunity.get.id+"_"+targetLabel.get.labelType.toString), Some(exportedLabel.getId), importItemCommunity)
        } else {
          new ImportItem(Some(labelType.id.toString), ImportItemType.CustomLabel, ImportItemMatch.ArchiveOnly, None, Some(exportedLabel.getId), importItemCommunity)
        }
      }
    }
    // Landing pages.
    if (importTargets.hasLandingPages) {
      exportedCommunity.getLandingPages.getLandingPage.foreach { exportedLandingPage =>
        var targetLandingPage: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetLandingPageMap.get(exportedLandingPage.getName)
          if (foundContent.nonEmpty) {
            targetLandingPage = Some(foundContent.get.remove(0))
            if (foundContent.isEmpty) {
              targetLandingPageMap.remove(exportedLandingPage.getName)
            }
          }
        }
        if (targetLandingPage.isDefined) {
          new ImportItem(Some(exportedLandingPage.getName), ImportItemType.LandingPage, ImportItemMatch.Both, Some(targetLandingPage.get.toString), Some(exportedLandingPage.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedLandingPage.getName), ImportItemType.LandingPage, ImportItemMatch.ArchiveOnly, None, Some(exportedLandingPage.getId), importItemCommunity)
        }
      }
    }
    // Legal notices.
    if (importTargets.hasLegalNotices) {
      exportedCommunity.getLegalNotices.getLegalNotice.foreach { exportedLegalNotice =>
        var targetLegalNotice: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetLegalNoticeMap.get(exportedLegalNotice.getName)
          if (foundContent.nonEmpty) {
            targetLegalNotice = Some(foundContent.get.remove(0))
            if (foundContent.isEmpty) {
              targetLegalNoticeMap.remove(exportedLegalNotice.getName)
            }
          }
        }
        if (targetLegalNotice.isDefined) {
          new ImportItem(Some(exportedLegalNotice.getName), ImportItemType.LegalNotice, ImportItemMatch.Both, Some(targetLegalNotice.get.toString), Some(exportedLegalNotice.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedLegalNotice.getName), ImportItemType.LegalNotice, ImportItemMatch.ArchiveOnly, None, Some(exportedLegalNotice.getId), importItemCommunity)
        }
      }
    }
    // Error templates.
    if (importTargets.hasErrorTemplates) {
      exportedCommunity.getErrorTemplates.getErrorTemplate.foreach { exportedErrorTemplate =>
        var targetErrorTemplate: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetErrorTemplateMap.get(exportedErrorTemplate.getName)
          if (foundContent.nonEmpty) {
            targetErrorTemplate = Some(foundContent.get.remove(0))
            if (foundContent.isEmpty) {
              targetErrorTemplateMap.remove(exportedErrorTemplate.getName)
            }
          }
        }
        if (targetErrorTemplate.isDefined) {
          new ImportItem(Some(exportedErrorTemplate.getName), ImportItemType.ErrorTemplate, ImportItemMatch.Both, Some(targetErrorTemplate.get.toString), Some(exportedErrorTemplate.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedErrorTemplate.getName), ImportItemType.ErrorTemplate, ImportItemMatch.ArchiveOnly, None, Some(exportedErrorTemplate.getId), importItemCommunity)
        }
      }
    }
    // Organisations.
    if (importTargets.hasOrganisations) {
      exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
        var targetOrganisation: Option[models.Organizations] = None
        var importItemOrganisation: ImportItem = null
        if (targetCommunity.isDefined) {
          val foundOrganisations = targetOrganisationMap.get(exportedOrganisation.getShortName)
          if (foundOrganisations.nonEmpty) {
            targetOrganisation = Some(foundOrganisations.get.remove(0))
            if (foundOrganisations.isEmpty) {
              targetOrganisationMap.remove(exportedOrganisation.getShortName)
            }
          }
        }
        if (targetOrganisation.isDefined) {
          importItemOrganisation = new ImportItem(Some(targetOrganisation.get.fullname), ImportItemType.Organisation, ImportItemMatch.Both, Some(targetOrganisation.get.id.toString), Some(exportedOrganisation.getId), importItemCommunity)
          importItemMapOrganisation += (targetOrganisation.get.id.toString -> importItemOrganisation)
        } else {
          importItemOrganisation = new ImportItem(Some(exportedOrganisation.getFullName), ImportItemType.Organisation, ImportItemMatch.ArchiveOnly, None, Some(exportedOrganisation.getId), importItemCommunity)
        }
        // Users.
        if (exportedOrganisation.getUsers != null) {
          exportedOrganisation.getUsers.getUser.foreach { exportedUser =>
            var targetUser: Option[models.Users] = None
            if (targetOrganisation.isDefined) {
              targetUser = targetOrganisationUserMap(targetOrganisation.get.id).remove(exportedUser.getEmail)
            }
            if (targetUser.isDefined) {
              new ImportItem(Some(targetUser.get.name), ImportItemType.OrganisationUser, ImportItemMatch.Both, Some(targetUser.get.id.toString), Some(exportedUser.getId), importItemOrganisation)
            } else {
              new ImportItem(Some(exportedUser.getName), ImportItemType.OrganisationUser, ImportItemMatch.ArchiveOnly, None, Some(exportedUser.getId), importItemOrganisation)
            }
          }
        }
        // Organisation property values.
        if (exportedOrganisation.getPropertyValues != null) {
          exportedOrganisation.getPropertyValues.getProperty.foreach { exportedProperty =>
            var targetPropertyValue: Option[models.OrganisationParameterValues] = None
            if (targetOrganisation.isDefined) {
              targetPropertyValue = targetOrganisationPropertyValueMap(targetOrganisation.get.id).remove(exportedProperty.getProperty.getName)
            }
            if (targetPropertyValue.isDefined) {
              new ImportItem(Some(targetOrganisationPropertyIdMap(targetPropertyValue.get.parameter).name), ImportItemType.OrganisationPropertyValue, ImportItemMatch.Both, Some(targetPropertyValue.get.organisation.toString+"_"+targetPropertyValue.get.parameter.toString), Some(exportedProperty.getId), importItemOrganisation)
            } else {
              new ImportItem(Some(exportedProperty.getProperty.getName), ImportItemType.OrganisationPropertyValue, ImportItemMatch.ArchiveOnly, None, Some(exportedProperty.getId), importItemOrganisation)
            }
          }
        }
        // Systems.
        if (exportedOrganisation.getSystems != null) {
          exportedOrganisation.getSystems.getSystem.foreach { exportedSystem =>
            var targetSystem: Option[models.Systems] = None
            var importItemSystem: ImportItem = null
            if (targetOrganisation.isDefined) {
                if (targetSystemMap(targetOrganisation.get.id).containsKey(exportedSystem.getShortName)) {
                  val foundSystems = targetSystemMap(targetOrganisation.get.id)(exportedSystem.getShortName)
                  if (foundSystems.nonEmpty) {
                    targetSystem = Some(foundSystems.remove(0))
                    if (foundSystems.isEmpty) {
                      targetSystemMap(targetOrganisation.get.id).remove(exportedSystem.getShortName)
                    }
                  }
                }
            }
            if (targetSystem.isDefined) {
              importItemSystem = new ImportItem(Some(targetSystem.get.fullname), ImportItemType.System, ImportItemMatch.Both, Some(targetSystem.get.id.toString), Some(exportedSystem.getId), importItemOrganisation)
              importItemMapSystem += (targetSystem.get.id.toString -> importItemSystem)
            } else {
              importItemSystem = new ImportItem(Some(exportedSystem.getShortName), ImportItemType.System, ImportItemMatch.ArchiveOnly, None, Some(exportedSystem.getId), importItemOrganisation)
            }
            // System property values.
            if (exportedSystem.getPropertyValues != null) {
              exportedSystem.getPropertyValues.getProperty.foreach { exportedValue =>
                var targetPropertyValue: Option[models.SystemParameterValues] = None
                if (targetSystem.isDefined) {
                  targetPropertyValue = targetSystemPropertyValueMap(targetSystem.get.id).remove(exportedValue.getProperty.getName)
                }
                if (targetPropertyValue.isDefined) {
                  new ImportItem(Some(targetSystemPropertyIdMap(targetPropertyValue.get.parameter).name), ImportItemType.SystemPropertyValue, ImportItemMatch.Both, Some(targetPropertyValue.get.system.toString+"_"+targetPropertyValue.get.parameter.toString), Some(exportedValue.getId), importItemSystem)
                } else {
                  new ImportItem(Some(exportedValue.getProperty.getName), ImportItemType.SystemPropertyValue, ImportItemMatch.ArchiveOnly, None, Some(exportedValue.getId), importItemSystem)
                }
              }
            }
            // Statements.
            if (exportedSystem.getStatements != null) {
              exportedSystem.getStatements.getStatement.foreach { exportedStatement =>
                var targetStatement: Option[(String, models.Actors)] = None
                var targetActor: models.Actors = null
                var importItemStatement: ImportItem = null
                if (targetSystem.isDefined) {
                  targetStatement = targetStatementMap(targetSystem.get.id).remove(exportedStatement.getActor.getActorId)
                }
                if (targetStatement.isDefined) {
                  targetActor = targetStatement.get._2
                  importItemStatement = new ImportItem(Some(domainImportInfo.targetActorToSpecificationMap(targetActor.id).fullname +" ("+targetActor.name+")"), ImportItemType.Statement, ImportItemMatch.Both, Some(targetSystem.get.id.toString+"_"+targetActor.id.toString), Some(exportedStatement.getId), importItemSystem)
                  importItemMapStatement += ((targetSystem.get.id.toString+"_"+targetActor.id.toString) -> importItemStatement)
                } else {
                  importItemStatement = new ImportItem(Some(exportedStatement.getActor.getSpecification.getFullName + "("+exportedStatement.getActor.getName+")"), ImportItemType.Statement, ImportItemMatch.ArchiveOnly, None, Some(exportedStatement.getId), importItemSystem)
                }
                // Statement configurations.
                if (exportedStatement.getConfigurations != null) {
                  exportedStatement.getConfigurations.getConfiguration.foreach { exportedConfig =>
                    var targetConfig: Option[models.Configs] = None
                    var targetConfigParam: Option[models.Parameters] = None
                    if (targetStatement.isDefined) {
                      val targetEndpoint = domainImportInfo.targetActorEndpointMap(targetActor.id).get(exportedConfig.getParameter.getEndpoint.getName)
                      if (targetEndpoint.isDefined) {
                        targetConfigParam = domainImportInfo.targetEndpointParameterMap(targetEndpoint.get.id).get(exportedConfig.getParameter.getName)
                        if (targetConfigParam.isDefined) {
                          val key = targetEndpoint.get.actor + "_" + targetSystem.get.id + "_" + targetConfigParam.get.id
                          targetConfig = targetStatementConfigurationMap(key).remove(exportedConfig.getParameter.getName)
                        }
                      }
                    }
                    if (targetConfig.isDefined) {
                      new ImportItem(Some(targetConfigParam.get.name), ImportItemType.StatementConfiguration, ImportItemMatch.Both, Some(targetConfig.get.system.toString+"_"+targetConfig.get.parameter.toString), Some(exportedConfig.getId), importItemStatement)
                    } else {
                      new ImportItem(Some(exportedConfig.getParameter.getName), ImportItemType.StatementConfiguration, ImportItemMatch.ArchiveOnly, None, Some(exportedConfig.getId), importItemStatement)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    // Mark items not found for deletion.
    targetAdministratorsMap.values.foreach { user =>
      new ImportItem(Some(user.name), ImportItemType.Administrator, ImportItemMatch.DBOnly, Some(user.id.toString), None, importItemCommunity)
    }
    targetOrganisationPropertyMap.values.foreach { property =>
      new ImportItem(Some(property.name), ImportItemType.OrganisationProperty, ImportItemMatch.DBOnly, Some(property.id.toString), None, importItemCommunity)
    }
    targetSystemPropertyMap.values.foreach { property =>
      new ImportItem(Some(property.name), ImportItemType.SystemProperty, ImportItemMatch.DBOnly, Some(property.id.toString), None, importItemCommunity)
    }
    targetCustomLabelMap.values.foreach { label =>
      new ImportItem(Some(label.labelType.toString), ImportItemType.CustomLabel, ImportItemMatch.DBOnly, Some(label.labelType.toString), None, importItemCommunity)
    }
    targetLandingPageMap.entrySet.foreach { entry =>
      entry.getValue.foreach { x =>
        new ImportItem(Some(entry.getKey), ImportItemType.LandingPage, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetLegalNoticeMap.entrySet.foreach { entry =>
      entry.getValue.foreach { x =>
        new ImportItem(Some(entry.getKey), ImportItemType.LegalNotice, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetErrorTemplateMap.entrySet.foreach { entry =>
      entry.getValue.foreach { x =>
        new ImportItem(Some(entry.getKey), ImportItemType.ErrorTemplate, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetOrganisationMap.entrySet.foreach { entry =>
      entry.getValue.foreach { x =>
        val item = new ImportItem(Some(entry.getKey), ImportItemType.Organisation, ImportItemMatch.DBOnly, Some(x.id.toString), None, importItemCommunity)
        importItemMapOrganisation += (x.id.toString -> item)
      }
    }
    targetOrganisationPropertyValueMap.entrySet.foreach { entry =>
      entry.getValue.entrySet.foreach { x =>
        new ImportItem(Some(x.getKey), ImportItemType.OrganisationPropertyValue, ImportItemMatch.DBOnly, Some(x.getValue.organisation.toString+"_"+x.getValue.parameter.toString), None, importItemMapOrganisation(entry.getKey.toString))
      }
    }
    targetOrganisationUserMap.entrySet.foreach { entry =>
      entry.getValue.values.foreach { user =>
        new ImportItem(Some(user.name), ImportItemType.OrganisationUser, ImportItemMatch.DBOnly, Some(user.id.toString), None, importItemMapOrganisation(entry.getKey.toString))
      }
    }
    targetSystemMap.entrySet.foreach { entry =>
      entry.getValue.values.foreach { systems =>
        systems.foreach { system =>
          val item = new ImportItem(Some(system.fullname), ImportItemType.System, ImportItemMatch.DBOnly, Some(system.id.toString), None, importItemMapOrganisation(entry.getKey.toString))
          importItemMapSystem += (system.id.toString -> item)
        }
      }
    }
    targetSystemPropertyValueMap.entrySet.foreach { entry =>
      entry.getValue.entrySet.foreach { entry =>
        new ImportItem(Some(entry.getKey), ImportItemType.SystemPropertyValue, ImportItemMatch.DBOnly, Some(entry.getValue.system.toString+"_"+entry.getValue.parameter.toString), None, importItemMapSystem(entry.getKey.toString))
      }
    }
    targetStatementMap.entrySet.foreach { x =>
      x.getValue.values.foreach { statement =>
        val specificationName = statement._1
        val actor = statement._2
        val item = new ImportItem(Some(specificationName +" ("+actor.name+")"), ImportItemType.Statement, ImportItemMatch.DBOnly, Some(x.getKey+"_"+actor.id.toString), None, importItemMapSystem(x.getKey.toString))
        importItemMapStatement += (x.getKey+"_"+actor.id -> item)
      }
    }
    targetStatementConfigurationMap.entrySet.foreach { entry1 =>
      // Key is [Actor ID]_[System ID]_[Parameter ID]
      val keyParts = StringUtils.split(entry1.getKey, "_")
      entry1.getValue.entrySet.foreach { entry2 =>
        val importItemStatement = importItemMapStatement(keyParts(1)+"_"+keyParts(0)) // This map has keys as [System ID]_[Actor ID]
        new ImportItem(Some(entry2.getKey), ImportItemType.StatementConfiguration, ImportItemMatch.DBOnly, Some(entry2.getValue.system+"_"+entry2.getValue.parameter), None, importItemStatement)
      }
    }
    (importItemCommunity, importItemDomain)
  }

  def completeDomainImport(exportedDomain: com.gitb.xml.export.Domain, importSettings: ImportSettings, importItems: List[ImportItem], targetDomainId: Option[Long], canAddOrDeleteDomain: Boolean): Unit = {
    // Load context
    val ctx = ImportContext(
      importSettings,
      toImportItemMaps(importItems, ImportItemType.Domain),
      ExistingIds.init(),
      ImportTargets.fromDomain(exportedDomain),
      mutable.Map[ImportItemType, mutable.Map[String, String]](),
      mutable.Map[Long, mutable.Map[String, Long]]()
    )
    exec(completeDomainImportInternal(exportedDomain, targetDomainId, ctx, canAddOrDeleteDomain).transactionally)
  }

  private def toImportItemMaps(importItems: List[ImportItem], itemType: ImportItemType): ImportItemMaps = {
    importItems.foreach { item =>
      if (item.itemType == itemType) {
        return ImportItemMaps(item.toSourceMap(), item.toTargetMap())
      }
    }
    throw new IllegalArgumentException("Expected data from import items not found ["+itemType.id+"].")
  }

  private def mergeImportItemMaps(existingMap: ImportItemMaps, newMap: ImportItemMaps): Unit = {
    // Sources
    newMap.sourceMap.entrySet.foreach { entry =>
      if (existingMap.sourceMap.containsKey(entry.getKey)) {
        existingMap.sourceMap(entry.getKey).addAll(entry.getValue)
      } else {
        existingMap.sourceMap += (entry.getKey -> entry.getValue)
      }
    }
    // Targets
    newMap.targetMap.entrySet.foreach { entry =>
      if (existingMap.targetMap.containsKey(entry.getKey)) {
        existingMap.targetMap(entry.getKey).addAll(entry.getValue)
      } else {
        existingMap.targetMap += (entry.getKey -> entry.getValue)
      }
    }
  }

  private def addIdToProcessedIdMap(itemType:ImportItemType, xmlId: String, dbId: String, ctx: ImportContext): Unit = {
    var idMap = ctx.processedIdMap.get(itemType)
    if (idMap.isEmpty) {
      idMap = Some(mutable.Map[String, String]())
      ctx.processedIdMap += (itemType -> idMap.get)
    }
    idMap.get += (xmlId -> dbId)
  }

  private def processFromArchive[A](itemType: ImportItemType, data: A, itemId: String, ctx: ImportContext, importCallbacks: ImportCallbacks[A]): DBIO[_] = {
    var dbAction: Option[DBIO[_]] = None
    val importItem = ctx.importItemMaps.sourceMap(itemType).get(itemId)
    if (importItem.isDefined) {
      if (importItem.get.itemChoice.get == ImportItemChoice.Proceed) {
        if (importItem.get.targetKey.isEmpty) {
          // Create
          dbAction = Some(for {
            newId <- {
              val result = importCallbacks.fnCreate.apply(data, importItem.get)
              result
            }
            _ <- {
              if (importItem.get.parentItem.isDefined) {
                // Assign the ID generated for this from the DB. This will be used for FK associations from children.
                importItem.get.targetKey = Some(newId.toString)
              }
              // Maintain also a reference of all processed XML IDs to DB IDs (per type)
              if (importCallbacks.fnCreatedIdHandle.isDefined) {
                // Custom method to handle created IDs.
                importCallbacks.fnCreatedIdHandle.get.apply(data, itemId, newId.toString, importItem.get)
              } else {
                // Default handling method.
                addIdToProcessedIdMap(itemType, itemId, newId.toString, ctx)
              }
              // Custom post-create method.
              if (importCallbacks.fnPostCreate.isDefined) {
                importCallbacks.fnPostCreate.get.apply(data, newId.toString, importItem.get)
              }
              DBIO.successful(())
            }
          } yield newId)
        } else {
          // Update - check also to see if this is one of the expected IDs.
          if (ctx.existingIds.map(itemType).contains(importItem.get.targetKey.get)) {
            dbAction = Some(importCallbacks.fnUpdate.apply(data, importItem.get.targetKey.get, importItem.get))
            // Maintain also a reference of all processed XML IDs to DB IDs (per type)
            var idMap = ctx.processedIdMap.get(itemType)
            if (idMap.isEmpty) {
              idMap = Some(mutable.Map[String, String]())
              ctx.processedIdMap += (itemType -> idMap.get)
            }
            idMap.get += (itemId -> importItem.get.targetKey.get)
          }
        }
      }
    }
    if (dbAction.isEmpty) {
      dbAction = Some(DBIO.successful(()))
    }
    dbAction.get
  }

  private def processRemaining(itemType: ImportItemType, ctx: ImportContext, fnDelete: String => DBIO[_]): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    val itemMap = ctx.importItemMaps.targetMap.get(itemType)
    if (itemMap.isDefined) {
      itemMap.get.values.foreach { item =>
        if (item.sourceKey.isEmpty && item.itemChoice.get == ImportItemChoice.Proceed) {
          // Delete - check also to see if this is one of the expected IDs.
          if (ctx.existingIds.map(itemType).contains(item.targetKey.get)) {
            dbActions += fnDelete.apply(item.targetKey.get)
            // We can also mark all children of this item as skipped as these will already have been deleted.
            item.markAllChildrenAsSkipped()
          }
        }
      }
    }
    toDBIO(dbActions)
  }

  private def propertyTypeToKind(propertyType: PropertyType): String = {
    require(propertyType != null, "Enum value cannot be null")
    propertyType match {
      case PropertyType.BINARY => "BINARY"
      case PropertyType.SIMPLE => "SIMPLE"
      case PropertyType.SECRET => "SECRET"
      case _ => throw new IllegalArgumentException("Unknown enum value ["+propertyType+"]")
    }
  }

  private def selfRegistrationMethodToModel(selfRegType: com.gitb.xml.export.SelfRegistrationMethod): Short = {
    require(selfRegType != null, "Enum value cannot be null")
    selfRegType match {
      case SelfRegistrationMethod.NOT_SUPPORTED => SelfRegistrationType.NotSupported.id.toShort
      case SelfRegistrationMethod.PUBLIC => SelfRegistrationType.PublicListing.id.toShort
      case SelfRegistrationMethod.PUBLIC_WITH_TOKEN => SelfRegistrationType.PublicListingWithToken.id.toShort
      case SelfRegistrationMethod.TOKEN => SelfRegistrationType.Token.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+selfRegType+"]")
    }
  }

  private def selfRegistrationRestrictionToModel(selfRegRestriction: com.gitb.xml.export.SelfRegistrationRestriction): Short = {
    require(selfRegRestriction != null, "Enum value cannot be null")
    selfRegRestriction match {
      case SelfRegistrationRestriction.NO_RESTRICTION => Enums.SelfRegistrationRestriction.NoRestriction.id.toShort
      case SelfRegistrationRestriction.USER_EMAIL => Enums.SelfRegistrationRestriction.UserEmail.id.toShort
      case SelfRegistrationRestriction.USER_EMAIL_DOMAIN => Enums.SelfRegistrationRestriction.UserEmailDomain.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+selfRegRestriction+"]")
    }
  }

  private def userRoleToModel(role: com.gitb.xml.export.OrganisationRoleType): Short = {
    require(role != null, "Enum value cannot be null")
    role match {
      case OrganisationRoleType.ORGANISATION_ADMIN => Enums.UserRole.VendorAdmin.id.toShort
      case OrganisationRoleType.ORGANISATION_USER => Enums.UserRole.VendorUser.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+role+"]")
    }
  }

  private def labelTypeToModel(labelType: com.gitb.xml.export.CustomLabelType): Short = {
    require(labelType != null, "Enum value cannot be null")
    labelType match {
      case CustomLabelType.DOMAIN => Enums.LabelType.Domain.id.toShort
      case CustomLabelType.SPECIFICATION => Enums.LabelType.Specification.id.toShort
      case CustomLabelType.ACTOR => Enums.LabelType.Actor.id.toShort
      case CustomLabelType.ENDPOINT => Enums.LabelType.Endpoint.id.toShort
      case CustomLabelType.ORGANISATION => Enums.LabelType.Organisation.id.toShort
      case CustomLabelType.SYSTEM => Enums.LabelType.System.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+labelType+"]")
    }
  }

  private def requiredToUse(required: Boolean): String = {
    if (required) {
      "R"
    } else {
      "O"
    }
  }

  private def decryptIfNeeded(importSettings: ImportSettings, propertyType: PropertyType, value: Option[String]): Option[String] = {
    var result: Option[String] = None
    if (value.isDefined) {
      if (propertyType == PropertyType.SECRET) {
        result = Some(decrypt(importSettings, value.get))
      } else {
        result = value
      }
    }
    result
  }

  private def decrypt(importSettings: ImportSettings, value: String): String = {
    require(importSettings.encryptionKey.isDefined, "The archive's encryption key is missing.")
    try {
      MimeUtil.decryptString(value, importSettings.encryptionKey.get.toCharArray)
    } catch {
      case e: Exception => throw new IllegalArgumentException("An encrypted value could not be successfully decrypted.")
    }
  }

  private def toModelTestCases(exportedTestCases: List[com.gitb.xml.export.TestCase], specificationId: Long): List[models.TestCases] = {
    val testCases = new ListBuffer[models.TestCases]()
    exportedTestCases.foreach { exportedTestCase =>
      testCases += models.TestCases(0L,
        exportedTestCase.getShortName, exportedTestCase.getFullName, exportedTestCase.getVersion, Option(exportedTestCase.getAuthors),
        Option(exportedTestCase.getOriginalDate), Option(exportedTestCase.getModificationDate), Option(exportedTestCase.getDescription),
        Option(exportedTestCase.getKeywords), exportedTestCase.getTestCaseType, "", specificationId,
        Option(exportedTestCase.getTargetActors), None, exportedTestCase.getTestSuiteOrder, exportedTestCase.isHasDocumentation,
        Option(exportedTestCase.getDocumentation)
      )
    }
    testCases.toList
  }

  private def getResourcePaths(testSuiteFolderName: String, testCases: List[com.gitb.xml.export.TestCase]): Map[String, String] = {
    val paths = mutable.Map[String, String]()
    testCases.foreach { testCase =>
      var pathToSet = testCase.getPath
      if (pathToSet.startsWith("/")) {
        pathToSet = testSuiteFolderName + pathToSet
      }
      paths += (testCase.getShortName -> pathToSet)
    }
    paths.toMap
  }

  private def saveTestSuiteFiles(data: TestSuite, item: ImportItem, domainId: Long, specificationId: Long, ctx: ImportContext): File = {
    // File system operations
    val testSuiteData = Base64.decodeBase64(data.getData)
    if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      require(ClamAVClient.isCleanReply(virusScanner.scan(testSuiteData)), "A virus was found in one of the imported test suites")
    }
    val tempTestSuitePath = Paths.get(ctx.importSettings.dataFilePath.get.getParent.toFile.getAbsolutePath, "testcases", item.sourceKey.get+".zip")
    Files.createDirectories(tempTestSuitePath.getParent)
    FileUtils.writeByteArrayToFile(tempTestSuitePath.toFile, testSuiteData)
    // Extract test suite to target location.
    val testSuiteFileName = RepositoryUtils.generateTestSuiteFileName()
    val targetFolder = new File(RepositoryUtils.getTestSuitesPath(domainId, specificationId), testSuiteFileName)
    RepositoryUtils.extractTestSuiteFilesFromZipToFolder(specificationId, targetFolder, tempTestSuitePath.toFile)
    targetFolder
  }

  private def toModelTestSuite(data: com.gitb.xml.export.TestSuite, specificationId: Long, testSuiteFileName: String): models.TestSuites = {
    models.TestSuites(0L, data.getShortName, data.getFullName, data.getVersion, Option(data.getAuthors),
      Option(data.getOriginalDate), Option(data.getModificationDate), Option(data.getDescription), Option(data.getKeywords),
      specificationId, testSuiteFileName, data.isHasDocumentation, Option(data.getDocumentation))
  }

  private def toModelCustomLabel(data: com.gitb.xml.export.CustomLabel, communityId: Long): models.CommunityLabels = {
    models.CommunityLabels(communityId, labelTypeToModel(data.getLabelType),
      data.getSingularForm, data.getPluralForm, data.isFixedCasing
    )
  }

  private def toModelOrganisationParameter(data: com.gitb.xml.export.OrganisationProperty, communityId: Long, modelId: Option[Long]): models.OrganisationParameters = {
    models.OrganisationParameters(modelId.getOrElse(0L), data.getLabel, data.getName, Option(data.getDescription), requiredToUse(data.isRequired),
      propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests, data.isInExports, data.isInSelfRegistration, communityId
    )
  }

  private def toModelSystemParameter(data: com.gitb.xml.export.SystemProperty, communityId: Long, modelId: Option[Long]): models.SystemParameters = {
    models.SystemParameters(modelId.getOrElse(0L), data.getLabel, data.getName, Option(data.getDescription), requiredToUse(data.isRequired),
      propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests, data.isInExports, communityId
    )
  }

  private def toModelLandingPage(data: com.gitb.xml.export.LandingPage, communityId: Long): models.LandingPages = {
    models.LandingPages(0L, data.getName, Option(data.getDescription), data.getContent, data.isDefault, communityId)
  }

  private def toModelLegalNotice(data: com.gitb.xml.export.LegalNotice, communityId: Long): models.LegalNotices = {
    models.LegalNotices(0L, data.getName, Option(data.getDescription), data.getContent, data.isDefault, communityId)
  }

  private def toModelErrorTemplate(data: com.gitb.xml.export.ErrorTemplate, communityId: Long): models.ErrorTemplates = {
    models.ErrorTemplates(0L, data.getName, Option(data.getDescription), data.getContent, data.isDefault, communityId)
  }

  private def toModelAdministrator(data: com.gitb.xml.export.CommunityAdministrator, userId: Option[Long], organisationId: Long, importSettings: ImportSettings): models.Users = {
    toModelUser(data, userId, Enums.UserRole.CommunityAdmin.id.toShort, organisationId, importSettings)
  }

  private def toModelOrganisationUser(data: com.gitb.xml.export.OrganisationUser, userId: Option[Long], userRole: Short, organisationId: Long, importSettings: ImportSettings): models.Users = {
    toModelUser(data, userId, userRole, organisationId, importSettings)
  }

  private def toModelUser(data: com.gitb.xml.export.User, userId: Option[Long], userRole: Short, organisationId: Long, importSettings: ImportSettings): models.Users = {
    models.Users(userId.getOrElse(0L), data.getName, data.getEmail, decrypt(importSettings, data.getPassword), data.isOnetimePassword, userRole, organisationId, None, None, Enums.UserSSOStatus.NotMigrated.id.toShort)
  }

  private def toModelUserRole(role: OrganisationRoleType): Short = {
    require(role != null, "Enum value cannot be null")
    role match {
      case OrganisationRoleType.ORGANISATION_ADMIN => Enums.UserRole.VendorAdmin.id.toShort
      case OrganisationRoleType.ORGANISATION_USER => Enums.UserRole.VendorUser.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+role+"]")
    }
  }

  private def getProcessedDbId(data: com.gitb.xml.export.ExportType, itemType: ImportItemType, ctx: ImportContext): Option[Long] = {
    var dbId: Option[Long] = None
    if (data != null && ctx.processedIdMap.containsKey(itemType) && ctx.processedIdMap(itemType).containsKey(data.getId)) {
      dbId = Some(ctx.processedIdMap(itemType)(data.getId).toLong)
    }
    dbId
  }

  private def getSavedActorMap(exportedTestSuite: com.gitb.xml.export.TestSuite, specificationId: Long, ctx: ImportContext): java.util.Map[String, Long] = {
    val savedActorMap = new java.util.HashMap[String, Long]()
    if (exportedTestSuite.getTestCases != null) {
      exportedTestSuite.getTestCases.getTestCase.foreach { exportedTestCase =>
        if (exportedTestCase.getActors != null) {
          exportedTestCase.getActors.getActor.foreach { actor =>
            if (!savedActorMap.containsKey(actor.getActor.getActorId)) {
              savedActorMap.put(actor.getActor.getActorId, ctx.savedSpecificationActors(specificationId)(actor.getActor.getActorId))
            }
          }
        }
      }
    }
    savedActorMap
  }

  def createTestSuite(data: TestSuite, ctx: ImportContext, item: ImportItem): DBIO[Long] = {
    val domainId = item.parentItem.get.parentItem.get.targetKey.get.toLong
    val specificationId = item.parentItem.get.targetKey.get.toLong
    // File system operations
    val testSuiteFile = saveTestSuiteFiles(data, item, domainId, specificationId, ctx)
    // Process DB operations
    val action = for {
      // Save test suite
      testSuiteId <- PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)) += toModelTestSuite(data, specificationId, testSuiteFile.getName)
      // Lookup the map of systems to actors for the specification
      systemActors <- testSuiteManager.getSystemActors(specificationId)
      // Create a map of actors to systems.
      existingActorToSystemMap <- testSuiteManager.getExistingActorToSystemMap(systemActors)
      // Save test cases
      processTestCasesStep <- testSuiteManager.stepProcessTestCases(
            specificationId,
            testSuiteId,
            Some(toModelTestCases(data.getTestCases.getTestCase.toList, specificationId)),
            getResourcePaths(testSuiteFile.getName, data.getTestCases.getTestCase.toList),
            new java.util.HashMap[String, java.lang.Long](), // existingTestCaseMap
            ctx.savedSpecificationActors(specificationId), // savedActorIds
            existingActorToSystemMap
      )
      // Update the actor links for the  test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteActorLinks(testSuiteId, getSavedActorMap(data, specificationId, ctx))
      // Update the test case links for the test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteTestCaseLinks(testSuiteId, processTestCasesStep._1)
    } yield testSuiteId
    action.flatMap(result => {
      DBIO.successful(result)
    }).cleanUp(error => {
      if (error.isDefined) {
        // Cleanup operations in case an error occurred.
        if (testSuiteFile.exists()) {
          FileUtils.deleteDirectory(testSuiteFile)
        }
        DBIO.failed(error.get)
      } else {
        DBIO.successful(())
      }
    })
  }

  def updateTestSuite(data: TestSuite, ctx: ImportContext, item: ImportItem): DBIO[_] = {
    val domainId = item.parentItem.get.parentItem.get.targetKey.get.toLong
    val specificationId = item.parentItem.get.targetKey.get.toLong
    val testSuiteId = item.targetKey.get.toLong
    // File system operations
    val testSuiteFile = saveTestSuiteFiles(data, item, domainId, specificationId, ctx)
    // Process DB operations
    val action = for {
      // Lookup existing test suite file (for later cleanup).
      existingTestSuiteFile <- PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(x => x.filename).result.head
      // Update existing test suite.
      _ <- testSuiteManager.updateTestSuiteInDb(testSuiteId, toModelTestSuite(data, specificationId, testSuiteFile.getName))
      // Remove existing actor links (these will be updated later).
      _ <- testSuiteManager.removeActorLinksForTestSuite(testSuiteId)
      // Lookup the existing test cases for the test suite.
      existingTestCasesForTestSuite <- testSuiteManager.getExistingTestCasesForTestSuite(testSuiteId)
      // Place the existing test cases in a map for further processing.
      existingTestCaseMap <- testSuiteManager.getExistingTestCaseMap(existingTestCasesForTestSuite)
      // Lookup the map of systems to actors for the specification
      systemActors <- testSuiteManager.getSystemActors(specificationId)
      // Create a map of actors to systems.
      existingActorToSystemMap <- testSuiteManager.getExistingActorToSystemMap(systemActors)
      // Process the test cases.
      processTestCasesStep <- {
        testSuiteManager.stepProcessTestCases(
          specificationId,
          testSuiteId,
          Some(toModelTestCases(data.getTestCases.getTestCase.toList, specificationId)),
          getResourcePaths(testSuiteFile.getName, data.getTestCases.getTestCase.toList),
          existingTestCaseMap,
          ctx.savedSpecificationActors(specificationId), // savedActorIds
          existingActorToSystemMap
        )
      }
      // Remove the test cases that are no longer in the test suite.
      _ <- testSuiteManager.stepRemoveTestCases(existingTestCaseMap)
      // Update the actor links for the  test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteActorLinks(testSuiteId, getSavedActorMap(data, specificationId, ctx))
      // Update the test case links for the test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteTestCaseLinks(testSuiteId, processTestCasesStep._1)
    } yield existingTestSuiteFile
    action.flatMap(existingTestSuiteFile => {
      // Finally, delete the backup folder
      val existingTestSuiteFolder = new File(RepositoryUtils.getTestSuitesPath(domainId, specificationId), existingTestSuiteFile)
      if (existingTestSuiteFolder != null && existingTestSuiteFolder.exists()) {
        FileUtils.deleteDirectory(existingTestSuiteFolder)
      }
      DBIO.successful(())
    }).cleanUp(error => {
      if (error.isDefined) {
        // Cleanup operations in case an error occurred.
        if (testSuiteFile.exists()) {
          FileUtils.deleteDirectory(testSuiteFile)
        }
        DBIO.failed(error.get)
      } else {
        DBIO.successful(())
      }
    })
  }

  private def completeDomainImportInternal(exportedDomain: com.gitb.xml.export.Domain, targetDomainId: Option[Long], ctx: ImportContext, canAddOrDeleteDomain: Boolean): DBIO[_] = {
    var createdDomainId: Option[Long] = None
    // Ensure that a domain cannot be created or added without appropriate access.
    if (!canAddOrDeleteDomain) {
      var domainImportItem: Option[ImportItem] = None
      if (ctx.importItemMaps.sourceMap.containsKey(ImportItemType.Domain) && ctx.importItemMaps.sourceMap(ImportItemType.Domain).nonEmpty) {
        domainImportItem = Some(ctx.importItemMaps.sourceMap(ImportItemType.Domain).head._2)
      } else if (ctx.importItemMaps.targetMap.containsKey(ImportItemType.Domain) && ctx.importItemMaps.targetMap(ImportItemType.Domain).nonEmpty) {
        domainImportItem = Some(ctx.importItemMaps.targetMap(ImportItemType.Domain).head._2)
      }
      if (domainImportItem.isDefined) {
        // The only way this wouldn't be defined is if there is no domain linked to the community, neither in the export source nor in the export target.
        if (domainImportItem.get.itemMatch != ImportItemMatch.Both) {
          // Force the deletion or creation to be skipped.
          domainImportItem.get.itemChoice = Some(ImportItemChoice.Skip)
        }
      }
    }
    // Load values pertinent to domain to ensure we are modifying items within (for security purposes).
    if (targetDomainId.isDefined) {
      exec(PersistenceSchema.domains.filter(_.id === targetDomainId.get).map(x => x.id).result).map(x => ctx.existingIds.map(ImportItemType.Domain) += x.toString)
      if (ctx.importTargets.hasSpecifications) {
        exec(PersistenceSchema.specifications
          .filter(_.domain === targetDomainId.get)
          .result
        ).map(x => {
          ctx.existingIds.map(ImportItemType.Specification) += x.id.toString
        })
        if (ctx.importTargets.hasTestSuites) {
          exportManager.loadSpecificationTestSuiteMap(targetDomainId.get).map { x =>
            x._2.foreach { testSuite =>
              ctx.existingIds.map(ImportItemType.TestSuite) += testSuite.id.toString
            }
            true
          }
        }
        if (ctx.importTargets.hasActors) {
          exportManager.loadSpecificationActorMap(targetDomainId.get).map { x =>
            x._2.foreach { actor =>
              ctx.existingIds.map(ImportItemType.Actor) += actor.id.toString
            }
            true
          }
          if (ctx.importTargets.hasEndpoints) {
            exportManager.loadActorEndpointMap(targetDomainId.get).map { x =>
              x._2.foreach { endpoint =>
                ctx.existingIds.map(ImportItemType.Endpoint) += endpoint.id.toString
              }
              true
            }
            if (ctx.importTargets.hasEndpointParameters) {
              exportManager.loadEndpointParameterMap(targetDomainId.get).map { x =>
                x._2.foreach { parameter =>
                  ctx.existingIds.map(ImportItemType.EndpointParameter) += parameter.id.toString
                }
                true
              }
            }
          }
        }
      }
      if (ctx.importTargets.hasDomainParameters) {
        exec(PersistenceSchema.domainParameters
          .filter(_.domain === targetDomainId.get)
          .result
        ).map(x => ctx.existingIds.map(ImportItemType.DomainParameter) += x.id.toString)
      }
    }
    val dbAction = for {
      _ <- {
        // Domain
        processFromArchive(ImportItemType.Domain, exportedDomain, exportedDomain.getId, ctx,
          ImportCallbacks.set(
            (data: com.gitb.xml.export.Domain, item: ImportItem) => {
              conformanceManager.createDomainInternal(models.Domain(0L, data.getShortName, data.getFullName, Option(data.getDescription)))
            },
            (data: com.gitb.xml.export.Domain, targetKey: String, item: ImportItem) => {
              conformanceManager.updateDomainInternal(targetKey.toLong, data.getShortName, data.getFullName, Option(data.getDescription))
            },
            (data: com.gitb.xml.export.Domain, targetKey: String, item: ImportItem) => {
              // Record this in case we need to do a global cleanup.
              createdDomainId = Some(targetKey.toLong)
            }
          )
        )
      }
      _ <- {
        processRemaining(ImportItemType.Domain, ctx,
          (targetKey: String) => {
            RepositoryUtils.deleteDomainTestSuiteFolder(targetKey.toLong)
            conformanceManager.deleteDomainInternal(targetKey.toLong)
          }
        )
      }
      _ <- {
        // Domain parameters
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getParameters != null) {
          exportedDomain.getParameters.getParameter.foreach { parameter =>
            dbActions += processFromArchive(ImportItemType.DomainParameter, parameter, parameter.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.DomainParameter, item: ImportItem) => {
                  conformanceManager.createDomainParameterInternal(models.DomainParameter(0L, data.getName, Option(data.getDescription), propertyTypeToKind(data.getType), decryptIfNeeded(ctx.importSettings, data.getType, Option(data.getValue)), item.parentItem.get.targetKey.get.toLong))
                },
                (data: com.gitb.xml.export.DomainParameter, targetKey: String, item: ImportItem) => {
                  conformanceManager.updateDomainParameterInternal(targetKey.toLong, data.getName, Option(data.getDescription), propertyTypeToKind(data.getType), Option(data.getValue))
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.DomainParameter, ctx,
          (targetKey: String) => {
            conformanceManager.deleteDomainParameter(targetKey.toLong)
          }
        )
      }
      _ <- {
        // Specifications
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.foreach { exportedSpecification =>
            dbActions += processFromArchive(ImportItemType.Specification, exportedSpecification, exportedSpecification.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.Specification, item: ImportItem) => {
                  conformanceManager.createSpecificationsInternal(models.Specifications(0L, data.getShortName, data.getFullName, Option(data.getDescription), data.isHidden, item.parentItem.get.targetKey.get.toLong))
                },
                (data: com.gitb.xml.export.Specification, targetKey: String, item: ImportItem) => {
                  specificationManager.updateSpecificationInternal(targetKey.toLong, data.getShortName, data.getFullName, Option(data.getDescription), data.isHidden)
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.Specification, ctx,
          (targetKey: String) => {
            conformanceManager.delete(targetKey.toLong)
          }
        )
      }
      _ <- {
        // Actors
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.foreach { exportedSpecification =>
            if (exportedSpecification.getActors != null) {
              exportedSpecification.getActors.getActor.foreach { exportedActor =>
                dbActions += processFromArchive(ImportItemType.Actor, exportedActor, exportedActor.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.Actor, item: ImportItem) => {
                      var order: Option[Short] = None
                      if (data.getOrder != null) {
                        order = Some(data.getOrder.shortValue())
                      }
                      val specificationId = item.parentItem.get.targetKey.get.toLong // Specification
                      val domainId = item.parentItem.get.parentItem.get.targetKey.get.toLong // Specification and then Domain
                      conformanceManager.createActor(models.Actors(0L, data.getActorId, data.getName, Option(data.getDescription), Some(data.isDefault), data.isHidden, order, domainId), specificationId)
                    },
                    (data: com.gitb.xml.export.Actor, targetKey: String, item: ImportItem) => {
                      // Record actor info (needed for test suite processing).
                      val specificationId = item.parentItem.get.targetKey.get.toLong
                      if (!ctx.savedSpecificationActors.containsKey(specificationId)) {
                        ctx.savedSpecificationActors += (specificationId -> mutable.Map[String, Long]())
                      }
                      ctx.savedSpecificationActors(specificationId) += (data.getActorId -> targetKey.toLong)
                      // Update actor.
                      var order: Option[Short] = None
                      if (data.getOrder != null) {
                        order = Some(data.getOrder.shortValue())
                      }
                      actorManager.updateActor(targetKey.toLong, data.getActorId, data.getName, Option(data.getDescription), Some(data.isDefault), data.isHidden, order, item.parentItem.get.targetKey.get.toLong)
                    },
                    (data: com.gitb.xml.export.Actor, targetKey: String, item: ImportItem) => {
                      // Record actor info (needed for test suite processing).
                      val specificationId = item.parentItem.get.targetKey.get.toLong
                      if (!ctx.savedSpecificationActors.containsKey(specificationId)) {
                        ctx.savedSpecificationActors += (specificationId -> mutable.Map[String, Long]())
                      }
                      ctx.savedSpecificationActors(specificationId) += (data.getActorId -> targetKey.toLong)
                    }
                  )
                )
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.Actor, ctx,
          (targetKey: String) => {
            actorManager.deleteActor(targetKey.toLong)
          }
        )
      }
      _ <- {
        // Endpoints
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.foreach { exportedSpecification =>
            if (exportedSpecification.getActors != null) {
              exportedSpecification.getActors.getActor.foreach { exportedActor =>
                if (exportedActor.getEndpoints != null) {
                  exportedActor.getEndpoints.getEndpoint.foreach { exportedEndpoint =>
                    dbActions += processFromArchive(ImportItemType.Endpoint, exportedEndpoint, exportedEndpoint.getId, ctx,
                      ImportCallbacks.set(
                        (data: com.gitb.xml.export.Endpoint, item: ImportItem) => {
                          endpointManager.createEndpoint(models.Endpoints(0L, data.getName, Option(data.getDescription), item.parentItem.get.targetKey.get.toLong))
                        },
                        (data: com.gitb.xml.export.Endpoint, targetKey: String, item: ImportItem) => {
                          endpointManager.updateEndPoint(targetKey.toLong, data.getName, Option(data.getDescription))
                        }
                      )
                    )
                  }
                }
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      - <- {
        processRemaining(ImportItemType.Endpoint, ctx,
          (targetKey: String) => {
            endpointManager.delete(targetKey.toLong)
          }
        )
      }
      _ <- {
        // Endpoint parameters
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.foreach { exportedSpecification =>
            if (exportedSpecification.getActors != null) {
              exportedSpecification.getActors.getActor.foreach { exportedActor =>
                if (exportedActor.getEndpoints != null) {
                  exportedActor.getEndpoints.getEndpoint.foreach { exportedEndpoint =>
                    if (exportedEndpoint.getParameters != null) {
                      exportedEndpoint.getParameters.getParameter.foreach { exportedParameter =>
                        dbActions += processFromArchive(ImportItemType.EndpointParameter, exportedParameter, exportedParameter.getId, ctx,
                          ImportCallbacks.set(
                            (data: com.gitb.xml.export.EndpointParameter, item: ImportItem) => {
                              parameterManager.createParameter(models.Parameters(0L, data.getName, Option(data.getDescription), requiredToUse(data.isRequired), propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests, item.parentItem.get.targetKey.get.toLong))
                            },
                            (data: com.gitb.xml.export.EndpointParameter, targetKey: String, item: ImportItem) => {
                              parameterManager.updateParameter(targetKey.toLong, data.getName, Option(data.getDescription), requiredToUse(data.isRequired), propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests)
                            }
                          )
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.EndpointParameter, ctx,
          (targetKey: String) => {
            parameterManager.delete(targetKey.toLong)
          }
        )
      }
      _ <- {
        // Test suites
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.foreach { exportedSpecification =>
            if (exportedSpecification.getTestSuites != null) {
              exportedSpecification.getTestSuites.getTestSuite.foreach { exportedTestSuite =>
                dbActions += processFromArchive(ImportItemType.TestSuite, exportedTestSuite, exportedTestSuite.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.TestSuite, item: ImportItem) => {
                      createTestSuite(data, ctx, item)
                    },
                    (data: com.gitb.xml.export.TestSuite, targetKey: String, item: ImportItem) => {
                      updateTestSuite(data, ctx, item)
                    }
                  )
                )
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.TestSuite, ctx,
          (targetKey: String) => {
            conformanceManager.undeployTestSuite(targetKey.toLong)
          }
        )
      }
    } yield ()
    dbAction.cleanUp(error => {
      if (error.isDefined) {
        // Cleanup operations in case an error occurred.
        if (createdDomainId.isDefined) {
          val domainFolder = RepositoryUtils.getDomainTestSuitesPath(createdDomainId.get)
          if (domainFolder.exists()) {
            FileUtils.deleteDirectory(domainFolder)
          }
        }
        DBIO.failed(error.get)
      } else {
        DBIO.successful(())
      }
    })
  }

  def completeCommunityImport(exportedCommunity: com.gitb.xml.export.Community, importSettings: ImportSettings, importItems: List[ImportItem], targetCommunityId: Option[Long], canAddOrDeleteDomain: Boolean, ownUserId: Option[Long]): Unit = {
    val ctx = ImportContext(
      importSettings,
      toImportItemMaps(importItems, ImportItemType.Community),
      ExistingIds.init(),
      ImportTargets.fromCommunity(exportedCommunity),
      mutable.Map[ImportItemType, mutable.Map[String, String]](),
      mutable.Map[Long, mutable.Map[String, Long]]()
    )
    // Load values pertinent to domain to ensure we are modifying items within (for security purposes).
    var targetCommunity: Option[models.Communities] = None
    var communityAdminOrganisationId: Option[Long] = None
    if (targetCommunityId.isDefined) {
      // Community
      targetCommunity = exec(PersistenceSchema.communities.filter(_.id === targetCommunityId.get).result.headOption)
      if (targetCommunity.isDefined) {
        ctx.existingIds.map(ImportItemType.Community) += targetCommunity.get.id.toString
        // Load the admin organisation ID for the community
        communityAdminOrganisationId = exec(PersistenceSchema.organizations.filter(_.community === targetCommunityId.get).filter(_.adminOrganization === true).map(x => x.id).result.headOption)
      }
      // Labels
      if (ctx.importTargets.hasCustomLabels) {
        exec(PersistenceSchema.communityLabels.filter(_.community === targetCommunityId.get).map(x => (x.community, x.labelType)).result).foreach(x => ctx.existingIds.map(ImportItemType.CustomLabel) += x._1+"_"+x._2)
      }
      // Organisation properties
      if (ctx.importTargets.hasOrganisationProperties) {
        exportManager.loadOrganisationProperties(targetCommunityId.get).foreach { x =>
          ctx.existingIds.map(ImportItemType.OrganisationProperty) += x.id.toString
        }
      }
      // System properties
      if (ctx.importTargets.hasSystemProperties) {
        exportManager.loadSystemProperties(targetCommunityId.get).foreach { x =>
          ctx.existingIds.map(ImportItemType.SystemProperty) += x.id.toString
        }
      }
      // Landing pages
      if (ctx.importTargets.hasLandingPages) {
        exec(PersistenceSchema.landingPages.filter(_.community === targetCommunityId.get).map(x => x.id).result).foreach(x => ctx.existingIds.map(ImportItemType.LandingPage) += x.toString)
      }
      // Legal notices
      if (ctx.importTargets.hasLegalNotices) {
        exec(PersistenceSchema.legalNotices.filter(_.community === targetCommunityId.get).map(x => x.id).result).foreach(x => ctx.existingIds.map(ImportItemType.LegalNotice) += x.toString)
      }
      // Error templates
      if (ctx.importTargets.hasErrorTemplates) {
        exec(PersistenceSchema.errorTemplates.filter(_.community === targetCommunityId.get).map(x => x.id).result).foreach(x => ctx.existingIds.map(ImportItemType.ErrorTemplate) += x.toString)
      }
      // Administrators
      if (ctx.importTargets.hasAdministrators) {
        exportManager.loadAdministrators(targetCommunityId.get).foreach { x =>
          ctx.existingIds.map(ImportItemType.Administrator) += x.toString
        }
      }
      // Organisations
      if (ctx.importTargets.hasOrganisations) {
        exec(PersistenceSchema.organizations.filter(_.community === targetCommunityId.get).map(x => x.id).result).foreach { x =>
          ctx.existingIds.map(ImportItemType.Organisation) += x.toString
        }
        // Organisation users
        if (ctx.importTargets.hasOrganisationUsers) {
          exportManager.loadOrganisationUserMap(targetCommunityId.get).map { x =>
            x._2.foreach { user =>
              ctx.existingIds.map(ImportItemType.OrganisationUser) +=  user.id.toString
            }
            true
          }
        }
        // Organisation property values
        if (ctx.importTargets.hasOrganisationPropertyValues) {
          exportManager.loadOrganisationParameterValueMap(targetCommunityId.get).map { x =>
            x._2.foreach { value =>
              ctx.existingIds.map(ImportItemType.OrganisationPropertyValue) +=  value.organisation+"_"+value.parameter
            }
            true
          }
        }
        // Systems
        if (ctx.importTargets.hasSystems) {
          exportManager.loadOrganisationSystemMap(targetCommunityId.get).map { x =>
            x._2.foreach { system =>
              ctx.existingIds.map(ImportItemType.System) +=  system.id.toString
            }
            true
          }
          // System property values
          if (ctx.importTargets.hasSystemPropertyValues) {
            exportManager.loadSystemParameterValues(targetCommunityId.get).map { x =>
              x._2.foreach { value =>
                ctx.existingIds.map(ImportItemType.SystemPropertyValue) +=  value.system+"_"+value.parameter
              }
              true
            }
          }
          // Statements
          if (ctx.importTargets.hasStatements && targetCommunity.isDefined) {
            exportManager.loadSystemStatementsMap(targetCommunity.get.id, targetCommunity.get.domain).map { x =>
              x._2.foreach { statement =>
                ctx.existingIds.map(ImportItemType.Statement) +=  x._1 +"_"+statement._2.id // [System ID]_[Actor ID]
              }
              true
            }
            // Statement configurations
            if (ctx.importTargets.hasStatementConfigurations) {
              exportManager.loadSystemConfigurationsMap(targetCommunity.get).map { x =>
                ctx.existingIds.map(ImportItemType.StatementConfiguration) += x._1 // [Actor ID]_[System ID]_[Parameter ID]
              }
            }
          }
        }
      }
    }
    val dbAction = for {
      // Domain
      _ <- {
        if (exportedCommunity.getDomain != null) {
          var targetDomainId: Option[Long] = None
          if (targetCommunity.isDefined && targetCommunity.get.domain.isDefined) {
            targetDomainId = targetCommunity.get.domain
          }
          mergeImportItemMaps(ctx.importItemMaps, toImportItemMaps(importItems, ImportItemType.Domain))
          completeDomainImportInternal(exportedCommunity.getDomain, targetDomainId, ctx, canAddOrDeleteDomain)
        } else {
          DBIO.successful(())
        }
      }
      // Community
      _ <- {
        processFromArchive(ImportItemType.Community, exportedCommunity, exportedCommunity.getId, ctx,
          ImportCallbacks.set(
            (data: com.gitb.xml.export.Community, item: ImportItem) => {
              var domainId: Option[Long] = None
              if (exportedCommunity.getDomain != null && ctx.processedIdMap.containsKey(ImportItemType.Domain)) {
                val processedDomainId = ctx.processedIdMap(ImportItemType.Domain).get(exportedCommunity.getDomain.getId)
                if (processedDomainId.isDefined) {
                  domainId = Some(processedDomainId.get.toLong)
                }
              }
              // This returns a tuple: (community ID, admin organisation ID)
              communityManager.createCommunityInternal(models.Communities(0L, data.getShortName, data.getFullName, Option(data.getSupportEmail),
                selfRegistrationMethodToModel(data.getSelfRegistrationSettings.getMethod), Option(data.getSelfRegistrationSettings.getToken),
                data.getSelfRegistrationSettings.isNotifications, Option(data.getDescription), selfRegistrationRestrictionToModel(data.getSelfRegistrationSettings.getRestriction),
                domainId
              ))
            },
            (data: com.gitb.xml.export.Community, targetKey: String, item: ImportItem) => {
              var domainId: Option[Long] = None
              if (exportedCommunity.getDomain != null) {
                if (ctx.processedIdMap.containsKey(ImportItemType.Domain)) {
                  val processedDomainId = ctx.processedIdMap(ImportItemType.Domain).get(exportedCommunity.getDomain.getId)
                  if (processedDomainId.isDefined) {
                    domainId = Some(processedDomainId.get.toLong)
                  }
                }
              } else {
                // The community may already have a domain defined.
                domainId = targetCommunity.get.domain
              }
              communityManager.updateCommunityInternal(targetCommunity.get, data.getShortName, data.getFullName, Option(data.getSupportEmail),
                selfRegistrationMethodToModel(data.getSelfRegistrationSettings.getMethod), Option(data.getSelfRegistrationSettings.getToken), data.getSelfRegistrationSettings.isNotifications,
                Option(data.getDescription), selfRegistrationRestrictionToModel(data.getSelfRegistrationSettings.getRestriction), domainId
              )
            },
            None,
            (data: com.gitb.xml.export.Community, targetKey: String, newId: Any, item: ImportItem) => {
              val ids: (Long, Long) = newId.asInstanceOf[(Long, Long)] // (community ID, admin organisation ID)
              // Record community ID.
              addIdToProcessedIdMap(ImportItemType.Community, data.getId, ids._1.toString, ctx)
              // Record admin organisation ID.
              communityAdminOrganisationId = Some(ids._2)
            }
          )
        )
      }
      // Certificate settings
      _ <- {
        val communityId = getProcessedDbId(exportedCommunity, ImportItemType.Community, ctx)
        if (communityId.isDefined) {
          if (exportedCommunity.getConformanceCertificateSettings == null) {
            // Delete
            conformanceManager.deleteConformanceCertificateSettings(communityId.get.toLong)
          } else {
            // Update/Add
            var keystoreFile: Option[String] = None
            var keystoreType: Option[String] = None
            var keystorePassword: Option[String] = None
            var keyPassword: Option[String] = None
            if (exportedCommunity.getConformanceCertificateSettings.getSignature != null) {
              keystoreFile = Option(exportedCommunity.getConformanceCertificateSettings.getSignature.getKeystore)
              if (exportedCommunity.getConformanceCertificateSettings.getSignature.getKeystoreType != null) {
                keystoreType = Some(exportedCommunity.getConformanceCertificateSettings.getSignature.getKeystoreType.value())
              }
              keystorePassword = Option(exportedCommunity.getConformanceCertificateSettings.getSignature.getKeystorePassword)
              keyPassword = Option(exportedCommunity.getConformanceCertificateSettings.getSignature.getKeyPassword)
            }
            conformanceManager.updateConformanceCertificateSettingsInternal(
                models.ConformanceCertificates(
                  0L, Option(exportedCommunity.getConformanceCertificateSettings.getTitle), Option(exportedCommunity.getConformanceCertificateSettings.getMessage),
                  exportedCommunity.getConformanceCertificateSettings.isAddMessage, exportedCommunity.getConformanceCertificateSettings.isAddResultOverview,
                  exportedCommunity.getConformanceCertificateSettings.isAddTestCases, exportedCommunity.getConformanceCertificateSettings.isAddDetails,
                  exportedCommunity.getConformanceCertificateSettings.isAddSignature, keystoreFile, keystoreType, keystorePassword, keyPassword,
                  communityId.get.toLong
                )
              , true, false
            )
          }
        } else {
          DBIO.successful(())
        }
      }
      // Custom labels
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getCustomLabels != null) {
          exportedCommunity.getCustomLabels.getLabel.foreach { exportedLabel =>
            dbActions += processFromArchive(ImportItemType.CustomLabel, exportedLabel, exportedLabel.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.CustomLabel, item: ImportItem) => {
                  val communityId = item.parentItem.get.targetKey.get.toLong
                  val labelObject = toModelCustomLabel(data, communityId)
                  communityManager.createCommunityLabel(labelObject) andThen
                  DBIO.successful(communityId+"_"+labelObject.labelType)
                },
                (data: com.gitb.xml.export.CustomLabel, targetKey: String, item: ImportItem) => {
                  val keyParts = StringUtils.split(targetKey, "_") // [community_id]_[label_type]
                  communityManager.deleteCommunityLabel(keyParts(0).toLong, keyParts(1).toShort) andThen
                    communityManager.createCommunityLabel(toModelCustomLabel(data, item.parentItem.get.targetKey.get.toLong))
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.CustomLabel, ctx,
          (targetKey: String) => {
            val keyParts = StringUtils.split(targetKey, "_") // [community_id]_[label_type]
            communityManager.deleteCommunityLabel(keyParts(0).toLong, keyParts(1).toShort)
          }
        )
      }
      // Organisation properties
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisationProperties != null) {
          exportedCommunity.getOrganisationProperties.getProperty.foreach { exportedProperty =>
            dbActions += processFromArchive(ImportItemType.OrganisationProperty, exportedProperty, exportedProperty.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.OrganisationProperty, item: ImportItem) => {
                  communityManager.createOrganisationParameterInternal(toModelOrganisationParameter(data, item.parentItem.get.targetKey.get.toLong, None))
                },
                (data: com.gitb.xml.export.OrganisationProperty, targetKey: String, item: ImportItem) => {
                  communityManager.updateOrganisationParameterInternal(toModelOrganisationParameter(data, item.parentItem.get.targetKey.get.toLong, Some(targetKey.toLong)))
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.OrganisationProperty, ctx,
          (targetKey: String) => {
            communityManager.deleteOrganisationParameter(targetKey.toLong)
          }
        )
      }
      // System properties
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getSystemProperties != null) {
          exportedCommunity.getSystemProperties.getProperty.foreach { exportedProperty =>
            dbActions += processFromArchive(ImportItemType.SystemProperty, exportedProperty, exportedProperty.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.SystemProperty, item: ImportItem) => {
                  communityManager.createSystemParameterInternal(toModelSystemParameter(data, item.parentItem.get.targetKey.get.toLong, None))
                },
                (data: com.gitb.xml.export.SystemProperty, targetKey: String, item: ImportItem) => {
                  communityManager.updateSystemParameterInternal(toModelSystemParameter(data, item.parentItem.get.targetKey.get.toLong, Some(targetKey.toLong)))
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.SystemProperty, ctx,
          (targetKey: String) => {
            communityManager.deleteSystemParameter(targetKey.toLong)
          }
        )
      }
      // Landing pages
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getLandingPages != null) {
          exportedCommunity.getLandingPages.getLandingPage.foreach { exportedContent =>
            dbActions += processFromArchive(ImportItemType.LandingPage, exportedContent, exportedContent.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.LandingPage, item: ImportItem) => {
                  landingPageManager.createLandingPageInternal(toModelLandingPage(data, item.parentItem.get.targetKey.get.toLong))
                },
                (data: com.gitb.xml.export.LandingPage, targetKey: String, item: ImportItem) => {
                  landingPageManager.updateLandingPageInternal(targetKey.toLong, data.getName, Option(data.getDescription), data.getContent, data.isDefault, item.parentItem.get.targetKey.get.toLong)
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.LandingPage, ctx,
          (targetKey: String) => {
            landingPageManager.deleteLandingPageInternal(targetKey.toLong)
          }
        )
      }
      // Legal notices
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getLegalNotices != null) {
          exportedCommunity.getLegalNotices.getLegalNotice.foreach { exportedContent =>
            dbActions += processFromArchive(ImportItemType.LegalNotice, exportedContent, exportedContent.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.LegalNotice, item: ImportItem) => {
                  legalNoticeManager.createLegalNoticeInternal(toModelLegalNotice(data, item.parentItem.get.targetKey.get.toLong))
                },
                (data: com.gitb.xml.export.LegalNotice, targetKey: String, item: ImportItem) => {
                  legalNoticeManager.updateLegalNoticeInternal(targetKey.toLong, data.getName, Option(data.getDescription), data.getContent, data.isDefault, item.parentItem.get.targetKey.get.toLong)
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.LegalNotice, ctx,
          (targetKey: String) => {
            legalNoticeManager.deleteLegalNoticeInternal(targetKey.toLong)
          }
        )
      }
      // Error templates
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getErrorTemplates != null) {
          exportedCommunity.getErrorTemplates.getErrorTemplate.foreach { exportedContent =>
            dbActions += processFromArchive(ImportItemType.ErrorTemplate, exportedContent, exportedContent.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.ErrorTemplate, item: ImportItem) => {
                  errorTemplateManager.createErrorTemplateInternal(toModelErrorTemplate(data, item.parentItem.get.targetKey.get.toLong))
                },
                (data: com.gitb.xml.export.ErrorTemplate, targetKey: String, item: ImportItem) => {
                  errorTemplateManager.updateErrorTemplateInternal(targetKey.toLong, data.getName, Option(data.getDescription), data.getContent, data.isDefault, item.parentItem.get.targetKey.get.toLong)
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.ErrorTemplate, ctx,
          (targetKey: String) => {
            errorTemplateManager.deleteErrorTemplateInternal(targetKey.toLong)
          }
        )
      }
      // Administrators
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getAdministrators != null) {
          exportedCommunity.getAdministrators.getAdministrator.foreach { exportedUser =>
            dbActions += processFromArchive(ImportItemType.Administrator, exportedUser, exportedUser.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.CommunityAdministrator, item: ImportItem) => {
                  PersistenceSchema.insertUser += toModelAdministrator(data, None, communityAdminOrganisationId.get, ctx.importSettings)
                },
                (data: com.gitb.xml.export.CommunityAdministrator, targetKey: String, item: ImportItem) => {
                  val query = for {
                    user <- PersistenceSchema.users.filter(_.id === targetKey.toLong)
                  } yield (user.email, user.name, user.password, user.onetimePassword)
                  query.update(data.getEmail, data.getName, decrypt(ctx.importSettings, data.getPassword), data.isOnetimePassword)
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.Administrator, ctx,
          (targetKey: String) => {
            val userId = targetKey.toLong
            if (ownUserId.isDefined && ownUserId.get.longValue() != userId) {
              // Avoid deleting self
              PersistenceSchema.users.filter(_.id === userId).delete
            } else {
              DBIO.successful(())
            }
          }
        )
      }
      // Organisations
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            dbActions += processFromArchive(ImportItemType.Organisation, exportedOrganisation, exportedOrganisation.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.Organisation, item: ImportItem) => {
                  organisationManager.createOrganizationInTrans(
                    models.Organizations(
                      0L, data.getShortName, data.getFullName, OrganizationType.Vendor.id.toShort, adminOrganization = false,
                      getProcessedDbId(data.getLandingPage, ImportItemType.LandingPage, ctx),
                      getProcessedDbId(data.getLegalNotice, ImportItemType.LegalNotice, ctx),
                      getProcessedDbId(data.getErrorTemplate, ImportItemType.ErrorTemplate, ctx),
                      template = data.isTemplate, Option(data.getTemplateName), item.parentItem.get.targetKey.get.toLong
                    ), None, None, copyOrganisationParameters = false, copySystemParameters = false, copyStatementParameters = false
                  )
                },
                (data: com.gitb.xml.export.Organisation, targetKey: String, item: ImportItem) => {
                  if (communityAdminOrganisationId.get.longValue() == targetKey.toLong.longValue()) {
                    // Prevent updating the community's admin organisation.
                    DBIO.successful(())
                  } else {
                    organisationManager.updateOrganizationInternal(targetKey.toLong, data.getShortName, data.getFullName,
                      getProcessedDbId(data.getLandingPage, ImportItemType.LandingPage, ctx),
                      getProcessedDbId(data.getLegalNotice, ImportItemType.LegalNotice, ctx),
                      getProcessedDbId(data.getErrorTemplate, ImportItemType.ErrorTemplate, ctx),
                      None, data.isTemplate, Option(data.getTemplateName), None, copyOrganisationParameters = false, copySystemParameters = false, copyStatementParameters = false
                    )
                  }
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.Organisation, ctx,
          (targetKey: String) => {
            if (communityAdminOrganisationId.get.longValue() == targetKey.toLong.longValue()) {
              // Prevent deleting the community's admin organisation.
              DBIO.successful(())
            } else {
              organisationManager.deleteOrganization(targetKey.toLong)
            }
          }
        )
      }
      // Organisation users
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            if (exportedOrganisation.getUsers != null) {
              exportedOrganisation.getUsers.getUser.foreach { exportedUser =>
                dbActions += processFromArchive(ImportItemType.OrganisationUser, exportedUser, exportedUser.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.OrganisationUser, item: ImportItem) => {
                      PersistenceSchema.insertUser += toModelOrganisationUser(data, None, toModelUserRole(data.getRole), item.parentItem.get.targetKey.get.toLong, ctx.importSettings)
                    },
                    (data: com.gitb.xml.export.OrganisationUser, targetKey: String, item: ImportItem) => {
                      val q = for { u <- PersistenceSchema.users.filter(_.id === targetKey.toLong) } yield (u.name, u.email, u.password, u.onetimePassword, u.role)
                      q.update(data.getName, data.getEmail, decrypt(ctx.importSettings, data.getPassword), data.isOnetimePassword, toModelUserRole(data.getRole))
                    }
                  )
                )
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.OrganisationUser, ctx,
          (targetKey: String) => {
            PersistenceSchema.users.filter(_.id === targetKey.toLong).delete
          }
        )
      }
      // Organisation property values
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            if (exportedOrganisation.getPropertyValues != null) {
              exportedOrganisation.getPropertyValues.getProperty.foreach { exportedValue =>
                dbActions += processFromArchive(ImportItemType.OrganisationPropertyValue, exportedValue, exportedValue.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.OrganisationPropertyValue, item: ImportItem) => {
                      val relatedPropertyId = getProcessedDbId(data.getProperty, ImportItemType.OrganisationProperty, ctx)
                      if (relatedPropertyId.isDefined) {
                        val organisationId = item.parentItem.get.targetKey.get.toLong
                        // The property this value related to has either been updated or inserted.
                        (PersistenceSchema.organisationParameterValues += models.OrganisationParameterValues(organisationId, relatedPropertyId.get, decryptIfNeeded(ctx.importSettings, data.getProperty.getType, Option(data.getValue)).get)) andThen
                        DBIO.successful(organisationId+"_"+relatedPropertyId.get)
                      } else {
                        DBIO.successful(())
                      }
                    },
                    (data: com.gitb.xml.export.OrganisationPropertyValue, targetKey: String, item: ImportItem) => {
                      val keyParts = StringUtils.split(targetKey, "_") // target key: [organisation ID]_[property ID]
                      val organisationId = keyParts(0).toLong
                      val propertyId = keyParts(1).toLong
                      if (getProcessedDbId(data.getProperty, ImportItemType.OrganisationProperty, ctx).isDefined) {
                        val q = for { p <- PersistenceSchema.organisationParameterValues.filter(_.organisation === organisationId).filter(_.parameter === propertyId) } yield p.value
                        q.update(decryptIfNeeded(ctx.importSettings, data.getProperty.getType, Some(data.getValue)).get)
                      } else {
                        DBIO.successful(())
                      }
                    }
                  )
                )
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.OrganisationPropertyValue, ctx,
          (targetKey: String) => {
            val keyParts = StringUtils.split(targetKey, "_") // target key: [organisation ID]_[property ID]
            val organisationId = keyParts(0).toLong
            val propertyId = keyParts(1).toLong
            PersistenceSchema.organisationParameterValues.filter(_.organisation === organisationId).filter(_.parameter === propertyId).delete
          }
        )
      }
      // Systems
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            if (exportedOrganisation.getSystems != null) {
              exportedOrganisation.getSystems.getSystem.foreach { exportedSystem =>
                dbActions += processFromArchive(ImportItemType.System, exportedSystem, exportedSystem.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.System, item: ImportItem) => {
                      PersistenceSchema.insertSystem += models.Systems(0L, data.getShortName, data.getFullName, Option(data.getDescription), data.getVersion, item.parentItem.get.targetKey.get.toLong)
                    },
                    (data: com.gitb.xml.export.System, targetKey: String, item: ImportItem) => {
                      var descriptionToSet = ""
                      if (data.getDescription != null) {
                        descriptionToSet = data.getDescription
                      }
                      systemManager.updateSystemProfileInternal(None, targetCommunityId, item.targetKey.get.toLong, Some(data.getShortName), Some(data.getFullName), Some(descriptionToSet), Some(data.getVersion),
                        None, None, copySystemParameters = false, copyStatementParameters = false
                      )
                    }
                  )
                )
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.System, ctx,
          (targetKey: String) => {
            systemManager.deleteSystem(targetKey.toLong)
          }
        )
      }
      // System property values
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            if (exportedOrganisation.getSystems != null) {
              exportedOrganisation.getSystems.getSystem.foreach { exportedSystem =>
                if (exportedSystem.getPropertyValues != null) {
                  exportedSystem.getPropertyValues.getProperty.foreach { exportedValue =>
                    dbActions += processFromArchive(ImportItemType.SystemPropertyValue, exportedValue, exportedValue.getId, ctx,
                      ImportCallbacks.set(
                        (data: com.gitb.xml.export.SystemPropertyValue, item: ImportItem) => {
                          val relatedPropertyId = getProcessedDbId(data.getProperty, ImportItemType.SystemProperty, ctx)
                          if (relatedPropertyId.isDefined) {
                            val systemId = item.parentItem.get.targetKey.get.toLong
                            // The property this value related to has either been updated or inserted.
                            (PersistenceSchema.systemParameterValues += models.SystemParameterValues(systemId, relatedPropertyId.get, decryptIfNeeded(ctx.importSettings, data.getProperty.getType, Option(data.getValue)).get)) andThen
                              DBIO.successful(systemId+"_"+relatedPropertyId.get)
                          } else {
                            DBIO.successful(())
                          }
                        },
                        (data: com.gitb.xml.export.SystemPropertyValue, targetKey: String, item: ImportItem) => {
                          val keyParts = StringUtils.split(targetKey, "_") // target key: [system ID]_[property ID]
                          val systemId = keyParts(0).toLong
                          val propertyId = keyParts(1).toLong
                          if (getProcessedDbId(data.getProperty, ImportItemType.SystemProperty, ctx).isDefined) {
                            val q = for { p <- PersistenceSchema.systemParameterValues.filter(_.system === systemId).filter(_.parameter === propertyId) } yield p.value
                            q.update(decryptIfNeeded(ctx.importSettings, data.getProperty.getType, Some(data.getValue)).get)
                          } else {
                            DBIO.successful(())
                          }
                        }
                      )
                    )
                  }
                }
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.SystemPropertyValue, ctx,
          (targetKey: String) => {
            val keyParts = StringUtils.split(targetKey, "_") // target key: [system ID]_[property ID]
            val systemId = keyParts(0).toLong
            val propertyId = keyParts(1).toLong
            PersistenceSchema.systemParameterValues.filter(_.system === systemId).filter(_.parameter === propertyId).delete
          }
        )
      }
      // Statements
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            if (exportedOrganisation.getSystems != null) {
              exportedOrganisation.getSystems.getSystem.foreach { exportedSystem =>
                if (exportedSystem.getStatements != null) {
                  exportedSystem.getStatements.getStatement.foreach { exportedStatement =>
                    dbActions += processFromArchive(ImportItemType.Statement, exportedStatement, exportedStatement.getId, ctx,
                      ImportCallbacks.set(
                        (data: com.gitb.xml.export.ConformanceStatement, item: ImportItem) => {
                          val relatedActorId = getProcessedDbId(data.getActor, ImportItemType.Actor, ctx)
                          var relatedSpecId: Option[Long] = None
                          if (data.getActor != null) {
                            relatedSpecId = getProcessedDbId(data.getActor.getSpecification, ImportItemType.Specification, ctx)
                          }
                          if (relatedActorId.isDefined && relatedSpecId.isDefined) {
                            val systemId = item.parentItem.get.targetKey.get.toLong
                            systemManager.defineConformanceStatement(systemId, relatedSpecId.get, relatedActorId.get, None)
                            DBIO.successful(systemId+"_"+relatedActorId.get)
                          } else {
                            DBIO.successful(())
                          }
                        },
                        (data: com.gitb.xml.export.ConformanceStatement, targetKey: String, item: ImportItem) => {
                          // Nothing to update.
                          DBIO.successful(())
                        }
                      )
                    )
                  }
                }
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.Statement, ctx,
          (targetKey: String) => {
            // Key: [System ID]_[actor ID]
            val keyParts = StringUtils.split(targetKey, "_")
            val systemId = keyParts(0).toLong
            val actorId = keyParts(1).toLong
            systemManager.deleteConformanceStatments(systemId, List(actorId))
          }
        )
      }
      // Statement configurations
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            if (exportedOrganisation.getSystems != null) {
              exportedOrganisation.getSystems.getSystem.foreach { exportedSystem =>
                if (exportedSystem.getStatements != null) {
                  exportedSystem.getStatements.getStatement.foreach { exportedStatement =>
                    if (exportedStatement.getConfigurations != null) {
                      exportedStatement.getConfigurations.getConfiguration.foreach { exportedValue =>
                        dbActions += processFromArchive(ImportItemType.StatementConfiguration, exportedValue, exportedValue.getId, ctx,
                          ImportCallbacks.set(
                            (data: com.gitb.xml.export.Configuration, item: ImportItem) => {
                              val relatedParameterId = getProcessedDbId(data.getParameter, ImportItemType.EndpointParameter, ctx)
                              var relatedEndpointId: Option[Long] = None
                              if (data.getParameter != null) {
                                relatedEndpointId = getProcessedDbId(data.getParameter.getEndpoint, ImportItemType.Endpoint, ctx)
                              }
                              if (relatedParameterId.isDefined && relatedEndpointId.isDefined) {
                                val relatedSystemId = item.parentItem.get.parentItem.get.targetKey.get.toLong // Statement -> System
                                systemManager.saveEndpointConfigurationInternal(forceAdd = true, forceUpdate = false,
                                  models.Configs(relatedSystemId, relatedParameterId.get, relatedEndpointId.get, decryptIfNeeded(ctx.importSettings, data.getParameter.getType, Option(data.getValue)).get))
                                DBIO.successful(relatedEndpointId.get+"_"+relatedSystemId+"_"+relatedParameterId.get)
                              } else {
                                DBIO.successful(())
                              }
                            },
                            (data: com.gitb.xml.export.Configuration, targetKey: String, item: ImportItem) => {
                              val relatedParameterId = getProcessedDbId(data.getParameter, ImportItemType.EndpointParameter, ctx)
                              var relatedEndpointId: Option[Long] = None
                              if (data.getParameter != null) {
                                relatedEndpointId = getProcessedDbId(data.getParameter.getEndpoint, ImportItemType.Endpoint, ctx)
                              }
                              if (relatedParameterId.isDefined && relatedEndpointId.isDefined) {
                                val relatedSystemId = item.parentItem.get.parentItem.get.targetKey.get.toLong // Statement -> System
                                systemManager.saveEndpointConfigurationInternal(forceAdd = false, forceUpdate = true,
                                  models.Configs(relatedSystemId, relatedParameterId.get, relatedEndpointId.get, decryptIfNeeded(ctx.importSettings, data.getParameter.getType, Option(data.getValue)).get))
                                DBIO.successful(relatedEndpointId.get+"_"+relatedSystemId+"_"+relatedParameterId.get)
                              } else {
                                DBIO.successful(())
                              }
                            }
                          )
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.StatementConfiguration, ctx,
          (targetKey: String) => {
            // Key: [Endpoint ID]_[System ID]_[Endpoint parameter ID]
            val keyParts = StringUtils.split(targetKey, "_")
            val endpointId = keyParts(0).toLong
            val systemId = keyParts(1).toLong
            val parameterId = keyParts(2).toLong
            systemManager.deleteEndpointConfigurationInternal(systemId, parameterId, endpointId)
          }
        )
      }
    } yield ()
    exec(dbAction.transactionally)
  }

}
