/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
