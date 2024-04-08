import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { DataService } from '../services/data.service';
import { SystemService } from '../services/system.service';
import { Constants } from '../common/constants';

@Injectable({
    providedIn: "root"
})
export class EditOwnSystemResolver  {

    constructor(
        private dataService: DataService,
        private systemService: SystemService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const $result = new Observable<boolean>((observer) => {
            if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
                observer.next(true)
                observer.complete()
            } else if (this.dataService.isVendorAdmin) {
                if  (this.dataService.community!.allowPostTestSystemUpdates) {
                    observer.next(true)
                    observer.complete()
                } else {
                    let hasTests = false
                    const systemId = Number(route.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID))
                    this.systemService.ownSystemHasTests(systemId)
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
