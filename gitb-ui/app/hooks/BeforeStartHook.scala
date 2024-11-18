package hooks

import config.Configurations
import org.slf4j.LoggerFactory

import javax.inject.Singleton

@Singleton
class BeforeStartHook {

  private def logger = LoggerFactory.getLogger(this.getClass)

  // Load application configurations before the applications starts
  Configurations.loadConfigurations()
  logger.info("Application has been configured")

}
