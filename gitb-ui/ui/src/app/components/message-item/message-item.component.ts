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
import {Constants} from '../../common/constants';
import {Utils} from '../../common/utils';

/**
 * A single message's display "card": header (importance, subject, options menu), peer pill + date +
 * collapsing icon, and the body itself behind a collapse. Shared by the main selected-message panel
 * (MessageDetailComponent, [collapsedByDefault]=false) and each entry of the reply chain
 * (MessageChainComponent, [collapsedByDefault]=true) so both present identically - the review's
 * explicit ask - with the same options menu, the same recipient-count expand behaviour for a
 * multi-recipient sent message, and the same collapse/expand affordance.
 */
@Component({
  selector: 'app-message-item',
  standalone: false,
  templateUrl: './message-item.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './message-item.component.less'
})
export class MessageItemComponent implements OnChanges {

  // Identifies which message is being rendered - used only to detect that a *different* message has
  // been bound to this instance (e.g. a newly selected row re-using the same MessageDetailComponent),
  // at which point the collapse state resets to collapsedByDefault rather than carrying over.
  @Input() itemId!: number
  @Input() subject?: string
  @Input() body?: string
  @Input() important = false
  @Input() date!: string
  @Input() peerLabel: 'From'|'To' = 'From'
  @Input() peerName!: string
  @Input() peerSubText?: string
  // > 1 switches the peer pill to the collapsible "(N recipients)" indicator - only ever exercised by
  // the main detail panel today (a fanned-out sent message); chain entries always pass the default.
  @Input() peerCount = 1
  @Input() recipientNames?: string[]
  @Input() loadingRecipientNames = false
  @Input() collapsedByDefault = false
  // False hides the options menu entirely - used for a chain entry the viewer's own side has
  // soft-deleted (see MessageChainItem.deleted), where no action makes sense any more.
  @Input() showOptions = true
  // Undefined means "Mark read/unread" doesn't apply (a sent message, or a chain entry the viewer sent
  // rather than received - see MessageChainItem.viewerIsSender) - the option is otherwise always shown
  // as read/unread depending on this value, alongside the always-present Reply and Delete.
  @Input() read?: boolean
  @Input() replyPending = false
  @Input() markReadOrUnreadPending = false
  @Input() deletePending = false
  @Input() showDelete = true
  @Input() showMarkReadOrUnread = true

  @Output() replyRequested = new EventEmitter<void>()
  @Output() markReadRequested = new EventEmitter<void>()
  @Output() deleteRequested = new EventEmitter<void>()
  @Output() recipientsToggleRequested = new EventEmitter<void>()

  expanded = false
  recipientsExpanded = false
  private awaitingRecipients = false

  protected readonly Constants = Constants

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['itemId']) {
      this.expanded = !this.collapsedByDefault
      this.recipientsExpanded = false
      this.awaitingRecipients = false
    }
    if (changes['recipientNames'] && this.awaitingRecipients && this.recipientNames != undefined) {
      this.recipientsExpanded = true
      this.awaitingRecipients = false
    }
  }

  toggleExpanded() {
    this.expanded = !this.expanded
  }

  toggleRecipients() {
    if (this.loadingRecipientNames) return
    if (this.recipientsExpanded) {
      this.recipientsExpanded = false
    } else if (this.recipientNames != undefined) {
      this.recipientsExpanded = true
    } else {
      this.awaitingRecipients = true
      this.recipientsToggleRequested.emit()
    }
  }

  reply() {
    this.replyRequested.emit()
  }

  markReadOrUnread() {
    this.markReadRequested.emit()
  }

  delete() {
    this.deleteRequested.emit()
  }

  protected readonly Utils = Utils;
}
