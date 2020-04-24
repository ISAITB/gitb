package modules

import hooks.{BeforeStartHook, OnStopHook, PostStartHook}
import org.flywaydb.play.PlayInitializer

class Module extends play.api.inject.Module {

  def bindings(environment: play.api.Environment, configuration: play.api.Configuration) = {
    Seq(
      bind[BeforeStartHook].toSelf.eagerly,
      bind[PlayInitializer].toSelf.eagerly,
      bind[PostStartHook].toSelf.eagerly,
      bind[OnStopHook].toSelf.eagerly
    )
  }

}
