import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { LogoutEventInfo } from '../types/logout-event-info.type'
import { LoginEventInfo } from "../types/login-event-info.type";
import { Constants } from './../common/constants'
import { CookieOptions, CookieService } from 'ngx-cookie-service'
import { DataService } from './data.service'
import { Utils } from '../common/utils';
import { ROUTES } from '../common/global';
import { RoutingService } from './routing.service';

@Injectable({
  providedIn: 'root'
})
export class AuthProviderService {

  private onLoginSource = new Subject<LoginEventInfo>()
  private afterLoginSource = new Subject<any>()
  private onLogoutSource = new Subject<LogoutEventInfo>()
  private onLogoutCompleteSource = new Subject<void>()
  private authenticated: boolean = false
  public logoutSignalled: boolean = false
  private logoutOngoing: boolean = false
  private cookiePath?: string
  private atKey = Constants.ACCESS_TOKEN_COOKIE_KEY
  private loginOptionKey = Constants.LOGIN_OPTION_COOKIE_KEY
  public accessToken?: string

  public onLogin$ = this.onLoginSource.asObservable()
  public afterLogin$ = this.afterLoginSource.asObservable()
  public onLogout$ = this.onLogoutSource.asObservable()
  public onLogoutComplete$ = this.onLogoutCompleteSource.asObservable()

  constructor(
      private cookieService: CookieService,
      private httpClient: HttpClient,
      private dataService: DataService,
      private routingService: RoutingService
    ) {
    // Check if access token is set in cookies
    let accessTokenValue = cookieService.get(this.atKey)
    if (accessTokenValue) {
      this.authenticate(accessTokenValue)
    }
    // Handle login event
    this.onLogin$.subscribe((info) => {
      this.dataService.cookiePath = info.path
      const accessToken = info.tokens.access_token
      let expiryDate: Date|undefined
			if (info.remember) {
				expiryDate = new Date(Date.now() + Constants.TOKEN_COOKIE_EXPIRE)
      }
      this.dataService.setCookie(this.atKey, accessToken, expiryDate)
      this.authenticate(accessToken, info.path)
      this.signalAfterLogin()
    })
    // Handle post-login event
    this.afterLogin$.subscribe(() => {
      this.routingService.toHome()
    })
    // Handle logout event
    this.onLogout$.subscribe((info) => {
			if (!this.logoutOngoing && (info.full || this.isAuthenticated())) {
        this.logoutOngoing = true
        this.httpClient.post(
          this.dataService.completePath(ROUTES.controllers.AuthenticationService.logout().url), 
          Utils.objectToFormRequest({full: info.full}).toString(),
          {
            headers: Utils.createHttpHeaders(this.accessToken)
          }
        ).subscribe(() => {
          console.debug('Successfully signalled logout')
        }).add(() => {
          this.dataService.destroy()
          if (localStorage) {
            localStorage.clear()
          }
          this.cookieService.delete(this.atKey)
					if (this.cookiePath) {
            this.cookieService.delete(this.atKey, this.cookiePath)
          }
					if (!info || !info.keepLoginOption) {
            this.cookieService.delete(this.loginOptionKey)
          }
					this.deauthenticate()
					if (info.full) {
            let url = window.location.href
            window.location.href = url.substring(0, url.indexOf('app#'))
          } else {
            this.routingService.toLogin().finally(() => {
              this.onLogoutCompleteSource.next()
            })
          }
        })
      }
    })
  }

  signalLogin(info: LoginEventInfo) {
    this.onLoginSource.next(info)
  }

  signalLogout(info: LogoutEventInfo) {
    this.onLogoutSource.next(info)
  }

  signalAfterLogin() {
    this.afterLoginSource.next({})
  }

  authenticate(accessToken: string, cookiePath?: string) {
		this.authenticated = true
		this.logoutSignalled = false
    this.cookiePath = cookiePath
    this.accessToken = accessToken
  }

	deauthenticate() {
		this.authenticated = false
		this.logoutOngoing = false
    delete this.cookiePath
  }

	isAuthenticated(): boolean {
    return this.authenticated
  }

}