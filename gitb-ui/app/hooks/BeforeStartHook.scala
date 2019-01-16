package hooks

import config.Configurations
import javax.inject.Singleton
import persistence.db.PersistenceLayer
import play.api.Logger

@Singleton
class BeforeStartHook {

  //Load application configurations before the applications starts
  Configurations.loadConfigurations()
  //Create database if not exists.
  PersistenceLayer.preInitialize()
  Logger.info("Application has been configured")

}
