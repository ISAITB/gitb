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

import { MessageTarget } from './message-target';
import { RecipientOption } from './recipient-option';
import { MessageChainItem } from './message-chain-item';

/** The in-progress state of a message being composed - held by MessageComposeService so it survives the
 * modal being minimised (dismissed) and later maximised (a fresh modal instance re-created from this). */
export interface MessageDraft {

    senderId: number
    subject?: string
    // The subject text the draft started out with (empty for a new message, "RE: ..." for a reply) -
    // compared against the current subject to tell whether the user actually changed anything, so the
    // "discard draft" confirmation isn't shown for an untouched reply. See MessageComposeService.hasNonDefaultState.
    pristineSubject?: string
    body?: string
    important: boolean
    // Authoritative recipients to send to, kept in sync with the picker selection(s) below.
    recipients: MessageTarget[]
    // The picker's selected items, retained only so the "Send to:" control(s) can restore their visual
    // selection when the modal is re-created on maximise.
    recipientDisplay: RecipientOption[]
    // Test Bed administrator only: the first-stage community picker's current selection.
    adminCommunitySelection?: RecipientOption
    // Set when this draft is a reply - the id of the message being replied to. The recipient picker(s)
    // above are still shown and fully editable - recipientDisplay/adminCommunitySelection are just
    // pre-seeded with a role-appropriate default (see MessagesComponent.buildReplySeed).
    parentMessageId?: number
    // Reply only - the parent's chain of earlier messages, pre-loaded (with the current message's peer
    // already resolved) before the modal opens - see MessagesComponent.startReply.
    chain?: MessageChainItem[]
    // Reply only - the pre-filled default's recipients.length (0 or 1), so
    // MessageComposeService.hasNonDefaultState can tell a genuine recipient edit apart from the
    // untouched default (a new message's equivalent baseline is role-based - see pristineRecipientCount).
    pristineRecipientCount?: number

}
