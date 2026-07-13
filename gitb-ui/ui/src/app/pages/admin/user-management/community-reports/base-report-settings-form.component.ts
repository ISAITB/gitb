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

import {HttpResponse} from '@angular/common/http';
import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {from, Observable} from 'rxjs';
import {CodeEditorModalComponent} from 'src/app/components/code-editor-modal/code-editor-modal.component';
import {CommunityKeystoreModalComponent} from 'src/app/modals/community-keystore-modal/community-keystore-modal.component';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {ConformanceService} from 'src/app/services/conformance.service';
import {ReportService} from 'src/app/services/report.service';
import {CommunityReportSettings} from 'src/app/types/community-report-settings';
import {FileData} from 'src/app/types/file-data.type';
import {saveAs} from 'file-saver';
import {ErrorService} from 'src/app/services/error.service';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Utils} from 'src/app/common/utils';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseReportSettingsFormComponent extends BaseComponent implements OnInit {

  @ViewChild("serviceField") serviceField?: ElementRef;

  @Input() communityId!: number
  @Input() selected!: EventEmitter<boolean>
  @Input() expanded!: EventEmitter<void>
  @Output() loaded = new EventEmitter<boolean>()
  @Output() collapse = new EventEmitter<boolean>()

  manageKeystorePending = false
  reportSettings?: CommunityReportSettings
  uploadedStylesheet?: FileData
  resetStylesheet = new EventEmitter<void>()
  acceptedStylesheetTypes: string[] = ['application/xml', 'text/xml', 'text/xsl', 'application/xslt+xml' ]
  stylesheetNameToShow?: string
  // Whether the report's file name naming expression is customised for this community (vs using the applicable default).
  customiseFileName = false

  constructor(
    protected readonly conformanceService: ConformanceService,
    protected readonly modalService: NgbModal,
    protected readonly reportService: ReportService,
    protected readonly errorService: ErrorService
  ) { super() }

  ngOnInit(): void {
    this.selected.subscribe(() => {
      this.loadData().subscribe(() => {
        this.loaded.emit(true)
      })
    })
    this.expanded.subscribe(() => {
      this.handleExpanded()
    })
  }

  protected focusServiceField() {
    setTimeout(() => {
      this.serviceField?.nativeElement.focus()
    }, 1)
  }

  abstract loadData(): Observable<any>

  handleExpanded() {
    // Do nothing by default
  }

  /**
   * Called once report settings are loaded to determine the initial state of the "Customise?" check.
   */
  protected initialiseFileNameCustomisation() {
    this.customiseFileName = this.reportSettings?.fileNameExpression != undefined
  }

  /**
   * Called when the "Customise?" check is toggled, so that checking it initialises the now-editable
   * field with the previously applicable default naming expression (rather than starting empty).
   */
  onCustomiseFileNameChange() {
    if (this.customiseFileName && this.reportSettings != undefined && !this.textProvided(this.reportSettings.fileNameExpression)) {
      this.reportSettings.fileNameExpression = this.reportSettings.defaultFileNameExpression
    }
  }

  /**
   * Called just before submitting an update, so that unchecking "Customise?" clears any previously
   * configured override (both for the UI state and what gets sent to be persisted).
   */
  protected prepareFileNameExpressionForSave() {
    if (this.reportSettings != undefined && !this.customiseFileName) {
      this.reportSettings.fileNameExpression = undefined
    }
  }

  fileNameExpressionOk(): boolean {
    return !this.customiseFileName || this.textProvided(this.reportSettings?.fileNameExpression)
  }

  manageKeystore() {
    this.manageKeystorePending = true
    this.conformanceService.getCommunityKeystoreInfo(this.communityId).subscribe((data) => {
      const modal = this.modalService.open(CommunityKeystoreModalComponent, { size: 'lg' })
      const modalInstance = modal.componentInstance as CommunityKeystoreModalComponent
      modalInstance.communityId = this.communityId
      modalInstance.communityKeystore = data
    }).add(() => {
      this.manageKeystorePending = false
    })
  }

  selectStylesheet(file: FileData) {
    if (this.reportSettings) {
      this.stylesheetNameToShow = file.name
      this.uploadedStylesheet = file
      this.reportSettings.stylesheetExists = true
    }
  }

  viewStylesheet(reportType: number) {
    if (this.reportSettings?.stylesheetExists) {
      let contentObservable: Observable<string>
      if (this.uploadedStylesheet?.file) {
        contentObservable = from(this.uploadedStylesheet.file.text())
      } else {
        contentObservable = this.reportService.getReportStylesheet(this.communityId, reportType)
      }
      contentObservable.subscribe((data) => {
        const modal = this.modalService.open(CodeEditorModalComponent, { size: 'lg' })
        const modalInstance = modal.componentInstance as CodeEditorModalComponent
        modalInstance.documentName = 'Stylesheet'
        modalInstance.editorOptions = {
          value: data,
          readOnly: true,
          lineNumbers: true,
          smartIndent: false,
          electricChars: false,
          mode: 'application/xml',
          download: {
            fileName: 'stylesheet.xslt',
            mimeType: 'application/xslt+xml'
          }
        }
      })
    }
  }

  handlePdfPreviewResult(response: HttpResponse<ArrayBuffer>, fallbackFileName: string) {
    if (response.headers.get("Content-Type") == "application/pdf") {
      const blobData = new Blob([response.body as ArrayBuffer], {type: 'application/pdf'});
      saveAs(blobData, Utils.fileNameFromContentDisposition(response, fallbackFileName));
    } else {
      const result = JSON.parse(new TextDecoder("utf-8").decode(response.body as ArrayBuffer))
      this.errorService.popupErrorsArray(result.texts, "Service call error(s)", result.contentType)
    }
  }

}
