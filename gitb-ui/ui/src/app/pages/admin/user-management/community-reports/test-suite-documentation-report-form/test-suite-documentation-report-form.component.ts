/*
 * Copyright (C) 2026 European Union
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

import {Component} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SimpleDocumentationReportFormComponent} from '../simple-documentation-report-form/simple-documentation-report-form.component';
import {ReportService} from 'src/app/services/report.service';
import {PopupService} from 'src/app/services/popup.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {ErrorService} from 'src/app/services/error.service';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {CommunityReportSettings} from 'src/app/types/community-report-settings';

@Component({
    selector: 'app-test-suite-documentation-report-form',
    templateUrl: './../simple-documentation-report-form/simple-documentation-report-form.component.html',
    standalone: false
})
export class TestSuiteDocumentationReportFormComponent extends SimpleDocumentationReportFormComponent {

  constructor(
    conformanceService: ConformanceService,
    reportService: ReportService,
    popupService: PopupService,
    modalService: NgbModal,
    errorService: ErrorService
  ) { super(conformanceService, reportService, popupService, modalService, errorService) }

  baseIdValue(): string {
    return 'testSuiteDocumentation'
  }

  reportType(): number {
    return this.Constants.REPORT_TYPE.TEST_SUITE_DOCUMENTATION_REPORT
  }

  protected previewFallbackFileName(): string {
    return 'test_suite_documentation.pdf'
  }

  protected previewCall(reportSettings: CommunityReportSettings): Observable<HttpResponse<ArrayBuffer>> {
    return this.reportService.exportDemoTestSuiteDocumentationReport(this.communityId, reportSettings)
  }

}
