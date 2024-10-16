import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { mergeMap, Observable, of, share } from 'rxjs';
import { AuthProviderService } from '../services/auth-provider.service';
import { DataService } from '../services/data.service';
import { RoutingService } from '../services/routing.service';
import { ProfileResolver } from './profile-resolver';

@Injectable({
  providedIn: 'root'
})
export class RouteAuthenticationGuard  {

  constructor(
    private authProviderService: AuthProviderService,
    private routingService: RoutingService,
    private dataService: DataService,
    private profileResolver: ProfileResolver
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      if (state.url === '/login') {
        this.dataService.applyRequestedRoute()
        if (this.authProviderService.isAuthenticated()) {
          return this.goToStartForUser(state)
        } else {
          return true
        }
      } else {
        this.dataService.recordLocationDataOrRequestedRoute(state.url)
        if (this.authProviderService.isAuthenticated()) {
          return this.goToStartForUser(state)
        } else {
          return this.routingService.toLogin()
        }
      }
  }

  private goToStartForUser(state: RouterStateSnapshot): Observable<boolean> {
    let userId$: Observable<number>
    if (this.dataService.user?.id) {
      userId$ = of(this.dataService.user.id)
    } else {
      userId$ = this.profileResolver.resolveData(state).pipe(
        mergeMap(() => {
          return of(this.dataService.user!.id!)
        })
      )
    }
    return userId$.pipe(
      mergeMap((userId) => {
        const location = this.dataService.retrieveLocationData(userId)
        if (location == undefined) {
          return this.routingService.toHome()
        } else if (location == state.url) {
          return of(true)
        } else {
          return this.routingService.toURL(location)
        }
      })
    )
  }

}
