import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { CommunityService } from 'src/app/services/community.service';
import { PopupService } from 'src/app/services/popup.service';
import { FileData } from 'src/app/types/file-data.type';
import {ResourceActions} from '../../components/resource-management-tab/resource-actions';

@Component({
    selector: 'app-community-resource-bulk-upload-modal',
    templateUrl: './community-resource-bulk-upload-modal.component.html',
    styles: [],
    standalone: false
})
export class CommunityResourceBulkUploadModalComponent implements OnInit {

  @Input() actions!: ResourceActions
  @Output() resourcesUpdated = new EventEmitter<boolean>()

  title!: string
  uploadPending = false
  updateMatching = true
  file?: FileData

  constructor(
    private modalInstance: BsModalRef,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
    this.title = `${this.actions.systemScope?'System':'Community'} resource bulk upload`
  }

  uploadEnabled() {
    return this.file != undefined && this.updateMatching != undefined
  }

  selectFile(file: FileData) {
    this.file = file
  }

  upload() {
    if (this.uploadEnabled()) {
      this.uploadPending = true
      this.actions.uploadBulk(this.file!, this.updateMatching)
      .subscribe((result) => {
        if (result.created == 0 && result.updated == 0) {
          this.popupService.warning("No resources were added or updated as part of this upload.")
        } else {
          this.popupService.success("Resources uploaded ("+result.created+" new, "+result.updated+" updated).")
          this.resourcesUpdated.emit(true)
        }
        this.modalInstance.hide()
      }).add(() => {
        this.uploadPending = false
      })
    }
  }

  cancel() {
    this.modalInstance.hide()
  }

}
