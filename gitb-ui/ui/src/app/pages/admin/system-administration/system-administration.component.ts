import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { TabsetComponent } from 'ngx-bootstrap/tabs';
import { SystemAdministrationTab } from './system-administration-tab.enum';
import { BaseComponent } from '../../base-component.component';
import { Router } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { UserService } from 'src/app/services/user.service';
import { DataService } from 'src/app/services/data.service';
import { User } from 'src/app/types/user.type';
import { RoutingService } from 'src/app/services/routing.service';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { LandingPage } from 'src/app/types/landing-page';
import { LegalNotice } from 'src/app/types/legal-notice';
import { ErrorTemplate } from 'src/app/types/error-template';
import { LandingPageService } from 'src/app/services/landing-page.service';
import { LegalNoticeService } from 'src/app/services/legal-notice.service';
import { ErrorTemplateService } from 'src/app/services/error-template.service';

@Component({
  selector: 'app-system-administration',
  templateUrl: './system-administration.component.html',
  styles: [
  ]
})
export class SystemAdministrationComponent extends BaseComponent implements OnInit, AfterViewInit {

  adminStatus = {status: Constants.STATUS.NONE}
  landingPageStatus = {status: Constants.STATUS.NONE}
  errorTemplateStatus = {status: Constants.STATUS.NONE}
  legalNoticeStatus = {status: Constants.STATUS.NONE}

  tabToShow = SystemAdministrationTab.administrators
  tabTriggers!: Record<SystemAdministrationTab, {index: number, loader: () => any}>
  @ViewChild('tabs', { static: false }) tabs?: TabsetComponent;

  adminColumns: TableColumnDefinition[] = []
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

  admins: User[] = []
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []

  constructor(
    router: Router,
    private userService: UserService,
    private dataService: DataService,
    private routingService: RoutingService,
    private landingPageService: LandingPageService,
    private legalNoticeService: LegalNoticeService,
    private errorTemplateService: ErrorTemplateService
  ) {
    super()
    // Access the tab to show via router state to have it cleared upon refresh.
    const tabParam = router.getCurrentNavigation()?.extras?.state?.tab
    if (tabParam != undefined) {
      this.tabToShow = SystemAdministrationTab[tabParam as keyof typeof SystemAdministrationTab]
    }    
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.triggerTab(this.tabToShow)
    })  }

  ngOnInit(): void {
    this.adminColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.adminColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.adminColumns.push({ field: 'email', title: 'Username' })
    }
    this.adminColumns.push({ field: 'ssoStatusText', title: 'Status' })
    // Setup tab triggers
    this.setupTabs()
  }

  triggerTab(tab: SystemAdministrationTab) {
    this.tabTriggers[tab].loader()
    if (this.tabs) {
      this.tabs.tabs[this.tabTriggers[tab].index].active = true
    }
  }

  private setupTabs() {
    const temp: Partial<Record<SystemAdministrationTab, {index: number, loader: () => any}>> = {}
    temp[SystemAdministrationTab.administrators] = {index: 0, loader: () => {this.showAdministrators()}}
    temp[SystemAdministrationTab.landingPages] = {index: 1, loader: () => {this.showLandingPages()}}
    temp[SystemAdministrationTab.legalNotices] = {index: 2, loader: () => {this.showLegalNotices()}}
    temp[SystemAdministrationTab.errorTemplates] = {index: 3, loader: () => {this.showErrorTemplates()}}
    this.tabTriggers = temp as Record<SystemAdministrationTab, {index: number, loader: () => any}>
  }

  showAdministrators() {
    if (this.adminStatus.status == Constants.STATUS.NONE) {
      this.adminStatus.status = Constants.STATUS.PENDING
      this.userService.getSystemAdministrators()
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

  createAdmin() {
    this.routingService.toCreateTestBedAdmin()
  }

  adminSelect(admin: User) {
    this.routingService.toTestBedAdmin(admin.id!)
  }

  showLandingPages() {
    if (this.landingPageStatus.status == Constants.STATUS.NONE) {
      this.landingPageStatus.status = Constants.STATUS.PENDING
      this.landingPageService.getLandingPagesByCommunity(Constants.DEFAULT_COMMUNITY_ID)
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
      this.legalNoticeService.getLegalNoticesByCommunity(Constants.DEFAULT_COMMUNITY_ID)
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
      this.errorTemplateService.getErrorTemplatesByCommunity(Constants.DEFAULT_COMMUNITY_ID)
      .subscribe((data) => {
        this.errorTemplates = data
      }).add(() => {
        this.errorTemplateStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  createLandingPage() {
    this.routingService.toCreateLandingPage()
  }

  landingPageSelect(landingPage: LandingPage) {
    this.routingService.toLandingPage(undefined, landingPage.id)
  }

  createLegalNotice() {
    this.routingService.toCreateLegalNotice()
  }

  legalNoticeSelect(legalNotice: LegalNotice) {
    this.routingService.toLegalNotice(undefined, legalNotice.id)
  }

  createErrorTemplate() {
    this.routingService.toCreateErrorTemplate()
  }

  errorTemplateSelect(errorTemplate: ErrorTemplate) {
    this.routingService.toErrorTemplate(undefined, errorTemplate.id)
  }

}
