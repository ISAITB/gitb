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
