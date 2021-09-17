import { AfterViewInit, Component, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { FileData } from 'src/app/types/file-data.type';
import { FeedbackType } from './feedback-type.type';

@Component({
  selector: 'app-contact-support',
  templateUrl: './contact-support.component.html',
  styles: [
  ]
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

  constructor(
    private dataService: DataService,
    private modalInstance: BsModalRef,
    private accountService: AccountService,
    private popupService: PopupService
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

  validateAttachments() {
    let valid = true
    if (this.attachments.length > 0) {
        if (this.attachments.length > this.maximumAttachments) {
          this.addAlertError('A maximum of '+this.maximumAttachments+' attachments can be provided')
          valid = false
        }
        let totalSize = 0
        for (let attachment of this.attachments) {
          totalSize += attachment.size
        }
        let maxSizeMBs = 5
        if (this.dataService.configuration !== undefined && this.dataService.configuration.emailAttachmentsMaxSize != undefined) {
          maxSizeMBs = this.dataService.configuration.emailAttachmentsMaxSize
        }
        let maxSize = maxSizeMBs * 1024 * 1024
        if (totalSize > maxSize) {
          this.addAlertError('The total size of attachments cannot exceed '+maxSizeMBs+' MBs.')
          valid = false
        }
    }
    return valid
  }

  send() {
    if (!this.sendDisabled()) {
      this.clearAlerts()
      const emailValid = this.requireValidEmail(this.contactAddress, 'Please enter a valid email address.')
      const feedbackSelected = this.requireDefined(this.feedback, 'Please select the feedback type.')
      const attachmentsValid = this.validateAttachments()
      if (emailValid && feedbackSelected && attachmentsValid) {
        this.sendPending = true
        this.accountService.submitFeedback(this.contactAddress!, this.feedback!.id, this.feedback!.description, this.contentMessage!, this.attachments)
        .subscribe((data) => {
          if (data && data.error_code) {
            this.addAlertError(data.error_description)
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
    this.attachments.push(file)
  }

  removeAttachment(index: number) {
    this.attachments.splice(index, 1)
  }

  showUpload() {
    return this.maximumAttachments > 0
  }
  
}
