import { Component, Input, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { TestService } from 'src/app/services/test.service';
import { AnyContent } from '../../any-content';
import { ReportSupport } from '../report-support';
import { StepReport } from '../step-report';

@Component({
  selector: 'app-any-content-view',
  templateUrl: './any-content-view.component.html'
})
export class AnyContentViewComponent extends ReportSupport implements OnInit {

  @Input() context!: AnyContent
  @Input() fileNameDownload?: string
  @Input() outputContentType?: string
  @Input() report?: StepReport

  value?: string
  isValueTooLong = false

  constructor(
    private testService: TestService,
    private dataService: DataService,
    modalService: BsModalService,
    private confirmationDialogService: ConfirmationDialogService
  ) { super(modalService) }

  ngOnInit(): void {
    this.value = this.context.valueToUse
    if (this.value != undefined) {
      this.isValueTooLong = this.value?.length! > 100
    }
  }

  open(lineNumber?: number) {
    try {
      let valueToShow
      if (this.context.embeddingMethod == 'BASE64') {
        if (this.dataService.isDataURL(this.value!)) {
          valueToShow = atob(this.dataService.base64FromDataURL(this.value!))
        } else {
          valueToShow = atob(this.value!)
        }
      } else {
        valueToShow = this.value!
      }
      this.openEditorWindow(
        this.context.name,
        valueToShow,
        this.report?.reports?.assertionReports,
        lineNumber, 
        this.context.mimeType)
    } catch (e) {
      this.confirmationDialogService.confirmed('Unable to open editor', 'It is not possible to display this content as text in an editor, only download it as a file.', 'Download as file', 'Cancel')
      .subscribe(() => {
        this.download()
      })
    }
  }

  private toBlob(mimeType: string) {
    let bb: Blob
    if (this.context!.embeddingMethod == 'BASE64') {
      bb = this.dataService.b64toBlob(this.context.value!, mimeType)
    } else {
      bb = new Blob([this.context.value!], {type: mimeType})
    }
    return bb
  }

  download() {
    if (this.outputContentType == undefined) {
      this.testService.getBinaryMetadata(this.context.value!, (this.context.embeddingMethod == 'BASE64'))
      .subscribe((info) => {
        const bb = this.toBlob(info.mimeType)
        if (this.fileNameDownload != undefined) {
          saveAs(bb, this.fileNameDownload)
        } else {
          saveAs(bb, 'file'+info.extension)
        }
      })
    } else {
      const bb = this.toBlob(this.outputContentType)
      if (this.fileNameDownload != undefined) {
        saveAs(bb, this.fileNameDownload)
      } else {
        let extension = this.dataService.extensionFromMimeType(this.outputContentType)
        if (extension == undefined) {
          if (this.context.embeddingMethod == 'BASE64') {
            extension = '.bin'
          } else {
            extension = '.txt'
          }
        }
        saveAs(bb, 'file'+extension)
      }
    }
  }

}
