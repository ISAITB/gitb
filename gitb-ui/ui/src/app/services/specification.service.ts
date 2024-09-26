import { ROUTES } from '../common/global';
import { Injectable } from '@angular/core';
import { RestService } from './rest.service';
import { SpecificationGroup } from '../types/specification-group';
import { Specification } from '../types/specification';
import { BadgesInfo } from '../components/manage-badges/badges-info';
import { DataService } from './data.service';

@Injectable({
  providedIn: 'root'
})
export class SpecificationService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  deleteSpecification(specId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.SpecificationService.deleteSpecification(specId).url,
      authenticate: true
    })
  }

  updateSpecification(specId: number, shortName: string, fullName: string, description: string|undefined, reportMetadata: string|undefined, hidden: boolean|undefined, groupId: number|undefined, badges: BadgesInfo) {
    const params:any = {
      sname: shortName,
      fname: fullName,
      hidden: false,
      group_id: groupId
    }
    if (hidden != undefined) {
      params.hidden = hidden
    }
    if (description != undefined) {
      params.description = description
    }
    if (reportMetadata != undefined) {
      params.metadata = reportMetadata
    }
    const files = this.dataService.parametersForBadgeUpdate(badges, params)
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.updateSpecification(specId).url,
      data: params,
      authenticate: true,
      files: files
    })
  }

  getSpecificationIdOfActor(actorId: number) {
    return this.restService.get<{id: number}>({
      path: ROUTES.controllers.SpecificationService.getSpecificationOfActor(actorId).url,
      authenticate: true
    }) 
  }

  getSpecificationOfActor(actorId: number) {
    return this.restService.get<Specification>({
      path: ROUTES.controllers.SpecificationService.getSpecificationOfActor(actorId).url,
      authenticate: true
    }) 
  }

  getSpecificationGroupsOfDomains(domainIds: number[]|undefined) {
    let params: any = {}
    if (domainIds != undefined && domainIds.length > 0) {
      params['domain_ids'] = domainIds.join(',')
    }    
    return this.restService.post<SpecificationGroup[]>({
      path: ROUTES.controllers.SpecificationService.getSpecificationGroupsOfDomains().url,
      data: params,
      authenticate: true
    })
  }

  getSpecificationGroups(domainId: number) {
    return this.restService.get<SpecificationGroup[]>({
      path: ROUTES.controllers.SpecificationService.getSpecificationGroups().url,
      params: {
        domain_id: domainId
      },
      authenticate: true
    })
  }

  getSpecificationGroup(groupId: number) {
    return this.restService.get<SpecificationGroup>({
      path: ROUTES.controllers.SpecificationService.getSpecificationGroup(groupId).url,
      authenticate: true
    })
  }

  createSpecificationGroup(shortName: string, fullName: string, description: string|undefined, reportMetadata: string|undefined, domainId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.createSpecificationGroup().url,
      data: {
        sname: shortName,
        fname: fullName,
        description: description,
        metadata: reportMetadata,
        domain_id: domainId
      },
      authenticate: true
    })
  }

  updateSpecificationGroup(groupId: number, shortName: string, fullName: string, description: string|undefined, reportMetadata: string|undefined) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.updateSpecificationGroup(groupId).url,
      data: {
        sname: shortName,
        fname: fullName,
        description: description,
        metadata: reportMetadata
      },
      authenticate: true
    })
  }

  deleteSpecificationGroup(groupId: number, alsoSpecs: boolean) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.deleteSpecificationGroup(groupId).url,
      data: {
        specs: alsoSpecs
      },
      authenticate: true
    })
  }

  addSpecificationToGroup(groupId: number, specificationId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.addSpecificationToGroup(groupId).url,
      data: {
        spec: specificationId
      },
      authenticate: true
    })
  }  

  copySpecificationToGroup(groupId: number, specificationId: number) {
    return this.restService.post<{id: number}>({
      path: ROUTES.controllers.SpecificationService.copySpecificationToGroup(groupId).url,
      data: {
        spec: specificationId
      },
      authenticate: true
    })
  }

  removeSpecificationFromGroup(specificationId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.removeSpecificationFromGroup(specificationId).url,
      authenticate: true
    })
  }  

  saveSpecificationOrder(domainId: number, groupIds: number[], groupOrders: number[], specIds: number[], specOrders: number[]) {
    const data: any = {
      domain_id: domainId
    }
    if (groupIds.length > 0) {
      data.group_ids = groupIds.join(',')
      data.group_orders = groupOrders.join(',')
    }
    if (specIds.length > 0) {
      data.specification_ids = specIds.join(',')
      data.specification_orders = specOrders.join(',')
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.saveSpecificationOrder().url,
      data: data,
      authenticate: true
    })
  }  

  resetSpecificationOrder(domainId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SpecificationService.resetSpecificationOrder().url,
      data: {
        domain_id: domainId
      },
      authenticate: true
    })
  }  
  
}
