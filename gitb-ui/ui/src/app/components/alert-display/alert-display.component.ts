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
import { Alert } from 'src/app/types/alert.type';

@Component({
    selector: 'app-alert-display',
    templateUrl: './alert-display.component.html',
    styles: ' .inTab ::ng-deep .alert { margin-bottom: 10px; } ',
    standalone: false
})
export class AlertDisplayComponent {

  @Input() alerts?: Alert[]
  @Input() outerClass = 'row'
  @Input() innerClass = ''
  @Input() dismissible = true

  constructor() { }

  clearAlert(alertIndex: number): void {
    if (this.alerts) {
      this.alerts.splice(alertIndex, 1)
    }
  }

}
