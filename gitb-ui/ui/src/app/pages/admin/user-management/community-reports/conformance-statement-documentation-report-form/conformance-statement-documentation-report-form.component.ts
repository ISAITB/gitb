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
import {BaseReportSettingsFormComponent} from '../base-report-settings-form.component';
import {ConformanceService} from 'src/app/services/conformance.service';
import {ReportService} from 'src/app/services/report.service';
import {PopupService} from 'src/app/services/popup.service';
import {ErrorService} from 'src/app/services/error.service';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Constants} from 'src/app/common/constants';
import {ConformanceStatementDocumentationReportSettings} from 'src/app/types/conformance-statement-documentation-report-settings';
import {map, Observable, share} from 'rxjs';
import {forkJoin} from 'rxjs';
import {CodeEditorModalComponent} from 'src/app/components/code-editor-modal/code-editor-modal.component';
import {DataService} from '../../../../../services/data.service';
import {Utils} from '../../../../../common/utils';

@Component({
    selector: 'app-conformance-statement-documentation-report-form',
    templateUrl: './conformance-statement-documentation-report-form.component.html',
    standalone: false
})
export class ConformanceStatementDocumentationReportFormComponent extends BaseReportSettingsFormComponent {

  settings?: ConformanceStatementDocumentationReportSettings
  updatePending = false
  previewPending = false
  animated = false

  constructor(
    conformanceService: ConformanceService,
    reportService: ReportService,
    modalService: NgbModal,
    errorService: ErrorService,
    private readonly popupService: PopupService,
    private readonly dataService: DataService
  ) { super(conformanceService, modalService, reportService, errorService) }

  loadData(): Observable<any> {
    return forkJoin([
      this.conformanceService.getConformanceStatementDocumentationReportSettings(this.communityId),
      this.reportService.loadReportSettings(this.communityId, Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_DOCUMENTATION_REPORT)
    ]).pipe(
      map((results) => {
        this.settings = results[0]
        this.reportSettings = results[1]
        if (this.reportSettings.stylesheetExists) {
          this.stylesheetNameToShow = 'stylesheet.xslt'
        }
      }), share()
    )
  }

  handleExpanded(): void {
    this.animated = true
    if (this.reportSettings?.customPdfs) {
      this.focusServiceField()
    }
  }

  reportConfigurationTypeChanged() {
    if (this.reportSettings?.customPdfs) {
      this.focusServiceField()
    }
  }

  previewEnabled() {
    return (this.reportSettings?.customPdfs == false && (this.settings?.includeStatementDocumentation == true || this.settings?.includeTestSuiteDocumentation == true || this.settings?.includeTestCaseDocumentation == true)) || (this.textProvided(this.reportSettings?.customPdfService) && (!this.reportSettings?.customPdfsWithCustomXml || this.reportSettings?.stylesheetExists))
  }

  update() {
    if (this.settings && this.reportSettings) {
      this.updatePending = true
      this.reportService.updateConformanceStatementDocumentationReportSettings(this.communityId, this.reportSettings, this.settings, this.uploadedStylesheet)
      .subscribe(() => {
        if (!this.reportSettings!.customPdfsWithCustomXml) {
          this.stylesheetNameToShow = undefined
          this.uploadedStylesheet = undefined
          this.reportSettings!.stylesheetExists = false
          this.resetStylesheet.emit()
        }
        if (this.dataService.isCommunityAdmin && this.dataService.community) {
          // The flag is loaded upon user login. In this case we will apply the change immediately to
          // ensure that the community admin can see the report option immediately.
          this.dataService.community.statementDocumentationReportEnabled = this.settings?.enabled === true
        }
        this.popupService.success('Report settings updated.')
      }).add(() => {
        this.updatePending = false
      })
    }
  }

  previewPdf() {
    if (this.settings && this.reportSettings) {
      this.previewPending = true
      this.reportService.exportDemoConformanceStatementDocumentationReport(this.communityId, this.reportSettings, this.settings, this.uploadedStylesheet)
      .subscribe((response) => {
        this.handlePdfPreviewResult(response, 'conformance_statement_documentation.pdf')
      }).add(() => {
        this.previewPending = false
      })
    }
  }

  previewXml() {
    if (this.settings && this.reportSettings) {
      this.previewPending = true
      this.reportService.exportDemoConformanceStatementDocumentationReportInXML(this.communityId, this.reportSettings, this.settings, this.uploadedStylesheet)
      .subscribe((response) => {
        const modal = this.modalService.open(CodeEditorModalComponent, { size: 'lg' })
        const modalInstance = modal.componentInstance as CodeEditorModalComponent
        modalInstance.documentName = 'Conformance statement documentation report (XML)'
        modalInstance.editorOptions = {
          value: response.body ?? '',
          readOnly: true,
          lineNumbers: true,
          smartIndent: false,
          electricChars: false,
          mode: 'application/xml',
          download: {
            fileName: Utils.fileNameFromContentDisposition(response, 'conformance_statement_documentation.xml'),
            mimeType: 'application/xml'
          }
        }
      }).add(() => {
        this.previewPending = false
      })
    }
  }

  protected readonly Constants = Constants;

}
