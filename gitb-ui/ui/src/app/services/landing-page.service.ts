import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { ErrorDescription } from '../types/error-description';
import { LandingPage } from '../types/landing-page';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class LandingPageService {

  constructor(private restService: RestService) { }

  getCommunityDefaultLandingPage(communityId: number) {
    return this.restService.get<LandingPage>({
      path: ROUTES.controllers.LandingPageService.getCommunityDefaultLandingPage().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  getLandingPagesByCommunity(communityId: number) {
    return this.restService.get<LandingPage[]>({
      path: ROUTES.controllers.LandingPageService.getLandingPagesByCommunity(communityId).url,
      authenticate: true
    })
  }

  createLandingPage(name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.LandingPageService.createLandingPage().url,
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

  getLandingPageById(pageId: number) {
    return this.restService.get<LandingPage>({
      path: ROUTES.controllers.LandingPageService.getLandingPageById(pageId).url,
      authenticate: true
    })
  }

  updateLandingPage(pageId: number, name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.LandingPageService.updateLandingPage(pageId).url,
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

  deleteLandingPage(pageId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.LandingPageService.deleteLandingPage(pageId).url,
      authenticate: true
    })
  }

}
