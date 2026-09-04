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

import models.{Constants, Enums}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.slf4j.LoggerFactory

import java.sql.Types
import scala.collection.mutable
import scala.util.Using

class V162__Message_peer_snapshots extends BaseJavaMigration {

  private val LOG = LoggerFactory.getLogger(classOf[V162__Message_peer_snapshots])

  override def migrate(context: Context): Unit = {
    val connection = context.getConnection
    // Determine message sender types and sender user names.
    val messageToSenderMap = mutable.HashMap[Long, (Short, Option[String])]()
    Using.resource(connection.prepareStatement(
      "SELECT `m`.`id`, `o`.`admin_organization`, `o`.`community`, `u`.`name` FROM `messages` `m` JOIN `organizations` `o` ON (`m`.`sender_id` = `o`.`id`) LEFT JOIN `users` `u` ON (`m`.`sender_user_id` = `u`.`id`) WHERE `m`.`sender_id` IS NOT NULL")) { select =>
      Using.resource(select.executeQuery()) { rs =>
        while (rs.next()) {
          val messageId = rs.getLong(1)
          val senderIsAdminOrganisation = rs.getBoolean(2)
          val senderCommunity = rs.getLong(3)
          val senderUserName = Option(rs.getString(4)).filter(_.nonEmpty)
          val senderType: Short = if (senderCommunity == Constants.DefaultCommunityId) {
            Enums.MessagePeerType.TestBedAdmin.id.toShort
          } else if (senderIsAdminOrganisation) {
            Enums.MessagePeerType.CommunityAdmin.id.toShort
          } else {
            Enums.MessagePeerType.Organisation.id.toShort
          }
          messageToSenderMap += (messageId -> (senderType, senderUserName))
        }
      }
    }
    // Determine message recipient types and information.
    val messageRecipientToRecipientTypeMap = mutable.HashMap[Long, Short]()
    val messageToRecipientMap = mutable.HashMap[Long, (Option[Short], Option[String], Int)]()
    Using.resource(connection.prepareStatement("SELECT `r`.`id`, `r`.`message_id`, `r`.`recipient_name_snapshot`, `o`.`admin_organization`, `o`.`community` FROM `messagerecipients` `r` JOIN `organizations` `o` ON `r`.`recipient_id` = `o`.`id`  WHERE `r`.`recipient_id` IS NOT NULL")) { select =>
      Using.resource(select.executeQuery()) { rs =>
        while (rs.next()) {
          val messageRecipientId = rs.getLong(1)
          val messageId = rs.getLong(2)
          val recipientNameSnapshot = rs.getString(3)
          val recipientIsAdminOrganisation = rs.getBoolean(4)
          val recipientCommunity = rs.getLong(5)
          val recipientType = if (recipientCommunity == Constants.DefaultCommunityId) {
            Enums.MessagePeerType.TestBedAdmin.id.toShort
          } else if (recipientIsAdminOrganisation) {
            Enums.MessagePeerType.CommunityAdmin.id.toShort
          } else {
            Enums.MessagePeerType.Organisation.id.toShort
          }
          messageRecipientToRecipientTypeMap += (messageRecipientId -> recipientType)
          val newEntry = messageToRecipientMap.get(messageId) match {
            case Some((_, _, recipientCount)) => (None, None, recipientCount + 1)
            case None => (Some(recipientType), Some(recipientNameSnapshot), 1)
          }
          messageToRecipientMap.put(messageId, newEntry)
        }
      }
    }
    // Updates
    messageToSenderMap.foreach { case (messageId, (senderType, senderUserName)) =>
      Using.resource(connection.prepareStatement("UPDATE `messages` SET `sender_type` = ?, `sender_user_name_snapshot` = ? WHERE `id` = ?")) { update =>
        update.setShort(1, senderType)
        senderUserName match {
          case Some(name) => update.setString(2, name)
          case None => update.setNull(2, Types.VARCHAR)
        }
        update.setLong(3, messageId)
        update.executeUpdate()
      }
    }
    LOG.info("Set sender type and sender user name snapshot to {} message(s)", messageToSenderMap.size)
    messageToRecipientMap.foreach { case (messageId, (singleRecipientType, singleRecipientName, recipientCount)) =>
      Using.resource(connection.prepareStatement("UPDATE `messages` SET `single_recipient_type` = ?, `single_recipient_name_snapshot` = ?, `recipient_count` = ? WHERE `id` = ?")) { update =>
        singleRecipientType match {
          case Some(v) => update.setShort(1, v)
          case None => update.setNull(1, Types.TINYINT)
        }
        singleRecipientName match {
          case Some(v) => update.setString(2, v)
          case None => update.setNull(2, Types.VARCHAR)
        }
        update.setInt(3, recipientCount)
        update.setLong(4, messageId)
        update.executeUpdate()
      }
    }
    LOG.info("Set snapshot recipient information to {} message(s)", messageToRecipientMap.size)
    messageRecipientToRecipientTypeMap.foreach { case (id, recipientType) =>
      Using.resource(connection.prepareStatement("UPDATE `messagerecipients` SET `recipient_type` = ? WHERE `id` = ?")) { update =>
        update.setShort(1, recipientType)
        update.setLong(2, id)
        update.executeUpdate()
      }
    }
    LOG.info("Set recipient type to {} message recipient(s)", messageRecipientToRecipientTypeMap.size)
  }

}
