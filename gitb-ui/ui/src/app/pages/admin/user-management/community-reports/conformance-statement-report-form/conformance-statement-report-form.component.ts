import { Component } from '@angular/core';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { CommunityXmlReportFormComponent } from '../community-xml-report-form/community-xml-report-form.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { PreviewConfig } from '../community-xml-report-form/preview-config';
import { ErrorService } from 'src/app/services/error.service';

@Component({
    selector: 'app-conformance-statement-report-form',
    templateUrl: './../community-xml-report-form/community-xml-report-form.component.html',
    standalone: false
})
export class ConformanceStatementReportFormComponent extends CommunityXmlReportFormComponent {

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
      baseIdValue: "conformanceStatementReport",
      previewFileNamePdf: "conformance_report.pdf",
      previewFileNameXml: "conformance_report.xml",
      previewTitleXml: "Conformance statement report preview",
      reportType: this.Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT,
      previewOptions: [
        [
          { label: "XML report", isXml: true },
          { label: "XML report with test case details", isXml: true, data: { tests: "true" } }
        ],
        [
          { label: "PDF report", isXml: false },
          { label: "PDF report with test case details", isXml: false, data: { tests: "true" } }
        ]
      ]
    }  
  }

}
