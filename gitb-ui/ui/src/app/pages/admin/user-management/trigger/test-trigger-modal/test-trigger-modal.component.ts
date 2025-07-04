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

import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { EditorOptions } from 'src/app/components/code-editor-modal/code-editor-options';
import { TriggerService } from 'src/app/services/trigger.service';
import { Subscription } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import {BaseComponent} from '../../../../base-component.component';

@Component({
    selector: 'app-test-trigger-modal',
    templateUrl: './test-trigger-modal.component.html',
    styles: [],
    standalone: false
})
export class TestTriggerModalComponent extends BaseComponent implements OnInit {

  @Input() request!: string
  @Input() communityId!: number
  @Input() url: string|undefined
  @Input() serviceType!: number
  initialRequest!: string
  callSubscription?: Subscription
  editorOptionsRequest!: EditorOptions
  editorOptionsResponse!: EditorOptions
  editorOptionsResponseError!: EditorOptions
  actionPending = false
  editStep = true
  responseSuccess = false
  response?: string

  constructor(
    private readonly modalRef: BsModalRef,
    public readonly dataService: DataService,
    private readonly popupService: PopupService,
    private readonly triggerService: TriggerService
  ) { super() }

  ngOnInit(): void {
    if (this.serviceType == Constants.TRIGGER_SERVICE_TYPE.JSON) {
      this.request = this.dataService.prettifyJSON(this.request)
    }
    this.initialRequest = this.request
    this.editorOptionsRequest = {
      readOnly: false,
      lineNumbers: true,
      smartIndent: false,
      electricChars: false,
      mode: ((this.serviceType == Constants.TRIGGER_SERVICE_TYPE.GITB)? 'application/xml' : 'application/json')
    }
    this.editorOptionsResponse = {
      readOnly: true,
      lineNumbers: true,
      smartIndent: false,
      electricChars: false,
      mode: ((this.serviceType == Constants.TRIGGER_SERVICE_TYPE.GITB)? 'application/xml' : 'application/json')
    }
    this.editorOptionsResponseError = {
      readOnly: true,
      lineNumbers: true,
      smartIndent: false,
      electricChars: false,
      mode: 'text/plain'
    }
  }

  callService() {
    if (this.url) {
      this.actionPending = true
      this.callSubscription = this.triggerService.test(this.url, this.serviceType, this.request, this.communityId)
        .subscribe((data) => {
          if (data?.texts?.length) {
            if (data.success) {
              this.responseSuccess = true
              if (this.serviceType == Constants.TRIGGER_SERVICE_TYPE.JSON) {
                let valueToShow = ''
                if (data.texts && data.texts.length > 0) {
                  valueToShow = data.texts[0]
                  try {
                    valueToShow = this.dataService.prettifyJSON(valueToShow)
                  } catch (e) {
                    console.warn('Response reported as JSON but could not be parsed')
                    valueToShow = data.texts[0]
                  }
                }
                this.response = valueToShow
              } else {
                this.response = data.texts[0]
              }
            } else {
              this.responseSuccess = false
              this.response = this.dataService.errorArrayToString(data.texts)
            }
            this.editStep = false
          }
        })
      this.callSubscription.add(() => {
        this.actionPending = false
      })
    }
  }

  reset() {
    this.request = this.initialRequest
  }

  copyToClipboard() {
    let contents = this.response
    if (this.editStep) {
      contents = this.request
    }
    if (contents) {
      this.dataService.copyToClipboard(contents).subscribe(() => {
        this.popupService.success('Content copied to clipboard.')
      })
    }
  }

  back() {
    this.editStep = true
  }

  close() {
    if (this.callSubscription) {
      this.callSubscription.unsubscribe()
    }
    this.modalRef.hide()
  }
}
