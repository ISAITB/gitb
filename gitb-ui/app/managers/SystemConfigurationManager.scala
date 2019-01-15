package managers

import models.SystemConfiguration
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

object SystemConfigurationManager extends BaseManager {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("SystemConfigurationManager")

  /**
   * Gets system config by name
   */
  def getSystemConfiguration(name: String): SystemConfiguration = {
    val sc = exec(PersistenceSchema.systemConfigurations.filter(_.name === name).result.head)
    val config = new SystemConfiguration(sc)
    config
  }

  /**
   * Set system parameter
   */
  def updateSystemParameter(name: String, value: Option[String] = None) = {
    val q = for {c <- PersistenceSchema.systemConfigurations if c.name === name} yield (c.parameter)
    exec(q.update(value).transactionally)
  }

}
