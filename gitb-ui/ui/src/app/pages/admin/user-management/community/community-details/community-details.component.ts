import { AfterViewInit, Component, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
import { TabsetComponent } from 'ngx-bootstrap/tabs';
import { CommunityTab } from './community-tab.enum';
import { CommunityResource } from 'src/app/types/community-resource';
import { BsModalService } from 'ngx-bootstrap/modal';
import { CreateEditCommunityResourceModalComponent } from 'src/app/modals/create-edit-community-resource-modal/create-edit-community-resource-modal.component';
import { saveAs } from 'file-saver';
import { CommunityResourceBulkUploadModalComponent } from 'src/app/modals/community-resource-bulk-upload-modal/community-resource-bulk-upload-modal.component';

@Component({
  selector: 'app-community-details',
  templateUrl: './community-details.component.html',
  styleUrls: ['./community-details.component.less']
})
export class CommunityDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  community!: Community
  adminStatus = {status: Constants.STATUS.NONE}
  organisationStatus = {status: Constants.STATUS.NONE}
  landingPageStatus = {status: Constants.STATUS.NONE}
  errorTemplateStatus = {status: Constants.STATUS.NONE}
  legalNoticeStatus = {status: Constants.STATUS.NONE}
  triggerStatus = {status: Constants.STATUS.NONE}
  resourcesStatus = {status: Constants.STATUS.NONE}
  savePending = false
  deletePending = false
  communityId!: number
  originalDomainId?: number
  adminColumns: TableColumnDefinition[] = []
  organizationColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name', sortable: true, order: 'asc' },
    { field: 'fname', title: 'Full name', sortable: true }
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
    { field: 'statusText', title: 'Status', iconFn: this.dataService.iconForTestResult, iconTooltipFn: this.tooltipForTriggerResult }
  ]
  resourceColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'reference', title: 'Reference to use' },
    { field: 'description', title: 'Description' }
  ]
  domains: Domain[] = []
  admins: User[] = []
  organizations: Organisation[] = []
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []
  triggers: Trigger[] = []
  resources: CommunityResource[] = []
  testBedLegalNotice?: LegalNotice
  testBedLandingPage?: LandingPage
  testBedErrorTemplate?: ErrorTemplate
  tabToShow = CommunityTab.organisations
  tabTriggers!: Record<CommunityTab, {index: number, loader: () => any}>
  @ViewChild('tabs', { static: false }) tabs?: TabsetComponent;

  organisationFilter?: string
  resourceFilter?: string
  currentOrganisationsPage = 1
  currentResourcesPage = 1
  organisationCount = 0
  resourcesCount = 0
  organisationSortOrder = 'asc'
  organisationSortColumn = 'shortname'
  isNextPageOrganisationsDisabled = false
  isPreviousPageOrganisationsDisabled = false
  isNextPageResourcesDisabled = false
  isPreviousPageResourcesDisabled = false

  sortByCreationOrderNone = "none"
  sortByCreationOrderAsc = "asc"
  sortByCreationOrderDesc = "desc"
  sortByCreationOrderLabelNone = "Sort by creation order"
  sortByCreationOrderLabelAsc = "Earliest created first"
  sortByCreationOrderLabelDesc = "Latest created first"
  sortByCreationOrderLabel = this.sortByCreationOrderLabelNone
  sortByCreationOrder = this.sortByCreationOrderNone

  organisationsRefreshing = false
  resourcesRefreshing = false
  downloadAllResourcesPending = false
  deleteResourcesPending = false
  selectingForDeleteResources = false

  clearResourceSelections = new EventEmitter<void>()

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    router: Router,
    private userService: UserService,
    private landingPageService: LandingPageService,
    private legalNoticeService: LegalNoticeService,
    private errorTemplateService: ErrorTemplateService,
    private triggerService: TriggerService,
    private confirmationDialogService: ConfirmationDialogService,
    private organisationService: OrganisationService,
    private communityService: CommunityService,
    private conformanceService: ConformanceService,
    private modalService: BsModalService,
    private popupService: PopupService
  ) {
    super()
    // Access the tab to show via router state to have it cleared upon refresh.
    const tabParam = router.getCurrentNavigation()?.extras?.state?.tab
    if (tabParam != undefined) {
      this.tabToShow = CommunityTab[tabParam as keyof typeof CommunityTab]
    }
  }

  ngAfterViewInit(): void {
    this.dataService.focus('sname')
    setTimeout(() => {
      this.triggerTab(this.tabToShow)
    })
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
      this.organizationColumns.push({ field: 'templateName', title: 'Set as template', sortable: true })
    }
    if (this.dataService.isSystemAdmin) {
      this.conformanceService.getDomains()
      .subscribe((data) => {
        this.domains = data
      })
    }
    // Setup tab triggers
    this.setupTabs()
  }

  private setupTabs() {
    const temp: Partial<Record<CommunityTab, {index: number, loader: () => any}>> = {}
    temp[CommunityTab.organisations] = {index: 0, loader: () => {this.showOrganisations()}}
    let tabIndexOffset = 1
    if (this.communityId != Constants.DEFAULT_COMMUNITY_ID) {
      tabIndexOffset = 0
      temp[CommunityTab.administrators] = {index: 1, loader: () => {this.showAdministrators()}}
    }
    temp[CommunityTab.landingPages] = {index: 2-tabIndexOffset, loader: () => {this.showLandingPages()}}
    temp[CommunityTab.legalNotices] = {index: 3-tabIndexOffset, loader: () => {this.showLegalNotices()}}
    temp[CommunityTab.errorTemplates] = {index: 4-tabIndexOffset, loader: () => {this.showErrorTemplates()}}
    temp[CommunityTab.triggers] = {index: 5-tabIndexOffset, loader: () => {this.showTriggers()}}
    this.tabTriggers = temp as Record<CommunityTab, {index: number, loader: () => any}>
  }

  triggerTab(tab: CommunityTab) {
    this.tabTriggers[tab].loader()
    if (this.tabs) {
      this.tabs.tabs[this.tabTriggers[tab].index].active = true
    }
  }

  showOrganisations() {
    if (this.organisationStatus.status == Constants.STATUS.NONE) {
      this.goFirstPageOrganisations()
    }
  }

  private queryOrganisations() {
    if (this.organisationStatus.status == Constants.STATUS.FINISHED) {
      this.organisationsRefreshing = true
    } else {
      this.organisationStatus.status = Constants.STATUS.PENDING
    }
    this.organisationService.searchOrganisationsByCommunity(this.communityId, this.organisationFilter, this.organisationSortOrder, this.organisationSortColumn, this.currentOrganisationsPage, Constants.TABLE_PAGE_SIZE, this.sortByCreationOrder)
    .subscribe((data) => {
        this.organizations = data.data
        this.organisationCount = data.count!
        this.updateOrganisationPagination()
    })
    .add(() => {
      this.organisationsRefreshing = false
      this.organisationStatus.status = Constants.STATUS.FINISHED
    })
  }

  queryResources() {
    if (this.resourcesStatus.status == Constants.STATUS.FINISHED) {
      this.resourcesRefreshing = true
    } else {
      this.resourcesStatus.status = Constants.STATUS.PENDING
    }
    this.clearResourceSelections.emit()
    this.communityService.searchCommunityResources(this.communityId, this.resourceFilter, this.currentResourcesPage, Constants.TABLE_PAGE_SIZE)
    .subscribe((data) => {
      this.resources = data.data
      this.resourcesCount = data.count!
      this.updateResourcesPagination()
    }).add(() => {
      this.resourcesRefreshing = false
      this.resourcesStatus.status = Constants.STATUS.FINISHED
    })
  }

  showAdministrators() {
    if (this.adminStatus.status == Constants.STATUS.NONE) {
      this.adminStatus.status = Constants.STATUS.PENDING
      this.userService.getCommunityAdministrators(this.communityId)
      .subscribe((data) => {
        for (let admin of data) {
          admin.ssoStatusText = this.dataService.userStatus(admin.ssoStatus)
        }
        this.admins = data
      }).add(() => {
        this.adminStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  showLandingPages() {
    if (this.landingPageStatus.status == Constants.STATUS.NONE) {
      this.landingPageStatus.status = Constants.STATUS.PENDING
      this.landingPageService.getCommunityDefaultLandingPage(Constants.DEFAULT_COMMUNITY_ID)
      .subscribe((data) => {
        if (data.exists) this.testBedLandingPage = data
      })
      this.landingPageService.getLandingPagesByCommunity(this.communityId)
      .subscribe((data) => {
        this.landingPages = data
      }).add(() => {
        this.landingPageStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  showLegalNotices() {
    if (this.legalNoticeStatus.status == Constants.STATUS.NONE) {
      this.legalNoticeStatus.status = Constants.STATUS.PENDING
      this.legalNoticeService.getTestBedDefaultLegalNotice()
      .subscribe((data) => {
        if (data.exists) this.testBedLegalNotice = data
      })
      this.legalNoticeService.getLegalNoticesByCommunity(this.communityId)
      .subscribe((data) => {
        this.legalNotices = data
      }).add(() => {
        this.legalNoticeStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  showErrorTemplates() {
    if (this.errorTemplateStatus.status == Constants.STATUS.NONE) {
      this.errorTemplateStatus.status = Constants.STATUS.PENDING
      this.errorTemplateService.getCommunityDefaultErrorTemplate(Constants.DEFAULT_COMMUNITY_ID)
      .subscribe((data) => {
        if (data.exists) this.testBedErrorTemplate = data
      })
      this.errorTemplateService.getErrorTemplatesByCommunity(this.communityId)
      .subscribe((data) => {
        this.errorTemplates = data
      }).add(() => {
        this.errorTemplateStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  showTriggers() {
    if (this.triggerStatus.status == Constants.STATUS.NONE) {
      this.triggerStatus.status = Constants.STATUS.PENDING
      this.triggerService.getTriggersByCommunity(this.communityId)
      .subscribe((data) => {
        for (let trigger of data) {
          trigger.eventTypeLabel = this.dataService.triggerEventTypeLabel(trigger.eventType)
          if (trigger.latestResultOk != undefined) {
            if (trigger.latestResultOk) {
              trigger.statusText = Constants.TEST_CASE_RESULT.SUCCESS
            } else {
              trigger.statusText = Constants.TEST_CASE_RESULT.FAILURE
            }
          } else {
            trigger.statusText = Constants.TEST_CASE_RESULT.UNDEFINED
          }
        }
        this.triggers = data
      }).add(() => {
        this.triggerStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  applyResourceFilter() {
    this.goFirstPageResources()
  }

  showResources() {
    if (this.resourcesStatus.status == Constants.STATUS.NONE) {
      this.goFirstPageResources()
    }
  }

  private openResourceModal(resourceToEdit?: CommunityResource) {
    const modal = this.modalService.show(CreateEditCommunityResourceModalComponent, {
      class: 'modal-lg',
      initialState: {
        communityId: this.communityId,
        resource: resourceToEdit
      }
    })
    modal.content!.resourceUpdated.subscribe((updateMade) => {
      if (updateMade) {
        this.queryResources()
      }
    })
    modal.onHide!.subscribe(() => {
      this.clearResourceSelections.emit()
    })
  }

  uploadResource() {
    this.openResourceModal()
  } 

  uploadResourceBulk() {
    const modal = this.modalService.show(CommunityResourceBulkUploadModalComponent, {
      class: 'modal-lg',
      initialState: {
        communityId: this.communityId
      }
    })
    modal.content!.resourcesUpdated.subscribe((updateMade) => {
      if (updateMade) {
        this.queryResources()
      }
    })
  }

  downloadAllResources() {
    if (this.resources.length > 0) {
      this.downloadAllResourcesPending = true
      this.communityService.downloadCommunityResources(this.communityId, this.resourceFilter)
      .subscribe((data) => {
        const blobData = new Blob([data], {type: 'application/zip'})
        saveAs(blobData, 'resources.zip')
      }).add(() => {
        this.downloadAllResourcesPending = false
      })
    }
  }

  selectResource(resource: CommunityResource) {
    this.cancelDeleteResources()
    this.openResourceModal(resource)
  }

  copyResourceReference(resource: CommunityResource) {
    this.dataService.copyToClipboard(resource.reference).subscribe(() => {
      this.popupService.success('Reference copied to clipboard.')
    })
  }

  downloadResource(resource: CommunityResource) {
    resource.downloadPending = true
    this.communityService.downloadCommunityResourceById(resource.id)
    .subscribe((response) => {
      let fileName = "file"
      const contentDisposition = response.headers.get('Content-Disposition')
      if (contentDisposition != null) {
        fileName = contentDisposition.split(';')[1].trim().split('=')[1].replace(/"/g, '')
      }
      const bb = new Blob([response.body as ArrayBuffer])
      saveAs(bb, fileName)
    }).add(() => {
      resource.downloadPending = false
    })
  }

  selectDeleteResources() {
    this.selectingForDeleteResources = true
  }

  confirmDeleteResources() {
    const resourceIds: number[] = []
    for (let resource of this.resources) {
      if (resource.checked != undefined && resource.checked) {
        resourceIds.push(resource.id)
      }
    }
    let msg: string
    if (resourceIds.length == 1) {
      msg = 'Are you sure you want to delete the selected resource?'
    } else {
      msg = 'Are you sure you want to delete the selected resources?'
    }
    this.confirmationDialogService.confirmed("Confirm delete", msg, "Yes", "No").subscribe(() => {
      this.deleteResourcesPending = true
      this.resourcesRefreshing = true
      this.communityService.deleteCommunityResources(this.communityId, resourceIds).subscribe(() => {
        this.popupService.success("Resources deleted.")
      }).add(() => {
        this.deleteResourcesPending = false
        this.selectingForDeleteResources = false
        this.goFirstPageResources()
      })
    })
  }

  cancelDeleteResources() {
    this.clearResourceSelections.emit()
    this.selectingForDeleteResources = false
    for (let resource of this.resources) {
      if (resource.checked != undefined) {
        resource.checked = false
      }
    }
  }

  resourcesChecked() {
    for (let resource of this.resources) {
      if (resource.checked !== undefined && resource.checked) {
        return true
      }
    }
    return false
  }

  deleteResource(resource: CommunityResource) {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this resource?", "Yes", "No").subscribe(() => {
      resource.deletePending = true
      this.communityService.deleteCommunityResource(resource.id).subscribe(() => {
        this.popupService.success("Resource deleted.")
        this.queryResources()
      }).add(() => {
        resource.deletePending = false
      })
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
    this.communityService.updateCommunity(this.communityId, this.community.sname!, this.community.fname!, this.community.email, this.community.selfRegType!, this.community.selfRegRestriction!, this.community.selfRegToken, this.community.selfRegTokenHelpText, this.community.selfRegNotification, descriptionToUse, this.community.selfRegForceTemplateSelection, this.community.selfRegForceRequiredProperties, this.community.allowCertificateDownload!, this.community.allowStatementManagement!, this.community.allowSystemManagement!, this.community.allowPostTestOrganisationUpdates!, this.community.allowPostTestSystemUpdates!, this.community.allowPostTestStatementUpdates!, this.community.allowAutomationApi, this.community.domainId)
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

  organisationSelect(organization: Organisation) {
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

  goPreviousPageOrganisations() {
    this.currentOrganisationsPage -= 1
    this.queryOrganisations()
  }

  goPreviousPageResources() {
    this.currentResourcesPage -= 1
    this.queryResources()
  }

  goNextPageOrganisations() {
    this.currentOrganisationsPage += 1
    this.queryOrganisations()
  }

  goNextPageResources() {
    this.currentResourcesPage += 1
    this.queryResources()
  }

  goFirstPageOrganisations() {
    this.currentOrganisationsPage = 1
    this.queryOrganisations()
  }

  goFirstPageResources() {
    this.currentResourcesPage = 1
    this.queryResources()
  }

  goLastPageOrganisations() {
    this.currentOrganisationsPage = Math.ceil(this.organisationCount / Constants.TABLE_PAGE_SIZE)
    this.queryOrganisations()
  }

  goLastPageResources() {
    this.currentResourcesPage = Math.ceil(this.resourcesCount / Constants.TABLE_PAGE_SIZE)
    this.queryResources()
  }

  private updateOrganisationPagination() {
    if (this.currentOrganisationsPage == 1) {
      this.isNextPageOrganisationsDisabled = this.organisationCount <= Constants.TABLE_PAGE_SIZE
      this.isPreviousPageOrganisationsDisabled = true
    } else if (this.currentOrganisationsPage == Math.ceil(this.organisationCount / Constants.TABLE_PAGE_SIZE)) {
      this.isNextPageOrganisationsDisabled = true
      this.isPreviousPageOrganisationsDisabled = false
    } else {
      this.isNextPageOrganisationsDisabled = false
      this.isPreviousPageOrganisationsDisabled = false
    }
  }

  private updateResourcesPagination() {
    if (this.currentResourcesPage == 1) {
      this.isNextPageResourcesDisabled = this.resourcesCount <= Constants.TABLE_PAGE_SIZE
      this.isPreviousPageResourcesDisabled = true
    } else if (this.currentResourcesPage == Math.ceil(this.resourcesCount / Constants.TABLE_PAGE_SIZE)) {
      this.isNextPageResourcesDisabled = true
      this.isPreviousPageResourcesDisabled = false
    } else {
      this.isNextPageResourcesDisabled = false
      this.isPreviousPageResourcesDisabled = false
    }
  }

  sortOrganisations(column: TableColumnDefinition) {
    if (column.field == 'sname') {
      this.organisationSortColumn = 'shortname'
    } else if (column.field == 'fname') {
      this.organisationSortColumn = 'fullname'
    } else {
      this.organisationSortColumn = 'template'
    }
    this.organisationSortOrder = column.order!
    this.goFirstPageOrganisations()
  }

  applyOrganisationFilter() {
    this.goFirstPageOrganisations()
  }

  applyCreationOrderSort(type: string, label: string) {
    this.sortByCreationOrderLabel = label
    this.sortByCreationOrder = type
    if (type == "none") {
      for (let column of this.organizationColumns) {
        if (column.field == "sname") column.order = "asc"
        column.sortable = true
      }
    } else {
      for (let column of this.organizationColumns) {
        column.sortable = false
        column.order = undefined
      }
    }
    this.applyOrganisationFilter()
  }

  tooltipForTriggerResult(result?: string): string {
    let text: string
    if (result == Constants.TEST_CASE_RESULT.SUCCESS) {
      text = "Success"
    } else if (result == Constants.TEST_CASE_RESULT.FAILURE) {
      text = "Failure"
    } else {
      text = "Unknown"
    }
    return text
  }  
}
