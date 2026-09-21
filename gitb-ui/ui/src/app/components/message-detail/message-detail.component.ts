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

import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ChangeDetectionStrategy} from '@angular/core';
import {MessageService} from '../../services/message.service';
import {MessageDetailView} from '../../types/message-detail-view';

/**
 * The panel below the message table showing the currently selected message's content. Kept as its own
 * component so a future alternative presentation (separate screen, side panel) can reuse it without
 * restructuring the "My messages" page - see the task's "Subsequent steps" notes.
 *
 * The message "card" itself (header, options menu, peer pill, collapsible body) is delegated to
 * app-message-item - shared with each entry of the reply chain below it (see MessageChainComponent) so
 * both present identically. This component keeps ownership only of what's specific to being the *main*
 * selected message: the empty/loading placeholder, and the lazy recipient-name fetch for a fanned-out
 * sent message (app-message-item only displays that state, it doesn't know how to load it).
 */
@Component({
  selector: 'app-message-detail',
  standalone: false,
  templateUrl: './message-detail.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './message-detail.component.less'
})
export class MessageDetailComponent implements OnChanges {

  @Input() mode: 'received'|'sent' = 'received'
  @Input() detail?: MessageDetailView
  @Input() loading = false
  @Input() replyPending = false
  @Input() markReadOrUnreadPending = false
  @Input() deletePending = false

  @Output() replyRequested = new EventEmitter<void>()
  @Output() markReadRequested = new EventEmitter<void>()
  @Output() deleteRequested = new EventEmitter<void>()
  // Forwarded from a specific chain entry (see app-message-chain) - MessagesComponent routes these to
  // the same per-id reply()/markOneRead()/deleteOne() methods it already uses for table rows.
  @Output() chainReplyRequested = new EventEmitter<{ id: number, subject?: string }>()

  recipientNames?: string[]
  loadingNames = false

  constructor(
    private readonly messageService: MessageService
  ) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['detail']) {
      this.recipientNames = undefined
      this.loadingNames = false
    }
  }

  toggleRecipients() {
    if (this.detail == undefined || this.loadingNames || this.recipientNames != undefined) return
    this.loadingNames = true
    this.messageService.getMessageRecipients(this.detail.id).subscribe((names) => {
      this.recipientNames = names
    }).add(() => {
      this.loadingNames = false
    })
  }

}
