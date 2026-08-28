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

/** A common shape used by app-message-detail for both received and sent message details - see
 * MessageRowView for the equivalent used by the listing table. peerName is blank when peerCount > 1 (a
 * sent, fanned-out message) - the panel then shows a clickable "(N recipients)" indicator instead. */
export interface MessageDetailView {

    id: number
    subject?: string
    body?: string
    peerName: string
    peerCount: number
    date: string
    important: boolean
    parentMessageId?: number
    // Only meaningful for received messages - drives the read/unread option shown in the header's options menu.
    read?: boolean

}
