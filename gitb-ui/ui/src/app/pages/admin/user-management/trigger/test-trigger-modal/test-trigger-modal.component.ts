import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { EditorOptions } from 'src/app/components/code-editor-modal/code-editor-options';
import { TriggerService } from 'src/app/services/trigger.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-test-trigger-modal',
  templateUrl: './test-trigger-modal.component.html',
  styles: [
  ]
})
export class TestTriggerModalComponent implements OnInit {

  @Input() request!: string
  @Input() communityId!: number
  @Input() url!: string
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
    private modalRef: BsModalRef,
    public dataService: DataService,
    private popupService: PopupService,
    private triggerService: TriggerService
  ) { }

  ngOnInit(): void {
    this.initialRequest = this.request
    this.editorOptionsRequest = {
      readOnly: false,
      lineNumbers: true,
      smartIndent: false,
      electricChars: false,
      mode: 'application/xml'
    }
    this.editorOptionsResponse = {
      readOnly: true,
      lineNumbers: true,
      smartIndent: false,
      electricChars: false,
      mode: 'application/xml'
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
    this.actionPending = true
    this.callSubscription = this.triggerService.test(this.url, this.request, this.communityId)
    .subscribe((data) => {
      if (data?.texts?.length) {
        if (data.success) {
          this.responseSuccess = true
          this.response = data.texts[0]
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
