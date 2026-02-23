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

class V133__Session_timeouts_for_interactive_sessions extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V133__Session_timeouts_for_interactive_sessions])

  override def migrate(context: Context): Unit = {
    val select = context.getConnection.prepareStatement("SELECT `parameter` from `systemconfigurations` WHERE `name` = ? AND `parameter` IS NOT NULL")
    select.setString(1, "session_alive_time")
    try {
      val rs = select.executeQuery()
      try {
        while (rs.next) {
          val parameterValue = rs.getString(1)
          val update = context.getConnection.prepareStatement("UPDATE `systemconfigurations` SET `parameter` = ? WHERE `name` = ? AND `parameter` IS NOT NULL")
          try {
            update.setString(1, "{ \"enabled\": true, \"userPendingTimeout\": %s, \"adminPendingTimeout\": %s, \"otherTimeout\": %s }".formatted(parameterValue, parameterValue, parameterValue))
            update.setString(2, "session_alive_time")
            update.executeUpdate()
          } finally if (update != null) update.close()
        }
      } finally if (rs != null) rs.close()
    } finally if (select != null) select.close()
    LOG.info("Updated session timeout settings")
  }

}
