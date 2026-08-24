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

import {Component, Input, OnInit} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {DataService} from 'src/app/services/data.service';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {PopupService} from 'src/app/services/popup.service';
import {TestFlagService} from 'src/app/services/test-flag.service';
import {Constants} from 'src/app/common/constants';
import {TestFlag} from 'src/app/types/test-flag';
import {ValidationState} from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-edit-test-flag-modal',
    templateUrl: './create-edit-test-flag-modal.component.html',
    standalone: false
})
export class CreateEditTestFlagModalComponent extends BaseComponent implements OnInit {

  @Input() testFlag!: Partial<TestFlag>
  @Input() communityId!: number

  testFlagToUse!: Partial<TestFlag>
  /** Unchecked = the flag has its own public name/colour overrides (shown as separate fields). */
  samePresentationForUsers = true
  title!: string
  pending = false
  savePending = false
  deletePending = false
  validation = new ValidationState()

  protected readonly Constants = Constants

  constructor(
    private readonly modalInstance: NgbActiveModal,
    private readonly testFlagService: TestFlagService,
    private readonly popupService: PopupService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    public readonly dataService: DataService
  ) { super() }

  ngOnInit(): void {
    this.testFlagToUse = structuredClone(this.testFlag)
    if (this.testFlagToUse.colour == undefined) {
      this.testFlagToUse.colour = '#337ab7'
    }
    this.samePresentationForUsers = !this.textProvided(this.testFlagToUse.publicName) && !this.textProvided(this.testFlagToUse.publicColour)
    this.title = this.testFlagToUse.id != undefined ? 'Update flag' : 'Create flag'
  }

  toggleSamePresentationForUsers() {
    this.samePresentationForUsers = !this.samePresentationForUsers
    if (!this.samePresentationForUsers) {
      // Defaults for the override fields, once revealed, are the main name/colour.
      if (!this.textProvided(this.testFlagToUse.publicName)) {
        this.testFlagToUse.publicName = this.testFlagToUse.name
      }
      if (!this.textProvided(this.testFlagToUse.publicColour)) {
        this.testFlagToUse.publicColour = this.testFlagToUse.colour
      }
    }
  }

  saveDisabled() {
    return this.pending || !this.textProvided(this.testFlagToUse.name) || !this.textProvided(this.testFlagToUse.colour) || (!this.samePresentationForUsers && (!this.textProvided(this.testFlagToUse.publicName) || !this.textProvided(this.testFlagToUse.publicColour)))
  }

  save() {
    if (!this.saveDisabled()) {
      this.validation.clearErrors()
      const publicName = this.samePresentationForUsers ? undefined : this.testFlagToUse.publicName
      const publicColour = this.samePresentationForUsers ? undefined : this.testFlagToUse.publicColour
      this.pending = true
      this.savePending = true
      const call = this.testFlagToUse.id != undefined ?
        this.testFlagService.updateTestFlag(this.testFlagToUse.id, this.testFlagToUse.name!, this.testFlagToUse.description, this.testFlagToUse.colour!, publicName, publicColour, this.testFlagToUse.adminOnly ?? false, this.communityId) :
        this.testFlagService.createTestFlag(this.testFlagToUse.name!, this.testFlagToUse.description, this.testFlagToUse.colour!, publicName, publicColour, this.testFlagToUse.adminOnly ?? false, this.communityId)
      call.subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.validation.applyError(data)
        } else {
          this.modalInstance.close()
          this.popupService.success(this.testFlagToUse.id != undefined ? 'Flag updated.' : 'Flag created.')
        }
      }).add(() => {
        this.pending = false
        this.savePending = false
      })
    }
  }

  /** Pre-fills a new create form from this flag's current values - lets an admin quickly define an
   * admin-only variant of a flag (e.g. several admin flags collapsing to the same public presentation)
   * without needing any "flag family" concept in the data model. */
  copy() {
    delete this.testFlagToUse.id
    this.title = 'Create flag'
  }

  delete() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this flag? Doing so will also remove it from associated test sessions.", "Delete", "Cancel", Constants.BUTTON_ICON.DELETE)
    .subscribe(() => {
      this.pending = true
      this.deletePending = true
      this.testFlagService.deleteTestFlag(this.testFlagToUse.id!)
      .subscribe(() => {
        this.modalInstance.close()
        this.popupService.success('Flag deleted.')
      }).add(() => {
        this.pending = false
        this.deletePending = false
      })
    })
  }

  cancel() {
    this.modalInstance.dismiss()
  }

}
