package modules

import hooks.{BeforeStartHook, OnStopHook, PostStartHook}

class Module extends play.api.inject.Module {

  def bindings(environment: play.api.Environment, configuration: play.api.Configuration) = {
    Seq(
      bind[BeforeStartHook].toSelf.eagerly,
      /*
       Calling here the initialisation of FlyWayDB (and not via its own module). The reason for this is to ensure a DB
       is correctly created/migrated before we do other changes that may require DB interactions at start-up.
       */
      bind[MigrationInitializer].toSelf.eagerly,
      bind[PostStartHook].toSelf.eagerly,
      bind[OnStopHook].toSelf.eagerly
    )
  }

}
