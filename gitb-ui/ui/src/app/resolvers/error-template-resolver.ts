import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, RouterStateSnapshot } from "@angular/router";
import { Observable, Subscriber } from "rxjs";
import { Constants } from "../common/constants";
import { ErrorTemplateService } from "../services/error-template.service";
import { ErrorTemplate } from "../types/error-template";

@Injectable({
    providedIn: "root"
})
export class ErrorTemplateResolver  {

    constructor(
        private errorTemplateService: ErrorTemplateService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        return new Observable<ErrorTemplate|undefined>((subscriber) => {
            if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY_DEFAULT)) {
                this.handleResult(subscriber, this.errorTemplateService.getCommunityDefaultErrorTemplate(Constants.DEFAULT_COMMUNITY_ID))
            } else if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY)) {
                this.handleResult(subscriber, this.errorTemplateService.getErrorTemplateById(Number(route.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.COPY))))
    
            } else {
                subscriber.next()
                subscriber.complete()
            }
        })
    }

    private handleResult(subscriber: Subscriber<any>, result: Observable<ErrorTemplate>) {
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
