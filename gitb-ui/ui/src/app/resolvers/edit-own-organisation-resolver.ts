import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { DataService } from '../services/data.service';
import { OrganisationService } from '../services/organisation.service';

@Injectable({
    providedIn: "root"
})
export class EditOwnOrganisationResolver implements Resolve<boolean> {

    constructor(
        private dataService: DataService,
        private organisationService: OrganisationService
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
