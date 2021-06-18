import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from "@angular/router";
import { of } from "rxjs";
import { mergeMap } from "rxjs/operators";
import { DataService } from "../services/data.service";
import { SystemService } from "../services/system.service";

@Injectable({
    providedIn: "root"
})
export class ConformanceStatementNavigationResolver implements Resolve<any> {

    constructor(
        private dataService: DataService,
        private systemService: SystemService,
        private router: Router
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const systemId = Number(route.paramMap.get('id'))
        return this.systemService.getConformanceStatements(systemId).pipe(
            mergeMap((data) => {
                if (this.dataService.isVendorUser && data.length == 1) {
                    // Skip statement list page.
                    return this.router.navigate(['/', 'organisation', 'systems', systemId, 'conformance', 'detail', data[0].actorId, data[0].specificationId])
                } else {
                    return of(data)
                }
            })
        )
    }
}
