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
