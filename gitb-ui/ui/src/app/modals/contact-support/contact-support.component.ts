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

import { AfterViewInit, Component, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { FileData } from 'src/app/types/file-data.type';
import { FeedbackType } from './feedback-type.type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-contact-support',
    templateUrl: './contact-support.component.html',
    styles: [],
    standalone: false
})
export class ContactSupportComponent extends BaseComponent implements OnInit, AfterViewInit {

  attachments: FileData[] = []
  feedbackTypes: FeedbackType[] = [
    {id: 0, description: "Technical issue"},
    {id: 1, description: "Feature request"},
    {id: 2, description: "Question on usage"},
    {id: 3, description: "Other"}
  ]
  surveyAddress?: string
  sendPending = false
  feedback?: FeedbackType
  contactAddress?: string
  contentMessage: string = ''
  maximumAttachments = 0
  validation = new ValidationState()

  constructor(
    private readonly dataService: DataService,
    private readonly modalInstance: BsModalRef,
    private readonly accountService: AccountService,
    private readonly popupService: PopupService
  ) {
    super()
  }

  ngOnInit(): void {
    if (this.dataService.user && this.dataService.configuration.ssoEnabled) {
      this.contactAddress = this.dataService.user.email
    }
    this.surveyAddress = this.dataService.configuration.surveyAddress
    if (this.dataService.configuration !== undefined && this.dataService.configuration.emailAttachmentsMaxCount != undefined) {
      this.maximumAttachments = this.dataService.configuration.emailAttachmentsMaxCount
    }
  }

  ngAfterViewInit(): void {
    this.dataService.focus('contact')
  }

  sendDisabled() {
    return !(this.textProvided(this.contactAddress) && this.feedback && this.textProvided(this.contentMessage))
  }

  showSurveyLink() {
    return this.dataService.configuration !== undefined && this.dataService.configuration.surveyEnabled == true
  }

  private validateAttachments(): string|undefined {
    if (this.attachments.length > 0) {
        if (this.attachments.length > this.maximumAttachments) {
          return 'A maximum of '+this.maximumAttachments+' attachments can be provided'
        }
        let totalSize = 0
        for (let attachment of this.attachments) {
          totalSize += attachment.size
        }
        let maxSizeMBs = 5
        if (this.dataService.configuration !== undefined && this.dataService.configuration.emailAttachmentsMaxSize != undefined) {
          maxSizeMBs = this.dataService.configuration.emailAttachmentsMaxSize
        }
        const maxSize = maxSizeMBs * 1024 * 1024
        if (totalSize > maxSize) {
          return 'The total size of attachments cannot exceed '+maxSizeMBs+' MBs.'
        }
    }
    return undefined
  }

  send() {
    if (!this.sendDisabled()) {
      this.validation.clearErrors()
      const emailInvalid = !this.isValidEmail(this.contactAddress)
      if (emailInvalid) {
        this.validation.invalid('email', 'Please enter a valid email address.')
      }
      const attachmentsError = this.validateAttachments()
      const attachmentsInvalid = attachmentsError != undefined
      if (attachmentsInvalid) {
        this.allAttachmentsInvalid(attachmentsError)
      }
      if (!emailInvalid && !attachmentsInvalid) {
        this.sendPending = true
        this.accountService.submitFeedback(this.contactAddress!, this.feedback!.id, this.feedback!.description, this.contentMessage!, this.attachments)
        .subscribe((data) => {
          if (data && data.error_code) {
            if (data.error_hint && data.error_hint.includes("files")) {
              // Error that relates to all attachments.
              this.allAttachmentsInvalid(data.error_description)
            }
            this.validation.applyError(data)
            this.sendPending = false
          } else {
            this.cancel()
            this.popupService.success('Your feedback was submitted successfully.')
          }
        }).add(() => {
          this.sendPending = false
        })
      }
    }
  }

  cancel() {
    this.modalInstance.hide()
  }

  attachFile(file: FileData) {
    this.clearAttachmentWarnings()
    this.validation.set("file"+this.attachments.length)
    this.attachments.push(file)
  }

  removeAttachment(index: number) {
    this.clearAttachmentWarnings()
    this.attachments.splice(index, 1)
  }

  private allAttachmentsInvalid(feedback?: string) {
    for (let index = 0; index < this.attachments.length; index++) {
      this.validation.invalid('file'+index, feedback)
    }
  }

  private clearAttachmentWarnings() {
    for (let index = 0; index < this.attachments.length; index++) {
      this.validation.clear('file'+index)
    }
  }

  showUpload() {
    return this.maximumAttachments > 0
  }

}
