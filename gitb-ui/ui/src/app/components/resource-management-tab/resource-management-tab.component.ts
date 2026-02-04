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

import {AfterViewInit, Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {Constants} from '../../common/constants';
import {ResourceActions} from './resource-actions';
import {CommunityResource} from '../../types/community-resource';
import {
  CommunityResourceBulkUploadModalComponent
} from '../../modals/community-resource-bulk-upload-modal/community-resource-bulk-upload-modal.component';
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
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ResourceState} from './resource-state';

@Component({
  selector: 'app-resource-management-tab',
  standalone: false,
  templateUrl: './resource-management-tab.component.html',
  styleUrl: './resource-management-tab.component.less'
})
export class ResourceManagementTabComponent implements AfterViewInit {

  @Input() state!: ResourceState
  @Input() actions!: ResourceActions
  @Output() pageSizeChanged = new EventEmitter<void>()
  @ViewChild("resourcesTable") resourcesTable?: TableComponent

  downloadAllResourcesPending = false
  deleteResourcesPending = false
  selectingForDeleteResources = false
  resourcesRefreshing = false
  clearResourceSelections = new EventEmitter<void>()

  resourceColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'reference', title: 'Reference to use' },
    { field: 'description', title: 'Description' }
  ]

  constructor(
    private readonly modalService: NgbModal,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly popupService: PopupService,
    private readonly dataService: DataService
  ) {
  }

  ngAfterViewInit(): void {
    if (this.state.status == Constants.STATUS.NONE) {
      this.refreshResources()
    } else {
      this.updateResourcesPagination(this.state.page, this.state.total)
    }
  }

  applyResourceFilter() {
    this.refreshResources()
  }

  queryResources(pagingInfo: PagingEvent) {
    if (this.state.status == Constants.STATUS.FINISHED) {
      this.resourcesRefreshing = true
    } else {
      this.state.status = Constants.STATUS.PENDING
    }
    this.clearResourceSelections.emit()
    this.actions.searchResources(this.state.filter, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.state.resources = data.data
        this.updateResourcesPagination(pagingInfo.targetPage, data.count!)
      }).add(() => {
      this.resourcesRefreshing = false
      this.state.status = Constants.STATUS.FINISHED
    })
  }

  private updateResourcesPagination(page: number, count: number) {
    this.resourcesTable?.pagingControls?.updateStatus(page, count)
    this.state.page = page
    this.state.total = count
  }

  uploadResource() {
    this.openResourceModal()
  }

  uploadResourceBulk() {
    const modal = this.modalService.open(CommunityResourceBulkUploadModalComponent, { modalDialogClass: "modal-lg" })
    const modalInstance = modal.componentInstance as CommunityResourceBulkUploadModalComponent
    modalInstance.actions = this.actions
    modal.result.then((updateMade: boolean) => {
      if (updateMade) {
        this.refreshResources()
      }
    })
  }

  private openResourceModal(resourceToEdit?: CommunityResource) {
    const modal = this.modalService.open(CreateEditCommunityResourceModalComponent, { modalDialogClass: "modal-lg" })
    const modalInstance = modal.componentInstance as CreateEditCommunityResourceModalComponent
    modalInstance.actions = this.actions
    modalInstance.resource = resourceToEdit
    modal.closed.subscribe((updateMade: boolean) => {
      if (updateMade) {
        this.refreshResources()
      }
    })
    modal.hidden.subscribe(() => {
      this.clearResourceSelections.emit()
    })
  }

  downloadAllResources() {
    if (this.state.resources.length > 0) {
      this.downloadAllResourcesPending = true
      this.actions.downloadResources(this.state.filter)
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
    for (let resource of this.state.resources) {
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
    this.confirmationDialogService.confirmedDangerous("Confirm delete", msg, "Delete", "Cancel", Constants.BUTTON_ICON.DELETE).subscribe(() => {
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
    for (let resource of this.state.resources) {
      if (resource.checked != undefined) {
        resource.checked = false
      }
    }
  }

  resourcesChecked() {
    for (let resource of this.state.resources) {
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
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this resource?", "Delete", "Cancel", Constants.BUTTON_ICON.DELETE).subscribe(() => {
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
    if (event.pageSizeChanged) {
      this.pageSizeChanged.emit()
    }
  }

  refreshResources() {
    this.queryResources({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize})
  }

  protected readonly Constants = Constants;
}
