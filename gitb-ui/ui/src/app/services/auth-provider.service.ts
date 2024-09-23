import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { LogoutEventInfo } from '../types/logout-event-info.type'
import { LoginEventInfo } from "../types/login-event-info.type";
import { Constants } from './../common/constants'
import { CookieService } from 'ngx-cookie-service'
import { DataService } from './data.service'
import { Utils } from '../common/utils';
import { ROUTES } from '../common/global';
import { RoutingService } from './routing.service';

@Injectable({
  providedIn: 'root'
})
export class AuthProviderService {

  private onLoginSource = new Subject<LoginEventInfo>()
  private afterLoginSource = new Subject<LoginEventInfo>()
  private onLogoutSource = new Subject<LogoutEventInfo>()
  private onLogoutCompleteSource = new Subject<void>()
  private authenticated: boolean = false
  public logoutSignalled: boolean = false
  private logoutOngoing: boolean = false
  private cookiePath?: string
  private atKey = Constants.ACCESS_TOKEN_COOKIE_KEY
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
      this.signalAfterLogin(info)
    })
    // Handle post-login event
    this.afterLogin$.subscribe((loginInfo) => {
      this.routingService.toStartPage(loginInfo.userId!)
    })
    // Handle logout event
    this.onLogout$.subscribe((info) => {
			if (!this.logoutOngoing && (info.full || this.isAuthenticated())) {
        const clearAllSessionInfo = info.full && (info.fromExpiry == undefined || !info.fromExpiry)
        this.logoutOngoing = true
        let logout$ = this.httpClient.post(
          this.dataService.completePath(ROUTES.controllers.AuthenticationService.logout().url), 
          Utils.objectToFormRequest({full: clearAllSessionInfo}).toString(),
          {
            headers: Utils.createHttpHeaders(this.accessToken)
          }
        )
        logout$.subscribe(() => {
          console.debug('Successfully signalled logout')
        }).add(() => {
          this.dataService.destroy(clearAllSessionInfo)
          this.cookieService.delete(this.atKey)
					if (this.cookiePath) {
            this.cookieService.delete(this.atKey, this.cookiePath)
          }
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

  signalLogin(info: LoginEventInfo) {
    this.onLoginSource.next(info)
  }

  signalLogout(info: LogoutEventInfo) {
    this.onLogoutSource.next(info)
  }

  signalAfterLogin(info: LoginEventInfo) {
    this.afterLoginSource.next(info)
  }

  authenticate(accessToken: string, cookiePath?: string) {
		this.authenticated = true
		this.logoutSignalled = false
    this.cookiePath = cookiePath
    this.accessToken = accessToken
  }

	deAuthenticate() {
		this.authenticated = false
		this.logoutOngoing = false
    delete this.cookiePath
  }

	isAuthenticated(): boolean {
    return this.authenticated
  }

}