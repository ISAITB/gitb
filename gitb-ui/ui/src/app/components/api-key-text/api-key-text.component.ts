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

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';

@Component({
    selector: 'app-api-key-text',
    templateUrl: './api-key-text.component.html',
    standalone: false
})
export class ApiKeyTextComponent {

  @Input() key!: string
  @Input() idName!: string
  @Input() name!: string
  @Input() label?: string
  @Input() inputWidth?: string
  @Input() supportUpdate = false
  @Input() supportDelete = false
  @Input() supportCopy = true
  @Input() updatePending = false
  @Input() deletePending = false

  @Output() update = new EventEmitter<string>()
  @Output() delete = new EventEmitter<string>()

  Constants = Constants

  constructor(
    private readonly dataService: DataService,
    private readonly popupService: PopupService) { }

  copy() {
    this.dataService.copyToClipboard(this.key).subscribe(() => {
      this.popupService.success('Value copied to clipboard.')
    })
  }

  doDelete() {
    this.delete.emit(this.key)
  }

  doUpdate() {
    this.update.emit(this.key)
  }

}
