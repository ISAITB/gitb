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

import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class ParameterService {

  constructor(
    private restService: RestService
  ) { }

  deleteParameter(parameterId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ParameterService.deleteParameter(parameterId).url,
      authenticate: true
    })
  }

  updateParameter(parameterId: number, name: string, testKey: string, description: string|undefined, use: string, kind: string, adminOnly: boolean, notForTests: boolean, hidden: boolean, allowedValues: string|undefined, dependsOn: string|undefined, dependsOnValue: string|undefined, defaultValue: string|undefined, endpointId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ParameterService.updateParameter(parameterId).url,
      data: {
        name: name,
        test_key: testKey,
        description: description,
        use: use,
        kind: kind,
        admin_only: adminOnly,
        not_for_tests: notForTests,
        hidden: hidden,
        allowedValues: allowedValues,
        dependsOn: dependsOn,
        dependsOnValue: dependsOnValue,
        defaultValue: defaultValue,
        endpoint_id: endpointId
      },
      authenticate: true
    })
  }

  orderParameters(endpointId: number, orderedIds: number[]) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ParameterService.orderParameters(endpointId).url,
      data: {
        ids: orderedIds.join(',')
      },
      authenticate: true
    })
  }
}
