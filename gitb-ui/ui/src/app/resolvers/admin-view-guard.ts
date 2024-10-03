import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { map, mergeMap, Observable, of, share } from 'rxjs';
import { AuthProviderService } from '../services/auth-provider.service';
import { DataService } from '../services/data.service';
import { RoutingService } from '../services/routing.service';
import { ProfileResolver } from './profile-resolver';
import { ErrorService } from '../services/error.service';

@Injectable({
  providedIn: 'root'
})
export class AdminViewGuard  {

  constructor(
    private dataService: DataService,
    private errorService: ErrorService,
    private routingService: RoutingService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      if (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) {
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
