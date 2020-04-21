package managers.export

import java.io.File

import javax.inject.{Inject, Singleton}
import managers._
import models.Enums.LabelType.LabelType
import models.Enums.{ImportItemMatch, ImportItemType, LabelType}
import models._
import org.apache.commons.lang3.StringUtils
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

@Singleton
class ImportPreviewManager @Inject()(exportManager: ExportManager, communityManager: CommunityManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

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

    var actorXmlIdToImportItemMap = mutable.Map[String, ImportItem]()

    var importItemDomain: ImportItem = null
    if (targetDomain.isDefined) {
      importItemDomain = new ImportItem(Some(targetDomain.get.fullname), ImportItemType.Domain, ImportItemMatch.Both, Some(targetDomain.get.id.toString), Some(exportedDomain.getId))
      // Load data.
      exec(PersistenceSchema.specifications
        .filter(_.domain === targetDomain.get.id)
        .result
      ).map(x => {
        targetSpecificationMap += (x.shortname -> x)
        targetSpecificationIdMap += (x.id -> x)
      })
      if (targetSpecificationMap.nonEmpty) {
        exportManager.loadSpecificationTestSuiteMap(targetDomain.get.id).map { x =>
          targetSpecificationTestSuiteMap += (x._1 -> listBufferToNameMap(x._2, { t => t.shortname }))
        }
        exportManager.loadSpecificationActorMap(targetDomain.get.id).map { x =>
          targetSpecificationActorMap += (x._1 -> listBufferToNameMap(x._2, { a => a.actorId }))
          x._2.foreach { a =>
            referenceActorToSpecificationMap += (a.id -> targetSpecificationIdMap(x._1))
          }
          true
        }
        if (targetSpecificationActorMap.nonEmpty) {
          exportManager.loadActorEndpointMap(targetDomain.get.id).map { x =>
            targetActorEndpointMap += (x._1 -> listBufferToNameMap(x._2, { e => e.name }))
            referenceActorEndpointMap += (x._1 -> listBufferToNameMap(x._2, { e => e.name }))
          }
          if (targetActorEndpointMap.nonEmpty) {
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
      exec(PersistenceSchema.domainParameters
        .filter(_.domain === targetDomain.get.id)
        .result
      ).map(x => targetDomainParametersMap += (x.name -> x))
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
            actorXmlIdToImportItemMap += (exportedActor.getId -> importItemActor)
            // Endpoints
            if (exportedActor.getEndpoints != null) {
              exportedActor.getEndpoints.getEndpoint.foreach { exportedEndpoint =>
                var targetEndpoint: Option[models.Endpoints] = None
                var importItemEndpoint: ImportItem = null
                if (targetActor.isDefined && targetActorEndpointMap.contains(targetActor.get.id)) {
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
    (DomainImportInfo(referenceActorEndpointMap, referenceEndpointParameterMap, referenceActorToSpecificationMap, referenceEndpointParameterIdMap, actorXmlIdToImportItemMap), importItemDomain)
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
    if (targetCommunity.isDefined) {
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
    var targetStatementMap = mutable.Map[Long, mutable.Map[Long, (models.Specifications, models.Actors)]]() // System to [actor_DB_ID] to (specification, actor)]    WAS: System to [actor name to (specification name, actor)]
    var targetStatementConfigurationMap = mutable.Map[String, mutable.Map[String, models.Configs]]() // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID] to Parameter name to Configs
    var importItemMapOrganisation = mutable.Map[String, ImportItem]()
    var importItemMapSystem = mutable.Map[String, ImportItem]()
    var importItemMapStatement = mutable.Map[String, ImportItem]()
    if (targetCommunity.isDefined) {
      importItemCommunity = new ImportItem(Some(targetCommunity.get.fullname), ImportItemType.Community, ImportItemMatch.Both, Some(targetCommunity.get.id.toString), Some(exportedCommunity.getId))
      // Load data.
      // Administrators.
      exportManager.loadAdministrators(targetCommunity.get.id).map { x =>
        targetAdministratorsMap += (x.email -> x)
      }
      // Organisation properties.
      exportManager.loadOrganisationProperties(targetCommunity.get.id).map { x =>
        targetOrganisationPropertyMap += (x.testKey -> x)
        targetOrganisationPropertyIdMap += (x.id -> x)
      }
      // System properties.
      exportManager.loadSystemProperties(targetCommunity.get.id).map { x =>
        targetSystemPropertyMap += (x.testKey -> x)
        targetSystemPropertyIdMap += (x.id -> x)
      }
      // Custom labels.
      communityManager.getCommunityLabels(targetCommunity.get.id).foreach { x =>
        targetCustomLabelMap += (LabelType.apply(x.labelType) -> x)
      }
      // Landing pages.
      exec(PersistenceSchema.landingPages.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
        if (!targetLandingPageMap.containsKey(x._1)) {
          targetLandingPageMap += (x._1 -> new ListBuffer[Long]())
        }
        targetLandingPageMap(x._1) += x._2
      }
      // Legal notices.
      exec(PersistenceSchema.legalNotices.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
        if (!targetLegalNoticeMap.containsKey(x._1)) {
          targetLegalNoticeMap += (x._1 -> new ListBuffer[Long]())
        }
        targetLegalNoticeMap(x._1) += x._2
      }
      // Error templates.
      exec(PersistenceSchema.errorTemplates.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
        if (!targetErrorTemplateMap.containsKey(x._1)) {
          targetErrorTemplateMap += (x._1 -> new ListBuffer[Long]())
        }
        targetErrorTemplateMap(x._1) += x._2
      }
      // Organisations.
      exportManager.loadOrganisations(targetCommunity.get.id).foreach { x =>
        if (!targetOrganisationMap.containsKey(x.shortname)) {
          targetOrganisationMap += (x.shortname -> new ListBuffer[Organizations]())
        }
        targetOrganisationMap(x.shortname) += x
      }
      if (targetOrganisationMap.nonEmpty) {
        // Users.
        exportManager.loadOrganisationUserMap(targetCommunity.get.id).map { x =>
          targetOrganisationUserMap += (x._1 -> listBufferToNameMap(x._2, { u => u.email }))
        }
        if (targetOrganisationPropertyMap.nonEmpty) {
          // Organisation property values.
          exportManager.loadOrganisationParameterValueMap(targetCommunity.get.id).map { x =>
            targetOrganisationPropertyValueMap += (x._1 -> listBufferToNameMap(x._2, { v => targetOrganisationPropertyIdMap(v.parameter).testKey }))
          }
        }
        // Systems.
        exportManager.loadOrganisationSystemMap(targetCommunity.get.id).map { x =>
          targetSystemMap += (x._1 -> listBufferToNonUniqueNameMap(x._2, { s => s.shortname }))
          x._2.foreach { system =>
            targetSystemIdMap += (system.id -> system)
          }
          true
        }
        if (targetSystemMap.nonEmpty) {
          if (targetSystemPropertyMap.nonEmpty) {
            // System property values.
            exportManager.loadSystemParameterValues(targetCommunity.get.id).map { x =>
              targetSystemPropertyValueMap += (x._1 -> listBufferToNameMap(x._2, { v => targetSystemPropertyIdMap(v.parameter).testKey }))
            }
          }
          if (domainImportInfo.targetActorToSpecificationMap.nonEmpty) {
            // Statements.
            exportManager.loadSystemStatementsMap(targetCommunity.get.id, targetCommunity.get.domain).map { x =>
              var statements = targetStatementMap.get(x._1)
              if (statements.isEmpty) {
                statements = Some(mutable.Map[Long, (models.Specifications, models.Actors)]())
                targetStatementMap += (x._1 -> statements.get)
              }
              x._2.foreach { y =>
                statements.get += (y._2.id -> y)
              }
              true
            }
            // Statement configurations.
            if (domainImportInfo.targetEndpointParameterIdMap.nonEmpty) {
              exportManager.loadSystemConfigurationsMap(targetCommunity.get).map { x =>
                // [actor ID]_[endpoint ID]_[System ID]_[Parameter ID]
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
          case _ => throw new IllegalArgumentException("Unknown enum value [" + exportedLabel.getLabelType + "]")
        }
        if (targetCommunity.isDefined) {
          targetLabel = targetCustomLabelMap.remove(labelType)
        }
        if (targetLabel.isDefined) {
          new ImportItem(Some(targetLabel.get.labelType.toString), ImportItemType.CustomLabel, ImportItemMatch.Both, Some(targetCommunity.get.id + "_" + targetLabel.get.labelType.toString), Some(exportedLabel.getId), importItemCommunity)
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
            if (targetOrganisation.isDefined && targetOrganisationUserMap.containsKey(targetOrganisation.get.id)) {
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
            if (targetOrganisation.isDefined && targetOrganisationPropertyValueMap.containsKey(targetOrganisation.get.id)) {
              targetPropertyValue = targetOrganisationPropertyValueMap(targetOrganisation.get.id).remove(exportedProperty.getProperty.getName)
            }
            if (targetPropertyValue.isDefined) {
              new ImportItem(Some(targetOrganisationPropertyIdMap(targetPropertyValue.get.parameter).name), ImportItemType.OrganisationPropertyValue, ImportItemMatch.Both, Some(targetPropertyValue.get.organisation.toString + "_" + targetPropertyValue.get.parameter.toString), Some(exportedProperty.getId), importItemOrganisation)
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
            if (targetOrganisation.isDefined && targetSystemMap.containsKey(targetOrganisation.get.id)) {
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
                if (targetSystem.isDefined && targetSystemPropertyValueMap.containsKey(targetSystem.get.id)) {
                  targetPropertyValue = targetSystemPropertyValueMap(targetSystem.get.id).remove(exportedValue.getProperty.getName)
                }
                if (targetPropertyValue.isDefined) {
                  new ImportItem(Some(targetSystemPropertyIdMap(targetPropertyValue.get.parameter).name), ImportItemType.SystemPropertyValue, ImportItemMatch.Both, Some(targetPropertyValue.get.system.toString + "_" + targetPropertyValue.get.parameter.toString), Some(exportedValue.getId), importItemSystem)
                } else {
                  new ImportItem(Some(exportedValue.getProperty.getName), ImportItemType.SystemPropertyValue, ImportItemMatch.ArchiveOnly, None, Some(exportedValue.getId), importItemSystem)
                }
              }
            }
            // Statements.
            if (exportedSystem.getStatements != null) {
              exportedSystem.getStatements.getStatement.foreach { exportedStatement =>
                var targetStatement: Option[(models.Systems, models.Actors)] = None
                var importItemStatement: ImportItem = null
                if (targetSystem.isDefined) {
                  if (domainImportInfo.actorXmlIdToImportItemMap.containsKey(exportedStatement.getActor.getId)) {
                    // The actor either exists in the DB or is new.
                    val referredActorImportItem = domainImportInfo.actorXmlIdToImportItemMap(exportedStatement.getActor.getId)
                    if (referredActorImportItem.itemMatch == ImportItemMatch.Both) {
                      // The actor was matched in the DB.
                      val matchedActorId = referredActorImportItem.targetKey.get.toLong
                      if (targetStatementMap.containsKey(targetSystem.get.id)) {
                        val matchedStatement = targetStatementMap(targetSystem.get.id).remove(matchedActorId)
                        if (matchedStatement.isDefined) {
                          // Existing statement that was matched.
                          targetStatement = Some(targetSystem.get, matchedStatement.get._2)
                        }
                      }
                    }
                  }
                }
                if (targetStatement.isDefined) {
                  importItemStatement = new ImportItem(Some(domainImportInfo.targetActorToSpecificationMap(targetStatement.get._2.id).fullname + " (" + targetStatement.get._2.name + ")"), ImportItemType.Statement, ImportItemMatch.Both, Some(targetStatement.get._1.id.toString + "_" + targetStatement.get._2.id.toString), Some(exportedStatement.getId), importItemSystem)
                  importItemMapStatement += ((targetStatement.get._1.id.toString + "_" + targetStatement.get._2.id.toString) -> importItemStatement)
                } else {
                  importItemStatement = new ImportItem(Some(exportedStatement.getActor.getSpecification.getFullName + "(" + exportedStatement.getActor.getName + ")"), ImportItemType.Statement, ImportItemMatch.ArchiveOnly, None, Some(exportedStatement.getId), importItemSystem)
                }
                // Statement configurations.
                if (exportedStatement.getConfigurations != null) {
                  exportedStatement.getConfigurations.getConfiguration.foreach { exportedConfig =>
                    var targetConfig: Option[models.Configs] = None
                    var targetConfigParam: Option[models.Parameters] = None
                    var targetEndpoint: Option[models.Endpoints] = None
                    if (targetStatement.isDefined && domainImportInfo.targetActorEndpointMap.containsKey(targetStatement.get._2.id)) {
                      targetEndpoint = domainImportInfo.targetActorEndpointMap(targetStatement.get._2.id).get(exportedConfig.getParameter.getEndpoint.getName)
                      if (targetEndpoint.isDefined && domainImportInfo.targetEndpointParameterMap.containsKey(targetEndpoint.get.id)) {
                        targetConfigParam = domainImportInfo.targetEndpointParameterMap(targetEndpoint.get.id).get(exportedConfig.getParameter.getName)
                        if (targetConfigParam.isDefined) {
                          val key = targetEndpoint.get.actor + "_" + targetEndpoint.get.id + "_" + targetStatement.get._1.id + "_" + targetConfigParam.get.id
                          if (targetStatementConfigurationMap.containsKey(key)) {
                            targetConfig = targetStatementConfigurationMap(key).remove(exportedConfig.getParameter.getName)
                          }
                        }
                      }
                    }
                    if (targetConfig.isDefined) {
                      // [Actor ID]_[Endpoint ID]_[System ID]_[Parameter ID]
                      new ImportItem(Some(targetConfigParam.get.name), ImportItemType.StatementConfiguration, ImportItemMatch.Both, Some(targetEndpoint.get.actor + "_" + targetEndpoint.get.id+"_"+targetConfig.get.system + "_" + targetConfig.get.parameter), Some(exportedConfig.getId), importItemStatement)
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
      new ImportItem(Some(label.labelType.toString), ImportItemType.CustomLabel, ImportItemMatch.DBOnly, Some(label.community + "_" + label.labelType), None, importItemCommunity)
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
        new ImportItem(Some(x.getKey), ImportItemType.OrganisationPropertyValue, ImportItemMatch.DBOnly, Some(x.getValue.organisation.toString + "_" + x.getValue.parameter.toString), None, importItemMapOrganisation(entry.getKey.toString))
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
    targetSystemPropertyValueMap.entrySet.foreach { x1 =>
      x1.getValue.entrySet.foreach { x2 =>
        new ImportItem(Some(x2.getKey), ImportItemType.SystemPropertyValue, ImportItemMatch.DBOnly, Some(x2.getValue.system.toString + "_" + x2.getValue.parameter.toString), None, importItemMapSystem(x2.getValue.system.toString))
      }
    }
    targetStatementMap.entrySet.foreach { x =>
      x.getValue.values.foreach { statement =>
        val specification = statement._1
        val actor = statement._2
        val item = new ImportItem(Some(specification.fullname + " (" + actor.name + ")"), ImportItemType.Statement, ImportItemMatch.DBOnly, Some(x.getKey + "_" + actor.id.toString), None, importItemMapSystem(x.getKey.toString))
        importItemMapStatement += (x.getKey + "_" + actor.id -> item)
      }
    }
    targetStatementConfigurationMap.entrySet.foreach { entry1 =>
      // Key is [Actor ID]_[Endpoint ID]_[System ID]_[Parameter ID]
      val keyParts = StringUtils.split(entry1.getKey, "_")
      entry1.getValue.entrySet.foreach { entry2 =>
        val statementMapKey = keyParts(2) + "_" + keyParts(0) // This map has keys as [System ID]_[Actor ID])
        if (importItemMapStatement.containsKey(statementMapKey)) {
          // It is possible to have a configuration value without a conformance statement.
          val importItemStatement = importItemMapStatement(statementMapKey)
          new ImportItem(Some(entry2.getKey), ImportItemType.StatementConfiguration, ImportItemMatch.DBOnly, Some(entry1.getKey), None, importItemStatement)
        }
      }
    }
    (importItemCommunity, importItemDomain)
  }

}