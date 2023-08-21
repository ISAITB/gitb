import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { UserService } from 'src/app/services/user.service';
import { OrganisationDetailsComponent } from '../../admin/user-management/organisation/organisation-details/organisation-details.component';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemService } from 'src/app/services/system.service';
import { map } from 'rxjs';
import { AccountService } from 'src/app/services/account.service';

@Component({
  selector: 'app-organisation',
  templateUrl: './../../admin/user-management/organisation/organisation-details/organisation-details.component.html'
})
export class OrganisationComponent extends OrganisationDetailsComponent implements OnInit {

  constructor(
    route: ActivatedRoute,
    confirmationDialogService: ConfirmationDialogService,
    organisationService: OrganisationService,
    userService: UserService,
    dataService: DataService,
    popupService: PopupService,
    routingService: RoutingService,
    systemService: SystemService,
    router: Router,
    private accountService: AccountService
  ) {
    super(route, confirmationDialogService, organisationService, userService, dataService, popupService, routingService, systemService, router)
  }

  override getOrganisationId() {
    return this.dataService.user?.organization?.id!
  }

  override getCommunityId(): number {
    return this.dataService.community!.id
  }
  
  override isShowAdminInfo() {
    return false
  }

  override isReadonly() {
    return !this.route.snapshot.data.canEditOwnOrganisation
  }

  override isShowCreateUser() {
    return this.dataService.isVendorAdmin
  }

  override showUserStatus() {
    return !this.dataService.isVendorUser
  }

  override isApiInfoVisible() {
    return super.isApiInfoVisible() && this.dataService.community?.allowAutomationApi == true
  }

  override getUsers() {
    return this.accountService.getVendorUsers()
      .pipe(
        map((data) => {
          return data.map((user) => {
            if (user.id == this.dataService.user!.id) {
              user.name = user.name + ' (You)'
            }
            return user
          })
        })
      )
  }

  override ngOnInit(): void {
    super.ngOnInit()
  }

  override doUpdate() {
    this.clearAlerts()
    this.savePending = true
    this.accountService.updateVendorProfile(this.organisation.fname, this.organisation.sname, this.propertyData.edit, this.propertyData.properties)
    .subscribe(() => {
      this.dataService.user!.organization!.fname = this.organisation.fname!
      this.dataService.user!.organization!.sname = this.organisation.sname!
      this.dataService.vendor!.fname = this.organisation.fname!
      this.dataService.vendor!.sname = this.organisation.sname!
      this.popupService.success(this.dataService.labelOrganisation()+" information updated.")
    }).add(() => {
      this.savePending = false
    })
  }

}