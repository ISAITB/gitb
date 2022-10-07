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
