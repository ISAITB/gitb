import { Component } from '@angular/core';
import { CommunityXmlReportFormComponent } from '../community-xml-report-form/community-xml-report-form.component';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';

@Component({
  selector: 'app-test-case-report-form',
  templateUrl: './../community-xml-report-form/community-xml-report-form.component.html'
})
export class TestCaseReportFormComponent extends CommunityXmlReportFormComponent {

  constructor(
    reportService: ReportService,
    popupService: PopupService,
    modalService: BsModalService,
    confirmationDialogService: ConfirmationDialogService
  ) { super(reportService, popupService, modalService, confirmationDialogService) }

  getIdValue(): string {
    return "stylesheetForTestCaseReport"
  }
  getReportType(): number {
    return this.Constants.XML_REPORT_TYPE.TEST_CASE_REPORT
  }
  getPreviewTitle(): string {
    return "Test case report preview"
  }
  getPreviewFileName(): string {
    return "test_report.xml"
  }

}
