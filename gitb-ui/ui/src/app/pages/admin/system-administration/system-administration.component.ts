/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import {AfterViewInit, Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {TabsetComponent} from 'ngx-bootstrap/tabs';
import {SystemAdministrationTab} from './system-administration-tab.enum';
import {BaseComponent} from '../../base-component.component';
import {Router} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {UserService} from 'src/app/services/user.service';
import {DataService} from 'src/app/services/data.service';
import {User} from 'src/app/types/user.type';
import {RoutingService} from 'src/app/services/routing.service';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {LandingPage} from 'src/app/types/landing-page';
import {LegalNotice} from 'src/app/types/legal-notice';
import {ErrorTemplate} from 'src/app/types/error-template';
import {LandingPageService} from 'src/app/services/landing-page.service';
import {LegalNoticeService} from 'src/app/services/legal-notice.service';
import {ErrorTemplateService} from 'src/app/services/error-template.service';
import {PopupService} from 'src/app/services/popup.service';
import {SystemConfigurationService} from 'src/app/services/system-configuration.service';
import {find} from 'lodash';
import {Community} from 'src/app/types/community';
import {CommunityService} from 'src/app/services/community.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {Organisation} from 'src/app/types/organisation.type';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConfigStatus} from './config-status';
import {forkJoin, map, mergeMap, Observable, of, share, tap} from 'rxjs';
import {Theme} from 'src/app/types/theme';
import {EmailSettings} from 'src/app/types/email-settings';
import {CodeEditorModalComponent} from 'src/app/components/code-editor-modal/code-editor-modal.component';
import {BsModalService} from 'ngx-bootstrap/modal';
import {SystemConfiguration} from 'src/app/types/system-configuration';
import {ResourceActions} from '../../../components/resource-management-tab/resource-actions';
import {FileData} from '../../../types/file-data.type';
import {CommunityResourceService} from '../../../services/community-resource.service';
import {MultiSelectConfig} from '../../../components/multi-select-filter/multi-select-config';
import {UserBasic} from '../../../types/user-basic.type';
import {FilterUpdate} from '../../../components/test-filter/filter-update';
import {SslProtocol} from '../../../types/ssl-protocol';
import {MimeType} from '../../../types/mime-type';

@Component({
    selector: 'app-system-administration',
    templateUrl: './system-administration.component.html',
    styleUrls: ['./system-administration.component.less'],
    standalone: false
})
export class SystemAdministrationComponent extends BaseComponent implements OnInit, AfterViewInit {

  adminStatus = {status: Constants.STATUS.NONE}
  landingPageStatus = {status: Constants.STATUS.NONE}
  errorTemplateStatus = {status: Constants.STATUS.NONE}
  legalNoticeStatus = {status: Constants.STATUS.NONE}
  themeStatus = {status: Constants.STATUS.NONE}

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
  themeColumns: TableColumnDefinition[] = [
    { field: 'key', title: 'Key' },
    { field: 'description', title: 'Description' },
    { field: 'active', title: 'Active' }
  ]

  admins: User[] = []
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []
  themes: Theme[] = []

  configValuesPending = true
  configsCollapsed = false
  configsCollapsedFinished = false

  // Account retention period
  accountRetentionPeriodStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  accountRetentionPeriodEnabled = false
  accountRetentionPeriodValue?: number

  // TTL
  ttlStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  ttlEnabled = false
  ttlValue?: number

  // Self-registration
  selfRegistrationStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  selfRegistrationEnabled = false

  // REST API
  restApiStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  restApiEnabled = false
  restApiAdminKey!: string
  updateRestApiAdminKeyPending = false

  // Demo account
  demoAccountStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  demoAccountEnabled = false
  communities: Community[] = []
  organisations: Organisation[] = []
  users: UserBasic[] = []
  selectedCommunity?: Community
  selectedOrganisation?: Organisation
  demoAccount?: UserBasic
  loadCommunitiesPending = false
  loadOrganisationsPending = false
  loadUsersPending = false
  communitySelectConfig!: MultiSelectConfig<Community>
  organisationSelectConfig!: MultiSelectConfig<Organisation>
  userSelectConfig!: MultiSelectConfig<UserBasic>

  // Welcome page
  welcomePageStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  welcomePageResetPending = false
  welcomePageMessage?: string

  // Email settings
  emailSettingsStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  emailTestActive = false
  emailResetPending = false
  emailTestPending = false
  emailTestToAddress?: string
  emailSettings: EmailSettings = { enabled: false }
  emailSslProtocols: SslProtocol[] = [
    { id: 1, value: 'SSLv2Hello', label: 'SSL v2' },
    { id: 2, value: 'SSLv3', label: 'SSL v3' },
    { id: 3, value: 'TLSv1', label: 'TLS v1' },
    { id: 4, value: 'TLSv1.1', label: 'TLS v1.1' },
    { id: 5, value: 'TLSv1.2', label: 'TLS v1.2' },
    { id: 6, value: 'TLSv1.3', label: 'TLS v1.3' }
  ]
  emailAttachmentTypes: MimeType[] = [
    { id: 1, value: 'text/plain', label: 'Text (text/plain)' },
    { id: 2, value: 'image/gif', label: 'GIF (image/gif)' },
    { id: 3, value: 'image/png', label: 'PNG (image/png)' },
    { id: 4, value: 'image/jpeg', label: 'JPEG (image/jpeg)' },
    { id: 5, value: 'application/pdf', label: 'PDF (application/pdf)' },
    { id: 6, value: 'application/xml', label: 'XML (application/xml)' },
    { id: 7, value: 'text/xml', label: 'XML (text/xml)' }
  ]
  emailSslProtocolsSelectConfig!: MultiSelectConfig<SslProtocol>
  emailAttachmentTypeSelectConfig!: MultiSelectConfig<MimeType>

  // Resources
  resourceActions!: ResourceActions
  resourceEmitter = new EventEmitter<void>()

  constructor(
    router: Router,
    private userService: UserService,
    public dataService: DataService,
    private routingService: RoutingService,
    private landingPageService: LandingPageService,
    private legalNoticeService: LegalNoticeService,
    private errorTemplateService: ErrorTemplateService,
    private popupService: PopupService,
    private systemConfigurationService: SystemConfigurationService,
    private communityService: CommunityService,
    private communityResourceService: CommunityResourceService,
    private organisationService: OrganisationService,
    private confirmationDialogService: ConfirmationDialogService,
    private modalService: BsModalService
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
    this.adminColumns.push({ field: 'ssoStatusText', title: 'Status', cellClass: 'td-nowrap' })
    this.resourceActions = this.createResourceActions()
    this.loadCommunitiesPending = true
    const communityObs = this.communityService.getUserCommunities()
    .pipe(
      map((data) => {
        this.communities = data
      }),
      share()
    )
    // Load system configuration values.
    const configObs = this.systemConfigurationService.getConfigurationValues()
    .pipe(
      mergeMap((data) => {
        // Account retention period.
        const accountRetentionPeriodConfig = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.ACCOUNT_RETENTION_PERIOD)
        if (accountRetentionPeriodConfig && accountRetentionPeriodConfig.parameter != undefined) {
          this.accountRetentionPeriodEnabled = true
          this.accountRetentionPeriodValue = Number(accountRetentionPeriodConfig.parameter)
        } else {
          this.accountRetentionPeriodEnabled = false
          this.accountRetentionPeriodValue = undefined
        }
        this.accountRetentionPeriodStatus.enabled = this.accountRetentionPeriodEnabled
        this.accountRetentionPeriodStatus.fromEnv = accountRetentionPeriodConfig != undefined && accountRetentionPeriodConfig.environment
        this.accountRetentionPeriodStatus.fromDefault = accountRetentionPeriodConfig != undefined && accountRetentionPeriodConfig.default
        // TTL.
        const ttlConfig = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.SESSION_ALIVE_TIME)
        if (ttlConfig && ttlConfig.parameter != undefined) {
          this.ttlEnabled = true
          this.ttlValue = Number(ttlConfig.parameter)
        } else {
          this.ttlEnabled = false
          this.ttlValue = undefined
        }
        this.ttlStatus.enabled = this.ttlEnabled
        this.ttlStatus.fromEnv = ttlConfig != undefined && ttlConfig.environment
        this.ttlStatus.fromDefault = ttlConfig != undefined && ttlConfig.default
        // REST API.
        const restApiConfig = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.REST_API_ENABLED)
        this.restApiEnabled = restApiConfig != undefined && restApiConfig.parameter != undefined && restApiConfig.parameter.toLowerCase() == 'true'
        this.restApiStatus.enabled = this.restApiEnabled
        this.restApiStatus.fromEnv = restApiConfig != undefined && restApiConfig.environment
        this.restApiStatus.fromDefault = restApiConfig != undefined && restApiConfig.default
        this.restApiAdminKey = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.REST_API_ADMIN_KEY)!.parameter!
        // Self registration.
        const selfRegistrationConfig = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.SELF_REGISTRATION_ENABLED)
        this.selfRegistrationEnabled = selfRegistrationConfig != undefined && selfRegistrationConfig.parameter != undefined && selfRegistrationConfig.parameter.toLowerCase() == 'true'
        this.selfRegistrationStatus.enabled = this.selfRegistrationEnabled
        this.selfRegistrationStatus.fromEnv = selfRegistrationConfig != undefined && selfRegistrationConfig.environment
        this.selfRegistrationStatus.fromDefault = selfRegistrationConfig != undefined && selfRegistrationConfig.default
        // Welcome page message.
        const welcomeMessageConfig = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.WELCOME_MESSAGE)
        if (welcomeMessageConfig && welcomeMessageConfig.parameter) {
          this.welcomePageMessage = welcomeMessageConfig.parameter
        }
        this.welcomePageStatus.fromEnv = welcomeMessageConfig != undefined && welcomeMessageConfig.environment
        this.welcomePageStatus.fromDefault = welcomeMessageConfig != undefined && welcomeMessageConfig.default
        this.welcomePageStatus.enabled = !this.welcomePageStatus.fromDefault
        // Email settings.
        const emailSettingsConfig = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.EMAIL_SETTINGS)
        this.initialiseEmailSettings(emailSettingsConfig)
        this.emailSslProtocolsSelectConfig = {
          name: 'emailSslProtocols',
          textField: 'label',
          initialValues: this.parseSslProtocols(this.emailSettings.sslProtocols),
          showAsFormControl: true,
          filterLabel: 'Select protocols...',
          loader: () => of(this.emailSslProtocols)
        }
        this.emailAttachmentTypeSelectConfig = {
          name: 'emailAttachmentTypes',
          textField: 'label',
          initialValues: this.parseAttachmentTypes(this.emailSettings.allowedAttachmentTypes),
          showAsFormControl: true,
          filterLabel: 'Select attachment types...',
          loader: () => of(this.emailAttachmentTypes)
        }
        // Demo account.
        this.communitySelectConfig = {
          name: 'demoCommunity',
          textField: 'fname',
          singleSelection: true,
          singleSelectionPersistent: true,
          clearItems: new EventEmitter(),
          replaceItems: new EventEmitter(),
          replaceSelectedItems: new EventEmitter(),
          filterLabel: 'Select community...',
          noItemsMessage: 'No communities available.',
          searchPlaceholder: 'Search community...',
          loader: () => of(this.communities)
        }
        this.organisationSelectConfig = {
          name: 'demoOrganisation',
          textField: 'fname',
          singleSelection: true,
          singleSelectionPersistent: true,
          replaceItems: new EventEmitter(),
          replaceSelectedItems: new EventEmitter(),
          filterLabel: 'Select organisation...',
          noItemsMessage: 'No organisations available.',
          searchPlaceholder: 'Search organisation...',
          loader: () => of(this.organisations)
        }
        this.userSelectConfig = {
          name: 'demoUser',
          textField: 'email',
          singleSelection: true,
          singleSelectionPersistent: true,
          replaceItems: new EventEmitter(),
          replaceSelectedItems: new EventEmitter(),
          filterLabel: 'Select user...',
          noItemsMessage: 'No valid users available.',
          searchPlaceholder: 'Search users...',
          loader: () => of(this.users)
        }
        const demoAccountConfig = find(data, (configItem) => configItem.name == Constants.SYSTEM_CONFIG.DEMO_ACCOUNT)
        if (demoAccountConfig) {
          this.demoAccountEnabled = demoAccountConfig.parameter != undefined
          this.demoAccountStatus.enabled = this.demoAccountEnabled
          this.demoAccountStatus.fromEnv = demoAccountConfig.environment
          this.demoAccountStatus.fromDefault = demoAccountConfig.default
          if (this.demoAccountStatus.enabled) {
            return this.userService.getUserById(Number(demoAccountConfig.parameter))
            .pipe(
              map((user) => {
                if (user) {
                  this.demoAccount = {
                    id: user.id!,
                    email: user.email!,
                  }
                  this.selectedOrganisation = user.organization
                } else {
                  this.demoAccountEnabled = false
                  this.demoAccountStatus.enabled = this.demoAccountEnabled
                }
              }),
              share()
            )
          }
        }
        return of(1)
      }),
      share()
    )
    // Wait for everything to complete.
    forkJoin([communityObs, configObs]).subscribe(() => {
      if (this.demoAccount != undefined) {
        this.applyDemoCommunity()
      }
    }).add(() => {
      this.loadCommunitiesPending = false
      this.configValuesPending = false
    })
    // Setup tab triggers
    this.setupTabs()
    this.routingService.systemConfigurationBreadcrumbs()
  }

  private parseSslProtocols(values: string[]|undefined): SslProtocol[]|undefined {
    if (values != undefined && values.length > 0) {
      return this.emailSslProtocols.filter(protocol => {
        return values.find((value) => value == protocol.value)
      })
    } else {
      return undefined
    }
  }

  private parseAttachmentTypes(values: string[]|undefined): MimeType[]|undefined {
    if (values != undefined && values.length > 0) {
      return this.emailAttachmentTypes.filter(mimeType => {
        return values.find((value) => value == mimeType.value)
      })
    } else {
      return undefined
    }
  }

  selectEmailSslProtocols(event: FilterUpdate<SslProtocol>) {
    this.emailSettings.sslProtocols = event.values.active.map((value) => value.value)
  }

  selectEmailAttachmentTypes(event: FilterUpdate<MimeType>) {
    this.emailSettings.allowedAttachmentTypes = event.values.active.map((value) => value.value)
  }

  private initialiseEmailSettings(emailSettingsConfig: SystemConfiguration|undefined) {
    if (emailSettingsConfig?.parameter) {
      this.emailSettings = JSON.parse(emailSettingsConfig.parameter)
      this.emailSettingsStatus.enabled = this.emailSettings.enabled
      this.emailSettingsStatus.fromDefault = emailSettingsConfig.default
      this.emailSettingsStatus.fromEnv = emailSettingsConfig.environment
      if (this.emailSettings.to != undefined &&  this.emailSettings.to.length > 0) {
        this.emailSettings.defaultSupportMailbox = this.emailSettings.to[0]
      }
      this.emailSettings.newPassword = undefined
      this.emailSettings.updatePassword = this.emailSettings.password == undefined;
    }
  }

  applyDemoCommunity() {
    if (this.selectedOrganisation && this.communities && this.communities.length > 0) {
      this.selectedCommunity = find(this.communities, (community) => community.id == this.selectedOrganisation?.community)
      this.loadCommunityOrganisations().subscribe(() => {
        this.communitySelectConfig.eventsDisabled = true
        this.organisationSelectConfig.eventsDisabled = true
        this.userSelectConfig.eventsDisabled = true
        setTimeout(() => {
          this.communitySelectConfig.replaceSelectedItems?.emit([this.selectedCommunity!])
          this.organisationSelectConfig.replaceSelectedItems?.emit([this.selectedOrganisation!])
          this.userSelectConfig.replaceSelectedItems?.emit([this.demoAccount!])
          this.communitySelectConfig.eventsDisabled = false
          this.organisationSelectConfig.eventsDisabled = false
          this.userSelectConfig.eventsDisabled = false
        })
      })
    }
  }

  selectCommunity(event: FilterUpdate<Community>) {
    setTimeout(() => {
      this.selectedCommunity = event.values.active[0]
      this.selectedOrganisation = undefined
      this.demoAccount = undefined
      this.organisations = []
      this.loadCommunityOrganisations().subscribe(() => {
        if (this.selectedOrganisation) {
          this.organisationSelectConfig.eventsDisabled = true
          setTimeout(() => {
            this.organisationSelectConfig.replaceSelectedItems?.emit([this.selectedOrganisation!])
            this.organisationSelectConfig.eventsDisabled = false
            if (this.demoAccount) {
              this.userSelectConfig.eventsDisabled = true
              this.userSelectConfig.replaceSelectedItems?.emit([this.demoAccount])
              this.userSelectConfig.eventsDisabled = false
            }
          })
        }
      })
    })
  }

  selectCommunityOrganisation(event: FilterUpdate<Organisation>) {
    setTimeout(() => {
      this.selectedOrganisation = event.values.active[0]
      this.demoAccount = undefined
      this.users = []
      this.loadOrganisationUsers().subscribe(() => {
        if (this.demoAccount) {
          this.userSelectConfig.eventsDisabled = true
          setTimeout(() => {
            this.userSelectConfig.replaceSelectedItems?.emit([this.demoAccount!])
            this.userSelectConfig.eventsDisabled = false
          })
        }
      })
    })
  }

  private loadCommunityOrganisations(): Observable<boolean> {
    if (this.selectedCommunity) {
      this.loadOrganisationsPending = true
      this.loadUsersPending = true
      return this.organisationService.getOrganisationsByCommunity(this.selectedCommunity.id).pipe(
        mergeMap((data) => {
          this.organisations = data
          this.organisationSelectConfig.replaceItems!.emit(this.organisations)
          if (this.selectedOrganisation == undefined) {
            if (this.organisations.length == 1) {
              this.selectedOrganisation = this.organisations[0]
            }
          }
          if (this.selectedOrganisation) {
            return this.loadOrganisationUsers()
          } else {
            return of(true)
          }
        }),
        tap(() => {
          this.loadOrganisationsPending = false
        })
      )
    } else {
      return of(false)
    }
  }

  private loadOrganisationUsers(): Observable<boolean> {
    if (this.selectedOrganisation) {
      this.loadUsersPending = true
      return this.userService.getBasicUsersByOrganization(this.selectedOrganisation.id).pipe(
        map((data) => {
          this.users = data.map((user) => {
            return {
              id: user.id!,
              email: user.email!
            }
          })
          if (this.demoAccount == undefined) {
            if (this.users.length == 1) {
              this.demoAccount = this.users[0]
            }
          }
          return true
        }),
        tap(() => {
          this.loadUsersPending = false
        })
      )
    } else {
      return of(false)
    }
  }

  private triggerTab(tab: SystemAdministrationTab) {
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
    temp[SystemAdministrationTab.themes] = {index: 4, loader: () => {this.showThemes()}}
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

  showThemes() {
    if (this.themeStatus.status == Constants.STATUS.NONE) {
      this.themeStatus.status = Constants.STATUS.PENDING
      this.systemConfigurationService.getThemes()
      .subscribe((data) => {
        this.themes = data
      }).add(() => {
        this.themeStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  showResources() {
    this.resourceEmitter.emit()
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

  createTheme() {
    const activeTheme = find(this.themes, (theme) => theme.active)
    if (activeTheme) {
      this.routingService.toCreateTheme(activeTheme.id)
    }
  }

  themeSelect(theme: Theme) {
    this.routingService.toTheme(theme.id)
  }

  ttlCheckChanged() {
    if (!this.ttlEnabled) {
      this.ttlValue = undefined
    }
  }

  accountRetentionPeriodCheckChanged() {
    if (!this.accountRetentionPeriodEnabled) {
      this.accountRetentionPeriodValue = undefined
    }
  }

  saveTTL() {
    this.ttlStatus.pending = true
    if (this.ttlEnabled && this.ttlValue != undefined) {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SESSION_ALIVE_TIME, this.ttlValue.toString())
      .subscribe(() => {
        this.ttlStatus.collapsed = true
        this.ttlStatus.enabled = true
        this.popupService.success('Updated maximum session time.')
      }).add(() => {
        this.ttlStatus.pending = false
      })
    } else {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SESSION_ALIVE_TIME)
      .subscribe(() => {
        this.ttlStatus.collapsed = true
        this.ttlStatus.enabled = false
        this.popupService.success('Disabled automatic session termination.')
      }).add(() => {
        this.ttlStatus.pending = false
      })
    }
  }

  saveAccountRetentionPeriod() {
    if (this.accountRetentionPeriodEnabled && this.accountRetentionPeriodValue != undefined) {
      this.confirmationDialogService.confirmedDangerous("Delete inactive accounts", "Inactive user accounts based on the configured retention period will be immediately deleted. Are you sure you want to proceed?", "Enable retention period and delete accounts", "Cancel")
      .subscribe(() => {
        this.accountRetentionPeriodStatus.pending = true
        this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.ACCOUNT_RETENTION_PERIOD, this.accountRetentionPeriodValue!.toString())
        .subscribe(() => {
          this.accountRetentionPeriodStatus.collapsed = true
          this.accountRetentionPeriodStatus.enabled = true
          this.popupService.success('Updated inactive account retention period.')
        }).add(() => {
          this.accountRetentionPeriodStatus.pending = false
        })
      })
    } else {
      this.accountRetentionPeriodStatus.pending = true
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.ACCOUNT_RETENTION_PERIOD)
      .subscribe(() => {
        this.accountRetentionPeriodStatus.collapsed = true
        this.accountRetentionPeriodStatus.enabled = false
        this.popupService.success('Disabled inactive account retention period.')
      }).add(() => {
        this.accountRetentionPeriodStatus.pending = false
      })
    }
  }

  saveSelfRegistration() {
    this.selfRegistrationStatus.pending = true
    if (this.selfRegistrationEnabled) {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SELF_REGISTRATION_ENABLED, "true")
      .subscribe(() => {
        this.selfRegistrationStatus.collapsed = true
        this.selfRegistrationStatus.enabled = true
        this.dataService.configuration.registrationEnabled = true
        this.popupService.success('Enabled self-registration.')
      }).add(() => {
        this.selfRegistrationStatus.pending = false
      })
    } else {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SELF_REGISTRATION_ENABLED, "false")
      .subscribe(() => {
        this.selfRegistrationStatus.collapsed = true
        this.selfRegistrationStatus.enabled = false
        this.dataService.configuration.registrationEnabled = false
        this.popupService.success('Disabled self-registration.')
      }).add(() => {
        this.selfRegistrationStatus.pending = false
      })
    }
  }

  saveRestApi() {
    this.restApiStatus.pending = true
    if (this.restApiEnabled) {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.REST_API_ENABLED, "true")
      .subscribe(() => {
        this.restApiStatus.collapsed = true
        this.restApiStatus.enabled = true
        this.dataService.configuration.automationApiEnabled = true
        this.popupService.success('Enabled REST API.')
      }).add(() => {
        this.restApiStatus.pending = false
      })
    } else {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.REST_API_ENABLED, "false")
      .subscribe(() => {
        this.restApiStatus.collapsed = true
        this.restApiStatus.enabled = false
        this.dataService.configuration.automationApiEnabled = false
        this.popupService.success('Disabled REST API.')
      }).add(() => {
        this.restApiStatus.pending = false
      })
    }
  }

  updateRestApiAdminKey() {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the value for the administration API key?", "Update", "Cancel")
    .subscribe(() => {
      this.updateRestApiAdminKeyPending = true
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.REST_API_ADMIN_KEY)
      .subscribe((data) => {
        if (data?.parameter) {
          this.restApiAdminKey = data.parameter
          this.popupService.success("API key updated.")
        }
      }).add(() => {
        this.updateRestApiAdminKeyPending = false
      })
    })
  }

  saveDemoAccount() {
    this.demoAccountStatus.pending = true
    let disable = true
    if (this.demoAccountEnabled && this.demoAccount) {
      disable = false
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.DEMO_ACCOUNT, this.demoAccount.id!.toString())
      .subscribe(() => {
        this.demoAccountStatus.enabled = true
        this.demoAccountStatus.fromDefault = false
        this.demoAccountStatus.fromEnv = false
        this.demoAccountStatus.collapsed = true
        this.dataService.configuration.demosEnabled = true
        this.dataService.configuration.demosAccount = this.demoAccount!.id!
        this.popupService.success('Enabled demo account.')
      }).add(() => {
        this.demoAccountStatus.pending = false
      })
    }
    if (disable) {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.DEMO_ACCOUNT)
      .subscribe(() => {
        this.selectedCommunity = undefined
        this.selectedOrganisation = undefined
        this.demoAccount = undefined
        this.demoAccountStatus.enabled = false
        this.demoAccountStatus.fromDefault = false
        this.demoAccountStatus.fromEnv = false
        this.demoAccountStatus.collapsed = true
        this.dataService.configuration.demosEnabled = false
        this.dataService.configuration.demosAccount = -1
        this.popupService.success('Disabled demo account.')
        setTimeout(() => {
          this.communitySelectConfig.clearItems?.emit()
        })
      }).add(() => {
        this.demoAccountStatus.pending = false
      })
    }
  }

  saveWelcomePage() {
    if (this.welcomePageMessage) {
      this.welcomePageStatus.pending = true
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.WELCOME_MESSAGE, this.welcomePageMessage)
      .subscribe(() => {
        this.welcomePageStatus.collapsed = true
        this.welcomePageStatus.enabled = true
        this.welcomePageStatus.fromDefault = false
        this.welcomePageStatus.fromEnv = false
        this.popupService.success('Welcome page message set.')
      }).add(() => {
        this.welcomePageStatus.pending = false
      })
    }
  }

  resetWelcomePage() {
    this.confirmationDialogService.confirmedDangerous("Confirm reset", "Are you sure you want to reset the welcome page message to its default?", "Reset", "Cancel")
    .subscribe(() => {
      this.welcomePageResetPending = true
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.WELCOME_MESSAGE)
      .subscribe((appliedValue) => {
        if (appliedValue) {
          this.welcomePageMessage = appliedValue.parameter
          this.welcomePageStatus.fromDefault = appliedValue.default
          this.welcomePageStatus.fromEnv = appliedValue.environment
          this.welcomePageStatus.enabled = false
        }
        this.welcomePageStatus.collapsed = true
        this.popupService.success('Welcome page message reset to default.')
      }).add(() => {
        this.welcomePageResetPending = false
      })
    })
  }

  emailSettingsOk() {
    return !this.emailSettings.enabled ||
      (
        this.textProvided(this.emailSettings.host) &&
        this.numberProvided(this.emailSettings.port, 1) &&
        (!this.emailSettings.authenticate || (
          this.textProvided(this.emailSettings.username) &&
          ((!this.emailSettings.updatePassword && this.textProvided(this.emailSettings.password)) || (this.emailSettings.updatePassword && this.textProvided(this.emailSettings.newPassword)))
        )) &&
        this.textProvided(this.emailSettings.from) &&
        (!this.emailSettings.contactFormEnabled || (
          this.textProvided(this.emailSettings.defaultSupportMailbox) &&
          this.numberProvided(this.emailSettings.maxAttachmentCount, 0) &&
          this.numberProvided(this.emailSettings.maxAttachmentSize, 1)
        )) &&
        this.numberProvided(this.emailSettings.testInteractionReminder, 1)
      )
  }

  private prepareEmailSettings():EmailSettings {
    const emailSettingsToPost: Partial<EmailSettings> = {
      enabled: this.emailSettings.enabled
    }
    if (this.emailSettings.enabled) {
      emailSettingsToPost.host = this.emailSettings.host
      emailSettingsToPost.port = this.emailSettings.port
      emailSettingsToPost.from = this.emailSettings.from
      emailSettingsToPost.startTlsEnabled = this.emailSettings.startTlsEnabled
      emailSettingsToPost.sslEnabled = this.emailSettings.sslEnabled
      if (emailSettingsToPost.sslEnabled) {
        emailSettingsToPost.sslProtocols = this.emailSettings.sslProtocols
      }
      emailSettingsToPost.authenticate = this.emailSettings.authenticate
      if (this.emailSettings.authenticate) {
        emailSettingsToPost.username = this.emailSettings.username
        if (this.emailSettings.updatePassword) {
          emailSettingsToPost.password = this.emailSettings.newPassword
        }
      }
      if (this.emailSettings.defaultSupportMailbox != undefined) {
        emailSettingsToPost.to = [this.emailSettings.defaultSupportMailbox]
      }
      emailSettingsToPost.maxAttachmentCount = this.emailSettings.maxAttachmentCount
      emailSettingsToPost.maxAttachmentSize = this.emailSettings.maxAttachmentSize
      emailSettingsToPost.allowedAttachmentTypes = this.emailSettings.allowedAttachmentTypes
      emailSettingsToPost.testInteractionReminder = this.emailSettings.testInteractionReminder
      emailSettingsToPost.contactFormEnabled = this.emailSettings.contactFormEnabled
      emailSettingsToPost.contactFormCopyDefaultMailbox = this.emailSettings.contactFormCopyDefaultMailbox
    }
    return (emailSettingsToPost as EmailSettings)
  }

  private applyEmailSettingsToCurrentConfiguration() {
    this.dataService.configuration.emailEnabled = this.emailSettings.enabled
    this.dataService.configuration.emailContactFormEnabled = this.emailSettings.contactFormEnabled != undefined && this.emailSettings.contactFormEnabled
    this.dataService.configuration.emailAttachmentsMaxSize = this.emailSettings.maxAttachmentSize
    this.dataService.configuration.emailAttachmentsMaxCount = this.emailSettings.maxAttachmentCount
    if (this.emailSettings.allowedAttachmentTypes == undefined) {
      this.dataService.configuration.emailAttachmentsAllowedTypes = undefined
    } else {
      this.dataService.configuration.emailAttachmentsAllowedTypes = this.emailSettings.allowedAttachmentTypes.join(',')
    }
  }

  saveEmailSettings() {
    this.emailSettingsStatus.pending = true
    const emailSettingsToPost = this.prepareEmailSettings()
    this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.EMAIL_SETTINGS, JSON.stringify(emailSettingsToPost))
    .subscribe((appliedValue) => {
      this.initialiseEmailSettings(appliedValue)
      this.applyEmailSettingsToCurrentConfiguration()
      this.emailSettingsStatus.collapsed = true
      this.popupService.success('Updated email settings.')
    }).add(() => {
      this.emailSettingsStatus.pending = false
    })
  }

  resetEmailSettings() {
    this.emailResetPending = true
    this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.EMAIL_SETTINGS)
    .subscribe((appliedValue) => {
      this.initialiseEmailSettings(appliedValue)
      this.applyEmailSettingsToCurrentConfiguration()
      this.emailSettingsStatus.collapsed = true
      this.popupService.success('Email settings reset to default.')
    }).add(() => {
      this.emailResetPending = false
    })

  }

  testEmailSettings() {
    if (this.emailTestToAddress) {
      this.emailTestPending = true
      const emailSettingsToPost = this.prepareEmailSettings()
      this.systemConfigurationService.testEmailSettings(emailSettingsToPost, this.emailTestToAddress)
      .subscribe((result) => {
        if (result.success) {
          this.popupService.success("Test email sent successfully.")
        } else {
          let content = this.dataService.errorArrayToString(result.messages)
          this.modalService.show(CodeEditorModalComponent, {
            class: 'modal-lg',
            initialState: {
              documentName: 'Error message(s)',
              editorOptions: {
                value: content,
                readOnly: true,
                copy: true,
                lineNumbers: false,
                smartIndent: false,
                electricChars: false,
                styleClass: 'editor-short',
                mode: 'text/plain'
              }
            }
          })
        }
      }).add(() => {
        this.emailTestPending = false
      })
    }
  }

  createResourceActions(): ResourceActions {
    return {
      searchResources: (filter: string|undefined, page: number|undefined, limit: number|undefined) => {
        return this.communityResourceService.searchSystemResources(filter, page, limit)
      },
      downloadResources: (filter: string|undefined) => {
        return this.communityResourceService.downloadSystemResources(filter)
      },
      downloadResource: (resourceId: number) => {
        return this.communityResourceService.downloadSystemResourceById(resourceId)
      },
      deleteResources: (resourceIds: number[]) => {
        return this.communityResourceService.deleteSystemResources(resourceIds)
      },
      deleteResource: (resourceId: number) => {
        return this.communityResourceService.deleteSystemResource(resourceId)
      },
      createResource: (name: string, description: string|undefined, file: FileData) => {
        return this.communityResourceService.createSystemResource(name, description, file)
      },
      updateResource: (resourceId: number, name: string, description: string|undefined, file?: FileData) => {
        return this.communityResourceService.updateSystemResource(resourceId, name, description, file)
      },
      uploadBulk: (file: FileData, updateMatching?: boolean) => {
        return this.communityResourceService.uploadSystemResourcesInBulk(file, updateMatching)
      },
      systemScope: true
    }
  }

}
