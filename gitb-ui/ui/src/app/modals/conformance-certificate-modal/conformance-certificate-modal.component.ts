import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ReportService } from 'src/app/services/report.service';
import { ConformanceCertificateSettings } from 'src/app/types/conformance-certificate-settings';
import { ConformanceResultFull } from 'src/app/types/conformance-result-full';

@Component({
  selector: 'app-conformance-certificate-modal',
  templateUrl: './conformance-certificate-modal.component.html',
  styles: [
  ]
})
export class ConformanceCertificateModalComponent implements OnInit {

  @Input() settings!: ConformanceCertificateSettings
  @Input() conformanceStatement!: ConformanceResultFull

  exportPending = false
  choice = Constants.REPORT_OPTION_CHOICE.REPORT
  Constants = Constants

  constructor(
    private dataService: DataService,
    private modalInstance: BsModalRef,
    private conformanceService: ConformanceService,
    private reportService: ReportService
  ) { }

  ngOnInit(): void {
    if (this.settings.message != undefined) {
      // Replace the placeholders for the preview.
      this.settings.message = this.settings.message.split(Constants.PLACEHOLDER__DOMAIN).join(this.conformanceStatement.domainName)
      this.settings.message = this.settings.message.split(Constants.PLACEHOLDER__SPECIFICATION).join(this.conformanceStatement.specName)
      this.settings.message = this.settings.message.split(Constants.PLACEHOLDER__ACTOR).join(this.conformanceStatement.actorName)
      this.settings.message = this.settings.message.split(Constants.PLACEHOLDER__ORGANISATION).join(this.conformanceStatement.organizationName)
      this.settings.message = this.settings.message.split(Constants.PLACEHOLDER__SYSTEM).join(this.conformanceStatement.systemName)
    } else {
      this.settings.message = ''
    }
  }

  certificateChoice() {
    this.dataService.focus('title', 200)
  }

  generate() {
    this.exportPending = true
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE) {
        this.conformanceService.exportConformanceCertificateReport(this.conformanceStatement.communityId, this.conformanceStatement.actorId, this.conformanceStatement.systemId, this.settings)
        .subscribe((data) => {
          const blobData = new Blob([data], {type: 'application/pdf'});
          saveAs(blobData, "conformance_certificate.pdf");
          this.modalInstance.hide()
        }).add(() => {
          this.exportPending = false
        })
    } else {
        const includeDetails = this.choice == Constants.REPORT_OPTION_CHOICE.DETAILED_REPORT
        this.reportService.exportConformanceStatementReport(this.conformanceStatement.actorId, this.conformanceStatement.systemId, includeDetails)
        .subscribe((data) => {
          const blobData = new Blob([data], {type: 'application/pdf'});
          saveAs(blobData, "conformance_report.pdf");
          this.modalInstance.hide()
        }).add(() => {
          this.exportPending = false
        })
    }
  }

  cancel() {
    this.modalInstance.hide()
  }

}
