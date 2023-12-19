package modules

import actors.{SessionLaunchActor, SessionManagerActor, SessionUpdateActor, TriggerActor}
import com.google.inject.AbstractModule
import hooks.{BeforeStartHook, OnStopHook, PostStartHook}
import play.api.libs.concurrent.PekkoGuiceSupport

class Module extends AbstractModule with PekkoGuiceSupport {

  override def configure() = {
    bind(classOf[BeforeStartHook]).asEagerSingleton()
    /*
     Calling here the initialisation of FlyWayDB (and not via its own module). The reason for this is to ensure a DB
     is correctly created/migrated before we do other changes that may require DB interactions at start-up.
     */
    bind(classOf[MigrationInitializer]).asEagerSingleton()
    // Bind top level actors - START
    bindActor[TriggerActor](TriggerActor.actorName)
    bindActor[SessionManagerActor](SessionManagerActor.actorName)
    bindActorFactory[SessionUpdateActor, SessionUpdateActor.Factory]
    bindActorFactory[SessionLaunchActor, SessionLaunchActor.Factory]
    // Bind top level actors - END
    bind(classOf[PostStartHook]).asEagerSingleton()
    bind(classOf[OnStopHook]).asEagerSingleton()
  }

}
