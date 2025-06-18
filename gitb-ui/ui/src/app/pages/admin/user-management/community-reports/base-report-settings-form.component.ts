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

import { HttpResponse } from '@angular/common/http';
import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { from, Observable } from 'rxjs';
import { CodeEditorModalComponent } from 'src/app/components/code-editor-modal/code-editor-modal.component';
import { CommunityKeystoreModalComponent } from 'src/app/modals/community-keystore-modal/community-keystore-modal.component';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { ReportService } from 'src/app/services/report.service';
import { CommunityReportSettings } from 'src/app/types/community-report-settings';
import { FileData } from 'src/app/types/file-data.type';
import { saveAs } from 'file-saver'
import { ErrorService } from 'src/app/services/error.service';

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

  constructor(
    protected conformanceService: ConformanceService,
    protected modalService: BsModalService,
    protected reportService: ReportService,
    protected errorService: ErrorService
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

  collapseForm() {
    this.collapse.emit(true)
  }

  manageKeystore() {
    this.manageKeystorePending = true
    this.conformanceService.getCommunityKeystoreInfo(this.communityId).subscribe((data) => {
      this.modalService.show(CommunityKeystoreModalComponent, {
        class: 'modal-lg',
        initialState: {
          communityId: this.communityId,
          communityKeystore: data
        }
      })
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
        this.modalService.show(CodeEditorModalComponent, {
          class: 'modal-lg',
          initialState: {
            documentName: 'Stylesheet',
            editorOptions: {
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
          }
        })
      })
    }
  }

  handlePdfPreviewResult(response: HttpResponse<ArrayBuffer>, fileName: string) {
    if (response.headers.get("Content-Type") == "application/pdf") {
      const blobData = new Blob([response.body as ArrayBuffer], {type: 'application/pdf'});
      saveAs(blobData, fileName);
    } else {
      const result = JSON.parse(new TextDecoder("utf-8").decode(response.body as ArrayBuffer))
      this.errorService.popupErrorsArray(result.texts, "Service call error(s)", result.contentType)
    }
  }

}
