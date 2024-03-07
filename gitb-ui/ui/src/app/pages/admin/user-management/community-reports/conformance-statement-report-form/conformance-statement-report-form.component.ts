import { Component } from '@angular/core';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { CommunityXmlReportFormComponent } from '../community-xml-report-form/community-xml-report-form.component';

@Component({
  selector: 'app-conformance-statement-report-form',
  templateUrl: './../community-xml-report-form/community-xml-report-form.component.html'
})
export class ConformanceStatementReportFormComponent extends CommunityXmlReportFormComponent {

  constructor(
    reportService: ReportService,
    popupService: PopupService,
    modalService: BsModalService,
    confirmationDialogService: ConfirmationDialogService,
  ) { super(reportService, popupService, modalService, confirmationDialogService) }

  getIdValue(): string {
    return "stylesheetForConformanceStatementReport"
  }

  getReportType(): number {
    return this.Constants.XML_REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT
  }

  getPreviewTitle(): string {
    return "Conformance statement report preview"
  }

  getPreviewFileName(): string {
    return "conformance_report.xml"
  }

  hasPreviewDefaultOption() {
    return true
  }

  getPreviewOptions(): { label: string; data: { [key: string]: any; }; }[] {
    return [
      { label: "Generate preview with test case details", data: { tests: "true" } }
    ]
  }

}
