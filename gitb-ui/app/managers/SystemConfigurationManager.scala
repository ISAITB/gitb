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

}
