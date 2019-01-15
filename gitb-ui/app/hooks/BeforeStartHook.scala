package hooks

import java.nio.file.Paths

import akka.actor.ActorSystem
import config.Configurations
import controllers.TestService
import javax.inject.{Inject, Singleton}
import javax.xml.ws.Endpoint
import jaxws.TestbedService
import managers.{SystemConfigurationManager, TestResultManager, TestSuiteManager}
import models.Constants
import org.apache.commons.io.FileUtils
import persistence.db.PersistenceLayer
import play.api.Logger
import utils.TimeUtil

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BeforeStartHook @Inject() (actorSystem: ActorSystem) {

  //Load application configurations before the applications starts
  Configurations.loadConfigurations()
  //Create database if not exists.
  PersistenceLayer.preInitialize()
  Logger.info("Application has been configured")
  onStart()

  def onStart(): Unit = {
    System.setProperty("java.io.tmpdir", System.getProperty("user.dir"))
    //start TestbedClient service
    TestbedService.endpoint = Endpoint.publish(Configurations.TESTBED_CLIENT_URL, new TestbedService());
    destroyIdleSessions()
    cleanupPendingTestSuiteUploads()
    cleanupTempReports()
    Logger.info("Application has started")
  }

  /**
    * Scheduled job that kills idle sessions
    */

  def destroyIdleSessions() = {
    actorSystem.scheduler.schedule(1.days, 1.days) {
      val config = SystemConfigurationManager.getSystemConfiguration(Constants.SessionAliveTime)
      val aliveTime = config.parameter
      if (aliveTime.isDefined) {
        val list = TestResultManager.getRunningTestResults
        list.foreach { result =>
          val difference = TimeUtil.getTimeDifferenceInSeconds(result.startTime)
          if (difference >= aliveTime.get.toInt) {
            val sessionId = result.sessionId
            TestService.endSession(sessionId)
            Logger.info("Stopped idle session [" + sessionId + "]")
          }
        }
      }
    }
  }

  def cleanupPendingTestSuiteUploads() = {
    actorSystem.scheduler.schedule(1.hours, 1.hours) {
      val pendingFolder = TestSuiteManager.getPendingFolder
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

  def cleanupTempReports() = {
    actorSystem.scheduler.schedule(0.minutes, 5.minutes) {
      val tempFolder = Paths.get("/tmp/reports").toFile
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

}
