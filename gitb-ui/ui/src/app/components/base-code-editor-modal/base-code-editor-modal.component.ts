import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { CodemirrorComponent } from '@ctrl/ngx-codemirror';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { EditorOptions } from '../code-editor-modal/code-editor-options';
import { Indicator } from '../code-editor-modal/indicator';

@Component({ template: '' })
export class BaseCodeEditorModalComponent implements AfterViewInit {

  editorOptions?: EditorOptions
  indicators?: Indicator[]
  lineNumber?: number

  @ViewChild('codeEditor', {static: false}) codeEditor?: CodemirrorComponent

  constructor(
    private modalRef: BsModalRef,
    private dataService: DataService,
    private popupService: PopupService
  ) { }

  applyLineStyles(): boolean {
    return false
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
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

}
