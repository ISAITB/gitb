
import config.Configurations
import jaxws.TestbedService
import javax.xml.ws.Endpoint
import persistence.db.PersistenceLayer
import filters._
import play.api._
import play.api.mvc._
import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits.defaultContext

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

    Logger.info("Application has started")
  }

  /**
   * Called on application stop
   */
  override def onStop(app: Application) {
    TestbedService.endpoint.stop()

    Logger.info("Application shutdown...")
  }
}
