import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, RouterStateSnapshot } from "@angular/router";
import { Observable, Subscriber } from "rxjs";
import { mergeMap } from "rxjs/operators";
import { Constants } from "../common/constants";
import { LandingPageService } from "../services/landing-page.service";
import { LandingPage } from "../types/landing-page";

@Injectable({
    providedIn: "root"
})
export class LandingPageResolver  {

    constructor(
        private landingPageService: LandingPageService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        return new Observable<LandingPage|undefined>((subscriber) => {
            if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY_DEFAULT)) {
                this.handleResult(subscriber, this.landingPageService.getCommunityDefaultLandingPage(Constants.DEFAULT_COMMUNITY_ID))
            } else if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY)) {
                this.handleResult(subscriber, this.landingPageService.getLandingPageById(Number(route.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.COPY))))
            } else {
                subscriber.next()
                subscriber.complete()
            }
        })
    }

    private handleResult(subscriber: Subscriber<any>, result: Observable<LandingPage>) {
        result.subscribe((data) => {
            if (data.exists) {
                data.name += ' COPY'
                subscriber.next(data)
            } else {
                subscriber.next()
            }
        }).add(() => {
            subscriber.complete()
        })
    }
}
