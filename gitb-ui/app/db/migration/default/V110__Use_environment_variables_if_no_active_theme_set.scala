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
