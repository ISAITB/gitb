package managers.export

import com.gitb.utils.XMLUtils
import com.gitb.xml.export.Export
import config.Configurations
import exceptions.ErrorCodes
import managers._
import models.Enums.LabelType.LabelType
import models.Enums.{ImportItemMatch, ImportItemType, LabelType}
import models._
import net.sf.saxon.xpath.XPathFactoryImpl
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{ClamAVClient, ZipArchiver}

import java.io.{File, InputStream}
import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.{Inject, Singleton}
import javax.xml.transform.stream.{StreamResult, StreamSource}
import javax.xml.xpath.XPathFactory
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ImportPreviewManager @Inject()(exportManager: ExportManager, systemConfigurationManager: SystemConfigurationManager, communityManager: CommunityManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  import scala.jdk.CollectionConverters._

  private val logger = LoggerFactory.getLogger(classOf[ImportPreviewManager])
  private val VERSION_NUMBER_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+).*$")
  /*
  Contains in sequence the migrations to be applied (older versions to newer versions). For each such pair there has
  to be a XSLT schema/export/migrations/from_[FROM_VERSION]_to_[TO_VERSION].xslt.
   */
  private val MIGRATIONS: List[(Version, Version)] = List(
    (new Version(1, 9, 1), new Version(1, 10, 0))
  )


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
      if (!map.contains(name)) {
        map += (name -> new ListBuffer[A]())
      }
      map(name) += x
    }
    map
  }

  private def specificationKey(specName: String, groupName: Option[String]): String = {
    if (groupName.nonEmpty) {
      groupName.get + "||" + specName
    } else {
      specName
    }
  }

  private def previewSystemSettingsImportInternal(exportedSettings: com.gitb.xml.export.Settings, referenceUserEmails: mutable.Set[String]): ImportItem = {
    val importTargets = ImportTargets.fromSettings(exportedSettings)
    val targetThemeMap = mutable.Map[String, ListBuffer[Long]]()
    exec(PersistenceSchema.themes.filter(_.custom === true).result).foreach { x =>
        if (!targetThemeMap.contains(x.key)) {
          targetThemeMap += (x.key -> new ListBuffer[Long]())
        }
      targetThemeMap(x.key) += x.id
    }
    val targetAdministratorsMap = mutable.Map[String, models.Users]()
    exportManager.loadSystemAdministrators().foreach { x =>
      targetAdministratorsMap += (x.email -> x)
    }
    val targetLandingPageMap = mutable.Map[String, ListBuffer[Long]]()
    exec(PersistenceSchema.landingPages.filter(_.community === Constants.DefaultCommunityId).map(x => (x.name, x.id)).result).foreach { x =>
      if (!targetLandingPageMap.contains(x._1)) {
        targetLandingPageMap += (x._1 -> new ListBuffer[Long]())
      }
      targetLandingPageMap(x._1) += x._2
    }
    val targetLegalNoticeMap = mutable.Map[String, ListBuffer[Long]]()
    exec(PersistenceSchema.legalNotices.filter(_.community === Constants.DefaultCommunityId).map(x => (x.name, x.id)).result).foreach { x =>
      if (!targetLegalNoticeMap.contains(x._1)) {
        targetLegalNoticeMap += (x._1 -> new ListBuffer[Long]())
      }
      targetLegalNoticeMap(x._1) += x._2
    }
    val targetErrorTemplateMap = mutable.Map[String, ListBuffer[Long]]()
    exec(PersistenceSchema.errorTemplates.filter(_.community === Constants.DefaultCommunityId).map(x => (x.name, x.id)).result).foreach { x =>
      if (!targetLegalNoticeMap.contains(x._1)) {
        targetErrorTemplateMap += (x._1 -> new ListBuffer[Long]())
      }
      targetErrorTemplateMap(x._1) += x._2
    }
    val targetSystemSettingsMap = mutable.HashSet[String]()
    systemConfigurationManager.getEditableSystemConfigurationValues(onlyPersisted = true).foreach { x =>
      targetSystemSettingsMap += x.config.name
    }
    val importItemSettings = new ImportItem(Some("System settings"), ImportItemType.Settings, ImportItemMatch.Both, None, None)
    if (importTargets.hasSettings) {
      // Themes.
      if (importTargets.hasThemes) {
        exportedSettings.getThemes.getTheme.asScala.foreach { exportedTheme =>
          var targetTheme: Option[Long] = None
          val foundContent = targetThemeMap.get(exportedTheme.getKey)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetTheme = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
              targetThemeMap.remove(exportedTheme.getKey)
            }
          }
          if (targetTheme.isDefined) {
            new ImportItem(Some(exportedTheme.getKey), ImportItemType.Theme, ImportItemMatch.Both, Some(targetTheme.get.toString), Some(exportedTheme.getId), importItemSettings)
          } else {
            new ImportItem(Some(exportedTheme.getKey), ImportItemType.Theme, ImportItemMatch.ArchiveOnly, None, Some(exportedTheme.getId), importItemSettings)
          }
        }
      }
      // Landing pages.
      if (importTargets.hasDefaultLandingPages) {
        exportedSettings.getLandingPages.getLandingPage.asScala.foreach { exportedLandingPage =>
          var targetLandingPage: Option[Long] = None
          val foundContent = targetLandingPageMap.get(exportedLandingPage.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetLandingPage = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
              targetLandingPageMap.remove(exportedLandingPage.getName)
            }
          }
          if (targetLandingPage.isDefined) {
            new ImportItem(Some(exportedLandingPage.getName), ImportItemType.DefaultLandingPage, ImportItemMatch.Both, Some(targetLandingPage.get.toString), Some(exportedLandingPage.getId), importItemSettings)
          } else {
            new ImportItem(Some(exportedLandingPage.getName), ImportItemType.DefaultLandingPage, ImportItemMatch.ArchiveOnly, None, Some(exportedLandingPage.getId), importItemSettings)
          }
        }
      }
      // Legal notices.
      if (importTargets.hasDefaultLegalNotices) {
        exportedSettings.getLegalNotices.getLegalNotice.asScala.foreach { exportedLegalNotice =>
          var targetLegalNotice: Option[Long] = None
          val foundContent = targetLegalNoticeMap.get(exportedLegalNotice.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetLegalNotice = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
              targetLegalNoticeMap.remove(exportedLegalNotice.getName)
            }
          }
          if (targetLegalNotice.isDefined) {
            new ImportItem(Some(exportedLegalNotice.getName), ImportItemType.DefaultLegalNotice, ImportItemMatch.Both, Some(targetLegalNotice.get.toString), Some(exportedLegalNotice.getId), importItemSettings)
          } else {
            new ImportItem(Some(exportedLegalNotice.getName), ImportItemType.DefaultLegalNotice, ImportItemMatch.ArchiveOnly, None, Some(exportedLegalNotice.getId), importItemSettings)
          }
        }
      }
      // Error templates.
      if (importTargets.hasDefaultErrorTemplates) {
        exportedSettings.getErrorTemplates.getErrorTemplate.asScala.foreach { exportedErrorTemplate =>
          var targetErrorTemplate: Option[Long] = None
          val foundContent = targetErrorTemplateMap.get(exportedErrorTemplate.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetErrorTemplate = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
              targetErrorTemplateMap.remove(exportedErrorTemplate.getName)
            }
          }
          if (targetErrorTemplate.isDefined) {
            new ImportItem(Some(exportedErrorTemplate.getName), ImportItemType.DefaultErrorTemplate, ImportItemMatch.Both, Some(targetErrorTemplate.get.toString), Some(exportedErrorTemplate.getId), importItemSettings)
          } else {
            new ImportItem(Some(exportedErrorTemplate.getName), ImportItemType.DefaultErrorTemplate, ImportItemMatch.ArchiveOnly, None, Some(exportedErrorTemplate.getId), importItemSettings)
          }
        }
      }
      // System administrators.
      if (importTargets.hasSystemAdministrators && !Configurations.AUTHENTICATION_SSO_ENABLED) {
        exportedSettings.getAdministrators.getAdministrator.asScala.foreach { exportedAdministrator =>
          val targetAdministrator = targetAdministratorsMap.remove(exportedAdministrator.getEmail)
          if (targetAdministrator.isDefined) {
            new ImportItem(Some(targetAdministrator.get.name), ImportItemType.SystemAdministrator, ImportItemMatch.Both, Some(targetAdministrator.get.id.toString), Some(exportedAdministrator.getId), importItemSettings)
          } else {
            if (!referenceUserEmails.contains(exportedAdministrator.getEmail.toLowerCase)) {
              new ImportItem(Some(exportedAdministrator.getName), ImportItemType.SystemAdministrator, ImportItemMatch.ArchiveOnly, None, Some(exportedAdministrator.getId), importItemSettings)
              referenceUserEmails += exportedAdministrator.getEmail.toLowerCase
            }
          }
        }
      }
      // System configurations.
      if (importTargets.hasSystemConfigurations) {
        exportedSettings.getSystemConfigurations.getConfig.asScala.foreach { exportedConfig =>
          if (targetSystemSettingsMap.contains(exportedConfig.getName)) {
            targetSystemSettingsMap.remove(exportedConfig.getName)
            new ImportItem(Some(nameForSystemConfiguration(exportedConfig.getName)), ImportItemType.SystemConfiguration, ImportItemMatch.Both, Some(exportedConfig.getName), Some(exportedConfig.getName), importItemSettings)
          } else {
            new ImportItem(Some(nameForSystemConfiguration(exportedConfig.getName)), ImportItemType.SystemConfiguration, ImportItemMatch.ArchiveOnly, None, Some(exportedConfig.getName), importItemSettings)
          }
        }
      }
    }
    // Mark items not found for deletion.
    targetThemeMap.foreach { entry =>
      entry._2.foreach { id =>
        new ImportItem(Some(entry._1), ImportItemType.Theme, ImportItemMatch.DBOnly, Some(id.toString), None, importItemSettings)
      }
    }
    targetLandingPageMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.DefaultLandingPage, ImportItemMatch.DBOnly, Some(x.toString), None, importItemSettings)
      }
    }
    targetLegalNoticeMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.DefaultLegalNotice, ImportItemMatch.DBOnly, Some(x.toString), None, importItemSettings)
      }
    }
    targetErrorTemplateMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.DefaultErrorTemplate, ImportItemMatch.DBOnly, Some(x.toString), None, importItemSettings)
      }
    }
    targetAdministratorsMap.values.foreach { user =>
      new ImportItem(Some(user.name), ImportItemType.SystemAdministrator, ImportItemMatch.DBOnly, Some(user.id.toString), None, importItemSettings)
    }
    targetSystemSettingsMap.foreach { name =>
      new ImportItem(Some(nameForSystemConfiguration(name)), ImportItemType.SystemConfiguration, ImportItemMatch.DBOnly, Some(name), None, importItemSettings)
    }
    importItemSettings
  }

  private def nameForSystemConfiguration(setting: String): String = {
    setting match {
      case Constants.SessionAliveTime => "Test session timeout"
      case Constants.RestApiEnabled => "REST API"
      case Constants.RestApiAdminKey => "REST API administration API key"
      case Constants.SelfRegistrationEnabled => "Self-registration"
      case Constants.DemoAccount => "Demo account"
      case Constants.WelcomeMessage => "Custom welcome page message"
      case Constants.AccountRetentionPeriod => "Inactive account retention period"
      case Constants.EmailSettings => "Email settings"
      case _ => setting
    }
  }

  private def previewDomainImportInternal(exportedDomain: com.gitb.xml.export.Domain, targetDomainId: Option[Long], canDoAdminOperations: Boolean, settings: ImportSettings, linkedToCommunity: Boolean): (DomainImportInfo, ImportItem) = {
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
    val targetSpecificationMap = mutable.Map[String, models.Specifications]()
    val targetSpecificationGroupMap = mutable.Map[String, models.SpecificationGroups]()
    val targetSpecificationIdMap = mutable.Map[Long, models.Specifications]()
    val targetDomainTestSuiteMap = mutable.Map[String, models.TestSuites]()
    val targetSpecificationTestSuiteMap = mutable.Map[Long, mutable.Map[String, models.TestSuites]]()
    val targetSpecificationActorMap = mutable.Map[Long, mutable.Map[String, models.Actors]]()
    val targetActorEndpointMap = mutable.Map[Long, mutable.Map[String, models.Endpoints]]()
    val referenceActorEndpointMap = mutable.Map[Long, mutable.Map[String, models.Endpoints]]()
    val referenceActorToSpecificationMap = mutable.Map[Long, models.Specifications]()
    val targetEndpointParameterMap = mutable.Map[Long, mutable.Map[String, models.Parameters]]()
    val referenceEndpointParameterMap = mutable.Map[Long, mutable.Map[String, models.Parameters]]()
    val referenceEndpointParameterIdMap = mutable.Map[Long, models.Parameters]()
    val targetDomainParametersMap = mutable.Map[String, models.DomainParameter]()

    val importItemMapSpecification = mutable.Map[String, ImportItem]()
    val importItemMapActor = mutable.Map[String, ImportItem]()
    val importItemMapEndpoint = mutable.Map[String, ImportItem]()

    val actorXmlIdToImportItemMap = mutable.Map[String, ImportItem]()

    var importItemDomain: ImportItem = null
    if (targetDomain.isDefined && importTargets.hasDomain) {
      importItemDomain = new ImportItem(Some(targetDomain.get.fullname), ImportItemType.Domain, ImportItemMatch.Both, Some(targetDomain.get.id.toString), Some(exportedDomain.getId))
    } else if (targetDomain.isDefined && !importTargets.hasDomain) {
      importItemDomain = new ImportItem(Some(targetDomain.get.fullname), ImportItemType.Domain, ImportItemMatch.DBOnly, Some(targetDomain.get.id.toString), None)
    } else if (targetDomain.isEmpty && importTargets.hasDomain) {
      val nameToUse = if (!linkedToCommunity && settings.fullNameReplacement.isDefined) {
        settings.fullNameReplacement.get
      } else {
        exportedDomain.getFullName
      }
      importItemDomain = new ImportItem(Some(nameToUse), ImportItemType.Domain, ImportItemMatch.ArchiveOnly, None, Some(exportedDomain.getId))
    }
    if (canDoAdminOperations || (importItemDomain != null && importItemDomain.itemMatch == ImportItemMatch.Both)) {
      // If the user isn't a Test Bed admin don't show the deletion or creation of a domain as this will anyway be skip when completing the import.
      if (targetDomain.isDefined) {
        // Load data.
        exec(PersistenceSchema.specificationGroups
          .filter(_.domain === targetDomain.get.id)
          .result
        ).foreach(x => {
          targetSpecificationGroupMap += (x.shortname -> x)
        })
        exec(PersistenceSchema.specifications
          .joinLeft(PersistenceSchema.specificationGroups).on(_.group === _.id)
          .filter(_._1.domain === targetDomain.get.id)
          .result
        ).foreach(x => {
          targetSpecificationMap += (specificationKey(x._1.shortname, x._2.map(_.shortname)) -> x._1)
          targetSpecificationIdMap += (x._1.id -> x._1)
        })
        if (targetSpecificationMap.nonEmpty) {
          exportManager.loadSpecificationTestSuiteMap(targetDomain.get.id).foreach { x =>
            targetSpecificationTestSuiteMap += (x._1 -> listBufferToNameMap(x._2, { t => t.identifier }))
          }
          exportManager.loadSpecificationActorMap(targetDomain.get.id).foreach { x =>
            targetSpecificationActorMap += (x._1 -> listBufferToNameMap(x._2, { a => a.actorId }))
            x._2.foreach { a =>
              referenceActorToSpecificationMap += (a.id -> targetSpecificationIdMap(x._1))
            }
          }
          if (targetSpecificationActorMap.nonEmpty) {
            exportManager.loadActorEndpointMap(targetDomain.get.id).foreach { x =>
              targetActorEndpointMap += (x._1 -> listBufferToNameMap(x._2, { e => e.name }))
              referenceActorEndpointMap += (x._1 -> listBufferToNameMap(x._2, { e => e.name }))
            }
            if (targetActorEndpointMap.nonEmpty) {
              exportManager.loadEndpointParameterMap(targetDomain.get.id).foreach { x =>
                targetEndpointParameterMap += (x._1 -> listBufferToNameMap(x._2, { p => p.testKey }))
                referenceEndpointParameterMap += (x._1 -> listBufferToNameMap(x._2, { p => p.testKey }))
                x._2.foreach { p =>
                  referenceEndpointParameterIdMap += (p.id -> p)
                }
              }
            }
          }
        }
        exec(PersistenceSchema.testSuites
          .filter(_.domain === targetDomain.get.id)
          .filter(_.shared)
          .result
        ).foreach(x => targetDomainTestSuiteMap += (x.identifier -> x))
        exec(PersistenceSchema.domainParameters
          .filter(_.domain === targetDomain.get.id)
          .result
        ).foreach(x => targetDomainParametersMap += (x.name -> x))
      }
      // Domain parameters.
      if (importTargets.hasDomainParameters) {
        exportedDomain.getParameters.getParameter.asScala.foreach { exportedDomainParameter =>
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
      // Shared test suites.
      if (importTargets.hasTestSuites && exportedDomain.getSharedTestSuites != null) {
        exportedDomain.getSharedTestSuites.getTestSuite.asScala.foreach { exportedTestSuite =>
          val targetTestSuite: Option[models.TestSuites] = targetDomainTestSuiteMap.remove(exportedTestSuite.getIdentifier)
          if (targetTestSuite.isDefined) {
            new ImportItem(Some(targetTestSuite.get.fullname), ImportItemType.TestSuite, ImportItemMatch.Both, Some(targetTestSuite.get.id.toString), Some(exportedTestSuite.getId), importItemDomain)
          } else {
            new ImportItem(Some(exportedTestSuite.getShortName), ImportItemType.TestSuite, ImportItemMatch.ArchiveOnly, None, Some(exportedTestSuite.getId), importItemDomain)
          }
        }
      }
      // Specifications.
      if (importTargets.hasSpecifications) {
        val groupImportItemMap = new mutable.HashMap[String, ImportItem]()
        // Groups.
        if (exportedDomain.getSpecificationGroups != null) {
          exportedDomain.getSpecificationGroups.getGroup.asScala.foreach { exportedGroup =>
            var targetGroup: Option[models.SpecificationGroups] = None
            var importItemGroup: ImportItem = null
            if (targetDomain.isDefined) {
              targetGroup = targetSpecificationGroupMap.remove(exportedGroup.getShortName)
            }
            if (targetGroup.isDefined) {
              importItemGroup = new ImportItem(Some(targetGroup.get.fullname), ImportItemType.SpecificationGroup, ImportItemMatch.Both, Some(targetGroup.get.id.toString), Some(exportedGroup.getId), importItemDomain)
            } else {
              importItemGroup = new ImportItem(Some(exportedGroup.getFullName), ImportItemType.SpecificationGroup, ImportItemMatch.ArchiveOnly, None, Some(exportedGroup.getId), importItemDomain)
            }
            groupImportItemMap += (exportedGroup.getId -> importItemGroup)
          }
        }
        // Specifications.
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.asScala.foreach { exportedSpecification =>
            var targetSpecification: Option[models.Specifications] = None
            var importItemSpecification: ImportItem = null
            if (targetDomain.isDefined) {
              val key = specificationKey(exportedSpecification.getShortName, Option(exportedSpecification.getGroup).map(_.getShortName))
              targetSpecification = targetSpecificationMap.remove(key)
            }
            var parentItem = importItemDomain
            if (exportedSpecification.getGroup != null) {
              parentItem = groupImportItemMap(exportedSpecification.getGroup.getId)
            }
            if (targetSpecification.isDefined) {
              importItemSpecification = new ImportItem(Some(targetSpecification.get.fullname), ImportItemType.Specification, ImportItemMatch.Both, Some(targetSpecification.get.id.toString), Some(exportedSpecification.getId), parentItem)
              importItemMapSpecification += (targetSpecification.get.id.toString -> importItemSpecification)
            } else {
              importItemSpecification = new ImportItem(Some(exportedSpecification.getFullName), ImportItemType.Specification, ImportItemMatch.ArchiveOnly, None, Some(exportedSpecification.getId), parentItem)
            }
            // Test suites.
            if (exportedSpecification.getTestSuites != null) {
              exportedSpecification.getTestSuites.getTestSuite.asScala.foreach { exportedTestSuite =>
                var targetTestSuite: Option[models.TestSuites] = None
                if (targetSpecification.isDefined && targetSpecificationTestSuiteMap.contains(targetSpecification.get.id)) {
                  targetTestSuite = targetSpecificationTestSuiteMap(targetSpecification.get.id).remove(exportedTestSuite.getIdentifier)
                }
                if (targetTestSuite.isDefined) {
                  new ImportItem(Some(targetTestSuite.get.fullname), ImportItemType.TestSuite, ImportItemMatch.Both, Some(targetTestSuite.get.id.toString), Some(exportedTestSuite.getId), importItemSpecification)
                } else {
                  new ImportItem(Some(exportedTestSuite.getShortName), ImportItemType.TestSuite, ImportItemMatch.ArchiveOnly, None, Some(exportedTestSuite.getId), importItemSpecification)
                }
              }
            }
            // Shared test suites.
            if (exportedSpecification.getSharedTestSuites != null) {
              exportedSpecification.getSharedTestSuites.asScala.foreach { exportedTestSuite =>
                var targetTestSuite: Option[models.TestSuites] = None
                if (targetSpecification.isDefined && targetSpecificationTestSuiteMap.contains(targetSpecification.get.id)) {
                  targetTestSuite = targetSpecificationTestSuiteMap(targetSpecification.get.id).remove(exportedTestSuite.getIdentifier)
                }
                val sourceId = importItemSpecification.sourceKey.get + "|" + exportedTestSuite.getId
                if (targetTestSuite.isDefined) {
                  val targetId = importItemSpecification.targetKey.get + "|" + targetTestSuite.get.id
                  new ImportItem(Some(targetTestSuite.get.fullname), ImportItemType.TestSuite, ImportItemMatch.Both, Some(targetId), Some(sourceId), importItemSpecification)
                } else {
                  new ImportItem(Some(exportedTestSuite.getShortName), ImportItemType.TestSuite, ImportItemMatch.ArchiveOnly, None, Some(sourceId), importItemSpecification)
                }
              }
            }
            // Actors
            if (exportedSpecification.getActors != null) {
              exportedSpecification.getActors.getActor.asScala.foreach { exportedActor =>
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
                  exportedActor.getEndpoints.getEndpoint.asScala.foreach { exportedEndpoint =>
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
                      exportedEndpoint.getParameters.getParameter.asScala.foreach { exportedEndpointParameter =>
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
      }
      // Mark items not found for deletion.
      targetDomainParametersMap.values.foreach { parameter =>
        new ImportItem(Some(parameter.name), ImportItemType.DomainParameter, ImportItemMatch.DBOnly, Some(parameter.id.toString), None, importItemDomain)
      }
      targetDomainTestSuiteMap.values.foreach { testSuite =>
        new ImportItem(Some(testSuite.fullname), ImportItemType.TestSuite, ImportItemMatch.DBOnly, Some(testSuite.id.toString), None, importItemDomain)
      }
      targetSpecificationGroupMap.values.foreach { group =>
        new ImportItem(Some(group.fullname), ImportItemType.SpecificationGroup, ImportItemMatch.DBOnly, Some(group.id.toString), None, importItemDomain)
      }
      targetSpecificationMap.values.foreach { specification =>
        val item = new ImportItem(Some(specification.fullname), ImportItemType.Specification, ImportItemMatch.DBOnly, Some(specification.id.toString), None, importItemDomain)
        importItemMapSpecification += (specification.id.toString -> item)
      }
      targetSpecificationTestSuiteMap.foreach { x =>
        x._2.values.foreach { testSuite =>
          new ImportItem(Some(testSuite.fullname), ImportItemType.TestSuite, ImportItemMatch.DBOnly, Some(testSuite.id.toString), None, importItemMapSpecification(x._1.toString))
        }
      }
      targetSpecificationActorMap.foreach { entry =>
        entry._2.values.foreach { actor =>
          val item = new ImportItem(Some(actor.name), ImportItemType.Actor, ImportItemMatch.DBOnly, Some(actor.id.toString), None, importItemMapSpecification(entry._1.toString))
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
    } else {
      importItemDomain = null
    }
    (DomainImportInfo(referenceActorEndpointMap, referenceEndpointParameterMap, referenceActorToSpecificationMap, referenceEndpointParameterIdMap, actorXmlIdToImportItemMap), importItemDomain)
  }

  private [export] def loadUserEmailSet(): mutable.Set[String] = {
    val emails = mutable.Set[String]()
    exec(PersistenceSchema.users.map(x => x.email).result).foreach { email =>
      if (email != null) {
        emails += email.toLowerCase
      }
    }
    emails
  }

  def previewSystemSettingsImport(exportedSettings: com.gitb.xml.export.Settings): ImportItem = {
    previewSystemSettingsImportInternal(exportedSettings, new mutable.HashSet[String]())
  }

  def previewDomainImport(exportedDomain: com.gitb.xml.export.Domain, targetDomainId: Option[Long], canDoAdminOperations: Boolean, settings: ImportSettings): ImportItem = {
    previewDomainImportInternal(exportedDomain, targetDomainId, canDoAdminOperations, settings, linkedToCommunity = false)._2
  }

  def previewDeletionsImport(exportedDeletions: com.gitb.xml.export.Deletions): List[ImportItem] = {
    val importItems = new ListBuffer[ImportItem]
    val results = exec(for {
      communities <- {
        if (exportedDeletions.getCommunity.isEmpty) {
          DBIO.successful(Seq.empty)
        } else {
          PersistenceSchema.communities
            .filter(_.apiKey inSet exportedDeletions.getCommunity.asScala)
            .map(x => (x.id, x.fullname))
            .sortBy(_._2.asc)
            .result
        }
      }
      domains <- {
        if (exportedDeletions.getDomain.isEmpty) {
          DBIO.successful(Seq.empty)
        } else {
          PersistenceSchema.domains
            .filter(_.apiKey inSet exportedDeletions.getDomain.asScala)
            .map(x => (x.id, x.fullname))
            .sortBy(_._2.asc)
            .result
        }
      }
    } yield (communities, domains))
    // Communities
    results._1.foreach { communityInfo =>
      importItems += new ImportItem(Some(communityInfo._2), ImportItemType.Community, ImportItemMatch.DBOnly, Some(communityInfo._1.toString), None)
    }
    // Domains
    results._2.foreach { domainInfo =>
      importItems += new ImportItem(Some(domainInfo._2), ImportItemType.Domain, ImportItemMatch.DBOnly, Some(domainInfo._1.toString), None)
    }
    importItems.toList
  }

  def previewCommunityImport(exportedData: com.gitb.xml.export.Export, targetCommunityId: Option[Long], canDoAdminOperations: Boolean, settings: ImportSettings): (ImportItem, Option[ImportItem], Option[ImportItem]) = {
    var importItemCommunity: ImportItem = null
    var targetCommunity: Option[models.Communities] = None
    if (targetCommunityId.isDefined) {
      require(targetCommunityId.get != Constants.DefaultCommunityId, "The Test Bed's default community cannot be the target of an import")
      targetCommunity = exec(PersistenceSchema.communities
        .filter(_.id === targetCommunityId.get)
        .result
        .headOption)
    }
    val exportedCommunity = exportedData.getCommunities.getCommunity.get(0)
    val importTargets = ImportTargets.fromCommunity(exportedCommunity)
    // Process first domain and get back reference maps.
    var targetDomainId: Option[Long] = None
    if (targetCommunity.isDefined) {
      targetDomainId = targetCommunity.get.domain
    }
    val domainImportResult = previewDomainImportInternal(exportedCommunity.getDomain, targetDomainId, canDoAdminOperations, settings, linkedToCommunity = true)
    val domainImportInfo = domainImportResult._1
    var importItemDomain: Option[ImportItem] = None
    if (domainImportResult._2 != null) {
      importItemDomain = Some(domainImportResult._2)
    }
    // Process community.
    val targetAdministratorsMap = mutable.Map[String, models.Users]()
    val targetOrganisationPropertyMap = mutable.Map[String, models.OrganisationParameters]()
    val targetOrganisationPropertyIdMap = mutable.Map[Long, models.OrganisationParameters]()
    val targetSystemPropertyMap = mutable.Map[String, models.SystemParameters]()
    val targetSystemPropertyIdMap = mutable.Map[Long, models.SystemParameters]()
    val targetCustomLabelMap = mutable.Map[LabelType, models.CommunityLabels]()
    val targetLandingPageMap = mutable.Map[String, ListBuffer[Long]]()
    val targetLegalNoticeMap = mutable.Map[String, ListBuffer[Long]]()
    val targetErrorTemplateMap = mutable.Map[String, ListBuffer[Long]]()
    val targetTriggerMap = mutable.Map[String, ListBuffer[Long]]()
    val targetResourceMap = mutable.Map[String, ListBuffer[Long]]()
    val targetOrganisationMap = mutable.Map[String, mutable.ListBuffer[models.Organizations]]()
    val targetOrganisationPropertyValueMap = mutable.Map[Long, mutable.Map[String, models.OrganisationParameterValues]]()
    val targetOrganisationUserMap = mutable.Map[Long, mutable.Map[String, models.Users]]()
    val targetSystemMap = mutable.Map[Long, mutable.Map[String, ListBuffer[models.Systems]]]()
    val targetSystemIdMap = mutable.Map[Long, models.Systems]()
    val targetSystemPropertyValueMap = mutable.Map[Long, mutable.Map[String, models.SystemParameterValues]]()
    val targetStatementMap = mutable.Map[Long, mutable.Map[Long, (models.Specifications, models.Actors)]]() // System to [actor_DB_ID] to (specification, actor)]    WAS: System to [actor name to (specification name, actor)]
    val targetStatementConfigurationMap = mutable.Map[String, mutable.Map[String, models.Configs]]() // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID] to Parameter name to Configs
    val importItemMapOrganisation = mutable.Map[String, ImportItem]()
    val importItemMapSystem = mutable.Map[String, ImportItem]()
    val importItemMapStatement = mutable.Map[String, ImportItem]()
    var referenceUserEmails = mutable.Set[String]()
    // Load user emails for uniqueness checks.
    if (!Configurations.AUTHENTICATION_SSO_ENABLED && (importTargets.hasAdministrators || importTargets.hasOrganisationUsers || importTargets.hasSystemAdministrators)) {
      referenceUserEmails = loadUserEmailSet()
    }
    // Process system settings. Do this here to use the loaded reference user emails.
    var importItemSettings: Option[ImportItem] = None
    if (canDoAdminOperations && exportedData.getSettings != null) {
      // Only a Test Bed administrator can process system settings. We will not show these as options to the user.
      importItemSettings = Some(previewSystemSettingsImportInternal(exportedData.getSettings, referenceUserEmails))
    }
    if (targetCommunity.isDefined) {
      importItemCommunity = new ImportItem(Some(targetCommunity.get.fullname), ImportItemType.Community, ImportItemMatch.Both, Some(targetCommunity.get.id.toString), Some(exportedCommunity.getId))
      // Load data.
      // Administrators.
      exportManager.loadCommunityAdministrators(targetCommunity.get.id).map { x =>
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
        if (!targetLandingPageMap.contains(x._1)) {
          targetLandingPageMap += (x._1 -> new ListBuffer[Long]())
        }
        targetLandingPageMap(x._1) += x._2
      }
      // Legal notices.
      exec(PersistenceSchema.legalNotices.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
        if (!targetLegalNoticeMap.contains(x._1)) {
          targetLegalNoticeMap += (x._1 -> new ListBuffer[Long]())
        }
        targetLegalNoticeMap(x._1) += x._2
      }
      // Error templates.
      exec(PersistenceSchema.errorTemplates.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
        if (!targetErrorTemplateMap.contains(x._1)) {
          targetErrorTemplateMap += (x._1 -> new ListBuffer[Long]())
        }
        targetErrorTemplateMap(x._1) += x._2
      }
      // Triggers.
      exec(PersistenceSchema.triggers.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
        if (!targetTriggerMap.contains(x._1)) {
          targetTriggerMap += (x._1 -> new ListBuffer[Long]())
        }
        targetTriggerMap(x._1) += x._2
      }
      // Resources.
      exec(PersistenceSchema.communityResources.filter(_.community === targetCommunity.get.id).map(x => (x.name, x.id)).result).map { x =>
        if (!targetResourceMap.contains(x._1)) {
          targetResourceMap += (x._1 -> new ListBuffer[Long]())
        }
        targetResourceMap(x._1) += x._2
      }
      // Organisations.
      exportManager.loadOrganisations(targetCommunity.get.id).foreach { x =>
        if (!targetOrganisationMap.contains(x.shortname)) {
          targetOrganisationMap += (x.shortname -> new ListBuffer[Organizations]())
        }
        targetOrganisationMap(x.shortname) += x
      }
      if (targetOrganisationMap.nonEmpty) {
        // Users.
        exportManager.loadOrganisationUserMap(targetCommunity.get.id).foreach { x =>
          targetOrganisationUserMap += (x._1 -> listBufferToNameMap(x._2, { u => u.email }))
        }
        if (targetOrganisationPropertyMap.nonEmpty) {
          // Organisation property values.
          exportManager.loadOrganisationParameterValueMap(targetCommunity.get.id).foreach { x =>
            targetOrganisationPropertyValueMap += (x._1 -> listBufferToNameMap(x._2, { v => targetOrganisationPropertyIdMap(v.parameter).testKey }))
          }
        }
        // Systems.
        exportManager.loadOrganisationSystemMap(targetCommunity.get.id).foreach { x =>
          targetSystemMap += (x._1 -> listBufferToNonUniqueNameMap(x._2, { s => s.shortname }))
          x._2.foreach { system =>
            targetSystemIdMap += (system.id -> system)
          }
          true
        }
        if (targetSystemMap.nonEmpty) {
          if (targetSystemPropertyMap.nonEmpty) {
            // System property values.
            exportManager.loadSystemParameterValues(targetCommunity.get.id).foreach { x =>
              targetSystemPropertyValueMap += (x._1 -> listBufferToNameMap(x._2, { v => targetSystemPropertyIdMap(v.parameter).testKey }))
            }
          }
          if (domainImportInfo.targetActorToSpecificationMap.nonEmpty) {
            // Statements.
            exportManager.loadSystemStatementsMap(targetCommunity.get.id, targetCommunity.get.domain).foreach { x =>
              var statements = targetStatementMap.get(x._1)
              if (statements.isEmpty) {
                statements = Some(mutable.Map[Long, (models.Specifications, models.Actors)]())
                targetStatementMap += (x._1 -> statements.get)
              }
              x._2.foreach { y =>
                statements.get += (y._2.id -> y)
              }
            }
            // Statement configurations.
            if (domainImportInfo.targetEndpointParameterIdMap.nonEmpty) {
              exportManager.loadSystemConfigurationsMap(targetCommunity.get).foreach { x =>
                // [actor ID]_[endpoint ID]_[System ID]_[Parameter ID]
                targetStatementConfigurationMap += (x._1 -> listBufferToNameMap(x._2, { v => domainImportInfo.targetEndpointParameterIdMap(v.parameter).testKey }))
              }
            }
          }
        }
      }
    } else {
      importItemCommunity = new ImportItem(Some(settings.fullNameReplacement.getOrElse(exportedCommunity.getFullName)), ImportItemType.Community, ImportItemMatch.ArchiveOnly, None, Some(exportedCommunity.getId))
    }
    // Administrators.
    if (importTargets.hasAdministrators && !Configurations.AUTHENTICATION_SSO_ENABLED) {
      exportedCommunity.getAdministrators.getAdministrator.asScala.foreach { exportedAdministrator =>
        var targetAdministrator: Option[models.Users] = None
        if (targetCommunity.isDefined) {
          targetAdministrator = targetAdministratorsMap.remove(exportedAdministrator.getEmail)
        }
        if (targetAdministrator.isDefined) {
          new ImportItem(Some(targetAdministrator.get.name), ImportItemType.Administrator, ImportItemMatch.Both, Some(targetAdministrator.get.id.toString), Some(exportedAdministrator.getId), importItemCommunity)
        } else {
          if (!referenceUserEmails.contains(exportedAdministrator.getEmail.toLowerCase)) {
            new ImportItem(Some(exportedAdministrator.getName), ImportItemType.Administrator, ImportItemMatch.ArchiveOnly, None, Some(exportedAdministrator.getId), importItemCommunity)
            referenceUserEmails += exportedAdministrator.getEmail.toLowerCase
          }
        }
      }
    }
    // Organisation properties.
    if (importTargets.hasOrganisationProperties) {
      exportedCommunity.getOrganisationProperties.getProperty.asScala.foreach { exportedProperty =>
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
      exportedCommunity.getSystemProperties.getProperty.asScala.foreach { exportedProperty =>
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
      exportedCommunity.getCustomLabels.getLabel.asScala.foreach { exportedLabel =>
        var targetLabel: Option[CommunityLabels] = None
        var labelType: LabelType = null
        exportedLabel.getLabelType match {
          case com.gitb.xml.export.CustomLabelType.DOMAIN => labelType = LabelType.Domain
          case com.gitb.xml.export.CustomLabelType.SPECIFICATION => labelType = LabelType.Specification
          case com.gitb.xml.export.CustomLabelType.SPECIFICATION_GROUP => labelType = LabelType.SpecificationGroup
          case com.gitb.xml.export.CustomLabelType.SPECIFICATION_IN_GROUP => labelType = LabelType.SpecificationInGroup
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
          new ImportItem(Some(targetLabel.get.labelType.toString), ImportItemType.CustomLabel, ImportItemMatch.Both, Some(s"${targetCommunity.get.id}_${targetLabel.get.labelType.toString}"), Some(exportedLabel.getId), importItemCommunity)
        } else {
          new ImportItem(Some(labelType.id.toString), ImportItemType.CustomLabel, ImportItemMatch.ArchiveOnly, None, Some(exportedLabel.getId), importItemCommunity)
        }
      }
    }
    // Landing pages.
    if (importTargets.hasLandingPages) {
      exportedCommunity.getLandingPages.getLandingPage.asScala.foreach { exportedLandingPage =>
        var targetLandingPage: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetLandingPageMap.get(exportedLandingPage.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetLandingPage = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
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
      exportedCommunity.getLegalNotices.getLegalNotice.asScala.foreach { exportedLegalNotice =>
        var targetLegalNotice: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetLegalNoticeMap.get(exportedLegalNotice.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetLegalNotice = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
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
      exportedCommunity.getErrorTemplates.getErrorTemplate.asScala.foreach { exportedErrorTemplate =>
        var targetErrorTemplate: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetErrorTemplateMap.get(exportedErrorTemplate.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetErrorTemplate = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
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
    // Triggers.
    if (importTargets.hasTriggers) {
      exportedCommunity.getTriggers.getTrigger.asScala.foreach { exportedTrigger =>
        var targetTrigger: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetTriggerMap.get(exportedTrigger.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetTrigger = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
              targetTriggerMap.remove(exportedTrigger.getName)
            }
          }
        }
        if (targetTrigger.isDefined) {
          new ImportItem(Some(exportedTrigger.getName), ImportItemType.Trigger, ImportItemMatch.Both, Some(targetTrigger.get.toString), Some(exportedTrigger.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedTrigger.getName), ImportItemType.Trigger, ImportItemMatch.ArchiveOnly, None, Some(exportedTrigger.getId), importItemCommunity)
        }
      }
    }
    // Resources.
    if (importTargets.hasResources) {
      exportedCommunity.getResources.getResource.asScala.foreach { exportedResource =>
        var targetResource: Option[Long] = None
        if (targetCommunity.isDefined) {
          val foundContent = targetResourceMap.get(exportedResource.getName)
          if (foundContent.isDefined && foundContent.get.nonEmpty) {
            targetResource = Some(foundContent.get.remove(0))
            if (foundContent.get.isEmpty) {
              targetResourceMap.remove(exportedResource.getName)
            }
          }
        }
        if (targetResource.isDefined) {
          new ImportItem(Some(exportedResource.getName), ImportItemType.CommunityResource, ImportItemMatch.Both, Some(targetResource.get.toString), Some(exportedResource.getId), importItemCommunity)
        } else {
          new ImportItem(Some(exportedResource.getName), ImportItemType.CommunityResource, ImportItemMatch.ArchiveOnly, None, Some(exportedResource.getId), importItemCommunity)
        }
      }
    }
    // Organisations.
    if (importTargets.hasOrganisations) {
      exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
        var targetOrganisation: Option[models.Organizations] = None
        var importItemOrganisation: ImportItem = null
        if (targetCommunity.isDefined) {
          val foundOrganisations = targetOrganisationMap.get(exportedOrganisation.getShortName)
          if (foundOrganisations.isDefined && foundOrganisations.get.nonEmpty) {
            targetOrganisation = Some(foundOrganisations.get.remove(0))
            if (foundOrganisations.get.isEmpty) {
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
        if (!Configurations.AUTHENTICATION_SSO_ENABLED && exportedOrganisation.getUsers != null) {
          exportedOrganisation.getUsers.getUser.asScala.foreach { exportedUser =>
            var targetUser: Option[models.Users] = None
            if (targetOrganisation.isDefined && targetOrganisationUserMap.contains(targetOrganisation.get.id)) {
              targetUser = targetOrganisationUserMap(targetOrganisation.get.id).remove(exportedUser.getEmail)
            }
            if (targetUser.isDefined) {
              new ImportItem(Some(targetUser.get.name), ImportItemType.OrganisationUser, ImportItemMatch.Both, Some(targetUser.get.id.toString), Some(exportedUser.getId), importItemOrganisation)
            } else {
              if (!referenceUserEmails.contains(exportedUser.getEmail.toLowerCase)) {
                referenceUserEmails += exportedUser.getEmail.toLowerCase
                new ImportItem(Some(exportedUser.getName), ImportItemType.OrganisationUser, ImportItemMatch.ArchiveOnly, None, Some(exportedUser.getId), importItemOrganisation)
              }
            }
          }
        }
        // Organisation property values.
        if (exportedOrganisation.getPropertyValues != null) {
          exportedOrganisation.getPropertyValues.getProperty.asScala.foreach { exportedProperty =>
            var targetPropertyValue: Option[models.OrganisationParameterValues] = None
            if (targetOrganisation.isDefined && targetOrganisationPropertyValueMap.contains(targetOrganisation.get.id)) {
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
          exportedOrganisation.getSystems.getSystem.asScala.foreach { exportedSystem =>
            var targetSystem: Option[models.Systems] = None
            var importItemSystem: ImportItem = null
            if (targetOrganisation.isDefined && targetSystemMap.contains(targetOrganisation.get.id)) {
              if (targetSystemMap(targetOrganisation.get.id).contains(exportedSystem.getShortName)) {
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
              exportedSystem.getPropertyValues.getProperty.asScala.foreach { exportedValue =>
                var targetPropertyValue: Option[models.SystemParameterValues] = None
                if (targetSystem.isDefined && targetSystemPropertyValueMap.contains(targetSystem.get.id)) {
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
              exportedSystem.getStatements.getStatement.asScala.foreach { exportedStatement =>
                var targetStatement: Option[(models.Systems, models.Actors)] = None
                var importItemStatement: ImportItem = null
                if (targetSystem.isDefined) {
                  if (domainImportInfo.actorXmlIdToImportItemMap.contains(exportedStatement.getActor.getId)) {
                    // The actor either exists in the DB or is new.
                    val referredActorImportItem = domainImportInfo.actorXmlIdToImportItemMap(exportedStatement.getActor.getId)
                    if (referredActorImportItem.itemMatch == ImportItemMatch.Both) {
                      // The actor was matched in the DB.
                      val matchedActorId = referredActorImportItem.targetKey.get.toLong
                      if (targetStatementMap.contains(targetSystem.get.id)) {
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
                  exportedStatement.getConfigurations.getConfiguration.asScala.foreach { exportedConfig =>
                    var targetConfig: Option[models.Configs] = None
                    var targetConfigParam: Option[models.Parameters] = None
                    var targetEndpoint: Option[models.Endpoints] = None
                    if (targetStatement.isDefined && domainImportInfo.targetActorEndpointMap.contains(targetStatement.get._2.id)) {
                      targetEndpoint = domainImportInfo.targetActorEndpointMap(targetStatement.get._2.id).get(exportedConfig.getParameter.getEndpoint.getName)
                      if (targetEndpoint.isDefined && domainImportInfo.targetEndpointParameterMap.contains(targetEndpoint.get.id)) {
                        targetConfigParam = domainImportInfo.targetEndpointParameterMap(targetEndpoint.get.id).get(exportedConfig.getParameter.getName)
                        if (targetConfigParam.isDefined) {
                          val key = s"${targetEndpoint.get.actor}_${targetEndpoint.get.id}_${targetStatement.get._1.id}_${targetConfigParam.get.id}"
                          if (targetStatementConfigurationMap.contains(key)) {
                            targetConfig = targetStatementConfigurationMap(key).remove(exportedConfig.getParameter.getName)
                          }
                        }
                      }
                    }
                    if (targetConfig.isDefined) {
                      // [Actor ID]_[Endpoint ID]_[System ID]_[Parameter ID]
                      new ImportItem(Some(targetConfigParam.get.testKey), ImportItemType.StatementConfiguration, ImportItemMatch.Both, Some(s"${targetEndpoint.get.actor}_${targetEndpoint.get.id}_${targetConfig.get.system}_${targetConfig.get.parameter}"), Some(exportedConfig.getId), importItemStatement)
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
      new ImportItem(Some(label.labelType.toString), ImportItemType.CustomLabel, ImportItemMatch.DBOnly, Some(s"${label.community}_${label.labelType}"), None, importItemCommunity)
    }
    targetLandingPageMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.LandingPage, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetLegalNoticeMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.LegalNotice, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetErrorTemplateMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.ErrorTemplate, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetTriggerMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.Trigger, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetResourceMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(entry._1), ImportItemType.CommunityResource, ImportItemMatch.DBOnly, Some(x.toString), None, importItemCommunity)
      }
    }
    targetOrganisationMap.foreach { entry =>
      entry._2.foreach { x =>
        val item = new ImportItem(Some(entry._1), ImportItemType.Organisation, ImportItemMatch.DBOnly, Some(x.id.toString), None, importItemCommunity)
        importItemMapOrganisation += (x.id.toString -> item)
      }
    }
    targetOrganisationPropertyValueMap.foreach { entry =>
      entry._2.foreach { x =>
        new ImportItem(Some(x._1), ImportItemType.OrganisationPropertyValue, ImportItemMatch.DBOnly, Some(x._2.organisation.toString + "_" + x._2.parameter.toString), None, importItemMapOrganisation(entry._1.toString))
      }
    }
    targetOrganisationUserMap.foreach { entry =>
      entry._2.values.foreach { user =>
        new ImportItem(Some(user.name), ImportItemType.OrganisationUser, ImportItemMatch.DBOnly, Some(user.id.toString), None, importItemMapOrganisation(entry._1.toString))
      }
    }
    targetSystemMap.foreach { entry =>
      entry._2.values.foreach { systems =>
        systems.foreach { system =>
          val item = new ImportItem(Some(system.fullname), ImportItemType.System, ImportItemMatch.DBOnly, Some(system.id.toString), None, importItemMapOrganisation(entry._1.toString))
          importItemMapSystem += (system.id.toString -> item)
        }
      }
    }
    targetSystemPropertyValueMap.foreach { x1 =>
      x1._2.foreach { x2 =>
        new ImportItem(Some(x2._1), ImportItemType.SystemPropertyValue, ImportItemMatch.DBOnly, Some(x2._2.system.toString + "_" + x2._2.parameter.toString), None, importItemMapSystem(x2._2.system.toString))
      }
    }
    targetStatementMap.foreach { x =>
      x._2.values.foreach { statement =>
        val specification = statement._1
        val actor = statement._2
        val item = new ImportItem(Some(s"${specification.fullname} (${actor.name})"), ImportItemType.Statement, ImportItemMatch.DBOnly, Some(s"${x._1}_${actor.id.toString}"), None, importItemMapSystem(x._1.toString))
        importItemMapStatement += (s"${x._1}_${actor.id}" -> item)
      }
    }
    targetStatementConfigurationMap.foreach { entry1 =>
      // Key is [Actor ID]_[Endpoint ID]_[System ID]_[Parameter ID]
      val keyParts = StringUtils.split(entry1._1, "_")
      entry1._2.foreach { entry2 =>
        val statementMapKey = keyParts(2) + "_" + keyParts(0) // This map has keys as [System ID]_[Actor ID])
        if (importItemMapStatement.contains(statementMapKey)) {
          // It is possible to have a configuration value without a conformance statement.
          val importItemStatement = importItemMapStatement(statementMapKey)
          new ImportItem(Some(entry2._1), ImportItemType.StatementConfiguration, ImportItemMatch.DBOnly, Some(entry1._1), None, importItemStatement)
        }
      }
    }
    (importItemCommunity, importItemDomain, importItemSettings)
  }

  private def getXPathFactory():XPathFactory = {
    new XPathFactoryImpl
  }

  private def getDataFileVersion(xmlFile: Path): Version = {
    val xPath = getXPathFactory().newXPath()
    val expression: String = "string(/*:export/@version)"
    val versionNumber = xPath.evaluate(expression, new InputSource(Files.newInputStream(xmlFile)))
    val matcher = VERSION_NUMBER_PATTERN.matcher(versionNumber)
    if (matcher.matches() && matcher.groupCount() == 3) {
      val fileVersion = new Version(matcher.group(1).toInt, matcher.group(2).toInt, matcher.group(3).toInt)
      fileVersion
    } else {
      throw new IllegalStateException("The data archive refers to an invalid version number ["+versionNumber+"]")
    }
  }

  private def migrateToLatestVersion(xmlFile: Path, fileVersion: Version) = {
    var xmlFileToUse = xmlFile
    // Get the declared version number from the file.
    val clearVersionNumber = fileVersion.toString
    val targetVersionNumber = Configurations.mainVersionNumber()
    if (!clearVersionNumber.equals(targetVersionNumber)) {
      logger.info("Data archive at version ["+clearVersionNumber+"] - migrating to ["+targetVersionNumber+"]")
      // XML has a different version number than the current one.
      val migrationsToApply = new ListBuffer[(Version, Version)]()
      MIGRATIONS.foreach { migration =>
        if (fileVersion.compareTo(migration._2) < 0) {
          // Keep this migration as it targets a version greater to the file version
          migrationsToApply += migration
        }
      }
      if (migrationsToApply.isEmpty) {
        logger.info("No migration needed")
      } else {
        val factory = XMLUtils.getSecureTransformerFactory()
        migrationsToApply.foreach { migration =>
          val fromVersion = migration._1.toString
          val toVersion = migration._2.toString
          logger.info("Applying migration from ["+fromVersion+"] to ["+toVersion+"]")
          val outputFile = new File(xmlFileToUse.getParent.toFile, UUID.randomUUID().toString + "_"+fromVersion+"_"+toVersion+".xml")
          val transformer = factory.newTransformer(new StreamSource(Thread.currentThread.getContextClassLoader.getResourceAsStream("schema/export/migrations/from_"+fromVersion+"_to_"+toVersion+".xslt")))
          transformer.transform(new StreamSource(xmlFileToUse.toFile), new StreamResult(outputFile))
          // Delete the previous file - this is important given that to complete the import we need to have a single XML file in the pending import folder. If the delete fails we don't proceed.
          FileUtils.forceDelete(xmlFileToUse.toFile)
          xmlFileToUse = outputFile.toPath
        }
        logger.info("Migration complete")
      }
    }
    xmlFileToUse
  }

  private def getXsdToUse(dataVersion: Version): InputStream = {
    var xsdPath = "schema/export/versions/gitb_export_"+dataVersion.toString+".xsd"
    if (Thread.currentThread().getContextClassLoader.getResource(xsdPath) == null) {
      // No XSD exists for the specific version - use latest one.
      xsdPath = "schema/export/gitb_export.xsd"
    }
    Thread.currentThread().getContextClassLoader.getResourceAsStream(xsdPath)
  }

  def prepareImportPreview(tempArchiveFile: File, importSettings: ImportSettings, requireDomain: Boolean, requireCommunity: Boolean, requireSettings: Boolean, requireDeletions: Boolean): (Option[(Int, String)], Option[Export], Option[String], Option[Path]) = {
    var errorInformation: Option[(Int, String)] = None
    var exportData: Option[Export] = None
    var pendingImportId: Option[String] = None
    var tempFolder: Option[Path] = None
    if (importSettings.encryptionKey.isEmpty) {
      errorInformation = Some((ErrorCodes.INVALID_REQUEST, "The archive encryption key was missing."))
    }
    if (errorInformation.isEmpty) {
      // Get import file.
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
        val scanResult = virusScanner.scan(tempArchiveFile)
        if (!ClamAVClient.isCleanReply(scanResult)) {
          errorInformation = Some((ErrorCodes.VIRUS_FOUND, "Archive failed virus scan."))
        }
      }
      if (errorInformation.isEmpty) {
        val importFileName = s"export_${RandomStringUtils.secure().next(10, false, true)}.zip"
        pendingImportId = Some(RandomStringUtils.secure().next(10, false, true))
        val zipFile = Paths.get(
          getPendingFolder().getAbsolutePath,
          pendingImportId.get,
          importFileName
        )
        tempFolder = Some(zipFile.getParent)
        tempFolder.get.toFile.mkdirs()
        var deleteUploadFolder = false
        try {
          // Write file.
          Files.copy(tempArchiveFile.toPath, zipFile)
          // Extract the XML document.
          var extractedFiles: java.util.Map[String, Path] = null
          try {
            extractedFiles = new ZipArchiver(zipFile, zipFile.getParent, importSettings.encryptionKey.get.toCharArray).unzip()
          } catch {
            case e: Exception =>
              deleteUploadFolder = true
              logger.warn("Unable to extract data archive", e)
              errorInformation = Some((ErrorCodes.INVALID_REQUEST, "The provided archive could not be successfully extracted."))
          } finally {
            // We don't need the ZIP archive anymore.
            FileUtils.deleteQuietly(zipFile.toFile)
          }
          if (errorInformation.isEmpty) {
            if (extractedFiles.size() != 1) {
              deleteUploadFolder = true
              errorInformation = Some((ErrorCodes.INVALID_REQUEST, "The provided archive must contain a single data file."))
            }
            if (errorInformation.isEmpty) {
              var xmlFile: Path = extractedFiles.values().asScala.head
              // Get export file version
              val dataVersion = getDataFileVersion(xmlFile)
              // XSD validation
              try {
                XMLUtils.validateAgainstSchema(Files.newInputStream(xmlFile), getXsdToUse(dataVersion))
              } catch {
                case e: Exception =>
                  deleteUploadFolder = true
                  logger.warn("Validation failure for uploaded import file", e)
                  errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided data archive failed validation. Ensure the file is not tampered and that it matches the current Test Bed version [" + Constants.VersionNumber + "].")
              }
              if (errorInformation.isEmpty) {
                // Migration to latest version (if needed)
                try {
                  xmlFile = migrateToLatestVersion(xmlFile, dataVersion)
                } catch {
                  case e: Exception =>
                    deleteUploadFolder = true
                    logger.warn("Migration failure for uploaded import file ["+dataVersion+"]", e)
                    errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided data archive is stated to be at version ["+dataVersion.toString+"] but failed migration to the current Test Bed version [" + Constants.VersionNumber + "].")
                }
                if (errorInformation.isEmpty) {
                  exportData = Some(XMLUtils.unmarshal(classOf[com.gitb.xml.export.Export], new StreamSource(Files.newInputStream(xmlFile))))
                  if (requireDomain) {
                    if (exportData.get.getDomains != null && !exportData.get.getDomains.getDomain.isEmpty) {
                      if (exportData.get.getDomains.getDomain.size() > 1) {
                        deleteUploadFolder = true
                        errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided archive includes multiple domains.")
                      }
                    } else {
                      deleteUploadFolder = true
                      errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided archive does not include a domain to process.")
                    }
                  }
                  if (requireCommunity && errorInformation.isEmpty) {
                    if (exportData.get.getCommunities != null && !exportData.get.getCommunities.getCommunity.isEmpty) {
                      if (exportData.get.getCommunities.getCommunity.size() > 1) {
                        deleteUploadFolder = true
                        errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided archive includes multiple communities.")
                      }
                    } else {
                      deleteUploadFolder = true
                      errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided archive does not include a community to process.")
                    }
                  }
                  if (requireSettings && errorInformation.isEmpty) {
                    if (exportData.get.getSettings == null) {
                      deleteUploadFolder = true
                      errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided archive does not include system settings to process.")
                    }
                  }
                  if (requireDeletions && errorInformation.isEmpty) {
                    if (exportData.get.getDeletions == null || (exportData.get.getDeletions.getDomain.isEmpty && exportData.get.getDeletions.getCommunity.isEmpty)) {
                      deleteUploadFolder = true
                      errorInformation = Some(ErrorCodes.INVALID_REQUEST, "The provided archive does not include any deletions to process.")
                    }
                  }
                }
              }
            }
          }
        } catch {
          case e: Exception =>
            deleteUploadFolder = true
            logger.error("An unexpected error occurred while processing the provided archive.", e)
            errorInformation = Some(ErrorCodes.INVALID_REQUEST, "An error occurred while processing the provided archive.")
        } finally {
          if (deleteUploadFolder && tempFolder.isDefined) {
            FileUtils.deleteQuietly(tempFolder.get.toFile)
          }
        }
      }
    }
    (errorInformation, exportData, pendingImportId, tempFolder)
  }

  def getPendingImportFile(folder: Path, pendingImportId: String): Option[File] = {
    if (Files.exists(folder) && Files.isDirectory(folder)) {
      val files = folder.toFile.listFiles()
      if (files != null && files.nonEmpty) {
        files.foreach { file =>
          if (Files.isRegularFile(file.toPath)) {
            val fileName = file.getName.toLowerCase
            if (fileName.endsWith(".xml")) {
              return Some(file)
            }
          }
        }
      }
    }
    logger.warn("No export file found for pending import ID ["+pendingImportId+"]")
    None
  }

}