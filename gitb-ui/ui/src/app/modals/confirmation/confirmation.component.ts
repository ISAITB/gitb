import { Component, OnInit, EventEmitter } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal'

@Component({
  selector: 'app-confirmation',
  templateUrl: './confirmation.component.html',
  styles: [
  ]
})
export class ConfirmationComponent implements OnInit {

  public result = new EventEmitter<boolean>()

  headerText = ''
  bodyText = ''
  actionButtonText = ''
  closeButtonText = ''
  sameStyles = true
  oneButton = false

  constructor(public modalRef: BsModalRef) { }

  ngOnInit(): void {
  }

  ok() {
    this.result.emit(true)
    this.modalRef.hide()
  }

  cancel() {
    this.result.emit(false)
    this.modalRef.hide()
  }

  okClass(): string {
    return "btn btn-default"
  }

  cancelClass(): string {
    if (this.sameStyles) {
      return this.okClass()
    } else {
      return "btn btn-default"  
    }
  }

}
