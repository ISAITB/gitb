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
import { ServiceCallResult } from '../types/service-call-result';
import { ErrorDescription } from '../types/error-description';
import { Trigger } from '../types/trigger';
import { TriggerDataItem } from '../types/trigger-data-item';
import { TriggerInfo } from '../types/trigger-info';
import { RestService } from './rest.service';
import {TriggerFireExpression} from '../types/trigger-fire-expression';

@Injectable({
  providedIn: 'root'
})
export class TriggerService {

  constructor(
    private readonly restService: RestService
  ) { }

  getTriggersByCommunity(communityId: number) {
    return this.restService.get<Trigger[]>({
      path: ROUTES.controllers.TriggerService.getTriggersByCommunity(communityId).url,
      authenticate: true
    })
  }

  createTrigger(name: string, description: string|undefined, operation: string|undefined, active: boolean|undefined, url: string, event: number, serviceType: number, communityId: number, dataItems?: TriggerDataItem[], fireExpressions?: TriggerFireExpression[]) {
    const data: any = {
        name: name,
        description: description,
        url: url,
        active: active != undefined && active,
        operation: operation,
        event: event,
        type: serviceType,
        community_id: communityId
    }
    if (dataItems != undefined) {
      data.data = JSON.stringify(dataItems)
    }
    if (fireExpressions != undefined && fireExpressions.length > 0) {
      data.expressions = JSON.stringify(fireExpressions)
    }
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.TriggerService.createTrigger().url,
      authenticate: true,
      data: data
    })
  }

  preview(operation: string|undefined, serviceType: number, dataItems: TriggerDataItem[]|undefined, communityId: number) {
    const data: any = {
        community_id: communityId,
        type: serviceType
    }
    if (operation != undefined) {
      data.operation = operation
    }
    if (dataItems != undefined) {
      data.data = JSON.stringify(dataItems)
    }
    return this.restService.post<{message: string}>({
      path: ROUTES.controllers.TriggerService.previewTriggerCall().url,
      authenticate: true,
      data: data
    })
  }

  test(url: string, serviceType: number, payload: string, communityId: number) {
    return this.restService.post<ServiceCallResult>({
      path: ROUTES.controllers.TriggerService.testTriggerCall().url,
      authenticate: true,
      data: {
        url: url,
        community_id: communityId,
        type: serviceType,
        payload: payload
      }
    })
  }

  getTriggerById(triggerId: number) {
    return this.restService.get<TriggerInfo>({
      path: ROUTES.controllers.TriggerService.getTriggerById(triggerId).url,
      authenticate: true
    })
  }

  updateTrigger(triggerId: number, name: string, description: string|undefined, operation: string|undefined, active: boolean|undefined, url: string, event: number, serviceType: number, communityId: number, dataItems?: TriggerDataItem[], fireExpressions?: TriggerFireExpression[]) {
    const data: any = {
        name: name,
        description: description,
        url: url,
        active: active != undefined && active,
        operation: operation,
        event: event,
        type: serviceType,
        community_id: communityId
    }
    if (dataItems != undefined) {
      data.data = JSON.stringify(dataItems)
    }
    if (fireExpressions != undefined && fireExpressions.length > 0) {
      data.expressions = JSON.stringify(fireExpressions)
    }
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.TriggerService.updateTrigger(triggerId).url,
      authenticate: true,
      data: data
    })
  }

  deleteTrigger(triggerId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.TriggerService.deleteTrigger(triggerId).url,
      authenticate: true
    })
  }

  clearStatus(triggerId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.TriggerService.clearStatus(triggerId).url,
      authenticate: true
    })
  }

  testTriggerEndpoint(url: string, serviceType: number, communityId: number) {
    return this.restService.post<ServiceCallResult>({
      path: ROUTES.controllers.TriggerService.testTriggerEndpoint().url,
      authenticate: true,
      data: {
        url: url,
        type: serviceType,
        community_id: communityId
      }
    })
  }
}
