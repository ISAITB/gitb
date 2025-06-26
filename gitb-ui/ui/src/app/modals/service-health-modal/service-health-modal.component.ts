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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {HealthCardInfo} from '../../types/health-card-info';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {HealthStatus} from '../../types/health-status';
import {BaseComponent} from '../../pages/base-component.component';
import {MarkdownService} from 'ngx-markdown';
import {marked} from 'marked';
import Parser = marked.Parser;

@Component({
  selector: 'app-service-health-modal',
  standalone: false,
  templateUrl: './service-health-modal.component.html',
  styleUrl: './service-health-modal.component.less'
})
export class ServiceHealthModalComponent extends BaseComponent implements OnInit {

  @Input() serviceInfo!: HealthCardInfo
  @Output() updated = new EventEmitter<void>();
  checkPending = false

  constructor(
    private readonly modalRef: BsModalRef,
    private readonly markdownService: MarkdownService,
  ) {
    super()
  }

  ngOnInit() {
    this.markdownService.renderer.link = ({ href, title, tokens }) => {
      return `<a href="${href}" title="${title || ''}" target="_blank" rel="noopener noreferrer">${Parser.parseInline(tokens)}</a>`;
    }
    this.updateSummaryAlert()
  }

  private updateSummaryAlert() {
    this.clearAlerts()
    if (this.serviceInfo.info) {
      switch (this.serviceInfo.info.status) {
        case HealthStatus.OK: this.addAlertSuccess(this.serviceInfo.info.summary); break;
        case HealthStatus.WARNING: this.addAlertWarning(this.serviceInfo.info.summary); break;
        case HealthStatus.ERROR: this.addAlertError(this.serviceInfo.info.summary); break;
        case HealthStatus.INFO: this.addAlertInfo(this.serviceInfo.info.summary); break;
        case HealthStatus.UNKNOWN: this.addAlertInfo(this.serviceInfo.info.summary); break;
      }
    }
  }

  close() {
    this.modalRef.hide()
  }

  checkStatus() {
    this.checkPending = true
    this.serviceInfo.checkFunction().subscribe((result) => {
      this.serviceInfo.info = result
    }).add(() => {
      this.updateSummaryAlert()
      this.checkPending = false
      this.updated.emit()
    })
  }

}
