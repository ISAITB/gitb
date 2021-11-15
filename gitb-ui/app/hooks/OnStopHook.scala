package hooks

import actors.{TestSessionUpdateActor, TriggerActor}
import akka.actor.ActorSystem

import javax.inject.{Inject, Singleton}
import jaxws.TestbedService
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

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
    Future.successful({
      actorSystem.actorSelection("/user/"+TriggerActor.actorName).resolveOne(1.second).onComplete { ref =>
        actorSystem.stop(ref.get)
      }
      actorSystem.actorSelection("/user/"+TestSessionUpdateActor.actorName).resolveOne(1.second).onComplete { ref =>
        actorSystem.stop(ref.get)
      }
    })
  })
  lifecycle.addStopHook(() => {
    Future.successful(logger.info("Application shutdown..."))
  })

}
