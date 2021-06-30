import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from "@angular/router";
import { Observable, Subscriber } from "rxjs";
import { LegalNoticeService } from "../services/legal-notice.service";
import { LegalNotice } from "../types/legal-notice";

@Injectable({
    providedIn: "root"
})
export class LegalNoticeResolver implements Resolve<any> {

    constructor(
        private legalNoticeService: LegalNoticeService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        return new Observable<LegalNotice|undefined>((subscriber) => {
            if (route.queryParamMap.has('copyDefault')) {
                this.handleResult(subscriber, this.legalNoticeService.getTestBedDefaultLegalNotice())
            } else if (route.queryParamMap.has('copy')) {
                this.handleResult(subscriber, this.legalNoticeService.getLegalNoticeById(Number(route.queryParamMap.get('copy'))))
            } else {
                subscriber.next()
                subscriber.complete()
            }
        })
    }

    private handleResult(subscriber: Subscriber<any>, result: Observable<LegalNotice>) {
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
