import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthProviderService } from '../services/auth-provider.service';
import { DataService } from '../services/data.service';
import { RoutingService } from '../services/routing.service';

@Injectable({
  providedIn: 'root'
})
export class RouteAuthenticationGuard  {

  constructor(
    private authProviderService: AuthProviderService,
    private routingService: RoutingService,
    private dataService: DataService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      if (state.url !== '/login' && (!this.authProviderService.isAuthenticated() || this.dataService.user?.id == undefined)) {
        this.dataService.recordLocationDataOrRequestedRoute(state.url)
        return this.routingService.toLogin()
      }
      return true;      
  }

}
