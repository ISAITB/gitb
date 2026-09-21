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
import {Observable, map, share} from 'rxjs';
import {BaseReportSettingsFormComponent} from '../base-report-settings-form.component';
import {ConformanceService} from 'src/app/services/conformance.service';
import {ReportService} from 'src/app/services/report.service';
import {PopupService} from 'src/app/services/popup.service';
import {ErrorService} from 'src/app/services/error.service';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Constants} from 'src/app/common/constants';
import {CommunityReportSettings} from 'src/app/types/community-report-settings';

/**
 * Base for the simple PDF-only documentation report forms (test case / test suite documentation),
 * which only expose a report file name and a signature option - no external service, stylesheet or
 * XML variant. Modelled after CommunityXmlReportFormComponent, but stripped down accordingly.
 */
@Component({
    template: '',
    standalone: false
})
export abstract class SimpleDocumentationReportFormComponent extends BaseReportSettingsFormComponent {

  idValueSign!: string
  idValueCustomiseFileName!: string

  updatePending = false
  previewPending = false

  constructor(
    conformanceService: ConformanceService,
    reportService: ReportService,
    private readonly popupService: PopupService,
    modalService: NgbModal,
    errorService: ErrorService
  ) { super(conformanceService, modalService, reportService, errorService) }

  ngOnInit(): void {
    super.ngOnInit()
    this.idValueSign = this.baseIdValue() + 'Sign'
    this.idValueCustomiseFileName = this.baseIdValue() + 'CustomiseFileName'
  }

  loadData(): Observable<any> {
    return this.reportService.loadReportSettings(this.communityId, this.reportType())
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
      this.reportService.updateReportSettings(this.communityId, this.reportType(), false, undefined, this.reportSettings)
      .subscribe(() => {
        this.popupService.success('Report settings updated.')
      }).add(() => {
        this.updatePending = false
      })
    }
  }

  preview() {
    if (this.reportSettings) {
      this.previewPending = true
      this.previewCall(this.reportSettings).subscribe((response) => {
        this.handlePdfPreviewResult(response, this.previewFallbackFileName())
      }).add(() => {
        this.previewPending = false
      })
    }
  }

  abstract baseIdValue(): string
  abstract reportType(): number
  protected abstract previewFallbackFileName(): string
  protected abstract previewCall(reportSettings: CommunityReportSettings): Observable<HttpResponse<ArrayBuffer>>

  protected readonly Constants = Constants;

}
