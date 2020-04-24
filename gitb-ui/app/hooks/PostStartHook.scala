package hooks

import java.nio.file.Files

import actors.WebSocketActor
import akka.actor.ActorSystem
import config.Configurations
import controllers.TestService
import javax.inject.{Inject, Singleton}
import javax.xml.ws.Endpoint
import jaxws.TestbedService
import managers.export.ImportCompleteManager
import managers.{ReportManager, SystemConfigurationManager, TestResultManager, TestSuiteManager, TestbedBackendClient}
import models.Constants
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.{Application, Logger}
import play.api.inject.ApplicationLifecycle
import utils.{RepositoryUtils, TimeUtil}

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PostStartHook @Inject() (app: Application, appLifecycle: ApplicationLifecycle, actorSystem: ActorSystem, systemConfigurationManager: SystemConfigurationManager, testResultManager: TestResultManager, testService: TestService, testSuiteManager: TestSuiteManager, reportManager: ReportManager, webSocketActor: WebSocketActor, testbedBackendClient: TestbedBackendClient, importCompleteManager: ImportCompleteManager) {

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
              val moveArchive = importCompleteManager.importSandboxData(file, archiveKey)._1
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

}
