import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { mergeMap, Observable, of } from 'rxjs';
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
    private routingService: RoutingService,
    private profileResolver: ProfileResolver
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      return this.userLoaded(state).pipe(
        mergeMap(() => {
          if (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) {
            return of(true)
          } else {
            return this.errorService.showUnauthorisedAccessError().pipe(
              mergeMap(() => {
                return this.routingService.toHome()
              })
            )
          }
        })
      )
  }

  private userLoaded(state: RouterStateSnapshot): Observable<any> {
    if (this.dataService.user?.id) {
      // The ID property of the user is set only after all information on the user had been loaded.
      return of(true)
    } else {
      return this.profileResolver.resolveData(state)
    }
  }

}
