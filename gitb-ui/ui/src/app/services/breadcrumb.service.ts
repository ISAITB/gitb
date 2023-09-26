import { Injectable } from '@angular/core';
import { RestService } from './rest.service';
import { ROUTES } from '../common/global';
import { BreadcrumbLabelRequest } from '../types/breadcrumb-label-request';
import { BreadcrumbLabelResponse } from '../types/breadcrumb-label-response';

@Injectable({
  providedIn: 'root'
})
export class BreadcrumbService {

  constructor(private restService: RestService) { }

  getBreadcrumbLabels(ids: BreadcrumbLabelRequest) {
    let data: any = {}
    if (ids.domain != undefined) data.domain_id = ids.domain
    if (ids.specificationGroup != undefined) data.group_id = ids.specificationGroup
    if (ids.specification != undefined) data.spec_id = ids.specification
    if (ids.actor != undefined) data.actor_id = ids.actor
    if (ids.community != undefined) data.community_id = ids.community
    if (ids.organisation != undefined) data.organization_id = ids.organisation
    if (ids.system != undefined) data.system_id = ids.system
    return this.restService.post<BreadcrumbLabelResponse>({
      path: ROUTES.controllers.BreadcrumbService.getBreadcrumbLabels().url,
      authenticate: true,
      data: data
    })
  }



  

}
