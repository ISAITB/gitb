import { ROUTES } from '../common/global';
import { Injectable } from '@angular/core';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class SpecificationService {

  constructor(
    private restService: RestService
  ) { }

  deleteSpecification(specId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.SpecificationService.deleteSpecification(specId).url,
      authenticate: true
    })
  }

  updateSpecification(specId: number, shortName: string, fullName: string, description: string|undefined, hidden: boolean|undefined) {
    const params:any = {
      sname: shortName,
      fname: fullName,
      hidden: false
    }
    if (hidden != undefined) {
      params.hidden = hidden
    }
    if (description != undefined) {
      params.description = description
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.updateSpecification(specId).url,
      data: params,
      authenticate: true
    })
  }

}
