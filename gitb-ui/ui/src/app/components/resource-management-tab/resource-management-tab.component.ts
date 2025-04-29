import {AfterViewInit, Component, EventEmitter, Input, ViewChild} from '@angular/core';
import {Constants} from '../../common/constants';
import {ResourceActions} from './resource-actions';
import {CommunityResource} from '../../types/community-resource';
import {
  CommunityResourceBulkUploadModalComponent
} from '../../modals/community-resource-bulk-upload-modal/community-resource-bulk-upload-modal.component';
import {BsModalService} from 'ngx-bootstrap/modal';
import {
  CreateEditCommunityResourceModalComponent
} from '../../modals/create-edit-community-resource-modal/create-edit-community-resource-modal.component';
import {saveAs} from 'file-saver';
import {ConfirmationDialogService} from '../../services/confirmation-dialog.service';
import {PopupService} from '../../services/popup.service';
import {TableColumnDefinition} from '../../types/table-column-definition.type';
import {DataService} from '../../services/data.service';
import {TableComponent} from '../table/table.component';
import {PagingEvent} from '../paging-controls/paging-event';

@Component({
  selector: 'app-resource-management-tab',
  standalone: false,
  templateUrl: './resource-management-tab.component.html',
  styleUrl: './resource-management-tab.component.less'
})
export class ResourceManagementTabComponent implements AfterViewInit {

  @Input() actions!: ResourceActions
  @Input() deferredActivation?: EventEmitter<void>
  @ViewChild("resourcesTable") resourcesTable?: TableComponent

  downloadAllResourcesPending = false
  deleteResourcesPending = false
  selectingForDeleteResources = false
  resourcesRefreshing = false
  resourceFilter?: string
  resourcesStatus = {status: Constants.STATUS.NONE}
  clearResourceSelections = new EventEmitter<void>()
  resources: CommunityResource[] = []

  resourceColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'reference', title: 'Reference to use' },
    { field: 'description', title: 'Description' }
  ]

  constructor(
    private modalService: BsModalService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    private dataService: DataService
  ) {
  }

  ngAfterViewInit(): void {
    if (this.deferredActivation) {
      this.deferredActivation.subscribe(() => {
        if (this.resourcesStatus.status == Constants.STATUS.NONE) {
          this.refreshResources()
        }
      })
    } else {
      this.refreshResources()
    }
  }

  applyResourceFilter() {
    this.refreshResources()
  }

  queryResources(pagingInfo: PagingEvent) {
    if (this.resourcesStatus.status == Constants.STATUS.FINISHED) {
      this.resourcesRefreshing = true
    } else {
      this.resourcesStatus.status = Constants.STATUS.PENDING
    }
    this.clearResourceSelections.emit()
    this.actions.searchResources(this.resourceFilter, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.resources = data.data
        this.updateResourcesPagination(pagingInfo.targetPage, data.count!)
      }).add(() => {
      this.resourcesRefreshing = false
      this.resourcesStatus.status = Constants.STATUS.FINISHED
    })
  }

  private updateResourcesPagination(page: number, count: number) {
    this.resourcesTable?.pagingControls?.updateStatus(page, count)
  }

  uploadResource() {
    this.openResourceModal()
  }

  uploadResourceBulk() {
    const modal = this.modalService.show(CommunityResourceBulkUploadModalComponent, {
      class: 'modal-lg',
      initialState: {
        actions: this.actions
      }
    })
    modal.content!.resourcesUpdated.subscribe((updateMade) => {
      if (updateMade) {
        this.refreshResources()
      }
    })
  }

  private openResourceModal(resourceToEdit?: CommunityResource) {
    const modal = this.modalService.show(CreateEditCommunityResourceModalComponent, {
      class: 'modal-lg',
      initialState: {
        actions: this.actions,
        resource: resourceToEdit
      }
    })
    modal.content!.resourceUpdated.subscribe((updateMade) => {
      if (updateMade) {
        this.refreshResources()
      }
    })
    modal.onHide!.subscribe(() => {
      this.clearResourceSelections.emit()
    })
  }

  downloadAllResources() {
    if (this.resources.length > 0) {
      this.downloadAllResourcesPending = true
      this.actions.downloadResources(this.resourceFilter)
        .subscribe((data) => {
          const blobData = new Blob([data], {type: 'application/zip'})
          saveAs(blobData, 'resources.zip')
        }).add(() => {
        this.downloadAllResourcesPending = false
      })
    }
  }

  selectDeleteResources() {
    this.selectingForDeleteResources = true
  }

  confirmDeleteResources() {
    const resourceIds: number[] = []
    for (let resource of this.resources) {
      if (resource.checked != undefined && resource.checked) {
        resourceIds.push(resource.id)
      }
    }
    let msg: string
    if (resourceIds.length == 1) {
      msg = 'Are you sure you want to delete the selected resource?'
    } else {
      msg = 'Are you sure you want to delete the selected resources?'
    }
    this.confirmationDialogService.confirmedDangerous("Confirm delete", msg, "Delete", "Cancel").subscribe(() => {
      this.deleteResourcesPending = true
      this.resourcesRefreshing = true
      this.actions.deleteResources(resourceIds).subscribe(() => {
        this.popupService.success("Resources deleted.")
      }).add(() => {
        this.deleteResourcesPending = false
        this.selectingForDeleteResources = false
        this.refreshResources()
      })
    })
  }

  cancelDeleteResources() {
    this.clearResourceSelections.emit()
    this.selectingForDeleteResources = false
    for (let resource of this.resources) {
      if (resource.checked != undefined) {
        resource.checked = false
      }
    }
  }

  resourcesChecked() {
    for (let resource of this.resources) {
      if (resource.checked !== undefined && resource.checked) {
        return true
      }
    }
    return false
  }

  selectResource(resource: CommunityResource) {
    this.cancelDeleteResources()
    this.openResourceModal(resource)
  }

  copyResourceReference(resource: CommunityResource) {
    this.dataService.copyToClipboard(resource.reference).subscribe(() => {
      this.popupService.success('Reference copied to clipboard.')
    })
  }

  downloadResource(resource: CommunityResource) {
    resource.downloadPending = true
    this.actions.downloadResource(resource.id)
      .subscribe((response) => {
        let fileName = "file"
        const contentDisposition = response.headers.get('Content-Disposition')
        if (contentDisposition != null) {
          fileName = contentDisposition.split(';')[1].trim().split('=')[1].replace(/"/g, '')
        }
        const bb = new Blob([response.body as ArrayBuffer])
        saveAs(bb, fileName)
      }).add(() => {
      resource.downloadPending = false
    })
  }

  deleteResource(resource: CommunityResource) {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this resource?", "Delete", "Cancel").subscribe(() => {
      resource.deletePending = true
      this.actions.deleteResource(resource.id).subscribe(() => {
        this.popupService.success("Resource deleted.")
        this.refreshResources()
      }).add(() => {
        resource.deletePending = false
      })
    })
  }

  doPageNavigation(event: PagingEvent) {
    this.queryResources(event)
  }

  refreshResources() {
    this.queryResources({ targetPage: 1, targetPageSize: this.resourcesTable?.pagingControls?.getCurrentStatus().pageSize!})
  }

}
