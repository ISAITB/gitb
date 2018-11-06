import javax.xml.ws.Endpoint

import config.Configurations
import controllers.TestService
import controllers.util.ResponseConstructor
import filters._
import jaxws.TestbedService
import managers.{ReportManager, SystemConfigurationManager, TestResultManager, TestSuiteManager}
import models.Constants
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.RandomStringUtils
import persistence.db.PersistenceLayer
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import utils.TimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Global extends WithFilters(new CorsFilter,
  new ErrorFilter,
  new AuthenticationFilter,
  new TimeoutFilter)
with GlobalSettings {

  /**
   * Called before the application starts.
   * Resources managed by plugins, such as database connections, are likely not available at this point.
   */
  override def beforeStart(app: Application) {
    //Load application configurations before the applications starts
    Configurations.loadConfigurations()

    //Create database if not exists.
    PersistenceLayer.preInitialize()

    Logger.info("Application has been configured")
  }

  /**
   * Called once the application is started.
   */
  override def onStart(app: Application) {
    System.setProperty("java.io.tmpdir", System.getProperty("user.dir"))
    //Initialize persistence service
    PersistenceLayer.initialize()

    //start TestbedClient service
    TestbedService.endpoint = Endpoint.publish(Configurations.TESTBED_CLIENT_URL, new TestbedService());

    // start tasks
    destroyIdleSessions()
    cleanupPendingTestSuiteUploads()
    cleanupTempReports()
    Logger.info("Application has started")
  }

  /**
   * Called on application stop
   */
  override def onStop(app: Application) {
    TestbedService.endpoint.stop()

    Logger.info("Application shutdown...")
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val errorIdentifier = RandomStringUtils.randomAlphabetic(10)
    Logger.error("Error ["+errorIdentifier+"]", ex)
    val result = ResponseConstructor.constructServerError("Unexpected error", ex.getMessage, Some(errorIdentifier))
    Future.successful(result)
  }

  /**
   * Scheduled job that kills idle sessions
   */
  def destroyIdleSessions() = {
    Akka.system.scheduler.schedule(1.days, 1.days) {
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
    Akka.system.scheduler.schedule(1.hours, 1.hours) {
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
    Akka.system.scheduler.schedule(0.minutes, 5.minutes) {
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

}
