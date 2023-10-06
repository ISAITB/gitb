import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from "@angular/router";
import { CommunityService } from "../services/community.service";
import { Constants } from "../common/constants";

@Injectable({
    providedIn: "root"
})
export class CommunityResolver implements Resolve<any> {

    constructor(
        private communityService: CommunityService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const communityId = Number(route.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
        return this.communityService.getCommunityById(communityId)
    }

}
