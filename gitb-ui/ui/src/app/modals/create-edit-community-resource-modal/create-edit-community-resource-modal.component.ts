/*
 * Copyright (C) 2025 European Union
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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {PopupService} from 'src/app/services/popup.service';
import {CommunityResource} from 'src/app/types/community-resource';
import {FileData} from 'src/app/types/file-data.type';
import {saveAs} from 'file-saver';
import {ResourceActions} from '../../components/resource-management-tab/resource-actions';

@Component({
    selector: 'app-create-edit-community-resource-modal',
    templateUrl: './create-edit-community-resource-modal.component.html',
    styles: [],
    standalone: false
})
export class CreateEditCommunityResourceModalComponent extends BaseComponent implements OnInit {

  @Input() resource?: CommunityResource
  @Input() actions!: ResourceActions
  @Output() resourceUpdated = new EventEmitter<boolean>()

  resourceToUse!: Partial<CommunityResource>
  title!: string
  savePending = false
  downloadPending = false
  deletePending = false
  file?: FileData

  constructor(
    private readonly modalInstance: BsModalRef,
    private readonly popupService: PopupService,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) {
    super()
  }

  ngOnInit(): void {
    this.resourceToUse = {}
    if (this.resource != undefined) {
      this.resourceToUse.id = this.resource.id
      this.resourceToUse.name = this.resource.name
      this.resourceToUse.description = this.resource.description
    }
    if (this.resourceToUse.id == undefined) {
      this.title = `Upload ${this.actions.systemScope?'system':'community'} resource`
    } else {
      this.title = `Update ${this.actions.systemScope?'system':'community'} resource`
    }
  }

  selectFile(file: FileData) {
    this.file = file
    this.resourceToUse.name = file.name
  }

  saveAllowed() {
    return this.textProvided(this.resourceToUse?.name)
  }

  save() {
    if (this.saveAllowed()) {
      this.savePending = true
      if (this.resourceToUse.id == undefined) {
        // Create.
        this.actions.createResource(this.resourceToUse.name!, this.resourceToUse.description, this.file!)
        .subscribe(() => {
          this.resourceUpdated.emit(true)
          this.modalInstance.hide()
          this.popupService.success("Resource added.")
        }).add(() => {
          this.savePending = false
        })
      } else {
        // Update.
        this.actions.updateResource(this.resourceToUse.id!, this.resourceToUse.name!, this.resourceToUse.description, this.file)
        .subscribe(() => {
          this.resourceUpdated.emit(true)
          this.modalInstance.hide()
          this.popupService.success("Resource updated.")
        }).add(() => {
          this.savePending = false
        })
      }
    }
  }

  download() {
    if (this.file?.file) {
      this.downloadPending = true
      saveAs(this.file.file!, this.resourceToUse.name)
      this.downloadPending = false
    } else if (this.resourceToUse.id != undefined) {
      this.downloadPending = true
      this.actions.downloadResource(this.resourceToUse.id)
      .subscribe((response) => {
        const bb = new Blob([response.body as ArrayBuffer])
        saveAs(bb, this.resourceToUse.name)
      }).add(() => {
        this.downloadPending = false
      })
    }
  }

  delete() {
    if (this.resourceToUse.id != undefined) {
      this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this resource?", "Delete", "Cancel").subscribe(() => {
        this.deletePending = true
        this.actions.deleteResource(this.resourceToUse.id!).subscribe(() => {
          this.resourceUpdated.emit(true)
          this.modalInstance.hide()
          this.popupService.success("Resource deleted.")
        }).add(() => {
          this.deletePending = false
        })
      })
    }
  }

  cancel() {
    this.modalInstance.hide()
  }
}
