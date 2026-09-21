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

import {AfterViewInit, Component, ViewChild, ChangeDetectionStrategy} from '@angular/core';
import {CodeEditorComponent} from '../code-editor/code-editor.component';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {EditorOptions} from '../code-editor-modal/code-editor-options';
import {Indicator} from '../code-editor-modal/indicator';
import {saveAs} from 'file-saver';
import {BaseComponent} from '../../pages/base-component.component';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    template: '',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class BaseCodeEditorModalComponent extends BaseComponent implements AfterViewInit {

  editorOptions?: EditorOptions
  indicators?: Indicator[]
  lineNumber?: number

  @ViewChild('codeEditor', {static: false}) codeEditor?: CodeEditorComponent

  constructor(
    private readonly modalRef: NgbActiveModal,
    protected readonly dataService: DataService,
    private readonly popupService: PopupService
  ) { super() }

  applyLineStyles(): boolean {
    return false
  }

  ngAfterViewInit(): void {
    this.codeEditor?.loaded.subscribe(() => {
      if (this.applyLineStyles()) {
        this.codeEditor?.refresh()
      }
    })
  }

  close() {
    this.modalRef.dismiss()
  }

  copyToClipboard() {
    if (this.codeEditor?.view) {
      this.dataService.copyToClipboard(this.codeEditor.getValue()).subscribe(() => {
        this.popupService.success('Content copied to clipboard.')
      })
    }
  }

  download() {
    if (this.codeEditor?.view && this.editorOptions) {
      const bb = new Blob([this.codeEditor.getValue()], {type: this.editorOptions.download!.mimeType})
      saveAs(bb, this.editorOptions.download!.fileName)
    }
  }

  /**
   * Scroll to the (1-based) line, centering it in the editor.
   */
  jumpToLine(line: number) {
    setTimeout(() => this.codeEditor?.scrollToLine(line), 100)
  }

}
