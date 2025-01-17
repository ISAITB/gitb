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
    private dataService: DataService,
    private communityService: CommunityService) {
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
