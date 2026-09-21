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

/** Presence of a row means the message delivery identified by recipientId (a MessageRecipients id) is
 * unread for userId. Absence means it is read - either because the user read it, or because they never
 * had it delivered to begin with (e.g. their account was created after the message was sent) - both cases
 * are intentionally indistinguishable. See MessageManager for how rows are created (message send, mark
 * unread) and removed (message read, mark read, message deleted, user deleted). */
case class MessageUnreadStatus(recipientId: Long, userId: Long)
