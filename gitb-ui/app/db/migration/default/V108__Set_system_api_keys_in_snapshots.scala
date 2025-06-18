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
import utils.CryptoUtil

import scala.collection.mutable

class V108__Set_system_api_keys_in_snapshots extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V108__Set_system_api_keys_in_snapshots])

  override def migrate(context: Context): Unit = {
    var counter = 0
    val select = context.getConnection.createStatement
    val systemIdToKeyMap = new mutable.HashMap[Long, String]()
    try {
      val rs = select.executeQuery("SELECT `id` from `conformancesnapshotsystems` WHERE `api_key` IS NULL")
      try {
        while (rs.next) {
          val id = rs.getLong(1)
          val update = context.getConnection.prepareStatement("UPDATE `conformancesnapshotsystems` SET `api_key` = ? WHERE `id` = ?")
          try {
            var apiKey = systemIdToKeyMap.get(id)
            if (apiKey.isEmpty) {
              apiKey = Some(CryptoUtil.generateApiKey())
              systemIdToKeyMap += (id -> apiKey.get)
            }
            update.setString(1, apiKey.get)
            update.setLong(2, id)
            update.executeUpdate()
          } finally if (update != null) update.close()
          counter += 1
        }
      } finally if (rs != null) rs.close()
    } finally if (select != null) select.close()
    LOG.info("Updated "+counter+" systems(s) with "+ systemIdToKeyMap.size +" API keys")
  }

}
