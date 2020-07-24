package hooks

import javax.inject.{Inject, Singleton}
import jaxws.TestbedService
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class OnStopHook @Inject()(lifecycle: ApplicationLifecycle) {

  private def logger = LoggerFactory.getLogger(this.getClass)

  lifecycle.addStopHook(() => {
    if (TestbedService.endpoint != null) {
      TestbedService.endpoint.stop()
    }
    logger.info("Application shutdown...")
    Future.successful(())
  })

}
