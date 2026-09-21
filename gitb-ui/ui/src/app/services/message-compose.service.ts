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

import { EventEmitter, Injectable } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Constants } from '../common/constants';
import { DataService } from './data.service';
import { ConfirmationDialogService } from './confirmation-dialog.service';
import { MessageDraft } from '../types/message-draft';
import { RecipientOption } from '../types/recipient-option';
import { MessageTarget } from '../types/message-target';
import { MessageChainItem } from '../types/message-chain-item';
import { ComposeMessageModalComponent } from '../modals/compose-message-modal/compose-message-modal.component';

/**
 * Owns the lifecycle of the "New message" compose modal independently of whichever page opened it, so
 * that minimising it (dismissing the modal but retaining the draft) and later maximising it (re-creating
 * the modal from the retained draft) both work regardless of navigation in between. See MessageDraft.
 */
@Injectable({
  providedIn: 'root'
})
export class MessageComposeService {

  minimised = false
  draft?: MessageDraft
  // Fires whenever minimised/draft presence changes - the minimised bar uses this to know when to show/hide.
  onStateChange = new EventEmitter<void>()
  // Fires only once a message has actually been sent - pages showing message listings use this to refresh.
  onMessageSent = new EventEmitter<void>()

  // Session-scoped (not persisted across browser restarts, deliberately - a stale draft popping back up
  // in an unrelated later session would be surprising) so an in-progress draft survives a page refresh.
  private static readonly STORAGE_KEY = 'itb.messageDraft'

  constructor(
    private readonly modalService: NgbModal,
    private readonly dataService: DataService,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) {
    // The most reliable point at which to capture the latest in-progress edits (e.g. still-open modal
    // content) before a refresh - onStateChange only fires at explicit transitions (minimise, send,
    // cancel...), not on every keystroke into the two-way-bound draft fields.
    window.addEventListener('beforeunload', () => this.persist())
    this.dataService.onUserLoaded$.subscribe(data => {
      this.restore()
    })
  }

  openNew() {
    this.replaceDraftIfNeeded(() => this.startDraft({ senderId: this.dataService.user!.id!, important: false, recipients: [], recipientDisplay: [] }))
  }

  openReply(parentMessageId: number, subject: string, chain: MessageChainItem[], recipients: MessageTarget[], recipientDisplay: RecipientOption[], adminCommunitySelection?: RecipientOption) {
    this.replaceDraftIfNeeded(() => this.startDraft({
      senderId: this.dataService.user!.id!,
      important: false,
      recipients: recipients,
      recipientDisplay: recipientDisplay,
      adminCommunitySelection: adminCommunitySelection,
      parentMessageId: parentMessageId,
      subject: subject,
      pristineSubject: subject,
      chain: chain,
      pristineRecipientCount: recipients.length
    }))
  }

  openDraft() {
    if (this.draft != undefined) {
      this.minimised = false
      this.persist()
      this.openModal()
    }
  }

  /** True while the current draft holds anything beyond what a brand-new draft for the caller's role
   * (or, for a reply, the freshly-opened reply itself) would already contain - see the "Replace draft
   * message" / "Discard draft message" prompts. */
  hasNonDefaultState(): boolean {
    if (this.draft == undefined) return false
    if (this.draft.important) return true
    const subject = (this.draft.subject ?? '').trim()
    const pristineSubject = (this.draft.pristineSubject ?? '').trim()
    if (subject !== pristineSubject) return true
    if (this.hasBodyContent(this.draft.body)) return true
    if (this.draft.parentMessageId != undefined) {
      return this.draft.recipients.length != (this.draft.pristineRecipientCount ?? 0)
    }
    return this.draft.recipients.length != this.pristineRecipientCount()
  }

  discardDraft() {
    if (this.hasNonDefaultState()) {
      this.confirmationDialogService.confirmedDangerous('Discard draft message', 'Are you sure you want to discard the current draft?', 'Discard', 'Cancel', Constants.BUTTON_ICON.DELETE, Constants.BUTTON_ICON.CANCEL).subscribe(() => {
        this.clearDraft()
      })
    } else {
      this.clearDraft()
    }
  }

  clearDraft() {
    if (this.draft != undefined) {
      this.draft = undefined
      this.minimised = false
      this.persist()
      this.onStateChange.emit()
    }
  }

  private replaceDraftIfNeeded(startFresh: () => void) {
    if (this.minimised && this.hasNonDefaultState()) {
      this.confirmationDialogService.confirmThreeWay(
        'Replace draft message', 'A message is currently being drafted. How should this be handled?',
        'Resume draft', 'Discard draft', 'Cancel',
        Constants.BUTTON_ICON.MESSAGE_NEW, Constants.BUTTON_ICON.DELETE, Constants.BUTTON_ICON.CANCEL
      ).subscribe((choice) => {
        if (choice == 'action') {
          this.openDraft()
        } else if (choice == 'middle') {
          startFresh()
        }
      })
    } else {
      startFresh()
    }
  }

  private startDraft(draft: MessageDraft) {
    this.draft = draft
    this.minimised = false
    this.persist()
    this.openModal()
  }

  private openModal() {
    const modalRef = this.modalService.open(ComposeMessageModalComponent, { size: 'lg', backdrop: 'static' })
    const instance = modalRef.componentInstance as ComposeMessageModalComponent
    instance.draft = this.draft!
    instance.sent.subscribe(() => {
      this.draft = undefined
      this.minimised = false
      this.persist()
      this.onStateChange.emit()
      this.onMessageSent.emit()
    })
    instance.cancelled.subscribe(() => {
      this.draft = undefined
      this.minimised = false
      this.persist()
      this.onStateChange.emit()
    })
    instance.minimiseRequested.subscribe(() => {
      this.minimised = true
      this.persist()
      this.onStateChange.emit()
    })
    this.onStateChange.emit()
  }

  /** Writes the in-progress draft to sessionStorage so it survives a page refresh - restored by
   * restore() below. Always persisted as minimised: re-opening the compose modal automatically on every
   * page load would be jarring, so a restored draft always starts out as the minimised bar instead,
   * letting the user choose whether to resume it. */
  private persist() {
    try {
      if (this.draft != undefined) {
        sessionStorage.setItem(MessageComposeService.STORAGE_KEY, JSON.stringify(this.draft))
      } else {
        sessionStorage.removeItem(MessageComposeService.STORAGE_KEY)
      }
    } catch {
      // sessionStorage unavailable (e.g. disabled) - the draft simply won't survive a refresh.
    }
  }

  private restore() {
    try {
      const raw = sessionStorage.getItem(MessageComposeService.STORAGE_KEY)
      if (raw != undefined) {
        const data = JSON.parse(raw) as MessageDraft
        if (data.senderId != this.dataService.user!.id) {
          sessionStorage.removeItem(MessageComposeService.STORAGE_KEY)
        } else {
          this.draft = data
          this.minimised = true
          this.onStateChange.emit()
        }
      }
    } catch {
      // Corrupt or unavailable storage - start with no draft.
    }
  }

  private pristineRecipientCount(): number {
    return (!this.dataService.isSystemAdmin && !this.dataService.isCommunityAdmin) ? 1 : 0
  }

  private hasBodyContent(html?: string): boolean {
    if (html == undefined) return false
    const stripped = html.replace(/<[^>]*>/g, '').replace(/&nbsp;/gi, ' ').trim()
    return stripped.length > 0
  }

}
