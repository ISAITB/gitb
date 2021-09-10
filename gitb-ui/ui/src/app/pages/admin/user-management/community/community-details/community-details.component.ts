import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { CommunityService } from 'src/app/services/community.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorTemplateService } from 'src/app/services/error-template.service';
import { LandingPageService } from 'src/app/services/landing-page.service';
import { LegalNoticeService } from 'src/app/services/legal-notice.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { TriggerService } from 'src/app/services/trigger.service';
import { UserService } from 'src/app/services/user.service';
import { Community } from 'src/app/types/community';
import { Domain } from 'src/app/types/domain';
import { ErrorTemplate } from 'src/app/types/error-template';
import { LandingPage } from 'src/app/types/landing-page';
import { LegalNotice } from 'src/app/types/legal-notice';
import { Organisation } from 'src/app/types/organisation.type';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { Trigger } from 'src/app/types/trigger';
import { User } from 'src/app/types/user.type';

@Component({
  selector: 'app-community-details',
  templateUrl: './community-details.component.html',
  styles: [
  ]
})
export class CommunityDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  community!: Community
  adminStatus = {status: Constants.STATUS.PENDING}
  organisationStatus = {status: Constants.STATUS.PENDING}
  landingPageStatus = {status: Constants.STATUS.PENDING}
  errorTemplateStatus = {status: Constants.STATUS.PENDING}
  legalNoticeStatus = {status: Constants.STATUS.PENDING}
  triggerStatus = {status: Constants.STATUS.PENDING}
  savePending = false
  deletePending = false
  communityId!: number
  originalDomainId?: number
  adminColumns: TableColumnDefinition[] = []
  organizationColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' }
  ]
  landingPagesColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default' }
  ]
  legalNoticesColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default' }
  ]
  errorTemplatesColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default' }
  ]
  triggerColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'eventTypeLabel', title: 'Event type' },
    { field: 'active', title: 'Active' },
    { field: 'statusText', title: 'Status' }
  ]
  domains: Domain[] = []
  admins: User[] = []
  organizations: Organisation[] = []
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []
  triggers: Trigger[] = []
  triggerEventTypeMap: {[key: number]: string} = {}
  testBedLegalNotice?: LegalNotice
  testBedLandingPage?: LandingPage
  testBedErrorTemplate?: ErrorTemplate

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private userService: UserService,
    private landingPageService: LandingPageService,
    private legalNoticeService: LegalNoticeService,
    private errorTemplateService: ErrorTemplateService,
    private triggerService: TriggerService,
    private confirmationDialogService: ConfirmationDialogService,
    private organisationService: OrganisationService,
    private communityService: CommunityService,
    private conformanceService: ConformanceService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('sname')
  }

  ngOnInit(): void {
    this.community = this.route.snapshot.data['community']
    this.communityId = this.community.id
    this.community.domainId = this.community.domain?.id
    this.originalDomainId = this.community.domain?.id
    this.adminColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.adminColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.adminColumns.push({ field: 'email', title: 'Username' })
    }
    this.adminColumns.push({ field: 'ssoStatusText', title: 'Status' })
    if (this.dataService.configuration.registrationEnabled) {
      this.organizationColumns.push({ field: 'templateName', title: 'Set as template' })
    }
    this.triggerEventTypeMap = this.dataService.idToLabelMap(this.dataService.triggerEventTypes())
    this.legalNoticeService.getTestBedDefaultLegalNotice()
    .subscribe((data) => {
      if (data.exists) this.testBedLegalNotice = data
    })
    this.landingPageService.getCommunityDefaultLandingPage(Constants.DEFAULT_COMMUNITY_ID)
    .subscribe((data) => {
      if (data.exists) this.testBedLandingPage = data
    })
    this.errorTemplateService.getCommunityDefaultErrorTemplate(Constants.DEFAULT_COMMUNITY_ID)
    .subscribe((data) => {
      if (data.exists) this.testBedErrorTemplate = data
    })
    this.userService.getCommunityAdministrators(this.communityId)
    .subscribe((data) => {
      for (let admin of data) {
        admin.ssoStatusText = this.dataService.userStatus(admin.ssoStatus)
      }
      this.admins = data
    }).add(() => {
      this.adminStatus.status = Constants.STATUS.FINISHED
    })
    if (this.dataService.isSystemAdmin) {
      this.conformanceService.getDomains()
      .subscribe((data) => {
        this.domains = data
      })
    }
    this.landingPageService.getLandingPagesByCommunity(this.communityId)
    .subscribe((data) => {
      this.landingPages = data
    }).add(() => {
      this.landingPageStatus.status = Constants.STATUS.FINISHED
    })
    this.legalNoticeService.getLegalNoticesByCommunity(this.communityId)
    .subscribe((data) => {
      this.legalNotices = data
    }).add(() => {
      this.legalNoticeStatus.status = Constants.STATUS.FINISHED
    })
    this.errorTemplateService.getErrorTemplatesByCommunity(this.communityId)
    .subscribe((data) => {
      this.errorTemplates = data
    }).add(() => {
      this.errorTemplateStatus.status = Constants.STATUS.FINISHED
    })
    this.triggerService.getTriggersByCommunity(this.communityId)
    .subscribe((data) => {
      for (let trigger of data) {
        trigger.eventTypeLabel = this.triggerEventTypeMap[trigger.eventType]
        if (trigger.latestResultOk != undefined) {
          if (trigger.latestResultOk) {
            trigger.statusText = 'Success'
          } else {
            trigger.statusText = 'Error'
          }
        } else {
          trigger.statusText = '-'
        }
      }
      this.triggers = data
    }).add(() => {
      this.triggerStatus.status = Constants.STATUS.FINISHED    
    })
    this.organisationService.getOrganisationsByCommunity(this.communityId)
    .subscribe((data) => {
      this.organizations = data
    }).add(() => {
      this.organisationStatus.status = Constants.STATUS.FINISHED    
    })
  }
  
  saveDisabled() {
    return !(this.textProvided(this.community.sname) && this.textProvided(this.community.fname) &&
      (!this.dataService.configuration.registrationEnabled || 
        (this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED || 
          (
            (this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING || this.textProvided(this.community.selfRegToken)) && 
            (!this.dataService.configuration.emailEnabled || (!this.community.selfRegNotification || this.textProvided(this.community.email)))
          )
        )
      )
    )
  }

  private updateCommunityInternal(descriptionToUse?: string) {
    this.savePending = true
    this.communityService.updateCommunity(this.communityId, this.community.sname!, this.community.fname!, this.community.email, this.community.selfRegType!, this.community.selfRegRestriction!, this.community.selfRegToken, this.community.selfRegTokenHelpText, this.community.selfRegNotification, descriptionToUse, this.community.selfRegForceTemplateSelection, this.community.selfRegForceRequiredProperties, this.community.allowCertificateDownload!, this.community.allowStatementManagement!, this.community.allowSystemManagement!, this.community.allowPostTestOrganisationUpdates!, this.community.allowPostTestSystemUpdates!, this.community.allowPostTestStatementUpdates!, this.community.domainId)
    .subscribe(() => {
      this.originalDomainId = this.community.domainId
      this.popupService.success('Community updated.')
    }).add(() => {
      this.savePending = false
    })
  }

  updateCommunity() {
    this.clearAlerts()
    const emailValid = !this.textProvided(this.community.email) || this.requireValidEmail(this.community.email, "Please enter a valid support email.")
    const notificationValid = !this.community.selfRegNotification || this.requireText(this.community.email, "A support email needs to be defined to support notifications.")
    if (emailValid && notificationValid) {
      let descriptionToUse: string|undefined
      if (!this.community.sameDescriptionAsDomain) {
        descriptionToUse = this.community.activeDescription
      }
      if ((this.originalDomainId == undefined && this.community.domainId != undefined) || (this.originalDomainId != undefined && this.community.domainId != undefined && this.originalDomainId != this.community.domainId)) {
        let confirmationMessage: string
        if (this.originalDomainId == undefined) {
          confirmationMessage = "Setting the "+this.dataService.labelDomainLower()+" will remove existing conformance statements linked to other "+this.dataService.labelDomainsLower()+". Are you sure you want to proceed?"
        } else {
          confirmationMessage = "Changing the "+this.dataService.labelDomainLower()+" will remove all existing conformance statements. Are you sure you want to proceed?"
        }
        this.confirmationDialogService.confirmed("Confirm "+this.dataService.labelDomainLower()+" change", confirmationMessage, "Yes", "No")
        .subscribe(() => {
          this.updateCommunityInternal(descriptionToUse)
        })
      } else {
        this.updateCommunityInternal(descriptionToUse)
      }
    }
  }

  deleteCommunity() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this community?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.communityService.deleteCommunity(this.communityId)
      .subscribe(() => {
        this.cancelCommunityDetail()
        this.popupService.success('Community deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  organizationSelect(organization: Organisation) {
    this.routingService.toOrganisationDetails(this.communityId, organization.id)
  }

  isDefaultCommunity() {
    return this.communityId == Number(Constants.DEFAULT_COMMUNITY_ID)
  }

  createLandingPage(copyTestBedDefault: boolean) {
    this.routingService.toCreateLandingPage(this.communityId, copyTestBedDefault)
  }

  landingPageSelect(landingPage: LandingPage) {
    this.routingService.toLandingPage(this.communityId, landingPage.id)
  }

  createLegalNotice(copyTestBedDefault: boolean) {
    this.routingService.toCreateLegalNotice(this.communityId, copyTestBedDefault)
  }

  legalNoticeSelect(legalNotice: LegalNotice) {
    this.routingService.toLegalNotice(this.communityId, legalNotice.id)
  }

  createErrorTemplate(copyTestBedDefault: boolean) {
    this.routingService.toCreateErrorTemplate(this.communityId, copyTestBedDefault)
  }

  errorTemplateSelect(errorTemplate: ErrorTemplate) {
    this.routingService.toErrorTemplate(this.communityId, errorTemplate.id)
  }

  createTrigger() {
    this.routingService.toCreateTrigger(this.communityId)
  }

  triggerSelect(trigger: Trigger) {
    this.routingService.toTrigger(this.communityId, trigger.id)
  }

  adminSelect(admin: User) {
    this.routingService.toCommunityAdmin(this.communityId, admin.id!)
  }

  cancelCommunityDetail() {
    this.routingService.toUserManagement()
  }

  updateConformanceCertificateSettings() {
    this.routingService.toCommunityCertificateSettings(this.communityId)
  }

  updateParameters() {
    this.routingService.toCommunityParameters(this.communityId)
  }

  editLabels() {
    this.routingService.toCommunityLabels(this.communityId)
  }

  createAdmin() {
    this.routingService.toCreateCommunityAdmin(this.communityId)
  }

  createOrganisation() {
    this.routingService.toCreateOrganisation(this.communityId)
  }
}
