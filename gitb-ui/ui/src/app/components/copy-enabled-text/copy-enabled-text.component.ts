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

import {Component, HostListener, Input} from '@angular/core';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';

@Component({
    selector: 'app-copy-enabled-text',
    templateUrl: './copy-enabled-text.component.html',
    styleUrls: ['./copy-enabled-text.component.less'],
    standalone: false
})
export class CopyEnabledTextComponent {

  @Input() value: string|undefined
  hovering = false

  constructor(
    private readonly dataService: DataService,
    private readonly popupService: PopupService
  ) { }

  @HostListener('mouseenter')
  onMouseEnter() {
    this.hovering = true
  }

  @HostListener('mouseleave')
  onMouseLeave() {
    this.hovering = false
  }

  copy() {
    this.dataService.copyToClipboard(this.value).subscribe(() => {
      this.popupService.success('Value copied to clipboard.')
    })
  }
}
