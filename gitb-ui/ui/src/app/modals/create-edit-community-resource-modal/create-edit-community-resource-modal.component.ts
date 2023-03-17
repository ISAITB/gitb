import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { CommunityService } from 'src/app/services/community.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { PopupService } from 'src/app/services/popup.service';
import { CommunityResource } from 'src/app/types/community-resource';
import { FileData } from 'src/app/types/file-data.type';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-create-edit-community-resource-modal',
  templateUrl: './create-edit-community-resource-modal.component.html',
  styles: [
  ]
})
export class CreateEditCommunityResourceModalComponent extends BaseComponent implements OnInit {

  @Input() resource: Partial<CommunityResource> = {}
  @Input() communityId!: number
  @Output() resourceUpdated = new EventEmitter<boolean>()
  title!: string
  savePending = false
  downloadPending = false
  deletePending = false
  file?: FileData

  constructor(
    private modalInstance: BsModalRef,
    private communityService: CommunityService,
    private popupService: PopupService,
    private confirmationDialogService: ConfirmationDialogService
  ) { 
    super()
  }

  ngOnInit(): void {
    if (this.resource == undefined) {
      this.resource = {}
    }
    if (this.resource.id == undefined) {
      this.title = "Upload community resource"
    } else {
      this.title = "Update community resource"
    }
  }

  selectFile(file: FileData) {
    this.file = file
    this.resource.name = file.name
  }

  saveAllowed() {
    return this.textProvided(this.resource?.name)
  }

  save() {
    if (this.saveAllowed()) {
      this.savePending = true
      if (this.resource.id == undefined) {
        // Create.
        this.communityService.createCommunityResource(this.resource.name!, this.resource.description, this.file!, this.communityId)
        .subscribe(() => {
          this.resourceUpdated.emit(true)
          this.modalInstance.hide()
          this.popupService.success("Resource added.")
        }).add(() => {
          this.savePending = false
        })
      } else {
        // Update.
        this.communityService.updateCommunityResource(this.resource.id!, this.resource.name!, this.resource.description, this.file)
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
      saveAs(this.file.file!, this.resource.name)
      this.downloadPending = false
    } else if (this.resource.id != undefined) {
      this.downloadPending = true
      this.communityService.downloadCommunityResourceById(this.resource.id)
      .subscribe((response) => {
        const bb = new Blob([response.body as ArrayBuffer])
        saveAs(bb, this.resource.name)
      }).add(() => {
        this.downloadPending = false
      })
    }
  }

  delete() {
    if (this.resource.id != undefined) {
      this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this resource?", "Yes", "No").subscribe(() => {
        this.deletePending = true
        this.communityService.deleteCommunityResource(this.resource.id!).subscribe(() => {
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
