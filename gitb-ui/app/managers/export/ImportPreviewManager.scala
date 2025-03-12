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
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ImportPreviewManager @Inject()(exportManager: ExportManager,
                                     systemConfigurationManager: SystemConfigurationManager,
                                     communityManager: CommunityManager,
                                     dbConfigProvider: DatabaseConfigProvider)
                                    (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._
  import scala.jdk.CollectionConverters._

  private val logger = LoggerFactory.getLogger(classOf[ImportPreviewManager])
  private val VERSION_NUMBER_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+).*$")
  /*
  Contains in sequence the migrations to be applied (older versions to newer versions). For each such pair there has
  to be an XSLT schema/export/migrations/from_[FROM_VERSION]_to_[TO_VERSION].xslt.
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

  private def listBufferToNameMap[A](input: Iterable[A], nameFn: A => String): mutable.Map[String, A] = {
    val map = mutable.Map[String, A]()
    input.foreach { x =>
      map += (nameFn.apply(x) -> x)
    }
    map
  }

  private def listBufferToNonUniqueNameMap[A](input: Iterable[A], nameFn: A => String): mutable.Map[String, ListBuffer[A]] = {
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

  private def loadSystemResourceMap(): Future[Map[String, List[Long]]] = {
    DB.run(
      PersistenceSchema.communityResources
        .filter(_.community === Constants.DefaultCommunityId)
        .map(x => (x.name, x.id))
        .result
    ).map { result =>
      val targetResourceMap = mutable.Map[String, ListBuffer[Long]]()
      result.foreach { x =>
        if (!targetResourceMap.contains(x._1)) {
          targetResourceMap += (x._1 -> new ListBuffer[Long]())
        }
        targetResourceMap(x._1) += x._2
      }
      targetResourceMap.view.mapValues(_.toList).toMap
    }
  }

  private def loadThemeMap(): Future[Map[String, List[Long]]] = {
    DB.run(
      PersistenceSchema.themes
        .filter(_.custom === true)
        .result
    ).map { results =>
      val targetThemeMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        if (!targetThemeMap.contains(x.key)) {
          targetThemeMap += (x.key -> new ListBuffer[Long]())
        }
        targetThemeMap(x.key) += x.id
      }
      targetThemeMap.view.mapValues(_.toList).toMap
    }
  }

  private def loadSystemAdministratorMap(): Future[Map[String, models.Users]] = {
    exportManager.loadSystemAdministrators().map { admins =>
      val targetAdministratorsMap = mutable.Map[String, models.Users]()
      admins.foreach { admin =>
        targetAdministratorsMap += (admin.email -> admin)
      }
      targetAdministratorsMap.toMap
    }
  }

  private def loadDefaultLandingPages(): Future[Map[String, List[Long]]] = {
    DB.run(
      PersistenceSchema.landingPages
        .filter(_.community === Constants.DefaultCommunityId)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetLandingPageMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        if (!targetLandingPageMap.contains(x._1)) {
          targetLandingPageMap += (x._1 -> new ListBuffer[Long]())
        }
        targetLandingPageMap(x._1) += x._2
      }
      targetLandingPageMap.view.mapValues(_.toList).toMap
    }
  }

  private def loadDefaultLegalNotices(): Future[Map[String, List[Long]]] = {
    DB.run(
      PersistenceSchema.legalNotices
        .filter(_.community === Constants.DefaultCommunityId)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetLegalNoticeMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        if (!targetLegalNoticeMap.contains(x._1)) {
          targetLegalNoticeMap += (x._1 -> new ListBuffer[Long]())
        }
        targetLegalNoticeMap(x._1) += x._2
      }
      targetLegalNoticeMap.view.mapValues(_.toList).toMap
    }
  }

  private def loadDefaultErrorTemplates(): Future[Map[String, List[Long]]] = {
    DB.run(
      PersistenceSchema.errorTemplates
        .filter(_.community === Constants.DefaultCommunityId)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetErrorTemplateMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        if (!targetErrorTemplateMap.contains(x._1)) {
          targetErrorTemplateMap += (x._1 -> new ListBuffer[Long]())
        }
        targetErrorTemplateMap(x._1) += x._2
      }
      targetErrorTemplateMap.view.mapValues(_.toList).toMap
    }
  }

  private def loadSystemSettings(): Future[Set[String]] = {
    systemConfigurationManager.getEditableSystemConfigurationValues(onlyPersisted = true).map { settings =>
      val targetSystemSettingsMap = mutable.HashSet[String]()
      settings.foreach { setting =>
        targetSystemSettingsMap += setting.config.name
      }
      targetSystemSettingsMap.toSet
    }
  }

  private def loadSystemSettingsImportPreviewData(): Future[SystemSettingsImportPreviewData] = {
    loadSystemResourceMap().zip(
      loadThemeMap().zip(
        loadSystemAdministratorMap().zip(
          loadDefaultLandingPages().zip(
            loadDefaultLegalNotices().zip(
              loadDefaultErrorTemplates().zip(
                loadSystemSettings()
              )
            )
          )
        )
      )
    ).map { results =>
      SystemSettingsImportPreviewData(
        resourceMap = results._1,
        themeMap = results._2._1,
        systemAdministrators = results._2._2._1,
        defaultLandingPages = results._2._2._2._1,
        defaultLegalNotices = results._2._2._2._2._1,
        defaultErrorTemplates = results._2._2._2._2._2._1,
        systemSettings = results._2._2._2._2._2._2
      )
    }
  }

  private def toMutable[A, B](map: Map[A, B]): mutable.Map[A, B] = {
    mutable.Map.from(map)
  }

  private def toMutable[A, B](map: Option[Map[A, B]]): mutable.Map[A, B] = {
    mutable.Map.from(map.getOrElse(Map.empty))
  }

  private def toMutableMapOfLists[A, B](map: Map[A, List[B]]): mutable.Map[A, ListBuffer[B]] = {
    mutable.Map.from(map.view.mapValues(ListBuffer.from(_)))
  }

  private def toMutableOfLists[A, B](map: Option[Map[A, List[B]]]): mutable.Map[A, ListBuffer[B]] = {
    mutable.Map.from(map.getOrElse(Map.empty).view.mapValues(ListBuffer.from(_)))
  }

  private def toMutableMapOfMapsOfLists[A, B, C](map: Option[Map[A, Map[B, List[C]]]]): mutable.Map[A, mutable.Map[B, ListBuffer[C]]] = {
    mutable.Map.from(map.getOrElse(Map.empty).view.mapValues(x => mutable.Map.from(x.view.mapValues(ListBuffer.from(_)))))
  }

  private def toMutableOfMaps[A, B, C](map: Option[Map[A, Map[B, C]]]): mutable.Map[A, mutable.Map[B, C]] = {
    mutable.Map.from(map.getOrElse(Map.empty).view.mapValues(mutable.Map.from(_)))
  }

  private def previewSystemSettingsImportInternal(exportedSettings: com.gitb.xml.export.Settings, originalUserEmails: Set[String]): Future[(ImportItem, Set[String])] = {
    for {
      targets <- loadSystemSettingsImportPreviewData()
      result <- {
        // Create mutable versions of data
        val targetResourceMap = toMutableMapOfLists(targets.resourceMap)
        val targetThemeMap = toMutableMapOfLists(targets.themeMap)
        val targetLandingPageMap = toMutableMapOfLists(targets.defaultLandingPages)
        val targetLegalNoticeMap = toMutableMapOfLists(targets.defaultLegalNotices)
        val targetErrorTemplateMap = toMutableMapOfLists(targets.defaultErrorTemplates)
        val targetAdministratorsMap = toMutable(targets.systemAdministrators)
        val targetSystemSettings = mutable.Set.from(targets.systemSettings)
        val referenceUserEmails = mutable.Set.from(originalUserEmails)
        // Proceed with the preview calculation
        val importTargets = ImportTargets.fromSettings(exportedSettings)
        val importItemSettings = Some(new ImportItem(Some("System settings"), ImportItemType.Settings, ImportItemMatch.Both, None, None))
        if (importTargets.hasSettings) {
          // System resources.
          if (importTargets.hasSystemResources) {
            exportedSettings.getResources.getResource.asScala.foreach { exportedResource =>
              var targetResource: Option[Long] = None
              val foundContent = targetResourceMap.get(exportedResource.getName)
              if (foundContent.isDefined && foundContent.get.nonEmpty) {
                targetResource = Some(foundContent.get.remove(0))
                if (foundContent.get.isEmpty) {
                  targetResourceMap.remove(exportedResource.getName)
                }
              }
              if (targetResource.isDefined) {
                new ImportItem(Some(exportedResource.getName), ImportItemType.SystemResource, ImportItemMatch.Both, Some(targetResource.get.toString), Some(exportedResource.getId), importItemSettings)
              } else {
                new ImportItem(Some(exportedResource.getName), ImportItemType.SystemResource, ImportItemMatch.ArchiveOnly, None, Some(exportedResource.getId), importItemSettings)
              }
            }
          }
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
              if (targetSystemSettings.contains(exportedConfig.getName)) {
                targetSystemSettings.remove(exportedConfig.getName)
                new ImportItem(Some(nameForSystemConfiguration(exportedConfig.getName)), ImportItemType.SystemConfiguration, ImportItemMatch.Both, Some(exportedConfig.getName), Some(exportedConfig.getName), importItemSettings)
              } else {
                new ImportItem(Some(nameForSystemConfiguration(exportedConfig.getName)), ImportItemType.SystemConfiguration, ImportItemMatch.ArchiveOnly, None, Some(exportedConfig.getName), importItemSettings)
              }
            }
          }
        }
        // Mark items not found for deletion.
        targetResourceMap.foreach { entry =>
          entry._2.foreach { id =>
            new ImportItem(Some(entry._1), ImportItemType.SystemResource, ImportItemMatch.DBOnly, Some(id.toString), None, importItemSettings)
          }
        }
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
        targetSystemSettings.foreach { name =>
          new ImportItem(Some(nameForSystemConfiguration(name)), ImportItemType.SystemConfiguration, ImportItemMatch.DBOnly, Some(name), None, importItemSettings)
        }
        Future.successful(
          (importItemSettings.get, referenceUserEmails.toSet)
        )
      }
    } yield result
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

  private def loadDeletionsImportPreviewData(exportedDeletions: com.gitb.xml.export.Deletions): Future[DeletionsImportPreviewData] = {
    // Community deletions
    val communityDeletions = if (exportedDeletions.getCommunity.isEmpty) {
      Future.successful(Seq.empty)
    } else {
      DB.run(
        PersistenceSchema.communities
          .filter(_.apiKey inSet exportedDeletions.getCommunity.asScala)
          .map(x => (x.id, x.fullname))
          .sortBy(_._2.asc)
          .result
      )
    }
    // Domain deletions
    val domainDeletions = if (exportedDeletions.getDomain.isEmpty) {
      Future.successful(Seq.empty)
    } else {
      DB.run(
        PersistenceSchema.domains
          .filter(_.apiKey inSet exportedDeletions.getDomain.asScala)
          .map(x => (x.id, x.fullname))
          .sortBy(_._2.asc)
          .result
      )
    }
    // Prepare data.
    communityDeletions.zip(domainDeletions).map { results =>
      DeletionsImportPreviewData(
        communityDeletions = results._1,
        domainDeletions = results._2
      )
    }
  }

  private def loadCommunityImportPreviewData(community: Communities, domainImportInfo: DomainImportInfo): Future[CommunityImportPreviewData] = {
    /*
     * Prepare the futures to load the data.
     */
    // Administrators.
    val administrators = exportManager.loadCommunityAdministrators(community.id).map { results =>
      val targetAdministratorsMap = mutable.Map[String, models.Users]()
      results.foreach { x =>
        targetAdministratorsMap += (x.email -> x)
      }
      targetAdministratorsMap.toMap
    }
    // Organisation properties.
    val organisationProperties = exportManager.loadOrganisationProperties(community.id).map { results =>
      val targetOrganisationPropertyMap = mutable.Map[String, OrganisationParameters]()
      val targetOrganisationPropertyIdMap = mutable.Map[Long, OrganisationParameters]()
      results.foreach { x =>
        targetOrganisationPropertyMap += (x.testKey -> x)
        targetOrganisationPropertyIdMap += (x.id -> x)
      }
      (targetOrganisationPropertyMap.toMap, targetOrganisationPropertyIdMap.toMap)
    }
    // System properties.
    val systemProperties = exportManager.loadSystemProperties(community.id).map { results =>
      val targetSystemPropertyMap = mutable.Map[String, SystemParameters]()
      val targetSystemPropertyIdMap = mutable.Map[Long, SystemParameters]()
      results.foreach { x =>
        targetSystemPropertyMap += (x.testKey -> x)
        targetSystemPropertyIdMap += (x.id -> x)
      }
      (targetSystemPropertyMap.toMap, targetSystemPropertyIdMap.toMap)
    }
    // Custom labels.
    val customLabels = communityManager.getCommunityLabels(community.id).map { results =>
      val targetCustomLabelMap = mutable.Map[LabelType, models.CommunityLabels]()
      results.foreach { x =>
        targetCustomLabelMap += (LabelType.apply(x.labelType) -> x)
      }
      targetCustomLabelMap.toMap
    }
    // Landing pages.
    val landingPages = DB.run(
      PersistenceSchema.landingPages
        .filter(_.community === community.id)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetLandingPageMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        targetLandingPageMap.getOrElseUpdate(x._1, new ListBuffer[Long]()) += x._2
      }
      targetLandingPageMap.view.mapValues(_.toList).toMap
    }
    // Legal notices.
    val legalNotices = DB.run(
      PersistenceSchema.legalNotices
        .filter(_.community === community.id)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetLegalNoticeMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        targetLegalNoticeMap.getOrElseUpdate(x._1, new ListBuffer[Long]()) += x._2
      }
      targetLegalNoticeMap.view.mapValues(_.toList).toMap
    }
    // Error templates.
    val errorTemplates = DB.run(
      PersistenceSchema.errorTemplates
        .filter(_.community === community.id)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetErrorTemplateMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        targetErrorTemplateMap.getOrElseUpdate(x._1, new ListBuffer[Long]()) += x._2
      }
      targetErrorTemplateMap.view.mapValues(_.toList).toMap
    }
    // Triggers.
    val triggers = DB.run(
      PersistenceSchema.triggers
        .filter(_.community === community.id)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetTriggerMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        targetTriggerMap.getOrElseUpdate(x._1, new ListBuffer[Long]()) += x._2
      }
      targetTriggerMap.view.mapValues(_.toList).toMap
    }
    // Resources.
    val resources = DB.run(
      PersistenceSchema.communityResources
        .filter(_.community === community.id)
        .map(x => (x.name, x.id))
        .result
    ).map { results =>
      val targetResourceMap = mutable.Map[String, ListBuffer[Long]]()
      results.foreach { x =>
        targetResourceMap.getOrElseUpdate(x._1, new ListBuffer[Long]()) += x._2
      }
      targetResourceMap.view.mapValues(_.toList).toMap
    }
    // Organisations.
    val organisations = exportManager.loadOrganisations(community.id).map { results =>
      val targetOrganisationMap = mutable.Map[String, mutable.ListBuffer[models.Organizations]]()
      results.foreach { x =>
        targetOrganisationMap.getOrElseUpdate(x.shortname, new ListBuffer[Organizations]()) += x
      }
      targetOrganisationMap.view.mapValues(_.toList).toMap
    }
    // Users.
    val users = exportManager.loadOrganisationUserMap(community.id).map { results =>
      val targetOrganisationUserMap = mutable.Map[Long, mutable.Map[String, models.Users]]()
      results.foreach { x =>
        targetOrganisationUserMap += (x._1 -> listBufferToNameMap(x._2, { u => u.email }))
      }
      targetOrganisationUserMap.view.mapValues(_.toMap).toMap
    }
    // Organisation property values.
    val organisationPropertyValues = DB.run(
      PersistenceSchema.organisationParameterValues
        .join(PersistenceSchema.organizations).on(_.organisation === _.id)
        .join(PersistenceSchema.organisationParameters).on(_._1.parameter === _.id)
        .filter(_._1._2.adminOrganization === false)
        .filter(_._1._2.community === community.id)
        .map(x => (x._1._2.id, x._2.testKey, x._1._1))
        .result
    ).map { results =>
      val targetOrganisationPropertyValueMap = mutable.Map[Long, mutable.Map[String, models.OrganisationParameterValues]]()
      results.foreach { x =>
        targetOrganisationPropertyValueMap.getOrElseUpdate(x._1, mutable.Map[String, models.OrganisationParameterValues]()) += (x._2 -> x._3)
      }
      targetOrganisationPropertyValueMap.view.mapValues(_.toMap).toMap
    }
    // Systems
    val systems = exportManager.loadOrganisationSystemMap(community.id).map { results =>
      val targetSystemMap = mutable.Map[Long, mutable.Map[String, ListBuffer[models.Systems]]]()
      results.foreach { x =>
        targetSystemMap += (x._1 -> listBufferToNonUniqueNameMap(x._2, { s => s.shortname }))
      }
      targetSystemMap.view.mapValues(_.view.mapValues(_.toList).toMap).toMap
    }
    // System property values.
    val systemPropertyValues = DB.run(
      PersistenceSchema.systemParameterValues
        .join(PersistenceSchema.systems).on(_.system === _.id)
        .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
        .join(PersistenceSchema.systemParameters).on(_._1._1.parameter === _.id)
        .filter(_._1._2.adminOrganization === false)
        .filter(_._1._2.community === community.id)
        .map(x => (x._1._1._2.id, x._2.testKey, x._1._1._1))
        .result
    ).map { results =>
      val targetSystemPropertyValueMap = mutable.Map[Long, mutable.Map[String, models.SystemParameterValues]]()
      results.foreach { x =>
        targetSystemPropertyValueMap.getOrElseUpdate(x._1, mutable.Map[String, models.SystemParameterValues]()) += (x._2 -> x._3)
      }
      targetSystemPropertyValueMap.view.mapValues(_.toMap).toMap
    }
    // Statements.
    val statements = exportManager.loadSystemStatementsMap(community.id, community.domain).map { results =>
      val targetStatementMap = mutable.Map[Long, mutable.Map[Long, (models.Specifications, models.Actors)]]() // System to [actor_DB_ID] to (specification, actor)]    WAS: System to [actor name to (specification name, actor)]
      results.foreach { x =>
        val statements = targetStatementMap.getOrElseUpdate(x._1, mutable.Map[Long, (models.Specifications, models.Actors)]())
        x._2.foreach { y =>
          statements += (y._2.id -> y)
        }
      }
      targetStatementMap.view.mapValues(_.toMap).toMap
    }
    // Statement configurations.
    val statementConfigurations = exportManager.loadSystemConfigurationsMap(community).map { results =>
      val targetStatementConfigurationMap = mutable.Map[String, mutable.Map[String, models.Configs]]() // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID] to Parameter name to Configs
      results.foreach { x =>
        // [actor ID]_[endpoint ID]_[System ID]_[Parameter ID]
        targetStatementConfigurationMap += (x._1 -> listBufferToNameMap(x._2, { v => domainImportInfo.targetEndpointParameterIdMap(v.parameter).testKey }))
      }
      targetStatementConfigurationMap.view.mapValues(_.toMap).toMap
    }
    /*
     * Load the data using zip to load in parallel (where possible).
     */
    for {
      results <- {
        administrators.zip(
          organisationProperties.zip(
            systemProperties.zip(
              customLabels.zip(
                landingPages.zip(
                  legalNotices.zip(
                    errorTemplates.zip(
                      triggers.zip(
                        resources.zip(
                          organisations
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      }
      // Load dependent data
      data <- {
        val administrators = results._1
        val organisationProperties = results._2._1._1
        val organisationPropertyIds = results._2._1._2
        val systemProperties = results._2._2._1._1
        val systemPropertyIds = results._2._2._1._2
        val customLabels = results._2._2._2._1
        val landingPages = results._2._2._2._2._1
        val legalNotices = results._2._2._2._2._2._1
        val errorTemplates = results._2._2._2._2._2._2._1
        val triggers = results._2._2._2._2._2._2._2._1
        val resources = results._2._2._2._2._2._2._2._2._1
        val organisations = results._2._2._2._2._2._2._2._2._2
        // Load dependent data
        val usersTask = if (organisations.nonEmpty) {
          users.map(Some(_))
        } else {
          Future.successful(None)
        }
        val organisationPropertyValuesTask = if (organisations.nonEmpty && organisationProperties.nonEmpty) {
          organisationPropertyValues.map(Some(_))
        } else {
          Future.successful(None)
        }
        val systemsTask = if (organisations.nonEmpty) {
          systems.flatMap { systems =>
            val systemPropertyTask = if (systems.nonEmpty && systemProperties.nonEmpty) {
              systemPropertyValues.map(Some(_))
            } else {
              Future.successful(None)
            }
            val statementTask = if (systems.nonEmpty && domainImportInfo.targetActorToSpecificationMap.nonEmpty) {
              statements.map(Some(_))
            } else {
              Future.successful(None)
            }
            val statementConfigurationsTask = if (systems.nonEmpty && domainImportInfo.targetActorToSpecificationMap.nonEmpty && domainImportInfo.targetEndpointParameterIdMap.nonEmpty) {
              statementConfigurations.map(Some(_))
            } else {
              Future.successful(None)
            }
            systemPropertyTask.zip(
              statementTask.zip(
                statementConfigurationsTask
              )
            ).map { result =>
              (Some(systems), result._1, result._2._1, result._2._2)
            }
          }
        } else {
          Future.successful((None, None, None, None))
        }
        usersTask.zip(
          organisationPropertyValuesTask.zip(
            systemsTask
          )
        ).map { results =>
          val users = results._1.getOrElse(Map.empty[Long, Map[String, Users]])
          val organisationPropertyValues = results._2._1.getOrElse(Map.empty[Long, Map[String, OrganisationParameterValues]])
          val systems = results._2._2._1.getOrElse(Map.empty[Long, Map[String, List[Systems]]])
          val systemPropertyValues = results._2._2._2.getOrElse(Map.empty[Long, Map[String, SystemParameterValues]])
          val statements = results._2._2._3.getOrElse(Map.empty[Long, Map[Long, (Specifications, Actors)]])
          val statementConfigurations = results._2._2._4.getOrElse(Map.empty[String, Map[String, Configs]])
          // Return the final data to use
          CommunityImportPreviewData(
            administratorMap = administrators,
            organisationPropertyMap = organisationProperties,
            organisationPropertyIdMap = organisationPropertyIds,
            systemPropertyMap = systemProperties,
            systemPropertyIdMap = systemPropertyIds,
            customLabelMap = customLabels,
            landingPageMap = landingPages,
            legalNoticeMap = legalNotices,
            errorTemplateMap = errorTemplates,
            triggerMap = triggers,
            resourceMap = resources,
            organisationMap = organisations,
            organisationUserMap = users,
            organisationPropertiesValueMap = organisationPropertyValues,
            systemMap = systems,
            systemPropertyValueMap = systemPropertyValues,
            statementMap = statements,
            statementConfigurationMap = statementConfigurations
          )
        }
      }
    } yield data
  }

  private def loadDomainImportPreviewData(domainId: Long): Future[DomainImportPreviewData] = {
    /*
     * Prepare the futures to load the data.
     */
    // Specification groups
    val specificationGroupMap = DB.run(
      PersistenceSchema.specificationGroups
        .filter(_.domain === domainId)
        .result
    ).map { results =>
      val map = mutable.Map[String, models.SpecificationGroups]()
      results.foreach(x => {
        map += (x.shortname -> x)
      })
      map.toMap
    }
    // Specifications
    val specificationMaps = DB.run(
      PersistenceSchema.specifications
        .joinLeft(PersistenceSchema.specificationGroups).on(_.group === _.id)
        .filter(_._1.domain === domainId)
        .result
    ).map { results =>
      val specificationMap = mutable.Map[String, models.Specifications]()
      val specificationIdMap = mutable.Map[Long, models.Specifications]()
      results.foreach(x => {
        specificationMap += (specificationKey(x._1.shortname, x._2.map(_.shortname)) -> x._1)
        specificationIdMap += (x._1.id -> x._1)
      })
      (specificationMap.toMap, specificationIdMap.toMap)
    }
    // Shared test suites
    val sharedTestSuiteMap = DB.run(
      PersistenceSchema.testSuites
        .filter(_.domain === domainId)
        .filter(_.shared)
        .result
    ).map { results =>
      val map = mutable.Map[String, models.TestSuites]()
      results.foreach(x => map += (x.identifier -> x))
      map.toMap
    }
    // Specification test suites
    val specificationTestSuiteMap = exportManager.loadSpecificationTestSuiteMap(domainId).map { results =>
      val map = mutable.Map[Long, mutable.Map[String, models.TestSuites]]()
      results.foreach { x =>
        map += (x._1 -> listBufferToNameMap(x._2, { t => t.identifier }))
      }
      map.view.mapValues(_.toMap).toMap
    }
    // Specification actors
    val specificationActorMap = exportManager.loadSpecificationActorMap(domainId).map { results =>
      val map = mutable.Map[Long, mutable.Map[String, models.Actors]]()
      results.foreach { x =>
        map += (x._1 -> listBufferToNameMap(x._2, { a => a.actorId }))
      }
      map.view.mapValues(_.toMap).toMap
    }
    // Actor endpoints
    val actorEndpointMap = exportManager.loadActorEndpointMap(domainId).map { results =>
      val map = mutable.Map[Long, mutable.Map[String, models.Endpoints]]()
      results.foreach { x =>
        map += (x._1 -> listBufferToNameMap(x._2, { e => e.name }))
      }
      map.view.mapValues(_.toMap).toMap
    }
    // Endpoint parameters
    val endpointParameterMap = exportManager.loadEndpointParameterMap(domainId).map { results =>
      val map = mutable.Map[Long, mutable.Map[String, models.Parameters]]()
      results.foreach { x =>
        map += (x._1 -> listBufferToNameMap(x._2, { p => p.testKey }))
      }
      map.view.mapValues(_.toMap).toMap
    }
    // Domain parameters
    val domainParameterMap = DB.run(
      PersistenceSchema.domainParameters
        .filter(_.domain === domainId)
        .result
    ).map { results =>
      val map = mutable.Map[String, models.DomainParameter]()
      results.foreach { x =>
        map += (x.name -> x)
      }
      map.toMap
    }
    /*
     * Load the data using zip to load in parallel.
     */
    specificationGroupMap.zip(
      specificationMaps.zip(
        sharedTestSuiteMap.zip(
          specificationTestSuiteMap.zip(
            specificationActorMap.zip(
              actorEndpointMap.zip(
                endpointParameterMap.zip(
                  domainParameterMap
                )
              )
            )
          )
        )
      )
    ).map { results =>
      DomainImportPreviewData(
        specificationGroupMap = results._1,
        specificationMap = results._2._1._1,
        specificationIdMap = results._2._1._2,
        domainTestSuiteMap = results._2._2._1,
        specificationTestSuiteMap = results._2._2._2._1,
        specificationActorMap = results._2._2._2._2._1,
        actorEndpointMap = results._2._2._2._2._2._1,
        endpointParameterMap = results._2._2._2._2._2._2._1,
        domainParametersMap = results._2._2._2._2._2._2._2
      )
    }
  }

  private def previewDomainImportInternal(exportedDomain: com.gitb.xml.export.Domain, targetDomainId: Option[Long], canDoAdminOperations: Boolean, settings: ImportSettings, linkedToCommunity: Boolean): Future[(DomainImportInfo, Option[ImportItem])] = {
    for {
      importTargets <- Future.successful(ImportTargets.fromDomain(exportedDomain))
      targetDomain <- {
        if (targetDomainId.isDefined) {
          DB.run(
            PersistenceSchema.domains
              .filter(_.id === targetDomainId.get)
              .result
              .headOption
          )
        } else {
          Future.successful(None)
        }
      }
      importItemDomain <- {
        var importItemDomain: Option[ImportItem] = None
        if (targetDomain.isDefined && importTargets.hasDomain) {
          importItemDomain = Some(new ImportItem(Some(targetDomain.get.fullname), ImportItemType.Domain, ImportItemMatch.Both, Some(targetDomain.get.id.toString), Some(exportedDomain.getId)))
        } else if (targetDomain.isDefined && !importTargets.hasDomain) {
          importItemDomain = Some(new ImportItem(Some(targetDomain.get.fullname), ImportItemType.Domain, ImportItemMatch.DBOnly, Some(targetDomain.get.id.toString), None))
        } else if (targetDomain.isEmpty && importTargets.hasDomain) {
          val nameToUse = if (!linkedToCommunity && settings.fullNameReplacement.isDefined) {
            settings.fullNameReplacement.get
          } else {
            exportedDomain.getFullName
          }
          importItemDomain = Some(new ImportItem(Some(nameToUse), ImportItemType.Domain, ImportItemMatch.ArchiveOnly, None, Some(exportedDomain.getId)))
        }
        Future.successful(importItemDomain)
      }
      proceed <- Future.successful(canDoAdminOperations || importItemDomain.exists(_.itemMatch == ImportItemMatch.Both))
      data <- {
        if (proceed && targetDomain.isDefined) {
          loadDomainImportPreviewData(targetDomain.get.id).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      result <- {
        if (proceed) {
          /*
          Important:
          ----------
          Maps named target* are checked within the processing of the domain and will have elements removed when matched. The
          remaining values from these maps will be flagged for deletion.
          Maps named reference* are populated but never have elements removed from them. These are used to pass reference
          information back to the community preview processing.
           */
          val targetSpecificationMap = toMutable(data.map(_.specificationMap))
          val targetSpecificationGroupMap = toMutable(data.map(_.specificationGroupMap))
          val targetSpecificationIdMap = toMutable(data.map(_.specificationIdMap))
          val targetDomainTestSuiteMap = toMutable(data.map(_.domainTestSuiteMap))
          val targetSpecificationTestSuiteMap = toMutableOfMaps(data.map(_.specificationTestSuiteMap))
          val targetSpecificationActorMap = toMutableOfMaps(data.map(_.specificationActorMap))
          val targetActorEndpointMap = toMutableOfMaps(data.map(_.actorEndpointMap))
          val targetDomainParametersMap = toMutable(data.map(_.domainParametersMap))
          val targetEndpointParameterMap = toMutableOfMaps(data.map(_.endpointParameterMap))

          val referenceActorEndpointMap = toMutableOfMaps(data.map(_.actorEndpointMap))
          val referenceActorToSpecificationMap = mutable.Map.from(targetSpecificationActorMap.flatMap { case (specificationId, actorMap) =>
            actorMap.values.map { actor =>
              actor.id -> targetSpecificationIdMap(specificationId)
            }
          })
          val referenceEndpointParameterMap = toMutableOfMaps(data.map(_.endpointParameterMap))
          val referenceEndpointParameterIdMap = targetEndpointParameterMap.flatMap { case (_, parameterMap) =>
            parameterMap.values.map { parameter =>
              parameter.id -> parameter
            }
          }

          val importItemMapSpecification = mutable.Map[String, ImportItem]()
          val importItemMapActor = mutable.Map[String, ImportItem]()
          val importItemMapEndpoint = mutable.Map[String, ImportItem]()
          val actorXmlIdToImportItemMap = mutable.Map[String, ImportItem]()

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
            val groupImportItemMap = new mutable.HashMap[String, Option[ImportItem]]()
            // Groups.
            if (exportedDomain.getSpecificationGroups != null) {
              exportedDomain.getSpecificationGroups.getGroup.asScala.foreach { exportedGroup =>
                var targetGroup: Option[models.SpecificationGroups] = None
                var importItemGroup: Option[ImportItem] = None
                if (targetDomain.isDefined) {
                  targetGroup = targetSpecificationGroupMap.remove(exportedGroup.getShortName)
                }
                if (targetGroup.isDefined) {
                  importItemGroup = Some(new ImportItem(Some(targetGroup.get.fullname), ImportItemType.SpecificationGroup, ImportItemMatch.Both, Some(targetGroup.get.id.toString), Some(exportedGroup.getId), importItemDomain))
                } else {
                  importItemGroup = Some(new ImportItem(Some(exportedGroup.getFullName), ImportItemType.SpecificationGroup, ImportItemMatch.ArchiveOnly, None, Some(exportedGroup.getId), importItemDomain))
                }
                groupImportItemMap += (exportedGroup.getId -> importItemGroup)
              }
            }
            // Specifications.
            if (exportedDomain.getSpecifications != null) {
              exportedDomain.getSpecifications.getSpecification.asScala.foreach { exportedSpecification =>
                var targetSpecification: Option[models.Specifications] = None
                var importItemSpecification: Option[ImportItem] = None
                if (targetDomain.isDefined) {
                  val key = specificationKey(exportedSpecification.getShortName, Option(exportedSpecification.getGroup).map(_.getShortName))
                  targetSpecification = targetSpecificationMap.remove(key)
                }
                var parentItem = importItemDomain
                if (exportedSpecification.getGroup != null) {
                  parentItem = groupImportItemMap(exportedSpecification.getGroup.getId)
                }
                if (targetSpecification.isDefined) {
                  importItemSpecification = Some(new ImportItem(Some(targetSpecification.get.fullname), ImportItemType.Specification, ImportItemMatch.Both, Some(targetSpecification.get.id.toString), Some(exportedSpecification.getId), parentItem))
                  importItemMapSpecification += (targetSpecification.get.id.toString -> importItemSpecification.get)
                } else {
                  importItemSpecification = Some(new ImportItem(Some(exportedSpecification.getFullName), ImportItemType.Specification, ImportItemMatch.ArchiveOnly, None, Some(exportedSpecification.getId), parentItem))
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
                    val sourceId = importItemSpecification.get.sourceKey.get + "|" + exportedTestSuite.getId
                    if (targetTestSuite.isDefined) {
                      val targetId = importItemSpecification.get.targetKey.get + "|" + targetTestSuite.get.id
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
                    var importItemActor: Option[ImportItem] = None
                    if (targetSpecification.isDefined && targetSpecificationActorMap.contains(targetSpecification.get.id)) {
                      targetActor = targetSpecificationActorMap(targetSpecification.get.id).remove(exportedActor.getActorId)
                    }
                    if (targetActor.isDefined) {
                      importItemActor = Some(new ImportItem(Some(targetActor.get.name), ImportItemType.Actor, ImportItemMatch.Both, Some(targetActor.get.id.toString), Some(exportedActor.getId), importItemSpecification))
                      importItemMapActor += (targetActor.get.id.toString -> importItemActor.get)
                    } else {
                      importItemActor = Some(new ImportItem(Some(exportedActor.getName), ImportItemType.Actor, ImportItemMatch.ArchiveOnly, None, Some(exportedActor.getId), importItemSpecification))
                    }
                    actorXmlIdToImportItemMap += (exportedActor.getId -> importItemActor.get)
                    // Endpoints
                    if (exportedActor.getEndpoints != null) {
                      exportedActor.getEndpoints.getEndpoint.asScala.foreach { exportedEndpoint =>
                        var targetEndpoint: Option[models.Endpoints] = None
                        var importItemEndpoint: Option[ImportItem] = None
                        if (targetActor.isDefined && targetActorEndpointMap.contains(targetActor.get.id)) {
                          targetEndpoint = targetActorEndpointMap(targetActor.get.id).remove(exportedEndpoint.getName)
                        }
                        if (targetEndpoint.isDefined) {
                          importItemEndpoint = Some(new ImportItem(Some(targetEndpoint.get.name), ImportItemType.Endpoint, ImportItemMatch.Both, Some(targetEndpoint.get.id.toString), Some(exportedEndpoint.getId), importItemActor))
                          importItemMapEndpoint += (targetEndpoint.get.id.toString -> importItemEndpoint.get)
                        } else {
                          importItemEndpoint = Some(new ImportItem(Some(exportedEndpoint.getName), ImportItemType.Endpoint, ImportItemMatch.ArchiveOnly, None, Some(exportedEndpoint.getId), importItemActor))
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
              new ImportItem(Some(testSuite.fullname), ImportItemType.TestSuite, ImportItemMatch.DBOnly, Some(testSuite.id.toString), None, importItemMapSpecification.get(x._1.toString))
            }
          }
          targetSpecificationActorMap.foreach { entry =>
            entry._2.values.foreach { actor =>
              val item = new ImportItem(Some(actor.name), ImportItemType.Actor, ImportItemMatch.DBOnly, Some(actor.id.toString), None, importItemMapSpecification.get(entry._1.toString))
              importItemMapActor += (actor.id.toString -> item)
            }
          }
          targetActorEndpointMap.values.foreach { x =>
            x.values.foreach { endpoint =>
              val item = new ImportItem(Some(endpoint.name), ImportItemType.Endpoint, ImportItemMatch.DBOnly, Some(endpoint.id.toString), None, importItemMapActor.get(endpoint.actor.toString))
              importItemMapEndpoint += (endpoint.id.toString -> item)
            }
          }
          targetEndpointParameterMap.values.foreach { x =>
            x.values.foreach { parameter =>
              new ImportItem(Some(parameter.name), ImportItemType.EndpointParameter, ImportItemMatch.DBOnly, Some(parameter.id.toString), None, importItemMapEndpoint.get(parameter.endpoint.toString))
            }
          }
          Future.successful {
            (
              DomainImportInfo(
                referenceActorEndpointMap.view.mapValues(_.toMap).toMap,
                referenceEndpointParameterMap.view.mapValues(_.toMap).toMap,
                referenceActorToSpecificationMap.toMap,
                referenceEndpointParameterIdMap.toMap,
                actorXmlIdToImportItemMap.toMap
              ),
              importItemDomain
            )
          }
        } else {
          Future.successful {
            (DomainImportInfo(Map.empty, Map.empty, Map.empty, Map.empty, Map.empty), importItemDomain)
          }
        }
      }
    } yield result
  }

  private [export] def loadUserEmailSet(): Future[Set[String]] = {
    DB.run(
      PersistenceSchema.users
        .map(x => x.email)
        .result
    ).map { results =>
      results.filter(_ != null).map(_.toLowerCase).toSet
    }
  }

  def previewSystemSettingsImport(exportedSettings: com.gitb.xml.export.Settings): Future[(ImportItem, Set[String])] = {
    previewSystemSettingsImportInternal(exportedSettings, Set.empty)
  }

  def previewDomainImport(exportedDomain: com.gitb.xml.export.Domain, targetDomainId: Option[Long], canDoAdminOperations: Boolean, settings: ImportSettings): Future[ImportItem] = {
    previewDomainImportInternal(exportedDomain, targetDomainId, canDoAdminOperations, settings, linkedToCommunity = false).map(_._2.get)
  }

  def previewDeletionsImport(exportedDeletions: com.gitb.xml.export.Deletions): Future[List[ImportItem]] = {
    for {
      data <- loadDeletionsImportPreviewData(exportedDeletions)
      result <- {
        val importItems = new ListBuffer[ImportItem]
        // Communities
        data.communityDeletions.foreach { communityInfo =>
          importItems += new ImportItem(Some(communityInfo._2), ImportItemType.Community, ImportItemMatch.DBOnly, Some(communityInfo._1.toString), None)
        }
        // Domains
        data.domainDeletions.foreach { domainInfo =>
          importItems += new ImportItem(Some(domainInfo._2), ImportItemType.Domain, ImportItemMatch.DBOnly, Some(domainInfo._1.toString), None)
        }
        Future.successful(importItems.toList)
      }
    } yield result
  }

  def previewCommunityImport(exportedData: com.gitb.xml.export.Export, targetCommunityId: Option[Long], canDoAdminOperations: Boolean, settings: ImportSettings): Future[(Option[ImportItem], Option[ImportItem], Option[ImportItem])] = {
    for {
      targetCommunity <- {
        if (targetCommunityId.isDefined) {
          require(targetCommunityId.get != Constants.DefaultCommunityId, "The Test Bed's default community cannot be the target of an import")
          DB.run(
            PersistenceSchema.communities
              .filter(_.id === targetCommunityId.get)
              .result
              .headOption
          )
        } else {
          Future.successful(None)
        }
      }
      exportedCommunity <- Future.successful(exportedData.getCommunities.getCommunity.get(0))
      importTargets <- Future.successful(ImportTargets.fromCommunity(exportedCommunity))
      // Process first domain and get back reference maps.
      domainImportResult <- {
        var targetDomainId: Option[Long] = None
        if (targetCommunity.isDefined) {
          targetDomainId = targetCommunity.get.domain
        }
        previewDomainImportInternal(exportedCommunity.getDomain, targetDomainId, canDoAdminOperations, settings, linkedToCommunity = true)
      }
      domainImportInfo <- Future.successful(domainImportResult._1)
      importItemDomain <- Future.successful(domainImportResult._2)
      // Load user emails for uniqueness checks.
      referenceUserEmails <- {
        if (!Configurations.AUTHENTICATION_SSO_ENABLED && (importTargets.hasAdministrators || importTargets.hasOrganisationUsers || importTargets.hasSystemAdministrators)) {
          loadUserEmailSet()
        } else {
          Future.successful(Set.empty[String])
        }
      }
      // Process system settings. Do this here to use the loaded reference user emails.
      settingsImportResult <- {
        if (canDoAdminOperations && exportedData.getSettings != null) {
          previewSystemSettingsImportInternal(exportedData.getSettings, referenceUserEmails).map(x => (Some(x._1), x._2))
        } else {
          Future.successful((None, referenceUserEmails))
        }
      }
      referenceUserEmailsToUse <- Future.successful(settingsImportResult._2)
      importItemSettings <- Future.successful(settingsImportResult._1)
      // Load data for community processing
      data <- {
        if (targetCommunity.isDefined) {
          loadCommunityImportPreviewData(targetCommunity.get, domainImportInfo).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      // Process community.
      result <- {
        val targetAdministratorsMap = toMutable(data.map(_.administratorMap))
        val targetOrganisationPropertyMap = toMutable(data.map(_.organisationPropertyMap))
        val targetOrganisationPropertyIdMap = toMutable(data.map(_.organisationPropertyIdMap))
        val targetSystemPropertyMap = toMutable(data.map(_.systemPropertyMap))
        val targetSystemPropertyIdMap = toMutable(data.map(_.systemPropertyIdMap))
        val targetCustomLabelMap = toMutable(data.map(_.customLabelMap))
        val targetLandingPageMap = toMutableOfLists(data.map(_.landingPageMap))
        val targetLegalNoticeMap = toMutableOfLists(data.map(_.legalNoticeMap))
        val targetErrorTemplateMap = toMutableOfLists(data.map(_.errorTemplateMap))
        val targetTriggerMap = toMutableOfLists(data.map(_.triggerMap))
        val targetResourceMap = toMutableOfLists(data.map(_.resourceMap))
        val targetOrganisationMap = toMutableOfLists(data.map(_.organisationMap))
        val targetOrganisationPropertyValueMap = toMutableOfMaps(data.map(_.organisationPropertiesValueMap))
        val targetOrganisationUserMap = toMutableOfMaps(data.map(_.organisationUserMap))
        val targetSystemMap = toMutableMapOfMapsOfLists(data.map(_.systemMap))
        val targetSystemPropertyValueMap = toMutableOfMaps(data.map(_.systemPropertyValueMap))
        val targetStatementMap = toMutableOfMaps(data.map(_.statementMap))  // System to [actor_DB_ID] to (specification, actor)]    WAS: System to [actor name to (specification name, actor)]
        val targetStatementConfigurationMap = toMutableOfMaps(data.map(_.statementConfigurationMap)) // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID] to Parameter name to Configs

        val referenceUserEmails = mutable.Set.from(referenceUserEmailsToUse)
        val importItemMapOrganisation = mutable.Map[String, ImportItem]()
        val importItemMapSystem = mutable.Map[String, ImportItem]()
        val importItemMapStatement = mutable.Map[String, ImportItem]()

        val importItemCommunity = if (targetCommunity.isDefined) {
          Some(new ImportItem(Some(targetCommunity.get.fullname), ImportItemType.Community, ImportItemMatch.Both, Some(targetCommunity.get.id.toString), Some(exportedCommunity.getId)))
        } else {
          Some(new ImportItem(Some(settings.fullNameReplacement.getOrElse(exportedCommunity.getFullName)), ImportItemType.Community, ImportItemMatch.ArchiveOnly, None, Some(exportedCommunity.getId)))
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
            var importItemOrganisation: Option[ImportItem] = None
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
              importItemOrganisation = Some(new ImportItem(Some(targetOrganisation.get.fullname), ImportItemType.Organisation, ImportItemMatch.Both, Some(targetOrganisation.get.id.toString), Some(exportedOrganisation.getId), importItemCommunity))
              importItemMapOrganisation += (targetOrganisation.get.id.toString -> importItemOrganisation.get)
            } else {
              importItemOrganisation = Some(new ImportItem(Some(exportedOrganisation.getFullName), ImportItemType.Organisation, ImportItemMatch.ArchiveOnly, None, Some(exportedOrganisation.getId), importItemCommunity))
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
                var importItemSystem: Option[ImportItem] = None
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
                  importItemSystem = Some(new ImportItem(Some(targetSystem.get.fullname), ImportItemType.System, ImportItemMatch.Both, Some(targetSystem.get.id.toString), Some(exportedSystem.getId), importItemOrganisation))
                  importItemMapSystem += (targetSystem.get.id.toString -> importItemSystem.get)
                } else {
                  importItemSystem = Some(new ImportItem(Some(exportedSystem.getShortName), ImportItemType.System, ImportItemMatch.ArchiveOnly, None, Some(exportedSystem.getId), importItemOrganisation))
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
                    var importItemStatement: Option[ImportItem] = None
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
                      importItemStatement = Some(new ImportItem(Some(domainImportInfo.targetActorToSpecificationMap(targetStatement.get._2.id).fullname + " (" + targetStatement.get._2.name + ")"), ImportItemType.Statement, ImportItemMatch.Both, Some(targetStatement.get._1.id.toString + "_" + targetStatement.get._2.id.toString), Some(exportedStatement.getId), importItemSystem))
                      importItemMapStatement += ((targetStatement.get._1.id.toString + "_" + targetStatement.get._2.id.toString) -> importItemStatement.get)
                    } else {
                      importItemStatement = Some(new ImportItem(Some(exportedStatement.getActor.getSpecification.getFullName + "(" + exportedStatement.getActor.getName + ")"), ImportItemType.Statement, ImportItemMatch.ArchiveOnly, None, Some(exportedStatement.getId), importItemSystem))
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
            new ImportItem(Some(x._1), ImportItemType.OrganisationPropertyValue, ImportItemMatch.DBOnly, Some(x._2.organisation.toString + "_" + x._2.parameter.toString), None, importItemMapOrganisation.get(entry._1.toString))
          }
        }
        targetOrganisationUserMap.foreach { entry =>
          entry._2.values.foreach { user =>
            new ImportItem(Some(user.name), ImportItemType.OrganisationUser, ImportItemMatch.DBOnly, Some(user.id.toString), None, importItemMapOrganisation.get(entry._1.toString))
          }
        }
        targetSystemMap.foreach { entry =>
          entry._2.values.foreach { systems =>
            systems.foreach { system =>
              val item = new ImportItem(Some(system.fullname), ImportItemType.System, ImportItemMatch.DBOnly, Some(system.id.toString), None, importItemMapOrganisation.get(entry._1.toString))
              importItemMapSystem += (system.id.toString -> item)
            }
          }
        }
        targetSystemPropertyValueMap.foreach { x1 =>
          x1._2.foreach { x2 =>
            new ImportItem(Some(x2._1), ImportItemType.SystemPropertyValue, ImportItemMatch.DBOnly, Some(x2._2.system.toString + "_" + x2._2.parameter.toString), None, importItemMapSystem.get(x2._2.system.toString))
          }
        }
        targetStatementMap.foreach { x =>
          x._2.values.foreach { statement =>
            val specification = statement._1
            val actor = statement._2
            val item = new ImportItem(Some(s"${specification.fullname} (${actor.name})"), ImportItemType.Statement, ImportItemMatch.DBOnly, Some(s"${x._1}_${actor.id.toString}"), None, importItemMapSystem.get(x._1.toString))
            importItemMapStatement += (s"${x._1}_${actor.id}" -> item)
          }
        }
        targetStatementConfigurationMap.foreach { entry1 =>
          // Key is [Actor ID]_[Endpoint ID]_[System ID]_[Parameter ID]
          val keyParts = StringUtils.split(entry1._1, "_")
          entry1._2.foreach { entry2 =>
            val statementMapKey = keyParts(2) + "_" + keyParts(0) // This map has keys as [System ID]_[Actor ID]
            if (importItemMapStatement.contains(statementMapKey)) {
              // It is possible to have a configuration value without a conformance statement.
              val importItemStatement = importItemMapStatement.get(statementMapKey)
              new ImportItem(Some(entry2._1), ImportItemType.StatementConfiguration, ImportItemMatch.DBOnly, Some(entry1._1), None, importItemStatement)
            }
          }
        }
        Future.successful((importItemCommunity, importItemDomain, importItemSettings))
      }
    } yield result
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
        val factory = XMLUtils.getSecureTransformerFactory
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

  def prepareImportPreview(tempArchiveFile: File, importSettings: ImportSettings, requireDomain: Boolean, requireCommunity: Boolean, requireSettings: Boolean, requireDeletions: Boolean): Future[(Option[(Int, String)], Option[Export], Option[String], Option[Path])] = {
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
    Future.successful {
      (errorInformation, exportData, pendingImportId, tempFolder)
    }
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