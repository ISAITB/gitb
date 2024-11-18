package modules

import config.Configurations
import org.flywaydb.play.Flyways
import persistence.db.PersistenceLayer
import play.api.{Configuration, Environment, Mode}

import javax.inject._

/**
 * Custom initializer to ensure that migrations are always checked - even in Dev.
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
    try {
      // Create database if not exists.
      PersistenceLayer.preInitialize()
      val flywayEnabled = !configuration.getOptional[String]("flywayplugin").contains("disabled")
      if (flywayEnabled) {
        // Perform migrations.
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
    } catch {
      case e: Exception =>
        Configurations.STARTUP_FAILURE = true
        throw e
    }
  }

  onStart()

}
