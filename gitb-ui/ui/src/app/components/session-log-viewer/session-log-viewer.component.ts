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

import {ChangeDetectionStrategy, Component, EventEmitter, HostBinding, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {saveAs} from 'file-saver';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {CodeEditorComponent} from '../code-editor/code-editor.component';
import {EditorOptions} from '../code-editor-modal/code-editor-options';
import {LineInfo} from '../session-log-modal/line-info';
import {LogLevel} from '../../types/log-level';
import {Constants} from '../../common/constants';
import {BaseComponent} from '../../pages/base-component.component';

/**
 * Presents a test session's log in a code editor with its display controls (follow latest, log level).
 * Switching the displayed session is done by changing the inputs.
 */
@Component({
  selector: 'app-session-log-viewer',
  templateUrl: './session-log-viewer.component.html',
  styleUrls: ['./session-log-viewer.component.less'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false
})
export class SessionLogViewerComponent extends BaseComponent implements OnInit, OnChanges, OnDestroy {

  private static readonly LINE_PARTS_REGEX = /^(.+)/gm
  private static instanceCounter = 0

  @Input() messages: string[] = []
  @Input() messageEmitter?: EventEmitter<string>
  @Input() layout: 'modal'|'panel' = 'modal'
  /** Text shown first in the panel's control bar (e.g. the test case the log belongs to). */
  @Input() contextLabel?: string
  @Input() tail = true
  @Output() tailChange = new EventEmitter<boolean>()
  @Output() closed = new EventEmitter<void>()

  @ViewChild('codeEditor', {static: false}) codeEditor?: CodeEditorComponent

  @HostBinding('class.panel') get panelLayout() { return this.layout == 'panel' }

  readonly instanceId =SessionLogViewerComponent.instanceCounter++
  editorOptions?: EditorOptions
  lines: LineInfo[] = []
  contentLines: LineInfo[] = []
  content = ''
  minimumLogLevel = LogLevel.DEBUG
  LogLevel = LogLevel
  Constants = Constants

  private emitterSubscription?: Subscription
  private loaded = false

  constructor(
    private readonly dataService: DataService,
    private readonly popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    this.editorOptions = {
      readOnly: true,
      lineNumbers: true,
      mode: 'text/plain',
      download: {
        fileName: 'log.txt',
        mimeType: 'text/plain'
      }
    }
    this.initialiseSource()
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.editorOptions && (changes['messages'] || changes['messageEmitter']) && !(changes['messages']?.firstChange && changes['messageEmitter']?.firstChange)) {
      this.initialiseSource()
    }
  }

  ngOnDestroy(): void {
    this.emitterSubscription?.unsubscribe()
  }

  onEditorLoaded() {
    this.loaded = true
    this.applyLineStyles()
    this.codeEditor?.refresh()
  }

  private initialiseSource() {
    this.emitterSubscription?.unsubscribe()
    this.emitterSubscription = undefined
    // Work on a copy so that live messages never mutate the caller's array.
    this.messages = this.messages.slice()
    this.lines = []
    this.initialiseLines(this.messages)
    this.updateContent()
    if (this.loaded) {
      setTimeout(() => this.applyLineStyles())
      if (this.tail) this.scrollToLast()
    }
    if (this.messageEmitter) {
      this.emitterSubscription = this.messageEmitter.subscribe((newMessage) => {
        this.messages.push(newMessage)
        const createdLines = this.initialiseLines([newMessage])
        for (let line of createdLines) {
          if (line.level >= this.minimumLogLevel) {
            this.contentLines.push(line)
            // Do not update the content directly because this causes a full editor refresh.
            this.codeEditor?.appendText(line.text+'\n')
            if (this.tail) {
              this.scrollToLast()
            }
          }
        }
        setTimeout(() => this.applyLineStyles())
      })
    }
  }

  scrollToLast() {
    if (this.codeEditor) {
      setTimeout(() => this.codeEditor?.scrollToLine(this.codeEditor.lineCount), 100)
    }
  }

  private initialiseLines(newMessages: string[]) {
    let previousLevel = LogLevel.INFO
    const createdLines: LineInfo[] = []
    for (let message of newMessages) {
      const messageParts = message.replace('\r', '\n').match(SessionLogViewerComponent.LINE_PARTS_REGEX)
      if (messageParts) {
        for (let part of messageParts) {
          if (part.length > 0) {
            const partLevel = this.dataService.logMessageLevel(part, previousLevel)
            previousLevel = partLevel
            createdLines.push({ text: part, level: partLevel })
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

  private applyLineStyles() {
    for (let i=0; i < this.contentLines.length; i++) {
      // Line numbers used by the editor are 1-based.
      this.codeEditor?.addLineClass(i + 1, 'log-level '+this.logLevelToString(this.contentLines[i].level))
    }
  }

  private logLevelToString(level: LogLevel) {
    if (level == LogLevel.DEBUG) return 'debug'
    else if (level == LogLevel.INFO) return 'info'
    else if (level == LogLevel.WARN) return 'warn'
    else return 'error'
  }

  applyMinimumLogLevel() {
    this.updateContent()
    setTimeout(() => this.applyLineStyles())
  }

  tailUpdated(value: boolean) {
    this.tail = value
    this.tailChange.emit(value)
    if (value) {
      this.scrollToLast()
    }
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

}
