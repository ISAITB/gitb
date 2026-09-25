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

import { Component, EventEmitter, Input, OnInit, Output, ChangeDetectionStrategy } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { PopupService } from 'src/app/services/popup.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { Constants } from 'src/app/common/constants';

@Component({
    selector: 'app-configuration-documentation-modal',
    templateUrl: './configuration-documentation-modal.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ConfigurationDocumentationModalComponent extends BaseComponent implements OnInit {

  @Input() documentation?: string
  @Input() saveFn!: (documentation: string) => Observable<any>
  @Input() deleteFn!: () => Observable<any>
  @Output() documentationUpdate = new EventEmitter<string|undefined>()

  protected content?: string
  protected savePending = false
  protected deletePending = false
  protected readonly Constants = Constants

  constructor(
    private readonly modalInstance: NgbActiveModal,
    private readonly popupService: PopupService,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) { super() }

  ngOnInit(): void {
    this.content = this.documentation
  }

  protected saveEnabled() {
    return !this.deletePending && this.visibleHtmlProvided(this.content)
  }

  protected hasExistingDocumentation() {
    return this.textProvided(this.documentation)
  }

  protected cancel() {
    this.modalInstance.dismiss()
  }

  protected save() {
    if (this.saveEnabled()) {
      this.savePending = true
      this.saveFn(this.content!).subscribe(() => {
        this.popupService.success('Documentation saved.')
        this.documentationUpdate.emit(this.content)
        this.modalInstance.close()
      }).add(() => {
        this.savePending = false
      })
    }
  }

  protected delete() {
    this.confirmationDialogService.confirmedDangerous('Delete documentation', 'Are you sure you want to delete the documentation?', 'Delete', 'Cancel', Constants.BUTTON_ICON.DELETE).subscribe(() => {
      this.deletePending = true
      this.deleteFn().subscribe(() => {
        this.popupService.success('Documentation deleted.')
        this.documentationUpdate.emit(undefined)
        this.modalInstance.close()
      }).add(() => {
        this.deletePending = false
      })
    })
  }

}
