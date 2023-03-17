import { Injectable } from '@angular/core';
import { Observable, Subscriber, throwError } from 'rxjs';
import { ErrorDataArrayBuffer } from '../types/error-data-array-buffer.type';
import { ErrorData } from '../types/error-data.type';
import { ConfirmationDialogService } from './confirmation-dialog.service';
import { BsModalService } from 'ngx-bootstrap/modal'
import { Constants } from '../common/constants';
import { ErrorComponent } from '../modals/error/error.component';
import { BaseRestService } from './base-rest.service';
import { ROUTES } from '../common/global';
import { HttpErrorResponse } from '@angular/common/http';
import { Organisation } from '../types/organisation.type';
import { ErrorTemplate } from '../types/error-template';
import { ErrorDescription } from '../types/error-description';

@Injectable({
  providedIn: 'root'
})
export class ErrorService {

  constructor(
    private confirmationDialogService: ConfirmationDialogService,
    private modalService: BsModalService,
    private baseRestService: BaseRestService
  ) { }

  customErrorHandler(title: string|undefined, message: string, error: string|ErrorData|ErrorDataArrayBuffer): Observable<any> {
    this.showSimpleErrorMessage(title, message)
    return throwError(() => error)
  }

  showSimpleErrorMessage(title: string|undefined, message: string) {
    let error: ErrorData = {
      statusText: title,
      error: { error_description: message }
    }
    this.showErrorMessage(error)
  }

  private isErrorDataArrayBuffer(error: string|ErrorData|ErrorDataArrayBuffer): error is ErrorDataArrayBuffer {
    return (error as ErrorDataArrayBuffer).data !== undefined && (error as ErrorDataArrayBuffer).data instanceof ArrayBuffer
  }

  showErrorMessage(error: string|ErrorData|ErrorDataArrayBuffer): void {
    this.showErrorMessageWithRetry(error, false).subscribe(() => {})
  }

  private fromHttpErrorResponse(error: HttpErrorResponse) {
    let message = 'Error occurred during internal service call'
    if (error.error?.error?.message != undefined) {
      message += ': '+error.error.error.message
    }
    return message
  }

  showErrorMessageWithRetry(error: undefined|string|ErrorData|ErrorDataArrayBuffer|HttpErrorResponse, withRetry: boolean): Observable<boolean> {
    return new Observable<boolean>((observer) => {
      if (this.confirmationDialogService.sessionNotificationOpen) {
        observer.next()
        observer.complete()
      } else {
        let errorObj:ErrorData
        if (!error) {
          errorObj = {}
        } else {
          if (typeof error == 'string' || error instanceof String) {
            errorObj = {
              error: {
                error_description: <string>error
              }
            }
          } else if (error instanceof HttpErrorResponse) {
            let errorInfoToUse: ErrorDescription = {}
            if (error.error != undefined) {
              if (error.error instanceof ArrayBuffer) {
                const decoder = new TextDecoder("utf-8")
                const decoded = JSON.parse(decoder.decode(error.error)) as ErrorDescription|undefined
                if (decoded?.error_description != undefined) {
                  errorInfoToUse = decoded
                }
              } else if (typeof error.error == 'string' || error.error instanceof String) {
                errorInfoToUse = JSON.parse(error.error as string)
              } else {
                errorInfoToUse = error.error
              }
            }
            errorObj = { 
              error: {
                error_code: errorInfoToUse.error_code,
                error_description: errorInfoToUse.error_description,
                error_id: errorInfoToUse.error_id
              }
            }
            if (errorObj.error?.error_description == undefined) {
              errorObj.error!.error_description = this.fromHttpErrorResponse(error)
            }
          } else if (this.isErrorDataArrayBuffer(error)) {
            errorObj = {
              error: {
                error_description: 'An error occurred while processing binary data'
              }
            }
          } else {
            errorObj = error as ErrorData
          }
        }
        if (errorObj.error && errorObj.error.error_id) {
          // An error ID is assigned only to unexpected errors
          if (errorObj.template == undefined) {
            this.getVendorProfile().subscribe((vendor) => {
              if (vendor.errorTemplates) {
                errorObj.template = vendor.errorTemplates.content
                this.openModal(errorObj, withRetry, observer)
              } else {
                let communityId = vendor.community
                this.getCommunityDefaultErrorTemplate(communityId).subscribe((data) => {
                  if (data.exists == true) {
                    errorObj.template = data.content
                  }
                  this.openModal(errorObj, withRetry, observer)
                })
              }
            })
          } else {
            this.openModal(errorObj, withRetry, observer)
          }
        } else {
          // Expected errors (e.g. validation errors) that have clear error messages
          this.openModal(errorObj, withRetry, observer)
        }
      }
    })
  }

  private getCommunityDefaultErrorTemplate(communityId: number) {
    return this.baseRestService.get<ErrorTemplate>({
      path: ROUTES.controllers.ErrorTemplateService.getCommunityDefaultErrorTemplate().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  private getVendorProfile(): Observable<any> {
    return this.baseRestService.get<Organisation>({
      path: ROUTES.controllers.AccountService.getVendorProfile().url,
      authenticate: true
    })
  }

  private openModal(error: ErrorData, withRetry: boolean, observer: Subscriber<boolean>) {
    console.error('Error caught: ' + JSON.stringify(error))
    if (!error.template || error.template == '') {
      if (error.error && error.error.error_id) {
        error.template = 
          '<p>'+Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</p>' +
          '<p><b>Error reference: </b>'+Constants.PLACEHOLDER__ERROR_ID+'</p>'
      } else {
        error.template = '<p>'+Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</p>'
      }
    }
    const modal = this.modalService.show(ErrorComponent, {
      initialState: {
        error: error,
        withRetry: withRetry
      }
    })
    modal.content!.result.subscribe((closed: boolean) => {
      if (closed) {
        // Closed
        observer.next(true)
        observer.complete()
      } else {
        // Dismissed
        if (withRetry) {
          // Do not retry
          observer.next(false)
        } else {
          observer.next(true)
        }
        observer.complete()
      }
    })
  }
}
