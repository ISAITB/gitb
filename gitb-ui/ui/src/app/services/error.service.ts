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
import { mergeMap, Observable, of, Subscriber, throwError } from 'rxjs';
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
import { DataService } from './data.service';
import { CodeEditorModalComponent } from '../components/code-editor-modal/code-editor-modal.component';
import {Alert} from '../types/alert.type';

@Injectable({
  providedIn: 'root'
})
export class ErrorService {

  private errorCurrentlyDisplayed = false

  constructor(
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly modalService: BsModalService,
    private readonly baseRestService: BaseRestService,
    private readonly dataService: DataService
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
      if (this.errorCurrentlyDisplayed) {
        observer.next(true)
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
            try {
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
            } catch (e) {
              if (error.statusText) {
                errorInfoToUse.error_description = `An unexpected error occurred [${error.statusText}].`
              } else {
                errorInfoToUse.error_description = "An unexpected error occurred."
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
    if (!this.errorCurrentlyDisplayed) {
      this.errorCurrentlyDisplayed = true
      if (!error.template || error.template == '') {
        if (error.error && error.error.error_id) {
          error.template =
            '<p>'+Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</p>' +
            '<span><b>Error reference: </b>'+Constants.PLACEHOLDER__ERROR_ID+'</span>'
        } else {
          error.template = '<span>'+Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</span>'
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
        this.errorCurrentlyDisplayed = false
      })
    }
  }

  popupErrorsArray(errorArray: string[]|undefined, title?: string, contentType?: string, alert?: Alert) {
    let content = this.dataService.errorArrayToString(errorArray)
    if (contentType == undefined) {
      contentType = 'text/plain'
    } else if (contentType == 'application/json') {
      content = this.dataService.prettifyJSON(content)
    }
    let titleToUse = title
    if (titleToUse == undefined) {
      titleToUse = 'Error message(s)'
    }
    this.modalService.show(CodeEditorModalComponent, {
      class: 'modal-lg',
      initialState: {
        alert: alert,
        documentName: titleToUse,
        editorOptions: {
          value: content,
          readOnly: true,
          copy: true,
          lineNumbers: false,
          smartIndent: false,
          electricChars: false,
          styleClass: 'editor-short',
          mode: contentType
        }
      }
    })
  }

  showInvalidSessionNotification(): Observable<boolean> {
    if (!this.errorCurrentlyDisplayed) {
      this.errorCurrentlyDisplayed = true
      return this.confirmationDialogService.notified("Invalid session", "Your current session is invalid. You will now return to the login screen to reconnect.", "Close").pipe(
        mergeMap(() => {
          this.errorCurrentlyDisplayed = false
          return of(true)
        })
      )
    } else {
      return of(false)
    }
  }

  showUnauthorisedAccessError(): Observable<boolean> {
    if (!this.errorCurrentlyDisplayed) {
      this.errorCurrentlyDisplayed = true
      return this.confirmationDialogService.notified("Access forbidden", "You don't have access to view this information. Close this popup to return to the home page.", "Close").pipe(
        mergeMap(() => {
          this.errorCurrentlyDisplayed = false
          return of(true)
        })
      )
    } else {
      return of(false)
    }
  }

}
