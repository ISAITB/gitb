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
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, of, ReplaySubject, shareReplay, tap} from 'rxjs';
import {LogoutEventInfo} from '../types/logout-event-info.type';
import {LoginEventInfo} from '../types/login-event-info.type';
import {DataService} from './data.service';
import {Utils} from '../common/utils';
import {ROUTES} from '../common/global';
import {RoutingService} from './routing.service';
import {PopupService} from './popup.service';
import {catchError, map} from 'rxjs/operators';
import {LoginResultOk} from '../types/login-result-ok';
import {AuthenticationStatus} from '../types/authentication-status';

@Injectable({
  providedIn: 'root'
})
export class AuthProviderService {

  private onLoginSource = new ReplaySubject<LoginEventInfo>()
  private afterLoginSource = new ReplaySubject<LoginEventInfo>()
  private onLogoutSource = new ReplaySubject<LogoutEventInfo>()
  private onLogoutCompleteSource = new ReplaySubject<void>()
  public logoutSignalled: boolean = false
  private logoutOngoing: boolean = false
  public accessToken?: string

  public onLogin$ = this.onLoginSource.asObservable()
  public afterLogin$ = this.afterLoginSource.asObservable()
  public onLogout$ = this.onLogoutSource.asObservable()
  public onLogoutComplete$ = this.onLogoutCompleteSource.asObservable()

  private readonly authenticatedSubject = new BehaviorSubject<AuthenticationStatus>(AuthenticationStatus.NotChecked);
  private recoverAuth$?: Observable<AuthenticationStatus>;

  constructor(
      private readonly httpClient: HttpClient,
      private readonly dataService: DataService,
      private readonly routingService: RoutingService,
      private readonly popupService: PopupService
    ) {
    // Handle login event
    this.onLogin$.subscribe((info) => {
      this.dataService.cookiePath = info.path
      const accessToken = info.tokens.access_token
      this.authenticate(accessToken, info.path)
      this.signalAfterLogin(info)
    })
    // Handle post-login event
    this.afterLogin$.subscribe((loginInfo) => {
      this.routingService.toStartPage(loginInfo.userId)
    })
    // Handle logout event
    this.onLogout$.subscribe((info) => {
			if (!this.logoutOngoing && (info.full || this.isAuthenticated())) {
        const clearAllSessionInfo = info.full
        this.logoutOngoing = true
        this.httpClient.post(
          this.dataService.completePath(ROUTES.controllers.AuthenticationService.logout().url),
          Utils.objectToFormRequest({full: clearAllSessionInfo}).toString(),
          { headers: Utils.createHttpHeaders(this.accessToken) }
        ).subscribe(() => {
          console.debug('Successfully signalled logout')
        }).add(() => {
          this.dataService.destroy(clearAllSessionInfo)
					if (!info || !info.keepLoginOption) {
            this.dataService.clearLoginOption()
          }
					this.deAuthenticate()
					if (info.full) {
            const url = window.location.href
            if (info.fromExpiry) {
              // Go to login screen.
              window.location.href = url.substring(0, url.indexOf('#'))
            } else {
              // Go to welcome screen.
              window.location.href = url.substring(0, url.indexOf('app#'))
            }
          } else {
            this.routingService.toLogin().finally(() => {
              this.onLogoutCompleteSource.next()
            })
          }
        })
      }
    })
  }

  recoverAuthenticationStatus(): Observable<AuthenticationStatus> {
    if (this.authenticatedSubject.value != AuthenticationStatus.NotChecked) {
      return of(this.authenticatedSubject.value);
    }
    if (!this.recoverAuth$) {
      this.recoverAuth$ = this.recoverAuthenticationStatusFromSession().pipe(
        tap(status => this.authenticatedSubject.next(status)),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.recoverAuth$;
  }

  private recoverAuthenticationStatusFromSession(): Observable<AuthenticationStatus> {
    return this.httpClient.get(this.dataService.completePath(ROUTES.controllers.AuthenticationService.retrieveAccessToken().url), { headers: Utils.createHttpHeaders()}).pipe(
      map(result => {
        if (this.isLoginOk(result)) {
          this.authenticate(result.access_token);
          return AuthenticationStatus.AuthenticatedWithAccessToken;
        } else if (this.isProfileExists(result)) {
          return AuthenticationStatus.Authenticated;
        } else {
          return AuthenticationStatus.NotAuthenticated;
        }
      }),
      catchError(() => of(AuthenticationStatus.NotAuthenticated))
    );
  }

  private isLoginOk(obj: LoginResultOk|any): obj is LoginResultOk {
    return obj != undefined && obj.access_token != undefined
  }

  private isProfileExists(obj: { profileExists: boolean }|any): obj is { profileExists: boolean }  {
    return obj != undefined && obj.profileExists != undefined
  }

  signalLogin(info: LoginEventInfo) {
    this.onLoginSource.next(info)
  }

  signalLogout(info: LogoutEventInfo) {
    // Make sure any persistent open popups are closed
    this.popupService.closeAll()
    this.onLogoutSource.next(info)
  }

  private signalAfterLogin(info: LoginEventInfo) {
    this.afterLoginSource.next(info)
  }

  private authenticate(accessToken: string, cookiePath?: string) {
		this.authenticatedSubject.next(AuthenticationStatus.AuthenticatedWithAccessToken)
		this.logoutSignalled = false
    this.accessToken = accessToken
  }

	private deAuthenticate() {
		this.authenticatedSubject.next(AuthenticationStatus.NotChecked)
    this.recoverAuth$ = undefined
		this.logoutOngoing = false
  }

	isAuthenticated(): boolean {
    return this.authenticatedSubject.value === AuthenticationStatus.AuthenticatedWithAccessToken
  }

  getAuthenticatedStatus(): AuthenticationStatus {
    return this.authenticatedSubject.value
  }

}
