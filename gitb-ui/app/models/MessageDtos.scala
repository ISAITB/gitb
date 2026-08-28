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

/** A recipient descriptor as submitted by the compose modal. Resolved and authorised server-side into
 * a concrete set of recipient organisation ids - see MessageManager.resolveTargets and
 * AuthorizationManager.canSendMessage. */
case class MessageTarget(targetType: Short, communityId: Option[Long], organisationId: Option[Long])

case class ReceivedMessageListItem(id: Long, subject: Option[String], bodyPreview: Option[String],
                                   senderName: String, date: Timestamp, important: Boolean, read: Boolean,
                                   parentMessageId: Option[Long])

case class SentMessageListItem(id: Long, subject: Option[String], bodyPreview: Option[String],
                               recipientName: String, recipientCount: Int, date: Timestamp, important: Boolean,
                               parentMessageId: Option[Long])

case class ReceivedMessageDetail(id: Long, subject: Option[String], body: Option[String],
                                 senderName: String, date: Timestamp, important: Boolean,
                                 parentMessageId: Option[Long])

/** singleRecipientName is only populated when recipientCount == 1 - for a fan-out message the client
 * shows "(N recipients)" and lazily loads the full list on demand (see MessageManager.getMessageRecipientNames). */
case class SentMessageDetail(id: Long, subject: Option[String], body: Option[String],
                             recipientCount: Int, singleRecipientName: Option[String],
                             date: Timestamp, important: Boolean, parentMessageId: Option[Long])

/** One entry in the "chain of earlier messages" shown when composing a reply, or when viewing a
 * received/sent message that is itself a reply - see MessageManager.getMessageChain. senderName is
 * viewer-aware (see MessageManager.resolveAdminPeerNames), matching the received/sent list displays. */
case class MessageChainItem(id: Long, subject: Option[String], bodyPreview: Option[String],
                            body: Option[String], date: Timestamp, important: Boolean, senderName: String)

/** The default recipient a reply's picker should be pre-selected with, expressed as a target descriptor
 * the client maps onto the same role-specific static option (or, for organisationId/communityId, a
 * loaded list entry) used by a new message's picker - see MessageManager.resolveReplyTarget.
 * communityName/organisationName are populated only when communityId/organisationId are, so the client
 * can show the pre-selected chip immediately without waiting for its picker's own loader to resolve. */
case class ReplyTargetInfo(targetType: Option[Short], communityId: Option[Long], communityName: Option[String],
                           organisationId: Option[Long], organisationName: Option[String])
