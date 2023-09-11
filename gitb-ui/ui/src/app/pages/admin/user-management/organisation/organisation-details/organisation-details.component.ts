import { AfterViewInit, Component, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { User } from 'src/app/types/user.type';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { OrganisationFormData } from '../organisation-form/organisation-form-data';
import { OrganisationTab } from './OrganisationTab';
import { TabsetComponent } from 'ngx-bootstrap/tabs';
import { SystemService } from 'src/app/services/system.service';
import { System } from 'src/app/types/system';

@Component({
  selector: 'app-organisation-details',
  templateUrl: './organisation-details.component.html'
})
export class OrganisationDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  orgId!: number
  communityId!: number
  organisation: Partial<OrganisationFormData> = {}
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'organisation'
  }
  users: User[] = []
  systems: System[] = []
  usersStatus = {status: Constants.STATUS.NONE}
  systemsStatus = {status: Constants.STATUS.NONE}
  userColumns: TableColumnDefinition[] = []
  systemColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description' },
    { field: 'version', title: 'Version' }
  ]  
  savePending = false
  deletePending = false
  tabToShow = OrganisationTab.systems
  tabTriggers!: Record<OrganisationTab, {index: number, loader: () => any}>
  @ViewChild('tabs', { static: false }) tabs?: TabsetComponent;
  showAdminInfo!: boolean
  showCreateUser!: boolean
  readonly!: boolean
  apiInfoVisible?: boolean
  fromCommunityManagement?: boolean

  loadApiInfo = new EventEmitter<void>()

  constructor(
    protected route: ActivatedRoute,
    private confirmationDialogService: ConfirmationDialogService,
    private organisationService: OrganisationService,
    private userService: UserService,
    public dataService: DataService,
    protected popupService: PopupService,
    private routingService: RoutingService,
    private systemService: SystemService,
    router: Router
  ) {
    super()
    // Access the tab to show via router state to have it cleared upon refresh.
    const tabParam = router.getCurrentNavigation()?.extras?.state?.tab
    if (tabParam != undefined) {
      this.tabToShow = OrganisationTab[tabParam as keyof typeof OrganisationTab]
    }    
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.triggerTab(this.tabToShow)
    })  
  }

  protected getOrganisationId() {
    return Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
  }

  protected getCommunityId() {
    return Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
  }

  protected isShowAdminInfo() {
    return true
  }

  protected isReadonly() {
    return false
  }

  protected showUserStatus() {
    return true
  }

  protected isApiInfoVisible() {
    return this.dataService.configuration.automationApiEnabled
  }

  protected isShowCreateUser() {
    return true
  }

  ngOnInit(): void {
    this.fromCommunityManagement = this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)
    this.orgId = this.getOrganisationId()
    this.organisation.id = this.orgId
    this.communityId = this.getCommunityId()
    this.showAdminInfo = this.isShowAdminInfo()
    this.readonly = this.isReadonly()
    this.showCreateUser = this.isShowCreateUser()
    const viewPropertiesParam = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES)
    if (viewPropertiesParam != undefined) {
      this.propertyData.edit = Boolean(viewPropertiesParam)
    }
    this.userColumns = []
    this.userColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.userColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.userColumns.push({ field: 'email', title: 'Username' })
    }
    this.userColumns.push({ field: 'roleText', title: 'Role' })
    if (this.showUserStatus()) {
      this.userColumns.push({ field: 'ssoStatusText', title: 'Status' })
    }
    this.organisationService.getOrganisationById(this.orgId)
    .subscribe((data) => {
      this.organisation = data
      if (data.landingPage == null) this.organisation.landingPage = undefined
      if (data.errorTemplate == null) this.organisation.errorTemplate = undefined
      if (data.legalNotice == null) this.organisation.legalNotice = undefined
    })
    this.apiInfoVisible = this.isApiInfoVisible()
    // Setup tab triggers
    this.setupTabs()    
  }

  private setupTabs() {
    const temp: Partial<Record<OrganisationTab, {index: number, loader: () => any}>> = {}
    temp[OrganisationTab.systems] = {index: 0, loader: () => {this.showSystems()}}
    temp[OrganisationTab.users] = {index: 1, loader: () => {this.showUsers()}}
    temp[OrganisationTab.apiKeys] = {index: 2, loader: () => {this.showApiInfo()}}
    this.tabTriggers = temp as Record<OrganisationTab, {index: number, loader: () => any}>
  }

  triggerTab(tab: OrganisationTab) {
    this.tabTriggers[tab].loader()
    if (this.tabs) {
      this.tabs.tabs[this.tabTriggers[tab].index].active = true
    }
  }

  protected getUsers() {
    return this.userService.getUsersByOrganisation(this.orgId)
  }

  showUsers() {
    if (this.usersStatus.status == Constants.STATUS.NONE) {
      this.usersStatus.status = Constants.STATUS.PENDING

      this.getUsers().subscribe((data) => {
        for (let user of data) {
          user.ssoStatusText = this.dataService.userStatus(user.ssoStatus)
          user.roleText = Constants.USER_ROLE_LABEL[user.role!]
        }
        this.users = data
      }).add(() => {
        this.usersStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  showSystems() {
    if (this.systemsStatus.status == Constants.STATUS.NONE) {
      this.systemsStatus.status = Constants.STATUS.PENDING
      this.systemService.getSystemsByOrganisation(this.orgId, false)
      .subscribe((data) => {
        this.systems = data
      }).add(() => {
        this.systemsStatus.status = Constants.STATUS.FINISHED
      })      
    }
  }

  deleteOrganisation() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelOrganisationLower()+"?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.organisationService.deleteOrganisation(this.orgId)
      .subscribe(() => {
        this.cancelDetailOrganisation()
        this.popupService.success(this.dataService.labelOrganisation()+" deleted.")
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  saveDisabled() {
    return !(this.textProvided(this.organisation.sname) && this.textProvided(this.organisation.fname) && (!this.dataService.configuration.registrationEnabled || (!this.organisation.template || this.textProvided(this.organisation.templateName))) && (!this.propertyData.edit || this.dataService.customPropertiesValid(this.propertyData.properties)))
  }

  doUpdate() {
    this.clearAlerts()
    this.savePending = true
    this.organisationService.updateOrganisation(this.orgId, this.organisation.sname!, this.organisation.fname!, this.organisation.landingPage, this.organisation.legalNotice, this.organisation.errorTemplate, this.organisation.otherOrganisations, this.organisation.template!, this.organisation.templateName, this.propertyData.edit, this.propertyData.properties, this.organisation.copyOrganisationParameters!, this.organisation.copySystemParameters!, this.organisation.copyStatementParameters!)
    .subscribe((result) => {
      if (result?.error_code != undefined) {
        this.addAlertError(result.error_description)
      } else {
        this.popupService.success(this.dataService.labelOrganisation()+" updated.")
      }
    }).add(() => {
      this.savePending = false
    })
  }

  updateOrganisation() {
    if (this.organisation.otherOrganisations != undefined) {
      this.confirmationDialogService.confirmedDangerous("Confirm test setup copy", "Copying the test setup from another "+this.dataService.labelOrganisationLower()+" will remove current "+this.dataService.labelSystemsLower()+", conformance statements and test results. Are you sure you want to proceed?", "Copy", "Cancel")
      .subscribe(() => {
        this.doUpdate()
      })
    } else {
      this.doUpdate()
    }
  }

  userSelect(user: User) {
    if (user.id == this.dataService.user?.id) {
      this.routingService.toProfile()
    } else {
      if (this.fromCommunityManagement) {
        this.routingService.toOrganisationUser(this.communityId, this.orgId, user.id!)
      } else {
        this.routingService.toOwnOrganisationUser(user.id!)
      }
    }
  }

  cancelDetailOrganisation() {
    this.routingService.toCommunity(this.communityId, CommunityTab.organisations)
  }

  manageOrganisationTests() {
    this.routingService.toConformanceStatements(this.communityId, this.orgId)
  }

  createUser() {
    if (this.fromCommunityManagement) {
      this.routingService.toCreateOrganisationUser(this.communityId, this.organisation.id!)
    } else {
      this.routingService.toCreateOwnOrganisationUser()
    }
  }

  systemSelect(system: System) {
    if (this.fromCommunityManagement) {
      this.routingService.toSystemDetails(this.communityId, this.orgId, system.id)
    } else {
      this.routingService.toOwnSystemDetails(system.id)
    }
  }

	createSystem() {
    if (this.fromCommunityManagement) {
      this.routingService.toCreateSystem(this.communityId, this.orgId)
    } else {
      this.routingService.toCreateOwnSystem()
    }
  }

  showApiInfo() {
    this.loadApiInfo.emit()
  }

}
