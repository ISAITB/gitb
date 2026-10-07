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

import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {Constants} from '../../common/constants';

export type ValuePillVariant = 'neutral'|'info'|'success'|'warning'|'danger'

/**
 * A light pill to present a value from a preset list (service type, user status, role, ...) in header-less
 * table columns.
 */
@Component({
  selector: 'app-value-pill',
  standalone: false,
  templateUrl: './value-pill.component.html',
  styleUrl: './value-pill.component.less',
  changeDetection: ChangeDetectionStrategy.Eager
})
export class ValuePillComponent {

  @Input() text!: string
  @Input() variant: ValuePillVariant = 'neutral'
  @Input() tooltip?: string

  protected readonly Constants = Constants

}
