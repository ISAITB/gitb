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

import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {Injectable} from '@angular/core';
import {mergeMap, Observable, of} from 'rxjs';
import {DataService} from '../services/data.service';
import {Constants} from '../common/constants';
import {CommunityService} from '../services/community.service';

@Injectable({
  providedIn: 'root'
})
export class ImplicitCommunityResolver {

  constructor(
    private readonly dataService: DataService,
    private readonly communityService: CommunityService) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    let result$: Observable<number|undefined> = of(undefined)
    if (this.dataService.isSystemAdmin) {
      let communityId$: Observable<number|undefined>
      if (route.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
        // We directly have the community ID
        communityId$ = of(Number(route.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)))
      } else if (route.paramMap.has(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID)) {
        // Lookup the community for the provided domain (if possible)
        communityId$ = this.communityService.getCommunityIdOfDomain(Number(route.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))).pipe(
          mergeMap((result) => {
            if (result != undefined) {
              return of(result.id)
            } else {
              return of(undefined)
            }
          })
        )
      } else if (route.paramMap.has(Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_ID)) {
        // Lookup the community for the provided snapshot
        communityId$ = this.communityService.getCommunityIdOfSnapshot(Number(route.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_ID))).pipe(
          mergeMap((result) => {
            if (result != undefined) {
              return of(result.id)
            } else {
              return of(undefined)
            }
          })
        )
      } else if (route.paramMap.has(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID)) {
        // Lookup the community for the provided actor (if possible)
        communityId$ = this.communityService.getCommunityIdOfActor(Number(route.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))).pipe(
          mergeMap((result) => {
            if (result != undefined) {
              return of(result.id)
            } else {
              return of(undefined)
            }
          })
        )
      } else {
        communityId$ = of(undefined)
      }
      result$ = communityId$.pipe(
        mergeMap((communityId) => {
          if (communityId != undefined) {
            this.dataService.setImplicitCommunity(Number(communityId))
          }
          return of(communityId)
        })
      )
    }
    return result$
  }

}
