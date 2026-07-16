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
import {Observable, map, share} from 'rxjs';
import {BaseReportSettingsFormComponent} from '../base-report-settings-form.component';
import {ConformanceService} from 'src/app/services/conformance.service';
import {ReportService} from 'src/app/services/report.service';
import {PopupService} from 'src/app/services/popup.service';
import {ErrorService} from 'src/app/services/error.service';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Constants} from 'src/app/common/constants';

/**
 * Report file name is the only configuration option available for the test data archive - there is
 * no PDF signature, external service or stylesheet option (it is a ZIP archive, not a PDF/XML report).
 */
@Component({
    selector: 'app-test-data-archive-report-form',
    templateUrl: './test-data-archive-report-form.component.html',
    standalone: false
})
export class TestDataArchiveReportFormComponent extends BaseReportSettingsFormComponent {

  idValueCustomiseFileName = 'testDataArchiveCustomiseFileName'
  updatePending = false

  constructor(
    conformanceService: ConformanceService,
    reportService: ReportService,
    private readonly popupService: PopupService,
    modalService: NgbModal,
    errorService: ErrorService
  ) { super(conformanceService, modalService, reportService, errorService) }

  loadData(): Observable<any> {
    return this.reportService.loadReportSettings(this.communityId, Constants.REPORT_TYPE.TEST_DATA_ARCHIVE)
    .pipe(
      map((result) => {
        this.reportSettings = result
        this.initialiseFileNameCustomisation()
      }), share()
    )
  }

  updateEnabled(): boolean {
    return this.reportSettings != undefined && this.fileNameExpressionOk()
  }

  update() {
    if (this.reportSettings) {
      this.prepareFileNameExpressionForSave()
      this.updatePending = true
      this.reportService.updateReportSettings(this.communityId, Constants.REPORT_TYPE.TEST_DATA_ARCHIVE, false, undefined, this.reportSettings)
      .subscribe(() => {
        this.popupService.success('Archive settings updated.')
      }).add(() => {
        this.updatePending = false
      })
    }
  }

  protected readonly Constants = Constants;

}
