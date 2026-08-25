/*
 * Copyright (C) 2026 European Union
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
import play.api.libs.json.Json

import scala.util.Using

/**
 * The single-value `welcome_title` system configuration is replaced by the JSON-serialised
 * `welcome_texts` setting (see [[models.WelcomeTexts]]), which also covers the welcome page's option
 * card texts. This migrates any existing customised title into the new setting, carrying over just
 * the title - the remaining texts are backfilled with their built-in defaults wherever the value is
 * read (see utils.JsonUtil#parseJsWelcomeTexts).
 */
class V154__Welcome_page_texts extends BaseJavaMigration {

  private val LOG = LoggerFactory.getLogger(classOf[V154__Welcome_page_texts])

  override def migrate(context: Context): Unit = {
    val connection = context.getConnection
    val existingTitle = Using.resource(connection.prepareStatement(
      "SELECT `parameter` FROM `systemconfigurations` WHERE `name` = 'welcome_title'")) { select =>
      Using.resource(select.executeQuery()) { rs =>
        if (rs.next()) Option(rs.getString(1)) else None
      }
    }
    existingTitle match {
      case Some(title) =>
        val welcomeTexts = Json.obj("title" -> title).toString()
        Using.resource(connection.prepareStatement(
          "UPDATE `systemconfigurations` SET `name` = 'welcome_texts', `parameter` = ? WHERE `name` = 'welcome_title'")) { update =>
          update.setString(1, welcomeTexts)
          update.executeUpdate()
        }
        LOG.info("Migrated the custom welcome page title to the new welcome page texts setting")
      case None =>
        // No customised title (or a row with a NULL parameter): drop the row, there is nothing to carry over.
        Using.resource(connection.prepareStatement("DELETE FROM `systemconfigurations` WHERE `name` = 'welcome_title'")) { delete =>
          delete.executeUpdate()
        }
    }
  }

}
