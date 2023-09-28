import { AfterViewInit, Component, Input, OnInit } from '@angular/core';
import { SystemFormData } from './system-form-data';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { DataService } from 'src/app/services/data.service';
import { CommunityService } from 'src/app/services/community.service';
import { SystemService } from 'src/app/services/system.service';
import { System } from 'src/app/types/system';

@Component({
  selector: 'app-system-form',
  templateUrl: './system-form.component.html',
  styles: [
  ]
})
export class SystemFormComponent implements OnInit, AfterViewInit {

  @Input() system!: Partial<SystemFormData>
  @Input() communityId!: number
  @Input() organisationId!: number
  @Input() propertyData!: OptionalCustomPropertyFormData
  @Input() showAdminInfo = true
  @Input() readonly = false

  otherSystems: System[] = []

  constructor(
    public dataService: DataService,
    private communityService: CommunityService,
    private systemService: SystemService,
  ) { }

  ngOnInit(): void {
    this.system.copySystemParameters = false
    this.system.copyStatementParameters = false
    if (this.system.id == undefined) {
      this.communityService.getSystemParameters(this.communityId)
      .subscribe((data) => {
        this.propertyData.properties = data
      })
    } else {
      this.propertyData.owner = this.system.id
			this.systemService.getSystemParameterValues(this.system.id)
      .subscribe((data) => {
        this.propertyData.properties = data
      })
    }
    if (this.showAdminInfo) {
      this.systemService.getSystemsByOrganisation(this.organisationId)
      .subscribe((data) => {
        for (let system of data) {
          if (this.system.id == undefined || Number(system.id) != Number(this.system.id)) {
            this.otherSystems.push(system)
          }
        }
      })
    }
  }

  ngAfterViewInit(): void {
    if (!this.readonly) {
      this.dataService.focus('sname')
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
}
