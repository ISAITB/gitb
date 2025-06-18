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

package db.migration.default

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.slf4j.LoggerFactory

import scala.util.Using

class V110__Use_environment_variables_if_no_active_theme_set extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V110__Use_environment_variables_if_no_active_theme_set])

  override def migrate(context: Context): Unit = {
    var proceed = false
    Using.resource(context.getConnection.createStatement()) { stmt =>
      Using.resource(stmt.executeQuery("SELECT COUNT(*) FROM `users`")) { rs =>
        if (rs.next()) {
          proceed = rs.getLong(1) == 1
        }
      }
    }
    if (proceed) {
      // We only have a single user.
      proceed = false
      Using.resource(context.getConnection.prepareStatement("SELECT `onetime_password` from `users` WHERE `id` = ?")) { stmt =>
        stmt.setLong(1, 1)
        Using.resource(stmt.executeQuery()) { rs =>
          if (rs.next()) {
            proceed = rs.getShort(1) == 1
          }
        }
      }
      if (proceed) {
        proceed = false
        // The single user is the default admin for which there has never been a login made.
        Using.resource(context.getConnection.createStatement()) { stmt =>
          Using.resource(stmt.executeQuery("SELECT `theme_key`, `active` FROM `themes`")) { rs =>
            var hasNonDefaultTheme = false
            var themeIsActive = false
            while (rs.next()) {
              val key = rs.getString(1)
              if (!hasNonDefaultTheme && key != "ec" && key != "gitb") {
                hasNonDefaultTheme = true
              }
              if (!themeIsActive && rs.getShort(2) == 1) {
                themeIsActive = true
              }
            }
            proceed = !hasNonDefaultTheme && themeIsActive
          }
        }
      }
      if (proceed) {
        // At this point we are certain that the Test Bed instance has never been used (at least through its UI).
        Using.resource(context.getConnection.prepareStatement("UPDATE `themes` SET `active` = ?")) { stmt =>
          stmt.setShort(1, 0)
          stmt.executeUpdate()
        }
        LOG.info("Set default themes to inactive")
      }
    }
  }

}
