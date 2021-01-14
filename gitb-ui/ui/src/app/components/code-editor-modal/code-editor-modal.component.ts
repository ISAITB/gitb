import { AfterViewInit, Component, Input, OnInit, ViewChild } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { EditorOptions } from './code-editor-options';
import { CodemirrorComponent } from '@ctrl/ngx-codemirror';
import { Indicator } from './indicator';

@Component({
  selector: 'app-code-editor-modal',
  templateUrl: './code-editor-modal.component.html',
  styles: [
  ]
})
export class CodeEditorModalComponent implements OnInit, AfterViewInit {

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
    private modalRef: BsModalRef
  ) { }

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

  applyIndicators() {
    if (this.indicators != undefined) {
      for (let i=0; i < this.indicators.length; i++) {
        let indicator = this.indicators[i]
        let indicatorIcon = ''
        let indicatorClass = ''
        if (indicator.type == 'info') {
          indicatorIcon = '<i class="fa fa-info-circle"></i>'
          indicatorClass = 'info-indicator-editor-widget'
        } else if (indicator.type == 'warning') {
          indicatorIcon = '<i class="fa fa-warning"></i>'
          indicatorClass = 'warning-indicator-editor-widget'
        } else if (indicator.type == 'error') {
            indicatorIcon = '<i class="fa fa-times-circle"></i>'
            indicatorClass = 'error-indicator-editor-widget'
        }
        let widget = document.createElement('div')
        widget.innerHTML =  '<div class="indicator-editor-widget '+ indicatorClass+'">'+
                              '<span class="indicator-icon">'+indicatorIcon+'</span>'+
                              '<span class="indicator-desc">'+indicator.description+'</span>'+
                            '</div>';

        this.codeEditor.codeMirror?.addLineClass(indicator.location.line-1, 'background', 'indicator-widget-line')
        this.codeEditor.codeMirror?.addLineWidget(indicator.location.line-1, widget, {
          coverGutter: false,
          noHScroll: true,
          above: true   
        })
      }
      if (this.lineNumber != undefined) {
        this.jumpToPosition(this.lineNumber, 0)
      }
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.applyIndicators()
      this.codeEditor.codeMirror!.refresh()
    })
  }

  close() {
    this.modalRef.hide()
  }

  copyToClipboard() {
    let cm = this.codeEditor.codeMirror!
    cm.focus()
    cm.execCommand('selectAll')
    try {
      document.execCommand('copy')
    } catch (e) { 
      // Ignore 
    }
  }

  download() {
    const bb = new Blob([this.editorOptions.value!], {type: this.editorOptions.download!.mimeType})
    saveAs(bb, this.editorOptions.download!.fileName)
  }

  jumpToPosition(line: number, ch: number) {
    setTimeout(() => {
      let pos = {
        line: line,
        ch: ch
      }
      let coordinates = this.codeEditor.codeMirror!.charCoords(pos, 'local')
      let top = coordinates?.top
      let middleHeight = this.codeEditor.codeMirror!.getScrollerElement().offsetHeight / 2
      this.codeEditor.codeMirror!.scrollTo(null, top - middleHeight - 5)
    }, 100)
  }
  
}
 