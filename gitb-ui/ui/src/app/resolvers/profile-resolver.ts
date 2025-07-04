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
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router'
import { Observable, forkJoin, of } from 'rxjs';
import { map, mergeMap, share } from 'rxjs/operators'
import { AuthProviderService } from '../services/auth-provider.service'
import { AccountService } from '../services/account.service'
import { DataService } from '../services/data.service'
import { UserGuideService } from '../services/user-guide.service'
import { CommunityService } from '../services/community.service'
import { AuthService } from '../services/auth.service'

@Injectable({
    providedIn: "root"
})
export class ProfileResolver  {

    constructor(
        private readonly authProviderService: AuthProviderService,
        private readonly accountService: AccountService,
        private readonly dataService: DataService,
        private readonly userGuideService: UserGuideService,
        private readonly communityService: CommunityService,
        private readonly authService: AuthService,
    ) {}

    resolveData(state: RouterStateSnapshot): Observable<void> {
        let result: Observable<any>
        const authenticated = this.authProviderService.isAuthenticated()
        let configObservable: Observable<any>
        if (this.dataService.configurationLoaded) {
            configObservable = of(true)
        } else {
            configObservable = this.accountService.getConfiguration()
            .pipe(
                mergeMap((data) => {
                    this.dataService.setConfiguration(data)
                    return of(true)
                }),
                share()
            )
        }
        result = configObservable
        .pipe(
            mergeMap(() => {
                if (authenticated) {
                    // User information
                    let userObservable: Observable<any>
                    if (this.dataService.user && this.dataService.user.role != undefined) {
                        userObservable = of(true)
                    } else {
                        let actualUserObservable: Observable<any>
                        if (this.dataService.configuration.ssoEnabled) {
                            if (this.dataService.actualUser) {
                                actualUserObservable = of(true)
                            } else {
                                actualUserObservable = this.authService.getUserFunctionalAccounts()
                                .pipe(
                                    map((data) => {
                                        this.dataService.setActualUser(data)
                                    }),
                                    share()
                                )
                            }
                        } else {
                            actualUserObservable = of(true)
                        }
                        userObservable = actualUserObservable.pipe(
                            mergeMap(() => {
                                return this.accountService.getUserProfile()
                                .pipe(
                                    map((data) => {
                                        this.dataService.setUser(data)
                                        this.userGuideService.initialise()
                                    }),
                                    share()
                                )
                            })
                        )
                    }
                    // Organisation information
                    let vendorObservable: Observable<any>
                    if (this.dataService.vendor) {
                        vendorObservable = of(true)
                    } else {
                        vendorObservable = this.accountService.getVendorProfile()
                        .pipe(
                            map((data) => {
                                this.dataService.setVendor(data)
                            }),
                            share()
                        )
                    }
                    // Community information
                    let communityObservable: Observable<any>
                    if (this.dataService.community) {
                        communityObservable = of(true)
                    } else {
                        communityObservable = this.communityService.getUserCommunity()
                        .pipe(
                            map((data) => {
                                this.dataService.setCommunity(data)
                            }),
                            share()
                        )
                    }
                    return forkJoin([userObservable, vendorObservable, communityObservable])
                } else {
                    if (this.dataService.configuration.ssoEnabled && !this.dataService.actualUser) {
                        return this.authService.getUserFunctionalAccounts()
                            .pipe(
                                map((data) => {
                                    this.dataService.setActualUser(data)
                                }),
                                share()
                            )
                    } else {
                        return of(true)
                    }
                }
            })
        )
        return result
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<void> {
        return this.resolveData(state)
    }

}
