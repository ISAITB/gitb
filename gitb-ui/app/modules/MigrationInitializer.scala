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
