import { Component, ElementRef, ViewChild } from '@angular/core';
import { BaseReportSettingsFormComponent } from './base-report-settings-form.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { CommunityKeystoreModalComponent } from 'src/app/modals/community-keystore-modal/community-keystore-modal.component';
import { CertificateSettings } from 'src/app/types/certificate-settings';
import { PlaceholderInfo } from 'src/app/components/placeholder-selector/placeholder-info';
import { Observable, mergeMap, of } from 'rxjs';
import { saveAs } from 'file-saver'
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';

@Component({ template: '' })
export abstract class BaseCertificateSettingsFormComponent<T extends CertificateSettings> extends BaseReportSettingsFormComponent {

  @ViewChild("titleField") titleField?: ElementRef;

  settings: Partial<T>|undefined
  placeholders: PlaceholderInfo[] = []
  manageKeystorePending = false
  updatePending = false
  exportPending = false

  constructor(
    protected conformanceService: ConformanceService,
    private modalService: BsModalService,
    private popupService: PopupService,
    protected reportService: ReportService
  ) { super() }

  handleExpanded(): void {
    if (this.settings?.includeTitle && this.titleField) {
      setTimeout(() => {
        this.titleField?.nativeElement.focus()
      }, 1)
    }
  }

  manageKeystore() {
    this.manageKeystorePending = true
    this.conformanceService.getCommunityKeystoreInfo(this.communityId).subscribe((data) => {
      this.modalService.show(CommunityKeystoreModalComponent, {
        class: 'modal-lg',
        initialState: {
          communityId: this.communityId,
          communityKeystore: data
        }
      })
    }).add(() => {
      this.manageKeystorePending = false
    })
  }

  loadData(): Observable<any> {
    this.placeholders = this.getPlaceholders()
    if (this.settings == undefined) {
      return this.getSettings()
      .pipe(
        mergeMap((data) => {
          if (data) {
            this.settings = data
          } else {
            this.settings = {}
          }
          return this.loadAdditionalData()
        })
      )
    } else {
      return of(true)
    }
  }

  preview(type?: string) {
    this.exportPending = true
    this.exportDemoReport(type).subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "conformance_certificate.pdf");
    }).add(() => {
      this.exportPending = false
    })
  }

  update() {
    this.updatePending = true
    this.updateSettings()
    .subscribe(() => {
      this.popupService.success('Conformance certificate settings updated.')
    }).add(() => {
      this.updatePending = false
    })
  }

  includeTitleChanged() {
    if (this.settings!.includeTitle) {
      setTimeout(() => {
        this.titleField?.nativeElement.focus()
      }, 1)      
    }
  }

  loadAdditionalData(): Observable<any> {
    return of(true)
  }

  abstract exportDemoReport(type?: string): Observable<ArrayBuffer>
  abstract getSettings(): Observable<T|undefined>
  abstract updateSettings(): Observable<any>
  abstract getPlaceholders(): PlaceholderInfo[]

}
