import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from "@angular/router";
import { of } from "rxjs";
import { mergeMap } from "rxjs/operators";
import { Constants } from "../common/constants";
import { DataService } from "../services/data.service";
import { OrganisationService } from "../services/organisation.service";
import { Organisation } from "../types/organisation.type";

@Injectable({
    providedIn: "root"
})
export class OrganisationResolver implements Resolve<any> {

    constructor(
        private dataService: DataService,
        private organisationService: OrganisationService
    ) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        // Handle target organisation.
        const targetOrgId = Number(route.paramMap.get('org_id'))
        if (targetOrgId == this.dataService.vendor!.id) {
            // We are looking up our own organisation.
            localStorage.setItem(Constants.LOCAL_DATA.ORGANISATION, JSON.stringify(this.dataService.vendor!))
            return of(this.dataService.vendor!)
        } else {
            const storedOrganisation = this.getStoredOrganisation()
            if (storedOrganisation?.id != targetOrgId) {
                // Lookup target organisation.
                return this.organisationService.getOrganisationById(targetOrgId).pipe(
                    mergeMap((data) => {
                        localStorage.setItem(Constants.LOCAL_DATA.ORGANISATION, JSON.stringify(data))
                        return of(data)
                    })
                )
            } else {
                return of(storedOrganisation)
            }
        }
    }

    getStoredOrganisation(): Organisation|undefined {
        const orgData = localStorage.getItem(Constants.LOCAL_DATA.ORGANISATION)
        if (orgData == undefined) {
            return undefined
        } else {
            return JSON.parse(orgData)
        }
    }

}
