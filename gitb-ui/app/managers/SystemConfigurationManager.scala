package managers

import models.SystemConfiguration
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import scala.slick.driver.MySQLDriver.simple._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

object SystemConfigurationManager extends BaseManager {
  def logger = LoggerFactory.getLogger("SystemConfigurationManager")

  /**
   * Gets system config by name
   */
  def getSystemConfiguration(name: String): Future[SystemConfiguration] = {
    Future {
      DB.withSession { implicit session =>
        val sc = PersistenceSchema.systemConfigurations.filter(_.name === name).firstOption.get
        val config = new SystemConfiguration(sc)
        config
      }
    }
  }

  /**
   * Set system parameter
   */
  def updateSystemParameter(name: String, value: Option[String] = None): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        val q = for {c <- PersistenceSchema.systemConfigurations if c.name === name} yield (c.parameter)
        q.update(value)
      }
    }
  }

}
