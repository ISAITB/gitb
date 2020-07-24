package hooks

import config.Configurations
import javax.inject.Singleton
import org.slf4j.LoggerFactory
import persistence.db.PersistenceLayer

@Singleton
class BeforeStartHook {

  private def logger = LoggerFactory.getLogger(this.getClass)

  //Load application configurations before the applications starts
  Configurations.loadConfigurations()
  //Create database if not exists.
  PersistenceLayer.preInitialize()
  logger.info("Application has been configured")

}
