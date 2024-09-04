import { Injectable } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConformanceCertificateModalComponent } from '../modals/conformance-certificate-modal/conformance-certificate-modal.component';
import { ConformanceCertificateSettings } from '../types/conformance-certificate-settings';
import { ConformanceService } from './conformance.service';
import { Observable, forkJoin, mergeMap, of } from 'rxjs';
import { ConformanceOverviewCertificateModalComponent } from '../modals/conformance-overview-certificate-modal/conformance-overview-certificate-modal.component';
import { ConformanceOverviewCertificateSettings } from '../types/conformance-overview-certificate-settings';
import { ReportService } from './report.service';
import { saveAs } from 'file-saver'
import { DataService } from './data.service';
import { Constants } from '../common/constants';

@Injectable({
  providedIn: 'root'
})
export class ReportSupportService {

  constructor(
    private conformanceService: ConformanceService,
    private modalService: BsModalService,
    private reportService: ReportService,
    private dataService: DataService
  ) {}

  private showConformanceOverviewModal(communityId: number, systemId: number, identifier: number|undefined, reportLevel: 'all'|'domain'|'specification'|'group', snapshotId: number|undefined, settings: ConformanceOverviewCertificateSettings|undefined) {
    this.modalService.show(ConformanceOverviewCertificateModalComponent, {
      class: 'modal-lg',
      initialState: {
        settings: settings,
        communityId: communityId,
        systemId: systemId,
        identifier: identifier,
        snapshotId: snapshotId,
        reportLevel: reportLevel
      }
    })
  }

  private showConformanceStatementModal(communityId: number, actorId: number, systemId: number, snapshotId: number|undefined, format: 'xml'|'pdf', settings: ConformanceCertificateSettings|undefined, certificateEnabled: boolean) {
    this.modalService.show(ConformanceCertificateModalComponent, {
      class: 'modal-lg',
      initialState: {
        settings: settings,
        communityId: communityId,
        actorId: actorId,
        systemId: systemId,
        snapshotId: snapshotId,
        format: format,
        certificateEnabled: certificateEnabled
      }
    })
  }

  handleConformanceStatementReport(communityId: number, actorId: number, systemId: number, snapshotId: number|undefined, format: 'xml'|'pdf', certificateEnabled: boolean): Observable<any> {
    if (format == 'xml') {
      this.showConformanceStatementModal(communityId, actorId, systemId, snapshotId, 'xml', undefined, false)
      return of(true)
    } else {
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        const $reportSettings = this.reportService.loadReportSettings(communityId, Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_CERTIFICATE)
        const $certificateSettings = this.conformanceService.getConformanceCertificateSettings(communityId)
        return forkJoin([$reportSettings, $certificateSettings]).pipe(
          mergeMap((data) => {
            let certificateSettingsToUse = data[1]
            if (data[0].customPdfs) {
              // Don't present settings to override as we're using a custom generation service.
              certificateSettingsToUse = undefined
            }
            this.showConformanceStatementModal(communityId, actorId, systemId, snapshotId, 'pdf', certificateSettingsToUse, true)
            return of(true)
          })
        )
      } else {
        this.showConformanceStatementModal(communityId, actorId, systemId, snapshotId, 'pdf', undefined, certificateEnabled)
        return of(true)
      }
    }
  }

  handleConformanceOverviewReport(communityId: number, systemId: number, identifier: number|undefined, reportLevel: 'all'|'domain'|'specification'|'group', snapshotId: number|undefined, format: 'xml'|'pdf', status: string): Observable<any> {
    let domainId: number|undefined
    let groupId: number|undefined
    let specId: number|undefined
    if (reportLevel == 'domain') domainId = identifier
    if (reportLevel == 'group') groupId = identifier
    if (reportLevel == 'specification') specId = identifier
    if (format == 'xml') {
      let observable: Observable<ArrayBuffer>
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        observable = this.reportService.exportConformanceOverviewReportInXML(communityId, systemId, domainId, groupId, specId, snapshotId) 
      } else {
        observable = this.reportService.exportOwnConformanceOverviewReportInXML(systemId, domainId, groupId, specId, snapshotId)
      }
      return observable.pipe(
          mergeMap((data) => {
            const blobData = new Blob([data], {type: "application/xml"})
            saveAs(blobData, "conformance_overview_report.xml")
            return of(true)
          })
        )
    } else {
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        // Get the overview certificate settings for the admin to verify before producing a certificate (if that choice is selected)
        const $reportSettings = this.reportService.loadReportSettings(communityId, Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_CERTIFICATE)
        const $certificateSettings = this.conformanceService.getConformanceOverviewCertificateSettingsWithApplicableMessage(communityId, reportLevel, identifier, snapshotId)
        return forkJoin([$reportSettings, $certificateSettings])
          .pipe(
            mergeMap((data) => {
              if (data[1]) {
                if ((reportLevel == 'all' && data[1].enableAllLevel) ||
                  (reportLevel == 'domain' && data[1].enableDomainLevel) ||
                  (reportLevel == 'group' && data[1].enableGroupLevel) ||
                  (reportLevel == 'specification' && data[1].enableSpecificationLevel)) {
                    let certificateSettingsToUse: ConformanceOverviewCertificateSettings|undefined = data[1]
                    if (data[0].customPdfs) {
                      // Don't present settings to override as we're using a custom generation service.
                      certificateSettingsToUse = undefined
                    }
                    // This can either be a report or a certificate
                    this.showConformanceOverviewModal(communityId, systemId, identifier, reportLevel, snapshotId, certificateSettingsToUse)
                    return of(true)
                } else {
                  // This can only be a report as the certificate settings don't foresee support for the requested reporting level
                  return this.reportService.exportConformanceOverviewReport(systemId, domainId, groupId, specId, snapshotId)
                    .pipe(mergeMap((data) => {
                      const blobData = new Blob([data], {type: "application/pdf"})
                      saveAs(blobData, "conformance_overview_report.pdf")
                      return of(true)
                    }))
                }
              } else {
                return of(true)
              }
            })
          )
      } else {
        const certificateEnabled = status == Constants.TEST_CASE_RESULT.SUCCESS && this.dataService.community?.allowCertificateDownload == true
        if (certificateEnabled) {
          return this.conformanceService.conformanceOverviewCertificateEnabled(communityId, reportLevel)
          .pipe(
            mergeMap((result) => {
              if (result.exists) {
                // This can either be a report or a certificate
                this.showConformanceOverviewModal(communityId, systemId, identifier, reportLevel, snapshotId, undefined)
                return of(true)
              } else {
                // This is a report
                return this.reportService.exportConformanceOverviewReport(systemId, domainId, groupId, specId, snapshotId)
                  .pipe(mergeMap((data) => {
                    const blobData = new Blob([data], {type: "application/pdf"})
                    saveAs(blobData, "conformance_overview_report.pdf")
                    return of(true)
                  }))              
              }
            })
          )
        } else {
          // This is a report
          return this.reportService.exportConformanceOverviewReport(systemId, domainId, groupId, specId, snapshotId)
            .pipe(mergeMap((data) => {
              const blobData = new Blob([data], {type: "application/pdf"})
              saveAs(blobData, "conformance_overview_report.pdf")
              return of(true)
            }))              
        }
      }
    }
  }

}
