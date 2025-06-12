import { Component, Input, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { TestService } from 'src/app/services/test.service';
import { AnyContent } from '../../any-content';
import { ReportSupport } from '../report-support';
import { StepReport } from '../step-report';
import { saveAs } from 'file-saver'
import { HtmlService } from 'src/app/services/html.service';

@Component({
    selector: 'app-any-content-view',
    templateUrl: './any-content-view.component.html',
    styleUrls: ['./any-content-view.component.less'],
    standalone: false
})
export class AnyContentViewComponent extends ReportSupport implements OnInit {

  @Input() context!: AnyContent
  @Input() fileNameDownload?: string
  @Input() report?: StepReport
  @Input() sessionId?: string
  @Input() noBorder = false
  @Input() noMargin = false
  @Input() root = true
  @Input() forceDisplay = false
  @Input() preserveName = true

  Constants = Constants

  name?: string
  value?: string
  showValueInline = true
  downloadPending = false
  openPending = false
  withItems = false
  withName = false
  withValue = false
  hoveringTitle = false
  breakNameText = true
  breakValueText = true
  showName = false
  collapsed = true

  constructor(
    private testService: TestService,
    reportService: ReportService,
    dataService: DataService,
    modalService: BsModalService,
    private popupService: PopupService,
    private confirmationDialogService: ConfirmationDialogService,
    htmlService: HtmlService
  ) { super(modalService, reportService, htmlService, dataService) }

  ngOnInit(): void {
    this.name = this.context.name
    if (this.textProvided(this.context.valueToUse)) {
      this.value = this.context.valueToUse
      this.withValue = true
    }
    this.withName = this.textProvided(this.name)
    this.withItems = this.context?.item != undefined
    if (this.root) {
      this.collapsed = false
    } else {
      if (this.withItems && !this.withName && !this.withValue) {
        this.name = ""
        this.withName = true
      }
    }
    if (!this.withItems && this.withName && !this.withValue && !this.preserveName) {
      this.value = this.name
      this.name = undefined
      this.withName = false
      this.withValue = true
    }
    this.showValueInline = this.withValue
      && (!this.isFileReference(this.context) && (this.value!.length <= 100 || this.forceDisplay))
      && !(this.context.embeddingMethod == 'BASE64' && this.textProvided(this.context.valueToUse))
    if (this.showValueInline) {
      this.breakValueText = this.value!.indexOf(" ") < 0
    }
    this.breakNameText = this.name != undefined && this.name.indexOf(" ") < 0
  }

  open(lineNumber?: number) {
    try {
      this.openPending = true
      this.commonOpen(this.context, this.sessionId!, this.report?.reports?.assertionReports, lineNumber)
      .subscribe(() => {
        this.openPending = false
      })
    } catch (e) {
      this.openPending = false
      this.confirmationDialogService.confirmed('Unable to open editor', 'It is not possible to display this content as text in an editor, only download it as a file.', 'Download', 'Cancel')
      .subscribe(() => {
        this.download()
      })
    }
  }

  private toBlob(mimeType: string) {
    let bb: Blob
    if (this.context!.embeddingMethod == 'BASE64' || this.dataService.isImageType(mimeType)) {
      bb = this.dataService.b64toBlob(this.context.value!, mimeType)
    } else {
      bb = new Blob([this.context.value!], {type: mimeType})
    }
    return bb
  }

  download() {
    if (this.isFileReference(this.context) && this.sessionId) {
      this.downloadPending = true
      this.downloadFileReference(this.sessionId, this.context, false)
      .subscribe((data) => {
        if (data.dataAsBytes) {
          const bb = new Blob([data.dataAsBytes], {type: data.mimeType})
          if (this.fileNameDownload != undefined && this.preserveName) {
            saveAs(bb, this.fileNameDownload)
          } else {
            saveAs(bb, 'file'+this.extension(data.mimeType))
          }
        }
      }).add(() => {
        this.downloadPending = false
      })
    } else {
      if (this.context.mimeType == undefined) {
        this.downloadPending = true
        this.testService.getBinaryMetadata(this.context.value!, (this.context.embeddingMethod == 'BASE64'))
        .subscribe((info) => {
          const bb = this.toBlob(info.mimeType)
          if (this.fileNameDownload != undefined) {
            saveAs(bb, this.fileNameDownload)
          } else {
            saveAs(bb, 'file'+info.extension)
          }
        }).add(() => {
          this.downloadPending = false
        })
      } else {
        const bb = this.toBlob(this.context.mimeType)
        if (this.fileNameDownload != undefined) {
          saveAs(bb, this.fileNameDownload)
        } else {
          saveAs(bb, 'file'+this.extension(this.context.mimeType))
        }
      }
    }
  }

  copy() {
    this.dataService.copyToClipboard(this.value).subscribe(() => {
      this.popupService.success('Value copied to clipboard.')
    })
  }

  private extension(mimeType: string|undefined) {
    let extension = this.dataService.extensionFromMimeType(mimeType)
    if (extension == undefined) {
      if (this.context.embeddingMethod == 'BASE64') {
        extension = '.bin'
      } else {
        extension = '.txt'
      }
    }
    return extension
  }

  containerClicked() {
    if (!this.root) {
      this.collapsed = !this.collapsed
    }
  }

  hoverUpdate(hovering: boolean) {
    this.hoveringTitle = hovering
  }
}
