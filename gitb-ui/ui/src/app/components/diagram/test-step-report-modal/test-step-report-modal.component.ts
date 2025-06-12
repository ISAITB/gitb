import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { ReportService } from 'src/app/services/report.service';
import { StepReport } from '../report/step-report';
import { StepData } from '../step-data';
import { saveAs } from 'file-saver'

@Component({
    selector: 'app-test-step-report-modal',
    templateUrl: './test-step-report-modal.component.html',
    styles: [],
    standalone: false
})
export class TestStepReportModalComponent implements OnInit {

  @Input() step!: StepData
  @Input() report!: StepReport
  @Input() sessionId!: string

  exportPdfPending = false
  exportXmlPending = false

  constructor(
    private modalRef: BsModalRef,
    private reportService: ReportService
  ) { }

  ngOnInit(): void {
  }

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
