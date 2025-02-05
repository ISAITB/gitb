import {Component, ElementRef, EventEmitter, Input, ViewChild} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {ReportService} from 'src/app/services/report.service';
import {saveAs} from 'file-saver';
import {ConformanceOverviewCertificateSettings} from 'src/app/types/conformance-overview-certificate-settings';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {Observable} from 'rxjs';
import {ConformanceOverviewMessage} from 'src/app/pages/admin/user-management/community-reports/conformance-overview-message';
import {ConformanceService} from 'src/app/services/conformance.service';

@Component({
    selector: 'app-conformance-overview-certificate-modal',
    templateUrl: './conformance-overview-certificate-modal.component.html',
    standalone: false
})
export class ConformanceOverviewCertificateModalComponent extends BaseComponent {

  @Input() communityId!: number
  @Input() systemId!: number
  @Input() identifier?: number
  @Input() snapshotId?: number
  @Input() reportLevel!: 'all'|'domain'|'specification'|'group'
  @Input() settings?: ConformanceOverviewCertificateSettings
  @ViewChild("editorContainer") editorContainerRef?: ElementRef;

  message?: string
  messagePending = false
  messageLoaded = false
  exportPending = false
  choice = Constants.REPORT_OPTION_CHOICE.REPORT
  Constants = Constants
  maximised = false
  editorHeight = 300
  editorSizeEmitter = new EventEmitter<number>()

  constructor(
    public dataService: DataService,
    private modalInstance: BsModalRef,
    private reportService: ReportService,
    private conformanceService: ConformanceService
  ) { super() }

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
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) && this.settings) {
      if (!this.messageLoaded) {
        if (this.settings && this.settings.messages && this.settings.messages.length == 1) {
          const messageId = this.settings.messages[0].id!
          this.messagePending = true
          let domainId: number|undefined = undefined
          let groupId: number|undefined = undefined
          let specificationId: number|undefined = undefined
          if (this.reportLevel == 'domain') domainId = this.identifier
          else if (this.reportLevel == 'group') groupId = this.identifier
          else if (this.reportLevel == 'specification') specificationId = this.identifier
          this.conformanceService.getResolvedMessageForConformanceOverviewCertificate(this.communityId, messageId, this.systemId, domainId, groupId, specificationId, this.snapshotId)
          .subscribe((data) => {
            if (data) {
              this.message = this.conformanceService.replaceBadgePlaceholdersInCertificateMessage(data)
            }
          }).add(() => {
            this.messagePending = false
            setTimeout(() => {
              this.messageLoaded = true
            }, 1)
          })
        } else {
          this.messageLoaded = true
        }
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
    let domainId: number|undefined
    let groupId: number|undefined
    let specId: number|undefined
    if (this.reportLevel == 'domain') domainId = this.identifier
    if (this.reportLevel == 'group') groupId = this.identifier
    if (this.reportLevel == 'specification') specId = this.identifier
    let fileName: string
    let contentType: string
    let exportObservable: Observable<ArrayBuffer>
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE) {
      fileName = "conformance_overview_certificate.pdf"
      contentType = "application/pdf"
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        if (this.settings) {
          if (this.textProvided(this.message)) {
            const messageDefinition: ConformanceOverviewMessage = {
              level: this.reportLevel,
              message: this.message
            }
            if (this.reportLevel != 'all') messageDefinition.identifier = this.identifier
            this.settings.messages = [messageDefinition]
          } else {
            this.settings.messages = undefined
          }
        }
        exportObservable = this.reportService.exportConformanceOverviewCertificate(this.communityId, this.systemId, domainId, groupId, specId, this.settings, this.snapshotId)
      } else {
        exportObservable = this.reportService.exportOwnConformanceOverviewCertificateReport(this.systemId, domainId, groupId, specId, this.snapshotId)
      }
    } else {
        fileName = "conformance_overview_report.pdf"
        contentType = "application/pdf"
        exportObservable = this.reportService.exportConformanceOverviewReport(this.systemId, domainId, groupId, specId, this.snapshotId)
    }
    exportObservable.subscribe((data) => {
      const blobData = new Blob([data], {type: contentType});
      saveAs(blobData, fileName);
      this.modalInstance.hide()
    }).add(() => {
      this.exportPending = false
    })
  }

  cancel() {
    this.modalInstance.hide()
  }

}
