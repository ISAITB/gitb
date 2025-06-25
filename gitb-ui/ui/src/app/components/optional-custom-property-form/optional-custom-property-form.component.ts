/*
 * Copyright (C) 2025 European Union
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

import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { OptionalCustomPropertyFormData } from './optional-custom-property-form-data.type';

@Component({
    selector: 'app-optional-custom-property-form',
    templateUrl: './optional-custom-property-form.component.html',
    standalone: false
})
export class OptionalCustomPropertyFormComponent implements OnInit {

  @Input() tbPropertyData!: OptionalCustomPropertyFormData
  @Input() tbColLabel = 3
  @Input() tbColOffset = 1
  @Input() tbColInputLess = 0
  @Input() tbReadonly = false
  @Input() tbPropertyType!: 'organisation'|'system'|'statement'
  @Input() tbOwner?: number
  @Input() tbSetDefaults = false
  @Input() tbTopMargin = false

  isAdmin = false

  constructor(
    private readonly dataService: DataService
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
