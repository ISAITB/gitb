import { Component, EventEmitter, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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

@Component({
  selector: 'app-organisation-details',
  templateUrl: './organisation-details.component.html'
})
export class OrganisationDetailsComponent extends BaseComponent implements OnInit {

  orgId!: number
  communityId!: number
  organisation: Partial<OrganisationFormData> = {}
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'organisation'
  }
  users: User[] = []
  dataStatus = {status: Constants.STATUS.PENDING}
  userColumns: TableColumnDefinition[] = []
  savePending = false
  deletePending = false
  loadApiInfo = new EventEmitter<void>()

  constructor(
    private route: ActivatedRoute,
    private confirmationDialogService: ConfirmationDialogService,
    private organisationService: OrganisationService,
    private userService: UserService,
    public dataService: DataService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.orgId = Number(this.route.snapshot.paramMap.get('org_id'))
    this.organisation.id = this.orgId
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    const viewPropertiesParam = this.route.snapshot.queryParamMap.get('viewProperties')
    if (viewPropertiesParam != undefined) {
      this.propertyData.edit = Boolean(viewPropertiesParam)
    }
    this.userColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.userColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.userColumns.push({ field: 'email', title: 'Username' })
    }
    this.userColumns.push({ field: 'roleText', title: 'Role' })
    this.userColumns.push({ field: 'ssoStatusText', title: 'Status' })
    this.organisationService.getOrganisationById(this.orgId)
    .subscribe((data) => {
      this.organisation = data
      if (data.landingPage == null) this.organisation.landingPage = undefined
      if (data.errorTemplate == null) this.organisation.errorTemplate = undefined
      if (data.legalNotice == null) this.organisation.legalNotice = undefined
    })
    this.userService.getUsersByOrganisation(this.orgId)
    .subscribe((data) => {
      for (let user of data) {
        user.ssoStatusText = this.dataService.userStatus(user.ssoStatus)
        user.roleText = Constants.USER_ROLE_LABEL[user.role!]
      }
      this.users = data
    }).add(() => {
      this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

  deleteOrganisation() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelOrganisationLower()+"?", "Yes", "No")
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
      this.confirmationDialogService.confirmed("Confirm test setup copy", "Copying the test setup from another "+this.dataService.labelOrganisationLower()+" will remove current "+this.dataService.labelSystemsLower()+", conformance statements and test results. Are you sure you want to proceed?", "Yes", "No")
      .subscribe(() => {
        this.doUpdate()
      })
    } else {
      this.doUpdate()
    }
  }

  userSelect(user: User) {
    this.routingService.toOrganisationUser(this.communityId, this.orgId, user.id!)
  }

  cancelDetailOrganisation() {
    this.routingService.toCommunity(this.communityId, CommunityTab.organisations)
  }

  manageOrganisationTests() {
    localStorage.setItem(Constants.LOCAL_DATA.ORGANISATION, JSON.stringify(this.organisation))
    this.routingService.toSystems(this.organisation.id!)
  }

  createUser() {
    this.routingService.toCreateOrganisationUser(this.communityId, this.organisation.id!)
  }

  apiInfoSelected() {
    this.loadApiInfo.emit()
  }

}
