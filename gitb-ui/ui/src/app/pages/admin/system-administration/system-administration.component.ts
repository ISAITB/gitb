/*
* Copyright (C) 2026 European Union
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

import {Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
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
import {Community} from 'src/app/types/community';
import {CommunityService} from 'src/app/services/community.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {Organisation} from 'src/app/types/organisation.type';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConfigStatus} from './config-status';
import {finalize, forkJoin, map, mergeMap, Observable, of, switchMap, tap} from 'rxjs';
import {Theme} from 'src/app/types/theme';
import {EmailSettings} from 'src/app/types/email-settings';
import {CodeEditorModalComponent} from 'src/app/components/code-editor-modal/code-editor-modal.component';
import {SystemConfiguration} from 'src/app/types/system-configuration';
import {ResourceActions} from '../../../components/resource-management-tab/resource-actions';
import {FileData} from '../../../types/file-data.type';
import {CommunityResourceService} from '../../../services/community-resource.service';
import {MultiSelectConfig} from '../../../components/multi-select-filter/multi-select-config';
import {UserBasic} from '../../../types/user-basic.type';
import {FilterUpdate} from '../../../components/test-filter/filter-update';
import {SslProtocol} from '../../../types/ssl-protocol';
import {MimeType} from '../../../types/mime-type';
import {NavigationTarget} from '../../../types/navigation-target';
import {BaseTabbedComponent} from '../../base-tabbed-component';
import {SoftwareVersionCheckSettings} from '../../../types/software-version-check-settings';
import {TestServiceCallbackSettings} from '../../../types/test-service-callback-settings';
import {ValidationState} from '../../../types/validation-state';
import {UsageTipsConfiguration} from '../../../types/usage-tips-configuration';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {TableApi} from '../../../components/table/table-api';
import {ResourceState} from '../../../components/resource-management-tab/resource-state';
import {SessionTimeoutConfiguration} from '../../../types/session-timeout-configuration';
import {RestApiRateLimits} from '../../../types/rest-api-rate-limits';
import {RestApiEndpointDescriptionWithId} from '../../../types/rest-api-endpoint-description-with-id';
import {RestApiEndpointLimit} from '../../../types/rest-api-endpoint-limit';
import {RestApiEndpointBasic} from '../../../types/rest-api-endpoint-basic';
import {ReportSettings} from '../../../types/report-settings';
import {REPORT_TYPE_INFOS} from '../../../types/report-type-info';
import {TimeZoneInfo} from '../../../types/time-zone-info';
import {WelcomeTexts} from '../../../types/welcome-texts';
import {ConfigurationValue} from '../../../types/configuration-value';
import {WELCOME_TEXT_INFOS} from '../../../types/welcome-text-info';
import {ConfigurationEntryComponentApi} from './configuration-entry/configuration-entry-component-api';

@Component({
    selector: 'app-system-administration',
    templateUrl: './system-administration.component.html',
    styleUrls: ['./system-administration.component.less'],
    standalone: false
})
export class SystemAdministrationComponent extends BaseTabbedComponent implements OnInit {

  @ViewChild("adminsTable") adminsTable?: TableApi
  @ViewChild("landingPagesTable") landingPagesTable?: TableApi
  @ViewChild("legalNoticesTable") legalNoticesTable?: TableApi
  @ViewChild("errorTemplatesTable") errorTemplatesTable?: TableApi
  @ViewChild("themesTable") themesTable?: TableApi
  @ViewChild("reportSettingsEntry") reportSettingsEntry?: ConfigurationEntryComponentApi
  @ViewChild("demoAccountEntry") demoAccountEntry?: ConfigurationEntryComponentApi
  @ViewChild("emailSettingsEntry") emailSettingsEntry?: ConfigurationEntryComponentApi
  @ViewChild("restApiEntry") restApiEntry?: ConfigurationEntryComponentApi

  adminStatus = {status: Constants.STATUS.NONE}
  landingPageStatus = {status: Constants.STATUS.NONE}
  errorTemplateStatus = {status: Constants.STATUS.NONE}
  legalNoticeStatus = {status: Constants.STATUS.NONE}
  themeStatus = {status: Constants.STATUS.NONE}

  adminColumns: TableColumnDefinition[] = []
  landingPagesColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default', headerClass: 'th-min centered', cellClass: 'td-min centered' }
  ]
  legalNoticesColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default', headerClass: 'th-min centered', cellClass: 'td-min centered' }
  ]
  errorTemplatesColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default', headerClass: 'th-min centered', cellClass: 'td-min centered' }
  ]
  themeColumns: TableColumnDefinition[] = [
    { field: 'key', title: 'Key' },
    { field: 'description', title: 'Description' },
    { field: 'active', title: 'Active', headerClass: 'th-min centered', cellClass: 'td-min centered' }
  ]

  admins: User[] = []
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []
  themes: Theme[] = []
  adminsRefreshing = false
  landingPagesRefreshing = false
  legalNoticesRefreshing = false
  errorTemplatesRefreshing = false
  themesRefreshing = false
  adminsPage = 1
  landingPagesPage = 1
  legalNoticesPage = 1
  errorTemplatesPage = 1
  themesPage = 1
  adminsTotal = 0
  landingPagesTotal = 0
  legalNoticesTotal = 0
  errorTemplatesTotal = 0
  themesTotal = 0
  resourceState: ResourceState = {
    resources: [],
    total: 0,
    page: 1,
    status: Constants.STATUS.NONE
  }

  configValuesPending = true
  configsCollapsed = false
  configsCollapsedFinished = false

  // Account retention period
  accountRetentionPeriodStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  accountRetentionPeriodEnabled = false
  accountRetentionPeriodValue?: number

  // Software version status check
  softwareVersionCheckStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  softwareVersionCheckSettings: SoftwareVersionCheckSettings = {
    enabled: false,
    jws: '',
    jwks: ''
  }
  softwareVersionCheckResetPending = false
  softwareVersionCheckValidation = new ValidationState()

  // Test service callbacks
  testServiceCallbacksStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  testServiceCallbacksSettings: TestServiceCallbackSettings = {
    enabled: true,
    soapEnabled: true,
    restEnabled: true,
    apiKeysEnabled: false
  }

  // TTL
  sessionTimeoutStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  sessionTimeoutSettings: SessionTimeoutConfiguration = {
    enabled: false,
    adminPendingTimeout: 3600,
    userPendingTimeout: 3600,
    otherTimeout: 3600
  }

  // Self-registration
  selfRegistrationStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  selfRegistrationEnabled = false

  // Startup wizard
  startupWizardStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  startupWizardEnabled = false

  // Usage tips
  usageTipsStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  usageTipsValue: UsageTipsConfiguration = {
    enabled: true,
    disabledForScreens: []
  }

  // REST API
  restApiStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false, deferredExpand: true }
  restApiLimits?: RestApiRateLimits
  restApiDataLoaded = false
  restApiEnabled = false
  restApiEndpointLimitsEnabled = false
  restApiAdminKey!: string
  updateRestApiAdminKeyPending = false
  restApiDevelopmentKey!: string
  updateRestApiDevelopmentKeyPending = false

  // Demo account
  demoAccountStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false, deferredExpand: true }
  demoAccountId?: number
  demoAccountEnabled = false
  demoAccountDataLoaded = false
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
  welcomePageMessage?: string
  welcomePageTexts: WelcomeTexts = {}
  welcomeTextInfos = WELCOME_TEXT_INFOS
  // Whether the welcome page form sections are shown and, on Save, whether the current values are
  // persisted (checked) or the persisted settings are deleted (unchecked). Initialised from whether
  // content is currently customised, but toggling it alone does not persist anything until saved.
  welcomePageOverride = false

  // Email settings
  emailSettingsStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false, deferredExpand: true }
  emailSettingsDataLoaded = false
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
  restApiEndpointSelectConfig!: MultiSelectConfig<RestApiEndpointDescriptionWithId>

  // Report settings
  reportSettingsStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false, deferredExpand: true}
  reportSettings: ReportSettings = { enabled: false, fileNameExpressions: {}, dateFormats: { date: 'dd/MM/yyyy', dateTime: 'dd/MM/yyyy HH:mm:ss', dateFile: 'yyyy-MM-dd' } }
  reportSettingsDataLoaded = false
  reportTypeInfos = REPORT_TYPE_INFOS
  timeZones?: TimeZoneInfo[]
  selectedTimeZone?: TimeZoneInfo
  timeZoneSelectConfig!: MultiSelectConfig<TimeZoneInfo>

  // Resources
  resourceActions!: ResourceActions
  prepareForShutdown = false

  constructor(
    route: ActivatedRoute,
    router: Router,
    private readonly userService: UserService,
    public readonly dataService: DataService,
    public readonly routingService: RoutingService,
    private readonly landingPageService: LandingPageService,
    private readonly legalNoticeService: LegalNoticeService,
    private readonly errorTemplateService: ErrorTemplateService,
    private readonly popupService: PopupService,
    private readonly systemConfigurationService: SystemConfigurationService,
    private readonly communityService: CommunityService,
    private readonly communityResourceService: CommunityResourceService,
    private readonly organisationService: OrganisationService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly modalService: NgbModal
  ) {
    super(router, route)
  }

  loadTab(tabIndex: number) {
    if (tabIndex == Constants.TAB.SYSTEM_ADMINISTRATION.ADMINISTRATORS) {
      this.showAdministrators()
    } else if (tabIndex == Constants.TAB.SYSTEM_ADMINISTRATION.LANDING_PAGES) {
      this.showLandingPages()
    } else if (tabIndex == Constants.TAB.SYSTEM_ADMINISTRATION.LEGAL_NOTICES) {
      this.showLegalNotices()
    } else if (tabIndex == Constants.TAB.SYSTEM_ADMINISTRATION.ERROR_TEMPLATES) {
      this.showErrorTemplates()
    } else if (tabIndex == Constants.TAB.SYSTEM_ADMINISTRATION.THEMES) {
      this.showThemes()
    } else {
      this.showResources()
    }
  }

  ngOnInit(): void {
    this.prepareForShutdown = this.dataService.configuration.preparingForShutdown
    this.adminColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.adminColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.adminColumns.push({ field: 'email', title: 'Username' })
    }
    this.adminColumns.push({ field: 'ssoStatusText', title: 'Status', headerClass: 'th-min centered', cellClass: 'td-min centered' })
    this.resourceActions = this.createResourceActions()
    // Load system configuration values.
    this.systemConfigurationService.getConfigurationValues().subscribe((data) => {
      const welcomeConfigs: SystemConfiguration[] = []
      data.forEach(configItem => {
        switch (configItem.name) {
          case Constants.SYSTEM_CONFIG.ACCOUNT_RETENTION_PERIOD:
            // Account retention period.
            if (configItem.parameter != undefined) {
              this.accountRetentionPeriodEnabled = true
              this.accountRetentionPeriodValue = Number(configItem.parameter)
            }
            this.accountRetentionPeriodStatus.enabled = this.accountRetentionPeriodEnabled
            this.accountRetentionPeriodStatus.fromEnv = configItem.environment
            this.accountRetentionPeriodStatus.fromDefault = configItem.default
            break
          case Constants.SYSTEM_CONFIG.SESSION_ALIVE_TIME:
            // TTL.
            if (configItem.parameter != undefined) {
              this.sessionTimeoutSettings = JSON.parse(configItem.parameter)
            }
            this.sessionTimeoutStatus.enabled = this.sessionTimeoutSettings.enabled
            this.sessionTimeoutStatus.fromEnv = configItem.environment
            this.sessionTimeoutStatus.fromDefault = configItem.default
            break
          case Constants.SYSTEM_CONFIG.SOFTWARE_VERSION_CHECK:
            // Software version status check.
            this.initialiseSoftwareVersionCheckSettings(configItem)
            break
          case Constants.SYSTEM_CONFIG.TEST_SERVICE_CALLBACKS:
            // Test service callbacks.
            this.initialiseTestServiceCallbacksSettings(configItem)
            break
          case Constants.SYSTEM_CONFIG.REST_API_ENABLED:
            // REST API.
            this.restApiEnabled = configItem.parameter != undefined && configItem.parameter.toLowerCase() == 'true'
            this.restApiStatus.enabled = this.restApiEnabled
            this.restApiStatus.fromEnv = configItem.environment
            this.restApiStatus.fromDefault = configItem.default
            break
          case Constants.SYSTEM_CONFIG.REST_API_ADMIN_KEY:
            // REST API admin key.
            this.restApiAdminKey = configItem.parameter!
            break
          case Constants.SYSTEM_CONFIG.REST_API_DEVELOPMENT_KEY:
            // REST API development key.
            this.restApiDevelopmentKey = configItem.parameter!
            break
          case Constants.SYSTEM_CONFIG.SELF_REGISTRATION_ENABLED:
            // Self registration.
            this.selfRegistrationEnabled = configItem.parameter != undefined && configItem.parameter.toLowerCase() == 'true'
            this.selfRegistrationStatus.enabled = this.selfRegistrationEnabled
            this.selfRegistrationStatus.fromEnv = configItem.environment
            this.selfRegistrationStatus.fromDefault = configItem.default
            break
          case Constants.SYSTEM_CONFIG.STARTUP_WIZARD:
            // Startup wizard.
            this.startupWizardEnabled = configItem.parameter != undefined && configItem.parameter.toLowerCase() == 'true'
            this.startupWizardStatus.enabled = this.startupWizardEnabled
            this.startupWizardStatus.fromEnv = configItem.environment
            this.startupWizardStatus.fromDefault = configItem.default
            break
          case Constants.SYSTEM_CONFIG.USAGE_TIPS:
            // Usage tips.
            if (configItem.parameter) {
              this.usageTipsValue = JSON.parse(configItem.parameter)
            }
            this.usageTipsStatus.enabled = this.usageTipsValue.enabled
            this.usageTipsStatus.fromEnv = configItem.environment
            this.usageTipsStatus.fromDefault = configItem.default
            break
          case Constants.SYSTEM_CONFIG.WELCOME_MESSAGE:
          case Constants.SYSTEM_CONFIG.WELCOME_TEXTS:
            // Welcome page message and texts (status is computed once both have been collected).
            welcomeConfigs.push(configItem)
            break
          case Constants.SYSTEM_CONFIG.EMAIL_SETTINGS:
            // Email settings.
            this.initialiseEmailSettings(configItem)
            break
          case Constants.SYSTEM_CONFIG.DEMO_ACCOUNT:
            // Demo account
            this.demoAccountEnabled = configItem.parameter != undefined
            this.demoAccountStatus.enabled = this.demoAccountEnabled
            this.demoAccountStatus.fromEnv = configItem.environment
            this.demoAccountStatus.fromDefault = configItem.default
            if (this.demoAccountStatus.enabled) {
              this.demoAccountId = Number(configItem.parameter)
            }
            break
          case Constants.SYSTEM_CONFIG.REST_API_RATE_LIMITS:
            // REST API rate limits
            if (configItem.parameter) {
              this.restApiLimits = JSON.parse(configItem.parameter)
              this.restApiEndpointLimitsEnabled = this.restApiLimits != undefined && this.restApiLimits.endpointLimits.length > 0
            }
            break
          case Constants.SYSTEM_CONFIG.REPORT_SETTINGS:
            // Report settings.
            this.initialiseReportSettings(configItem)
            break
          default:
            console.warn(`Unknown system configuration [${configItem.name}]`)
        }
      })
      this.applyWelcomePageConfigs(welcomeConfigs)
    }).add(() => {
      this.configValuesPending = false
    })
    this.routingService.systemConfigurationBreadcrumbs()
  }

  /**
   * Applies the welcome message and welcome texts configuration items, and derives the aggregate
   * status (fromDefault/fromEnv/enabled) for the "Custom welcome page content" section from both of
   * them together. Used both when configuration values are first loaded and after a reset to default.
   */
  private applyWelcomePageConfigs(configs: SystemConfiguration[]) {
    configs.forEach((configItem) => {
      if (configItem.parameter) {
        if (configItem.name == Constants.SYSTEM_CONFIG.WELCOME_MESSAGE) {
          this.welcomePageMessage = configItem.parameter
        } else if (configItem.name == Constants.SYSTEM_CONFIG.WELCOME_TEXTS) {
          this.welcomePageTexts = JSON.parse(configItem.parameter)
        }
      }
    })
    const complete = configs.length == 2
    this.welcomePageStatus.fromEnv = complete && configs.every((configItem) => configItem.environment)
    this.welcomePageStatus.fromDefault = complete && configs.every((configItem) => configItem.default)
    this.welcomePageStatus.enabled = !this.welcomePageStatus.fromDefault
    this.welcomePageOverride = this.welcomePageStatus.enabled
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

  private initialiseSoftwareVersionCheckSettings(settings: SystemConfiguration|undefined) {
    if (settings != undefined) {
      if (settings.parameter != undefined) {
        this.softwareVersionCheckSettings = JSON.parse(settings.parameter)
        this.softwareVersionCheckStatus.enabled = this.softwareVersionCheckSettings.enabled
      } else {
        this.softwareVersionCheckStatus.enabled = false
      }
      this.softwareVersionCheckStatus.fromDefault = settings.default
      this.softwareVersionCheckStatus.fromEnv = settings.environment
    }
  }

  private initialiseTestServiceCallbacksSettings(settings: SystemConfiguration|undefined) {
    if (settings != undefined) {
      if (settings.parameter != undefined) {
        this.testServiceCallbacksSettings = JSON.parse(settings.parameter)
        this.testServiceCallbacksStatus.enabled = this.testServiceCallbacksSettings.enabled
      } else {
        this.testServiceCallbacksStatus.enabled = false
      }
      this.testServiceCallbacksStatus.fromDefault = settings.default
      this.testServiceCallbacksStatus.fromEnv = settings.environment
    }
  }

  expandingReportSettings() {
    if (!this.reportSettingsDataLoaded) {
      this.timeZones = this.buildTimeZoneInfos()
      this.selectedTimeZone = this.timeZones.find((zone) => zone.zoneId == this.reportSettings.timeZone)
      this.timeZoneSelectConfig = {
        name: 'timeZone',
        textField: 'label',
        textDecorator: (item) => item.offset,
        singleSelection: true,
        singleSelectionPersistent: true,
        showAsFormControl: true,
        filterLabel: 'Select time zone...',
        searchPlaceholder: 'Search time zones...',
        noItemsMessage: 'No time zones available.',
        loader: () => of(this.timeZones!),
        // Separate the "UTC" entry (id 0), always listed first, from the rest of the zones.
        placeholderItemIds: new Set([0])
      }
      this.reportSettingsDataLoaded = true
    }
    this.reportSettingsEntry?.expand()
  }

  private initialiseReportSettings(settings: SystemConfiguration|undefined) {
    if (settings != undefined) {
      if (settings.parameter != undefined) {
        this.reportSettings = JSON.parse(settings.parameter)
        this.reportSettingsStatus.enabled = this.reportSettings.enabled
      } else {
        this.reportSettingsStatus.enabled = false
      }
      this.reportSettingsStatus.fromDefault = settings.default
      this.reportSettingsStatus.fromEnv = settings.environment
      // Guard against older persisted settings that predate the date format properties.
      if (this.reportSettings.dateFormats == undefined) {
        this.reportSettings.dateFormats = {}
      }
      this.reportSettings.dateFormats.date ??= 'dd/MM/yyyy'
      this.reportSettings.dateFormats.dateTime ??= 'dd/MM/yyyy HH:mm:ss'
      this.reportSettings.dateFormats.dateFile ??= 'yyyy-MM-dd'
    }
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

  showAdministrators() {
    if (this.adminStatus.status == Constants.STATUS.NONE) {
      this.queryAdministrators({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
    } else {
      this.updateAdminPagination(this.adminsPage, this.adminsTotal)
    }
  }

  showLandingPages() {
    if (this.landingPageStatus.status == Constants.STATUS.NONE) {
      this.queryLandingPages({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
    } else {
      this.updateLandingPagesPagination(this.landingPagesPage, this.landingPagesTotal)
    }
  }

  showLegalNotices() {
    if (this.legalNoticeStatus.status == Constants.STATUS.NONE) {
      this.queryLegalNotices({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
    } else {
      this.updateLegalNoticePagination(this.legalNoticesPage, this.legalNoticesTotal)
    }
  }

  showErrorTemplates() {
    if (this.errorTemplateStatus.status == Constants.STATUS.NONE) {
      this.queryErrorTemplates({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
    } else {
      this.updateErrorTemplatePagination(this.errorTemplatesPage, this.errorTemplatesTotal)
    }
  }

  showThemes() {
    if (this.themeStatus.status == Constants.STATUS.NONE) {
      this.queryThemes({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
    } else {
      this.updateThemePagination(this.themesPage, this.themesTotal)
    }
  }

  private queryAdministrators(pagingInfo: PagingEvent) {
    if (this.adminStatus.status == Constants.STATUS.FINISHED) {
      this.adminsRefreshing = true
    } else {
      this.adminStatus.status = Constants.STATUS.PENDING
    }
    this.userService.getSystemAdministrators(pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.admins = data.data
        for (let admin of this.admins) {
          admin.ssoStatusText = this.dataService.userStatus(admin.ssoStatus)
        }
        this.updateAdminPagination(pagingInfo.targetPage, data.count!)
      }).add(() => {
      this.adminsRefreshing = false
      this.adminStatus.status = Constants.STATUS.FINISHED
    })
  }

  private queryLandingPages(pagingInfo: PagingEvent) {
    if (this.landingPageStatus.status == Constants.STATUS.FINISHED) {
      this.landingPagesRefreshing = true
    } else {
      this.landingPageStatus.status = Constants.STATUS.PENDING
    }
    this.landingPageService.searchLandingPagesByCommunity(Constants.DEFAULT_COMMUNITY_ID, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.landingPages = data.data
        this.updateLandingPagesPagination(pagingInfo.targetPage, data.count!)
      }).add(() => {
      this.landingPagesRefreshing = false
      this.landingPageStatus.status = Constants.STATUS.FINISHED
    })
  }

  private queryLegalNotices(pagingInfo: PagingEvent) {
    if (this.legalNoticeStatus.status == Constants.STATUS.FINISHED) {
      this.legalNoticesRefreshing = true
    } else {
      this.legalNoticeStatus.status = Constants.STATUS.PENDING
    }
    this.legalNoticeService.searchLegalNoticesByCommunity(Constants.DEFAULT_COMMUNITY_ID, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.legalNotices = data.data
        this.updateLegalNoticePagination(pagingInfo.targetPage, data.count!)
      }).add(() => {
      this.legalNoticesRefreshing = false
      this.legalNoticeStatus.status = Constants.STATUS.FINISHED
    })
  }

  private queryErrorTemplates(pagingInfo: PagingEvent) {
    if (this.errorTemplateStatus.status == Constants.STATUS.FINISHED) {
      this.errorTemplatesRefreshing = true
    } else {
      this.errorTemplateStatus.status = Constants.STATUS.PENDING
    }
    this.errorTemplateService.searchErrorTemplatesByCommunity(Constants.DEFAULT_COMMUNITY_ID, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.errorTemplates = data.data
        this.updateErrorTemplatePagination(pagingInfo.targetPage, data.count!)
      }).add(() => {
      this.errorTemplatesRefreshing = false
      this.errorTemplateStatus.status = Constants.STATUS.FINISHED
    })
  }

  private queryThemes(pagingInfo: PagingEvent) {
    if (this.themeStatus.status == Constants.STATUS.FINISHED) {
      this.themesRefreshing = true
    } else {
      this.themeStatus.status = Constants.STATUS.PENDING
    }
    this.systemConfigurationService.getThemes(pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.themes = data.data
        this.updateThemePagination(pagingInfo.targetPage, data.count!)
      }).add(() => {
      this.themesRefreshing = false
      this.themeStatus.status = Constants.STATUS.FINISHED
    })
  }


  private updateAdminPagination(page: number, count: number) {
    this.adminsTable?.getPagingControls()?.updateStatus(page, count)
    this.adminsPage = page
    this.adminsTotal = count
  }

  private updateLandingPagesPagination(page: number, count: number) {
    this.landingPagesTable?.getPagingControls()?.updateStatus(page, count)
    this.landingPagesPage = page
    this.landingPagesTotal = count
  }

  private updateLegalNoticePagination(page: number, count: number) {
    this.legalNoticesTable?.getPagingControls()?.updateStatus(page, count)
    this.legalNoticesPage = page
    this.legalNoticesTotal = count
  }

  private updateErrorTemplatePagination(page: number, count: number) {
    this.errorTemplatesTable?.getPagingControls()?.updateStatus(page, count)
    this.errorTemplatesPage = page
    this.errorTemplatesTotal = count
  }

  private updateThemePagination(page: number, count: number) {
    this.themesTable?.getPagingControls()?.updateStatus(page, count)
    this.themesPage = page
    this.themesTotal = count
  }

  doAdminPaging(event: PagingEvent) {
    this.queryAdministrators(event)
    if (event.pageSizeChanged) {
      this.landingPageStatus.status = Constants.STATUS.NONE
      this.legalNoticeStatus.status = Constants.STATUS.NONE
      this.errorTemplateStatus.status = Constants.STATUS.NONE
      this.themeStatus.status = Constants.STATUS.NONE
      this.resourceState.status = Constants.STATUS.NONE
    }
  }

  doLandingPagePaging(event: PagingEvent) {
    this.queryLandingPages(event)
    if (event.pageSizeChanged) {
      this.adminStatus.status = Constants.STATUS.NONE
      this.legalNoticeStatus.status = Constants.STATUS.NONE
      this.errorTemplateStatus.status = Constants.STATUS.NONE
      this.themeStatus.status = Constants.STATUS.NONE
    }
  }

  doLegalNoticePaging(event: PagingEvent) {
    this.queryLegalNotices(event)
    if (event.pageSizeChanged) {
      this.adminStatus.status = Constants.STATUS.NONE
      this.landingPageStatus.status = Constants.STATUS.NONE
      this.errorTemplateStatus.status = Constants.STATUS.NONE
      this.themeStatus.status = Constants.STATUS.NONE
      this.resourceState.status = Constants.STATUS.NONE
    }
  }

  doErrorTemplatePaging(event: PagingEvent) {
    this.queryErrorTemplates(event)
    if (event.pageSizeChanged) {
      this.adminStatus.status = Constants.STATUS.NONE
      this.landingPageStatus.status = Constants.STATUS.NONE
      this.legalNoticeStatus.status = Constants.STATUS.NONE
      this.themeStatus.status = Constants.STATUS.NONE
      this.resourceState.status = Constants.STATUS.NONE
    }
  }

  doThemePaging(event: PagingEvent) {
    this.queryThemes(event)
    if (event.pageSizeChanged) {
      this.adminStatus.status = Constants.STATUS.NONE
      this.landingPageStatus.status = Constants.STATUS.NONE
      this.legalNoticeStatus.status = Constants.STATUS.NONE
      this.errorTemplateStatus.status = Constants.STATUS.NONE
      this.resourceState.status = Constants.STATUS.NONE
    }
  }

  resourceTabPageSizeChange() {
    this.adminStatus.status = Constants.STATUS.NONE
    this.landingPageStatus.status = Constants.STATUS.NONE
    this.legalNoticeStatus.status = Constants.STATUS.NONE
    this.errorTemplateStatus.status = Constants.STATUS.NONE
    this.themeStatus.status = Constants.STATUS.NONE
  }

  adminRowTarget = (admin: User): NavigationTarget => {
    return this.routingService.linkToTestBedAdmin(admin.id!)
  }

  showResources() {
    // No action needed.
  }

  landingPageRowTarget = (landingPage: LandingPage): NavigationTarget => {
    return this.routingService.linkToLandingPage(undefined, landingPage.id)
  }

  legalNoticeRowTarget = (legalNotice: LegalNotice): NavigationTarget => {
    return this.routingService.linkToLegalNotice(undefined, legalNotice.id)
  }

  errorTemplateRowTarget = (errorTemplate: ErrorTemplate): NavigationTarget => {
    return this.routingService.linkToErrorTemplate(undefined, errorTemplate.id)
  }

  /**
   * Bound directly as [navTarget], evaluated on every change detection cycle - returning undefined
   * (rather than silently ignoring the click, as the previous click handler did) leaves the link
   * without a destination, which is likewise inert on click, matching the previous behaviour.
   */
  createThemeTarget(): NavigationTarget|undefined {
    const activeTheme = this.themes.find((theme) => theme.active)
    if (activeTheme) {
      return this.routingService.linkToCreateTheme(activeTheme.id)
    }
    return undefined
  }

  themeRowTarget = (theme: Theme): NavigationTarget => {
    return this.routingService.linkToTheme(theme.id)
  }

  accountRetentionPeriodCheckChanged() {
    if (!this.accountRetentionPeriodEnabled) {
      this.accountRetentionPeriodValue = undefined
    }
  }

  saveSessionTimeoutSettings() {
    this.sessionTimeoutStatus.pending = true
    if (this.sessionTimeoutSettings.enabled) {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SESSION_ALIVE_TIME, JSON.stringify(this.sessionTimeoutSettings))
      .subscribe(() => {
        this.sessionTimeoutStatus.collapsed = true
        this.sessionTimeoutStatus.enabled = true
        this.popupService.success('Updated session timeout settings.')
      }).add(() => {
        this.sessionTimeoutStatus.pending = false
      })
    } else {
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SESSION_ALIVE_TIME)
      .subscribe(() => {
        this.sessionTimeoutStatus.collapsed = true
        this.sessionTimeoutStatus.enabled = false
        this.popupService.success('Disabled session timeout settings.')
      }).add(() => {
        this.sessionTimeoutStatus.pending = false
      })
    }
  }

  saveAccountRetentionPeriod() {
    if (this.accountRetentionPeriodEnabled && this.accountRetentionPeriodValue != undefined) {
      this.confirmationDialogService.confirmedDangerous("Delete inactive accounts", "Inactive user accounts based on the configured retention period will be immediately deleted. Are you sure you want to proceed?", "Enable retention period and delete accounts", "Cancel", Constants.BUTTON_ICON.DELETE)
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

  saveStartupWizardConfig() {
    this.startupWizardStatus.pending = true
    let valueToSave: string
    let message: string
    if (this.startupWizardEnabled) {
      valueToSave = "true"
      message = "Enabled startup configuration wizard."
    } else {
      valueToSave = "false"
      message = "Disabled startup configuration wizard."
    }
    this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.STARTUP_WIZARD, valueToSave).subscribe(() => {
      this.startupWizardStatus.collapsed = true
      this.startupWizardStatus.enabled = this.startupWizardEnabled
      this.startupWizardStatus.fromDefault = false
      this.startupWizardStatus.fromEnv = false
      this.dataService.configuration.startupWizardEnabled = this.startupWizardEnabled
      this.popupService.success(message)
    }).add(() => {
      this.startupWizardStatus.pending = false
    })
  }

  saveUsageTipsConfig() {
    this.usageTipsStatus.pending = true
    let message: string
    if (this.usageTipsValue?.enabled) {
      message = "Enabled usage tips."
    } else {
      message = "Disabled usage tips."
    }
    this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.USAGE_TIPS, JSON.stringify(this.usageTipsValue)).subscribe(() => {
      this.usageTipsStatus.collapsed = true
      this.usageTipsStatus.enabled = this.usageTipsValue?.enabled
      this.usageTipsStatus.fromDefault = false
      this.usageTipsStatus.fromEnv = false
      this.dataService.configuration.usageTipsEnabled = this.usageTipsValue?.enabled
      if (!this.dataService.configuration.usageTipsEnabled) {
        this.dataService.configuration.usageTipsDisabledForScreens = []
      }
      this.popupService.success(message)
    }).add(() => {
      this.usageTipsStatus.pending = false
    })
  }

  saveRestApi() {
    if (this.saveRestApiEnabled()) {
      this.restApiStatus.pending = true
      this.systemConfigurationService.updateConfigurationValues([
        { name: Constants.SYSTEM_CONFIG.REST_API_ENABLED, value: this.restApiEnabled.toString() },
        { name: Constants.SYSTEM_CONFIG.REST_API_RATE_LIMITS, value: this.restApiLimitsToSave() },
      ]).subscribe(() => {
        this.restApiStatus.collapsed = true
        this.restApiStatus.enabled = this.restApiEnabled
        this.dataService.configuration.automationApiEnabled = this.restApiEnabled
        this.popupService.success('Updated REST API settings.')
      }).add(() => {
        this.restApiStatus.pending = false
      })
    }
  }

  private restApiLimitsToSave(): string|undefined {
    if (this.restApiLimits) {
      this.restApiLimits.endpointLimits.forEach((endpointLimit) => {
        if (!Number.isInteger(endpointLimit.limit) || endpointLimit.limit < 0) {
          endpointLimit.limit = this.restApiLimits!.defaultEndpointLimit
        }
      })
      return JSON.stringify(this.restApiLimits)
    } else {
      return undefined
    }
  }

  saveRestApiEnabled() {
    return this.numberProvided(this.restApiLimits?.globalLimit, 0) && this.numberProvided(this.restApiLimits?.defaultEndpointLimit, 0)
  }

  updateRestApiAdminKey() {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the value for the administration API key?", "Update", "Cancel", Constants.BUTTON_ICON.RESET)
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

  updateRestApiDevelopmentKey() {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the value for the development API key?", "Update", "Cancel", Constants.BUTTON_ICON.RESET)
    .subscribe(() => {
      this.updateRestApiDevelopmentKeyPending = true
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.REST_API_DEVELOPMENT_KEY)
      .subscribe((data) => {
        if (data?.parameter) {
          this.restApiDevelopmentKey = data.parameter
          this.popupService.success("API key updated.")
        }
      }).add(() => {
        this.updateRestApiDevelopmentKeyPending = false
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

  welcomePageTextsOk(): boolean {
    return this.textProvided(this.welcomePageMessage) &&
      this.welcomeTextInfos.every((info) => this.textProvided(this.welcomePageTexts[info.key]))
  }

  /**
   * Saves the welcome page content. When "Override default content?" is checked this persists the
   * current message and texts; when unchecked this deletes the persisted settings (equivalent to a
   * reset to default), since there is otherwise no way to distinguish "not yet overridden" from
   * "explicitly reverted" once custom values have been set.
   */
  saveWelcomePage() {
    if (this.welcomePageOverride && !this.welcomePageTextsOk()) {
      return
    }
    this.welcomePageStatus.pending = true
    const values: ConfigurationValue[] = this.welcomePageOverride ? [
      { name: Constants.SYSTEM_CONFIG.WELCOME_MESSAGE, value: this.welcomePageMessage },
      { name: Constants.SYSTEM_CONFIG.WELCOME_TEXTS, value: JSON.stringify(this.welcomePageTexts) }
    ] : [
      { name: Constants.SYSTEM_CONFIG.WELCOME_MESSAGE },
      { name: Constants.SYSTEM_CONFIG.WELCOME_TEXTS }
    ]
    this.systemConfigurationService.updateConfigurationValues(values)
    .subscribe((appliedValues) => {
      if (this.welcomePageOverride) {
        this.welcomePageStatus.enabled = true
        this.welcomePageStatus.fromDefault = false
        this.welcomePageStatus.fromEnv = false
        this.popupService.success('Welcome page content set.')
      } else {
        if (appliedValues) {
          this.applyWelcomePageConfigs(appliedValues)
        }
        this.popupService.success('Welcome page content reset to default.')
      }
      this.welcomePageStatus.collapsed = true
    }).add(() => {
      this.welcomePageStatus.pending = false
    })
  }

  sessionTimeoutSettingsOk() {
    return !this.sessionTimeoutSettings.enabled || (this.sessionTimeoutSettings.otherTimeout != undefined && this.sessionTimeoutSettings.adminPendingTimeout != undefined && this.sessionTimeoutSettings.userPendingTimeout != undefined && this.sessionTimeoutSettings.otherTimeout >= 0 && this.sessionTimeoutSettings.adminPendingTimeout >= 0 && this.sessionTimeoutSettings.userPendingTimeout >= 0)
  }

  softwareVersionCheckSettingsOk() {
    return !this.softwareVersionCheckSettings.enabled || (this.textProvided(this.softwareVersionCheckSettings.jws) && this.textProvided(this.softwareVersionCheckSettings.jwks))
  }

  reportSettingsOk() {
    return !this.reportSettings.enabled || (
      this.reportTypeInfos.every((info) => this.textProvided(this.reportSettings.fileNameExpressions[info.type])) &&
      this.textProvided(this.reportSettings.dateFormats?.date) &&
      this.textProvided(this.reportSettings.dateFormats?.dateTime) &&
      this.textProvided(this.reportSettings.dateFormats?.dateFile)
    )
  }

  testServiceCallbacksSettingsOk() {
    return !this.testServiceCallbacksSettings.enabled || this.testServiceCallbacksSettings.soapEnabled || this.testServiceCallbacksSettings.restEnabled
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

  saveSoftwareVersionCheckSettings() {
    this.softwareVersionCheckValidation.clearErrors()
    let proceed = true
    if (this.softwareVersionCheckSettings.enabled) {
      if (!this.isValidAbsoluteHttpUrl(this.softwareVersionCheckSettings.jws)) {
        this.softwareVersionCheckValidation.apply("softwareCheckJws", "The value provided must be a valid absolute HTTP or HTTPS URL.")
        proceed = false
      }
      if (!this.isValidAbsoluteHttpUrl(this.softwareVersionCheckSettings.jwks)) {
        this.softwareVersionCheckValidation.apply("softwareCheckJwks", "The value provided must be a valid absolute HTTP or HTTPS URL.")
        proceed = false
      }
    }
    if (proceed) {
      this.softwareVersionCheckStatus.pending = true
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SOFTWARE_VERSION_CHECK, JSON.stringify(this.softwareVersionCheckSettings))
        .subscribe((appliedValue) => {
          this.initialiseSoftwareVersionCheckSettings(appliedValue)
          this.softwareVersionCheckStatus.collapsed = true
          this.popupService.success('Updated software check settings.')
        }).add(() => {
        this.softwareVersionCheckStatus.pending = false
      })
    }
  }

  resetSoftwareVersionCheckSettings() {
    this.softwareVersionCheckResetPending = true
    this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.SOFTWARE_VERSION_CHECK)
      .subscribe((appliedValue) => {
        this.initialiseSoftwareVersionCheckSettings(appliedValue)
        this.softwareVersionCheckStatus.collapsed = true
        this.popupService.success('Software check settings reset to default.')
      }).add(() => {
      this.softwareVersionCheckResetPending = false
    })
  }

  saveReportSettings() {
    if (this.reportSettingsOk()) {
      this.reportSettingsStatus.pending = true
      this.reportSettings.timeZone = this.selectedTimeZone?.zoneId
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.REPORT_SETTINGS, JSON.stringify(this.reportSettings))
        .subscribe((appliedValue) => {
          this.initialiseReportSettings(appliedValue)
          this.reportSettingsStatus.collapsed = true
          this.popupService.success('Updated report and display settings.')
          // Apply the date formats immediately for the Test Bed administrator making the change, rather than
          // waiting for the next initial connection (see DataService.emptyAppConfiguration/setConfiguration).
          if (this.reportSettings.dateFormats?.date != undefined && this.reportSettings.dateFormats?.dateTime != undefined) {
            this.dataService.configuration.dateFormat = this.reportSettings.dateFormats.date
            this.dataService.configuration.dateTimeFormat = this.reportSettings.dateFormats.dateTime
          }
        }).add(() => {
          this.reportSettingsStatus.pending = false
        })
    }
  }

  saveTestServiceCallbacks() {
    if (this.testServiceCallbacksSettingsOk()) {
      this.testServiceCallbacksStatus.pending = true
      this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.TEST_SERVICE_CALLBACKS, JSON.stringify(this.testServiceCallbacksSettings))
        .subscribe((appliedValue) => {
          this.initialiseTestServiceCallbacksSettings(appliedValue)
          this.testServiceCallbacksStatus.collapsed = true
          this.dataService.configuration.testServiceCallbacksApiKeysEnabled = this.testServiceCallbacksSettings.apiKeysEnabled
          this.popupService.success('Updated test engine callback settings.')
        }).add(() => {
        this.testServiceCallbacksStatus.pending = false
      })
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
          const modal = this.modalService.open(CodeEditorModalComponent, { size: 'lg' })
          const modalInstance = modal.componentInstance as CodeEditorModalComponent
          modalInstance.documentName = 'Error message(s)'
          modalInstance.editorOptions = {
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

  togglePrepareForShutdown() {
    const flagValue = this.prepareForShutdown
    this.systemConfigurationService.prepareForShutdown(flagValue).subscribe(() => {
      this.dataService.togglePrepareForShutdown(flagValue)
      setTimeout(() => {
        // Show with delay to allow the persistent warning notification to be shown or hidden.
        this.popupService.success(`Shutdown preparation mode ${flagValue?'enabled':'disabled'}.`)
      }, 500)
    })
  }

  expandingDemoAccount() {
    if (this.demoAccountDataLoaded) {
      this.demoAccountEntry?.expand()
    } else {
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
        filterLabelIcon: Constants.BUTTON_ICON.COMMUNITY,
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
        filterLabelIcon: Constants.BUTTON_ICON.ORGANISATION,
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
        filterLabelIcon: Constants.BUTTON_ICON.USER,
        noItemsMessage: 'No valid users available.',
        searchPlaceholder: 'Search users...',
        loader: () => of(this.users)
      }
      let user$: Observable<User | undefined | null>
      if (this.demoAccountStatus.enabled && this.demoAccountId != undefined) {
        user$ = this.userService.getUserById(this.demoAccountId).pipe(
          tap((user) => {
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
          })
        )
      } else {
        user$ = of(null)
      }
      this.loadCommunitiesPending = true
      forkJoin([user$, this.communityService.getUserCommunities()]).pipe(
        switchMap(data => {
          // User
          if (data[0]) {
            this.demoAccount = {
              id: data[0].id!,
              email: data[0].email!,
            }
            this.selectedOrganisation = data[0].organization
          } else {
            this.demoAccountEnabled = false
            this.demoAccountStatus.enabled = this.demoAccountEnabled
          }
          // Communities
          this.communities = data[1]
          this.loadCommunitiesPending = false
          // Apply
          if (this.selectedOrganisation && this.communities && this.communities.length > 0) {
            this.selectedCommunity = this.communities.find((community) => community.id == this.selectedOrganisation?.community)
            return this.loadCommunityOrganisations().pipe(
              tap(() => {
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
            )
          } else {
            return of(void 1)
          }
        }),
        finalize(() => {
          this.demoAccountDataLoaded = true
          this.demoAccountEntry?.expand()
        })
      ).subscribe(() => {})
    }
  }

  expandingEmailSettings() {
    if (!this.emailSettingsDataLoaded) {
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
      this.emailSettingsDataLoaded = true
    }
    this.emailSettingsEntry?.expand()
  }

  restApiEndpointDescriptionsMap?: Map<string, string|undefined>
  restApiEndpoints?: RestApiEndpointDescriptionWithId[]

  expandingRestApi() {
    if (this.restApiDataLoaded) {
      this.restApiEntry?.expand()
    } else {
      this.restApiEndpointSelectConfig = {
        name: 'restApiEndpoints',
        textField: 'path',
        textDecorator: (item) => item.method.toUpperCase(),
        filterLabel: 'Add limit for specific operation...',
        filterLabelIcon: Constants.BUTTON_ICON.NEW,
        squashItemsWithSameText: false,
        noItemsMessage: 'All operations have been configured',
        replaceItems: new EventEmitter<RestApiEndpointDescriptionWithId[]>(),
        replaceSelectedItems: new EventEmitter<RestApiEndpointDescriptionWithId[]>(),
        searchPlaceholder: 'Search operations...',
        enableSelectAll: true,
        loader: () => this.getAvailableRestApiEndpoints()
      }
      this.systemConfigurationService.getRestApiEndpointsFromDocumentation().pipe(
        tap(data => {
          let counter = 1
          this.restApiEndpoints = this.sortRestApiEndpoints(data.map(x => ({...x, id: counter++})))
          this.restApiEndpointDescriptionsMap = new Map<string, string>()
          this.restApiEndpoints.forEach(item => {
            this.restApiEndpointDescriptionsMap!.set(this.restApiEndpointKey(item), item.description)
          })
        }),
        finalize(() => {
          this.restApiDataLoaded = true
          this.restApiEntry?.expand()
        })
      ).subscribe(() => {})
    }
  }

  restApiEndpointDescription(item: RestApiEndpointBasic): string {
    return this.restApiEndpointDescriptionsMap!.get(this.restApiEndpointKey(item))??''
  }

  private restApiEndpointKey(endpoint: RestApiEndpointBasic): string {
    return `${endpoint.path}|${endpoint.method}`
  }

  private sortRestApiEndpoints<T extends RestApiEndpointBasic>(items: T[]) {
    items.sort((a, b) => a.path.localeCompare(b.path) || a.method.localeCompare(b.method));
    return items
  }

  private getAvailableRestApiEndpoints(): Observable<RestApiEndpointDescriptionWithId[]> {
    const selectedEndpoints = new Set<string>()
    this.restApiLimits!.endpointLimits.forEach(item => selectedEndpoints.add(this.restApiEndpointKey(item)))
    return of(this.restApiEndpoints!.filter(item => !selectedEndpoints.has(this.restApiEndpointKey(item))))
  }

  removeRestApiEndpoint(item: RestApiEndpointLimit) {
    const keyToRemove = this.restApiEndpointKey(item)
    this.restApiLimits!.endpointLimits = this.restApiLimits!.endpointLimits.filter(item => this.restApiEndpointKey(item) != keyToRemove)
  }

  selectedRestApiEndpoint(event: FilterUpdate<RestApiEndpointDescriptionWithId>) {
    setTimeout(() => {
      const newEndpoints: RestApiEndpointLimit[] = []
      event.values.active.forEach((value) => {
        newEndpoints.push({
          path: value.path,
          method: value.method,
          limit: this.restApiLimits!.defaultEndpointLimit
        })
      })
      newEndpoints.push(...this.restApiLimits!.endpointLimits)
      this.restApiLimits!.endpointLimits = this.sortRestApiEndpoints(newEndpoints)
      this.restApiEndpointSelectConfig.eventsDisabled = true
      this.restApiEndpointSelectConfig.replaceSelectedItems!.emit([])
      this.restApiEndpointSelectConfig.eventsDisabled = false
    })
  }

  removeAllRestApiEndpoints() {
    this.restApiLimits!.endpointLimits = []
    this.restApiEndpointSelectConfig.eventsDisabled = true
    this.restApiEndpointSelectConfig.replaceSelectedItems!.emit([])
    this.restApiEndpointSelectConfig.eventsDisabled = false
  }

  /**
   * Builds the full list of IANA time zone IDs supported by the browser, along with their current
   * UTC offset (e.g. "Europe/Brussels" / "UTC+02:00"), sorted by zone ID. A "UTC" entry is always
   * added first (id 0), followed by a separator, ahead of the regular sorted zone list.
   */
  buildTimeZoneInfos(): TimeZoneInfo[] {
    // Exclude a bare "UTC" if the browser already reports it, to avoid a duplicate entry.
    const zoneIds = Intl.supportedValuesOf('timeZone').filter((zoneId) => zoneId != 'UTC').sort()
    const utcInfo: TimeZoneInfo = { id: 0, zoneId: 'UTC', label: 'Coordinated Universal Time - UTC', offset: 'UTC+00:00' }
    const otherInfos = zoneIds.map((zoneId, index) => {
      return { id: index + 1, zoneId: zoneId, label: zoneId, offset: this.offsetForZone(zoneId) }
    })
    return [utcInfo, ...otherInfos]
  }

  offsetForZone(zoneId: string): string {
    const parts = new Intl.DateTimeFormat('en-US', { timeZone: zoneId, timeZoneName: 'longOffset' }).formatToParts(new Date())
    const namePart = parts.find((part) => part.type == 'timeZoneName')
    const offset = namePart?.value ?? 'UTC'
    // Normalise the bare "GMT"/"UTC" (returned for zero offset) to "UTC+00:00".
    if (offset == 'GMT' || offset == 'UTC') {
      return 'UTC+00:00'
    }
    return offset.replace('GMT', 'UTC')
  }

  protected readonly Constants = Constants;
}
