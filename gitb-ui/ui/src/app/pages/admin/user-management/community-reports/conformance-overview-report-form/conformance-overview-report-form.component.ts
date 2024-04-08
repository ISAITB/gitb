import { Component } from '@angular/core';
import { CommunityXmlReportFormComponent } from '../community-xml-report-form/community-xml-report-form.component';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-conformance-overview-report-form',
  templateUrl: './../community-xml-report-form/community-xml-report-form.component.html'
})
export class ConformanceOverviewReportFormComponent extends CommunityXmlReportFormComponent {

  constructor(
    reportService: ReportService,
    popupService: PopupService,
    modalService: BsModalService,
    confirmationDialogService: ConfirmationDialogService,
    private dataService: DataService
  ) { super(reportService, popupService, modalService, confirmationDialogService) }

  getIdValue(): string {
    return "stylesheetForConformanceOverviewReport"
  }

  getReportType(): number {
    return this.Constants.XML_REPORT_TYPE.CONFORMANCE_OVERVIEW_REPORT
  }

  getPreviewTitle(): string {
    return "Conformance overview report preview"
  }

  getPreviewFileName(): string {
    return "conformance_report.xml"
  }

  hasPreviewDefaultOption() {
    return false
  }

  getPreviewOptions(): { label: string; data: { [key: string]: any; }; }[] {
    return [
      { label: "Aggregate level", data: { level: "all" } },
      { label: `${this.dataService.labelDomain()} level`, data: { level: "domain" } },
      { label: `${this.dataService.labelSpecificationGroup()} level`, data: { level: "group" } },
      { label: `${this.dataService.labelSpecification()} level`, data: { level: "specification" } }
    ]
  }

}
