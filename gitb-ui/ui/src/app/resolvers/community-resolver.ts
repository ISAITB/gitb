import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from "@angular/router";
import { of } from "rxjs";
import { mergeMap } from "rxjs/operators";
import { CommunityService } from "../services/community.service";

@Injectable({
    providedIn: "root"
})
export class CommunityResolver implements Resolve<any> {

    constructor(
        private communityService: CommunityService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const communityId = Number(route.paramMap.get('community_id'))
        return this.communityService.getCommunityById(communityId).pipe(
            mergeMap((data) => {
                localStorage['community'] = JSON.stringify(data)
                return of(data)
            })
        )
    }

}
