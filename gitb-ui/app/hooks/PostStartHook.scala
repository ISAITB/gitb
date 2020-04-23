package hooks

import java.nio.file.{Files, Paths}

import actors.WebSocketActor
import akka.actor.ActorSystem
import config.Configurations
import controllers.TestService
import javax.inject.{Inject, Singleton}
import javax.xml.ws.Endpoint
import jaxws.TestbedService
import managers.export.{ImportCompleteManager, ImportItem, ImportPreviewManager, ImportSettings}
import managers.{ReportManager, SystemConfigurationManager, TestResultManager, TestSuiteManager, TestbedBackendClient}
import models.Constants
import models.Enums.ImportItemChoice
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import utils.{RepositoryUtils, TimeUtil}

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PostStartHook @Inject() (appLifecycle: ApplicationLifecycle, actorSystem: ActorSystem, systemConfigurationManager: SystemConfigurationManager, testResultManager: TestResultManager, testService: TestService, testSuiteManager: TestSuiteManager, reportManager: ReportManager, webSocketActor: WebSocketActor, testbedBackendClient: TestbedBackendClient, importPreviewManager: ImportPreviewManager, importCompleteManager: ImportCompleteManager) {

  onStart()

  def onStart(): Unit = {
    Logger.info("Starting Application")
    System.setProperty("java.io.tmpdir", System.getProperty("user.dir"))
    //start TestbedClient service
    TestbedService.endpoint = Endpoint.publish(Configurations.TESTBED_CLIENT_URL, new TestbedService(reportManager, webSocketActor, testbedBackendClient))
    destroyIdleSessions()
    cleanupPendingTestSuiteUploads()
    cleanupTempReports()
    loadDataExports()
    Logger.info("Application has started")
  }

  /**
    * Scheduled job that kills idle sessions
    */

  private def destroyIdleSessions() = {
    actorSystem.scheduler.schedule(1.days, 1.days) {
      val config = systemConfigurationManager.getSystemConfiguration(Constants.SessionAliveTime)
      val aliveTime = config.parameter
      if (aliveTime.isDefined) {
        val list = testResultManager.getRunningTestResults
        list.foreach { result =>
          val difference = TimeUtil.getTimeDifferenceInSeconds(result.startTime)
          if (difference >= aliveTime.get.toInt) {
            val sessionId = result.sessionId
            testService.endSession(sessionId)
            Logger.info("Stopped idle session [" + sessionId + "]")
          }
        }
      }
    }
  }

  private def cleanupPendingTestSuiteUploads() = {
    actorSystem.scheduler.schedule(1.hours, 1.hours) {
      val pendingFolder = testSuiteManager.getPendingFolder
      if (pendingFolder.exists() && pendingFolder.isDirectory) {
        for (file <- pendingFolder.listFiles()) {
          if (file.lastModified() + 3600000 < System.currentTimeMillis) {
            // Delete pending test suite folders that were created min 1 hour ago
            FileUtils.deleteDirectory(file)
          }
        }
      }
    }
  }

  private def cleanupTempReports() = {
    actorSystem.scheduler.schedule(0.minutes, 5.minutes) {
      val tempFolder = ReportManager.getTempFolderPath().toFile
      if (tempFolder.exists() && tempFolder.isDirectory) {
        for (file <- tempFolder.listFiles()) {
          try {
            FileUtils.deleteDirectory(file)
          } catch {
            case e:Exception => {
              Logger.warn("Unable to delete temp folder [" + file.getAbsolutePath + "]")
            }
          }
        }
      }
    }
  }

  private def loadDataExports() = {
    val dataIn = RepositoryUtils.getDataInFolder()
    if (dataIn.exists() && dataIn.isDirectory && dataIn.canRead) {
      val containedFiles = dataIn.listFiles()
      if (containedFiles != null && containedFiles.nonEmpty) {
        val archiveKey = Configurations.DATA_ARCHIVE_KEY
        if (archiveKey.isBlank) {
          Logger.warn("No key was provided to open provided data archives. Skipping data import.")
        } else {
          containedFiles.foreach { file =>
            if (file.getName.toLowerCase.endsWith(".zip")) {
              var moveArchive = false
              Logger.info("Processing data archive ["+file.getName+"]")
              val importSettings = new ImportSettings()
              importSettings.encryptionKey = Some(archiveKey)
              var archiveData = FileUtils.readFileToByteArray(file)
              val preparationResult = importPreviewManager.prepareImportPreview(archiveData, importSettings, requireDomain = false, requireCommunity = false)
              archiveData = null // GC
              try {
                if (preparationResult._1.isDefined) {
                  Logger.warn("Unable to process data archive ["+file.getName+"]: " + preparationResult._1.get._2)
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
                    importCompleteManager.completeCommunityImport(exportedCommunity, importSettings, importItems, None, canAddOrDeleteDomain = true, None)
                  } else if (preparationResult._2.get.getDomains != null && !preparationResult._2.get.getDomains.getDomain.isEmpty) {
                    // Domain import.
                    val exportedDomain = preparationResult._2.get.getDomains.getDomain.get(0)
                    // Step 1 - prepare import.
                    val importItems = List(importPreviewManager.previewDomainImport(exportedDomain, None))
                    // Set all import items to proceed.
                    approveImportItems(importItems)
                    // Step 2 - Import.
                    importSettings.dataFilePath = Some(importPreviewManager.getPendingImportFile(preparationResult._4.get, preparationResult._3.get).get.toPath)
                    importCompleteManager.completeDomainImport(exportedDomain, importSettings, importItems, None, canAddOrDeleteDomain = true)
                  } else {
                    Logger.warn("Data archive ["+file.getName+"] was empty")
                  }
                  // Avoid processing this archive again.
                  moveArchive = true
                }
              } catch {
                case e:Exception => {
                  Logger.warn("Unexpected exception while processing data archive ["+file.getName+"]", e)
                }
              } finally {
                if (preparationResult._4.isDefined) {
                  FileUtils.deleteQuietly(preparationResult._4.get.toFile)
                }
              }
              Logger.info("Finished processing data archive ["+file.getName+"]")
              if (moveArchive) {
                // Ensure a unique name in the "processed" folder.
                val targetFile = RepositoryUtils.getDataProcessedFolder().toPath.resolve("export_"+RandomStringUtils.random(10, false, true)+".zip").toFile
                Files.createDirectories(targetFile.getParentFile.toPath)
                FileUtils.moveFile(file, targetFile)
              }
            }
          }
        }
      }
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

}
