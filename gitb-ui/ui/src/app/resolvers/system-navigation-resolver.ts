import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from "@angular/router";
import { DataService } from "../services/data.service";
import { SystemService } from "../services/system.service";
import { Observable, of } from 'rxjs'
import { mergeMap } from "rxjs/operators";
import { Constants } from "../common/constants";
import { RoutingService } from "../services/routing.service";

@Injectable({
    providedIn: "root"
})
export class SystemNavigationResolver implements Resolve<any> {

    constructor(
        private systemService: SystemService,
        private dataService: DataService,
        private routingService: RoutingService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<any> {
        let organisationId: number
        if (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) {
            const organisation = JSON.parse(localStorage.getItem(Constants.LOCAL_DATA.ORGANISATION)!)
            organisationId = organisation.id
        } else {
            organisationId = this.dataService.vendor!.id
        }
        return this.systemService.getSystemsByOrganisation(organisationId).pipe(
            mergeMap((data) => {
                if (this.dataService.isVendorUser && data.length == 1) {
                    // Skip system list page.
                    return this.routingService.toConformanceStatements(organisationId, data[0].id, data.length)
                } else {
                    return of(data)
                }
            })
        )
    }
}
