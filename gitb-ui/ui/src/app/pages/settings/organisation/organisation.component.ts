import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { LoadStatus } from 'src/app/common/load-status';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { AddMemberComponent } from 'src/app/modals/add-member/add-member.component';
import { AccountService } from 'src/app/services/account.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { UserService } from 'src/app/services/user.service';
import { Organisation } from 'src/app/types/organisation.type';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { User } from 'src/app/types/user.type';
import { BaseComponent } from '../../base-component.component';

@Component({
  selector: 'app-organisation',
  templateUrl: './organisation.component.html'
})
export class OrganisationComponent extends BaseComponent implements OnInit, AfterViewInit {

  users?: User[]
  udata: User = {} 
  vdata!: Partial<Organisation>
  dataStatus = new LoadStatus()
  propertyData:OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'organisation'
  }
  tableColumns: TableColumnDefinition[] = []
  canEditOwnOrganisation = false
  vendorUpdatePending = false

  constructor(
    public dataService: DataService,
    private organisationService: OrganisationService,
    private userService: UserService,
    private accountService: AccountService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    private errorService: ErrorService,
    private route: ActivatedRoute,
    private modalService: BsModalService
  ) { super() }

  ngAfterViewInit(): void {
    if (this.dataService.isVendorAdmin || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
      this.dataService.focus('shortName')  
    }
  }

  ngOnInit(): void {
    const viewPropertiesParam = this.route.snapshot.queryParamMap.get('viewProperties')
    if (viewPropertiesParam != undefined) {
      this.propertyData.edit = Boolean(viewPropertiesParam)
    }
    this.tableColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.tableColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.tableColumns.push({ field: 'email', title: 'Username' })
    }
    this.tableColumns.push({ field: 'roleText', title: 'Role' })
    this.vdata = {
      fname: this.dataService.vendor!.fname,
      sname: this.dataService.vendor!.sname
    }
    this.organisationService.getOwnOrganisationParameterValues().subscribe((data) => {
      this.propertyData.properties = data
    })
    if (!this.dataService.isVendorUser) {
      this.tableColumns.push({
          field: 'ssoStatusText',
          title: 'Status'
      })
    }
    this.getVendorUsers()
    this.canEditOwnOrganisation = this.route.snapshot.data.canEditOwnOrganisation
  }

  userStatus(ssoStatus: number) {
    return this.dataService.userStatus(ssoStatus)
  }

  deleteVisible() {
    return ((member: User) => {
      // Don't allow deletion of own account or demo user account.
      return member.id != this.dataService.user?.id && (!this.dataService.configuration.demosEnabled || this.dataService.configuration.demosAccount != member.id)
    }).bind(this)
  }

  deleteMember(member: User) {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this user?", "Yes", "No")
    .subscribe(() => {
      this.userService.deleteVendorUser(member.id!)
      .subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.errorService.showErrorMessage(data.error_description!)
        } else {
          this.getVendorUsers() // Get user list again
          this.popupService.success('User deleted.')
        }
      })
    })
  }

  getVendorUsers() {
    this.accountService.getVendorUsers()
    .subscribe((data) => {
      this.users = data.map((user) => {
          if (user.id == this.dataService.user!.id) {
            user.name = user.name + ' (You)'
          }
          user.roleText = Constants.USER_ROLE_LABEL[user.role!]
          user.ssoStatusText = this.userStatus(user.ssoStatus!)
          return user
        }
      )
    }).add(() => {
      this.dataStatus.finished()
    })
  }

  saveDisabled() {
    return this.vendorUpdatePending
      || !this.textProvided(this.vdata.fname) 
      || !this.textProvided(this.vdata.sname)
      || (this.propertyData.edit && !this.dataService.customPropertiesValid(this.propertyData.properties))
  }

  updateVendorProfile() {
    this.vendorUpdatePending = true
    if (this.checkForm1()) {
      this.accountService.updateVendorProfile(this.vdata.fname, this.vdata.sname, this.propertyData.edit, this.propertyData.properties)
      .subscribe(() => {
        this.dataService.user!.organization!.fname = this.vdata.fname!
        this.dataService.user!.organization!.sname = this.vdata.sname!
        this.dataService.vendor!.fname = this.vdata.fname!
        this.dataService.vendor!.sname = this.vdata.sname!
        this.popupService.success(this.dataService.labelOrganisation()+" information updated.")
      })
      .add(() => {
        this.vendorUpdatePending = false
      })
    }
  }

  checkForm1() {
    this.clearAlerts()
    let valid = true
    if (!this.textProvided(this.vdata.fname)) {
      this.addAlertError("Full name of your "+this.dataService.labelOrganisationLower()+" can not be empty.")
      valid = false
    } else if (!this.textProvided(this.vdata.sname)) {
      this.addAlertError("Short name of your "+this.dataService.labelOrganisationLower()+" can not be empty.")
      valid = false
    }
    return valid
  }

  popupMemberForm() {
    this.clearAlerts()
    this.modalService.show(AddMemberComponent, {
      class: 'modal-lg'
    }).content!.$memberAdded.subscribe((memberAdded: boolean) => {
      if (memberAdded) {
        this.getVendorUsers() // Refresh user list
      }
    })
  }

}