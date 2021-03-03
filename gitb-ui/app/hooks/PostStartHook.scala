package hooks

import java.nio.file.{Files, Path}
import actors.{TriggerActor, WebSocketActor}
import akka.actor.{ActorSystem, Props}
import config.Configurations
import controllers.TestService

import javax.inject.{Inject, Singleton}
import javax.xml.ws.Endpoint
import jaxws.TestbedService
import managers._
import managers.export.ImportCompleteManager
import models.Constants
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle
import utils.{RepositoryUtils, TimeUtil, ZipArchiver}

import java.io.{File, FileFilter}
import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class PostStartHook @Inject() (implicit ec: ExecutionContext, appLifecycle: ApplicationLifecycle, actorSystem: ActorSystem, systemConfigurationManager: SystemConfigurationManager, testResultManager: TestResultManager, testService: TestService, testSuiteManager: TestSuiteManager, reportManager: ReportManager, webSocketActor: WebSocketActor, testbedBackendClient: TestbedBackendClient, importCompleteManager: ImportCompleteManager, triggerManager: TriggerManager, repositoryUtils: RepositoryUtils) {

  private def logger = LoggerFactory.getLogger(this.getClass)

  onStart()

  def onStart(): Unit = {
    logger.info("Starting Application")
    System.setProperty("java.io.tmpdir", System.getProperty("user.dir"))
    //start TestbedClient service
    TestbedService.endpoint = Endpoint.publish(Configurations.TESTBED_CLIENT_URL, new TestbedService(testResultManager, reportManager, webSocketActor, testbedBackendClient))
    destroyIdleSessions()
    cleanupPendingTestSuiteUploads()
    cleanupTempFiles()
    initialiseActors(triggerManager)
    loadDataExports()
    archiveOldTestSessions()
    logger.info("Application has started in "+Configurations.TESTBED_MODE+" mode")
  }

  private def initialiseActors(triggerManager: TriggerManager): Unit = {
    actorSystem.actorOf(Props(new TriggerActor(triggerManager)), TriggerActor.actorName)
  }

  /**
    * Scheduled job that kills idle sessions
    */
  private def destroyIdleSessions() = {
    actorSystem.scheduler.scheduleWithFixedDelay(5.minutes, 30.minutes) {
      () => {
        val config = systemConfigurationManager.getSystemConfiguration(Constants.SessionAliveTime)
        val aliveTime = config.parameter
        if (aliveTime.isDefined) {
          val list = testResultManager.getRunningTestResults
          list.foreach { result =>
            val difference = TimeUtil.getTimeDifferenceInSeconds(result.startTime)
            if (difference >= aliveTime.get.toInt) {
              val sessionId = result.sessionId
              testService.endSession(sessionId)
              logger.info("Terminated idle session [" + sessionId + "]")
            }
          }
        }
      }
    }
  }

  private def cleanupPendingTestSuiteUploads() = {
    actorSystem.scheduler.scheduleWithFixedDelay(1.hours, 1.hours) {
      () => {
        deleteSubfolders(repositoryUtils.getPendingFolder(), 3600000) // 1 hour
      }
    }
  }

  private def cleanupTempFiles() = {
    actorSystem.scheduler.scheduleAtFixedRate(0.minutes, 5.minutes) {
      () => {
        deleteSubfolders(repositoryUtils.getTempReportFolder(), 300000) // 5 minutes
        deleteSubfolders(repositoryUtils.getTempArchivedSessionWorkspaceFolder(), 300000) // 5 minutes
      }
    }
  }

  private def deleteSubfolders(rootFolder: File, gracePeriodMillis: Long) = {
    if (rootFolder.exists() && rootFolder.isDirectory) {
      for (file <- rootFolder.listFiles()) {
        if (file.lastModified() + gracePeriodMillis < System.currentTimeMillis) {
          try {
            FileUtils.deleteDirectory(file)
          } catch {
            case e:Exception =>
              logger.warn("Unable to delete temp folder [" + file.getAbsolutePath + "]", e)
          }
        }
      }
    }
  }

  private def loadDataExports(): Unit = {
    val dataIn = repositoryUtils.getDataInFolder()
    if (dataIn.exists() && dataIn.isDirectory && dataIn.canRead) {
      val containedFiles = dataIn.listFiles()
      if (containedFiles != null && containedFiles.nonEmpty) {
        val archiveKey = Configurations.DATA_ARCHIVE_KEY
        if (archiveKey.isBlank) {
          logger.warn("No key was provided to open provided data archives. Skipping data import.")
        } else {
          containedFiles.foreach { file =>
            if (file.getName.toLowerCase.endsWith(".zip")) {
              val moveArchive = importCompleteManager.importSandboxData(file, archiveKey)._1
              if (moveArchive) {
                // Ensure a unique name in the "processed" folder.
                val targetFile = repositoryUtils.getDataProcessedFolder().toPath.resolve("export_"+RandomStringUtils.random(10, false, true)+".zip").toFile
                Files.createDirectories(targetFile.getParentFile.toPath)
                FileUtils.moveFile(file, targetFile)
              }
            }
          }
        }
      }
    }
  }

  private def archiveOldTestSessions() = {
    actorSystem.scheduler.scheduleAtFixedRate(0.minutes, 20.hours) {
      () => {
        val now = LocalDate.now()
        val archivalThreshold = now.minusDays(Configurations.TEST_SESSION_ARCHIVE_THRESHOLD)
        val statusUpdatesFolder = repositoryUtils.getStatusUpdatesFolder()
        if (statusUpdatesFolder.exists() && statusUpdatesFolder.isDirectory) {
          val yearFolders = statusUpdatesFolder.listFiles(new FileFilter {
            override def accept(pathname: File): Boolean = {
              pathname.isDirectory && isNumeric(pathname.getName)
            }
          })
          if (yearFolders != null) {
            yearFolders.foreach { yearFolder =>
              val monthFoldersToArchive = yearFolder.listFiles(new FileFilter {
                override def accept(pathname: File): Boolean = {
                  if (pathname.isDirectory) {
                    try {
                      val year = Integer.parseInt(yearFolder.getName)
                      val month = Integer.parseInt(pathname.getName)
                      val folderDate = LocalDate.of(year, month, 1)
                      folderDate.isBefore(archivalThreshold) && (now.getYear != year || month < now.getMonthValue)
                    } catch {
                      case _: NumberFormatException =>
                        // In case we have unexpected folders that don't match what we expect
                        false
                    }
                  } else {
                    false
                  }
                }
              })
              if (monthFoldersToArchive != null) {
                monthFoldersToArchive.foreach { monthFolder =>
                  // Create the zip archive.
                  val zipArchive = Path.of(yearFolder.getAbsolutePath, monthFolder.getName+".zip")
                  Files.deleteIfExists(zipArchive)
                  new ZipArchiver(monthFolder.toPath, zipArchive).zip()
                  // All ok - delete the folder.
                  FileUtils.deleteDirectory(monthFolder)
                  logger.info("Archived test session folder for year ["+yearFolder.getName+"] and month ["+monthFolder.getName+"]")
                }
              }
            }
          }
        }
      }
    }
  }

  private def isNumeric(name: String): Boolean = {
    try {
      Integer.parseInt(name)
      true
    } catch {
      case _: NumberFormatException =>
        false
    }
  }

}
