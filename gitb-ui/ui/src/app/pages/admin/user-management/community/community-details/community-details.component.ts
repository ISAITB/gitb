import {AfterViewInit, Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {CommunityService} from 'src/app/services/community.service';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {ErrorTemplateService} from 'src/app/services/error-template.service';
import {LandingPageService} from 'src/app/services/landing-page.service';
import {LegalNoticeService} from 'src/app/services/legal-notice.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {TriggerService} from 'src/app/services/trigger.service';
import {UserService} from 'src/app/services/user.service';
import {Community} from 'src/app/types/community';
import {Domain} from 'src/app/types/domain';
import {ErrorTemplate} from 'src/app/types/error-template';
import {LandingPage} from 'src/app/types/landing-page';
import {LegalNotice} from 'src/app/types/legal-notice';
import {Organisation} from 'src/app/types/organisation.type';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {Trigger} from 'src/app/types/trigger';
import {User} from 'src/app/types/user.type';
import {TabsetComponent} from 'ngx-bootstrap/tabs';
import {CommunityTab} from './community-tab.enum';
import {BreadcrumbType} from 'src/app/types/breadcrumb-type';
import {ValidationState} from 'src/app/types/validation-state';
import {Observable, of} from 'rxjs';
import {ResourceActions} from '../../../../../components/resource-management-tab/resource-actions';
import {FileData} from '../../../../../types/file-data.type';
import {CommunityResourceService} from '../../../../../services/community-resource.service';

@Component({
    selector: 'app-community-details',
    templateUrl: './community-details.component.html',
    styleUrls: ['./community-details.component.less'],
    standalone: false
})
export class CommunityDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  community!: Community
  adminStatus = {status: Constants.STATUS.NONE}
  organisationStatus = {status: Constants.STATUS.NONE}
  landingPageStatus = {status: Constants.STATUS.NONE}
  errorTemplateStatus = {status: Constants.STATUS.NONE}
  legalNoticeStatus = {status: Constants.STATUS.NONE}
  triggerStatus = {status: Constants.STATUS.NONE}
  loaded = false
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
  domains: Domain[] = []
  admins: User[] = []
  organizations: Organisation[] = []
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []
  triggers: Trigger[] = []
  testBedLegalNotice?: LegalNotice
  testBedLandingPage?: LandingPage
  testBedErrorTemplate?: ErrorTemplate
  tabToShow = CommunityTab.organisations
  tabTriggers!: Record<CommunityTab, {index: number, loader: () => any}>
  @ViewChild('tabs', { static: false }) tabs?: TabsetComponent;

  organisationFilter?: string
  currentOrganisationsPage = 1
  organisationCount = 0
  organisationSortOrder = 'asc'
  organisationSortColumn = 'shortname'
  isNextPageOrganisationsDisabled = false
  isPreviousPageOrganisationsDisabled = false

  sortByCreationOrderNone = "none"
  sortByCreationOrderAsc = "asc"
  sortByCreationOrderDesc = "desc"
  sortByCreationOrderLabelNone = "Sort by creation order"
  sortByCreationOrderLabelAsc = "Earliest created first"
  sortByCreationOrderLabelDesc = "Latest created first"
  sortByCreationOrderLabel = this.sortByCreationOrderLabelNone
  sortByCreationOrder = this.sortByCreationOrderNone

  organisationsRefreshing = false

  resourceActions!: ResourceActions
  resourceEmitter = new EventEmitter<void>()
  validation = new ValidationState()

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
    private communityResourceService: CommunityResourceService,
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
    setTimeout(() => {
      this.triggerTab(this.tabToShow)
    })
  }

  ngOnInit(): void {
    this.community = this.route.snapshot.data['community']
    this.communityId = this.community.id
    if (Number(this.communityId) == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toSystemAdministration()
    }
    this.community.domainId = this.community.domain?.id
    this.originalDomainId = this.community.domain?.id
    this.adminColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.adminColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.adminColumns.push({ field: 'email', title: 'Username' })
    }
    this.adminColumns.push({ field: 'ssoStatusText', title: 'Status', cellClass: 'td-nowrap' })
    if (this.dataService.configuration.registrationEnabled) {
      this.organizationColumns.push({ field: 'templateName', title: 'Set as template', sortable: true })
    }
    this.resourceActions = this.createCommunityResourceActions()
    this.routingService.communityBreadcrumbs(this.communityId, this.community.sname)
    // Setup tab triggers
    this.setupTabs()
    let domains$: Observable<Domain[]> = of([])
    if (this.dataService.isSystemAdmin) {
      domains$ = this.conformanceService.getDomains()
    }
    domains$.subscribe((data) => {
      this.domains = data
    }).add(() => {
      this.loaded = true
    })
  }

  private setupTabs() {
    const temp: Partial<Record<CommunityTab, {index: number, loader: () => any}>> = {}
    temp[CommunityTab.organisations] = {index: 0, loader: () => {this.showOrganisations()}}
    temp[CommunityTab.administrators] = {index: 1, loader: () => {this.showAdministrators()}}
    temp[CommunityTab.landingPages] = {index: 2, loader: () => {this.showLandingPages()}}
    temp[CommunityTab.legalNotices] = {index: 3, loader: () => {this.showLegalNotices()}}
    temp[CommunityTab.errorTemplates] = {index: 4, loader: () => {this.showErrorTemplates()}}
    temp[CommunityTab.triggers] = {index: 5, loader: () => {this.showTriggers()}}
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

  showResources() {
    this.resourceEmitter.emit()
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
      ) &&
      (!this.dataService.configuration.emailEnabled || (!this.community.interactionNotification || this.textProvided(this.community.email)))
    )
  }

  private updateCommunityInternal(descriptionToUse?: string) {
    this.savePending = true
    this.communityService.updateCommunity(this.communityId, this.community.sname!, this.community.fname!, this.community.email,
      this.community.selfRegType!, this.community.selfRegRestriction!, this.community.selfRegToken, this.community.selfRegTokenHelpText, this.community.selfRegNotification,
      this.community.interactionNotification, descriptionToUse, this.community.selfRegForceTemplateSelection, this.community.selfRegForceRequiredProperties,
      this.community.allowCertificateDownload!, this.community.allowStatementManagement!, this.community.allowSystemManagement!, this.community.allowPostTestOrganisationUpdates!,
      this.community.allowPostTestSystemUpdates!, this.community.allowPostTestStatementUpdates!, this.community.allowAutomationApi, this.community.allowCommunityView,
      this.community.domainId)
    .subscribe(() => {
      this.originalDomainId = this.community.domainId
      this.popupService.success('Community updated.')
      this.dataService.breadcrumbUpdate({id: this.communityId, type: BreadcrumbType.community, label: this.community.sname!})
    }).add(() => {
      this.savePending = false
    })
  }

  updateCommunity() {
    this.validation.clearErrors()
    const emailValid = !this.textProvided(this.community.email) || this.isValidEmail(this.community.email)
    if (!emailValid) {
      this.validation.invalid("supportEmail", "Please enter a valid support email.")
    }
    const notificationValid = !this.community.selfRegNotification || this.textProvided(this.community.email)
    if (!notificationValid) {
      this.validation.invalid("supportEmail", "A support email needs to be defined to support notifications.")
    }
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
        this.confirmationDialogService.confirmedDangerous("Confirm "+this.dataService.labelDomainLower()+" change", confirmationMessage, "Change", "Cancel")
        .subscribe(() => {
          this.updateCommunityInternal(descriptionToUse)
        })
      } else {
        this.updateCommunityInternal(descriptionToUse)
      }
    }
  }

  deleteCommunity() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this community?", "Delete", "Cancel")
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

  updateReportSettings() {
    this.routingService.toCommunityReportSettings(this.community.id)
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

  goNextPageOrganisations() {
    this.currentOrganisationsPage += 1
    this.queryOrganisations()
  }

  goFirstPageOrganisations() {
    this.currentOrganisationsPage = 1
    this.queryOrganisations()
  }

  goLastPageOrganisations() {
    this.currentOrganisationsPage = Math.ceil(this.organisationCount / Constants.TABLE_PAGE_SIZE)
    this.queryOrganisations()
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

  createCommunityResourceActions(): ResourceActions {
    return {
      searchResources: (filter: string|undefined, page: number|undefined, limit: number|undefined) => {
        return this.communityResourceService.searchCommunityResources(this.communityId, filter, page, limit)
      },
      downloadResources: (filter: string|undefined) => {
        return this.communityResourceService.downloadCommunityResources(this.communityId, filter)
      },
      downloadResource: (resourceId: number) => {
        return this.communityResourceService.downloadCommunityResourceById(resourceId)
      },
      deleteResources: (resourceIds: number[]) => {
        return this.communityResourceService.deleteCommunityResources(this.communityId, resourceIds)
      },
      deleteResource: (resourceId: number) => {
        return this.communityResourceService.deleteCommunityResource(resourceId)
      },
      createResource: (name: string, description: string|undefined, file: FileData) => {
        return this.communityResourceService.createCommunityResource(name, description, file, this.communityId)
      },
      updateResource: (resourceId: number, name: string, description: string|undefined, file?: FileData) => {
        return this.communityResourceService.updateCommunityResource(resourceId, name, description, file)
      },
      uploadBulk: (file: FileData, updateMatching?: boolean) => {
        return this.communityResourceService.uploadCommunityResourcesInBulk(this.communityId, file, updateMatching)
      },
      systemScope: false
    }
  }

}
