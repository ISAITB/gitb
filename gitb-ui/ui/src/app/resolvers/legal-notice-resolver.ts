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

import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, RouterStateSnapshot } from "@angular/router";
import { Observable, Subscriber } from "rxjs";
import { LegalNoticeService } from "../services/legal-notice.service";
import { LegalNotice } from "../types/legal-notice";
import { Constants } from "../common/constants";

@Injectable({
    providedIn: "root"
})
export class LegalNoticeResolver  {

    constructor(
        private readonly legalNoticeService: LegalNoticeService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        return new Observable<LegalNotice|undefined>((subscriber) => {
            if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY_DEFAULT)) {
                this.handleResult(subscriber, this.legalNoticeService.getTestBedDefaultLegalNotice())
            } else if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY)) {
                this.handleResult(subscriber, this.legalNoticeService.getLegalNoticeById(Number(route.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.COPY))))
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
