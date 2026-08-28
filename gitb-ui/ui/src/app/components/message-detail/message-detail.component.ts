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

import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Constants } from '../../common/constants';
import { MessageService } from '../../services/message.service';
import { MessageDetailView } from '../../types/message-detail-view';
import { MessageChainItem } from '../../types/message-chain-item';
import { CheckboxOption } from '../checkbox-option-panel/checkbox-option';
import { CheckboxOptionState } from '../checkbox-option-panel/checkbox-option-state';

/**
 * The panel below the message table showing the currently selected message's content. Kept as its own
 * component so a future alternative presentation (separate screen, side panel) can reuse it without
 * restructuring the "My messages" page - see the task's "Subsequent steps" notes.
 */
@Component({
  selector: 'app-message-detail',
  standalone: false,
  templateUrl: './message-detail.component.html',
  styleUrl: './message-detail.component.less'
})
export class MessageDetailComponent implements OnChanges {

  @Input() mode: 'received'|'sent' = 'received'
  @Input() detail?: MessageDetailView
  // Fetched by the parent (MessagesComponent.selectMessage()) in parallel with detail - see that
  // method's comment for why: the row already knows its own parentMessageId, so both requests can be
  // issued together and applied as a single atomic display swap rather than the chain trailing the
  // message onto the screen.
  @Input() chain: MessageChainItem[] = []
  @Input() loading = false
  @Input() actionPending = false

  @Output() replyRequested = new EventEmitter<void>()
  @Output() markReadRequested = new EventEmitter<void>()
  @Output() deleteRequested = new EventEmitter<void>()

  expanded = false
  recipientNames?: string[]
  loadingNames = false

  protected readonly Constants = Constants

  // Stable reference so the options panel's input doesn't churn on every change-detection pass.
  optionsFactory: () => Observable<CheckboxOption[][]> = () => this.loadAvailableOptions()

  constructor(
    private readonly messageService: MessageService
  ) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['detail']) {
      this.expanded = false
      this.recipientNames = undefined
    }
  }

  toggleRecipients() {
    if (this.detail == undefined || this.loadingNames) return
    if (this.expanded) {
      this.expanded = false
    } else if (this.recipientNames != undefined) {
      this.expanded = true
    } else {
      this.loadingNames = true
      this.messageService.getMessageRecipients(this.detail.id).subscribe((names) => {
        this.recipientNames = names
        this.expanded = true
      }).add(() => {
        this.loadingNames = false
      })
    }
  }

  private loadAvailableOptions(): Observable<CheckboxOption[][]> {
    const options: CheckboxOption[] = [
      { key: 'reply', label: 'Reply', default: true, iconClass: Constants.BUTTON_ICON.REPLY }
    ]
    if (this.mode == 'received') {
      if (this.detail?.read) {
        options.push({ key: 'unread', label: 'Mark unread', default: true, iconClass: Constants.BUTTON_ICON.MESSAGE_UNREAD })
      } else {
        options.push({ key: 'read', label: 'Mark read', default: true, iconClass: Constants.BUTTON_ICON.MESSAGE_READ })
      }
    }
    options.push({ key: 'delete', label: 'Delete', default: true, iconClass: Constants.BUTTON_ICON.DELETE })
    return of([options])
  }

  handleOption(event: CheckboxOptionState) {
    if (event['reply']) {
      this.replyRequested.emit()
    } else if (event['read'] || event['unread']) {
      this.markReadRequested.emit()
    } else if (event['delete']) {
      this.deleteRequested.emit()
    }
  }

}
