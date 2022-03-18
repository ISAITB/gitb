import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { ReportService } from 'src/app/services/report.service';
import { StepReport } from '../report/step-report';
import { StepData } from '../step-data';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-test-step-report-modal',
  templateUrl: './test-step-report-modal.component.html',
  styles: [
  ]
})
export class TestStepReportModalComponent implements OnInit {

  @Input() step!: StepData
  @Input() report!: StepReport
  @Input() sessionId!: string

  exportDisabled = false

  constructor(
    private modalRef: BsModalRef,
    private reportService: ReportService
  ) { }

  ngOnInit(): void {
  }

  export() {
    this.exportDisabled = true
    let pathForReport = this.step.report!.path
    if (pathForReport == undefined) {
      pathForReport = this.step.id + '.xml'
    }
    this.reportService.exportTestStepReport(this.sessionId, escape(pathForReport))
    .subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "report.pdf");
    }).add(() => {
      this.exportDisabled = false
    })
  }

  close() {
    this.modalRef.hide()
  }

}
