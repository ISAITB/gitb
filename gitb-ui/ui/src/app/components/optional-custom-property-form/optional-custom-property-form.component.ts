import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { OptionalCustomPropertyFormData } from './optional-custom-property-form-data.type';

@Component({
  selector: 'app-optional-custom-property-form',
  templateUrl: './optional-custom-property-form.component.html'
})
export class OptionalCustomPropertyFormComponent implements OnInit {

  @Input() tbPropertyData!: OptionalCustomPropertyFormData
  @Input() tbPopup = false
  @Input() tbColLabel = 3
  @Input() tbColOffset = 1
  @Input() tbColInputLess = 0
  @Input() tbReadonly = false
  @Input() tbPropertyType!: 'organisation'|'system'
  @Input() tbOwner?: number

  isAdmin = false

  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.isAdmin = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
  }

  hasVisibleProperties() {
    let result = false
    if (this.tbPropertyData.properties.length > 0) {
      if (this.isAdmin) {
        result = true
      } else {
        for (let prop of this.tbPropertyData.properties) {
          if (!prop.hidden) {
            result = true
          }
        }
      }
    }
    return result
  }

  collapseChanged(collapsed: boolean) {
    this.tbPropertyData.edit = !collapsed
  }
}
