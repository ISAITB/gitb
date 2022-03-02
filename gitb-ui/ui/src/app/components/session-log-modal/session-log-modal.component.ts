import { Component, Input } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { BaseCodeEditorModalComponent } from '../base-code-editor-modal/base-code-editor-modal.component';
import { LineInfo } from './line-info';
import { LogLevel } from './log-level';

@Component({
  selector: 'app-session-log-modal',
  templateUrl: './session-log-modal.component.html',
  styleUrls: ['./session-log-modal.component.less']
})
export class SessionLogModalComponent extends BaseCodeEditorModalComponent {

  static LOG_MESSAGE_REGEX = /^\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\] (DEBUG|ERROR|WARN|INFO) /
  static LINE_PARTS_REGEX = /^(.+)/gm

  @Input() messages?: string[]
  lines: LineInfo[] = []
  minimumLogLevel = LogLevel.DEBUG
  content = ''
  contentLines: LineInfo[] = []

  LogLevel = LogLevel
  levelFilterLabelDebug = 'Show all messages'
  levelFilterLabelInfo = 'Show at least information messages'
  levelFilterLabelWarn = 'Show at least warning messages'
  levelFilterLabelError = 'Show error messages'
  levelFilterLabel = this.levelFilterLabelDebug

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
    this.initialiseLines()
    this.updateContent()
  }

  private initialiseLines() {
    if (this.messages != undefined) {
      let previousLevel = LogLevel.INFO
      for (let message of this.messages) {
        const messageParts = message.replace('\r', '\n').match(SessionLogModalComponent.LINE_PARTS_REGEX)
        if (messageParts) {
          for (let part of messageParts) {
            if (part.length > 0) {
              const partLevel = this.messageLevel(part, previousLevel)
              previousLevel = partLevel
              this.lines.push({
                text: part,
                level: partLevel
              })
            }
          }
        }
      }
    }
  }

  private updateContent() {
    const tempLines: LineInfo[] = []
    let tempContent: string = ''
    for (let line of this.lines) {
      if (line.level >= this.minimumLogLevel) {
        tempLines.push(line)
        tempContent += line.text + '\n'
      }
    }
    this.contentLines = tempLines
    this.content = tempContent
  }

  private messageLevel(message: string, defaultLevel: LogLevel): LogLevel {
    let logLevel = defaultLevel
    let match = SessionLogModalComponent.LOG_MESSAGE_REGEX.exec(message)
    if (match != null) {
      const logLevelStr = match[1]
      if (logLevelStr == 'DEBUG') {
        logLevel = LogLevel.DEBUG
      } else if (logLevelStr == 'INFO') {
        logLevel = LogLevel.INFO
      } else if (logLevelStr == 'WARN') {
        logLevel = LogLevel.WARN
      } else if (logLevelStr == 'ERROR') {
        logLevel = LogLevel.ERROR
      }
    }
    return logLevel
  }

  applyLineStyles(): boolean {
    if (this.codeEditor?.codeMirror) {
      for (let i=0; i < this.contentLines.length; i++) {
        this.codeEditor.codeMirror.addLineClass(i, 'text', 'log-level '+this.logLevelToString(this.contentLines[i].level) + (' line-'+i))
      }
    }
    return true
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
