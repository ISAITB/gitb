/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
    // Apply any Flyway migrations not covered by the current baseline.
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
