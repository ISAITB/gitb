import {Component, Input, OnInit} from '@angular/core';
import {SystemFormData} from './system-form-data';
import {OptionalCustomPropertyFormData} from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import {DataService} from 'src/app/services/data.service';
import {SystemService} from 'src/app/services/system.service';
import {System} from 'src/app/types/system';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {PopupService} from 'src/app/services/popup.service';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';
import {map, Observable, of} from 'rxjs';
import {FilterUpdate} from '../../../../../components/test-filter/filter-update';

@Component({
    selector: 'app-system-form',
    templateUrl: './system-form.component.html',
    styles: [],
    standalone: false
})
export class SystemFormComponent implements OnInit {

  @Input() system!: Partial<SystemFormData>
  @Input() communityId!: number
  @Input() organisationId!: number
  @Input() propertyData!: OptionalCustomPropertyFormData
  @Input() showAdminInfo = true
  @Input() readonly = false

  otherSystems?: System[]
  apiKeyUpdatePending = false
  copySelectionConfig!: MultiSelectConfig<System>

  constructor(
    public dataService: DataService,
    private systemService: SystemService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
    this.system.copySystemParameters = false
    this.system.copyStatementParameters = false
    this.copySelectionConfig = {
      name: "otherSystem",
      textField: "fname",
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: "Select source " + this.dataService.labelSystemLower() + "...",
      loader: () => this.loadOtherSystems()
    }
  }

  private loadOtherSystems(): Observable<System[]> {
    if (this.otherSystems == undefined) {
      return this.systemService.getSystemsByOrganisation(this.organisationId).pipe(
        map((data) => {
          let sources: System[]
          if (this.system.id != undefined) {
            sources = data
          } else {
            sources = data.filter(x => Number(x.id) != Number(this.system.id))
          }
          this.otherSystems = sources
          return sources
        })
      )
    } else {
      return of(this.otherSystems)
    }
  }

  otherSystemSelected(event: FilterUpdate<System>) {
    if (this.system) {
      if (event.values.active.length != 0) {
        this.system.otherSystems = event.values.active[0].id
      } else {
        this.system.otherSystems = undefined
      }
      this.copyChanged()
    }
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
