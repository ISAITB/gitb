import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthProviderService } from '../services/auth-provider.service';

@Injectable({
  providedIn: 'root'
})
export class RouteAuthenticationGuard  {

  constructor(
    private authProviderService: AuthProviderService,
    private router: Router
  ) {}

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
