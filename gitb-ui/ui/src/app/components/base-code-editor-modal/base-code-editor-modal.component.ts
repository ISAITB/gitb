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

import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { CodemirrorComponent } from '@ctrl/ngx-codemirror';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { EditorOptions } from '../code-editor-modal/code-editor-options';
import { Indicator } from '../code-editor-modal/indicator';
import { saveAs } from 'file-saver'

@Component({
    template: '',
    standalone: false
})
export class BaseCodeEditorModalComponent implements AfterViewInit {

  editorOptions?: EditorOptions
  indicators?: Indicator[]
  lineNumber?: number

  @ViewChild('codeEditor', {static: false}) codeEditor?: CodemirrorComponent

  constructor(
    private modalRef: BsModalRef,
    protected dataService: DataService,
    private popupService: PopupService
  ) { }

  applyLineStyles(): boolean {
    return false
  }

  ngAfterViewInit(): void {
    this.codeEditor?.codeMirrorLoaded.subscribe(() => {
      if (this.applyLineStyles()) {
        if (this.codeEditor?.codeMirror) {
          this.codeEditor.codeMirror.refresh()
        }
      }
    })
  }

  close() {
    this.modalRef.hide()
  }

  copyToClipboard() {
    if (this.codeEditor?.codeMirror) {
      this.dataService.copyToClipboard(this.codeEditor.codeMirror.getValue()).subscribe(() => {
        this.popupService.success('Content copied to clipboard.')
      })
    }
  }

  download() {
    if (this.codeEditor?.codeMirror && this.editorOptions) {
      const bb = new Blob([this.codeEditor.codeMirror.getValue()], {type: this.editorOptions.download!.mimeType})
      saveAs(bb, this.editorOptions.download!.fileName)
    }
  }

  jumpToPosition(line: number, ch: number) {
    setTimeout(() => {
      let pos = {
        line: line,
        ch: ch
      }
      if (this.codeEditor?.codeMirror) {
        let coordinates = this.codeEditor.codeMirror.charCoords(pos, 'local')
        let top = coordinates?.top
        let middleHeight = this.codeEditor.codeMirror.getScrollerElement().offsetHeight / 2
        this.codeEditor.codeMirror.scrollTo(null, top - middleHeight - 5)
      }
    }, 100)
  }

}
