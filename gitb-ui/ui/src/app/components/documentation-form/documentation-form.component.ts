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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';
import { PopupService } from 'src/app/services/popup.service';

/**
 * Shared documentation editor block used in specification, actor, test suite and test case detail pages.
 *
 * - `showPreviewAndCopy`: when true the Preview and Copy buttons are displayed (enabled for test suites/test cases).
 *   For specifications and actors this defaults to false and the buttons are hidden.
 */
@Component({
    selector: 'app-documentation-form',
    templateUrl: './documentation-form.component.html',
    styleUrls: ['./documentation-form.component.less'],
    standalone: false
})
export class DocumentationFormComponent {

  @Input() documentation: string | undefined
  @Output() documentationChange = new EventEmitter<string | undefined>()

  @Input() communityId: number | undefined
  @Input() showPreviewAndCopy = false
  @Input() showPdfPreview = false
  @Input() previewTitle = 'Documentation'
  @Input() tooltipText: string = ''
  @Output() previewPdf = new EventEmitter<void>()

  pending = false

  constructor(
    readonly dataService: DataService,
    private conformanceService: ConformanceService,
    private htmlService: HtmlService,
    private popupService: PopupService
  ) {}

  onDocumentationChange(value: string | undefined) {
    this.documentation = value
    this.documentationChange.emit(value)
  }

  previewDocumentation() {
    if (this.documentation) {
      this.pending = true
      this.conformanceService.getDocumentationForPreview(this.documentation)
        .subscribe((html) => {
          this.htmlService.showHtml(this.previewTitle, html)
        }).add(() => {
          this.pending = false
        })
    }
  }

  copyDocumentation() {
    if (this.documentation) {
      this.dataService.copyToClipboard(this.documentation).subscribe(() => {
        this.popupService.success('HTML source copied to clipboard.')
      })
    }
  }

  protected readonly Constants = Constants;
}
