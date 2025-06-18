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

import {Component, Input, OnInit} from '@angular/core';
import {DataService} from 'src/app/services/data.service';
import {filter} from 'lodash';
import {Parameter} from 'src/app/types/parameter';
import {Constants} from 'src/app/common/constants';

@Component({
    selector: 'app-parameter-display',
    templateUrl: './parameter-display.component.html',
    styleUrl: './parameter-display.component.less',
    standalone: false
})
export class ParameterDisplayComponent<T extends Parameter> implements OnInit {

  @Input() title!: string;
  @Input() noMargin = false;
  @Input() parameters!: T[]
  @Input() parameterLabel = 'Property'

  Constants = Constants

  isAdmin!: boolean
  collapsed = false
  hovering = false

  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.isAdmin = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
    if (this.parameters != undefined) {
      this.parameters = filter(this.parameters, (parameter) => {
        return ((parameter.configured == undefined || !parameter.configured)) && (this.isAdmin || !parameter.hidden) && (parameter.prerequisiteOk == undefined || parameter.prerequisiteOk) && this.isRequired(parameter)
      })
    }
  }

  isRequired(parameter: T) {
    return parameter.use == 'R'
  }

}
