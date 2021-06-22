import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router'
import { Observable, forkJoin } from 'rxjs';
import { mergeMap } from 'rxjs/operators'
import { AuthProviderService } from '../services/auth-provider.service'
import { AccountService } from '../services/account.service'
import { DataService } from '../services/data.service'
import { UserGuideService } from '../services/user-guide.service'
import { CommunityService } from '../services/community.service'
import { AuthService } from '../services/auth.service'

@Injectable({
    providedIn: "root"
})
export class ProfileResolver implements Resolve<any> {

    constructor(
        private authProviderService: AuthProviderService, 
        private accountService: AccountService, 
        private dataService: DataService,
        private userGuideService: UserGuideService,
        private communityService: CommunityService,
        private authService: AuthService,
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<void> {
        let result: Observable<any>
        const authenticated = this.authProviderService.isAuthenticated()
        const configObservable = new Observable<any>((configObserver) => {
            if (this.dataService.configurationLoaded) {
                configObserver.next()
                configObserver.complete()
            } else {
                this.accountService.getConfiguration().subscribe((data) => {
                    this.dataService.setConfiguration(data)
                    configObserver.next()
                    configObserver.complete()
                })
            }
        })
        if (authenticated) {
            const userObservable = new Observable<any>((userObserver) => {
                if (this.dataService.user && this.dataService.user.role !== undefined) {
                    userObserver.next()
                    userObserver.complete()
                } else {
                    this.accountService.getUserProfile().subscribe((data) => {
                        this.dataService.setUser(data)
                        this.userGuideService.initialise()
                        userObserver.next()
                        userObserver.complete()
                    })
                }
            })
            const vendorObservable = new Observable<any>((vendorObserver) => {
                if (this.dataService.vendor) {
                    vendorObserver.next()
                    vendorObserver.complete()
                } else {
                    this.accountService.getVendorProfile().subscribe((data) => {
                        this.dataService.setVendor(data)
                        vendorObserver.next()
                        vendorObserver.complete()
                    })
                }
            })
            const communityObservable = new Observable<any>((communityObserver) => {
                if (this.dataService.community) {
                    communityObserver.next()
                    communityObserver.complete()
                } else {
                    this.communityService.getUserCommunity().subscribe((data) => {
                        this.dataService.setCommunity(data)
                        communityObserver.next()
                        communityObserver.complete()
                    })
                }
            })
            result = forkJoin([userObservable, vendorObservable, communityObservable, configObservable])
        } else {
            result = configObservable.pipe(
                mergeMap(() => new Observable<any>((ssoObserver) => {
                    if (this.dataService.configuration.ssoEnabled && !this.dataService.actualUser) {
                        this.authService.getUserFunctionalAccounts().subscribe((data) => {
                            this.dataService.setActualUser(data)
                            ssoObserver.next()
                            ssoObserver.complete()
                        }) 
                    } else {
                        ssoObserver.next()
                        ssoObserver.complete()
                    }
                }))
            )
        }
        return result
    }

}
