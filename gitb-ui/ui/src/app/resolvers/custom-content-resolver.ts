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
import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {Observable, Subscriber} from 'rxjs';
import {Constants} from '../common/constants';
import {CustomContentService} from '../services/custom-content.service';
import {CustomContent} from '../types/custom-content';

/**
 * Resolves the content to copy (if any) when creating custom content: either the Test Bed default of the type
 * being created, or an existing content item.
 */
@Injectable({
    providedIn: "root"
})
export class CustomContentResolver  {

    constructor(
        private readonly customContentService: CustomContentService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        return new Observable<CustomContent|undefined>((subscriber) => {
            if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY_DEFAULT)) {
                const type = Number(route.data['contentType'])
                this.handleResult(subscriber, this.customContentService.getTestBedDefaultCustomContent(type))
            } else if (route.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COPY)) {
                this.handleResult(subscriber, this.customContentService.getCustomContentById(Number(route.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.COPY))))
            } else {
                subscriber.next(undefined)
                subscriber.complete()
            }
        })
    }

    private handleResult(subscriber: Subscriber<any>, result: Observable<CustomContent>) {
        result.subscribe((data) => {
            if (data.exists) {
                data.name += ' COPY'
                subscriber.next(data)
            } else {
                subscriber.next(undefined)
            }
        }).add(() => {
            subscriber.complete()
        })
    }
}
