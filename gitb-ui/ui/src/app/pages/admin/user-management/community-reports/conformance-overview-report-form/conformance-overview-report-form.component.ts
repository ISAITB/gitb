/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
