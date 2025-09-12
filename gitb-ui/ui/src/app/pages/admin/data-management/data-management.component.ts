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

import {Component, OnInit} from '@angular/core';
import {RoutingService} from '../../../services/routing.service';

@Component({
  selector: 'app-data-management',
  standalone: false,
  templateUrl: './data-management.component.html',
  styleUrl: './data-management.component.less'
})
export class DataManagementComponent implements OnInit {

  operationType: number|undefined
  operationSelected = false
  initialSelection = true

  readonly OPERATION_EXPORT = 1
  readonly OPERATION_IMPORT = 2

  constructor(private routingService: RoutingService) {
  }

  ngOnInit() {
    this.routingService.dataManagementBreadcrumbs()
  }

  setOperation(operation: number) {
    this.operationType = operation
    if (this.operationSelected) {
      this.initialSelection = false
    } else {
      this.operationSelected = true
    }
  }

}
