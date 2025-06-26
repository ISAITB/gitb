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

import {Component, Input} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {ReportService} from 'src/app/services/report.service';
import {StepReport} from '../report/step-report';
import {StepData} from '../step-data';
import {saveAs} from 'file-saver';

@Component({
    selector: 'app-test-step-report-modal',
    templateUrl: './test-step-report-modal.component.html',
    styles: [],
    standalone: false
})
export class TestStepReportModalComponent {

  @Input() step!: StepData
  @Input() report!: StepReport
  @Input() sessionId!: string

  exportPdfPending = false
  exportXmlPending = false

  constructor(
    private readonly modalRef: BsModalRef,
    private readonly reportService: ReportService
  ) { }

  exportPdf() {
    this.exportPdfPending = true
    this.export("application/pdf", "report.pdf")
  }

  exportXml() {
    this.exportXmlPending = true
    this.export("application/xml", "report.xml")
  }

  private export(contentType: string, fileName: string) {
    let pathForReport = this.step.report!.path
    if (pathForReport == undefined) {
      pathForReport = this.step.id + '.xml'
    }
    this.reportService.exportTestStepReport(this.sessionId, escape(pathForReport), contentType)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: contentType});
      saveAs(blobData, fileName);
    }).add(() => {
      this.exportPdfPending = false
      this.exportXmlPending = false
    })
  }

  close() {
    this.modalRef.hide()
  }

}
