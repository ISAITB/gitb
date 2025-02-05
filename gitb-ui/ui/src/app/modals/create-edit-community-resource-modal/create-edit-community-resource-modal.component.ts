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
    styles: [],
    standalone: false
})
export class CreateEditCommunityResourceModalComponent extends BaseComponent implements OnInit {

  @Input() resource?: CommunityResource
  @Input() communityId!: number
  @Output() resourceUpdated = new EventEmitter<boolean>()

  resourceToUse!: Partial<CommunityResource>
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
    this.resourceToUse = {}
    if (this.resource != undefined) {
      this.resourceToUse.id = this.resource.id
      this.resourceToUse.name = this.resource.name
      this.resourceToUse.description = this.resource.description
    }
    if (this.resourceToUse.id == undefined) {
      this.title = "Upload community resource"
    } else {
      this.title = "Update community resource"
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
        this.communityService.createCommunityResource(this.resourceToUse.name!, this.resourceToUse.description, this.file!, this.communityId)
        .subscribe(() => {
          this.resourceUpdated.emit(true)
          this.modalInstance.hide()
          this.popupService.success("Resource added.")
        }).add(() => {
          this.savePending = false
        })
      } else {
        // Update.
        this.communityService.updateCommunityResource(this.resourceToUse.id!, this.resourceToUse.name!, this.resourceToUse.description, this.file)
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
      this.communityService.downloadCommunityResourceById(this.resourceToUse.id)
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
        this.communityService.deleteCommunityResource(this.resourceToUse.id!).subscribe(() => {
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
