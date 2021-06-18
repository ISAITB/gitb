import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class EndpointService {

  constructor(
    private restService: RestService
  ) { }

  deleteEndPoint(endPointId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.EndPointService.deleteEndPoint(endPointId).url,
      authenticate: true
    })
  }

  updateEndPoint(endPointId: number, name: string, description: string|undefined, actorId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.EndPointService.updateEndPoint(endPointId).url,
      data: {
        name: name,
        description: description,
        actor_id: actorId
      },
      authenticate: true
    })
  }
  
}
