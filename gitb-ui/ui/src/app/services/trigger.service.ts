import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { ServiceCallResult } from '../types/service-call-result';
import { ErrorDescription } from '../types/error-description';
import { Trigger } from '../types/trigger';
import { TriggerDataItem } from '../types/trigger-data-item';
import { TriggerInfo } from '../types/trigger-info';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class TriggerService {

  constructor(
    private restService: RestService
  ) { }

  getTriggersByCommunity(communityId: number) {
    return this.restService.get<Trigger[]>({
      path: ROUTES.controllers.TriggerService.getTriggersByCommunity(communityId).url,
      authenticate: true
    })
  }

  createTrigger(name: string, description: string|undefined, operation: string|undefined, active: boolean|undefined, url: string, event: number, serviceType: number, communityId: number, dataItems?: TriggerDataItem[]) {
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

  updateTrigger(triggerId: number, name: string, description: string|undefined, operation: string|undefined, active: boolean|undefined, url: string, event: number, serviceType: number, communityId: number, dataItems?: TriggerDataItem[]) {
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
