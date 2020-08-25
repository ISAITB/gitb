package managers

import actors.events.TriggerEvent
import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}

@Singleton
class TriggerHelper @Inject() (actorSystem: ActorSystem) {

  def publishTriggerEvent[T <: TriggerEvent](event:T):Unit = {
    actorSystem.eventStream.publish(event)
  }

}
