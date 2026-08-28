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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { map } from 'rxjs/operators';
import { Constants } from '../../common/constants';
import { BaseComponent } from '../../pages/base-component.component';
import { DataService } from '../../services/data.service';
import { MessageService } from '../../services/message.service';
import { OrganisationService } from '../../services/organisation.service';
import { CommunityService } from '../../services/community.service';
import { PopupService } from '../../services/popup.service';
import { ConfirmationDialogService } from '../../services/confirmation-dialog.service';
import { MessageComposeService } from '../../services/message-compose.service';
import { MessageDraft } from '../../types/message-draft';
import { RecipientOption } from '../../types/recipient-option';
import { MessageChainItem } from '../../types/message-chain-item';
import { MultiSelectConfig } from '../../components/multi-select-filter/multi-select-config';
import { FilterUpdate } from '../../components/test-filter/filter-update';

@Component({
  selector: 'app-compose-message-modal',
  standalone: false,
  templateUrl: './compose-message-modal.component.html',
  styleUrl: './compose-message-modal.component.less'
})
export class ComposeMessageModalComponent extends BaseComponent implements OnInit {

  @Input() draft!: MessageDraft
  @Output() sent = new EventEmitter<void>()
  @Output() cancelled = new EventEmitter<void>()
  @Output() minimiseRequested = new EventEmitter<void>()

  sendPending = false
  isReply = false
  chain: MessageChainItem[] = []

  isOrganisationUser = false
  isCommunityAdminRole = false
  isTestBedAdminRole = false

  recipientConfig!: MultiSelectConfig<RecipientOption>
  communityPickerConfig!: MultiSelectConfig<RecipientOption>
  stage2Config!: MultiSelectConfig<RecipientOption>
  showStage2 = false
  stage2Loading = false

  constructor(
    private readonly modalInstance: NgbActiveModal,
    protected readonly dataService: DataService,
    private readonly messageService: MessageService,
    private readonly organisationService: OrganisationService,
    private readonly communityService: CommunityService,
    private readonly popupService: PopupService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly messageComposeService: MessageComposeService
  ) { super() }

  ngOnInit(): void {
    this.isReply = this.draft.parentMessageId != undefined
    if (this.isReply) {
      // Already fully loaded (with collapsed:true and reply-adjacent ordering applied) before the modal
      // was opened - see MessagesComponent.startReply. No in-modal fetch or pending state needed.
      this.chain = this.draft.chain ?? []
    }
    this.isTestBedAdminRole = this.dataService.isSystemAdmin
    this.isCommunityAdminRole = this.dataService.isCommunityAdmin
    this.isOrganisationUser = !this.isTestBedAdminRole && !this.isCommunityAdminRole
    if (this.isOrganisationUser) {
      this.setupOrganisationUserPicker()
    } else if (this.isCommunityAdminRole) {
      this.setupCommunityAdminPicker()
    } else {
      this.setupTestBedAdminPicker()
    }
  }

  private setupOrganisationUserPicker() {
    const options: RecipientOption[] = [
      { id: -1, fname: 'Community administrator', targetType: Constants.MESSAGE_TARGET_TYPE.COMMUNITY_ADMIN },
      { id: -2, fname: 'My '+this.dataService.labelOrganisationLower(), targetType: Constants.MESSAGE_TARGET_TYPE.OWN_ORGANISATION }
    ]
    this.recipientConfig = {
      name: 'recipients',
      textField: 'fname',
      showAsFormControl: true,
      countLabel: 'recipients',
      filterLabel: 'Select recipients...',
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'No recipients available.',
      searchPlaceholder: 'Search...',
      replaceItems: new EventEmitter(),
      replaceSelectedItems: new EventEmitter()
    }
    // Deferred to the next tick (matching ConformanceStatementsComponent.systemsLoaded()) - applying the
    // default selection synchronously during ngOnInit (e.g. via config.initialValues, which resolves
    // immediately for a synchronous loader) changes a template expression this same component reads
    // (canSend()) within the same change-detection pass, tripping Angular's dev-mode
    // ExpressionChangedAfterItHasBeenCheckedError.
    const initial = this.draft.recipientDisplay.length > 0 ? this.draft.recipientDisplay : [options[0]]
    setTimeout(() => {
      this.recipientConfig.replaceItems!.emit(options)
      this.recipientConfig.replaceSelectedItems!.emit(initial)
    })
  }

