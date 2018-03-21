package managers

import models.SystemConfiguration
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

object SystemConfigurationManager extends BaseManager {
  def logger = LoggerFactory.getLogger("SystemConfigurationManager")

  /**
   * Gets system config by name
   */
  def getSystemConfiguration(name: String): SystemConfiguration = {
    DB.withSession { implicit session =>
      val sc = PersistenceSchema.systemConfigurations.filter(_.name === name).firstOption.get
      val config = new SystemConfiguration(sc)
      config
    }
  }

  /**
   * Set system parameter
   */
  def updateSystemParameter(name: String, value: Option[String] = None) = {
    DB.withTransaction { implicit session =>
      val q = for {c <- PersistenceSchema.systemConfigurations if c.name === name} yield (c.parameter)
      q.update(value)
    }
  }

}
