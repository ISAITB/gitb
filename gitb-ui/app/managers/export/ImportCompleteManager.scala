package managers.export

import java.io.File
import java.nio.file.{Files, Paths}

import com.gitb.xml.export.{SelfRegistrationRestriction => _, _}
import config.Configurations
import javax.inject.{Inject, Singleton}
import managers._
import models.Enums.ImportItemType.ImportItemType
import models.Enums._
import models.{Enums, TestSuiteUploadItemResult}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{ClamAVClient, MimeUtil, RepositoryUtils}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ImportCompleteManager @Inject()(exportManager: ExportManager, communityManager: CommunityManager, conformanceManager: ConformanceManager, specificationManager: SpecificationManager, actorManager: ActorManager, endpointManager: EndPointManager, parameterManager: ParameterManager, testSuiteManager: TestSuiteManager, landingPageManager: LandingPageManager, legalNoticeManager: LegalNoticeManager, errorTemplateManager: ErrorTemplateManager, organisationManager: OrganizationManager, systemManager: SystemManager, importPreviewManager: ImportPreviewManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  private def logger = LoggerFactory.getLogger("ImportCompleteManager")

  import dbConfig.profile.api._

  import scala.collection.JavaConversions._

  def completeDomainImport(exportedDomain: com.gitb.xml.export.Domain, importSettings: ImportSettings, importItems: List[ImportItem], targetDomainId: Option[Long], canAddOrDeleteDomain: Boolean): Unit = {
    // Load context
    val ctx = ImportContext(
      importSettings,
      toImportItemMaps(importItems, ImportItemType.Domain),
      ExistingIds.init(),
      ImportTargets.fromImportItems(importItems),
      mutable.Map[ImportItemType, mutable.Map[String, String]](),
      mutable.Map[Long, mutable.Map[String, Long]](),
      mutable.ListBuffer[() => _](),
      mutable.ListBuffer[() => _]()
    )
    exec(completeFileSystemFinalisation(ctx, completeDomainImportInternal(exportedDomain, targetDomainId, ctx, canAddOrDeleteDomain)).transactionally)
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
      } else {
        // No source key for the parent. We should normally never reach this point.
        logger.warn("Item ["+importItem.sourceKey.get+"]["+importItem.itemType+"] being checked for processing of which the parent source is not available")
        false
      }
    }
  }

  private def processFromArchive[A](itemType: ImportItemType, data: A, itemId: String, ctx: ImportContext, importCallbacks: ImportCallbacks[A]): DBIO[_] = {
    var dbAction: Option[DBIO[_]] = None
    if (ctx.importItemMaps.sourceMap.containsKey(itemType)) {
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
                    importItem.get.targetKey = Some(newId.toString)
                    // Add to processed ID map..
                    addIdToProcessedIdMap(itemType, itemId, newId.toString, ctx)
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

  private def propertyTypeToKind(propertyType: PropertyType, isDomainParameter: Boolean): String = {
    require(propertyType != null, "Enum value cannot be null")
    propertyType match {
      case PropertyType.BINARY => "BINARY"
      case PropertyType.SIMPLE => "SIMPLE"
      case PropertyType.SECRET => {
        if (isDomainParameter) {
          "HIDDEN"
        } else {
          "SECRET"
        }
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

  private def isAvailableInDb(dataId: String, itemType: ImportItemType, ctx: ImportContext): Boolean = {
    dataId != null && ctx.processedIdMap.containsKey(itemType) && ctx.processedIdMap(itemType).containsKey(dataId)
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
    ctx.onFailureCalls += (() => {
      // Cleanup operation in case an error occurred.
      if (testSuiteFile.exists()) {
        FileUtils.deleteDirectory(testSuiteFile)
      }
    })
    // Process DB operations
    val action = for {
      // Save test suite
      testSuiteId <- PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)) += toModelTestSuite(data, specificationId, testSuiteFile.getName)
      // Lookup the map of systems to actors for the specification
      systemActors <- testSuiteManager.getSystemActors(specificationId)
      // Create a map of actors to systems.
      existingActorToSystemMap <- testSuiteManager.getExistingActorToSystemMap(systemActors)
      // Save test cases
      processTestCasesStep <- {
        if (ctx.savedSpecificationActors.containsKey(specificationId)) {
          testSuiteManager.stepProcessTestCases(
            specificationId,
            testSuiteId,
            Some(toModelTestCases(data.getTestCases.getTestCase.toList, specificationId)),
            getResourcePaths(testSuiteFile.getName, data.getTestCases.getTestCase.toList),
            new java.util.HashMap[String, java.lang.Long](), // existingTestCaseMap
            ctx.savedSpecificationActors(specificationId), // savedActorIds
            existingActorToSystemMap
          )
        } else {
          DBIO.successful((new java.util.ArrayList[Long](), List[TestSuiteUploadItemResult]()))
        }
      }
      // Update the actor links for the  test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteActorLinks(testSuiteId, getSavedActorMap(data, specificationId, ctx))
      // Update the test case links for the test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteTestCaseLinks(testSuiteId, processTestCasesStep._1)
    } yield testSuiteId
    action
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
        if (ctx.savedSpecificationActors.containsKey(specificationId)) {
          testSuiteManager.stepProcessTestCases(
            specificationId,
            testSuiteId,
            Some(toModelTestCases(data.getTestCases.getTestCase.toList, specificationId)),
            getResourcePaths(testSuiteFile.getName, data.getTestCases.getTestCase.toList),
            existingTestCaseMap,
            ctx.savedSpecificationActors(specificationId), // savedActorIds
            existingActorToSystemMap
          )
        } else {
          DBIO.successful((new java.util.ArrayList[Long](), List[TestSuiteUploadItemResult]()))
        }
      }
      // Remove the test cases that are no longer in the test suite.
      _ <- testSuiteManager.stepRemoveTestCases(existingTestCaseMap)
      // Update the actor links for the  test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteActorLinks(testSuiteId, getSavedActorMap(data, specificationId, ctx))
      // Update the test case links for the test suite.
      _ <- testSuiteManager.stepUpdateTestSuiteTestCaseLinks(testSuiteId, processTestCasesStep._1)
    } yield existingTestSuiteFile
    action.flatMap(existingTestSuiteFile => {
      ctx.onSuccessCalls += (() => {
        // Finally, delete the backup folder
        val existingTestSuiteFolder = new File(RepositoryUtils.getTestSuitesPath(domainId, specificationId), existingTestSuiteFile)
        if (existingTestSuiteFolder != null && existingTestSuiteFolder.exists()) {
          FileUtils.deleteDirectory(existingTestSuiteFolder)
        }
      })
      ctx.onFailureCalls += (() => {
        // Cleanup operations in case an error occurred.
        if (testSuiteFile.exists()) {
          FileUtils.deleteDirectory(testSuiteFile)
        }
      })
      DBIO.successful(())
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
            (data: com.gitb.xml.export.Domain, targetKey: Any, item: ImportItem) => {
              // Record this in case we need to do a global cleanup.
              createdDomainId = Some(targetKey.asInstanceOf[Long])
              // In case of a failure delete the created domain test suite folder (if one was created later on).
              ctx.onFailureCalls += (() => {
                val domainFolder = RepositoryUtils.getDomainTestSuitesPath(createdDomainId.get)
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
          (targetKey: String) => {
            conformanceManager.deleteDomainInternal(targetKey.toLong, ctx.onSuccessCalls)
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
                  val domainId = item.parentItem.get.targetKey.get.toLong
                  conformanceManager.createDomainParameterInternal(models.DomainParameter(0L, data.getName, Option(data.getDescription), propertyTypeToKind(data.getType, isDomainParameter = true), decryptIfNeeded(ctx.importSettings, data.getType, Option(data.getValue)), domainId))
                },
                (data: com.gitb.xml.export.DomainParameter, targetKey: String, item: ImportItem) => {
                  conformanceManager.updateDomainParameterInternal(targetKey.toLong, data.getName, Option(data.getDescription), propertyTypeToKind(data.getType, isDomainParameter = true), Option(data.getValue))
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
                },
                (data: com.gitb.xml.export.Specification, targetKey: Any, item: ImportItem) => {
                  // In case of a failure delete the created domain test suite folder (if one was created later on).
                  ctx.onFailureCalls += (() => {
                    val domainId = item.parentItem.get.targetKey.get.toLong
                    val specificationFolder = RepositoryUtils.getTestSuitesPath(domainId, targetKey.asInstanceOf[Long])
                    if (specificationFolder.exists()) {
                      FileUtils.deleteQuietly(specificationFolder)
                    }
                  })
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
            conformanceManager.delete(targetKey.toLong, ctx.onSuccessCalls)
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
                    (data: com.gitb.xml.export.Actor, targetKey: Any, item: ImportItem) => {
                      // Record actor info (needed for test suite processing).
                      val specificationId = item.parentItem.get.targetKey.get.toLong
                      if (!ctx.savedSpecificationActors.containsKey(specificationId)) {
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
            conformanceManager.undeployTestSuite(targetKey.toLong, ctx.onSuccessCalls)
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

  private def hasExisting(itemType: ImportItemType, key: String, ctx: ImportContext): Boolean = {
    ctx.existingIds.map.containsKey(itemType) && ctx.existingIds.map(itemType).contains(key)
  }

  private def completeFileSystemFinalisation(ctx: ImportContext, dbAction: DBIO[_]): DBIO[_] = {
    dbActionFinalisation(Some(ctx.onSuccessCalls), Some(ctx.onFailureCalls), dbAction)
  }

  private def prepareCertificateSettingKey(archiveValue: String, importSettings: ImportSettings): Option[String] = {
    if (archiveValue != null) {
      // Decrypt using archive password and then encrypt for local storage
      Some(MimeUtil.encryptString(decrypt(importSettings, archiveValue)))
    } else {
      None
    }
  }

  def completeCommunityImport(exportedCommunity: com.gitb.xml.export.Community, importSettings: ImportSettings, importItems: List[ImportItem], targetCommunityId: Option[Long], canAddOrDeleteDomain: Boolean, ownUserId: Option[Long]): Unit = {
    val ctx = ImportContext(
      importSettings,
      toImportItemMaps(importItems, ImportItemType.Community),
      ExistingIds.init(),
      ImportTargets.fromImportItems(importItems),
      mutable.Map[ImportItemType, mutable.Map[String, String]](),
      mutable.Map[Long, mutable.Map[String, Long]](),
      mutable.ListBuffer[() => _](),
      mutable.ListBuffer[() => _]()
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
      if (!Configurations.AUTHENTICATION_SSO_ENABLED && ctx.importTargets.hasAdministrators) {
        exportManager.loadAdministrators(targetCommunityId.get).foreach { x =>
          ctx.existingIds.map(ImportItemType.Administrator) += x.id.toString
        }
      }
      // Organisations
      if (ctx.importTargets.hasOrganisations) {
        exec(PersistenceSchema.organizations.filter(_.community === targetCommunityId.get).map(x => x.id).result).foreach { x =>
          ctx.existingIds.map(ImportItemType.Organisation) += x.toString
        }
        // Organisation users
        if (!Configurations.AUTHENTICATION_SSO_ENABLED && ctx.importTargets.hasOrganisationUsers) {
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
                ctx.existingIds.map(ImportItemType.StatementConfiguration) += x._1 // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
              }
            }
          }
        }
      }
    }
    // Load also set of unique email emails to ensure these are unique.
    var referenceUserEmails = mutable.Set[String]()
    if (!Configurations.AUTHENTICATION_SSO_ENABLED && (ctx.importTargets.hasAdministrators || ctx.importTargets.hasOrganisationUsers)) {
      referenceUserEmails = importPreviewManager.loadUserEmailSet()
    }
    // If we have users load their emails to ensure we don't end up with duplicates.
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
              // Set on import item.
              item.targetKey = Some(ids._1.toString)
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
            conformanceManager.deleteConformanceCertificateSettings(communityId.get)
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
              keystorePassword = prepareCertificateSettingKey(exportedCommunity.getConformanceCertificateSettings.getSignature.getKeystorePassword, importSettings)
              keyPassword = prepareCertificateSettingKey(exportedCommunity.getConformanceCertificateSettings.getSignature.getKeyPassword, importSettings)
            }
            conformanceManager.updateConformanceCertificateSettingsInternal(
                models.ConformanceCertificates(
                  0L, Option(exportedCommunity.getConformanceCertificateSettings.getTitle), Option(exportedCommunity.getConformanceCertificateSettings.getMessage),
                  exportedCommunity.getConformanceCertificateSettings.isAddMessage, exportedCommunity.getConformanceCertificateSettings.isAddResultOverview,
                  exportedCommunity.getConformanceCertificateSettings.isAddTestCases, exportedCommunity.getConformanceCertificateSettings.isAddDetails,
                  exportedCommunity.getConformanceCertificateSettings.isAddSignature, keystoreFile, keystoreType, keystorePassword, keyPassword,
                  communityId.get
                )
              , updatePasswords =true, removeKeystore =false
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
                  val key = communityId+"_"+labelObject.labelType
                  if (!hasExisting(ImportItemType.CustomLabel, key, ctx)) {
                    communityManager.createCommunityLabel(labelObject) andThen
                      DBIO.successful(key)
                  } else {
                    DBIO.successful(())
                  }
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
        if (!Configurations.AUTHENTICATION_SSO_ENABLED && exportedCommunity.getAdministrators != null) {
          exportedCommunity.getAdministrators.getAdministrator.foreach { exportedUser =>
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
                    to be existing. Not updating the email avoids the need to check that the email is unique with respect
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
        } else {
          DBIO.successful(())
        }
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
        if (!Configurations.AUTHENTICATION_SSO_ENABLED && exportedCommunity.getOrganisations != null) {
          exportedCommunity.getOrganisations.getOrganisation.foreach { exportedOrganisation =>
            if (exportedOrganisation.getUsers != null) {
              exportedOrganisation.getUsers.getUser.foreach { exportedUser =>
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
                        to be existing. Not updating the email avoids the need to check that the email is unique with respect
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
            (targetKey: String) => {
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
                        val key = organisationId+"_"+relatedPropertyId.get
                        if (!hasExisting(ImportItemType.OrganisationPropertyValue, key, ctx)) {
                          (PersistenceSchema.organisationParameterValues += models.OrganisationParameterValues(organisationId, relatedPropertyId.get, decryptIfNeeded(ctx.importSettings, data.getProperty.getType, Option(data.getValue)).get)) andThen
                            DBIO.successful(key)
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
                            val key = systemId+"_"+relatedPropertyId.get
                            if (!hasExisting(ImportItemType.SystemPropertyValue, key, ctx)) {
                              // The property this value related to has either been updated or inserted.
                              (PersistenceSchema.systemParameterValues += models.SystemParameterValues(systemId, relatedPropertyId.get, decryptIfNeeded(ctx.importSettings, data.getProperty.getType, Option(data.getValue)).get)) andThen
                                DBIO.successful(key)
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
                            val key = systemId+"_"+relatedActorId.get
                            if (!hasExisting(ImportItemType.Statement, key, ctx)) {
                              systemManager.defineConformanceStatement(systemId, relatedSpecId.get, relatedActorId.get, None) andThen
                                DBIO.successful(key)
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
                              val statementTargetKeyParts = StringUtils.split(item.parentItem.get.targetKey.get, "_") // [System ID]_[Actor ID]
                              val relatedActorId = statementTargetKeyParts(1).toLong
                              if (relatedParameterId.isDefined && relatedEndpointId.isDefined) {
                                val relatedSystemId = item.parentItem.get.parentItem.get.targetKey.get.toLong // Statement -> System
                                val key = relatedActorId+"_"+relatedEndpointId.get+"_"+relatedSystemId+"_"+relatedParameterId.get
                                if (!hasExisting(ImportItemType.StatementConfiguration, key, ctx)) {
                                  systemManager.saveEndpointConfigurationInternal(forceAdd = true, forceUpdate = false,
                                    models.Configs(relatedSystemId, relatedParameterId.get, relatedEndpointId.get, decryptIfNeeded(ctx.importSettings, data.getParameter.getType, Option(data.getValue)).get)) andThen
                                    DBIO.successful(relatedEndpointId.get+"_"+relatedSystemId+"_"+relatedParameterId.get)
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
                                val relatedSystemId = item.parentItem.get.parentItem.get.targetKey.get.toLong // Statement -> System
                                systemManager.saveEndpointConfigurationInternal(forceAdd = false, forceUpdate = true,
                                  models.Configs(relatedSystemId, relatedParameterId.get, relatedEndpointId.get, decryptIfNeeded(ctx.importSettings, data.getParameter.getType, Option(data.getValue)).get)) andThen
                                DBIO.successful(relatedActorId+"_"+relatedEndpointId.get+"_"+relatedSystemId+"_"+relatedParameterId.get)
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
            // Key: [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
            val keyParts = StringUtils.split(targetKey, "_")
            val endpointId = keyParts(1).toLong
            val systemId = keyParts(2).toLong
            val parameterId = keyParts(3).toLong
            systemManager.deleteEndpointConfigurationInternal(systemId, parameterId, endpointId)
          }
        )
      }
    } yield ()
    exec(completeFileSystemFinalisation(ctx, dbAction).transactionally)
  }

  def importSandboxData(archive: File, archiveKey: String): (Boolean, Option[String]) = {
    var processingComplete = false
    var errorMessage: Option[String] = None
    logger.info("Processing data archive ["+archive.getName+"]")
    val importSettings = new ImportSettings()
    importSettings.encryptionKey = Some(archiveKey)
    var archiveData = FileUtils.readFileToByteArray(archive)
    val preparationResult = importPreviewManager.prepareImportPreview(archiveData, importSettings, requireDomain = false, requireCommunity = false)
    archiveData = null // GC
    try {
      if (preparationResult._1.isDefined) {
        errorMessage = Some(preparationResult._1.get._2)
        logger.warn("Unable to process data archive ["+archive.getName+"]: " + preparationResult._1.get._2)
      } else {
        if (preparationResult._2.get.getCommunities != null && !preparationResult._2.get.getCommunities.getCommunity.isEmpty) {
          // Community import.
          val exportedCommunity = preparationResult._2.get.getCommunities.getCommunity.get(0)
          // Step 1 - prepare import.
          var importItems: List[ImportItem] = null
          val previewResult = importPreviewManager.previewCommunityImport(exportedCommunity, None)
          if (previewResult._2.isDefined) {
            importItems = List(previewResult._2.get, previewResult._1)
          } else {
            importItems = List(previewResult._1)
          }
          // Set all import items to proceed.
          approveImportItems(importItems)
          // Step 2 - Import.
          importSettings.dataFilePath = Some(importPreviewManager.getPendingImportFile(preparationResult._4.get, preparationResult._3.get).get.toPath)
          completeCommunityImport(exportedCommunity, importSettings, importItems, None, canAddOrDeleteDomain = true, None)
          // Avoid processing this archive again.
          processingComplete = true
        } else if (preparationResult._2.get.getDomains != null && !preparationResult._2.get.getDomains.getDomain.isEmpty) {
          // Domain import.
          val exportedDomain = preparationResult._2.get.getDomains.getDomain.get(0)
          // Step 1 - prepare import.
          val importItems = List(importPreviewManager.previewDomainImport(exportedDomain, None))
          // Set all import items to proceed.
          approveImportItems(importItems)
          // Step 2 - Import.
          importSettings.dataFilePath = Some(importPreviewManager.getPendingImportFile(preparationResult._4.get, preparationResult._3.get).get.toPath)
          completeDomainImport(exportedDomain, importSettings, importItems, None, canAddOrDeleteDomain = true)
          // Avoid processing this archive again.
          processingComplete = true
        } else {
          errorMessage = Some("Provided data archive is empty")
          logger.warn(errorMessage.get)
        }
      }
    } catch {
      case e:Exception => {
        logger.warn("Unexpected exception while processing data archive ["+archive.getName+"]", e)
      }
    } finally {
      if (preparationResult._4.isDefined) {
        FileUtils.deleteQuietly(preparationResult._4.get.toFile)
      }
    }
    logger.info("Finished processing data archive ["+archive.getName+"]")
    (processingComplete, errorMessage)
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

}
