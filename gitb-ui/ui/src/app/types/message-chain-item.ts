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

/** One entry in the "chain of earlier messages" rendered by app-message-chain - either below the body
 * editor of a reply being composed, or above a reply's own content in the message detail panel. Ordered
 * oldest first by the backend (see MessageManager.getMessageChain). */
export interface MessageChainItem {

    id: number
    subject?: string
    bodyPreview?: string
    body?: string
    date: string
    important?: boolean
    // Viewer-aware sender display name (see MessageManager.resolveAdminPeerNames), shown as a pill.
    senderName: string
    // Client-side only - expand/collapse state, collapsed by default.
    collapsed?: boolean

}
