package db.migration.default

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.slf4j.LoggerFactory
import utils.CryptoUtil

class V114__Set_specification_group_api_keys extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V114__Set_specification_group_api_keys])

  override def migrate(context: Context): Unit = {
    var counter = 0
    val select = context.getConnection.createStatement
    try {
      val rs = select.executeQuery("SELECT `id` from `specificationgroups` WHERE `api_key` IS NULL")
      try {
        while (rs.next) {
          val id = rs.getLong(1)
          val update = context.getConnection.prepareStatement("UPDATE `specificationgroups` SET `api_key` = ? WHERE `id` = ?")
          try {
            update.setString(1, CryptoUtil.generateApiKey())
            update.setLong(2, id)
            update.executeUpdate()
          } finally if (update != null) update.close()
          counter += 1
        }
      } finally if (rs != null) rs.close()
    } finally if (select != null) select.close()
    LOG.info("Updated "+counter+" specification group(s) with API keys")
  }

}
