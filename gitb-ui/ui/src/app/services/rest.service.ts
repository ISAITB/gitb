import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';

import { HttpRequestConfig } from '../types/http-request-config.type'
import { catchError, map, mergeMap, share } from 'rxjs/operators';
import { AuthProviderService } from './auth-provider.service'
import { ErrorService } from './error.service';
import { BaseRestService } from './base-rest.service';
import { DataService } from './data.service';
import { RoutingService } from './routing.service';

@Injectable({
  providedIn: 'root'
})
export class RestService {

  constructor(
    private baseRestService: BaseRestService,
    private authProviderService: AuthProviderService,
    private errorService: ErrorService,
    private dataService: DataService,
    private routingService: RoutingService
  ) { }

  private call<T>(callFn: () => Observable<T>, errorHandler?: (_:any) => Observable<any>): Observable<T> {
    return callFn().pipe(catchError(error => this.handleError(error, errorHandler)))
  }

  get<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(() => {
      return this.baseRestService.get<T>(config)
    }, config.errorHandler)
  }

  put<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(() => {
      return this.baseRestService.put<T>(config)
    }, config.errorHandler)
  }

  post<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(() => {
      return this.baseRestService.post<T>(config)
    }, config.errorHandler)
  }

  delete<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(() => {
      return this.baseRestService.delete<T>(config)
    }, config.errorHandler)
  }

  private errorDueToSsoExpiryAfterPageRefresh(response: any): boolean {
    /*
     * Due to the sequence of calls on the ProfileResolver, the DataService configuration will already be loaded before we reach this point.
     * The ProfileResolver is evaluated before every route in the application.
     *
     * If we are in SSO mode the authentication is driven server-side. We have two cases here:
     * - Case 1: The expiry was detected after the user did an action in the frontend app. In this case, we are executing a REST call
     *           and catch a 401 error. As we were already in the frontend app we know that this was due to an expiry and can signal it as such.
     *           In addition, we can show a popup informing the user accordingly.
     * - Case 2: The expiry was detected after the user did a page refresh. In this case, as the path is protected server-side, the first thing
     *           that happens is a server-side re-authentication. We then execute the frontend code routing in which case the ProfileResolver
     *           kicks in and results in a 401 error when it loads the user's profile information. This happens after the app's configuration has
     *           been loaded, as in that case we don't force authentication using the access token header. As such, in this case we have a 401
     *           error reported by the server that however is specifically due to an invalid authorisation token (error code 203). This means
     *           that the overall SSO authorisation worked (otherwise we would have a generic 401 error with no detail), but we need to refresh
     *           the token. This will take place anyway by signalling a logout, but by detecting this case we avoid showing a session expiry
     *           popup after the user has already done the SSO authentication.
     */
    return this.dataService.configurationLoaded && this.dataService.configuration.ssoEnabled &&
      response && response.error &&
      response.error.error_code == "203" // INVALID_AUTHORIZATION_HEADER
  }

  private handleError(error: any, errorHandler?: (_:any) => Observable<any>) {
    let result: Observable<any>
    if (error && error.status && error.status == 401) {
      // Always handle authorization-related errors.
      result = new Observable<any>((observer) => {
        if (!this.authProviderService.logoutSignalled) {
          this.authProviderService.logoutSignalled = true
          if (this.errorDueToSsoExpiryAfterPageRefresh(error)) {
            this.authProviderService.signalLogout({full: true, fromExpiry: true})
            observer.next()
            observer.complete()
          } else {
            this.errorService.showInvalidSessionNotification().subscribe((processed) => {
              if (processed) {
                this.authProviderService.signalLogout({full: true, fromExpiry: true})
              }
              observer.next()
              observer.complete()
            })
          }
        } else {
          observer.next()
          observer.complete()
        }
      })
    } else if (error && error.status && error.status == 403) {
      console.warn(`Access forbidden: ${error.error?.error_description}`)
      result = this.errorService.showUnauthorisedAccessError().pipe(
        mergeMap((shown) => {
          if (shown) {
            this.routingService.toHome()
          }
          return throwError(() => error)
        }
      ), share())
    } else {
      if (errorHandler) {
        // Custom error handling.
        result = errorHandler(error)
      } else {
        // Default handling - Display error popup and log error.
        this.errorService.showErrorMessage(error)
        result = throwError(() => error)
      }
    }
    return result
  }

}
