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

import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { ErrorDescription } from '../types/error-description';
import { TestFlag } from '../types/test-flag';
import { RestService } from './rest.service';
import { SearchResult } from '../types/search-result';

@Injectable({
  providedIn: 'root'
})
export class TestFlagService {

  constructor(
    private readonly restService: RestService
  ) { }

  getTestFlagsByCommunity(communityId: number, page: number|undefined, limit: number|undefined) {
    return this.restService.get<SearchResult<TestFlag>>({
      path: ROUTES.controllers.TestFlagService.getTestFlagsByCommunity(communityId).url,
      authenticate: true,
      params: {
        page: page,
        limit: limit
      }
    })
  }

  getAllTestFlagsByCommunity(communityId: number) {
    return this.restService.get<TestFlag[]>({
      path: ROUTES.controllers.TestFlagService.getAllTestFlagsByCommunity(communityId).url,
      authenticate: true
    })
  }

  createTestFlag(name: string, description: string|undefined, colour: string, publicName: string|undefined, publicColour: string|undefined, adminOnly: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.TestFlagService.createTestFlag().url,
      authenticate: true,
      data: {
        name: name,
        description: description,
        colour: colour,
        publicName: publicName,
        publicColour: publicColour,
        admin_only: adminOnly,
        community_id: communityId
      }
    })
  }

  updateTestFlag(testFlagId: number, name: string, description: string|undefined, colour: string, publicName: string|undefined, publicColour: string|undefined, adminOnly: boolean, communityId: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.TestFlagService.updateTestFlag(testFlagId).url,
      authenticate: true,
      data: {
        name: name,
        description: description,
        colour: colour,
        publicName: publicName,
        publicColour: publicColour,
        admin_only: adminOnly,
        community_id: communityId
      }
    })
  }

  deleteTestFlag(testFlagId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.TestFlagService.deleteTestFlag(testFlagId).url,
      authenticate: true
    })
  }

  orderTestFlags(communityId: number, orderedIds: number[]) {
    return this.restService.post<void>({
      path: ROUTES.controllers.TestFlagService.orderTestFlags(communityId).url,
      authenticate: true,
      data: {
        ids: orderedIds.join(',')
      }
    })
  }

  resetTestFlagOrder(communityId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.TestFlagService.resetTestFlagOrder(communityId).url,
      authenticate: true
    })
  }

}
