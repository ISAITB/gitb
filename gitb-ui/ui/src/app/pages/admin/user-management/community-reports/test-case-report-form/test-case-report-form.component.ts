import { Component } from '@angular/core';
import { CommunityXmlReportFormComponent } from '../community-xml-report-form/community-xml-report-form.component';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { PreviewConfig } from '../community-xml-report-form/preview-config';
import { ErrorService } from 'src/app/services/error.service';

@Component({
    selector: 'app-test-case-report-form',
    templateUrl: './../community-xml-report-form/community-xml-report-form.component.html',
    standalone: false
})
export class TestCaseReportFormComponent extends CommunityXmlReportFormComponent {

  constructor(
    conformanceService: ConformanceService,    
    reportService: ReportService,
    popupService: PopupService,
    modalService: BsModalService,
    confirmationDialogService: ConfirmationDialogService,
    errorService: ErrorService
  ) { super(conformanceService, reportService, popupService, modalService, confirmationDialogService, errorService) }

  getPreviewConfig(): PreviewConfig {
    return {
      baseIdValue: "testCaseReport",
      previewFileNamePdf: "test_report.pdf",
      previewFileNameXml: "test_report.xml",
      previewTitleXml: "Test case report preview",
      reportType: this.Constants.REPORT_TYPE.TEST_CASE_REPORT,
      previewOptions: [
        [
          { label: "XML report", isXml: true },
          { label: "PDF report", isXml: false }
        ]
      ]
    }
  }

}
