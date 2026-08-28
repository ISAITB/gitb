/*
 * Copyright (C) 2026 European Union
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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Constants } from '../../common/constants';

/**
 * A generic rounded "pill" display - extracted from the applied custom property filter's styling (see
 * CustomPropertyFilterComponent) so it can be reused wherever a compact, rounded value summary is shown
 * (applied custom property filters, message sender/recipient display, etc). An optional bold `title`
 * precedes `text`, and an optional trailing icon (with its own tooltip) fires `action` when clicked.
 */
@Component({
  selector: 'app-pill',
  standalone: false,
  templateUrl: './pill.component.html',
  styleUrl: './pill.component.less'
})
export class PillComponent {

  @Input() title?: string
  @Input() text!: string
  @Input() shaded = true
  @Input() maxWidth?: number
  @Input() actionIcon?: string
  @Input() actionTooltip?: string
  @Input() actionPending = false
  @Output() action = new EventEmitter<void>()

  protected readonly Constants = Constants

  actionClicked(event: Event) {
    event.stopPropagation()
    this.action.emit()
  }

}
