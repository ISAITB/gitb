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

import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { ErrorDescription } from '../types/error-description';
import { ErrorTemplate } from '../types/error-template';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class ErrorTemplateService {

  constructor(private readonly restService: RestService) { }

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
