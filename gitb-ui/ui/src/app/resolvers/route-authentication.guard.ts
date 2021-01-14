import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthProviderService } from '../services/auth-provider.service';
import { DataService } from '../services/data.service';

@Injectable({
  providedIn: 'root'
})
export class RouteAuthenticationGuard implements CanActivate {

  constructor(
    private authProviderService: AuthProviderService,
    private router: Router,
    private dataService: DataService
  ) {
  }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      if (state.url !== '/login' 
        && !this.authProviderService.isAuthenticated()) {
          return this.router.parseUrl('login')
      }
    return true;
  }

}
