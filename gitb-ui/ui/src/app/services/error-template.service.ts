import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { ErrorDescription } from '../types/error-description';
import { ErrorTemplate } from '../types/error-template';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class ErrorTemplateService {

  constructor(private restService: RestService) { }

  getCommunityDefaultErrorTemplate(communityId: number) {
    return this.restService.get<ErrorTemplate>({
      path: ROUTES.controllers.ErrorTemplateService.getCommunityDefaultErrorTemplate().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  getErrorTemplatesByCommunity(communityId: number) {
    return this.restService.get<ErrorTemplate[]>({
      path: ROUTES.controllers.ErrorTemplateService.getErrorTemplatesByCommunity(communityId).url,
      authenticate: true
    })
  }

  createErrorTemplate(name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.ErrorTemplateService.createErrorTemplate().url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      },
      authenticate: true
    })
  }

  getErrorTemplateById(pageId: number) {
    return this.restService.get<ErrorTemplate>({
      path: ROUTES.controllers.ErrorTemplateService.getErrorTemplateById(pageId).url,
      authenticate: true
    })
  }

  updateErrorTemplate(pageId: number, name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.ErrorTemplateService.updateErrorTemplate(pageId).url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      },
      authenticate: true
    })
  }

  deleteErrorTemplate(pageId: number) {
    return this.restService.delete({
      path: ROUTES.controllers.ErrorTemplateService.deleteErrorTemplate(pageId).url,
      authenticate: true
    })
  }

}
