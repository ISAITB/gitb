import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ReportService } from 'src/app/services/report.service';
import { ConformanceCertificateSettings } from 'src/app/types/conformance-certificate-settings';
import { ConformanceResultFull } from 'src/app/types/conformance-result-full';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-conformance-certificate-modal',
  templateUrl: './conformance-certificate-modal.component.html',
  styleUrls: [ './conformance-certificate-modal.component.less' ]
})
export class ConformanceCertificateModalComponent implements OnInit {

  @Input() settings!: ConformanceCertificateSettings
  @Input() conformanceStatement!: ConformanceResultFull
  @Input() snapshotId?: number

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
      if (this.settings.message.includes(Constants.PLACEHOLDER__DOMAIN+'{')) {
        // The message includes domain parameter placeholders.
        this.conformanceService.getDomainParametersOfCommunity(this.settings.community, true, true)
        .subscribe((data) => {
          let message = this.settings.message!
          for (let param of data) {
            message = message.split(Constants.PLACEHOLDER__DOMAIN+'{'+param.name+'}').join((param.value == undefined)?'':param.value!)
          }
          this.settings.message = this.replacePlaceholders(message)
        })
      } else {
        this.settings.message = this.replacePlaceholders(this.settings.message)
      }
    } else {
      this.settings.message = ''
    }
  }

  private replacePlaceholders(message: string) {
    message = message.split(Constants.PLACEHOLDER__DOMAIN).join(this.conformanceStatement.domainName)
    message = message.split(Constants.PLACEHOLDER__SPECIFICATION_GROUP_OPTION).join(this.conformanceStatement.specGroupOptionName)
    message = message.split(Constants.PLACEHOLDER__SPECIFICATION_GROUP).join(this.conformanceStatement.specGroupName?this.conformanceStatement.specGroupName:'')
    message = message.split(Constants.PLACEHOLDER__SPECIFICATION).join(this.conformanceStatement.specName)
    message = message.split(Constants.PLACEHOLDER__ACTOR).join(this.conformanceStatement.actorName)
    message = message.split(Constants.PLACEHOLDER__ORGANISATION).join(this.conformanceStatement.organizationName)
    message = message.split(Constants.PLACEHOLDER__SYSTEM).join(this.conformanceStatement.systemName)
    return message
  }

  certificateChoice() {
    if (this.settings.includeTitle) {
        this.dataService.focus('title', 200)
    }
  }

  includeTitleChanged() {
    if (this.settings.includeTitle) {
      this.dataService.focus('title')
    }
  }

  generate() {
    this.exportPending = true
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE) {
        this.conformanceService.exportConformanceCertificateReport(this.conformanceStatement.communityId, this.conformanceStatement.actorId, this.conformanceStatement.systemId, this.settings, this.snapshotId)
        .subscribe((data) => {
          const blobData = new Blob([data], {type: 'application/pdf'});
          saveAs(blobData, "conformance_certificate.pdf");
          this.modalInstance.hide()
        }).add(() => {
          this.exportPending = false
        })
    } else {
        const includeDetails = this.choice == Constants.REPORT_OPTION_CHOICE.DETAILED_REPORT
        this.reportService.exportConformanceStatementReport(this.conformanceStatement.actorId, this.conformanceStatement.systemId, includeDetails, this.snapshotId)
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
