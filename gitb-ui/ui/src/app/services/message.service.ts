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

import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { RestService } from './rest.service';
import { SearchResult } from '../types/search-result';
import { ReceivedMessage } from '../types/received-message';
import { SentMessage } from '../types/sent-message';
import { ReceivedMessageDetail } from '../types/received-message-detail';
import { SentMessageDetail } from '../types/sent-message-detail';
import { MessageTarget } from '../types/message-target';
import { MessageChainItem } from '../types/message-chain-item';
import { ReplyTargetInfo } from '../types/reply-target-info';

@Injectable({
  providedIn: 'root'
})
export class MessageService {

  constructor(
    private readonly restService: RestService
  ) { }

  getReceivedMessages(page: number, limit: number, filterText: string|undefined, showRead: boolean, showUnread: boolean,
                      showImportant: boolean, deliveredAfter: string|undefined, deliveredBefore: string|undefined,
                      sortColumn: string|undefined, sortOrder: string|undefined, peerTargets: MessageTarget[]) {
    return this.restService.get<SearchResult<ReceivedMessage>>({
      path: ROUTES.controllers.MessageService.getReceivedMessages().url,
      authenticate: true,
      params: {
        page: page,
        limit: limit,
        filter: filterText,
        show_read: showRead,
        show_unread: showUnread,
        show_important: showImportant,
        start_time_begin: deliveredAfter,
        start_time_end: deliveredBefore,
        sort_column: sortColumn,
        sort_order: sortOrder,
        peer_targets: peerTargets.length > 0 ? JSON.stringify(peerTargets) : undefined
      }
    })
  }

  getSentMessages(page: number, limit: number, filterText: string|undefined, showImportant: boolean, createdAfter: string|undefined,
                  createdBefore: string|undefined, sortColumn: string|undefined, sortOrder: string|undefined, peerTargets: MessageTarget[]) {
    return this.restService.get<SearchResult<SentMessage>>({
      path: ROUTES.controllers.MessageService.getSentMessages().url,
      authenticate: true,
      params: {
        page: page,
        limit: limit,
        filter: filterText,
        show_important: showImportant,
        start_time_begin: createdAfter,
        start_time_end: createdBefore,
        sort_column: sortColumn,
        sort_order: sortOrder,
        peer_targets: peerTargets.length > 0 ? JSON.stringify(peerTargets) : undefined
      }
    })
  }

  getReceivedMessage(id: number) {
    return this.restService.get<ReceivedMessageDetail>({
      path: ROUTES.controllers.MessageService.getMessage(id).url,
      authenticate: true,
      params: { sent: false }
    })
  }

  getSentMessage(id: number) {
    return this.restService.get<SentMessageDetail>({
      path: ROUTES.controllers.MessageService.getMessage(id).url,
      authenticate: true,
      params: { sent: true }
    })
  }

  getMessageRecipients(id: number) {
    return this.restService.get<string[]>({
      path: ROUTES.controllers.MessageService.getMessageRecipients(id).url,
      authenticate: true
    })
  }

  getMessageChain(id: number) {
    return this.restService.get<MessageChainItem[]>({
      path: ROUTES.controllers.MessageService.getMessageChain(id).url,
      authenticate: true
    })
  }

  /** Used only for the post-login unread-messages notification/menu badge - see MenuItemStatus and
   * IndexComponent.handlePostUserLoad. */
  hasUnreadMessages() {
    return this.restService.get<{ unread: boolean }>({
      path: ROUTES.controllers.MessageService.hasUnreadMessages().url,
      authenticate: true
    })
  }

  getReplyTarget(id: number) {
    return this.restService.get<ReplyTargetInfo>({
      path: ROUTES.controllers.MessageService.getReplyTarget(id).url,
      authenticate: true
    })
  }

  createMessage(subject: string|undefined, body: string|undefined, important: boolean, recipients: MessageTarget[], parentMessageId: number|undefined) {
    return this.restService.post<void>({
      path: ROUTES.controllers.MessageService.createMessage().url,
      authenticate: true,
      data: {
        subject: subject,
        body: body,
        important: important,
        recipients: JSON.stringify(recipients),
        parent_message_id: parentMessageId
      }
    })
  }

  updateMessageReadStatus(ids: number[], read: boolean) {
    return this.restService.post<void>({
      path: ROUTES.controllers.MessageService.updateMessageReadStatus().url,
      authenticate: true,
      data: {
        ids: ids.join(','),
        read: read
      }
    })
  }

  deleteMessages(ids: number[], sent: boolean) {
    return this.restService.post<void>({
      path: ROUTES.controllers.MessageService.deleteMessages().url,
      authenticate: true,
      data: {
        ids: ids.join(','),
        sent: sent
      }
    })
  }

}
