/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import {Injectable} from '@angular/core';
import {RestService} from './rest.service';
import {ROUTES} from '../common/global';
import {LegalNotice} from '../types/legal-notice';
import {ErrorDescription} from '../types/error-description';

@Injectable({
  providedIn: 'root'
})
export class LegalNoticeService {

  constructor(private readonly restService: RestService) { }

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
