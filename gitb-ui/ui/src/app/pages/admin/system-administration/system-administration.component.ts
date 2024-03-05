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
import { PopupService } from 'src/app/services/popup.service';
import { SystemConfigurationService } from 'src/app/services/system-configuration.service';
import { find } from 'lodash';
import { Community } from 'src/app/types/community';
import { CommunityService } from 'src/app/services/community.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { Organisation } from 'src/app/types/organisation.type';
import { EntityWithId } from 'src/app/types/entity-with-id';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConfigStatus } from './config-status';
import { forkJoin, map, mergeMap, of, share } from 'rxjs';
import { Theme } from 'src/app/types/theme';
import { EmailSettings } from 'src/app/types/email-settings';
import { CodeEditorModalComponent } from 'src/app/components/code-editor-modal/code-editor-modal.component';
import { BsModalService } from 'ngx-bootstrap/modal';
import { SystemConfiguration } from 'src/app/types/system-configuration';

@Component({
  selector: 'app-system-administration',
  templateUrl: './system-administration.component.html',
  styleUrls: [ './system-administration.component.less' ]
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

  // Demo account
  demoAccountStatus: ConfigStatus = { pending: false, collapsed: true, enabled: false, fromDefault: false, fromEnv: false}
  demoAccountEnabled = false
  communities: Community[] = []
  selectedCommunity?: Community
  organisations: Organisation[] = []
  selectedOrganisation?: Organisation
  users: User[] = []
  loadCommunitiesPending = false
  loadOrganisationsPending = false
  loadUsersPending = false
  demoAccount?: User

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
        // Demo account.
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
                  this.demoAccount = user
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
      if (this.emailSettings.password == undefined) {
        this.emailSettings.updatePassword = true
      } else {
        this.emailSettings.updatePassword = false
      }
    }
  }

  applyDemoCommunity() {
    if (this.selectedOrganisation && this.communities && this.communities.length > 0) {
      this.selectedCommunity = find(this.communities, (community) => community.id == this.selectedOrganisation?.community)
      this.loadCommunityOrganisations()
    }
  }

  selectCommunity() {
    this.selectedOrganisation = undefined
    this.demoAccount = undefined
    this.organisations = []
    this.loadCommunityOrganisations()
  }

  selectCommunityOrganisation() {
    this.demoAccount = undefined
    this.users = []
    this.loadOrganisationUsers()
  }

  private loadCommunityOrganisations() {
    if (this.selectedCommunity) {
      this.loadOrganisationsPending = true
      this.loadUsersPending = true
      this.organisationService.getOrganisationsByCommunity(this.selectedCommunity.id)
      .subscribe((data) => {
        this.organisations = data
        if (this.selectedOrganisation == undefined) {
          if (this.organisations.length == 1) {
            this.selectedOrganisation = this.organisations[0]
          }
        }
        if (this.selectedOrganisation) {
          this.loadOrganisationUsers()
        }
      }).add(() => {
        this.loadOrganisationsPending = false
      })
    }
  }

  private loadOrganisationUsers() {
    if (this.selectedOrganisation) {
      this.loadUsersPending = true
      this.userService.getBasicUsersByOrganization(this.selectedOrganisation.id)
      .subscribe((data) => {
        this.users = data
        if (this.demoAccount == undefined) {
          if (this.users.length == 1) {
            this.demoAccount = this.users[0]
          }
        }
      }).add(() => {
        this.loadUsersPending = false
      })
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

  sameId(a: EntityWithId, b: EntityWithId) {
    return a == undefined && b == undefined || a != undefined && b != undefined && a.id == b.id
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

}
