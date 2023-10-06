import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { EditorOptions } from './code-editor-options';
import { CodemirrorComponent } from '@ctrl/ngx-codemirror';
import { Indicator } from './indicator';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { BaseCodeEditorModalComponent } from '../base-code-editor-modal/base-code-editor-modal.component';

@Component({
  selector: 'app-code-editor-modal',
  templateUrl: './code-editor-modal.component.html',
  styles: [
  ]
})
export class CodeEditorModalComponent extends BaseCodeEditorModalComponent implements OnInit {

  @Input() documentName?: string
  @Input() editorOptions!: EditorOptions
  @Input() indicators?: Indicator[]
  @Input() lineNumber?: number
  
  @ViewChild('codeEditor', {static: false}) codeEditor!: CodemirrorComponent

  isNameVisible = false
  isDownloadVisible = false
  isCopyVisible = false
  styleClass = 'editor-normal'

  constructor(
    modalRef: BsModalRef,
    dataService: DataService,
    popupService: PopupService
  ) { super(modalRef, dataService, popupService) }

  ngOnInit(): void {
    this.isNameVisible = this.documentName != undefined
    this.isDownloadVisible = this.editorOptions.download != undefined && this.editorOptions.value != undefined
    if (this.editorOptions.copy == undefined) {
      this.isCopyVisible = true 
    } else {
      this.isCopyVisible = this.editorOptions.copy
    }
    if (this.editorOptions.styleClass != undefined) {
      this.styleClass = this.editorOptions.styleClass
    }
  }

  applyLineStyles(): boolean {
    if (this.indicators != undefined) {
      for (let i=0; i < this.indicators.length; i++) {
        let indicator = this.indicators[i]
        let indicatorIcon = ''
        let indicatorClass = ''
        if (indicator.type == 'info') {
          indicatorIcon = '<i class="fa-solid fa-info-circle"></i>'
          indicatorClass = 'info-indicator-editor-widget'
        } else if (indicator.type == 'warning') {
          indicatorIcon = '<i class="fa-solid fa-warning"></i>'
          indicatorClass = 'warning-indicator-editor-widget'
        } else if (indicator.type == 'error') {
            indicatorIcon = '<i class="fa-solid fa-times-circle"></i>'
            indicatorClass = 'error-indicator-editor-widget'
        }
        let widget = document.createElement('div')
        widget.innerHTML =  '<div class="indicator-editor-widget '+ indicatorClass+'">'+
                              '<span class="indicator-icon">'+indicatorIcon+'</span>'+
                              '<span class="indicator-desc">'+indicator.description+'</span>'+
                            '</div>';
        if (this.codeEditor) {
          this.codeEditor.codeMirror?.addLineClass(indicator.location.line-1, 'background', 'indicator-widget-line')
          this.codeEditor.codeMirror?.addLineWidget(indicator.location.line-1, widget, {
            coverGutter: false,
            noHScroll: true,
            above: true   
          })
        }
      }
      if (this.lineNumber != undefined) {
        this.jumpToPosition(this.lineNumber, 0)
      }
      return true
    } else {
      return false
    }
  }

}
 