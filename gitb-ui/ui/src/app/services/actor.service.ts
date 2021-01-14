import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class ActorService {

  constructor(
    private restService: RestService
  ) { }

  deleteActor(actorId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ActorService.deleteActor(actorId).url,
      authenticate: true
    })
  }

  updateActor(id: number, actorId: string, name: string, description: string|undefined, defaultActor: boolean|undefined, hiddenActor: boolean|undefined, displayOrder: number|undefined, domainId: number, specificationId: number) {
    if (hiddenActor == undefined) hiddenActor = false
    if (defaultActor == undefined) defaultActor = false
    const data: any = {
      actor_id: actorId,
      name: name,
      description: description,
      default: defaultActor,
      hidden: hiddenActor,
      domain_id: domainId,
      spec_id: specificationId
    }
    if (displayOrder != undefined) data.displayOrder = Number(displayOrder)
    return this.restService.post<void>({
      path: ROUTES.controllers.ActorService.updateActor(id).url,
      authenticate: true,
      data: data
    })
  }

}
