import { Component, EventEmitter } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { PlaceholderInfo } from 'src/app/components/placeholder-selector/placeholder-info';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { ConformanceCertificateSettings } from 'src/app/types/conformance-certificate-settings';
import { Observable, map, of, share } from 'rxjs';
import { BsModalService } from 'ngx-bootstrap/modal';
import { BaseCertificateSettingsFormComponent } from '../base-certificate-settings-form.component';
import { ReportService } from 'src/app/services/report.service';
import { HttpResponse } from '@angular/common/http';
import { ErrorService } from 'src/app/services/error.service';

@Component({
    selector: 'app-conformance-certificate-form',
    templateUrl: './conformance-certificate-form.component.html',
    standalone: false
})
export class ConformanceCertificateFormComponent extends BaseCertificateSettingsFormComponent<ConformanceCertificateSettings> {

  placeholders: PlaceholderInfo[] = []
  updatePending = false
  exportPending = false
  domainId?: number
  domainChangedEmitter = new EventEmitter<number>()

  constructor(
    reportService: ReportService,
    conformanceService: ConformanceService,
    modalService: BsModalService,
    popupService: PopupService,
    public dataService: DataService,
    errorService: ErrorService
  ) { super(conformanceService, modalService, popupService, reportService, errorService) }

  getPlaceholders(): PlaceholderInfo[] {
    return [
      { key: Constants.PLACEHOLDER__DOMAIN, value: 'The name of the ' + this.dataService.labelDomainLower() + '.' },
      { key: Constants.PLACEHOLDER__SPECIFICATION, value: 'The name of the ' + this.dataService.labelSpecificationLower() + '.' },
      { key: Constants.PLACEHOLDER__SPECIFICATION_GROUP, value: 'The name of the ' + this.dataService.labelSpecificationGroupLower() + '.' },
      { key: Constants.PLACEHOLDER__SPECIFICATION_GROUP_OPTION, value: 'The name of the ' + this.dataService.labelSpecificationInGroupLower() + '.' },
      { key: Constants.PLACEHOLDER__ACTOR, value: 'The name of the ' + this.dataService.labelActorLower() + ' linked to the conformance statement.' },
      { key: Constants.PLACEHOLDER__ORGANISATION, value: 'The name of the ' + this.dataService.labelOrganisationLower() + ' to be granted the certificate.' },
      { key: Constants.PLACEHOLDER__SYSTEM, value: 'The name of the ' + this.dataService.labelSystemLower() + ' that was used in the tests.' },
      { key: Constants.PLACEHOLDER__SNAPSHOT, value: 'The public name of the relevant conformance snapshot.' },
      { key: Constants.PLACEHOLDER__BADGE, value: 'The badge image corresponding to the current conformance status (original image size).'},
      { key: Constants.PLACEHOLDER__BADGE+'{width}', value: 'The badge image corresponding to the current conformance status (with fixed width in pixels).', select: () => Constants.PLACEHOLDER__BADGE+'{100}' },
      { key: Constants.PLACEHOLDER__REPORT_DATE+'{format}', value: 'The report generation date (with date format).', select: () => Constants.PLACEHOLDER__REPORT_DATE+'{dd/MM/yyyy}' },
      { key: Constants.PLACEHOLDER__LAST_UPDATE_DATE+'{format}', value: 'The conformance last update time (with date format).', select: () => Constants.PLACEHOLDER__LAST_UPDATE_DATE+'{dd/MM/yyyy}' }
    ]
  }

  loadAdditionalData(): Observable<any> {
    if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId != undefined) {
      this.domainId = this.dataService.community!.domainId
      this.domainChangedEmitter.emit(this.domainId)
      return of(true)
    } else {
      return this.conformanceService.getCommunityDomain(this.communityId)
      .pipe(
        map((data) => {
          this.domainId = data?.id
          this.domainChangedEmitter.emit(this.domainId)
        }), share()
      )
    }
  }

  getSettings(): Observable<ConformanceCertificateSettings|undefined> {
    return this.conformanceService.getConformanceCertificateSettings(this.communityId)
  }

  prepareSettingsForUse(): ConformanceCertificateSettings {
    const settingsToUse: ConformanceCertificateSettings = {
      title: this.settings!.title,
      includeTitle: this.settings!.includeTitle == true,
      includeDetails: this.settings!.includeDetails == true,
      includeMessage: this.settings!.includeMessage == true,
      includePageNumbers: this.settings!.includePageNumbers == true,
      includeSignature: this.settings!.includeSignature == true,
      includeTestCases: this.settings!.includeTestCases == true,
      includeTestStatus: this.settings!.includeTestStatus == true,
      community: this.communityId
    }
    if (this.settings!.includeMessage) {
      settingsToUse.message = this.settings!.message
    }
    return settingsToUse
  }

  exportDemoReport(): Observable<HttpResponse<ArrayBuffer>> {
    return this.reportService.exportDemoConformanceCertificateReport(this.communityId, this.reportSettings!, this.prepareSettingsForUse(), this.uploadedStylesheet)
  }

  updateSettings(): Observable<any> {
    return this.reportService.updateConformanceCertificateSettings(this.communityId, this.reportSettings!, this.prepareSettingsForUse(), this.uploadedStylesheet)
  }

  getReportType(): number {
    return Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_CERTIFICATE
  }

}
