package hooks

import org.apache.pekko.actor.ActorSystem
import jaxws.TestbedService
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class OnStopHook @Inject()(lifecycle: ApplicationLifecycle, actorSystem: ActorSystem) {

  private def logger = LoggerFactory.getLogger(this.getClass)

  // Stop backend service.
  lifecycle.addStopHook(() => {
    Future.successful(if (TestbedService.endpoint != null) {
      TestbedService.endpoint.stop()
    })
  })
  // Stop event listeners.
  lifecycle.addStopHook(() => actorSystem.terminate())
  lifecycle.addStopHook(() => {
    Future.successful(logger.info("Application shutdown..."))
  })

}
