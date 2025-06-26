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

import { Component, EventEmitter, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ErrorData } from 'src/app/types/error-data.type';
import { BsModalRef } from 'ngx-bootstrap/modal'

@Component({
    selector: 'app-error',
    templateUrl: './error.component.html',
    styles: [],
    standalone: false
})
export class ErrorComponent implements OnInit {

  public result = new EventEmitter<boolean>()

  title = 'Unexpected Error'
  error?: ErrorData
  withRetry = false
  errorMessage = '-'
  messageToShow?: string

  constructor(
    public readonly modalRef: BsModalRef
  ) { }

  ngOnInit(): void {
    if (this.error?.statusText) {
      this.title = this.error.statusText
    }
    if (this.error) {
      if (this.error.error && this.error.error.error_description) {
        this.errorMessage = this.error.error.error_description
      }
      let errorContent = this.error.template!.split(Constants.PLACEHOLDER__ERROR_DESCRIPTION).join(this.errorMessage)
      if (this.error.error && this.error.error.error_id) {
        errorContent = errorContent.split(Constants.PLACEHOLDER__ERROR_ID).join(this.error.error.error_id)
      }
      this.messageToShow = errorContent
    }
  }

  retry() {
    this.result.emit(true)
    this.modalRef.hide()
  }

  close() {
    this.result.emit(false)
    this.modalRef.hide()
  }

}
