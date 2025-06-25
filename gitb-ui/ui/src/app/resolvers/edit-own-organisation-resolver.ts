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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { DataService } from '../services/data.service';
import { OrganisationService } from '../services/organisation.service';

@Injectable({
    providedIn: "root"
})
export class EditOwnOrganisationResolver  {

    constructor(
        private readonly dataService: DataService,
        private readonly organisationService: OrganisationService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const $result = new Observable<boolean>((observer) => {
            if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
                observer.next(true)
                observer.complete()
            } else if (this.dataService.isVendorAdmin) {
                if  (this.dataService.community!.allowPostTestOrganisationUpdates) {
                    observer.next(true)
                    observer.complete()
                } else {
                    let hasTests = false
                    this.organisationService.ownOrganisationHasTests()
                    .subscribe((data) => {
                        hasTests = data.hasTests
                    }).add(() => {
                        observer.next(!hasTests)
                        observer.complete()
                    })
                }
            } else {
                observer.next(false)
                observer.complete()
            }
        })
        return $result
    }

}
