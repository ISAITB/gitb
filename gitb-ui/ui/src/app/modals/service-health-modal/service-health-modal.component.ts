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

import {Component, DoCheck, EventEmitter, Input, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {HealthInfo} from '../../types/health-info';
import {HealthCardInfo} from '../../types/health-card-info';
import {HealthStatus} from '../../types/health-status';
import {BaseComponent} from '../../pages/base-component.component';
import {MarkdownService} from 'ngx-markdown';
import {marked} from 'marked';
import {Constants} from '../../common/constants';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import Parser = marked.Parser;

@Component({
  selector: 'app-service-health-modal',
  standalone: false,
  templateUrl: './service-health-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './service-health-modal.component.less'
})
export class ServiceHealthModalComponent extends BaseComponent implements OnInit, DoCheck {

  @Input() serviceInfo!: HealthCardInfo
  @Input() updated!: EventEmitter<void>

  checkPending = false
  // The displayed result. This is retained if the service is checked again in the background (while the modal is open).
  shownInfo!: HealthInfo

  constructor(
    private readonly modalRef: NgbActiveModal,
    private readonly markdownService: MarkdownService,
  ) {
    super()
  }

  ngOnInit() {
    this.markdownService.renderer.link = ({ href, title, tokens }) => {
      return `<a href="${href}" title="${title || ''}" target="_blank" rel="noopener noreferrer">${Parser.parseInline(tokens)}</a>`;
    }
    this.show(this.serviceInfo.info)
  }

  ngDoCheck() {
    // If opened while a check was in progress, there is no result to retain yet: display it once received.
    if (this.shownInfo?.status == HealthStatus.PENDING && this.serviceInfo.info.status != HealthStatus.PENDING) {
      this.show(this.serviceInfo.info)
    }
  }

  private show(info: HealthInfo) {
    this.shownInfo = info
    this.updateSummaryAlert()
  }

  private updateSummaryAlert() {
    this.clearAlerts()
    if (this.shownInfo) {
      switch (this.shownInfo.status) {
        case HealthStatus.OK: this.addAlertSuccess(this.shownInfo.summary); break;
        case HealthStatus.WARNING: this.addAlertWarning(this.shownInfo.summary); break;
        case HealthStatus.ERROR: this.addAlertError(this.shownInfo.summary); break;
        case HealthStatus.INFO: this.addAlertInfo(this.shownInfo.summary); break;
        case HealthStatus.UNKNOWN: this.addAlertInfo(this.shownInfo.summary); break;
      }
    }
  }

  close() {
    this.modalRef.dismiss()
  }

  checkStatus() {
    this.checkPending = true
    this.serviceInfo.checkFunction().subscribe((result) => {
      this.serviceInfo.info = result
    }).add(() => {
      this.show(this.serviceInfo.info)
      this.checkPending = false
      this.updated.emit()
    })
  }

  protected readonly Constants = Constants;
}
