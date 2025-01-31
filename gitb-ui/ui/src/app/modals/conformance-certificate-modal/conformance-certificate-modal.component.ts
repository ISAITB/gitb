import {Component, ElementRef, EventEmitter, Input, ViewChild} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {ReportService} from 'src/app/services/report.service';
import {ConformanceCertificateSettings} from 'src/app/types/conformance-certificate-settings';
import {saveAs} from 'file-saver';
import {Observable} from 'rxjs';
import {ConformanceService} from 'src/app/services/conformance.service';

@Component({
  selector: 'app-conformance-certificate-modal',
  templateUrl: './conformance-certificate-modal.component.html'
})
export class ConformanceCertificateModalComponent {

  @Input() communityId!: number
  @Input() actorId!: number
  @Input() systemId!: number
  @Input() snapshotId?: number
  @Input() format!: 'xml'|'pdf'
  @Input() settings?: ConformanceCertificateSettings
  @Input() certificateEnabled!: boolean
  @ViewChild("editorContainer") editorContainerRef?: ElementRef;

  exportPending = false
  messagePending = false
  messageLoaded = false
  choice = Constants.REPORT_OPTION_CHOICE.REPORT
  Constants = Constants
  maximised = false
  editorHeight = 300
  editorSizeEmitter = new EventEmitter<number>()

  constructor(
    private dataService: DataService,
    private modalInstance: BsModalRef,
    private reportService: ReportService,
    private conformanceService: ConformanceService
  ) { }

  expandModal(): void {
    this.modalInstance.setClass("conformanceCertificatePreview")
    this.maximised = true
    if (this.editorContainerRef) {
      const editorBottom = this.editorContainerRef.nativeElement.getBoundingClientRect().bottom
      const windowHeight = window.innerHeight
      this.editorSizeEmitter.emit(this.editorHeight + windowHeight - editorBottom - 45)
    }
  }

  choiceChanged() {
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE && this.settings) {
      if (!this.messageLoaded) {
        this.messagePending = true
        this.conformanceService.getResolvedMessageForConformanceStatementCertificate(this.communityId, this.systemId, this.actorId, this.snapshotId)
        .subscribe((data) => {
          if (data) {
            this.settings!.message = this.conformanceService.replaceBadgePlaceholdersInCertificateMessage(data)
          }
        }).add(() => {
          this.messagePending = false
          setTimeout(() => {
            this.messageLoaded = true
          }, 1)
        })
      }
    }
  }

  certificateChoicesVisible() {
    if (this.settings?.includeTitle) {
      this.dataService.focus('title')
    }
  }

  includeTitleChanged() {
    if (this.settings?.includeTitle) {
      this.dataService.focus('title')
    }
  }

  generate() {
    this.exportPending = true
    let fileName: string
    let contentType: string
    let exportObservable: Observable<ArrayBuffer>
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE) {
      fileName = "conformance_certificate.pdf"
      contentType = "application/pdf"
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        exportObservable = this.reportService.exportConformanceCertificate(this.communityId, this.actorId, this.systemId, this.settings, this.snapshotId)
      } else {
        exportObservable = this.reportService.exportOwnConformanceCertificateReport(this.actorId, this.systemId, this.snapshotId)
      }
    } else {
      const includeDetails = this.choice == Constants.REPORT_OPTION_CHOICE.DETAILED_REPORT
      if (this.format == 'pdf') {
        contentType = "application/pdf"
        fileName = "conformance_report.pdf"
        exportObservable = this.reportService.exportConformanceStatementReport(this.actorId, this.systemId, includeDetails, this.snapshotId)
      } else {
        contentType = "application/xml"
        fileName = "conformance_report.xml"
        if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
          exportObservable = this.reportService.exportConformanceStatementReportInXML(this.actorId, this.systemId, this.communityId, includeDetails, this.snapshotId)
        } else {
          exportObservable = this.reportService.exportOwnConformanceStatementReportInXML(this.actorId, this.systemId, includeDetails, this.snapshotId)
        }
      }
    }
    exportObservable.subscribe((data) => {
      const blobData = new Blob([data], {type: contentType})
      saveAs(blobData, fileName)
      this.modalInstance.hide()
    }).add(() => {
      this.exportPending = false
    })

  }

  cancel() {
    this.modalInstance.hide()
  }

}
