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

package models

import java.sql.Timestamp

case class MessageTarget(targetType: Short, communityId: Option[Long], organisationId: Option[Long])

case class ReceivedMessageListItem(id: Long, subject: Option[String], bodyPreview: Option[String],
                                   senderName: String, date: Timestamp, important: Boolean, read: Boolean,
                                   parentMessageId: Option[Long])

case class SentMessageListItem(id: Long, subject: Option[String], bodyPreview: Option[String],
                               recipientName: String, recipientCount: Int, date: Timestamp, important: Boolean,
                               parentMessageId: Option[Long])

case class ReceivedMessageDetail(id: Long, subject: Option[String], body: Option[String],
                                 senderName: String, senderUserName: Option[String], date: Timestamp, important: Boolean,
                                 parentMessageId: Option[Long])

case class SentMessageDetail(id: Long, subject: Option[String], body: Option[String],
                             recipientCount: Int, singleRecipientName: Option[String],
                             date: Timestamp, important: Boolean, parentMessageId: Option[Long])

case class MessageChainItem(id: Long, subject: Option[String], body: Option[String], date: Timestamp, important: Boolean,
                            senderName: String, senderUserName: Option[String], viewerIsSender: Boolean)

case class ReplyTargetInfo(targetType: Option[Short], communityId: Option[Long], communityName: Option[String],
                           organisationId: Option[Long], organisationName: Option[String])