  private setupCommunityAdminPicker() {
    const communityId = this.dataService.vendor!.community
    const organisationsLabel = this.dataService.labelOrganisationsLower()
    this.recipientConfig = {
      name: 'recipients',
      textField: 'fname',
      showAsFormControl: true,
      countLabel: 'recipients',
      filterLabel: 'Select recipients...',
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'No recipients available.',
      searchPlaceholder: 'Search recipients...',
      loader: () => this.organisationService.getOrganisationsByCommunity(communityId).pipe(
        map(orgs => {
          const groupOptions: RecipientOption[] = [
            { id: -1, fname: 'Test Bed administrator', targetType: Constants.MESSAGE_TARGET_TYPE.TESTBED_ADMIN },
            { id: -2, fname: 'Community administrator', targetType: Constants.MESSAGE_TARGET_TYPE.COMMUNITY_ADMIN },
            { id: -3, fname: 'All '+organisationsLabel, targetType: Constants.MESSAGE_TARGET_TYPE.ALL_COMMUNITY_MEMBERS }
          ]
          const orgOptions: RecipientOption[] = orgs
            .filter(o => !o.adminOrganization)
            .sort((a, b) => a.fname.localeCompare(b.fname))
            .map(o => ({ id: o.id, fname: o.fname, targetType: Constants.MESSAGE_TARGET_TYPE.ORGANISATION, organisationId: o.id }))
          return groupOptions.concat(orgOptions)
        })
      ),
      placeholderItemIds: new Set([-3]),
      initialValues: this.draft.recipientDisplay
    }
  }

  private setupTestBedAdminPicker() {
    const organisationsLabel = this.dataService.labelOrganisationsLower()
    const allOptions: RecipientOption[] = [
      { id: -1, fname: 'Test Bed administrator', targetType: Constants.MESSAGE_TARGET_TYPE.TESTBED_ADMIN },
      { id: -2, fname: 'All community administrators', targetType: Constants.MESSAGE_TARGET_TYPE.ALL_COMMUNITY_ADMINS },
      { id: -3, fname: 'All '+organisationsLabel, targetType: Constants.MESSAGE_TARGET_TYPE.ALL_ORGANISATIONS },
      { id: -4, fname: 'All users', targetType: Constants.MESSAGE_TARGET_TYPE.ALL_USERS }
    ]
    this.communityPickerConfig = {
      name: 'communityPicker',
      textField: 'fname',
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: 'Select recipients...',
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'No recipients available.',
      searchPlaceholder: 'Search recipients...',
      loader: () => this.communityService.getCommunities(undefined, true).pipe(
        map(communities => {
          const communityOptions: RecipientOption[] = communities
            .slice()
            .sort((a, b) => a.fname.localeCompare(b.fname))
            .map(c => ({ id: c.id, fname: c.fname }))
          return allOptions.concat(communityOptions)
        })
      ),
      placeholderItemIds: new Set([-4]),
      // When set, MultiSelectFilterComponent applies this synchronously in its own ngOnInit and emits
      // (apply) once as an echo - handled by communityPickerChanged's isRestoreEcho branch below, which is
      // what actually (re)establishes showStage2/stage2Config for a restored minimised draft or a reply
      // pre-seeded with a resolved two-stage default (see MessagesComponent.buildReplySeed). No separate
      // handling is needed here - a second, independent attempt to do so raced with that echo and either
      // clobbered or duplicated it.
      initialValues: this.draft.adminCommunitySelection != undefined ? [this.draft.adminCommunitySelection] : undefined
    }
  }

