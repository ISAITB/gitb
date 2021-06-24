package modules

import org.flywaydb.play.Flyways
import play.api.{Configuration, Environment, Mode}

import javax.inject._

/**
 * Custom initialiser to ensure that migrations are always checked - even in Dev.
 *
 * @param configuration The app configuration.
 * @param environment The app environment.
 * @param flyways The internal Flyway component.
 */
@Singleton
class MigrationInitializer @Inject() (
   configuration: Configuration,
   environment: Environment,
   flyways: Flyways) {

  def onStart(): Unit = {

    flyways.allDatabaseNames.foreach { dbName =>
      environment.mode match {
        case Mode.Test =>
          flyways.migrate(dbName)
        case Mode.Prod if flyways.config(dbName).auto =>
          flyways.migrate(dbName)
        case Mode.Prod =>
          flyways.checkState(dbName)
        case Mode.Dev =>
          flyways.migrate(dbName)
      }
    }
  }

  val enabled: Boolean =
    !configuration.getOptional[String]("flywayplugin").contains("disabled")

  if (enabled) {
    onStart()
  }

}
