import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { LogoutEventInfo } from '../types/logout-event-info.type'
import { LoginEventInfo } from "../types/login-event-info.type";
import { Constants } from './../common/constants'
import { CookieService } from 'ngx-cookie-service'
import { DataService } from './data.service'
import { Router } from '@angular/router';
import { Utils } from '../common/utils';
import { ROUTES } from '../common/global';

@Injectable({
  providedIn: 'root'
})
export class AuthProviderService {

  private onLoginSource = new Subject<LoginEventInfo>()
  private afterLoginSource = new Subject<any>()
  private onLogoutSource = new Subject<LogoutEventInfo>()
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

  constructor(
      private cookieService: CookieService,
      private httpClient: HttpClient,
      private dataService: DataService,
      private router: Router
    ) {
    // Check if access token is set in cookies
    let accessTokenValue = cookieService.get(this.atKey)
    if (accessTokenValue) {
      this.authenticate(accessTokenValue)
    }
    // Handle login event
    this.onLogin$.subscribe((info) => {
      let accessToken  = info.tokens.access_token
			let cookieOptions: any = {
        path: info.path,
        sameSite: 'strict'
      }
			let protocol = window.location.protocol
			if (protocol && (protocol.toLowerCase() == 'https')) {
        cookieOptions.secure = true
      }
			if (info.remember) {
				let expiryDate = new Date(Date.now() + Constants.TOKEN_COOKIE_EXPIRE)
				cookieOptions.expires = expiryDate
      }
      this.cookieService.set(this.atKey, accessToken, cookieOptions)
      this.authenticate(accessToken, info.path)
      this.signalAfterLogin()
    })
    // Handle post-login event
    this.afterLogin$.subscribe(() => {
      this.router.navigate(['home'])
    })
    // Handle logout event
    this.onLogout$.subscribe((info) => {
			if (!this.logoutOngoing && (info.full || this.isAuthenticated())) {
        this.logoutOngoing = true
        this.httpClient.post(
          Utils.completePath(ROUTES.controllers.AuthenticationService.logout().url), 
          Utils.objectToFormRequest({full: info.full}).toString(),
          {
            headers: Utils.createHttpHeaders(this.accessToken)
          }
        ).subscribe(() => {
          console.debug('Successfully signalled logout')
        }).add(() => {
          this.dataService.destroy()
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
            this.router.navigate(['login'])
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