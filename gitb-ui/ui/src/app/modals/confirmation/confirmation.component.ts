import { Component, OnInit, EventEmitter } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal'

@Component({
    selector: 'app-confirmation',
    templateUrl: './confirmation.component.html',
    styles: [],
    standalone: false
})
export class ConfirmationComponent implements OnInit {

  public result = new EventEmitter<boolean>()

  headerText = ''
  bodyText = ''
  actionButtonText = ''
  closeButtonText = ''
  sameStyles = true
  oneButton = false
  actionClass = 'btn btn-secondary'

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

  cancelClass(): string {
    if (this.sameStyles) {
      return this.actionClass
    } else {
      return "btn btn-secondary"  
    }
  }

}
