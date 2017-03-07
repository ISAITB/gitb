import config.Configurations
import jaxws.TestbedService
import javax.xml.ws.Endpoint
import filters._
import managers.{TestResultManager, SystemConfigurationManager}
import models.Constants
import controllers.TestService

import persistence.db.PersistenceLayer
import utils.TimeUtil
import scala.concurrent.duration.DurationInt

import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

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

    // start idle session task
    destroyIdleSessions
    Logger.info("Application has started")
  }

  /**
   * Called on application stop
   */
  override def onStop(app: Application) {
    TestbedService.endpoint.stop()

    Logger.info("Application shutdown...")
  }

  /**
   * Scheduled job that kills idle sessions
   */
  def destroyIdleSessions = {
    Akka.system.scheduler.schedule(1.days, 1.days) {
      SystemConfigurationManager.getSystemConfiguration(Constants.SessionAliveTime) map { config =>
        val aliveTime = config.parameter
        if (aliveTime.isDefined) {
          TestResultManager.getRunningTestResults map { list =>
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
    }
  }

}
