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

import {Component, EventEmitter, Input, ViewChild, ChangeDetectionStrategy} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {BaseComponent} from '../../pages/base-component.component';
import {Constants} from '../../common/constants';
import {SessionLogViewerComponent} from '../session-log-viewer/session-log-viewer.component';

@Component({
    selector: 'app-session-log-modal',
    templateUrl: './session-log-modal.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class SessionLogModalComponent extends BaseComponent {

  @Input() messages!: string[]
  @Input() messageEmitter?: EventEmitter<string>

  @ViewChild('viewer') viewer?: SessionLogViewerComponent

  tail = true
  Constants = Constants

  constructor(private readonly modalRef: NgbActiveModal) { super() }

  close() {
    this.modalRef.dismiss()
  }

  copyToClipboard() {
    this.viewer?.copyToClipboard()
  }

  download() {
    this.viewer?.download()
  }

}
