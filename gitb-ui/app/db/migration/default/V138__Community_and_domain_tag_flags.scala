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
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.util.Using

class V138__Community_and_domain_tag_flags extends BaseJavaMigration {

  private val LOG = LoggerFactory.getLogger(classOf[V138__Community_and_domain_tag_flags])

  override def migrate(context: Context): Unit = {
    processTable(context, "communities")
    processTable(context, "domains")
    LOG.info("Updated tag flags")
  }

  private def processTable(context: Context, tableName: String): Unit = {
    val connection = context.getConnection
    Using(connection.prepareStatement(s"SELECT `id`,`tags` from `$tableName` WHERE `tags` IS NOT NULL")) { select =>
      Using(connection.prepareStatement(s"UPDATE `$tableName` SET `tags` = ? WHERE `id` = ?")) { update =>
        Using(select.executeQuery()) { rs =>
          while (rs.next) {
            val existingId = rs.getLong(1)
            val existingTags = rs.getString(2)
            val updatedTags = Json.parse(existingTags) match {
              case JsArray(tags) =>
                val updatedArray = tags.map {
                  case obj: JsObject => obj ++ Json.obj("flag1" -> true, "flag2" -> true)
                  case other => other
                }
                Json.stringify(JsArray(updatedArray))
              case _ =>
                LOG.warn("Parsed tags column was not a JSON array for id={} in {}", existingId, tableName)
                existingTags
            }
            update.setString(1, updatedTags)
            update.setLong(2, existingId)
            update.executeUpdate()
          }
        }
      }
    }
  }

}
