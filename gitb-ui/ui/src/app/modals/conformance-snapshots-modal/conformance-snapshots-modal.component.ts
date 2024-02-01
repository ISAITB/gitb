import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { filter } from 'lodash';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Observable } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { ConformanceSnapshot } from 'src/app/types/conformance-snapshot';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';

@Component({
  selector: 'app-conformance-snapshots-modal',
  templateUrl: './conformance-snapshots-modal.component.html',
  styleUrls: [ './conformance-snapshots-modal.component.less' ]
})
export class ConformanceSnapshotsModalComponent extends BaseComponent implements OnInit {

  @Input() communityId!: number
  @Input() currentlySelectedSnapshot?: number
  @Output() select = new EventEmitter<ConformanceSnapshot|undefined>()

  snapshots?: ConformanceSnapshot[]
  visibleSnapshots?: ConformanceSnapshot[]
  snapshotColumns: TableColumnDefinition[] = [
    { field: 'labelToDisplay', title: 'Label' },
    { field: 'snapshotTime', title: 'Snapshot time', headerClass: 'th-min padding-right-large', order:'desc' },
    { field: 'hidden', title: '', atEnd: false, isHiddenFlag: true, headerClass: 'th-min-centered' }
  ]
  snapshotsStatus = {status: Constants.STATUS.NONE}
  snapshotToEdit?: Partial<ConformanceSnapshot>
  savePending = false
  editMode = false
  publicView = false
  snapshotFilter?: string
  refreshSnapshots = new EventEmitter<void>()

  constructor(
    private modalInstance: BsModalRef,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private confirmationDialogService: ConfirmationDialogService,
    private dataService: DataService
  ) { super() }

  ngOnInit(): void {
    this.loadSnapshots()
  }

  private loadSnapshots() {
    this.snapshotFilter = undefined
    this.snapshots = []
    this.visibleSnapshots = []
    this.snapshotsStatus.status = Constants.STATUS.PENDING
    this.conformanceService.getConformanceSnapshots(this.communityId)
    .subscribe((data) => {
      this.snapshots = data.snapshots
      this.snapshots.unshift({
        id: -1,
        label: Constants.LATEST_CONFORMANCE_STATUS_LABEL,
        publicLabel: data.latest,
        hidden: false,
        snapshotTime: '-',
        latest: true
      })
      this.togglePublicView()
      this.visibleSnapshots = this.snapshots
    }).add(() => {
      this.snapshotsStatus.status = Constants.STATUS.FINISHED
    })
  }

  selectSnapshot(snapshot: ConformanceSnapshot) {
    if (snapshot.latest) {
      this.select.emit()
    } else {
      this.select.emit(snapshot)
    }
    this.close()
  }

  editSnapshot(snapshot: Partial<ConformanceSnapshot>) {
    this.snapshotToEdit = {
      id: snapshot.id,
      label: snapshot.label,
      publicLabel: snapshot.publicLabel,
      snapshotTime: snapshot.snapshotTime,
      hidden: snapshot.hidden,
      sameLabel: snapshot.hidden || snapshot.publicLabel == undefined,
      latest: snapshot.latest
    }
    this.editMode = true
    if (!snapshot.latest) {
      this.dataService.focus("label")
    } else if (!snapshot.sameLabel) {
      this.dataService.focus("publicLabel")
    }
  }

  createSnapshot() {
    this.editSnapshot({
      hidden: true
    })
  }

  deleteSnapshot(snapshot: ConformanceSnapshot) {
    this.confirmationDialogService.confirmedDangerous("Delete snapshot", "Are you sure you want to delete this conformance snapshot?", "Delete", "Cancel")
    .subscribe(() => {
      snapshot.deletePending = true
      this.conformanceService.deleteConformanceSnapshot(snapshot.id)
      .subscribe(() => {
        if (this.currentlySelectedSnapshot != undefined && this.currentlySelectedSnapshot == snapshot.id) {
          this.select.emit()
        }
        this.popupService.success("Snapshot deleted.")
        this.loadSnapshots()
      }).add(() => {
        snapshot.deletePending = true
      })
    })
  }

  saveDisabled() {
    return !(
      this.textProvided(this.snapshotToEdit?.label) &&
      (this.snapshotToEdit?.hidden || this.snapshotToEdit?.sameLabel || this.textProvided(this.snapshotToEdit?.publicLabel))
    )
  }

  saveSnapshot() {
    if (!this.saveDisabled()) {
      let action: Observable<any>
      let successMessage: string
      if (this.snapshotToEdit?.id != undefined) {
        // Edit case.
        if (this.snapshotToEdit.latest) {
          // Latest status.
          let labelToSet: string|undefined
          if (!this.snapshotToEdit.sameLabel) {
            labelToSet = this.snapshotToEdit.publicLabel
          }
          action = this.conformanceService.setLatestConformanceStatusLabel(this.communityId, labelToSet)
          successMessage = 'Label updated.'
        } else {
          // Snapshot.
          action = this.conformanceService.editConformanceSnapshot(this.snapshotToEdit!.id!, this.snapshotToEdit!.label!, this.snapshotToEdit.publicLabel, !this.snapshotToEdit!.hidden!)
          successMessage = 'Snapshot updated.'
        }
      } else {
        // Create case.
        action = this.conformanceService.createConformanceSnapshot(this.communityId, this.snapshotToEdit!.label!, this.snapshotToEdit!.publicLabel, !this.snapshotToEdit!.hidden!)
        successMessage = 'Snapshot created.'
      }
      this.savePending = true
      action.subscribe(() => {
        if (this.currentlySelectedSnapshot != undefined 
            && this.snapshotToEdit?.id != undefined 
            && this.currentlySelectedSnapshot == this.snapshotToEdit?.id!) {
          this.select.emit(this.snapshotToEdit as ConformanceSnapshot)
        }
        this.popupService.success(successMessage)
        this.editMode = false
      }).add(() => {
        this.savePending = false
        this.loadSnapshots()
      })
    }
  }

  cancel() {
    this.editMode = false
  }

  close() {
    this.modalInstance.hide()
  }

  applySnapshotFilter() {
    if (this.snapshotFilter == undefined) {
      this.visibleSnapshots = this.snapshots
    } else {
      const filterToApply = this.snapshotFilter.toLowerCase()
      this.visibleSnapshots = filter(this.snapshots, (snapshot) => {
        return snapshot.label.toLowerCase().includes(filterToApply) || (!snapshot.hidden && snapshot.publicLabel != undefined && snapshot.publicLabel.toLowerCase().includes(filterToApply))
      })
    }
  }

  deleteVisibleForSnapshot(snapshot: ConformanceSnapshot) {
    return !snapshot.latest
  }

  togglePublicView() {
    if (this.snapshots) {
      for (let snapshot of this.snapshots) {
        snapshot.labelToDisplay = snapshot.label
        if (this.publicView && !snapshot.hidden && snapshot.publicLabel != undefined) {
          snapshot.labelToDisplay = snapshot.publicLabel
        }
      }
      this.refreshSnapshots.emit()
    }
  }

}
