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

import {Injectable} from '@angular/core';
import {HtmlComponent} from '../modals/html/html.component';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

@Injectable({
  providedIn: 'root'
})
export class HtmlService {

  constructor(
    private readonly modalService: NgbModal
  ) { }

  showHtml(headerText: string|undefined, html: string, customClass?: string):void {
    if (customClass == undefined) {
      customClass = 'modal-lg'
    }
    const modal = this.modalService.open(HtmlComponent, { modalDialogClass: customClass })
    const modalInstance = modal.componentInstance as HtmlComponent
    modalInstance.headerText = headerText
    modalInstance.html = html
  }

}
