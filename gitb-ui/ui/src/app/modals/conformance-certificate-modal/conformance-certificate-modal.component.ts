import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ReportService } from 'src/app/services/report.service';
import { ConformanceCertificateSettings } from 'src/app/types/conformance-certificate-settings';
import { ConformanceResultFull } from 'src/app/types/conformance-result-full';
import { saveAs } from 'file-saver'
import { BadgePlaceholderInfo } from './badge-placeholder-info';
import { Observable, forkJoin, of } from 'rxjs';
import { DomainParameter } from 'src/app/types/domain-parameter';
import { OrganisationParameterWithValue } from 'src/app/types/organisation-parameter-with-value';
import { OrganisationService } from 'src/app/services/organisation.service';
import { SystemParameterWithValue } from 'src/app/types/system-parameter-with-value';
import { SystemService } from 'src/app/services/system.service';

@Component({
  selector: 'app-conformance-certificate-modal',
  templateUrl: './conformance-certificate-modal.component.html'
})
export class ConformanceCertificateModalComponent implements OnInit {

  @Input() settings!: ConformanceCertificateSettings
  @Input() conformanceStatement!: ConformanceResultFull
  @Input() snapshotId?: number

  exportPending = false
  choice = Constants.REPORT_OPTION_CHOICE.REPORT
  Constants = Constants

  constructor(
    private dataService: DataService,
    private modalInstance: BsModalRef,
    private conformanceService: ConformanceService,
    private organisationService: OrganisationService,
    private systemService: SystemService,
    private reportService: ReportService
  ) { }

  ngOnInit(): void {
    if (this.settings.message != undefined) {
      // Replace the placeholders for the preview.
      let domainParamObservable: Observable<DomainParameter[]>
      if (this.settings.message.includes(Constants.PLACEHOLDER__DOMAIN+'{')) {
        domainParamObservable = this.conformanceService.getDomainParametersOfCommunity(this.settings.community, true, true)
      } else {
        domainParamObservable = of([])
      }
      let organisationParamObservable: Observable<OrganisationParameterWithValue[]>
      if (this.settings.message.includes(Constants.PLACEHOLDER__ORGANISATION+'{')) {
        organisationParamObservable = this.organisationService.getOrganisationParameterValues(this.conformanceStatement.organizationId, true)
      } else {
        organisationParamObservable = of([])
      }
      let systemParamObservable: Observable<SystemParameterWithValue[]>
      if (this.settings.message.includes(Constants.PLACEHOLDER__SYSTEM+'{')) {
        systemParamObservable = this.systemService.getSystemParameterValues(this.conformanceStatement.systemId, true)
      } else {
        systemParamObservable = of([])
      }
      forkJoin([domainParamObservable, organisationParamObservable, systemParamObservable]).subscribe((data) => {
        let message = this.settings.message!
        // Domain params
        if (data[0].length > 0) {
          for (let param of data[0]) {
            message = message.split(Constants.PLACEHOLDER__DOMAIN+'{'+param.name+'}').join((param.value == undefined)?'':param.value!)
          }
        }
        // Organisation properties
        if (data[1].length > 0) {
          for (let param of data[1]) {
            message = message.split(Constants.PLACEHOLDER__ORGANISATION+'{'+param.testKey+'}').join((param.value == undefined)?'':param.value!)
          }
        }
        // System properties
        if (data[2].length > 0) {
          for (let param of data[2]) {
            message = message.split(Constants.PLACEHOLDER__SYSTEM+'{'+param.testKey+'}').join((param.value == undefined)?'':param.value!)
          }
        }
        this.settings.message = message
      }).add(() => {
        // Replace the other placeholders
        this.settings.message = this.replacePlaceholders(this.settings.message!)
      })
    } else {
      this.settings.message = ''
    }
  }

  private replacePlaceholders(message: string) {
    message = message.split(Constants.PLACEHOLDER__DOMAIN).join(this.conformanceStatement.domainName)
    message = message.split(Constants.PLACEHOLDER__SPECIFICATION_GROUP_OPTION).join(this.conformanceStatement.specGroupOptionName)
    message = message.split(Constants.PLACEHOLDER__SPECIFICATION_GROUP).join(this.conformanceStatement.specGroupName?this.conformanceStatement.specGroupName:'')
    message = message.split(Constants.PLACEHOLDER__SPECIFICATION).join(this.conformanceStatement.specName)
    message = message.split(Constants.PLACEHOLDER__ACTOR).join(this.conformanceStatement.actorName)
    message = message.split(Constants.PLACEHOLDER__ORGANISATION).join(this.conformanceStatement.organizationName)
    message = message.split(Constants.PLACEHOLDER__SYSTEM).join(this.conformanceStatement.systemName)
    message = this.replaceBadgePlaceholders(message)
    return message
  }

  private replaceBadgePlaceholders(message: string): string {
    // Find placeholders.
    const placeholders: BadgePlaceholderInfo[] = []
    const matches = message.match(Constants.BADGE_PLACEHOLDER_REGEX)
    if (matches) {
      matches.forEach((match) => {
        const openBracket = match.indexOf("{")
        let width: number|undefined
        if (openBracket > 0) {
          const closeBracket = match.indexOf("}", openBracket)
          if (closeBracket > openBracket) {
            width = parseInt(match.substring(openBracket+1, closeBracket))
          }
        }
        placeholders.push({ placeholder: match, width: width})
      })
    }
    // Replace with images.
    for (let placeholder of placeholders) {
      let imagePath = this.conformanceService.conformanceBadgeByIdsPath(this.conformanceStatement.overallStatus!, this.conformanceStatement.systemId, this.conformanceStatement.specId, this.conformanceStatement.actorId, this.snapshotId)
      let imageDefinition: string
      if (placeholder.width) {
        imageDefinition = "<img width='"+placeholder.width+"' src='"+imagePath+"'/>"
      } else {
        imageDefinition = "<img src='"+imagePath+"'/>"
      }
      message = message.split(placeholder.placeholder).join(imageDefinition)
    }
    return message
  }

  certificateChoicesVisible() {
    if (this.settings.includeTitle) {
      this.dataService.focus('title')
    }
  }

  includeTitleChanged() {
    if (this.settings.includeTitle) {
      this.dataService.focus('title')
    }
  }

  generate() {
    this.exportPending = true
    if (this.choice == Constants.REPORT_OPTION_CHOICE.CERTIFICATE) {
        this.conformanceService.exportConformanceCertificateReport(this.conformanceStatement.communityId, this.conformanceStatement.actorId, this.conformanceStatement.systemId, this.settings, this.snapshotId)
        .subscribe((data) => {
          const blobData = new Blob([data], {type: 'application/pdf'});
          saveAs(blobData, "conformance_certificate.pdf");
          this.modalInstance.hide()
        }).add(() => {
          this.exportPending = false
        })
    } else {
        const includeDetails = this.choice == Constants.REPORT_OPTION_CHOICE.DETAILED_REPORT
        this.reportService.exportConformanceStatementReport(this.conformanceStatement.actorId, this.conformanceStatement.systemId, includeDetails, this.snapshotId)
        .subscribe((data) => {
          const blobData = new Blob([data], {type: 'application/pdf'});
          saveAs(blobData, "conformance_report.pdf");
          this.modalInstance.hide()
        }).add(() => {
          this.exportPending = false
        })
    }
  }

  cancel() {
    this.modalInstance.hide()
  }

}
