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

import { AfterViewInit, Component, Input } from '@angular/core';
import { DataService } from 'src/app/services/data.service';

@Component({
    selector: 'div[pending]',
    templateUrl: './pending-div.component.html',
    styleUrls: ['./pending-div.component.less'],
    standalone: false
})
export class PendingDivComponent implements AfterViewInit {

  _pending = false
  private visible = false
  private _focus?: string

  @Input() set pending(value: boolean|undefined) {
    this._pending = value == true
    this.applyFocus()
  }
  @Input() set focus(value:string|undefined) {
    this._focus = value
    this.applyFocus()
  }

  constructor(private readonly dataService: DataService) {}

  ngAfterViewInit(): void {
    this.visible = true
    this.applyFocus()
  }

  private applyFocus() {
    if (!this._pending && this._focus && this.visible) {
      this.dataService.focus(this._focus)
    }
  }

}
