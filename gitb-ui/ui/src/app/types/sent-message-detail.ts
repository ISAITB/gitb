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

/** singleRecipientName is only present when recipientCount == 1 - otherwise the UI shows a clickable
 * "(N recipients)" indicator that lazily loads the full list (see MessageService.getMessageRecipients). */
export interface SentMessageDetail {

    id: number
    subject?: string
    body?: string
    recipientCount: number
    singleRecipientName?: string
    date: string
    important: boolean
    parentMessageId?: number

}
