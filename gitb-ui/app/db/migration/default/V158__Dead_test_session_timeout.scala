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

import models.Constants
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

import scala.util.Using

/**
 * The `session_alive_time` system configuration's JSON value (see [[models.SessionTimeoutConfiguration]])
 * is extended with a `deadTimeout` field, used to configure the timeout applicable to dead test sessions
 * (i.e. sessions still listed as active in gitb-ui but unknown to the test engine, typically following a
 * test engine restart). Any existing customised settings are preserved as-is, with `deadTimeout` (and any
 * other timeout property that may be missing) initialised to the same default (3600 seconds). Note that
 * this backfill is not strictly required for correctness - JsonUtil#parseJsSessionTimeoutConfiguration
 * already tolerates missing timeout properties, applying the same default - but keeps the persisted value
 * self-describing and complete.
 */
class V158__Dead_test_session_timeout extends BaseJavaMigration {

  private val LOG = LoggerFactory.getLogger(classOf[V158__Dead_test_session_timeout])

  private val timeoutProperties = Seq("adminPendingTimeout", "userPendingTimeout", "otherTimeout", "deadTimeout")

  override def migrate(context: Context): Unit = {
    val connection = context.getConnection
    val existingValue = Using.resource(connection.prepareStatement(
      "SELECT `parameter` FROM `systemconfigurations` WHERE `name` = 'session_alive_time' AND `parameter` IS NOT NULL")) { select =>
      Using.resource(select.executeQuery()) { rs =>
        if (rs.next()) Option(rs.getString(1)) else None
      }
    }
    existingValue.foreach { parameterValue =>
      val existingJson = Json.parse(parameterValue).as[JsObject]
      val missingProperties = timeoutProperties.filterNot(existingJson.keys.contains)
      if (missingProperties.nonEmpty) {
        val updatedJson = missingProperties.foldLeft(existingJson) { (json, property) =>
          json ++ Json.obj(property -> Constants.DefaultSessionTimeout)
        }
        Using.resource(connection.prepareStatement(
          "UPDATE `systemconfigurations` SET `parameter` = ? WHERE `name` = 'session_alive_time'")) { update =>
          update.setString(1, Json.stringify(updatedJson))
          update.executeUpdate()
        }
        LOG.info("Updated session timeout settings with default values for missing timeout properties")
      }
    }
  }

}
