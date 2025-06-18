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

import { Component, Input } from '@angular/core';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-custom-property-panel',
    templateUrl: './custom-property-panel.component.html',
    styleUrl: './custom-property-panel.component.less',
    standalone: false
})
export class CustomPropertyPanelComponent {

  @Input() properties!: CustomProperty[]
  @Input() propertyType!: 'organisation'|'system'|'statement'
  @Input() owner!: number
  @Input() header!: string
  @Input() collapsed = false
  @Input() validation!: ValidationState
  @Input() topMargin = true
  @Input() readonly = false

  hovering = false
  headerCollapsed = false

  panelExpanding() {
    setTimeout(() => {
      this.headerCollapsed = false
    }, 1)
  }

}
