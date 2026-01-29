/*
 * Copyright (C) 2026 European Union
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

import {Component, Input, OnInit} from '@angular/core';
import {Constants} from '../../common/constants';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-confirmation',
    templateUrl: './confirmation.component.html',
    styles: [],
    standalone: false
})
export class ConfirmationComponent implements OnInit {

  @Input() headerText = ''
  @Input() bodyText = ''
  @Input() actionButtonText = ''
  @Input() actionButtonIcon?: string
  @Input() closeButtonText = ''
  @Input() closeButtonIcon?: string
  @Input() sameStyles? = true
  @Input() oneButton = false
  @Input() actionClass = 'btn btn-secondary'

  constructor(public readonly modalRef: NgbActiveModal) { }

  ngOnInit(): void {
    if (this.actionButtonIcon == undefined) {
      this.actionButtonIcon = Constants.BUTTON_ICON.CONFIRM
    }
    if (this.closeButtonIcon == undefined) {
      this.closeButtonIcon = Constants.BUTTON_ICON.CANCEL
    }
  }

  ok() {
    this.modalRef.close(true)
  }

  cancel() {
    this.modalRef.close(false)
  }

  cancelClass(): string {
    if (this.sameStyles) {
      return this.actionClass
    } else {
      return "btn btn-secondary"
    }
  }

}
