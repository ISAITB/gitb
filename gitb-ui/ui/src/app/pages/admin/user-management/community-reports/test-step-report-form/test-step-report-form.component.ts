import { Component } from '@angular/core';
import { CommunityXmlReportFormComponent } from '../community-xml-report-form/community-xml-report-form.component';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';

@Component({
  selector: 'app-test-step-report-form',
  templateUrl: './../community-xml-report-form/community-xml-report-form.component.html'
})
export class TestStepReportFormComponent extends CommunityXmlReportFormComponent {

  constructor(
    reportService: ReportService,
    popupService: PopupService,
    modalService: BsModalService,
    confirmationDialogService: ConfirmationDialogService
  ) { super(reportService, popupService, modalService, confirmationDialogService) }

  getIdValue(): string {
    return "stylesheetForTestStepReport"
  }
  getReportType(): number {
    return this.Constants.XML_REPORT_TYPE.TEST_STEP_REPORT
  }
  getPreviewTitle(): string {
    return "Test step report preview"
  }
  getPreviewFileName(): string {
    return "step_report.xml"
  }

}
