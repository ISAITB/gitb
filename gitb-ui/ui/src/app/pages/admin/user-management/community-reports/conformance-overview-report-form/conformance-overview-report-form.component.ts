import { Component } from '@angular/core';
import { CommunityXmlReportFormComponent } from '../community-xml-report-form/community-xml-report-form.component';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { PreviewConfig } from '../community-xml-report-form/preview-config';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';

@Component({
    selector: 'app-conformance-overview-report-form',
    templateUrl: './../community-xml-report-form/community-xml-report-form.component.html',
    standalone: false
})
export class ConformanceOverviewReportFormComponent extends CommunityXmlReportFormComponent {

  constructor(
    conformanceService: ConformanceService,
    reportService: ReportService,
    popupService: PopupService,
    modalService: BsModalService,
    confirmationDialogService: ConfirmationDialogService,
    private dataService: DataService,
    errorService: ErrorService
  ) { super(conformanceService, reportService, popupService, modalService, confirmationDialogService, errorService) }

  getPreviewConfig(): PreviewConfig {
    return {
      baseIdValue: "conformanceOverviewReport",
      previewFileNamePdf: "conformance_overview.pdf",
      previewFileNameXml: "conformance_overview.xml",
      previewTitleXml: "Conformance overview report preview",
      reportType: this.Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_REPORT,
      previewOptions: [
        [
          { label: "XML report (aggregate level)", isXml: true, data: { level: "all" } },
          { label: `XML report (${this.dataService.labelDomain()} level)`, isXml: true, data: { level: "domain" } },
          { label: `XML report (${this.dataService.labelSpecificationGroup()} level)`, isXml: true, data: { level: "group" } },
          { label: `XML report (${this.dataService.labelSpecification()} level)`, isXml: true, data: { level: "specification" } }
        ],
        [
          { label: "PDF report (aggregate level)", isXml: false, data: { level: "all" } },
          { label: `PDF report (${this.dataService.labelDomain()} level)`, isXml: false, data: { level: "domain" } },
          { label: `PDF report (${this.dataService.labelSpecificationGroup()} level)`, isXml: false, data: { level: "group" } },
          { label: `PDF report (${this.dataService.labelSpecification()} level)`, isXml: false, data: { level: "specification" } }
        ]
      ]
    }  
  }

}
