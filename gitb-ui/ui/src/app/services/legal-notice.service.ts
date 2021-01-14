import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { RestService } from './rest.service'
import { ROUTES } from '../common/global';
import { LegalNotice } from '../types/legal-notice';
import { ErrorData } from '../types/error-data.type';
import { ErrorDescription } from '../types/error-description';

@Injectable({
  providedIn: 'root'
})
export class LegalNoticeService {

  constructor(private restService: RestService) { }

  getCommunityDefaultLegalNotice(communityId: number) {
    return this.restService.get<LegalNotice>({
      path: ROUTES.controllers.LegalNoticeService.getCommunityDefaultLegalNotice().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  getTestBedDefaultLegalNotice() {
    return this.restService.get<LegalNotice>({
      path: ROUTES.controllers.LegalNoticeService.getTestBedDefaultLegalNotice().url
    })
  }

  getLegalNoticesByCommunity(communityId: number) {
    return this.restService.get<LegalNotice[]>({
      path: ROUTES.controllers.LegalNoticeService.getLegalNoticesByCommunity(communityId).url,
      authenticate: true
    })
  }

  createLegalNotice(name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.LegalNoticeService.createLegalNotice().url,
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

  updateLegalNotice(noticeId: number, name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.LegalNoticeService.updateLegalNotice(noticeId).url,
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

  getLegalNoticeById(noticeId: number) {
    return this.restService.get<LegalNotice>({
      path: ROUTES.controllers.LegalNoticeService.getLegalNoticeById(noticeId).url,
      authenticate: true
    })
  }

  deleteLegalNotice(noticeId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.LegalNoticeService.deleteLegalNotice(noticeId).url,
      authenticate: true
    })
  }

}
