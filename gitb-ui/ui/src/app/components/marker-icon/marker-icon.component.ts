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

/**
 * A row marker for a flag, shown in header-less table columns. Always rendered large (fa-lg) with a tooltip
 * attached to the body so that it is never clipped by the table container.
 */
@Component({
  selector: 'app-marker-icon',
  standalone: false,
  templateUrl: './marker-icon.component.html',
  styleUrl: './marker-icon.component.less',
  changeDetection: ChangeDetectionStrategy.Eager
})
export class MarkerIconComponent {

  @Input() icon!: string
  @Input() tooltip?: string
  @Input() colour?: string

  protected readonly Constants = Constants

}
