/*
 * Copyright (C) 2026 European Union
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
import {Constants} from '../common/constants';
import {CustomContent} from '../types/custom-content';
import {ErrorDescription} from '../types/error-description';
import {SearchResult} from '../types/search-result';

@Injectable({
  providedIn: 'root'
})
export class CustomContentService {

  constructor(private readonly restService: RestService) { }

  getCommunityDefaultCustomContent(type: number, communityId: number) {
    return this.restService.get<CustomContent>({
      path: ROUTES.controllers.CustomContentService.getCommunityDefaultCustomContent().url,
      authenticate: true,
      params: {
        community_id: communityId,
        type: type
      }
    })
  }

  /** The Test Bed default of the given type. */
  getTestBedDefaultCustomContent(type: number) {
    return this.getCommunityDefaultCustomContent(type, Constants.DEFAULT_COMMUNITY_ID)
  }

  /** The default legal notice of the Test Bed, accessible without authentication (e.g. from the login page). */
  getTestBedDefaultLegalNotice() {
    return this.restService.get<CustomContent>({
      path: ROUTES.controllers.CustomContentService.getTestBedDefaultLegalNotice().url
    })
  }

  /** The types of custom content for which the Test Bed has a default defined. */
  getTestBedDefaultTypes() {
    return this.restService.get<{landingPage: boolean, legalNotice: boolean, errorTemplate: boolean}>({
      path: ROUTES.controllers.CustomContentService.getTestBedDefaultTypes().url,
      authenticate: true
    })
  }

  /** The custom content (without the content itself) of the given community, of all types or only the given ones. */
  getCustomContentByCommunity(communityId: number, types?: number[]) {
    return this.restService.get<CustomContent[]>({
      path: ROUTES.controllers.CustomContentService.getCustomContentByCommunity(communityId).url,
      authenticate: true,
      params: {
        types: types?.join(',')
      }
    })
  }

  searchCustomContentByCommunity(communityId: number, types: number[]|undefined, page: number|undefined, limit: number|undefined) {
    return this.restService.get<SearchResult<CustomContent>>({
      path: ROUTES.controllers.CustomContentService.searchCustomContentByCommunity(communityId).url,
      authenticate: true,
      params: {
        types: types?.join(','),
        page: page,
        limit: limit
      }
    })
  }

  createCustomContent(type: number, name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.CustomContentService.createCustomContent().url,
      data: {
        type: type,
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      },
      authenticate: true
    })
  }

  getCustomContentById(contentId: number) {
    return this.restService.get<CustomContent>({
      path: ROUTES.controllers.CustomContentService.getCustomContentById(contentId).url,
      authenticate: true
    })
  }

  updateCustomContent(contentId: number, name: string, description: string|undefined, content: string|undefined, defaultFlag: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.CustomContentService.updateCustomContent(contentId).url,
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

  deleteCustomContent(contentId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.CustomContentService.deleteCustomContent(contentId).url,
      authenticate: true
    })
  }

}
