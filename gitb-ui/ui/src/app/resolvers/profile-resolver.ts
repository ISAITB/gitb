/*
 * Copyright (C) 2026 European Union
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

import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {forkJoin, Observable, of, switchMap, tap} from 'rxjs';
import {map} from 'rxjs/operators';
import {AuthProviderService} from '../services/auth-provider.service';
import {AccountService} from '../services/account.service';
import {DataService} from '../services/data.service';
import {UserGuideService} from '../services/user-guide.service';
import {CommunityService} from '../services/community.service';
import {AuthService} from '../services/auth.service';
import {Constants} from '../common/constants';
import {AuthenticationStatus} from '../types/authentication-status';

@Injectable({
  providedIn: 'root'
})
export class ProfileResolver {

  constructor(
    private readonly authProviderService: AuthProviderService,
    private readonly accountService: AccountService,
    private readonly dataService: DataService,
    private readonly userGuideService: UserGuideService,
    private readonly communityService: CommunityService,
    private readonly authService: AuthService,
  ) { }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<void> {
    return this.resolveData(state);
  }

  resolveData(state: RouterStateSnapshot): Observable<void> {
    return this.loadConfiguration().pipe(
      switchMap(() => {
        const authStatus = this.authProviderService.getAuthenticatedStatus()
        if (authStatus === AuthenticationStatus.AuthenticatedWithAccessToken) {
          return this.loadAuthenticatedData();
        } else {
          return this.loadUnauthenticatedData(authStatus);
        }
      }),
      map(() => void 0)
    )
  }

  private loadConfiguration(): Observable<void> {
    if (this.dataService.configurationLoaded) {
      return of(void 0);
    }
    return this.accountService.getConfiguration().pipe(
      tap(data => this.dataService.setConfiguration(data)),
      map(() => void 0)
    );
  }

  private loadAuthenticatedData(): Observable<any> {
    return forkJoin([
      this.loadUserData(),
      this.loadOrganisation(),
      this.loadCommunity()
    ]);
  }

  private loadUserData(): Observable<any> {
    if (this.dataService.user?.role !== undefined) {
      return of(void 0);
    }
    const actualUser$ = this.dataService.configuration.ssoEnabled
      ? this.loadActualUserIfNeeded()
      : of(void 0);
    return actualUser$.pipe(
      switchMap(() =>
        this.accountService.getUserProfile().pipe(
          tap(data => {
            this.dataService.setUser(data);
            this.userGuideService.initialise();
          })
        )
      )
    );
  }

  private loadOrganisation(): Observable<any> {
    if (this.dataService.vendor) {
      return of(void 0);
    }
    return this.accountService.getVendorProfile().pipe(
      tap(data => this.dataService.setVendor(data))
    );
  }

  private loadCommunity(): Observable<any> {
    if (this.dataService.community) {
      return of(void 0);
    }
    return this.communityService.getUserCommunity().pipe(
      tap(data => this.dataService.setCommunity(data))
    );
  }

  private loadActualUserIfNeeded(): Observable<any> {
    if (this.dataService.actualUser) {
      return of(void 0);
    }
    return this.authService.getUserFunctionalAccounts().pipe(
      tap(data => this.dataService.setActualUser(data))
    );
  }

  private loadUnauthenticatedData(authStatus: AuthenticationStatus): Observable<any> {
    if (!this.loadAccountsForSelection(authStatus)) {
      return of(void 0);
    }
    return this.authService.getUserFunctionalAccounts().pipe(
      tap(data => this.dataService.setActualUser(data))
    );
  }

  private loadAccountsForSelection(authStatus: AuthenticationStatus): boolean {
    let result = this.dataService.configuration.ssoEnabled && !this.dataService.actualUser;
    if (result && this.dataService.configuration.ssoWithNativeLogin) {
      const loginOption = this.dataService.retrieveLoginOption();
      if (authStatus === AuthenticationStatus.NotAuthenticated || (loginOption !== Constants.LOGIN_OPTION.REGISTER_INTERNAL && loginOption !== Constants.LOGIN_OPTION.LINK_ACCOUNT_INTERNAL && loginOption !== Constants.LOGIN_OPTION.FORCE_CHOICE)) {
        // We need to present the login form.
        result = false;
      }
    }
    return result;
  }

}