  private loadStage2Picker(communityId: number, initial: RecipientOption[]) {
    const organisationsLabel = this.dataService.labelOrganisationsLower()
    this.stage2Config = {
      name: 'stage2',
      textField: 'fname',
      showAsFormControl: true,
      countLabel: 'recipients',
      filterLabel: 'Select recipients...',
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'No recipients available.',
      searchPlaceholder: 'Search recipients...',
      replaceItems: new EventEmitter(),
      replaceSelectedItems: new EventEmitter(),
      placeholderItemIds: new Set([-2])
    }
    this.stage2Loading = true
    this.organisationService.getOrganisationsByCommunity(communityId).pipe(
      map(orgs => {
        const groupOptions: RecipientOption[] = [
          { id: -1, fname: 'Community administrator', targetType: Constants.MESSAGE_TARGET_TYPE.COMMUNITY_ADMIN, communityId: communityId },
          { id: -2, fname: 'All community '+organisationsLabel, targetType: Constants.MESSAGE_TARGET_TYPE.ALL_COMMUNITY_MEMBERS, communityId: communityId }
        ]
        const orgOptions: RecipientOption[] = orgs
          .filter(o => !o.adminOrganization)
          .sort((a, b) => a.fname.localeCompare(b.fname))
          .map(o => ({ id: o.id, fname: o.fname, targetType: Constants.MESSAGE_TARGET_TYPE.ORGANISATION, communityId: communityId, organisationId: o.id }))
        return groupOptions.concat(orgOptions)
      })
    ).subscribe((options) => {
      setTimeout(() => {
        this.stage2Config.replaceItems!.emit(options)
        if (initial.length > 0) {
          this.stage2Config.replaceSelectedItems!.emit(initial)
        }
      })
    }).add(() => {
      this.stage2Loading = false
    })
  }

  recipientsChanged(event: FilterUpdate<RecipientOption>) {
    this.draft.recipientDisplay = event.values.active
    this.draft.recipients = event.values.active.map(o => ({ targetType: o.targetType!, organisationId: o.organisationId }))
  }

  communityPickerChanged(event: FilterUpdate<RecipientOption>) {
    const selected = event.values.active.length > 0 ? event.values.active[0] : undefined
    const isRestoreEcho = selected != undefined && selected.targetType == undefined && this.draft.adminCommunitySelection?.id === selected.id
    const previousRecipientDisplay = this.draft.recipientDisplay
    const apply = () => {
      this.draft.adminCommunitySelection = selected
      this.draft.recipientDisplay = []
      if (selected == undefined) {
        this.showStage2 = false
        this.stage2Loading = false
        this.draft.recipients = []
      } else if (selected.targetType != undefined) {
        this.showStage2 = false
        this.stage2Loading = false
        this.draft.recipients = [{ targetType: selected.targetType }]
      } else {
        this.showStage2 = true
        this.draft.recipients = []
        this.loadStage2Picker(selected.id, isRestoreEcho ? previousRecipientDisplay : [])
      }
    }
    if (isRestoreEcho) { setTimeout(apply) } else { apply() }
  }

  stage2Changed(event: FilterUpdate<RecipientOption>) {
    this.draft.recipientDisplay = event.values.active
    this.draft.recipients = event.values.active.map(o => ({ targetType: o.targetType!, communityId: o.communityId, organisationId: o.organisationId }))
  }

  canSend(): boolean {
    if (this.sendPending) return false
    return this.draft.recipients.length > 0
  }

  sendMessage() {
    if (!this.canSend()) return
    const hasSubject = this.textProvided(this.draft.subject)
    const hasBody = this.visibleHtmlProvided(this.draft.body)
    if (!hasSubject || !hasBody) {
      let missing: string
      if (!hasSubject && !hasBody) {
        missing = 'a subject or any content'
      } else if (!hasSubject) {
        missing = 'a subject'
      } else {
        missing = 'any content'
      }
      this.confirmationDialogService.confirmed('Send message', `This message doesn't have ${missing}. Send it anyway?`, 'Send', 'Cancel', Constants.BUTTON_ICON.SEND).subscribe(() => {
        this.doSend()
      })
    } else {
      this.doSend()
    }
  }

  private doSend() {
    this.sendPending = true
    const subjectToSave = this.trimString(this.draft.subject)
    this.messageService.createMessage(subjectToSave, this.draft.body, this.draft.important, this.draft.recipients, this.draft.parentMessageId).subscribe(() => {
      this.popupService.success('Message sent.')
      this.sent.emit()
      this.modalInstance.close()
    }).add(() => {
      this.sendPending = false
    })
  }

  minimise() {
    this.minimiseRequested.emit()
    this.modalInstance.dismiss()
  }

  cancel() {
    if (this.messageComposeService.hasNonDefaultState()) {
      this.confirmationDialogService.confirmedDangerous('Discard draft message', 'Are you sure you want to discard the current draft?', 'Discard', 'Cancel', Constants.BUTTON_ICON.DELETE, Constants.BUTTON_ICON.CANCEL).subscribe(() => {
        this.cancelled.emit()
        this.modalInstance.dismiss()
      })
    } else {
      this.cancelled.emit()
      this.modalInstance.dismiss()
    }
  }

}
