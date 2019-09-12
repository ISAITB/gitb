import play.api.ApplicationLoader
import play.api.inject.guice.{GuiceApplicationLoader, GuiceApplicationBuilder}

/**
  * This application loader is added to allow WebJars to be injected into the ErrorHandler.
  * This implementation allows circular dependencies (which only occur in the error handler).
  */
class CustomApplicationLoader extends GuiceApplicationLoader {
  override protected def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
    super.builder(context).disableCircularProxies(false)
  }
}