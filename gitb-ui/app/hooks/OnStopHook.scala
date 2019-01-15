package hooks

import javax.inject.{Inject, Singleton}
import jaxws.TestbedService
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class OnStopHook @Inject()(lifecycle: ApplicationLifecycle) {

  lifecycle.addStopHook(() => {
    TestbedService.endpoint.stop()
    Logger.info("Application shutdown...")
    Future.successful(())
  })

}
