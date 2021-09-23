import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';

import { HttpRequestConfig } from '../types/http-request-config.type'
import { catchError } from 'rxjs/operators';
import { AuthProviderService } from './auth-provider.service'
import { ConfirmationDialogService } from './confirmation-dialog.service'
import { ErrorService } from './error.service';
import { BaseRestService } from './base-rest.service';

@Injectable({
  providedIn: 'root'
})
export class RestService {

  constructor(
    private baseRestService: BaseRestService,
    private authProviderService: AuthProviderService,
    private confirmationDialogService: ConfirmationDialogService,
    private errorService: ErrorService) { }

  private call<T>(callFn: () => Observable<T>, errorHandler?: (_:any) => Observable<any>): Observable<T> {
    return callFn().pipe(catchError(error => this.handleError(error, errorHandler)))
  }

  get<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(() => {
      return this.baseRestService.get<T>(config)
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

  private handleError(error: any, errorHandler?: (_:any) => Observable<any>) {
    let result: Observable<any>
    if (error && error.status && error.status == 401) {
      // Always handle authorization-related errors.
      result = new Observable<any>((observer) => {
        if (!this.authProviderService.logoutSignalled) {
          this.authProviderService.logoutSignalled = true
          this.confirmationDialogService.invalidSessionNotification().subscribe((processed) => {
            if (processed) {
              this.authProviderService.signalLogout({full: true})
            }
            observer.next()
            observer.complete()
          })
        } else {
          observer.next()
          observer.complete()
        }
      })
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