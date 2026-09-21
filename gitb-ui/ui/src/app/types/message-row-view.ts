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

/** A common row shape used by app-message-table for both the received and sent message listings, so the
 * table itself doesn't need to know about ReceivedMessage/SentMessage's differing field names. peerCount
 * is always 1 for received messages; for sent messages it may be greater than 1, in which case peerName
 * is blank and the table shows "(N recipients)" instead. */
export interface MessageRowView {

    id: number
    subject?: string
    bodyPreview?: string
    peerName: string
    peerCount: number
    date: string
    important: boolean
    // Only meaningful for received messages.
    read?: boolean
    // Present when this message is itself a reply - lets selectMessage() fetch its chain in parallel
    // with the message detail instead of waiting for the detail response (see MessageManager.getMessageChain).
    parentMessageId?: number
    // Client-side only - row selection state for the aggregate action toolbar.
    checked?: boolean
    // Client-side only - true while an action (mark read/unread, delete, reply-menu action) triggered
    // from this row is in flight, driving the row's own pending-button treatment.
    actionPending?: boolean

}
