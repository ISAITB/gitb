package hooks

import actors.TriggerActor
import akka.actor.{ActorSystem, Props}
import javax.inject.{Inject, Singleton}
import jaxws.TestbedService
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle

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
  lifecycle.addStopHook(() => {
    val triggerListener = actorSystem.actorOf(Props(classOf[TriggerActor]))
    Future.successful(actorSystem.stop(triggerListener))
  })
  logger.info("Application shutdown...")

}
