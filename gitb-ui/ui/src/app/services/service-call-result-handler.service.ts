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
import {ServiceCallResult} from '../types/service-call-result';
import {CodeEditorModalComponent} from '../components/code-editor-modal/code-editor-modal.component';
import {DataService} from './data.service';
import {BsModalService} from 'ngx-bootstrap/modal';
import {ErrorService} from './error.service';
import {Alert} from '../types/alert.type';

@Injectable({
  providedIn: 'root'
})
export class ServiceCallResultHandlerService {

  constructor(
    private readonly dataService: DataService,
    private readonly modalService: BsModalService,
    private readonly errorService: ErrorService
  ) { }

  handleResult(result: ServiceCallResult) {
    if (result.success) {
      let valueToShow = ''
      if (result.texts && result.texts.length > 0) {
        valueToShow = result.texts[0]
        if (result.contentType == 'application/json') {
          try {
            valueToShow = this.dataService.prettifyJSON(valueToShow)
          } catch (e) {
            console.warn('Response reported as JSON but could not be parsed')
            valueToShow = result.texts[0]
          }
        }
      }
      const alert: Alert = {
        msg: "The service call succeeded. The editor below includes the service's response.",
        type: 'success'
      }
      this.modalService.show(CodeEditorModalComponent, {
        class: 'modal-lg',
        initialState: {
          documentName: 'Service test result',
          alert: alert,
          editorOptions: {
            value: valueToShow,
            readOnly: true,
            lineNumbers: true,
            smartIndent: false,
            electricChars: false,
            mode: result.contentType
          }
        }
      })
    } else {
      const alert: Alert = {
        msg: 'The service call failed. The editor below includes the errors collected during the call.',
        type: 'danger'
      }
      this.errorService.popupErrorsArray(result.texts, "Service test result", result.contentType, alert)
    }
  }

}
