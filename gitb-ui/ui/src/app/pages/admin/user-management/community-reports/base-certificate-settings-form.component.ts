import { Component, ElementRef, ViewChild } from '@angular/core';
import { BaseReportSettingsFormComponent } from './base-report-settings-form.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { CertificateSettings } from 'src/app/types/certificate-settings';
import { PlaceholderInfo } from 'src/app/components/placeholder-selector/placeholder-info';
import { Observable, forkJoin, mergeMap, of } from 'rxjs';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { Constants } from 'src/app/common/constants';
import { HttpResponse } from '@angular/common/http';
import { ErrorService } from 'src/app/services/error.service';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseCertificateSettingsFormComponent<T extends CertificateSettings> extends BaseReportSettingsFormComponent {

  @ViewChild("titleField") titleField?: ElementRef;

  settings: Partial<T>|undefined
  useCustomService = false
  placeholders: PlaceholderInfo[] = []
  updatePending = false
  exportPending = false
  reportType!: number

  constructor(
    conformanceService: ConformanceService,
    modalService: BsModalService,
    private popupService: PopupService,
    reportService: ReportService,
    errorService: ErrorService
  ) { super(conformanceService, modalService, reportService, errorService) }

  previewEnabled() {
    return this.reportSettings?.customPdfs == false || (this.textProvided(this.reportSettings?.customPdfService) && (!this.reportSettings?.customPdfsWithCustomXml || this.reportSettings?.stylesheetExists))
  }

  handleExpanded(): void {
    this.focusFirstTextField()
  }

  reportConfigurationTypeChanged() {
    this.focusFirstTextField()
  }

  protected focusFirstTextField() {
    if (this.reportSettings?.customPdfs && this.serviceField) {
      this.focusServiceField()
    } else if (this.settings?.includeTitle && this.titleField) {
      this.focusTitleField()
    }
  }

  protected focusTitleField() {
    setTimeout(() => {
      this.titleField?.nativeElement.focus()
    }, 1)
  }

  loadData(): Observable<any> {
    this.placeholders = this.getPlaceholders()
    this.reportType = this.getReportType()
    if (this.settings == undefined) {
      const loadCertificateSettings = this.getSettings()
      const loadReportSettings = this.reportService.loadReportSettings(this.communityId, Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_CERTIFICATE)
      return forkJoin([loadCertificateSettings, loadReportSettings]).pipe(
        mergeMap((data) => {
          if (data[0]) {
            this.settings = data[0]
          } else {
            this.settings = {}
          }
          if (data[1]) {
            this.reportSettings = data[1]
            if (this.reportSettings.stylesheetExists) {
              this.stylesheetNameToShow = 'stylesheet.xslt'
            }
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
    this.exportDemoReport(type)
    .subscribe((response) => {
      this.handlePdfPreviewResult(response, "conformance_certificate.pdf")
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

  abstract exportDemoReport(type?: string): Observable<HttpResponse<ArrayBuffer>>
  abstract getSettings(): Observable<T|undefined>
  abstract updateSettings(): Observable<any>
  abstract getPlaceholders(): PlaceholderInfo[]
  abstract getReportType(): number

}
