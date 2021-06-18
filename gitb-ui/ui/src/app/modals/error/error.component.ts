import { Component, EventEmitter, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ErrorData } from 'src/app/types/error-data.type';
import { BsModalRef } from 'ngx-bootstrap/modal'

@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  styles: [
  ]
})
export class ErrorComponent implements OnInit {

  public result = new EventEmitter<boolean>()

  title = 'Unexpected Error'
  error?: ErrorData
  withRetry = false
  errorMessage = '-'
  messageToShow?: string

  constructor(public modalRef: BsModalRef) { }

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
