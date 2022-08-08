import { Component, EventEmitter, Input } from '@angular/core';
import * as CodeMirror from 'codemirror';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { BaseCodeEditorModalComponent } from '../base-code-editor-modal/base-code-editor-modal.component';
import { LineInfo } from './line-info';
import { LogLevel } from '../../types/log-level';

@Component({
  selector: 'app-session-log-modal',
  templateUrl: './session-log-modal.component.html',
  styleUrls: ['./session-log-modal.component.less']
})
export class SessionLogModalComponent extends BaseCodeEditorModalComponent {

  static LINE_PARTS_REGEX = /^(.+)/gm

  @Input() messages!: string[]
  @Input() messageEmitter?: EventEmitter<string>

  lines: LineInfo[] = []
  minimumLogLevel = LogLevel.DEBUG
  content = ''
  contentLines: LineInfo[] = []
  tail = false

  LogLevel = LogLevel
  levelFilterLabelDebug = 'Show all messages'
  levelFilterLabelInfo = 'Show at least info messages'
  levelFilterLabelWarn = 'Show at least warnings'
  levelFilterLabelError = 'Show errors'
  levelFilterLabel = this.levelFilterLabelDebug

  tailLabelYes = 'Scroll to latest'
  tailLabelNo = 'Do not scroll to latest'
  tailLabel = this.tailLabelNo

  constructor(
    modalRef: BsModalRef,
    dataService: DataService,
    popupService: PopupService    
  ) { super(modalRef, dataService, popupService) }

  ngOnInit(): void {
    this.editorOptions = {
      readOnly: true,
      lineNumbers: true,
      smartIndent: false,
      electricChars: false,
      mode: 'text/plain',
      download: {
        fileName: 'log.txt',
        mimeType: 'text/plain'
      }
    }
    this.initialiseLines(this.messages)
    this.updateContent()
    if (this.messageEmitter) {
      // Subscribe to live log updates
      this.messageEmitter.subscribe((newMessage) => {
        this.messages.push(newMessage)
        const createdLines = this.initialiseLines([newMessage])
        for (let line of createdLines) {
          if (line.level >= this.minimumLogLevel) {
            this.contentLines.push(line)
            // Do not update the content directly because this causes a full editor refresh
            this.codeEditor!.codeMirror!.replaceRange(line.text+'\n', CodeMirror.Pos(this.codeEditor!.codeMirror!.lastLine()))
            if (this.tail) {
              this.scrollToLast()
            }
          }
        }
        setTimeout(() => {
          this.applyLineStyles()
        })
      })
    }
  }

  scrollToLast() {
    this.jumpToPosition(this.codeEditor!.codeMirror!.lastLine(), 0)
  }

  private initialiseLines(newMessages: string[]) {
    let previousLevel = LogLevel.INFO
    const createdLines: LineInfo[] = []
    for (let message of newMessages) {
      const messageParts = message.replace('\r', '\n').match(SessionLogModalComponent.LINE_PARTS_REGEX)
      if (messageParts) {
        for (let part of messageParts) {
          if (part.length > 0) {
            const partLevel = this.dataService.logMessageLevel(part, previousLevel)
            previousLevel = partLevel
            createdLines.push({
              text: part,
              level: partLevel
            })
          }
        }
      }
    }
    this.lines.push(...createdLines)
    return createdLines
  }

  private updateContent() {
    this.content = ''
    this.contentLines = []
    for (let line of this.lines) {
      if (line.level >= this.minimumLogLevel) {
        this.contentLines.push(line)
        this.content += line.text + '\n'
      }
    }
  }

  applyLineStyles(): boolean {
    for (let i=0; i < this.contentLines.length; i++) {
      this.applyLineStyle(i, this.contentLines[i])
    }
    return true
  }

  applyLineStyle(lineNumber: number, lineData: LineInfo) {
    if (this.codeEditor?.codeMirror) {
      this.codeEditor.codeMirror.addLineClass(lineNumber, 'text', 'log-level '+this.logLevelToString(lineData.level))
    }
  }

  private logLevelToString(level: LogLevel) {
    if (level == LogLevel.DEBUG) return 'debug'
    else if (level == LogLevel.INFO) return 'info'
    else if (level == LogLevel.WARN) return 'warn'
    else return 'error'
  }

  applyMinimumLogLevel(level: LogLevel) {
    if (this.minimumLogLevel != level) {
      this.minimumLogLevel = level
      if (level == LogLevel.DEBUG) {
        this.levelFilterLabel = this.levelFilterLabelDebug
      } else if (level == LogLevel.INFO) {
        this.levelFilterLabel = this.levelFilterLabelInfo
      } else if (level == LogLevel.WARN) {
        this.levelFilterLabel = this.levelFilterLabelWarn
      } else if (level == LogLevel.ERROR) {
        this.levelFilterLabel = this.levelFilterLabelError
      }
      this.updateContent()
      setTimeout(() => {
        this.applyLineStyles()
      })
    }
  }

}
