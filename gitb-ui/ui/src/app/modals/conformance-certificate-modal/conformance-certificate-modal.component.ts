import {Component, ElementRef, EventEmitter, Input, OnInit, ViewChild} from '@angular/core';
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
  templateUrl: './conformance-certificate-modal.component.html',
  styleUrls: ['./conformance-certificate-modal.component.less'],
  standalone: false
})
export class ConformanceCertificateModalComponent implements OnInit {

  @Input() communityId!: number
  @Input() actorId!: number
  @Input() systemId!: number
  @Input() snapshotId?: number
  @Input() format!: 'xml'|'pdf'
  @Input() settings?: ConformanceCertificateSettings
  @Input() certificateEnabled!: boolean
  @Input() testCaseCount?: number
  @ViewChild("editorContainer") editorContainerRef?: ElementRef;
  @ViewChild("reportOption") reportOptionRef?: ElementRef;
  @ViewChild("certificateOption") certificateOptionRef?: ElementRef;

  exportPending = false
  messagePending = false
  messageLoaded = false
  statementReportDisabled = false
  previousChoice = Constants.REPORT_OPTION_CHOICE.REPORT
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

  ngOnInit(): void {
    this.statementReportDisabled = this.testCaseCount != undefined && this.testCaseCount > this.dataService.configuration.conformanceStatementReportMaxTestCases
  }

  expandModal(): void {
    this.modalInstance.setClass("conformanceCertificatePreview")
    this.maximised = true
    if (this.editorContainerRef) {
      const editorBottom = this.editorContainerRef.nativeElement.getBoundingClientRect().bottom
      const windowHeight = window.innerHeight
      this.editorSizeEmitter.emit(this.editorHeight + windowHeight - editorBottom - 45)
    }
  }

  private handleCertificateOption() {
    if (this.settings) {
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

  choiceChanged(event?: Event) {
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE) {
      this.handleCertificateOption()
      this.previousChoice = this.choice
    } else if (this.choice == Constants.REPORT_OPTION_CHOICE.DETAILED_REPORT && this.statementReportDisabled) {
      // We want to skip this option
      setTimeout(() => {
        (event?.target as HTMLElement).blur();
        if (this.previousChoice == Constants.REPORT_OPTION_CHOICE.REPORT) {
          if (this.format == 'pdf' && this.certificateEnabled) {
            this.choice = Constants.REPORT_OPTION_CHOICE.CERTIFICATE
            if (this.certificateOptionRef) {
              this.certificateOptionRef.nativeElement.focus()
            }
            this.handleCertificateOption()
          } else {
            this.choice = Constants.REPORT_OPTION_CHOICE.REPORT
            if (this.reportOptionRef) {
              this.reportOptionRef.nativeElement.focus()
            }
          }
          this.previousChoice = this.choice
        } else if (this.previousChoice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE) {
          this.choice = Constants.REPORT_OPTION_CHOICE.REPORT
          this.previousChoice = this.choice
          if (this.reportOptionRef) {
            this.reportOptionRef.nativeElement.focus()
          }
        }
      }, 1)
    } else {
      this.previousChoice = this.choice
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
      const includeDetails = this.choice == Constants.REPORT_OPTION_CHOICE.DETAILED_REPORT && !this.statementReportDisabled
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
