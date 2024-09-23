import { Component, Input, OnInit } from '@angular/core';
import { SystemFormData } from './system-form-data';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';
import { System } from 'src/app/types/system';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { PopupService } from 'src/app/services/popup.service';

@Component({
  selector: 'app-system-form',
  templateUrl: './system-form.component.html',
  styles: [
  ]
})
export class SystemFormComponent implements OnInit {

  @Input() system!: Partial<SystemFormData>
  @Input() communityId!: number
  @Input() organisationId!: number
  @Input() propertyData!: OptionalCustomPropertyFormData
  @Input() showAdminInfo = true
  @Input() readonly = false
  @Input() otherSystems: System[] = []

  apiKeyUpdatePending = false

  constructor(
    public dataService: DataService,
    private systemService: SystemService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
    this.system.copySystemParameters = false
    this.system.copyStatementParameters = false
  }

  copyChanged() {
    if (this.system.otherSystems == undefined) {
      this.system.copySystemParameters = false
      this.system.copyStatementParameters = false
    } else if (this.system.copySystemParameters) {
      this.propertyData.edit = false
    }
  }

  updateApiKey(): void {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the API key value?", "Update", "Cancel")
    .subscribe(() => {
      this.apiKeyUpdatePending = true
      this.systemService.updateSystemApiKey(this.system.id!)
      .subscribe((newApiKey) => {
        this.system.apiKey = newApiKey
        this.popupService.success(this.dataService.labelSystem()+" API key updated.")
      }).add(() => {
        this.apiKeyUpdatePending = false
      })
    })
  }

}
