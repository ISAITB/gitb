import { AfterViewInit, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { CommunityService } from 'src/app/services/community.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { SystemService } from 'src/app/services/system.service';
import { System } from 'src/app/types/system';
import { OptionalCustomPropertyFormData } from '../../components/optional-custom-property-form/optional-custom-property-form-data.type';

@Component({
  selector: 'app-create-edit-system-modal',
  templateUrl: './create-edit-system-modal.component.html',
  styles: [
  ]
})
export class CreateEditSystemModalComponent extends BaseComponent implements OnInit, AfterViewInit {

  @Input() system: Partial<System> = {}
  @Input() organisationId!: number
  @Input() communityId!: number
  @Input() viewProperties = false
  @Output() reload = new EventEmitter<boolean>()
  savePending = false
  deletePending = false
  otherSystems: System[] = []
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'system'
  }
  title!: string
  otherSystem?: number
  copySystemParameters = false
  copyStatementParameters = false

  constructor(
    public dataService: DataService,
    private modalRef: BsModalRef,
    private confirmationDialogService: ConfirmationDialogService,
    private systemService: SystemService,
    private communityService: CommunityService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('sname')
  }

  ngOnInit(): void {
    this.propertyData.edit = this.viewProperties
		if (this.system.id != undefined) {
			this.systemService.getSystemParameterValues(this.system.id)
      .subscribe((data) => {
        this.propertyData.properties = data
      })
    } else {
			this.communityService.getSystemParameters(this.communityId)
      .subscribe((data) => {
        this.propertyData.properties = data
      })
    }
		this.systemService.getSystemsByOrganisation(this.organisationId)
    .subscribe((data) => {
      if (this.system.id != undefined) {
        for (let system of data) {
          if (Number(system.id) != Number(this.system.id)) {
            this.otherSystems.push(system)
          }
        }
      } else {
        this.otherSystems = data
      }
    })
		if (this.system.id != undefined) {
			this.title = 'Update ' + this.dataService.labelSystemLower()
    } else {
			this.title = 'Create ' + this.dataService.labelSystemLower()
    }
  }

  saveEnabled() {
    return this.textProvided(this.system.sname) && this.textProvided(this.system.fname) && this.textProvided(this.system.version)
  }

  copyChanged() {
    if (this.otherSystem == undefined) {
      this.copySystemParameters = false
      this.copyStatementParameters = false
    } else if (this.copySystemParameters) {
      this.propertyData.edit = false
    }
  }

  private doUpdate() {
    this.savePending = true
    this.systemService.updateSystem(this.system.id!, this.system.sname!, this.system.fname!, this.system.description, this.system.version!, this.organisationId, this.otherSystem, this.propertyData.edit, this.propertyData.properties, this.copySystemParameters, this.copyStatementParameters)
    .subscribe(() => {
      this.modalRef.hide()
      this.reload.emit(true)
      this.popupService.success(this.dataService.labelSystem() + ' updated.')
    }).add(() => {
      this.savePending = false
    })
  }

  save() {
    if (this.saveEnabled()) {
      if (this.system.id != undefined) {
        // Update
        if (this.otherSystem != undefined) {
          this.confirmationDialogService.confirmed("Confirm test setup copy", "Copying the test setup from another "+ this.dataService.labelSystemLower()+" will remove current conformance statements and test results. Are you sure you want to proceed?", "Yes", "No")
          .subscribe(() => {
            this.doUpdate()
          })
        } else {
          this.doUpdate()
        }
      } else {
        // Create
        this.systemService.registerSystemWithOrganisation(this.system.sname!, this.system.fname!, this.system.description, this.system.version!, this.organisationId, this.otherSystem, this.propertyData.edit, this.propertyData.properties, this.copySystemParameters, this.copyStatementParameters)
        .subscribe(() => {
          this.modalRef.hide()
          this.reload.emit(true)
          this.popupService.success(this.dataService.labelSystem() + ' created.')
        }).add(() => {
          this.savePending = false
        })
      }
    }
  }

  delete() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this "+ this.dataService.labelSystemLower() + "?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.systemService.deleteSystem(this.system.id!, this.organisationId)
      .subscribe(() => {
        this.modalRef.hide()
        this.reload.emit(true)
        this.popupService.success(this.dataService.labelSystem() + ' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancel() {
    this.modalRef.hide()
  }

  canDelete() {
    return this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowSystemManagement)
  }

}
