import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { mergeMap, Observable } from 'rxjs';
import { DataService } from '../services/data.service';
import { RoutingService } from '../services/routing.service';
import { ErrorService } from '../services/error.service';

@Injectable({
  providedIn: 'root'
})
export class SystemAdminViewGuard  {

  constructor(
    private dataService: DataService,
    private errorService: ErrorService,
    private routingService: RoutingService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      if (this.dataService.isSystemAdmin) {
        return true
      } else {
        return this.errorService.showUnauthorisedAccessError().pipe(
          mergeMap(() => {
            return this.routingService.toHome()
          })
        )
      }
  }

}
