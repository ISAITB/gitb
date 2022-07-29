import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { TriggerCallResult } from '../types/trigger-call-result';
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

  createTrigger(name: string, description: string|undefined, operation: string|undefined, active: boolean|undefined, url: string, event: number, communityId: number, dataItems?: TriggerDataItem[]) {
    const data: any = {
        name: name,
        description: description,
        url: url,
        active: active != undefined && active,
        operation: operation,
        event: event,
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

  preview(operation: string|undefined, dataItems: TriggerDataItem[]|undefined, communityId: number) {
    const data: any = {
        community_id: communityId
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

  test(url: string, payload: string, communityId: number) {
    return this.restService.post<TriggerCallResult>({
      path: ROUTES.controllers.TriggerService.testTriggerCall().url,
      authenticate: true,
      data: {
        url: url,
        community_id: communityId,
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

  updateTrigger(triggerId: number, name: string, description: string|undefined, operation: string|undefined, active: boolean|undefined, url: string, event: number, communityId: number, dataItems?: TriggerDataItem[]) {
    const data: any = {
        name: name,
        description: description,
        url: url,
        active: active != undefined && active,
        operation: operation,
        event: event,
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

  testTriggerEndpoint(url: string, communityId: number) {
    return this.restService.post<TriggerCallResult>({
      path: ROUTES.controllers.TriggerService.testTriggerEndpoint().url,
      authenticate: true,
      data: {
        url: url,
        community_id: communityId
      }  
    })
  }
}
