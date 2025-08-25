/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers.export

import com.gitb.xml.export.{ReportType, SelfRegistrationRestriction => _, _}
import config.Configurations
import managers._
import managers.testsuite.TestSuitePaths
import managers.triggers.TriggerHelper
import models.Enums.ImportItemType.ImportItemType
import models.Enums.OverviewLevelType.OverviewLevelType
import models.Enums.TestSuiteReplacementChoice.PROCEED
import models.Enums._
import models.theme.ThemeFiles
import models.{TestCases, _}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils._

import java.io.{ByteArrayInputStream, File}
import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

@Singleton
class ImportCompleteManager @Inject()(systemConfigurationManager: SystemConfigurationManager,
                                      domainParameterManager: DomainParameterManager,
                                      communityResourceManager: CommunityResourceManager,
                                      domainManager: DomainManager,
                                      triggerManager: TriggerManager,
                                      triggerHelper: TriggerHelper,
                                      exportManager: ExportManager,
                                      communityManager: CommunityManager,
                                      specificationManager: SpecificationManager,
                                      actorManager: ActorManager,
                                      endpointManager: EndPointManager,
                                      parameterManager: ParameterManager,
                                      testSuiteManager: TestSuiteManager,
                                      landingPageManager: LandingPageManager,
                                      legalNoticeManager: LegalNoticeManager,
                                      errorTemplateManager: ErrorTemplateManager,
                                      organisationManager: OrganizationManager,
                                      systemManager: SystemManager,
                                      importPreviewManager: ImportPreviewManager,
                                      repositoryUtils: RepositoryUtils,
                                      reportManager: ReportManager,
                                      dbConfigProvider: DatabaseConfigProvider)
                                     (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  private def logger = LoggerFactory.getLogger("ImportCompleteManager")

  import dbConfig.profile.api._

  import scala.jdk.CollectionConverters._

  def completeDomainImport(exportedDomain: com.gitb.xml.export.Domain, importSettings: ImportSettings, importItems: List[ImportItem], targetDomainId: Option[Long], canAddOrDeleteDomain: Boolean): Future[Unit] = {
    // Load context
    val ctx = ImportContext(
      importSettings,
      toImportItemMaps(importItems, ImportItemType.Domain),
      ExistingIds.init(),
      ImportTargets.fromImportItems(importItems),
      mutable.Map[ImportItemType, mutable.Map[String, String]](),
      mutable.Map[Long, mutable.Map[String, Long]](),
      mutable.Map[Long, (Option[List[TestCases]], Map[String, (Long, Boolean)])](),
      mutable.ListBuffer[() => _](),
      mutable.ListBuffer[() => _]()
    )
    for {
      ctx <- {
        if (targetDomainId.isDefined) {
          loadExistingDomainData(ctx, targetDomainId.get)
        } else {
          Future.successful(ctx)
        }
      }
      _ <- DB.run(completeFileSystemFinalisation(ctx, completeDomainImportInternal(exportedDomain, ctx, canAddOrDeleteDomain, linkedToCommunity = false)).transactionally)
    } yield ()
  }

  def completeSystemSettingsImport(exportedSettings: com.gitb.xml.export.Settings, importSettings: ImportSettings, importItems: List[ImportItem], canManageSettings: Boolean, ownUserId: Option[Long]): Future[Unit] = {
    // Load context
    val ctx = ImportContext(
      importSettings,
      toImportItemMaps(importItems, ImportItemType.Settings),
      ExistingIds.init(),
      ImportTargets.fromImportItems(importItems),
      mutable.Map[ImportItemType, mutable.Map[String, String]](),
      mutable.Map[Long, mutable.Map[String, Long]](),
      mutable.Map[Long, (Option[List[TestCases]], Map[String, (Long, Boolean)])](),
      mutable.ListBuffer[() => _](),
      mutable.ListBuffer[() => _]()
    )
    loadExistingSystemSettingsData(canManageSettings, ctx).flatMap { data =>
      DB.run(completeFileSystemFinalisation(data._1, completeSystemSettingsImportInternal(exportedSettings, data._1, canManageSettings, ownUserId, new mutable.HashSet[String](), data._2)).transactionally)
    }
  }

  private def loadExistingDeletionsData(ctx: ImportContext): Future[ImportContext] = {
    for {
      ctx <- {
        loadIfApplicable(ctx.importTargets.hasDomain,
          () => DB.run(PersistenceSchema.domains.map(_.id).result)
        ).zip(
          loadIfApplicable(ctx.importTargets.hasCommunity,
            () => DB.run(PersistenceSchema.communities.map(_.id).result)
          )
        ).map { results =>
          val domains = results._1
          val communities = results._2
          domains.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Domain) += x.toString))
          communities.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Community) += x.toString))
          ctx
        }
      }
    } yield ctx
  }

  def completeDeletionsImport(exportedDeletions: com.gitb.xml.export.Deletions, importSettings: ImportSettings, importItems: List[ImportItem]): Future[Unit] = {
    if (exportedDeletions != null && (!exportedDeletions.getDomain.isEmpty || !exportedDeletions.getCommunity.isEmpty)) {
      val ctx = ImportContext(
        importSettings,
        toImportItemMaps(importItems, List(ImportItemType.Domain, ImportItemType.Community)),
        ExistingIds.init(),
        ImportTargets.fromImportItems(importItems),
        mutable.Map[ImportItemType, mutable.Map[String, String]](),
        mutable.Map[Long, mutable.Map[String, Long]](),
        mutable.Map[Long, (Option[List[TestCases]], Map[String, (Long, Boolean)])](),
        mutable.ListBuffer[() => _](),
        mutable.ListBuffer[() => _]()
      )
      for {
        // Load existing data
        ctx <- loadExistingDeletionsData(ctx)
        _ <- {
          val dbAction = for {
            // Domains
            _ <- {
              val dbActions = ListBuffer[DBIO[_]]()
              if (exportedDeletions.getDomain != null) {
                exportedDeletions.getDomain.asScala.foreach { domain =>
                  dbActions += processFromArchive(ImportItemType.Domain, domain, domain, ctx,
                    ImportCallbacks.set(
                      (_: String, _: ImportItem) => DBIO.successful(()),
                      (_: String, _: String, _: ImportItem) => DBIO.successful(())
                    )
                  )
                }
              }
              toDBIO(dbActions)
            }
            _ <- {
              processRemaining(ImportItemType.Domain, ctx,
                (targetKey: String, _: ImportItem) => {
                  domainManager.deleteDomainInternal(targetKey.toLong, ctx.onSuccessCalls)
                }
              )
            }
            // Communities
            _ <- {
              val dbActions = ListBuffer[DBIO[_]]()
              if (exportedDeletions.getCommunity != null) {
                exportedDeletions.getCommunity.asScala.foreach { community =>
                  dbActions += processFromArchive(ImportItemType.Community, community, community, ctx,
                    ImportCallbacks.set(
                      (_: String, _: ImportItem) => DBIO.successful(()),
                      (_: String, _: String, _: ImportItem) => DBIO.successful(())
                    )
                  )
                }
              }
              toDBIO(dbActions)
            }
            _ <- {
              processRemaining(ImportItemType.Community, ctx,
                (targetKey: String, _: ImportItem) => {
                  communityManager.deleteCommunityInternal(targetKey.toLong, ctx.onSuccessCalls)
                }
              )
            }
          } yield ()
          DB.run(completeFileSystemFinalisation(ctx, dbAction).transactionally)
        }
      } yield ()
    } else {
      Future.successful(())
    }
  }

  private def toImportItemMaps(importItems: List[ImportItem], itemTypes: List[ImportItemType]): ImportItemMaps = {
    if (itemTypes.isEmpty) {
      ImportItemMaps.empty()
    } else if (itemTypes.size == 1) {
      toImportItemMaps(importItems, itemType = itemTypes.head)
    } else {
      val maps = ImportItemMaps.empty()
      itemTypes.foreach { itemType =>
        maps.merge(toImportItemMaps(importItems, itemType = itemType, failIfNotFound = false))
      }
      maps
    }
  }

  private def toImportItemMaps(importItems: List[ImportItem], itemType: ImportItemType, failIfNotFound: Boolean = true): ImportItemMaps = {
    importItems.foreach { item =>
      if (item.itemType == itemType) {
        return ImportItemMaps(item.toSourceMap(), item.toTargetMap())
      }
    }
    if (failIfNotFound) {
      throw new IllegalArgumentException("Expected data from import items not found ["+itemType.id+"].")
    } else {
      ImportItemMaps.empty()
    }
  }

  private def mergeImportItemMaps(existingMap: ImportItemMaps, newMap: ImportItemMaps): Unit = {
    // Sources
    newMap.sourceMap.foreach { entry =>
      if (existingMap.sourceMap.contains(entry._1)) {
        existingMap.sourceMap.update(entry._1, entry._2)
      } else {
        existingMap.sourceMap += (entry._1 -> entry._2)
      }
    }
    // Targets
    newMap.targetMap.foreach { entry =>
      if (existingMap.targetMap.contains(entry._1)) {
        existingMap.targetMap.update(entry._1, entry._2)
      } else {
        existingMap.targetMap += (entry._1 -> entry._2)
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

  @scala.annotation.tailrec
  private def allParentItemsAvailableInDb(importItem: ImportItem, ctx: ImportContext): Boolean = {
    if (importItem.parentItem.isEmpty) {
      // No parent - consider ok.
      true
    } else {
      if (importItem.parentItem.get.sourceKey.isDefined) {
        if (isAvailableInDb(importItem.parentItem.get.sourceKey.get, importItem.parentItem.get.itemType, ctx)) {
          // The parent ID is defined in the DB - process now the other ancestors.
          allParentItemsAvailableInDb(importItem.parentItem.get, ctx)
        } else {
          false
        }
      } else if (importItem.parentItem.get.itemType.id == ImportItemType.Settings.id) {
        // The parent is not expected to be in the DB.
        true
      } else {
        // No source key for the parent. We should normally never reach this point.
        logger.warn("Item ["+importItem.sourceKey.get+"]["+importItem.itemType+"] being checked for processing of which the parent source is not available")
        false
      }
    }
  }

  private def processFromArchive[A](itemType: ImportItemType, data: A, itemId: String, ctx: ImportContext, importCallbacks: ImportCallbacks[A]): DBIO[_] = {
    var dbAction: Option[DBIO[_]] = None
    if (ctx.importItemMaps.sourceMap.contains(itemType)) {
      /*
       An import item type might be missing from the map if we have data that exists in the archive but is being forcibly
       skipped in the import process. An example are users being skipped when SSO is active or when they would represent new
       users but their email address is not unique.
       */
      val importItem = ctx.importItemMaps.sourceMap(itemType).get(itemId)
      if (importItem.isDefined) {
        if (importItem.get.itemChoice.get == ImportItemChoice.Proceed) {
          if (allParentItemsAvailableInDb(importItem.get, ctx)) {
            if (importItem.get.targetKey.isEmpty) {
              // Create
              dbAction = Some(for {
                newId <- {
                  val result = importCallbacks.fnCreate.apply(data, importItem.get)
                  result
                }
                _ <- {
                  // Maintain also a reference of all processed XML IDs to DB IDs (per type)
                  if (importCallbacks.fnCreatedIdHandle.isDefined) {
                    // Custom method to handle created IDs.
                    importCallbacks.fnCreatedIdHandle.get.apply(data, itemId, newId, importItem.get)
                  } else {
                    // Default handling methods.
                    // Assign the ID generated for this from the DB. This will be used for FK associations from children.
                    val idAsString = newId.toString
                    if (idAsString.nonEmpty) {
                      importItem.get.targetKey = Some(idAsString)
                      // Add to processed ID map.
                      addIdToProcessedIdMap(itemType, itemId, idAsString, ctx)
                    }
                  }
                  // Custom post-create method.
                  if (importCallbacks.fnPostCreate.isDefined) {
                    importCallbacks.fnPostCreate.get.apply(data, newId, importItem.get)
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
        } else if (importItem.get.itemChoice.get == ImportItemChoice.Skip || importItem.get.itemChoice.get == ImportItemChoice.SkipProcessChildren) {
          if (importItem.get.targetKey.isDefined) {
            if (importItem.get.itemChoice.get == ImportItemChoice.SkipProcessChildren && importCallbacks.fnSkipButProcessChildren.isDefined) {
              dbAction = Some(importCallbacks.fnSkipButProcessChildren.get.apply(data, importItem.get.targetKey.get, importItem.get))
            }
            // A skipped update - add it to the processed ID map if it exists in the DB.
            if (ctx.existingIds.map(itemType).contains(importItem.get.targetKey.get)) {
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
    }
    if (dbAction.isEmpty) {
      dbAction = Some(DBIO.successful(()))
    }
    dbAction.get
  }

  private def processRemaining(itemType: ImportItemType, ctx: ImportContext, fnDelete: (String, ImportItem) => DBIO[_]): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    val itemMap = ctx.importItemMaps.targetMap.get(itemType)
    if (itemMap.isDefined) {
      itemMap.get.values.foreach { item =>
        if (item.sourceKey.isEmpty && item.itemChoice.get == ImportItemChoice.Proceed) {
          // Delete - check also to see if this is one of the expected IDs.
          if (ctx.existingIds.map(itemType).contains(item.targetKey.get)) {
            dbActions += fnDelete.apply(item.targetKey.get, item)
            // We can also mark all children of this item as skipped as these will already have been deleted.
            item.markAllChildrenAsSkipped()
          }
        }
      }
    }
    toDBIO(dbActions)
  }

  private def determineDomainIdForCommunityUpdate(exportedCommunity: com.gitb.xml.export.Community, targetCommunity: Option[models.Communities], ctx: ImportContext): Option[Long] = {
    var domainId: Option[Long] = None
    if (exportedCommunity.getDomain != null) {
      if (ctx.processedIdMap.contains(ImportItemType.Domain)) {
        val processedDomainId = ctx.processedIdMap(ImportItemType.Domain).get(exportedCommunity.getDomain.getId)
        if (processedDomainId.isDefined) {
          domainId = Some(processedDomainId.get.toLong)
        }
      }
    } else if (targetCommunity.isDefined) {
      // The community may already have a domain defined (in the case of an update).
      domainId = targetCommunity.get.domain
    }
    domainId
  }

  private def propertyTypeToKind(propertyType: PropertyType, isDomainParameter: Boolean): String = {
    require(propertyType != null, "Enum value cannot be null")
    propertyType match {
      case PropertyType.BINARY => "BINARY"
      case PropertyType.SIMPLE => "SIMPLE"
      case PropertyType.SECRET =>
        if (isDomainParameter) {
          "HIDDEN"
        } else {
          "SECRET"
        }
      case _ => throw new IllegalArgumentException("Unknown enum value ["+propertyType+"]")
    }
  }

  private def propertyTypeToKind(propertyType: PropertyType): String = {
    propertyTypeToKind(propertyType, isDomainParameter = false)
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
      case com.gitb.xml.export.SelfRegistrationRestriction.NO_RESTRICTION => Enums.SelfRegistrationRestriction.NoRestriction.id.toShort
      case com.gitb.xml.export.SelfRegistrationRestriction.USER_EMAIL => Enums.SelfRegistrationRestriction.UserEmail.id.toShort
      case com.gitb.xml.export.SelfRegistrationRestriction.USER_EMAIL_DOMAIN => Enums.SelfRegistrationRestriction.UserEmailDomain.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+selfRegRestriction+"]")
    }
  }

  private def labelTypeToModel(labelType: com.gitb.xml.export.CustomLabelType): Short = {
    require(labelType != null, "Enum value cannot be null")
    labelType match {
      case CustomLabelType.DOMAIN => Enums.LabelType.Domain.id.toShort
      case CustomLabelType.SPECIFICATION => Enums.LabelType.Specification.id.toShort
      case CustomLabelType.SPECIFICATION_GROUP => Enums.LabelType.SpecificationGroup.id.toShort
      case CustomLabelType.SPECIFICATION_IN_GROUP => Enums.LabelType.SpecificationInGroup.id.toShort
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

  private def manageEncryptionIfNeeded(importSettings: ImportSettings, propertyType: PropertyType, value: Option[String]): Option[String] = {
    var result: Option[String] = None
    if (value.isDefined) {
      if (propertyType == PropertyType.SECRET) {
        // In transit the value is clear-text but encrypted with the export password.
        // When stored this needs to be encrypted with the master password.
        result = Some(MimeUtil.encryptString(decrypt(importSettings, value.get)))
      } else if (propertyType == PropertyType.BINARY) {
        // In transit this is a data URL that will be stored empty with a separate file.
        result = Some("")
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
      case _: Exception => throw new IllegalArgumentException("An encrypted value could not be successfully decrypted.")
    }
  }

  private def toModelConformanceOverCertificateSettingsWithMessages(exportedSettings: com.gitb.xml.export.ConformanceOverviewCertificateSettings, communityId: Long, ctx: ImportContext): ConformanceOverviewCertificateWithMessages = {
    val settings = models.ConformanceOverviewCertificate(
      0L, Option(exportedSettings.getTitle), exportedSettings.isAddTitle, exportedSettings.isAddMessage, exportedSettings.isAddResultOverview,
      exportedSettings.isAddStatementList, exportedSettings.isAddStatementDetails, exportedSettings.isAddDetails, exportedSettings.isAddSignature,
      exportedSettings.isAddPageNumbers, exportedSettings.isEnableAggregateLevel, exportedSettings.isEnableDomainLevel,
      exportedSettings.isEnableSpecificationGroupLevel, exportedSettings.isEnableSpecificationLevel, communityId
    )
    val messages = new ListBuffer[models.ConformanceOverviewCertificateMessage]
    if (exportedSettings.getMessages != null) {
      exportedSettings.getMessages.getMessage.forEach { exportedMessage =>
        val messageType = toModelConformanceOverviewCertificateMessageType(exportedMessage.getMessageType)
        var domainId: Option[Long] = None
        var groupId: Option[Long] = None
        var specificationId: Option[Long] = None
        var process = false
        messageType match {
          case OverviewLevelType.OrganisationLevel => process = true
          case OverviewLevelType.DomainLevel =>
            if (exportedMessage.getIdentifier != null) {
              domainId = getProcessedDbId(exportedMessage.getIdentifier.asInstanceOf[com.gitb.xml.export.ExportType].getId, ImportItemType.Domain, ctx)
              process = domainId.isDefined
            } else {
              process = true
            }
          case OverviewLevelType.SpecificationGroupLevel =>
            if (exportedMessage.getIdentifier != null) {
              groupId = getProcessedDbId(exportedMessage.getIdentifier.asInstanceOf[com.gitb.xml.export.ExportType].getId, ImportItemType.SpecificationGroup, ctx)
              process = groupId.isDefined
            } else {
              process = true
            }
          case OverviewLevelType.SpecificationLevel =>
            if (exportedMessage.getIdentifier != null) {
              specificationId = getProcessedDbId(exportedMessage.getIdentifier.asInstanceOf[com.gitb.xml.export.ExportType].getId, ImportItemType.Specification, ctx)
              process = specificationId.isDefined
            } else {
              process = true
            }
        }
        if (process) {
          messages += models.ConformanceOverviewCertificateMessage(
            0L, messageType.id.toShort, exportedMessage.getMessage, domainId, groupId, specificationId, None, communityId
          )
        }
      }
    }
    ConformanceOverviewCertificateWithMessages(settings, messages)
  }

  private def toModelConformanceOverviewCertificateMessageType(exportedType: com.gitb.xml.export.ConformanceOverviewCertificateMessageType): OverviewLevelType = {
    exportedType match {
      case com.gitb.xml.export.ConformanceOverviewCertificateMessageType.DOMAIN => OverviewLevelType.DomainLevel
      case com.gitb.xml.export.ConformanceOverviewCertificateMessageType.SPECIFICATION_GROUP => OverviewLevelType.SpecificationGroupLevel
      case com.gitb.xml.export.ConformanceOverviewCertificateMessageType.SPECIFICATION => OverviewLevelType.SpecificationLevel
      case _ => OverviewLevelType.OrganisationLevel
    }
  }

  private def toModelTestCases(exportedTestCases: List[com.gitb.xml.export.TestCase]): List[models.TestCases] = {
    val testCases = new ListBuffer[models.TestCases]()
    exportedTestCases.foreach { exportedTestCase =>
      testCases += models.TestCases(0L,
        exportedTestCase.getShortName, exportedTestCase.getFullName, exportedTestCase.getVersion, Option(exportedTestCase.getAuthors),
        Option(exportedTestCase.getOriginalDate), Option(exportedTestCase.getModificationDate), Option(exportedTestCase.getDescription),
        Option(exportedTestCase.getKeywords), exportedTestCase.getTestCaseType, "",
        Option(exportedTestCase.getTargetActors), None, exportedTestCase.getTestSuiteOrder, exportedTestCase.isHasDocumentation,
        Option(exportedTestCase.getDocumentation), exportedTestCase.getIdentifier,
        Option(exportedTestCase.isOptional).exists(_.booleanValue()), Option(exportedTestCase.isDisabled).exists(_.booleanValue()),
        Option(exportedTestCase.getTags), Option(exportedTestCase.getSpecReference), Option(exportedTestCase.getSpecDescription), Option(exportedTestCase.getSpecLink),
        Option(exportedTestCase.getGroup).map(_.getId.hashCode)
      )
    }
    testCases.toList
  }

  private def saveTestSuiteFiles(data: com.gitb.xml.export.TestSuite, item: ImportItem, domainId: Long, ctx: ImportContext): TestSuitePaths = {
    // File system operations
    val testSuiteData = Base64.decodeBase64(data.getData)
    if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      Using.resource(new ByteArrayInputStream(testSuiteData)) { input =>
        require(ClamAVClient.isCleanReply(virusScanner.scan(input)), "A virus was found in one of the imported test suites")
      }
    }
    val tempTestSuitePath = Paths.get(ctx.importSettings.dataFilePath.get.getParent.toFile.getAbsolutePath, "testcases", item.sourceKey.get+".zip")
    Files.createDirectories(tempTestSuitePath.getParent)
    FileUtils.writeByteArrayToFile(tempTestSuitePath.toFile, testSuiteData)
    // Extract test suite to target location.
    val testSuiteFileName = repositoryUtils.generateTestSuiteFileName()
    val targetFolder = repositoryUtils.getTestSuitePath(domainId, testSuiteFileName)
    val resourcePaths = repositoryUtils.extractTestSuiteFilesFromZipToFolder(targetFolder, tempTestSuitePath.toFile)
    TestSuitePaths(targetFolder, resourcePaths._1, resourcePaths._2)
  }

  private def toModelTestSuite(data: com.gitb.xml.export.TestSuite, domainId: Long, testSuiteFileName: String, hasTestCases: Boolean, testSuiteDefinitionPath: Option[String], shared: Boolean): models.TestSuites = {
    models.TestSuites(0L, data.getShortName, data.getFullName, data.getVersion, Option(data.getAuthors),
      Option(data.getOriginalDate), Option(data.getModificationDate), Option(data.getDescription), Option(data.getKeywords),
      testSuiteFileName, data.isHasDocumentation, Option(data.getDocumentation), data.getIdentifier, !hasTestCases, shared, domainId, testSuiteDefinitionPath,
      Option(data.getSpecReference), Option(data.getSpecDescription), Option(data.getSpecLink)
    )
  }

  private def toModelCustomLabel(data: com.gitb.xml.export.CustomLabel, communityId: Long): models.CommunityLabels = {
    models.CommunityLabels(communityId, labelTypeToModel(data.getLabelType),
      data.getSingularForm, data.getPluralForm, data.isFixedCasing
    )
  }

  private def toModelOrganisationParameter(data: com.gitb.xml.export.OrganisationProperty, communityId: Long, modelId: Option[Long]): models.OrganisationParameters = {
    var displayOrder: Short = 0
    if (data.getDisplayOrder != null) {
      displayOrder = data.getDisplayOrder.toShort
    }
    models.OrganisationParameters(modelId.getOrElse(0L), data.getLabel, data.getName, Option(data.getDescription), requiredToUse(data.isRequired),
      propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests, data.isInExports, data.isInSelfRegistration, data.isHidden, Option(data.getAllowedValues),
      displayOrder, Option(data.getDependsOn), Option(data.getDependsOnValue), Option(data.getDefaultValue), communityId
    )
  }

  private def toModelSystemParameter(data: com.gitb.xml.export.SystemProperty, communityId: Long, modelId: Option[Long]): models.SystemParameters = {
    var displayOrder: Short = 0
    if (data.getDisplayOrder != null) {
      displayOrder = data.getDisplayOrder.toShort
    }
    models.SystemParameters(modelId.getOrElse(0L), data.getLabel, data.getName, Option(data.getDescription), requiredToUse(data.isRequired),
      propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests, data.isInExports, data.isHidden, Option(data.getAllowedValues),
      displayOrder, Option(data.getDependsOn), Option(data.getDependsOnValue), Option(data.getDefaultValue), communityId
    )
  }

  private def toModelTheme(idToUse: Option[Long], data: com.gitb.xml.export.Theme): models.theme.Theme = {
    models.theme.Theme(idToUse.getOrElse(0L), data.getKey, Option(data.getDescription), data.isActive, custom = true,
      data.getSeparatorTitleColor, data.getModalTitleColor, data.getTableTitleColor, data.getCardTitleColor,
      data.getPageTitleColor, data.getHeadingColor, data.getTabLinkColor, data.getFooterTextColor,
      data.getHeaderBackgroundColor, data.getHeaderBorderColor, data.getHeaderSeparatorColor, data.getHeaderLogoPath,
      data.getFooterBackgroundColor, data.getFooterBorderColor, data.getFooterLogoPath, data.getFooterLogoDisplay, data.getFaviconPath,
      // Provide default values matching Bootstrap 5
      Option(data.getPrimaryButtonColor).getOrElse("#337ab7"), Option(data.getPrimaryButtonLabelColor).getOrElse("#FFFFFF"), Option(data.getPrimaryButtonHoverColor).getOrElse("#2b689c"), Option(data.getPrimaryButtonActiveColor).getOrElse("#296292"),
      Option(data.getSecondaryButtonColor).getOrElse("#6c757d"), Option(data.getSecondaryButtonLabelColor).getOrElse("#FFFFFF"), Option(data.getSecondaryButtonHoverColor).getOrElse("#5c636a"), Option(data.getSecondaryButtonActiveColor).getOrElse("#565e64")
    )
  }

  private def toModelThemeFiles(data: com.gitb.xml.export.Theme, ctx: ImportContext): models.theme.ThemeFiles = {
    // Header logo.
    var headerFile: Option[NamedFile] = None
    if (!systemConfigurationManager.isBuiltInThemeResource(data.getHeaderLogoPath)) {
      val fileToStore = dataUrlToTempFile(data.getHeaderLogoContent)
      ctx.onFailureCalls += (() => if (fileToStore.exists()) { FileUtils.deleteQuietly(fileToStore) })
      headerFile = Some(NamedFile(fileToStore, data.getHeaderLogoPath))
    }
    // Footer logo.
    var footerFile: Option[NamedFile] = None
    if (!systemConfigurationManager.isBuiltInThemeResource(data.getFooterLogoPath)) {
      val fileToStore = dataUrlToTempFile(data.getFooterLogoContent)
      ctx.onFailureCalls += (() => if (fileToStore.exists()) {
        FileUtils.deleteQuietly(fileToStore)
      })
      footerFile = Some(NamedFile(fileToStore, data.getFooterLogoPath))
    }
    // Favicon.
    var faviconFile: Option[NamedFile] = None
    if (!systemConfigurationManager.isBuiltInThemeResource(data.getFaviconPath)) {
      val fileToStore = dataUrlToTempFile(data.getFaviconContent)
      ctx.onFailureCalls += (() => if (fileToStore.exists()) {
        FileUtils.deleteQuietly(fileToStore)
      })
      faviconFile = Some(NamedFile(fileToStore, data.getFaviconPath))
    }
    ThemeFiles(headerFile, footerFile, faviconFile)
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

  private def toModelReportSetting(data: com.gitb.xml.export.CommunityReportSetting, communityId: Long): models.CommunityReportSettings = {
    models.CommunityReportSettings(toModelReportType(data.getReportType).id.toShort, data.isSignPdfs, data.isCustomPdfs, data.isCustomPdfsWithCustomXml, Option(data.getCustomPdfService), communityId)
  }

  private def toModelTestServiceType(data: com.gitb.xml.export.TestServiceType): models.Enums.TestServiceType.TestServiceType = {
    data match {
      case com.gitb.xml.export.TestServiceType.MESSAGING => models.Enums.TestServiceType.MessagingService
      case com.gitb.xml.export.TestServiceType.PROCESSING => models.Enums.TestServiceType.ProcessingService
      case com.gitb.xml.export.TestServiceType.VALIDATION => models.Enums.TestServiceType.ValidationService
      case _ => throw new IllegalArgumentException("Unknown test service type [%s]".formatted(data.value()))
    }
  }

  private def toModelTestServiceApiType(data: com.gitb.xml.export.TestServiceApiType): models.Enums.TestServiceApiType.TestServiceApiType = {
    data match {
      case com.gitb.xml.export.TestServiceApiType.SOAP => models.Enums.TestServiceApiType.SoapApi
      case com.gitb.xml.export.TestServiceApiType.REST => models.Enums.TestServiceApiType.RestApi
      case _ => throw new IllegalArgumentException("Unknown test service API type [%s]".formatted(data.value()))
    }
  }

  private def toModelTestServiceAuthTokenPasswordType(data: com.gitb.xml.export.TestServiceAuthTokenPasswordType): models.Enums.TestServiceAuthTokenPasswordType.TestServiceAuthTokenPasswordType = {
    data match {
      case com.gitb.xml.export.TestServiceAuthTokenPasswordType.DIGEST => models.Enums.TestServiceAuthTokenPasswordType.Digest
      case com.gitb.xml.export.TestServiceAuthTokenPasswordType.TEXT => models.Enums.TestServiceAuthTokenPasswordType.Text
      case _ => throw new IllegalArgumentException("Unknown test service auth token password type [%s]".formatted(data.value()))
    }
  }

  private def toModelTestService(data: com.gitb.xml.export.TestService, parameterId: Long, serviceId: Option[Long], importSettings: ImportSettings): models.TestService = {
    models.TestService(serviceId.getOrElse(0L), toModelTestServiceType(data.getServiceType).id.toShort,
      toModelTestServiceApiType(data.getApiType).id.toShort, Option(data.getIdentifier), Option(data.getVersion),
      Option(data.getAuthBasicUsername), Option(data.getAuthBasicPassword).map(decrypt(importSettings, _)),
      Option(data.getAuthTokenUsername), Option(data.getAuthTokenPassword).map(decrypt(importSettings, _)),
      Option(data.getAuthTokenPasswordType).map(toModelTestServiceAuthTokenPasswordType(_).id.toShort),
      parameterId
    )
  }

  private def toModelReportType(data: com.gitb.xml.export.ReportType): models.Enums.ReportType.ReportType = {
    val reportType = data match {
      case ReportType.CONFORMANCE_OVERVIEW =>
        models.Enums.ReportType.ConformanceOverviewReport
      case ReportType.CONFORMANCE_STATEMENT =>
        models.Enums.ReportType.ConformanceStatementReport
      case ReportType.TEST_CASE =>
        models.Enums.ReportType.TestCaseReport
      case ReportType.TEST_STEP =>
        models.Enums.ReportType.TestStepReport
      case ReportType.CONFORMANCE_STATEMENT_CERTIFICATE =>
        models.Enums.ReportType.ConformanceStatementCertificate
      case ReportType.CONFORMANCE_OVERVIEW_CERTIFICATE =>
        models.Enums.ReportType.ConformanceOverviewCertificate
      case _ => throw new IllegalArgumentException("Unknown report type [%s]".formatted(data.value()))
    }
    reportType
  }

  private def toModelTriggerEventType(eventType: com.gitb.xml.export.TriggerEventType): Short = {
    require(eventType != null, "Enum value cannot be null")
    eventType match {
      case com.gitb.xml.export.TriggerEventType.ORGANISATION_CREATED => Enums.TriggerEventType.OrganisationCreated.id.toShort
      case com.gitb.xml.export.TriggerEventType.ORGANISATION_UPDATED => Enums.TriggerEventType.OrganisationUpdated.id.toShort
      case com.gitb.xml.export.TriggerEventType.SYSTEM_CREATED => Enums.TriggerEventType.SystemCreated.id.toShort
      case com.gitb.xml.export.TriggerEventType.SYSTEM_UPDATED => Enums.TriggerEventType.SystemUpdated.id.toShort
      case com.gitb.xml.export.TriggerEventType.CONFORMANCE_STATEMENT_CREATED => Enums.TriggerEventType.ConformanceStatementCreated.id.toShort
      case com.gitb.xml.export.TriggerEventType.CONFORMANCE_STATEMENT_UPDATED => Enums.TriggerEventType.ConformanceStatementUpdated.id.toShort
      case com.gitb.xml.export.TriggerEventType.TEST_SESSION_SUCCEEDED => Enums.TriggerEventType.TestSessionSucceeded.id.toShort
      case com.gitb.xml.export.TriggerEventType.TEST_SESSION_FAILED => Enums.TriggerEventType.TestSessionFailed.id.toShort
      case com.gitb.xml.export.TriggerEventType.CONFORMANCE_STATEMENT_SUCCEEDED => Enums.TriggerEventType.ConformanceStatementSucceeded.id.toShort
      case com.gitb.xml.export.TriggerEventType.TEST_SESSION_STARTED => Enums.TriggerEventType.TestSessionStarted.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+eventType+"]")
    }
  }

  private def toModelTriggerServiceType(serviceType: com.gitb.xml.export.TriggerServiceType): Short = {
    require(serviceType != null, "Enum value cannot be null")
    serviceType match {
      case com.gitb.xml.export.TriggerServiceType.GITB => Enums.TriggerServiceType.GITB.id.toShort
      case com.gitb.xml.export.TriggerServiceType.JSON => Enums.TriggerServiceType.JSON.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+serviceType+"]")
    }
  }

  private def toModelTriggerFireExpressionType(expressionType: com.gitb.xml.export.TriggerFireExpressionType): Short = {
    require(expressionType != null, "Enum value cannot be null")
    expressionType match {
      case com.gitb.xml.export.TriggerFireExpressionType.TEST_CASE_IDENTIFIER => Enums.TriggerFireExpressionType.TestCaseIdentifier.id.toShort
      case com.gitb.xml.export.TriggerFireExpressionType.TEST_SUITE_IDENTIFIER => Enums.TriggerFireExpressionType.TestSuiteIdentifier.id.toShort
      case com.gitb.xml.export.TriggerFireExpressionType.ACTOR_IDENTIFIER => Enums.TriggerFireExpressionType.ActorIdentifier.id.toShort
      case com.gitb.xml.export.TriggerFireExpressionType.SPECIFICATION_NAME => Enums.TriggerFireExpressionType.SpecificationName.id.toShort
      case com.gitb.xml.export.TriggerFireExpressionType.SYSTEM_NAME => Enums.TriggerFireExpressionType.SystemName.id.toShort
      case com.gitb.xml.export.TriggerFireExpressionType.ORGANISATION_NAME => Enums.TriggerFireExpressionType.OrganisationName.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+expressionType+"]")
    }
  }

  private def toModelTriggerDataType(dataType: com.gitb.xml.export.TriggerDataType): Short = {
    require(dataType != null, "Enum value cannot be null")
    dataType match {
      case com.gitb.xml.export.TriggerDataType.COMMUNITY => Enums.TriggerDataType.Community.id.toShort
      case com.gitb.xml.export.TriggerDataType.ORGANISATION => Enums.TriggerDataType.Organisation.id.toShort
      case com.gitb.xml.export.TriggerDataType.SYSTEM => Enums.TriggerDataType.System.id.toShort
      case com.gitb.xml.export.TriggerDataType.SPECIFICATION => Enums.TriggerDataType.Specification.id.toShort
      case com.gitb.xml.export.TriggerDataType.ACTOR => Enums.TriggerDataType.Actor.id.toShort
      case com.gitb.xml.export.TriggerDataType.TEST_SESSION => Enums.TriggerDataType.TestSession.id.toShort
      case com.gitb.xml.export.TriggerDataType.TEST_REPORT => Enums.TriggerDataType.TestReport.id.toShort
      case com.gitb.xml.export.TriggerDataType.ORGANISATION_PARAMETER => Enums.TriggerDataType.OrganisationParameter.id.toShort
      case com.gitb.xml.export.TriggerDataType.SYSTEM_PARAMETER => Enums.TriggerDataType.SystemParameter.id.toShort
      case com.gitb.xml.export.TriggerDataType.DOMAIN_PARAMETER => Enums.TriggerDataType.DomainParameter.id.toShort
      case com.gitb.xml.export.TriggerDataType.STATEMENT_PARAMETER => Enums.TriggerDataType.StatementParameter.id.toShort
      case _ => throw new IllegalArgumentException("Unknown enum value ["+dataType+"]")
    }
  }

  private def toModelTrigger(modelTriggerId: Option[Long], data: com.gitb.xml.export.Trigger, communityId: Long, ctx: ImportContext): models.Trigger = {
    val modelTrigger = models.Triggers(modelTriggerId.getOrElse(0L), data.getName, Option(data.getDescription),
      data.getUrl, toModelTriggerEventType(data.getEventType), toModelTriggerServiceType(data.getServiceType),
      Option(data.getOperation), data.isActive, None, None, communityId)
    var modelDataItems: Option[List[models.TriggerData]] = None
    if (data.getDataItems != null) {
      val modelDataItemsToProcess = ListBuffer[models.TriggerData]()
      data.getDataItems.getTriggerDataItem.asScala.foreach { dataItem =>
        var dataId: Option[Long] = None
        if (dataItem.getDataType == com.gitb.xml.export.TriggerDataType.ORGANISATION_PARAMETER) {
          dataId = getProcessedDbId(dataItem.getData, ImportItemType.OrganisationProperty, ctx)
        } else if (dataItem.getDataType == com.gitb.xml.export.TriggerDataType.SYSTEM_PARAMETER) {
          dataId = getProcessedDbId(dataItem.getData, ImportItemType.SystemProperty, ctx)
        } else if (dataItem.getDataType == com.gitb.xml.export.TriggerDataType.DOMAIN_PARAMETER) {
          dataId = getProcessedDbId(dataItem.getData, ImportItemType.DomainParameter, ctx)
        } else if (dataItem.getDataType == com.gitb.xml.export.TriggerDataType.STATEMENT_PARAMETER) {
          dataId = getProcessedDbId(dataItem.getData, ImportItemType.EndpointParameter, ctx)
        } else {
          dataId = Some(-1)
        }
        // This might be referring to an organisation, system or domain property that does not exist
        if (dataId.isDefined) {
          modelDataItemsToProcess += models.TriggerData(toModelTriggerDataType(dataItem.getDataType), dataId.get, modelTriggerId.getOrElse(0L))
        }
      }
      modelDataItems = Some(modelDataItemsToProcess.toList)
    }
    var modelFireExpressions: Option[List[models.TriggerFireExpression]] = None
    if (data.getFireExpressions != null) {
      val expressions = ListBuffer[models.TriggerFireExpression]()
      data.getFireExpressions.getTriggerFireExpression.asScala.foreach { expression =>
        expressions += models.TriggerFireExpression(0L, expression.getExpression, toModelTriggerFireExpressionType(expression.getExpressionType), expression.isNotMatch, modelTriggerId.getOrElse(0L))
      }
      if (expressions.nonEmpty) {
        modelFireExpressions = Some(expressions.toList)
      }
    }
    new models.Trigger(modelTrigger, modelDataItems, modelFireExpressions)
  }

  private def toModelCommunityResource(data: com.gitb.xml.export.CommunityResource, communityId: Long): models.CommunityResources = {
    models.CommunityResources(0L, data.getName, Option(data.getDescription), communityId)
  }

  private def toModelSystemAdministrator(data: com.gitb.xml.export.SystemAdministrator, userId: Option[Long], organisationId: Long, importSettings: ImportSettings): models.Users = {
    toModelUser(data, userId, Enums.UserRole.SystemAdmin.id.toShort, organisationId, importSettings)
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

  private def isAvailableInDb(dataId: String, itemType: ImportItemType, ctx: ImportContext): Boolean = {
    dataId != null && ctx.processedIdMap.contains(itemType) && ctx.processedIdMap(itemType).contains(dataId)
  }

  private def getProcessedDbId(data: com.gitb.xml.export.ExportType, itemType: ImportItemType, ctx: ImportContext): Option[Long] = {
    if (data != null) {
      getProcessedDbId(data.getId, itemType, ctx)
    } else {
      None
    }
  }

  private def getProcessedDbId(dataId: String, itemType: ImportItemType, ctx: ImportContext): Option[Long] = {
    var dbId: Option[Long] = None
    if (dataId != null && ctx.processedIdMap.contains(itemType) && ctx.processedIdMap(itemType).contains(dataId)) {
      dbId = Some(ctx.processedIdMap(itemType)(dataId).toLong)
    }
    dbId
  }

  private def toModelTestCaseGroups(data: com.gitb.xml.export.TestSuite): Option[List[models.TestCaseGroup]] = {
    var exportedGroups: Option[List[models.TestCaseGroup]] = None
    if (data.getTestCaseGroups != null) {
      val groupsBuffer = ListBuffer[models.TestCaseGroup]()
      data.getTestCaseGroups.getTestCaseGroup.forEach { group =>
        groupsBuffer += models.TestCaseGroup(group.getId.hashCode, group.getIdentifier, Option(group.getName), Option(group.getDescription), 0L)
      }
      if (groupsBuffer.nonEmpty) {
        exportedGroups = Some(groupsBuffer.toList)
      }
    }
    exportedGroups
  }

  private def createSharedTestSuite(data: com.gitb.xml.export.TestSuite, ctx: ImportContext, item: ImportItem): DBIO[Long] = {
    val domainId = getDomainIdFromParentItem(item)
    // File system operations
    val testSuitePaths = saveTestSuiteFiles(data, item, domainId, ctx)
    ctx.onFailureCalls += (() => {
      // Cleanup operation in case an error occurred.
      if (testSuitePaths.testSuiteFolder.exists()) {
        FileUtils.deleteDirectory(testSuitePaths.testSuiteFolder)
      }
    })
    var testCases: List[com.gitb.xml.export.TestCase] = null
    if (data.getTestCases != null && data.getTestCases.getTestCase != null) {
      testCases = data.getTestCases.getTestCase.asScala.toList
    } else {
      testCases = List.empty
    }
    val testCasesToUse = Some(toModelTestCases(testCases))
    val updateActions = testSuiteUpdateActions(None, testCases)
    val groups = toModelTestCaseGroups(data)
    val action = for {
      // Save test suite and test cases.
      stepSaveTestSuite <- testSuiteManager.stepSaveTestSuiteAndTestCases(toModelTestSuite(data, domainId, testSuitePaths.testSuiteFolder.getName, testCases.nonEmpty, testSuitePaths.testSuiteDefinitionPath, shared = true), None, testCasesToUse, testSuitePaths, groups, updateActions)
      _ <- {
        // Needed to map specifications to shared test suites.
        ctx.sharedTestSuiteInfo += (stepSaveTestSuite.testSuite.id -> (testCasesToUse, stepSaveTestSuite.updatedTestCases))
        DBIO.successful(())
      }
    } yield stepSaveTestSuite.testSuite.id
    action
  }

  private def getDomainIdFromParentItem(item: ImportItem): Long = {
    val domainId = findDomainItem(item)
    if (domainId.nonEmpty && domainId.get.targetKey.nonEmpty) {
      domainId.get.targetKey.get.toLong
    } else {
      throw new IllegalStateException("Unable to determine domain ID for ["+item.itemType+"].")
    }
  }

  @scala.annotation.tailrec
  private def findDomainItem(item: ImportItem): Option[ImportItem] = {
    if (item.itemType == ImportItemType.Domain) {
      Some(item)
    } else if (item.parentItem.nonEmpty) {
      findDomainItem(item.parentItem.get)
    } else {
      None
    }
  }

  private def handleCommunityKeystore(exportedSettings: com.gitb.xml.export.SignatureSettings, communityId: Long, importSettings: ImportSettings): DBIO[_] = {
    if (exportedSettings == null) {
      // Delete
      communityManager.deleteCommunityKeystoreInternal(communityId)
    } else {
      // Update/Add
      communityManager.saveCommunityKeystoreInternal(communityId, exportedSettings.getKeystoreType.value(),
        Some(exportedSettings.getKeystore), Some(decrypt(importSettings, exportedSettings.getKeyPassword)),
        Some(decrypt(importSettings, exportedSettings.getKeystorePassword))
      )
    }
  }

  private def createTestSuite(data: com.gitb.xml.export.TestSuite, ctx: ImportContext, item: ImportItem): DBIO[Long] = {
    val domainId = getDomainIdFromParentItem(item)
    val specificationId = item.parentItem.get.targetKey.get.toLong
    // File system operations
    val testSuitePaths = saveTestSuiteFiles(data, item, domainId, ctx)
    ctx.onFailureCalls += (() => {
      // Cleanup operation in case an error occurred.
      if (testSuitePaths.testSuiteFolder.exists()) {
        FileUtils.deleteDirectory(testSuitePaths.testSuiteFolder)
      }
    })
    var testCases: List[com.gitb.xml.export.TestCase] = null
    if (data.getTestCases != null && data.getTestCases.getTestCase != null) {
      testCases = data.getTestCases.getTestCase.asScala.toList
    } else {
      testCases = List.empty
    }
    // Process DB operations
    val testCasesToUse = Some(toModelTestCases(testCases))
    val updateActions = testSuiteUpdateActions(Some(specificationId), testCases)
    val groups = toModelTestCaseGroups(data)
    val action = for {
      // Save test suite and test cases.
      stepSaveTestSuite <- testSuiteManager.stepSaveTestSuiteAndTestCases(toModelTestSuite(data, domainId, testSuitePaths.testSuiteFolder.getName, testCases.nonEmpty, testSuitePaths.testSuiteDefinitionPath, shared = false), None, testCasesToUse, testSuitePaths, groups, updateActions)
      _ <- {
        if (ctx.savedSpecificationActors.contains(specificationId)) {
          // Make all actor and specification updates.
          testSuiteManager.stepUpdateTestSuiteSpecificationLinks(specificationId, stepSaveTestSuite.testSuite.id, testCasesToUse,
            ctx.savedSpecificationActors(specificationId).asJava, // savedActorIds
            stepSaveTestSuite.updatedTestCases)
        } else {
          DBIO.successful(())
        }
      }
    } yield stepSaveTestSuite.testSuite.id
    action
  }

  private def testSuiteUpdateActions(specification: Option[Long], testCases: List[com.gitb.xml.export.TestCase]): TestSuiteDeploymentAction = {
    val sharedTestSuite = specification.isEmpty
    val updateActors = if (sharedTestSuite) {
      None
    } else {
      Some(true)
    }
    val testCaseUpdates = if (sharedTestSuite) {
      None
    } else {
      Some(testCases.map { testCase =>
        new TestCaseDeploymentAction(testCase.getIdentifier, updateDefinition = Some(true), resetTestHistory = Some(false))
      })
    }
    new TestSuiteDeploymentAction(specification, PROCEED, updateTestSuite = true, updateActors, sharedTestSuite, testCaseUpdates)
  }

  private def updateSharedTestSuite(data: com.gitb.xml.export.TestSuite, ctx: ImportContext, item: ImportItem): DBIO[_] = {
    val domainId = getDomainIdFromParentItem(item)
    val testSuiteId = item.targetKey.get.toLong
    // File system operations
    val testSuitePaths = saveTestSuiteFiles(data, item, domainId, ctx)
    var testCases: List[com.gitb.xml.export.TestCase] = null
    if (data.getTestCases != null && data.getTestCases.getTestCase != null) {
      testCases = data.getTestCases.getTestCase.asScala.toList
    } else {
      testCases = List.empty
    }
    // Process DB operations
    val updateActions = testSuiteUpdateActions(None, testCases)
    val testCasesToUse = Some(toModelTestCases(testCases))
    val groups = toModelTestCaseGroups(data)
    val action = for {
      // Lookup existing test suite file (for later cleanup).
      existingTestSuiteFile <- PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(x => x.filename).result.head
      // Update existing test suite and test cases.
      stepSaveTestSuite <- testSuiteManager.stepSaveTestSuiteAndTestCases(toModelTestSuite(data, domainId, testSuitePaths.testSuiteFolder.getName, testCases.nonEmpty, testSuitePaths.testSuiteDefinitionPath, shared = true), Some(testSuiteId), testCasesToUse, testSuitePaths, groups, updateActions)
      _ <- {
        // Needed to map specifications to shared test suites.
        ctx.sharedTestSuiteInfo += (stepSaveTestSuite.testSuite.id -> (testCasesToUse, stepSaveTestSuite.updatedTestCases))
        DBIO.successful(())
      }
    } yield existingTestSuiteFile
    action.flatMap(existingTestSuiteFile => {
      ctx.onSuccessCalls += (() => {
        // Finally, delete the backup folder
        val existingTestSuiteFolder = repositoryUtils.getTestSuitePath(domainId, existingTestSuiteFile)
        if (existingTestSuiteFolder != null && existingTestSuiteFolder.exists()) {
          FileUtils.deleteDirectory(existingTestSuiteFolder)
        }
      })
      ctx.onFailureCalls += (() => {
        // Cleanup operations in case an error occurred.
        if (testSuitePaths.testSuiteFolder.exists()) {
          FileUtils.deleteDirectory(testSuitePaths.testSuiteFolder)
        }
      })
      DBIO.successful(())
    })
  }

  private def updateTestSuite(data: com.gitb.xml.export.TestSuite, ctx: ImportContext, item: ImportItem): DBIO[_] = {
    val domainId = getDomainIdFromParentItem(item)
    val specificationId = item.parentItem.get.targetKey.get.toLong
    val testSuiteId = item.targetKey.get.toLong
    // File system operations
    val testSuitePaths = saveTestSuiteFiles(data, item, domainId, ctx)
    var testCases: List[com.gitb.xml.export.TestCase] = null
    if (data.getTestCases != null && data.getTestCases.getTestCase != null) {
      testCases = data.getTestCases.getTestCase.asScala.toList
    } else {
      testCases = List.empty
    }
    // Process DB operations
    val updateActions = testSuiteUpdateActions(Some(specificationId), testCases)
    val testCasesToUse = Some(toModelTestCases(testCases))
    val groups = toModelTestCaseGroups(data)
    val action = for {
      // Lookup existing test suite file (for later cleanup).
      existingTestSuiteFile <- PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(x => x.filename).result.head
      // Update existing test suite and test cases.
      stepSaveTestSuite <- testSuiteManager.stepSaveTestSuiteAndTestCases(toModelTestSuite(data, domainId, testSuitePaths.testSuiteFolder.getName, testCases.nonEmpty, testSuitePaths.testSuiteDefinitionPath, shared = false), Some(testSuiteId), testCasesToUse, testSuitePaths, groups, updateActions)
      // Specification-related updates.
      _ <- if (ctx.savedSpecificationActors.contains(specificationId)) {
          testSuiteManager.stepUpdateTestSuiteSpecificationLinks(specificationId, testSuiteId, testCasesToUse,
            ctx.savedSpecificationActors(specificationId).asJava, // savedActorIds
            stepSaveTestSuite.updatedTestCases)
      } else {
        DBIO.successful(())
      }
    } yield existingTestSuiteFile
    action.flatMap(existingTestSuiteFile => {
      ctx.onSuccessCalls += (() => {
        // Finally, delete the backup folder
        val existingTestSuiteFolder = repositoryUtils.getTestSuitePath(domainId, existingTestSuiteFile)
        if (existingTestSuiteFolder != null && existingTestSuiteFolder.exists()) {
          FileUtils.deleteDirectory(existingTestSuiteFolder)
        }
      })
      ctx.onFailureCalls += (() => {
        // Cleanup operations in case an error occurred.
        if (testSuitePaths.testSuiteFolder.exists()) {
          FileUtils.deleteDirectory(testSuitePaths.testSuiteFolder)
        }
      })
      DBIO.successful(())
    })
  }

  private def loadExistingSystemSettingsData(canManageSettings: Boolean, ctx: ImportContext): Future[(ImportContext, Option[Long])] = {
    if (canManageSettings) {
      for {
        // Load existing values.
        systemAdminOrganisationId <- {
          // System resources
          loadIfApplicable(ctx.importTargets.hasSystemResources,
            () => DB.run(PersistenceSchema.communityResources.filter(_.community === Constants.DefaultCommunityId).map(_.id).result)
          ).zip(
            // Themes
            loadIfApplicable(ctx.importTargets.hasThemes,
              () => DB.run(PersistenceSchema.themes.filter(_.custom === true).map(_.id).result)
            )
          ).zip(
            // Default landing pages
            loadIfApplicable(ctx.importTargets.hasDefaultLandingPages,
              () => DB.run(PersistenceSchema.landingPages.filter(_.community === Constants.DefaultCommunityId).map(x => x.id).result)
            )
          ).zip(
            // Default legal notices
            loadIfApplicable(ctx.importTargets.hasDefaultLegalNotices,
              () => DB.run(PersistenceSchema.legalNotices.filter(_.community === Constants.DefaultCommunityId).map(x => x.id).result)
            )
          ).zip(
            // Default error templates
            loadIfApplicable(ctx.importTargets.hasDefaultErrorTemplates,
              () => DB.run(PersistenceSchema.errorTemplates.filter(_.community === Constants.DefaultCommunityId).map(x => x.id).result)
            )
          ).zip(
            // System administrators
            loadIfApplicable(!Configurations.AUTHENTICATION_SSO_ENABLED && ctx.importTargets.hasSystemAdministrators,
              () => exportManager.loadSystemAdministrators()
            )
          ).zip(
            // System configurations
            loadIfApplicable(ctx.importTargets.hasSystemConfigurations,
              () => systemConfigurationManager.getEditableSystemConfigurationValues(onlyPersisted = true)
            )
          ).map { results =>
            val systemResources = results._1._1._1._1._1._1
            val themes = results._1._1._1._1._1._2
            val defaultLandingPages = results._1._1._1._1._2
            val defaultLegalNotices = results._1._1._1._2
            val defaultErrorTemplates = results._1._1._2
            val systemAdministrators = results._1._2
            val systemConfigurations = results._2
            var systemAdminOrganisationId: Option[Long] = None
            systemResources.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.SystemResource) += x.toString))
            themes.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Theme) += x.toString))
            defaultLandingPages.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.DefaultLandingPage) += x.toString))
            defaultLegalNotices.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.DefaultLegalNotice) += x.toString))
            defaultErrorTemplates.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.DefaultErrorTemplate) += x.toString))
            systemAdministrators.foreach(admins => {
              admins.foreach(x => ctx.existingIds.map(ImportItemType.SystemAdministrator) += x.id.toString)
              systemAdminOrganisationId = admins.headOption.map(_.organization)
            })
            systemConfigurations.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.SystemConfiguration) += x.config.name))
            systemAdminOrganisationId
          }
        }
      } yield (ctx, systemAdminOrganisationId)
    } else {
      Future.successful((ctx, None))
    }
  }

  private def completeSystemSettingsImportInternal(exportedSettings: com.gitb.xml.export.Settings, ctx: ImportContext, canManageSettings: Boolean, ownUserId: Option[Long], referenceUserEmails: mutable.Set[String], systemAdminOrganisationId: Option[Long]): DBIO[_] = {
    if (canManageSettings) {
      for {
        // Resources
        _ <- {
          val dbActions = ListBuffer[DBIO[_]]()
          if (exportedSettings.getResources != null) {
            exportedSettings.getResources.getResource.asScala.foreach { exportedContent =>
              dbActions += processFromArchive(ImportItemType.SystemResource, exportedContent, exportedContent.getId, ctx,
                ImportCallbacks.set(
                  (data: com.gitb.xml.export.CommunityResource, item: ImportItem) => {
                    val fileToStore = dataUrlToTempFile(data.getContent)
                    ctx.onFailureCalls += (() => if (fileToStore.exists()) { FileUtils.deleteQuietly(fileToStore) })
                    communityResourceManager.createCommunityResourceInternal(toModelCommunityResource(data, Constants.DefaultCommunityId), fileToStore, ctx.onSuccessCalls)
                  },
                  (data: com.gitb.xml.export.CommunityResource, targetKey: String, item: ImportItem) => {
                    val fileToStore = dataUrlToTempFile(data.getContent)
                    ctx.onFailureCalls += (() => if (fileToStore.exists()) { FileUtils.deleteQuietly(fileToStore) })
                    communityResourceManager.updateCommunityResourceInternal(Some(Constants.DefaultCommunityId), targetKey.toLong, Some(data.getName), Some(Option(data.getDescription)), Some(fileToStore), ctx.onSuccessCalls)
                  }
                )
              )
            }
          }
          toDBIO(dbActions)
        }
        _ <- {
          processRemaining(ImportItemType.SystemResource, ctx,
            (targetKey: String, item: ImportItem) => {
              communityResourceManager.deleteCommunityResourceInternal(Some(Constants.DefaultCommunityId), targetKey.toLong, ctx.onSuccessCalls)
            }
          )
        }
        // Themes
        _ <- {
          val dbActions = ListBuffer[DBIO[_]]()
          if (exportedSettings.getThemes != null) {
            exportedSettings.getThemes.getTheme.asScala.foreach { theme =>
              dbActions += processFromArchive(ImportItemType.Theme, theme, theme.getId, ctx,
                ImportCallbacks.set(
                  (data: com.gitb.xml.export.Theme, item: ImportItem) => {
                    // Only allow the active theme to be changed if this is a sandbox import
                    systemConfigurationManager.createThemeInternal(None, toModelTheme(None, data), toModelThemeFiles(data, ctx), canActivateTheme = ctx.importSettings.sandboxImport, ctx.onSuccessCalls)
                  },
                  (data: com.gitb.xml.export.Theme, targetKey: String, item: ImportItem) => {
                    // Only allow the active theme to be changed if this is a sandbox import
                    systemConfigurationManager.updateThemeInternal(toModelTheme(Some(targetKey.toLong), data), toModelThemeFiles(data, ctx), canActivateTheme = ctx.importSettings.sandboxImport, ctx.onSuccessCalls)
                  }
                )
              )
            }
          }
          toDBIO(dbActions)
        }
        _ <- {
          processRemaining(ImportItemType.Theme, ctx,
            (targetKey: String, item: ImportItem) => {
              systemConfigurationManager.deleteThemeInternal(targetKey.toLong, ctx.onSuccessCalls)
            }
          )
        }
        // Landing pages
        _ <- {
          val dbActions = ListBuffer[DBIO[_]]()
          if (exportedSettings.getLandingPages != null) {
            exportedSettings.getLandingPages.getLandingPage.asScala.foreach { exportedContent =>
              dbActions += processFromArchive(ImportItemType.DefaultLandingPage, exportedContent, exportedContent.getId, ctx,
                ImportCallbacks.set(
                  (data: com.gitb.xml.export.LandingPage, item: ImportItem) => {
                    landingPageManager.createLandingPageInternal(toModelLandingPage(data, Constants.DefaultCommunityId))
                  },
                  (data: com.gitb.xml.export.LandingPage, targetKey: String, item: ImportItem) => {
                    landingPageManager.updateLandingPageInternal(targetKey.toLong, data.getName, Option(data.getDescription), data.getContent, data.isDefault, Constants.DefaultCommunityId)
                  }
                )
              )
            }
          }
          toDBIO(dbActions)
        }
        _ <- {
          processRemaining(ImportItemType.DefaultLandingPage, ctx,
            (targetKey: String, item: ImportItem) => {
              landingPageManager.deleteLandingPageInternal(targetKey.toLong)
            }
          )
        }
        // Legal notices
        _ <- {
          val dbActions = ListBuffer[DBIO[_]]()
          if (exportedSettings.getLegalNotices != null) {
            exportedSettings.getLegalNotices.getLegalNotice.asScala.foreach { exportedContent =>
              dbActions += processFromArchive(ImportItemType.DefaultLegalNotice, exportedContent, exportedContent.getId, ctx,
                ImportCallbacks.set(
                  (data: com.gitb.xml.export.LegalNotice, item: ImportItem) => {
                    legalNoticeManager.createLegalNoticeInternal(toModelLegalNotice(data, Constants.DefaultCommunityId))
                  },
                  (data: com.gitb.xml.export.LegalNotice, targetKey: String, item: ImportItem) => {
                    legalNoticeManager.updateLegalNoticeInternal(targetKey.toLong, data.getName, Option(data.getDescription), data.getContent, data.isDefault, Constants.DefaultCommunityId)
                  }
                )
              )
            }
          }
          toDBIO(dbActions)
        }
        _ <- {
          processRemaining(ImportItemType.DefaultLegalNotice, ctx,
            (targetKey: String, item: ImportItem) => {
              legalNoticeManager.deleteLegalNoticeInternal(targetKey.toLong)
            }
          )
        }
        // Error templates
        _ <- {
          val dbActions = ListBuffer[DBIO[_]]()
          if (exportedSettings.getErrorTemplates != null) {
            exportedSettings.getErrorTemplates.getErrorTemplate.asScala.foreach { exportedContent =>
              dbActions += processFromArchive(ImportItemType.DefaultErrorTemplate, exportedContent, exportedContent.getId, ctx,
                ImportCallbacks.set(
                  (data: com.gitb.xml.export.ErrorTemplate, item: ImportItem) => {
                    errorTemplateManager.createErrorTemplateInternal(toModelErrorTemplate(data, Constants.DefaultCommunityId))
                  },
                  (data: com.gitb.xml.export.ErrorTemplate, targetKey: String, item: ImportItem) => {
                    errorTemplateManager.updateErrorTemplateInternal(targetKey.toLong, data.getName, Option(data.getDescription), data.getContent, data.isDefault, Constants.DefaultCommunityId)
                  }
                )
              )
            }
          }
          toDBIO(dbActions)
        }
        _ <- {
          processRemaining(ImportItemType.DefaultErrorTemplate, ctx,
            (targetKey: String, item: ImportItem) => {
              errorTemplateManager.deleteErrorTemplateInternal(targetKey.toLong)
            }
          )
        }
        // Administrators
        _ <- {
          val dbActions = ListBuffer[DBIO[_]]()
          if (!Configurations.AUTHENTICATION_SSO_ENABLED && exportedSettings.getAdministrators != null) {
            exportedSettings.getAdministrators.getAdministrator.asScala.foreach { exportedUser =>
              dbActions += processFromArchive(ImportItemType.SystemAdministrator, exportedUser, exportedUser.getId, ctx,
                ImportCallbacks.set(
                  (data: com.gitb.xml.export.SystemAdministrator, item: ImportItem) => {
                    if (!referenceUserEmails.contains(exportedUser.getEmail.toLowerCase) && systemAdminOrganisationId.isDefined) {
                      referenceUserEmails += exportedUser.getEmail.toLowerCase
                      PersistenceSchema.insertUser += toModelSystemAdministrator(data, None, systemAdminOrganisationId.get, ctx.importSettings)
                    } else {
                      DBIO.successful(())
                    }
                  },
                  (data: com.gitb.xml.export.SystemAdministrator, targetKey: String, item: ImportItem) => {
                    /*
                      We don't update the email as this must anyway be already matching (this was how the user was found
                      to be existing). Not updating the email avoids the need to check that the email is unique with respect
                      to other users.
                     */
                    val query = for {
                      user <- PersistenceSchema.users.filter(_.id === targetKey.toLong)
                    } yield (user.name, user.password, user.onetimePassword)
                    query.update(data.getName, decrypt(ctx.importSettings, data.getPassword), data.isOnetimePassword)
                  }
                )
              )
            }
          }
          toDBIO(dbActions)
        }
        _ <- {
          if (!Configurations.AUTHENTICATION_SSO_ENABLED) {
            processRemaining(ImportItemType.SystemAdministrator, ctx,
              (targetKey: String, item: ImportItem) => {
                val userId = targetKey.toLong
                if (ownUserId.isDefined && ownUserId.get.longValue() != userId) {
                  // Avoid deleting self
                  PersistenceSchema.users.filter(_.id === userId).delete
                } else {
                  DBIO.successful(())
                }
              }
            )
          } else {
            DBIO.successful(())
          }
        }
        // System configurations
        _ <- {
          val dbActions = ListBuffer[DBIO[_]]()
          if (exportedSettings.getSystemConfigurations != null) {
            exportedSettings.getSystemConfigurations.getConfig.asScala.foreach { config =>
              dbActions += processFromArchive(ImportItemType.SystemConfiguration, config, config.getName, ctx,
                ImportCallbacks.set(
                  (data: com.gitb.xml.export.SystemConfiguration, item: ImportItem) => {
                    handleSystemParameter(data, ctx)
                  },
                  (data: com.gitb.xml.export.SystemConfiguration, targetKey: String, item: ImportItem) => {
                    handleSystemParameter(data, ctx)
                  }
                )
              )
            }
          }
          toDBIO(dbActions)
        }
        _ <- {
          processRemaining(ImportItemType.SystemConfiguration, ctx,
            (targetKey: String, item: ImportItem) => {
              if (systemConfigurationManager.isEditableSystemParameter(targetKey)) {
                systemConfigurationManager.updateSystemParameterInternal(targetKey, None, applySetting = true)
              } else {
                DBIO.successful(())
              }
            }
          )
        }
      } yield ()
    } else {
      DBIO.successful(())
    }
  }

  private def handleSystemParameter(data: com.gitb.xml.export.SystemConfiguration, ctx: ImportContext): DBIO[_] = {
    if (systemConfigurationManager.isEditableSystemParameter(data.getName)) {
      var valueToSet = Option(data.getValue)
      if (valueToSet.isDefined) {
        if (data.getName == Constants.DemoAccount) {
          // Make sure the demo account ID matches the after-import result for the demo user account.
          valueToSet = ctx.processedIdMap.get(ImportItemType.OrganisationUser).flatMap(_.get(valueToSet.get))
        } else if (data.getName == Constants.EmailSettings) {
          // Make sure SMTP password is encrypted with target master key.
          valueToSet = prepareSmtpSettings(valueToSet, ctx.importSettings)
        }
      }
      systemConfigurationManager.updateSystemParameterInternal(data.getName, valueToSet, applySetting = true)
    } else {
      DBIO.successful(())
    }
  }

  private def loadExistingDomainData(ctx: ImportContext, domainId: Long): Future[ImportContext] = {
    // Load values pertinent to domain to ensure we are modifying items within (for security purposes).
    // Domain
    DB.run(
      PersistenceSchema.domains.filter(_.id === domainId).map(_.id).result.headOption
    ).zip(
      // Shared test suites
      loadIfApplicable(ctx.importTargets.hasTestSuites,
        () => DB.run(PersistenceSchema.testSuites
          .filter(_.domain === domainId)
          .filter(_.shared)
          .map(_.id)
          .result
        )
      )
    ).zip(
      // Specifications
      loadIfApplicable(ctx.importTargets.hasSpecifications,
        () => DB.run(PersistenceSchema.specifications.filter(_.domain === domainId).map(_.id).result)
      )
    ).zip(
      // Specification groups
      loadIfApplicable(ctx.importTargets.hasSpecifications,
        () => DB.run(PersistenceSchema.specificationGroups.filter(_.domain === domainId).map(_.id).result)
      )
    ).zip(
      // Test suites
      loadIfApplicable(ctx.importTargets.hasSpecifications && ctx.importTargets.hasTestSuites,
        () => DB.run(PersistenceSchema.testSuites
          .join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
          .filter(_._1.domain === domainId)
          .map(_._1.id)
          .result
        )
      )
    ).zip(
      // Actors
      loadIfApplicable(ctx.importTargets.hasSpecifications && ctx.importTargets.hasActors,
        () => DB.run(PersistenceSchema.actors
            .filter(_.domain === domainId)
            .map(_.id)
            .result
        )
      )
    ).zip(
      // Endpoints
      loadIfApplicable(ctx.importTargets.hasSpecifications && ctx.importTargets.hasActors && ctx.importTargets.hasEndpoints,
        () => DB.run(PersistenceSchema.endpoints
          .join(PersistenceSchema.actors).on(_.actor === _.id)
          .filter(_._2.domain === domainId)
          .map(_._1.id)
          .result
        )
      )
    ).zip(
      // Endpoint parameters
      loadIfApplicable(ctx.importTargets.hasSpecifications && ctx.importTargets.hasActors && ctx.importTargets.hasEndpoints && ctx.importTargets.hasEndpointParameters,
        () => DB.run(PersistenceSchema.parameters
            .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
            .join(PersistenceSchema.actors).on(_._2.actor === _.id)
            .filter(_._2.domain === domainId)
            .map(_._1._1.id)
            .result
        )
      )
    ).zip(
      // Domain parameters
      loadIfApplicable(ctx.importTargets.hasDomainParameters,
        () => DB.run(PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).result)
      )
    ).zip(
      // Test services
      loadIfApplicable(ctx.importTargets.hasTestServices,
        () => DB.run(PersistenceSchema.testServices
          .join(PersistenceSchema.domainParameters).on(_.parameter === _.id)
          .filter(_._2.domain === domainId).map(_._1.id).result)
      )
    ).map { results =>
      val domain =              results._1._1._1._1._1._1._1._1._1
      val sharedTestSuites =    results._1._1._1._1._1._1._1._1._2
      val specifications =      results._1._1._1._1._1._1._1._2
      val specificationGroups = results._1._1._1._1._1._1._2
      val testSuites =          results._1._1._1._1._1._2
      val actors =              results._1._1._1._1._2
      val endpoints =           results._1._1._1._2
      val endpointParameters =  results._1._1._2
      val domainParameters =    results._1._2
      val testServices =        results._2
      domain.foreach(x => ctx.existingIds.map(ImportItemType.Domain) += x.toString)
      sharedTestSuites.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.TestSuite) += x.toString))
      specifications.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Specification) += x.toString))
      specificationGroups.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.SpecificationGroup) += x.toString))
      testSuites.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.TestSuite) += x.toString))
      actors.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Actor) += x.toString))
      endpoints.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Endpoint) += x.toString))
      endpointParameters.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.EndpointParameter) += x.toString))
      domainParameters.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.DomainParameter) += x.toString))
      testServices.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.TestService) += x.toString))
      ctx
    }
  }

  private def completeDomainImportInternal(exportedDomain: com.gitb.xml.export.Domain, ctx: ImportContext, canAddOrDeleteDomain: Boolean, linkedToCommunity: Boolean): DBIO[_] = {
    var createdDomainId: Option[Long] = None
    // Ensure that a domain cannot be created or added without appropriate access.
    if (!canAddOrDeleteDomain) {
      var domainImportItem: Option[ImportItem] = None
      if (ctx.importItemMaps.sourceMap.contains(ImportItemType.Domain) && ctx.importItemMaps.sourceMap(ImportItemType.Domain).nonEmpty) {
        domainImportItem = Some(ctx.importItemMaps.sourceMap(ImportItemType.Domain).head._2)
      } else if (ctx.importItemMaps.targetMap.contains(ImportItemType.Domain) && ctx.importItemMaps.targetMap(ImportItemType.Domain).nonEmpty) {
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
    val dbAction = for {
      _ <- {
        // Domain
        processFromArchive(ImportItemType.Domain, exportedDomain, exportedDomain.getId, ctx,
          ImportCallbacks.set(
            (data: com.gitb.xml.export.Domain, item: ImportItem) => {
              val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
              val shortNameToUse = if (!linkedToCommunity && ctx.importSettings.shortNameReplacement.isDefined) {
                ctx.importSettings.shortNameReplacement.get
              } else {
                data.getShortName
              }
              val fullNameToUse = if (!linkedToCommunity && ctx.importSettings.fullNameReplacement.isDefined) {
                ctx.importSettings.fullNameReplacement.get
              } else {
                data.getFullName
              }
              domainManager.createDomainForImport(models.Domain(0L,
                shortNameToUse,
                fullNameToUse,
                Option(data.getDescription), Option(data.getReportMetadata), apiKey))
            },
            (data: com.gitb.xml.export.Domain, targetKey: String, item: ImportItem) => {
              val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
              domainManager.updateDomainInternal(targetKey.toLong, data.getShortName, data.getFullName, Option(data.getDescription), Option(data.getReportMetadata), Some(apiKey))
            },
            (data: com.gitb.xml.export.Domain, targetKey: Any, item: ImportItem) => {
              // Record this in case we need to do a global cleanup.
              createdDomainId = Some(targetKey.asInstanceOf[Long])
              // In case of a failure delete the created domain test suite folder (if one was created later on).
              ctx.onFailureCalls += (() => {
                val domainFolder = repositoryUtils.getDomainTestSuitesPath(createdDomainId.get)
                if (domainFolder.exists()) {
                  FileUtils.deleteQuietly(domainFolder)
                }
              })
            }
          )
        )
      }
      _ <- {
        processRemaining(ImportItemType.Domain, ctx,
          (targetKey: String, item: ImportItem) => {
            domainManager.deleteDomainInternal(targetKey.toLong, ctx.onSuccessCalls)
          }
        )
      }
      _ <- {
        // Domain parameters
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getParameters != null) {
          exportedDomain.getParameters.getParameter.asScala.foreach { parameter =>
            dbActions += processFromArchive(ImportItemType.DomainParameter, parameter, parameter.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.DomainParameter, item: ImportItem) => {
                  val domainId = getDomainIdFromParentItem(item)
                  val fileData = parameterFileMetadata(ctx, data.getType, isDomainParameter = true, data.getValue)
                  domainParameterManager.createDomainParameterInternal(
                    models.DomainParameter(0L, data.getName, Option(data.getDescription),
                      fileData._1, manageEncryptionIfNeeded(ctx.importSettings, data.getType, Option(data.getValue)), data.isInTests,
                      fileData._2, data.isTestService, domainId), fileData._3, ctx.onSuccessCalls)
                },
                (data: com.gitb.xml.export.DomainParameter, targetKey: String, item: ImportItem) => {
                  val domainId = getDomainIdFromParentItem(item)
                  val fileData = parameterFileMetadata(ctx, data.getType, isDomainParameter = true, data.getValue)
                  domainParameterManager.updateDomainParameterInternal(domainId,
                    targetKey.toLong, data.getName, Option(data.getDescription), fileData._1,
                    manageEncryptionIfNeeded(ctx.importSettings, data.getType, Option(data.getValue)), data.isInTests, data.isTestService,
                    fileData._2, fileData._3, ctx.onSuccessCalls)
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.DomainParameter, ctx,
          (targetKey: String, item: ImportItem) => {
            val domainId = getDomainIdFromParentItem(item)
            domainParameterManager.deleteDomainParameter(domainId, targetKey.toLong, checkToDeleteLinkedTestService = true, ctx.onSuccessCalls)
          }
        )
      }
      _ <- {
        // Test services
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getTestServices != null) {
          exportedDomain.getTestServices.getService.asScala.foreach { testService =>
            dbActions += processFromArchive(ImportItemType.TestService, testService, testService.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.TestService, item: ImportItem) => {
                  val processedDomainParameterId = ctx.processedIdMap(ImportItemType.DomainParameter).get(data.getParameter.getId).map(_.toLong)
                  if (processedDomainParameterId.isDefined) {
                    domainParameterManager.createTestServiceInternal(toModelTestService(data, processedDomainParameterId.get, None, ctx.importSettings))
                  } else {
                    DBIO.successful("")
                  }
                },
                (data: com.gitb.xml.export.TestService, targetKey: String, item: ImportItem) => {
                  val processedDomainParameterId = ctx.processedIdMap(ImportItemType.DomainParameter).get(data.getParameter.getId).map(_.toLong)
                  if (processedDomainParameterId.isDefined) {
                    domainParameterManager.updateTestServiceInternal(toModelTestService(data, processedDomainParameterId.get, Some(targetKey.toLong), ctx.importSettings))
                  } else {
                    DBIO.successful(())
                  }
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.TestService, ctx,
          (targetKey: String, item: ImportItem) => {
            domainParameterManager.deleteTestServiceInternal(targetKey.toLong)
          }
        )
      }
      _ <- {
        // Post-processing consistency steps for domain parameters and test services.
        val domainId = ctx.processedIdMap(ImportItemType.Domain).get(exportedDomain.getId).map(_.toLong)
        if (domainId.isDefined) {
          for {
            // Get the parameter IDs that are linked to test services.
            testServiceParametersId <- PersistenceSchema.testServices
              .join(PersistenceSchema.domainParameters).on(_.parameter === _.id)
              .filter(_._2.domain === domainId.get)
              .map(_._2.id)
              .result
            // Any parameters with other IDs that are flagged as linked to test services should be set as not being services.
            _ <- PersistenceSchema.domainParameters
              .filter(_.domain === domainId.get)
              .filter(_.isTestService)
              .filterNot(_.id inSet testServiceParametersId)
              .map(_.isTestService)
              .update(false)
            // The parameters linked with test services flagged as not included in tests should be force-included.
            _ <- PersistenceSchema.domainParameters
              .filter(_.id inSet testServiceParametersId)
              .filterNot(_.inTests)
              .map(_.inTests)
              .update(true)
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        // Shared test suites
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSharedTestSuites != null) {
          exportedDomain.getSharedTestSuites.getTestSuite.asScala.foreach { exportedTestSuite =>
            dbActions += processFromArchive(ImportItemType.TestSuite, exportedTestSuite, exportedTestSuite.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.TestSuite, item: ImportItem) => {
                  createSharedTestSuite(data, ctx, item)
                },
                (data: com.gitb.xml.export.TestSuite, targetKey: String, item: ImportItem) => {
                  updateSharedTestSuite(data, ctx, item)
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        // Specification groups
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecificationGroups != null) {
          exportedDomain.getSpecificationGroups.getGroup.asScala.foreach { exportedGroup =>
            dbActions += processFromArchive(ImportItemType.SpecificationGroup, exportedGroup, exportedGroup.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.SpecificationGroup, item: ImportItem) => {
                  val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                  specificationManager.createSpecificationGroupInternal(models.SpecificationGroups(0L, data.getShortName, data.getFullName, Option(data.getDescription), Option(data.getReportMetadata), data.getDisplayOrder, apiKey, getDomainIdFromParentItem(item)), checkApiKeyUniqueness = true)
                },
                (data: com.gitb.xml.export.SpecificationGroup, targetKey: String, item: ImportItem) => {
                  val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                  specificationManager.updateSpecificationGroupInternal(targetKey.toLong, data.getShortName, data.getFullName, Option(data.getDescription), Option(data.getReportMetadata), Some(data.getDisplayOrder), Some(apiKey), checkApiKeyUniqueness = true)
                },
                (data: com.gitb.xml.export.SpecificationGroup, targetKey: Any, item: ImportItem) => {
                  // No action.
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.SpecificationGroup, ctx,
          (targetKey: String, item: ImportItem) => {
            specificationManager.deleteSpecificationGroupInternal(targetKey.toLong, deleteSpecifications = false, ctx.onSuccessCalls)
          }
        )
      }
      _ <- {
        // Specifications
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.asScala.foreach { exportedSpecification =>
            dbActions += processFromArchive(ImportItemType.Specification, exportedSpecification, exportedSpecification.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.Specification, item: ImportItem) => {
                  val relatedGroupId = getProcessedDbId(data.getGroup, ImportItemType.SpecificationGroup, ctx)
                  if (data.getGroup == null || relatedGroupId.nonEmpty) {
                    val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                    specificationManager.createSpecificationsInternal(models.Specifications(0L, data.getShortName, data.getFullName, Option(data.getDescription), Option(data.getReportMetadata), data.isHidden, apiKey, getDomainIdFromParentItem(item), data.getDisplayOrder, relatedGroupId), checkApiKeyUniqueness = true,
                      BadgeInfo(toModelBadges(data.getBadges, ctx), toModelBadges(data.getBadgesForReport, ctx)), ctx.onSuccessCalls)
                  } else {
                    DBIO.successful(())
                  }
                },
                (data: com.gitb.xml.export.Specification, targetKey: String, item: ImportItem) => {
                  val relatedGroupId = getProcessedDbId(data.getGroup, ImportItemType.SpecificationGroup, ctx)
                  if (data.getGroup == null || relatedGroupId.nonEmpty) {
                    val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                    specificationManager.updateSpecificationInternal(targetKey.toLong, data.getShortName, data.getFullName, Option(data.getDescription), Option(data.getReportMetadata), data.isHidden, Some(apiKey), checkApiKeyUniqueness = true, relatedGroupId, Some(data.getDisplayOrder),
                      Some(BadgeInfo(toModelBadges(data.getBadges, ctx), toModelBadges(data.getBadgesForReport, ctx))), ctx.onSuccessCalls)
                  } else {
                    DBIO.successful(())
                  }
                },
                (data: com.gitb.xml.export.Specification, targetKey: Any, item: ImportItem) => {
                  // No action.
                }
              )
            )
          }
        }
        toDBIO(dbActions)
      }
      _ <- {
        processRemaining(ImportItemType.Specification, ctx,
          (targetKey: String, item: ImportItem) => {
            specificationManager.deleteSpecificationInternal(targetKey.toLong, ctx.onSuccessCalls)
          }
        )
      }
      _ <- {
        // Actors
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.asScala.foreach { exportedSpecification =>
            if (exportedSpecification.getActors != null) {
              exportedSpecification.getActors.getActor.asScala.foreach { exportedActor =>
                dbActions += processFromArchive(ImportItemType.Actor, exportedActor, exportedActor.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.Actor, item: ImportItem) => {
                      var order: Option[Short] = None
                      if (data.getOrder != null) {
                        order = Some(data.getOrder.shortValue())
                      }
                      val specificationId = item.parentItem.get.targetKey.get.toLong // Specification
                      val domainId = getDomainIdFromParentItem(item)
                      val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                      actorManager.createActor(models.Actors(0L, data.getActorId, data.getName, Option(data.getDescription), Option(data.getReportMetadata), Some(data.isDefault), data.isHidden, order, apiKey, domainId), specificationId, checkApiKeyUniqueness = true,
                        Some(BadgeInfo(toModelBadges(data.getBadges, ctx), toModelBadges(data.getBadgesForReport, ctx))), ctx.onSuccessCalls)
                    },
                    (data: com.gitb.xml.export.Actor, targetKey: String, item: ImportItem) => {
                      // Record actor info (needed for test suite processing).
                      val specificationId = item.parentItem.get.targetKey.get.toLong
                      if (!ctx.savedSpecificationActors.contains(specificationId)) {
                        ctx.savedSpecificationActors += (specificationId -> mutable.Map[String, Long]())
                      }
                      ctx.savedSpecificationActors(specificationId) += (data.getActorId -> targetKey.toLong)
                      // Update actor.
                      var order: Option[Short] = None
                      if (data.getOrder != null) {
                        order = Some(data.getOrder.shortValue())
                      }
                      val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                      actorManager.updateActor(targetKey.toLong, data.getActorId, data.getName, Option(data.getDescription), Option(data.getReportMetadata), Some(data.isDefault), data.isHidden, order, item.parentItem.get.targetKey.get.toLong, Some(apiKey), checkApiKeyUniqueness = true,
                        Some(BadgeInfo(toModelBadges(data.getBadges, ctx), toModelBadges(data.getBadgesForReport, ctx))), ctx.onSuccessCalls)
                    },
                    (data: com.gitb.xml.export.Actor, targetKey: Any, item: ImportItem) => {
                      // Record actor info (needed for test suite processing).
                      val specificationId = item.parentItem.get.targetKey.get.toLong
                      if (!ctx.savedSpecificationActors.contains(specificationId)) {
                        ctx.savedSpecificationActors += (specificationId -> mutable.Map[String, Long]())
                      }
                      ctx.savedSpecificationActors(specificationId) += (data.getActorId -> targetKey.asInstanceOf[Long])
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
          (targetKey: String, item: ImportItem) => {
            actorManager.deleteActor(targetKey.toLong, ctx.onSuccessCalls)
          }
        )
      }
      _ <- {
        // Endpoints
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.asScala.foreach { exportedSpecification =>
            if (exportedSpecification.getActors != null) {
              exportedSpecification.getActors.getActor.asScala.foreach { exportedActor =>
                if (exportedActor.getEndpoints != null) {
                  exportedActor.getEndpoints.getEndpoint.asScala.foreach { exportedEndpoint =>
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
          (targetKey: String, item: ImportItem) => {
            endpointManager.delete(targetKey.toLong, ctx.onSuccessCalls)
          }
        )
      }
      _ <- {
        // Endpoint parameters
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.asScala.foreach { exportedSpecification =>
            if (exportedSpecification.getActors != null) {
              exportedSpecification.getActors.getActor.asScala.foreach { exportedActor =>
                if (exportedActor.getEndpoints != null) {
                  exportedActor.getEndpoints.getEndpoint.asScala.foreach { exportedEndpoint =>
                    var defaultDisplayOrder = 0
                    if (exportedEndpoint.getParameters != null) {
                      exportedEndpoint.getParameters.getParameter.asScala.foreach { exportedParameter =>
                        defaultDisplayOrder = defaultDisplayOrder + 1
                        var displayOrderToUse = defaultDisplayOrder
                        if (exportedParameter.getDisplayOrder != null) {
                          displayOrderToUse = exportedParameter.getDisplayOrder.toShort
                        }
                        dbActions += processFromArchive(ImportItemType.EndpointParameter, exportedParameter, exportedParameter.getId, ctx,
                          ImportCallbacks.set(
                            (data: com.gitb.xml.export.EndpointParameter, item: ImportItem) => {
                              var labelToUse = data.getLabel
                              if (labelToUse == null) {
                                labelToUse = data.getName
                              }
                              parameterManager.createParameter(models.Parameters(0L, labelToUse, data.getName, Option(data.getDescription),
                                requiredToUse(data.isRequired), propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests, data.isHidden,
                                Option(data.getAllowedValues), displayOrderToUse.toShort, Option(data.getDependsOn), Option(data.getDependsOnValue), Option(data.getDefaultValue),
                                item.parentItem.get.targetKey.get.toLong))
                            },
                            (data: com.gitb.xml.export.EndpointParameter, targetKey: String, item: ImportItem) => {
                              var labelToUse = data.getLabel
                              if (labelToUse == null) {
                                labelToUse = data.getName
                              }
                              parameterManager.updateParameter(targetKey.toLong, labelToUse, data.getName, Option(data.getDescription),
                                requiredToUse(data.isRequired), propertyTypeToKind(data.getType), !data.isEditable, !data.isInTests, data.isHidden,
                                Option(data.getAllowedValues), Option(data.getDependsOn), Option(data.getDependsOnValue), Option(data.getDefaultValue),
                                Option(data.getDisplayOrder), ctx.onSuccessCalls)
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
          (targetKey: String, item: ImportItem) => {
            parameterManager.delete(targetKey.toLong, ctx.onSuccessCalls)
          }
        )
      }
      _ <- {
        // Test suites
        val dbActions = ListBuffer[DBIO[_]]()
        if (exportedDomain.getSpecifications != null) {
          exportedDomain.getSpecifications.getSpecification.asScala.foreach { exportedSpecification =>
            // Shared test suites.
            if (exportedSpecification.getSharedTestSuites != null) {
              exportedSpecification.getSharedTestSuites.asScala.foreach { exportedTestSuite =>
                val id = exportedSpecification.getId+"|"+exportedTestSuite.getId
                dbActions += processFromArchive(ImportItemType.TestSuite, exportedTestSuite, id, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.TestSuite, item: ImportItem) => {
                      val relatedSpecificationId = getProcessedDbId(exportedSpecification, ImportItemType.Specification, ctx)
                      val relatedTestSuiteId = getProcessedDbId(exportedTestSuite, ImportItemType.TestSuite, ctx)
                      if (relatedSpecificationId.isDefined && relatedTestSuiteId.isDefined) {
                        val sharedTestSuiteInfo = ctx.sharedTestSuiteInfo.get(relatedTestSuiteId.get)
                        if (sharedTestSuiteInfo.isDefined) {
                          testSuiteManager.stepUpdateTestSuiteSpecificationLinks(relatedSpecificationId.get, relatedTestSuiteId.get, sharedTestSuiteInfo.get._1,
                            ctx.savedSpecificationActors(relatedSpecificationId.get).asJava, // savedActorIds
                            sharedTestSuiteInfo.get._2)
                        } else {
                          DBIO.successful(())
                        }
                      } else {
                        DBIO.successful(())
                      }
                    },
                    (data: com.gitb.xml.export.TestSuite, targetKey: String, item: ImportItem) => {
                      // Update not needed. Test case links and updates to conformance statements will have happened in the test suite update.
                      DBIO.successful(())
                    }
                  )
                )
              }
            }
            // Specification-specific test suites.
            if (exportedSpecification.getTestSuites != null) {
              exportedSpecification.getTestSuites.getTestSuite.asScala.foreach { exportedTestSuite =>
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
          (targetKey: String, item: ImportItem) => {
            val testSuiteId = targetKey.toLong
            val isSharedTestSuiteLink = ctx.sharedTestSuiteInfo.contains(testSuiteId) && item.parentItem.isDefined && item.parentItem.get.itemType == ImportItemType.Specification
            if (isSharedTestSuiteLink) {
              // This is a shared test suite that was either created or updated during this import (i.e. it needs to remain present).
              // This means that this removal is not for the test suite itself but for the link to a specification.
              val specificationId = item.parentItem.get.targetKey.get.toLong
              testSuiteManager.unlinkSharedTestSuiteInternal(testSuiteId, List(specificationId))
            } else {
              // This is either a specification-specific test suite or a shared test suite that needs deleting.
              testSuiteManager.undeployTestSuite(targetKey.toLong, ctx.onSuccessCalls)
            }
          }
        )
      }
    } yield ()
    dbAction.cleanUp(error => {
      if (error.isDefined) {
        // Cleanup operations in case an error occurred.
        if (createdDomainId.isDefined) {
          val domainFolder = repositoryUtils.getDomainTestSuitesPath(createdDomainId.get)
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

  private def toModelBadgeFile(exportBadge: ConformanceBadge, ctx: ImportContext): Option[NamedFile] = {
    if (exportBadge != null) {
      val extension = FilenameUtils.getExtension(exportBadge.getName)
      val extensionToUse = if (extension.isBlank) {
        None
      } else {
        Some("."+extension)
      }
      val fileToStore = dataUrlToTempFile(exportBadge.getContent, extensionToUse)
      ctx.onFailureCalls += (() => if (fileToStore.exists()) {
        FileUtils.deleteQuietly(fileToStore)
      })
      Some(NamedFile(fileToStore, fileToStore.getName))
    } else {
      None
    }
  }

  private def toModelBadges(exportBadges: ConformanceBadges, ctx: ImportContext): Badges = {
    if (exportBadges != null) {
      Badges(
        exportBadges.getSuccess != null, exportBadges.getFailure != null, exportBadges.getOther != null,
        toModelBadgeFile(exportBadges.getSuccess, ctx),
        toModelBadgeFile(exportBadges.getFailure, ctx),
        toModelBadgeFile(exportBadges.getOther, ctx)
      )
    } else {
      Badges(hasSuccess = false, hasFailure = false, hasOther = false, None, None, None)
    }
  }

  private def hasExisting(itemType: ImportItemType, key: String, ctx: ImportContext): Boolean = {
    ctx.existingIds.map.contains(itemType) && ctx.existingIds.map(itemType).contains(key)
  }

  private def completeFileSystemFinalisation(ctx: ImportContext, dbAction: DBIO[_]): DBIO[Unit] = {
    dbActionFinalisation(Some(ctx.onSuccessCalls), Some(ctx.onFailureCalls), dbAction).map(_ => ())
  }

  private def prepareSmtpSettings(value: Option[String], importSettings: ImportSettings): Option[String] = {
    if (value.isDefined) {
      val emailSettings = JsonUtil.parseJsEmailSettings(value.get)
      if (emailSettings.authPassword.isDefined) {
        // We decrypt and set the password in clear because the update service will later encrypt it.
        val newPassword = decrypt(importSettings, emailSettings.authPassword.get)
        Some(JsonUtil.jsEmailSettings(emailSettings.withPassword(newPassword), maskPassword = false).toString())
      } else {
        value
      }
    } else {
      value
    }
  }

  private def loadExistingCommunityData(ctx: ImportContext, communityId: Long, domainId: Option[Long]): Future[ImportContext] = {
    // Labels
    loadIfApplicable(ctx.importTargets.hasCustomLabels,
      () => DB.run(PersistenceSchema.communityLabels.filter(_.community === communityId).map(x => (x.community, x.labelType)).result)
    ).zip(
      // Organisation properties
      loadIfApplicable(ctx.importTargets.hasOrganisationProperties,
        () => DB.run(PersistenceSchema.organisationParameters.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // System properties
      loadIfApplicable(ctx.importTargets.hasSystemProperties,
        () => DB.run(PersistenceSchema.systemParameters.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // Landing pages
      loadIfApplicable(ctx.importTargets.hasLandingPages,
        () => DB.run(PersistenceSchema.landingPages.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // Legal notices
      loadIfApplicable(ctx.importTargets.hasLegalNotices,
        () => DB.run(PersistenceSchema.legalNotices.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // Error templates
      loadIfApplicable(ctx.importTargets.hasErrorTemplates,
        () => DB.run(PersistenceSchema.errorTemplates.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // Triggers
      loadIfApplicable(ctx.importTargets.hasTriggers,
        () => DB.run(PersistenceSchema.triggers.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // Resources
      loadIfApplicable(ctx.importTargets.hasResources,
        () => DB.run(PersistenceSchema.communityResources.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // Administrators
      loadIfApplicable(!Configurations.AUTHENTICATION_SSO_ENABLED && ctx.importTargets.hasAdministrators,
        () => DB.run(PersistenceSchema.users
            .join(PersistenceSchema.organizations).on(_.organization === _.id)
            .filter(_._2.community === communityId)
            .filter(_._2.adminOrganization === true)
            .filter(_._1.role === UserRole.CommunityAdmin.id.toShort)
            .map(_._1.id)
            .result
        )
      )
    ).zip(
      // Organisations
      loadIfApplicable(ctx.importTargets.hasOrganisations,
        () => DB.run(PersistenceSchema.organizations.filter(_.community === communityId).map(_.id).result)
      )
    ).zip(
      // Organisation users
      loadIfApplicable(ctx.importTargets.hasOrganisations && !Configurations.AUTHENTICATION_SSO_ENABLED && ctx.importTargets.hasOrganisationUsers,
        () => DB.run(PersistenceSchema.users
            .join(PersistenceSchema.organizations).on(_.organization === _.id)
            .filter(_._2.adminOrganization === false)
            .filter(_._2.community === communityId)
            .map(_._1.id)
            .result
        )
      )
    ).zip(
      // Organisation property values
      loadIfApplicable(ctx.importTargets.hasOrganisations && ctx.importTargets.hasOrganisationPropertyValues,
        () => DB.run(PersistenceSchema.organisationParameterValues
            .join(PersistenceSchema.organizations).on(_.organisation === _.id)
            .filter(_._2.adminOrganization === false)
            .filter(_._2.community === communityId)
            .map(x => (x._2.id, x._1.parameter)) // Organisation ID, ParameterID
            .result
        )
      )
    ).zip(
      // Systems
      loadIfApplicable(ctx.importTargets.hasOrganisations && ctx.importTargets.hasSystems,
        () => DB.run(PersistenceSchema.systems
            .join(PersistenceSchema.organizations).on(_.owner === _.id)
            .filter(_._2.adminOrganization === false)
            .filter(_._2.community === communityId)
            .map(_._1.id)
            .result
        )
      )
    ).zip(
      // System property values
      loadIfApplicable(ctx.importTargets.hasOrganisations && ctx.importTargets.hasSystems && ctx.importTargets.hasSystemPropertyValues,
        () => DB.run(
          PersistenceSchema.systemParameterValues
            .join(PersistenceSchema.systems).on(_.system === _.id)
            .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
            .filter(_._2.adminOrganization === false)
            .filter(_._2.community === communityId)
            .map(x => (x._1._2.id, x._1._1.parameter)) // System ID, Parameter ID
            .result
        )
      )
    ).zip(
      // Statements
      loadIfApplicable(ctx.importTargets.hasOrganisations && ctx.importTargets.hasSystems && ctx.importTargets.hasStatements,
        () => DB.run(PersistenceSchema.systemImplementsActors
          .join(PersistenceSchema.systems).on(_.systemId === _.id)
          .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
          .join(PersistenceSchema.actors).on(_._1._1.actorId === _.id)
          .join(PersistenceSchema.specificationHasActors).on(_._2.id === _.actorId)
          .join(PersistenceSchema.specifications).on(_._2.specId === _.id)
          .filter(_._1._1._1._2.adminOrganization === false)
          .filter(_._1._1._1._2.community === communityId)
          .filterOpt(domainId)((q, id) => q._2.domain === id)
          .map(x => (x._1._1._1._1._1.systemId, x._1._1._1._1._1.actorId)) // System ID, Actor ID
          .result
        )
      )
    ).zip(
      // Statement configurations
      loadIfApplicable(ctx.importTargets.hasOrganisations && ctx.importTargets.hasSystems && ctx.importTargets.hasStatements && ctx.importTargets.hasStatementConfigurations,
        () => DB.run(PersistenceSchema.configs
          .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
          .join(PersistenceSchema.actors).on(_._2.actor === _.id)
          .join(PersistenceSchema.systems).on(_._1._1.system === _.id)
          .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
          .filter(_._2.adminOrganization === false)
          .filter(_._2.community === communityId)
          .filterOpt(domainId)((q, id) => q._1._1._2.domain === id)
          .map(x => (x._1._1._2.id, x._1._1._1._2.id, x._1._2.id, x._1._1._1._1.parameter)) // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
          .result
        )
      )
    ).map { results =>
      val labels =                     results._1._1._1._1._1._1._1._1._1._1._1._1._1._1._1
      val organisationProperties =     results._1._1._1._1._1._1._1._1._1._1._1._1._1._1._2
      val systemProperties =           results._1._1._1._1._1._1._1._1._1._1._1._1._1._2
      val landingPages =               results._1._1._1._1._1._1._1._1._1._1._1._1._2
      val legalNotices =               results._1._1._1._1._1._1._1._1._1._1._1._2
      val errorTemplates =             results._1._1._1._1._1._1._1._1._1._1._2
      val triggers =                   results._1._1._1._1._1._1._1._1._1._2
      val resources =                  results._1._1._1._1._1._1._1._1._2
      val administrators =             results._1._1._1._1._1._1._1._2
      val organisations =              results._1._1._1._1._1._1._2
      val organisationUsers =          results._1._1._1._1._1._2
      val organisationPropertyValues = results._1._1._1._1._2
      val systems =                    results._1._1._1._2
      val systemPropertyValues =       results._1._1._2
      val statements =                 results._1._2
      val statementConfigurations =    results._2

      // Add the community
      ctx.existingIds.map(ImportItemType.Community) += communityId.toString
      // Add the community-related data
      labels.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.CustomLabel) += s"${x._1}_${x._2}"))
      organisationProperties.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.OrganisationProperty) += x.toString))
      systemProperties.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.SystemProperty) += x.toString))
      landingPages.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.LandingPage) += x.toString))
      legalNotices.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.LegalNotice) += x.toString))
      errorTemplates.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.ErrorTemplate) += x.toString))
      triggers.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Trigger) += x.toString))
      resources.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.CommunityResource) += x.toString))
      administrators.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Administrator) += x.toString))
      organisations.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Organisation) += x.toString))
      organisationUsers.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.OrganisationUser) += x.toString))
      organisationPropertyValues.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.OrganisationPropertyValue) += s"${x._1}_${x._2}"))
      systems.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.System) += x.toString))
      systemPropertyValues.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.SystemPropertyValue) += s"${x._1}_${x._2}"))
      statements.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.Statement) += s"${x._1}_${x._2}"))
      statementConfigurations.foreach(_.foreach(x => ctx.existingIds.map(ImportItemType.StatementConfiguration) += s"${x._1}_${x._2}_${x._3}_${x._4}"))
      // Return the updated context
      ctx
    }
  }

  def completeCommunityImport(exportedData: com.gitb.xml.export.Export, importSettings: ImportSettings, importItems: List[ImportItem], targetCommunityId: Option[Long], canDoAdminOperations: Boolean, ownUserId: Option[Long]): Future[Unit] = {
    val ctx = ImportContext(
      importSettings,
      toImportItemMaps(importItems, ImportItemType.Community),
      ExistingIds.init(),
      ImportTargets.fromImportItems(importItems),
      mutable.Map[ImportItemType, mutable.Map[String, String]](),
      mutable.Map[Long, mutable.Map[String, Long]](),
      mutable.Map[Long, (Option[List[TestCases]], Map[String, (Long, Boolean)])](),
      mutable.ListBuffer[() => _](),
      mutable.ListBuffer[() => _]()
    )
    val exportedCommunity = exportedData.getCommunities.getCommunity.asScala.head
    for {
      /*
       * Start with database lookups for existing data
       */
      targetCommunity <- {
        if (targetCommunityId.isDefined) {
          DB.run(PersistenceSchema.communities.filter(_.id === targetCommunityId.get).result.headOption)
        } else {
          Future.successful(None)
        }
      }
      // Load the admin organisation ID for the community
      existingCommunityAdminOrganisationId <- {
        if (targetCommunity.isDefined) {
          DB.run(PersistenceSchema.organizations.filter(_.community === targetCommunityId.get).filter(_.adminOrganization === true).map(x => x.id).result.headOption)
        } else {
          Future.successful(None)
        }
      }
      // Load existing data
      data <- {
        if (targetCommunity.isDefined) {
          loadExistingCommunityData(ctx, targetCommunity.get.id, targetCommunity.get.domain)
        } else {
          Future.successful(ctx)
        }
      }
      // If we have users load their emails to ensure we don't end up with duplicates.
      existingUserEmails <- {
        if (!Configurations.AUTHENTICATION_SSO_ENABLED && (ctx.importTargets.hasAdministrators || ctx.importTargets.hasOrganisationUsers)) {
          importPreviewManager.loadUserEmailSet()
        } else {
          Future.successful(Set.empty[String])
        }
      }
      // Load the domain ID corresponding to the community's linked domain API key.
      domainIdFromArchive <- {
        if (canDoAdminOperations && exportedCommunity.getDomain != null) {
          domainManager.getByApiKey(exportedCommunity.getDomain.getApiKey).map(_.map(_.id))
        } else {
          Future.successful(None)
        }
      }
      domainInfo <- {
        var targetDomainId: Option[Long] = None
        var proceedWithDomainImport = false
        if (exportedCommunity.getDomain != null) {
          var domainIdFromDatabase: Option[Long] = None
          if (targetCommunity.isDefined && targetCommunity.get.domain.isDefined) {
            domainIdFromDatabase = targetCommunity.get.domain
          }
          if (domainIdFromDatabase.isDefined && domainIdFromArchive.isDefined) {
            if (domainIdFromDatabase.get == domainIdFromArchive.get) {
              // The community is linked to an existing domain, which is the same domain the community is linked to in the imported archive.
              targetDomainId = domainIdFromDatabase
              proceedWithDomainImport = true
            } else {
              // The community is linked to an existing domain, which is different from the domain (also existing in the DB) the community is linked to in the imported archive.
              targetDomainId = domainIdFromArchive
              proceedWithDomainImport = canDoAdminOperations
            }
          } else if (domainIdFromDatabase.isDefined) {
            // The community is linked to an existing domain, but in the archive it is linked to another domain that does not exist.
            targetDomainId = domainIdFromDatabase
            proceedWithDomainImport = true
          } else if (domainIdFromArchive.isDefined) {
            // The community is not linked to a domain, but in the archive it is linked to a domain that exists in the DB.
            targetDomainId = domainIdFromArchive
            proceedWithDomainImport = canDoAdminOperations
          } else {
            // The community is not linked to domain, but in the archive it is linked to a domain that doesn't exist in the DB.
            targetDomainId = None
            proceedWithDomainImport = canDoAdminOperations
          }
        }
        if (proceedWithDomainImport) {
          /*
           * We only allow a domain import to proceed if the user is a Test Bed administrator or
           * if the target community also has a domain (i.e. this is an update). A community
           * administrator can never add domains, delete domains, or update previously unrelated domains;
           * only update the community's (existing) domain.
           */
          mergeImportItemMaps(ctx.importItemMaps, toImportItemMaps(importItems, ImportItemType.Domain))
          if (targetDomainId.isDefined) {
            loadExistingDomainData(ctx, targetDomainId.get).map(ctx => (ctx, proceedWithDomainImport))
          } else {
            Future.successful((ctx, proceedWithDomainImport))
          }
        } else {
          Future.successful((ctx, proceedWithDomainImport))
        }
      }
      proceedWithDomainImport <- Future.successful(domainInfo._2)
      ctx <- Future.successful(domainInfo._1)
      // Settings
      settingsInfo <- {
        if (canDoAdminOperations && exportedData.getSettings != null) {
          mergeImportItemMaps(ctx.importItemMaps, toImportItemMaps(importItems, ImportItemType.Settings))
          loadExistingSystemSettingsData(canDoAdminOperations, ctx)
        } else {
          Future.successful((ctx, None))
        }
      }
      systemAdminOrganisationId <- Future.successful(settingsInfo._2)
      ctx <- Future.successful(settingsInfo._1)
      /*
       * Proceed with database updates
       */
      _ <- {
        var communityAdminOrganisationId = existingCommunityAdminOrganisationId
        val referenceUserEmails = mutable.Set.from(existingUserEmails)
        val dbAction = for {
          // Domain
          _ <- {
            if (proceedWithDomainImport) {
              completeDomainImportInternal(exportedCommunity.getDomain, ctx, canDoAdminOperations, linkedToCommunity = true)
            } else {
              DBIO.successful(())
            }
          }
          // Community
          _ <- {
            processFromArchive(ImportItemType.Community, exportedCommunity, exportedCommunity.getId, ctx,
              ImportCallbacks.set(
                (data: com.gitb.xml.export.Community, item: ImportItem) => {
                  val domainId = determineDomainIdForCommunityUpdate(exportedCommunity, None, ctx)
                  val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                  // This returns a tuple: (community ID, admin organisation ID)
                  communityManager.createCommunityInternal(models.Communities(0L,
                    ctx.importSettings.shortNameReplacement.getOrElse(data.getShortName),
                    ctx.importSettings.fullNameReplacement.getOrElse(data.getFullName),
                    Option(data.getSupportEmail),
                    selfRegistrationMethodToModel(data.getSelfRegistrationSettings.getMethod), Option(data.getSelfRegistrationSettings.getToken), Option(data.getSelfRegistrationSettings.getTokenHelpText),
                    data.getSelfRegistrationSettings.isNotifications, data.isInteractionNotification, Option(data.getDescription), selfRegistrationRestrictionToModel(data.getSelfRegistrationSettings.getRestriction),
                    data.getSelfRegistrationSettings.isForceTemplateSelection, data.getSelfRegistrationSettings.isForceRequiredProperties,
                    data.isAllowCertificateDownload, data.isAllowStatementManagement, data.isAllowSystemManagement,
                    data.isAllowPostTestOrganisationUpdates, data.isAllowSystemManagement, data.isAllowPostTestStatementUpdates, data.isAllowAutomationApi, data.isAllowCommunityView,
                    apiKey, None, domainId
                  ), checkApiKeyUniqueness = true)
                },
                (data: com.gitb.xml.export.Community, targetKey: String, item: ImportItem) => {
                  val domainId = determineDomainIdForCommunityUpdate(exportedCommunity, targetCommunity, ctx)
                  val apiKey = Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey())
                  communityManager.updateCommunityInternal(targetCommunity.get, data.getShortName, data.getFullName, Option(data.getSupportEmail),
                    selfRegistrationMethodToModel(data.getSelfRegistrationSettings.getMethod), Option(data.getSelfRegistrationSettings.getToken), Option(data.getSelfRegistrationSettings.getTokenHelpText), data.getSelfRegistrationSettings.isNotifications,
                    data.isInteractionNotification, Option(data.getDescription), selfRegistrationRestrictionToModel(data.getSelfRegistrationSettings.getRestriction),
                    data.getSelfRegistrationSettings.isForceTemplateSelection, data.getSelfRegistrationSettings.isForceRequiredProperties,
                    data.isAllowCertificateDownload, data.isAllowStatementManagement, data.isAllowSystemManagement,
                    data.isAllowPostTestOrganisationUpdates, data.isAllowSystemManagement, data.isAllowPostTestStatementUpdates, Some(data.isAllowAutomationApi), data.isAllowCommunityView, Some(apiKey),
                    domainId, checkApiKeyUniqueness = true, ctx.onSuccessCalls
                  )
                },
                None,
                (data: com.gitb.xml.export.Community, targetKey: String, newId: Any, item: ImportItem) => {
                  val ids: (Long, Long) = newId.asInstanceOf[(Long, Long)] // (community ID, admin organisation ID)
                  // Set on import item.
                  item.targetKey = Some(ids._1.toString)
                  // Record community ID.
                  addIdToProcessedIdMap(ImportItemType.Community, data.getId, ids._1.toString, ctx)
                  // Record admin organisation ID.
                  communityAdminOrganisationId = Some(ids._2)
                }
              ).withFnForSkipButProcessChildren((data: com.gitb.xml.export.Community, targetKey: String, item: ImportItem) => {
                /*
                 Exceptional case for community import to make sure we always update its domain. Not doing so would create problems for
                 triggers with domain parameters and conformance statements. We cannot do these checks using a normal approach on the ID
                 being processed as the domain is processed separately.
                 */
                val domainId = determineDomainIdForCommunityUpdate(exportedCommunity, targetCommunity, ctx)
                communityManager.updateCommunityDomain(targetCommunity.get, domainId, ctx.onSuccessCalls)
              })
            )
          }
          // Certificate settings
          _ <- {
            val communityId = getProcessedDbId(exportedCommunity, ImportItemType.Community, ctx)
            if (communityId.isDefined) {
              if (exportedCommunity.getConformanceCertificateSettings == null) {
                // Delete
                communityManager.deleteConformanceCertificateSettings(communityId.get)
              } else {
                // Update/Add
                for {
                  _ <- reportManager.updateConformanceCertificateSettingsInternal(
                    models.ConformanceCertificate(
                      0L, Option(exportedCommunity.getConformanceCertificateSettings.getTitle),
                      exportedCommunity.getConformanceCertificateSettings.isAddTitle, exportedCommunity.getConformanceCertificateSettings.isAddMessage, exportedCommunity.getConformanceCertificateSettings.isAddResultOverview,
                      exportedCommunity.getConformanceCertificateSettings.isAddTestCases, exportedCommunity.getConformanceCertificateSettings.isAddDetails,
                      exportedCommunity.getConformanceCertificateSettings.isAddSignature, exportedCommunity.getConformanceCertificateSettings.isAddPageNumbers,
                      Option(exportedCommunity.getConformanceCertificateSettings.getMessage), communityId.get
                    )
                  )
                  _ <- {
                    if (exportedCommunity.getConformanceCertificateSettings.getSignature != null) {
                      // This case is added here for backwards compatibility. Signature settings are normally added directly under the community.
                      handleCommunityKeystore(exportedCommunity.getConformanceCertificateSettings.getSignature, communityId.get, importSettings)
                    } else {
                      DBIO.successful(())
                    }
                  }
                } yield ()
              }
            } else {
              DBIO.successful(())
            }
          }
          // Certificate overview settings
          _ <- {
            DBIO.successful(())
            val communityId = getProcessedDbId(exportedCommunity, ImportItemType.Community, ctx)
            if (communityId.isDefined) {
              if (exportedCommunity.getConformanceOverviewCertificateSettings == null) {
                // Delete
                communityManager.deleteConformanceOverviewCertificateSettings(communityId.get)
              } else {
                // Update/Add
                reportManager.updateConformanceOverviewCertificateSettingsInternal(
                  toModelConformanceOverCertificateSettingsWithMessages(exportedCommunity.getConformanceOverviewCertificateSettings, communityId.get, ctx)
                )
              }
            } else {
              DBIO.successful(())
            }
          }
          // Signature settings
          _ <- {
            val communityId = getProcessedDbId(exportedCommunity, ImportItemType.Community, ctx)
            if (communityId.isDefined) {
              if (exportedCommunity.getSignatureSettings == null) {
                // Delete
                communityManager.deleteCommunityKeystoreInternal(communityId.get)
              } else {
                // Update/Add
                handleCommunityKeystore(exportedCommunity.getSignatureSettings, communityId.get, importSettings)
              }
            } else {
              DBIO.successful(())
            }
          }
          // Community report stylesheets
          _ <- {
            val communityId = getProcessedDbId(exportedCommunity, ImportItemType.Community, ctx)
            if (communityId.isDefined && exportedCommunity.getReportStylesheets != null) {
              var hasConformanceOverview = false
              var hasConformanceStatement = false
              var hasTestCase = false
              var hasTestStep = false
              var hasConformanceOverviewCertificate = false
              var hasConformanceStatementCertificate = false
              exportedCommunity.getReportStylesheets.getStylesheet.forEach { stylesheet =>
                val tempFile = stringToTempFile(stylesheet.getContent).toPath
                val reportType = toModelReportType(stylesheet.getReportType)
                reportType match {
                  case models.Enums.ReportType.ConformanceOverviewReport => hasConformanceOverview = true
                  case models.Enums.ReportType.ConformanceStatementReport => hasConformanceStatement = true
                  case models.Enums.ReportType.TestCaseReport => hasTestCase = true
                  case models.Enums.ReportType.TestStepReport => hasTestStep = true
                  case models.Enums.ReportType.ConformanceStatementCertificate => hasConformanceStatementCertificate = true
                  case models.Enums.ReportType.ConformanceOverviewCertificate => hasConformanceOverviewCertificate = true
                }
                ctx.onSuccessCalls += (() => repositoryUtils.saveCommunityReportStylesheet(communityId.get, reportType, tempFile))
                ctx.onSuccessCalls += (() => if (Files.exists(tempFile)) { FileUtils.deleteQuietly(tempFile.toFile) })
                ctx.onFailureCalls += (() => if (Files.exists(tempFile)) { FileUtils.deleteQuietly(tempFile.toFile) })
              }
              if (!hasConformanceOverview) ctx.onSuccessCalls += (() => repositoryUtils.deleteCommunityReportStylesheet(communityId.get, models.Enums.ReportType.ConformanceOverviewReport))
              if (!hasConformanceStatement) ctx.onSuccessCalls += (() => repositoryUtils.deleteCommunityReportStylesheet(communityId.get, models.Enums.ReportType.ConformanceStatementReport))
              if (!hasTestCase) ctx.onSuccessCalls += (() => repositoryUtils.deleteCommunityReportStylesheet(communityId.get, models.Enums.ReportType.TestCaseReport))
              if (!hasTestStep) ctx.onSuccessCalls += (() => repositoryUtils.deleteCommunityReportStylesheet(communityId.get, models.Enums.ReportType.TestStepReport))
              if (!hasConformanceOverviewCertificate) ctx.onSuccessCalls += (() => repositoryUtils.deleteCommunityReportStylesheet(communityId.get, models.Enums.ReportType.ConformanceOverviewCertificate))
              if (!hasConformanceStatementCertificate) ctx.onSuccessCalls += (() => repositoryUtils.deleteCommunityReportStylesheet(communityId.get, models.Enums.ReportType.ConformanceStatementCertificate))
            }
            DBIO.successful(())
          }
          // Community report settings
          _ <- {
            val communityId = getProcessedDbId(exportedCommunity, ImportItemType.Community, ctx)
            if (communityId.isDefined && exportedCommunity.getReportSettings != null) {
              val dbActions = ListBuffer[DBIO[_]]()
              exportedCommunity.getReportSettings.getReportSetting.forEach { exportedSetting =>
                dbActions += reportManager.updateReportSettingsInternal(toModelReportSetting(exportedSetting, communityId.get), None, ctx.onSuccessCalls)
              }
              toDBIO(dbActions)
            } else {
              DBIO.successful(())
            }
          }
          // Custom labels
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getCustomLabels != null) {
              exportedCommunity.getCustomLabels.getLabel.asScala.foreach { exportedLabel =>
                dbActions += processFromArchive(ImportItemType.CustomLabel, exportedLabel, exportedLabel.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.CustomLabel, item: ImportItem) => {
                      val communityId = item.parentItem.get.targetKey.get.toLong
                      val labelObject = toModelCustomLabel(data, communityId)
                      val key = s"${communityId}_${labelObject.labelType}"
                      if (!hasExisting(ImportItemType.CustomLabel, key, ctx)) {
                        for {
                          _ <- communityManager.createCommunityLabel(labelObject)
                          key <- DBIO.successful(key)
                        } yield key
                      } else {
                        DBIO.successful(())
                      }
                    },
                    (data: com.gitb.xml.export.CustomLabel, targetKey: String, item: ImportItem) => {
                      val keyParts = StringUtils.split(targetKey, "_") // [community_id]_[label_type]
                      for {
                        _ <- communityManager.deleteCommunityLabel(keyParts(0).toLong, keyParts(1).toShort)
                        _ <- communityManager.createCommunityLabel(toModelCustomLabel(data, item.parentItem.get.targetKey.get.toLong))
                      } yield ()
                    }
                  )
                )
              }
            }
            toDBIO(dbActions)
          }
          _ <- {
            processRemaining(ImportItemType.CustomLabel, ctx,
              (targetKey: String, item: ImportItem) => {
                val keyParts = StringUtils.split(targetKey, "_") // [community_id]_[label_type]
                communityManager.deleteCommunityLabel(keyParts(0).toLong, keyParts(1).toShort)
              }
            )
          }
          // Organisation properties
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getOrganisationProperties != null) {
              exportedCommunity.getOrganisationProperties.getProperty.asScala.foreach { exportedProperty =>
                dbActions += processFromArchive(ImportItemType.OrganisationProperty, exportedProperty, exportedProperty.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.OrganisationProperty, item: ImportItem) => {
                      communityManager.createOrganisationParameterInternal(toModelOrganisationParameter(data, item.parentItem.get.targetKey.get.toLong, None))
                    },
                    (data: com.gitb.xml.export.OrganisationProperty, targetKey: String, item: ImportItem) => {
                      communityManager.updateOrganisationParameterInternal(toModelOrganisationParameter(data, item.parentItem.get.targetKey.get.toLong, Some(targetKey.toLong)), updateDisplayOrder = true, ctx.onSuccessCalls)
                    }
                  )
                )
              }
            }
            toDBIO(dbActions)
          }
          _ <- {
            processRemaining(ImportItemType.OrganisationProperty, ctx,
              (targetKey: String, item: ImportItem) => {
                communityManager.deleteOrganisationParameter(targetKey.toLong, ctx.onSuccessCalls)
              }
            )
          }
          // System properties
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getSystemProperties != null) {
              exportedCommunity.getSystemProperties.getProperty.asScala.foreach { exportedProperty =>
                dbActions += processFromArchive(ImportItemType.SystemProperty, exportedProperty, exportedProperty.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.SystemProperty, item: ImportItem) => {
                      communityManager.createSystemParameterInternal(toModelSystemParameter(data, item.parentItem.get.targetKey.get.toLong, None))
                    },
                    (data: com.gitb.xml.export.SystemProperty, targetKey: String, item: ImportItem) => {
                      communityManager.updateSystemParameterInternal(toModelSystemParameter(data, item.parentItem.get.targetKey.get.toLong, Some(targetKey.toLong)), updateDisplayOrder = true, ctx.onSuccessCalls)
                    }
                  )
                )
              }
            }
            toDBIO(dbActions)
          }
          _ <- {
            processRemaining(ImportItemType.SystemProperty, ctx,
              (targetKey: String, item: ImportItem) => {
                communityManager.deleteSystemParameter(targetKey.toLong, ctx.onSuccessCalls)
              }
            )
          }
          // Landing pages
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getLandingPages != null) {
              exportedCommunity.getLandingPages.getLandingPage.asScala.foreach { exportedContent =>
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
              (targetKey: String, item: ImportItem) => {
                landingPageManager.deleteLandingPageInternal(targetKey.toLong)
              }
            )
          }
          // Legal notices
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getLegalNotices != null) {
              exportedCommunity.getLegalNotices.getLegalNotice.asScala.foreach { exportedContent =>
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
              (targetKey: String, item: ImportItem) => {
                legalNoticeManager.deleteLegalNoticeInternal(targetKey.toLong)
              }
            )
          }
          // Error templates
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getErrorTemplates != null) {
              exportedCommunity.getErrorTemplates.getErrorTemplate.asScala.foreach { exportedContent =>
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
              (targetKey: String, item: ImportItem) => {
                errorTemplateManager.deleteErrorTemplateInternal(targetKey.toLong)
              }
            )
          }
          // Triggers
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getTriggers != null) {
              exportedCommunity.getTriggers.getTrigger.asScala.foreach { exportedContent =>
                dbActions += processFromArchive(ImportItemType.Trigger, exportedContent, exportedContent.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.Trigger, item: ImportItem) => {
                      triggerManager.createTriggerInternal(toModelTrigger(None, data, item.parentItem.get.targetKey.get.toLong, ctx))
                    },
                    (data: com.gitb.xml.export.Trigger, targetKey: String, item: ImportItem) => {
                      triggerManager.updateTriggerInternal(toModelTrigger(Some(targetKey.toLong), data, item.parentItem.get.targetKey.get.toLong, ctx))
                    }
                  )
                )
              }
            }
            toDBIO(dbActions)
          }
          _ <- {
            processRemaining(ImportItemType.Trigger, ctx,
              (targetKey: String, item: ImportItem) => {
                triggerHelper.deleteTriggerInternal(targetKey.toLong)
              }
            )
          }
          // Resources
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getResources != null) {
              exportedCommunity.getResources.getResource.asScala.foreach { exportedContent =>
                dbActions += processFromArchive(ImportItemType.CommunityResource, exportedContent, exportedContent.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.CommunityResource, item: ImportItem) => {
                      val fileToStore = dataUrlToTempFile(data.getContent)
                      ctx.onFailureCalls += (() => if (fileToStore.exists()) { FileUtils.deleteQuietly(fileToStore) })
                      communityResourceManager.createCommunityResourceInternal(toModelCommunityResource(data, item.parentItem.get.targetKey.get.toLong), fileToStore, ctx.onSuccessCalls)
                    },
                    (data: com.gitb.xml.export.CommunityResource, targetKey: String, item: ImportItem) => {
                      val fileToStore = dataUrlToTempFile(data.getContent)
                      ctx.onFailureCalls += (() => if (fileToStore.exists()) { FileUtils.deleteQuietly(fileToStore) })
                      communityResourceManager.updateCommunityResourceInternal(Some(item.parentItem.get.targetKey.get.toLong), targetKey.toLong, Some(data.getName), Some(Option(data.getDescription)), Some(fileToStore), ctx.onSuccessCalls)
                    }
                  )
                )
              }
            }
            toDBIO(dbActions)
          }
          _ <- {
            processRemaining(ImportItemType.CommunityResource, ctx,
              (targetKey: String, item: ImportItem) => {
                communityResourceManager.deleteCommunityResourceInternal(Some(item.parentItem.get.targetKey.get.toLong), targetKey.toLong, ctx.onSuccessCalls)
              }
            )
          }
          // Administrators
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (!Configurations.AUTHENTICATION_SSO_ENABLED && exportedCommunity.getAdministrators != null) {
              exportedCommunity.getAdministrators.getAdministrator.asScala.foreach { exportedUser =>
                dbActions += processFromArchive(ImportItemType.Administrator, exportedUser, exportedUser.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.CommunityAdministrator, item: ImportItem) => {
                      if (!referenceUserEmails.contains(exportedUser.getEmail.toLowerCase)) {
                        referenceUserEmails += exportedUser.getEmail.toLowerCase
                        PersistenceSchema.insertUser += toModelAdministrator(data, None, communityAdminOrganisationId.get, ctx.importSettings)
                      } else {
                        DBIO.successful(())
                      }
                    },
                    (data: com.gitb.xml.export.CommunityAdministrator, targetKey: String, item: ImportItem) => {
                      /*
                        We don't update the email as this must anyway be already matching (this was how the user was found
                        to be existing). Not updating the email avoids the need to check that the email is unique with respect
                        to other users.
                       */
                      val query = for {
                        user <- PersistenceSchema.users.filter(_.id === targetKey.toLong)
                      } yield (user.name, user.password, user.onetimePassword)
                      query.update(data.getName, decrypt(ctx.importSettings, data.getPassword), data.isOnetimePassword)
                    }
                  )
                )
              }
            }
            toDBIO(dbActions)
          }
          _ <- {
            if (!Configurations.AUTHENTICATION_SSO_ENABLED) {
              processRemaining(ImportItemType.Administrator, ctx,
                (targetKey: String, item: ImportItem) => {
                  val userId = targetKey.toLong
                  if (ownUserId.isDefined && ownUserId.get.longValue() != userId) {
                    // Avoid deleting self
                    PersistenceSchema.users.filter(_.id === userId).delete
                  } else {
                    DBIO.successful(())
                  }
                }
              )
            } else {
              DBIO.successful(())
            }
          }
          // Organisations
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getOrganisations != null) {
              exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
                dbActions += processFromArchive(ImportItemType.Organisation, exportedOrganisation, exportedOrganisation.getId, ctx,
                  ImportCallbacks.set(
                    (data: com.gitb.xml.export.Organisation, item: ImportItem) => {
                      for {
                        orgInfo <- {
                          organisationManager.createOrganizationWithRelatedData(
                            models.Organizations(
                              0L, data.getShortName, data.getFullName, OrganizationType.Vendor.id.toShort, adminOrganization = false,
                              getProcessedDbId(data.getLandingPage, ImportItemType.LandingPage, ctx),
                              getProcessedDbId(data.getLegalNotice, ImportItemType.LegalNotice, ctx),
                              getProcessedDbId(data.getErrorTemplate, ImportItemType.ErrorTemplate, ctx),
                              template = data.isTemplate, Option(data.getTemplateName), Option(data.getApiKey), item.parentItem.get.targetKey.get.toLong
                            ), None, None, None, copyOrganisationParameters = false, copySystemParameters = false, copyStatementParameters = false,
                            checkApiKeyUniqueness = true, setDefaultPropertyValues = false, ctx.onSuccessCalls
                          )
                        }
                      } yield orgInfo.organisationId
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
                          None, data.isTemplate, Option(data.getTemplateName), Some(Option(data.getApiKey)), None, None,
                          copyOrganisationParameters = false, copySystemParameters = false, copyStatementParameters = false,
                          checkApiKeyUniqueness = true, ctx.onSuccessCalls
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
              (targetKey: String, item: ImportItem) => {
                if (communityAdminOrganisationId.get.longValue() == targetKey.toLong.longValue()) {
                  // Prevent deleting the community's admin organisation.
                  DBIO.successful(())
                } else {
                  organisationManager.deleteOrganization(targetKey.toLong, ctx.onSuccessCalls)
                }
              }
            )
          }
          // Organisation users
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (!Configurations.AUTHENTICATION_SSO_ENABLED && exportedCommunity.getOrganisations != null) {
              exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
                if (exportedOrganisation.getUsers != null) {
                  exportedOrganisation.getUsers.getUser.asScala.foreach { exportedUser =>
                    dbActions += processFromArchive(ImportItemType.OrganisationUser, exportedUser, exportedUser.getId, ctx,
                      ImportCallbacks.set(
                        (data: com.gitb.xml.export.OrganisationUser, item: ImportItem) => {
                          if (!referenceUserEmails.contains(exportedUser.getEmail.toLowerCase)) {
                            referenceUserEmails += exportedUser.getEmail.toLowerCase
                            PersistenceSchema.insertUser += toModelOrganisationUser(data, None, toModelUserRole(data.getRole), item.parentItem.get.targetKey.get.toLong, ctx.importSettings)
                          } else {
                            DBIO.successful(())
                          }
                        },
                        (data: com.gitb.xml.export.OrganisationUser, targetKey: String, item: ImportItem) => {
                          /*
                            We don't update the email as this must anyway be already matching (this was how the user was found
                            to be existing). Not updating the email avoids the need to check that the email is unique with respect
                            to other users.
                           */
                          val q = for { u <- PersistenceSchema.users.filter(_.id === targetKey.toLong) } yield (u.name, u.password, u.onetimePassword, u.role)
                          q.update(data.getName, decrypt(ctx.importSettings, data.getPassword), data.isOnetimePassword, toModelUserRole(data.getRole))
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
            if (!Configurations.AUTHENTICATION_SSO_ENABLED) {
              processRemaining(ImportItemType.OrganisationUser, ctx,
                (targetKey: String, item: ImportItem) => {
                  PersistenceSchema.users.filter(_.id === targetKey.toLong).delete
                }
              )
            } else {
              DBIO.successful(())
            }
          }
          // Organisation property values
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getOrganisations != null) {
              exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
                if (exportedOrganisation.getPropertyValues != null) {
                  exportedOrganisation.getPropertyValues.getProperty.asScala.foreach { exportedValue =>
                    dbActions += processFromArchive(ImportItemType.OrganisationPropertyValue, exportedValue, exportedValue.getId, ctx,
                      ImportCallbacks.set(
                        (data: com.gitb.xml.export.OrganisationPropertyValue, item: ImportItem) => {
                          val relatedPropertyId = getProcessedDbId(data.getProperty, ImportItemType.OrganisationProperty, ctx)
                          if (relatedPropertyId.isDefined) {
                            val organisationId = item.parentItem.get.targetKey.get.toLong
                            // The property this value related to has either been updated or inserted.
                            val key = s"${organisationId}_${relatedPropertyId.get}"
                            if (!hasExisting(ImportItemType.OrganisationPropertyValue, key, ctx)) {
                              val fileData = parameterFileMetadata(ctx, data.getProperty.getType, isDomainParameter = false, data.getValue)
                              if (fileData._3.isDefined) {
                                ctx.onSuccessCalls += (() => repositoryUtils.setOrganisationPropertyFile(relatedPropertyId.get, organisationId, fileData._3.get))
                              }
                              for {
                                _ <- PersistenceSchema.organisationParameterValues += models.OrganisationParameterValues(organisationId, relatedPropertyId.get, manageEncryptionIfNeeded(ctx.importSettings, data.getProperty.getType, Option(data.getValue)).get, fileData._2)
                                key <- DBIO.successful(key)
                              } yield key
                            } else {
                              DBIO.successful(())
                            }
                          } else {
                            DBIO.successful(())
                          }
                        },
                        (data: com.gitb.xml.export.OrganisationPropertyValue, targetKey: String, item: ImportItem) => {
                          val keyParts = StringUtils.split(targetKey, "_") // target key: [organisation ID]_[property ID]
                          val organisationId = keyParts(0).toLong
                          val propertyId = keyParts(1).toLong
                          if (getProcessedDbId(data.getProperty, ImportItemType.OrganisationProperty, ctx).isDefined) {
                            val fileData = parameterFileMetadata(ctx, data.getProperty.getType, isDomainParameter = false, data.getValue)
                            if (fileData._3.isDefined) {
                              ctx.onSuccessCalls += (() => repositoryUtils.setOrganisationPropertyFile(propertyId, organisationId, fileData._3.get))
                            }
                            val q = for { p <- PersistenceSchema.organisationParameterValues.filter(_.organisation === organisationId).filter(_.parameter === propertyId) } yield (p.value, p.contentType)
                            q.update(manageEncryptionIfNeeded(ctx.importSettings, data.getProperty.getType, Some(data.getValue)).get, fileData._2)
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
              (targetKey: String, item: ImportItem) => {
                val keyParts = StringUtils.split(targetKey, "_") // target key: [organisation ID]_[property ID]
                val organisationId = keyParts(0).toLong
                val propertyId = keyParts(1).toLong
                ctx.onSuccessCalls += (() => repositoryUtils.deleteOrganisationPropertyFile(propertyId, organisationId))
                PersistenceSchema.organisationParameterValues.filter(_.organisation === organisationId).filter(_.parameter === propertyId).delete
              }
            )
          }
          // Systems
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getOrganisations != null) {
              exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
                if (exportedOrganisation.getSystems != null) {
                  exportedOrganisation.getSystems.getSystem.asScala.foreach { exportedSystem =>
                    dbActions += processFromArchive(ImportItemType.System, exportedSystem, exportedSystem.getId, ctx,
                      ImportCallbacks.set(
                        (data: com.gitb.xml.export.System, item: ImportItem) => {
                          systemManager.registerSystemInternal(models.Systems(0L, data.getShortName, data.getFullName, Option(data.getDescription), Option(data.getVersion), Option(data.getApiKey).getOrElse(CryptoUtil.generateApiKey()), Option(data.getBadgeKey).getOrElse(CryptoUtil.generateApiKey()), item.parentItem.get.targetKey.get.toLong), checkApiKeyUniqueness = true)
                        },
                        (data: com.gitb.xml.export.System, targetKey: String, item: ImportItem) => {
                          systemManager.updateSystemProfileInternal(None, targetCommunityId, item.targetKey.get.toLong, data.getShortName, data.getFullName, Option(data.getDescription), Option(data.getVersion), Option(data.getApiKey), Option(data.getBadgeKey),
                            None, None, None, copySystemParameters = false, copyStatementParameters = false,
                            checkApiKeyUniqueness = true, ctx.onSuccessCalls
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
              (targetKey: String, item: ImportItem) => {
                systemManager.deleteSystem(targetKey.toLong, ctx.onSuccessCalls)
              }
            )
          }
          // System property values
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getOrganisations != null) {
              exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
                if (exportedOrganisation.getSystems != null) {
                  exportedOrganisation.getSystems.getSystem.asScala.foreach { exportedSystem =>
                    if (exportedSystem.getPropertyValues != null) {
                      exportedSystem.getPropertyValues.getProperty.asScala.foreach { exportedValue =>
                        dbActions += processFromArchive(ImportItemType.SystemPropertyValue, exportedValue, exportedValue.getId, ctx,
                          ImportCallbacks.set(
                            (data: com.gitb.xml.export.SystemPropertyValue, item: ImportItem) => {
                              val relatedPropertyId = getProcessedDbId(data.getProperty, ImportItemType.SystemProperty, ctx)
                              if (relatedPropertyId.isDefined) {
                                val systemId = item.parentItem.get.targetKey.get.toLong
                                // The property this value related to has either been updated or inserted.
                                val key = s"${systemId}_${relatedPropertyId.get}"
                                if (!hasExisting(ImportItemType.SystemPropertyValue, key, ctx)) {
                                  val fileData = parameterFileMetadata(ctx, data.getProperty.getType, isDomainParameter = false, data.getValue)
                                  if (fileData._3.isDefined) {
                                    ctx.onSuccessCalls += (() => repositoryUtils.setSystemPropertyFile(relatedPropertyId.get, systemId, fileData._3.get))
                                  }
                                  for {
                                    _ <- PersistenceSchema.systemParameterValues += models.SystemParameterValues(systemId, relatedPropertyId.get, manageEncryptionIfNeeded(ctx.importSettings, data.getProperty.getType, Option(data.getValue)).get, fileData._2)
                                    key <- DBIO.successful(key)
                                  } yield key
                                } else {
                                  DBIO.successful(())
                                }
                              } else {
                                DBIO.successful(())
                              }
                            },
                            (data: com.gitb.xml.export.SystemPropertyValue, targetKey: String, item: ImportItem) => {
                              val keyParts = StringUtils.split(targetKey, "_") // target key: [system ID]_[property ID]
                              val systemId = keyParts(0).toLong
                              val propertyId = keyParts(1).toLong
                              if (getProcessedDbId(data.getProperty, ImportItemType.SystemProperty, ctx).isDefined) {
                                val fileData = parameterFileMetadata(ctx, data.getProperty.getType, isDomainParameter = false, data.getValue)
                                if (fileData._3.isDefined) {
                                  ctx.onSuccessCalls += (() => repositoryUtils.setSystemPropertyFile(propertyId, systemId, fileData._3.get))
                                }
                                val q = for { p <- PersistenceSchema.systemParameterValues.filter(_.system === systemId).filter(_.parameter === propertyId) } yield (p.value, p.contentType)
                                q.update(manageEncryptionIfNeeded(ctx.importSettings, data.getProperty.getType, Some(data.getValue)).get, fileData._2)
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
              (targetKey: String, item: ImportItem) => {
                val keyParts = StringUtils.split(targetKey, "_") // target key: [system ID]_[property ID]
                val systemId = keyParts(0).toLong
                val propertyId = keyParts(1).toLong
                ctx.onSuccessCalls += (() => repositoryUtils.deleteSystemPropertyFile(propertyId, systemId))
                PersistenceSchema.systemParameterValues.filter(_.system === systemId).filter(_.parameter === propertyId).delete
              }
            )
          }
          // Statements
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getOrganisations != null) {
              exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
                if (exportedOrganisation.getSystems != null) {
                  exportedOrganisation.getSystems.getSystem.asScala.foreach { exportedSystem =>
                    if (exportedSystem.getStatements != null) {
                      exportedSystem.getStatements.getStatement.asScala.foreach { exportedStatement =>
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
                                val key = s"${systemId}_${relatedActorId.get}"
                                if (!hasExisting(ImportItemType.Statement, key, ctx)) {
                                  for {
                                    _ <- systemManager.defineConformanceStatement(systemId, relatedSpecId.get, relatedActorId.get, setDefaultParameterValues = false)
                                    key <- DBIO.successful(key)
                                  } yield key
                                } else {
                                  DBIO.successful(())
                                }
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
              (targetKey: String, item: ImportItem) => {
                // Key: [System ID]_[actor ID]
                val keyParts = StringUtils.split(targetKey, "_")
                val systemId = keyParts(0).toLong
                val actorId = keyParts(1).toLong
                systemManager.deleteConformanceStatements(systemId, List(actorId), ctx.onSuccessCalls)
              }
            )
          }
          // Statement configurations
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (exportedCommunity.getOrganisations != null) {
              exportedCommunity.getOrganisations.getOrganisation.asScala.foreach { exportedOrganisation =>
                if (exportedOrganisation.getSystems != null) {
                  exportedOrganisation.getSystems.getSystem.asScala.foreach { exportedSystem =>
                    if (exportedSystem.getStatements != null) {
                      exportedSystem.getStatements.getStatement.asScala.foreach { exportedStatement =>
                        if (exportedStatement.getConfigurations != null) {
                          exportedStatement.getConfigurations.getConfiguration.asScala.foreach { exportedValue =>
                            dbActions += processFromArchive(ImportItemType.StatementConfiguration, exportedValue, exportedValue.getId, ctx,
                              ImportCallbacks.set(
                                (data: com.gitb.xml.export.Configuration, item: ImportItem) => {
                                  val relatedParameterId = getProcessedDbId(data.getParameter, ImportItemType.EndpointParameter, ctx)
                                  var relatedEndpointId: Option[Long] = None
                                  if (data.getParameter != null) {
                                    relatedEndpointId = getProcessedDbId(data.getParameter.getEndpoint, ImportItemType.Endpoint, ctx)
                                  }
                                  if (relatedParameterId.isDefined && relatedEndpointId.isDefined) {
                                    val statementTargetKeyParts = StringUtils.split(item.parentItem.get.targetKey.get, "_") // [System ID]_[Actor ID]
                                    val relatedActorId = statementTargetKeyParts(1).toLong
                                    val relatedSystemId = item.parentItem.get.parentItem.get.targetKey.get.toLong // Statement -> System
                                    val key = s"${relatedActorId}_${relatedEndpointId.get}_${relatedSystemId}_${relatedParameterId.get}"
                                    if (!hasExisting(ImportItemType.StatementConfiguration, key, ctx)) {
                                      val fileData = parameterFileMetadata(ctx, data.getParameter.getType, isDomainParameter = false, data.getValue)
                                      for {
                                        _ <- systemManager.saveEndpointConfigurationInternal(forceAdd = true, forceUpdate = false,
                                          models.Configs(relatedSystemId, relatedParameterId.get, relatedEndpointId.get, manageEncryptionIfNeeded(ctx.importSettings, data.getParameter.getType, Option(data.getValue)).get, fileData._2), fileData._3, ctx.onSuccessCalls)
                                        key <- DBIO.successful(s"${relatedEndpointId.get}_${relatedSystemId}_${relatedParameterId.get}")
                                      } yield key
                                    } else {
                                      DBIO.successful(())
                                    }
                                  } else {
                                    DBIO.successful(())
                                  }
                                },
                                (data: com.gitb.xml.export.Configuration, targetKey: String, item: ImportItem) => {
                                  val configKeyParts = StringUtils.split(targetKey, "_") // [Actor ID]_[endpoint ID]_[System ID]_[parameter ID]
                                  val relatedActorId = configKeyParts(0).toLong
                                  val relatedParameterId = getProcessedDbId(data.getParameter, ImportItemType.EndpointParameter, ctx)
                                  var relatedEndpointId: Option[Long] = None
                                  if (data.getParameter != null) {
                                    relatedEndpointId = getProcessedDbId(data.getParameter.getEndpoint, ImportItemType.Endpoint, ctx)
                                  }
                                  if (relatedParameterId.isDefined && relatedEndpointId.isDefined) {
                                    val fileData = parameterFileMetadata(ctx, data.getParameter.getType, isDomainParameter = false, data.getValue)
                                    val relatedSystemId = item.parentItem.get.parentItem.get.targetKey.get.toLong // Statement -> System
                                    for {
                                      _ <- systemManager.saveEndpointConfigurationInternal(forceAdd = false, forceUpdate = true,
                                        models.Configs(relatedSystemId, relatedParameterId.get, relatedEndpointId.get, manageEncryptionIfNeeded(ctx.importSettings, data.getParameter.getType, Option(data.getValue)).get, fileData._2), fileData._3, ctx.onSuccessCalls)
                                      key <- DBIO.successful(s"${relatedActorId}_${relatedEndpointId.get}_${relatedSystemId}_${relatedParameterId.get}")
                                    } yield key
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
              (targetKey: String, item: ImportItem) => {
                // Key: [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
                val keyParts = StringUtils.split(targetKey, "_")
                val endpointId = keyParts(1).toLong
                val systemId = keyParts(2).toLong
                val parameterId = keyParts(3).toLong
                systemManager.deleteEndpointConfigurationInternal(systemId, parameterId, endpointId, ctx.onSuccessCalls)
              }
            )
          }
          // Settings
          // We process this here to allow a possible demo account setting to match a previously imported user.
          _ <- {
            if (canDoAdminOperations && exportedData.getSettings != null) {
              completeSystemSettingsImportInternal(exportedData.getSettings, ctx, canDoAdminOperations, ownUserId, referenceUserEmails, systemAdminOrganisationId)
            } else {
              DBIO.successful(())
            }
          }
        } yield ()
        DB.run(completeFileSystemFinalisation(ctx, dbAction).transactionally)
      }
    } yield ()
  }

  private def recordProcessedArchive(archiveHash: String): Future[Unit] = {
    DB.run(PersistenceSchema.insertProcessedArchive += ProcessedArchive(0L, archiveHash, TimeUtil.getCurrentTimestamp())).map(_ => ())
  }

  private def findProcessedArchive(archiveHash: String): Future[Option[ProcessedArchive]] = {
    DB.run(PersistenceSchema.processedArchives.filter(_.hash === archiveHash).result.headOption)
  }

  def importSandboxData(archive: File, archiveKey: String): Future[SandboxImportResult] = {
    val task = for {
      resultWithHash <- {
        var archiveHash: Option[String] = None
        Using.resource(Files.newInputStream(archive.toPath)) { inputStream =>
          archiveHash = Some(DigestUtils.sha256Hex(inputStream))
        }
        findProcessedArchive(archiveHash.get).map { processedArchive =>
          if (processedArchive.isDefined) {
            logger.info("Skipping data archive ["+archive.getName+"] as it has already been processed on ["+TimeUtil.serializeTimestamp(processedArchive.get.processTime)+"]")
            (Some(SandboxImportResult.incomplete()), archiveHash.get)
          } else {
            (None, archiveHash.get)
          }
        }
      }
      result <- {
        if (resultWithHash._1.isDefined) {
          Future.successful(resultWithHash._1.get)
        } else {
          logger.info("Processing data archive [" + archive.getName + "]")
          val importSettings = new ImportSettings()
          importSettings.encryptionKey = Some(archiveKey)
          importSettings.sandboxImport = true
          importPreviewManager.prepareImportPreview(archive, importSettings, requireDomain = false, requireCommunity = false, requireSettings = false, requireDeletions = false).flatMap { preparationResult =>
            val task = if (preparationResult._1.isDefined) {
              logger.warn("Unable to process data archive ["+archive.getName+"]: " + preparationResult._1.get._2)
              Future.successful {
                SandboxImportResult.incomplete(preparationResult._1.get._2)
              }
            } else {
              if (preparationResult._2.get.getCommunities != null && !preparationResult._2.get.getCommunities.getCommunity.isEmpty) {
                // Community import.
                val exportData = preparationResult._2.get
                // Step 1 - prepare import.
                var importItems: List[ImportItem] = null
                // Check to see if a community can be matched by the defined (in the archive) API key.
                communityManager.getByApiKey(exportData.getCommunities.getCommunity.get(0).getApiKey).flatMap { community =>
                  val targetCommunityId = community.map(_.id)
                  importPreviewManager.previewCommunityImport(exportData, targetCommunityId, canDoAdminOperations = true, new ImportSettings).flatMap { previewResult =>
                    val items = new ListBuffer[ImportItem]()
                    // First add domain.
                    if (previewResult._2.isDefined) {
                      items += previewResult._2.get
                    }
                    // Next add community.
                    items += previewResult._1.get
                    // Finally add system settings.
                    if (previewResult._3.isDefined) {
                      items += previewResult._3.get
                    }
                    importItems = items.toList
                    // Set all import items to proceed.
                    approveImportItems(importItems)
                    // Step 2 - Import.
                    importSettings.dataFilePath = Some(importPreviewManager.getPendingImportFile(preparationResult._4.get, preparationResult._3.get).get.toPath)
                    completeCommunityImport(exportData, importSettings, importItems, targetCommunityId, canDoAdminOperations = true, None).map { _ =>
                      // Avoid processing this archive again.
                      SandboxImportResult.complete()
                    }
                  }
                }
              } else if (preparationResult._2.get.getDomains != null && !preparationResult._2.get.getDomains.getDomain.isEmpty) {
                // Domain import.
                val exportedDomain = preparationResult._2.get.getDomains.getDomain.get(0)
                // Step 1 - prepare import.
                // Check to see if a domain can be matched by the defined (in the archive) API key.
                domainManager.getByApiKey(exportedDomain.getApiKey).flatMap { domain =>
                  val targetDomainId = domain.map(_.id)
                  importPreviewManager.previewDomainImport(exportedDomain, targetDomainId, canDoAdminOperations = true, new ImportSettings).flatMap { previewResult =>
                    val importItems = List(previewResult)
                    // Set all import items to proceed.
                    approveImportItems(importItems)
                    // Step 2 - Import.
                    importSettings.dataFilePath = Some(importPreviewManager.getPendingImportFile(preparationResult._4.get, preparationResult._3.get).get.toPath)
                    completeDomainImport(exportedDomain, importSettings, importItems, targetDomainId, canAddOrDeleteDomain = true).map { _ =>
                      // Avoid processing this archive again.
                      SandboxImportResult.complete()
                    }
                  }
                }
              } else if (preparationResult._2.get.getSettings != null) {
                // System settings import.
                val exportedSettings = preparationResult._2.get.getSettings
                // Step 1 - prepare import.
                importPreviewManager.previewSystemSettingsImport(exportedSettings).flatMap { previewResult =>
                  val importItems = List(previewResult._1)
                  // Set all import items to proceed.
                  approveImportItems(importItems)
                  // Step 2 - Import.
                  importSettings.dataFilePath = Some(importPreviewManager.getPendingImportFile(preparationResult._4.get, preparationResult._3.get).get.toPath)
                  completeSystemSettingsImport(exportedSettings, importSettings, importItems, canManageSettings = true, None).map { _ =>
                    // Avoid processing this archive again.
                    SandboxImportResult.complete()
                  }
                }
              } else if (preparationResult._2.get.getDeletions != null) {
                // Deletions import.
                val exportedDeletions = preparationResult._2.get.getDeletions
                // Step 1 - prepare import.
                importPreviewManager.previewDeletionsImport(exportedDeletions).flatMap { previewResult =>
                  // Set all import items to proceed.
                  approveImportItems(previewResult)
                  // Step 2 - Import.
                  importSettings.dataFilePath = Some(importPreviewManager.getPendingImportFile(preparationResult._4.get, preparationResult._3.get).get.toPath)
                  completeDeletionsImport(exportedDeletions, importSettings, previewResult).map { _ =>
                    // Avoid processing this archive again.
                    SandboxImportResult.complete()
                  }
                }
              } else {
                val result = SandboxImportResult.incomplete("Provided data archive is empty")
                logger.warn(result.errorMessage.get)
                Future.successful(result)
              }
            }
            task.andThen { _ =>
              if (preparationResult._4.isDefined) {
                FileUtils.deleteQuietly(preparationResult._4.get.toFile)
              }
            }
          }
        }
      }
    } yield (result, resultWithHash._2)
    task.flatMap { taskOutput =>
      val recordTask = if (taskOutput._1.processingComplete) {
        recordProcessedArchive(taskOutput._2)
      } else {
        Future.successful(())
      }
      recordTask.map { _ =>
        logger.info("Finished processing data archive ["+archive.getName+"]")
        taskOutput._1
      }
    }.recover {
      case e:Exception =>
        logger.warn("Unexpected exception while processing data archive ["+archive.getName+"]", e)
        SandboxImportResult.incomplete()
    }
  }

  private def approveImportItems(items: List[ImportItem]): Unit = {
    items.foreach { item =>
      if (item.itemChoice.isEmpty) {
        item.itemChoice = Some(ImportItemChoice.Proceed)
        if (item.childrenItems.nonEmpty) {
          approveImportItems(item.childrenItems.toList)
        }
      }
    }
  }

  private def dataUrlToTempFile(dataUrl: String, suffix: Option[String] = None): File = {
    Files.write(Files.createTempFile("itb", suffix.orNull), Base64.decodeBase64(MimeUtil.getBase64FromDataURL(dataUrl))).toFile
  }

  private def stringToTempFile(content: String, suffix: Option[String] = None): File = {
    Files.writeString(Files.createTempFile("itb", suffix.orNull), content).toFile
  }

  private def parameterFileMetadata(ctx: ImportContext, parameterType: PropertyType, isDomainParameter: Boolean, parameterValue: String): (String, Option[String], Option[File]) = {
    val kind = propertyTypeToKind(parameterType, isDomainParameter)
    var fileToStore: Option[File] = None
    var contentType: Option[String] = None
    if (kind == "BINARY") {
      contentType = Some(MimeUtil.getMimeTypeFromDataURL(parameterValue))
      fileToStore = Some(dataUrlToTempFile(parameterValue))
      ctx.onFailureCalls += (() => if (fileToStore.get.exists()) { FileUtils.deleteQuietly(fileToStore.get) })
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
        require(ClamAVClient.isCleanReply(virusScanner.scan(fileToStore.get)), "A virus was found in one of the imported files")
      }
    }
    (kind, contentType, fileToStore)
  }

}
