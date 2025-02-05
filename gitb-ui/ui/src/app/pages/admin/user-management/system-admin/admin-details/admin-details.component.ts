import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';

@Component({
    selector: 'app-admin-details',
    templateUrl: './admin-details.component.html',
    standalone: false
})
export class AdminDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  user: Partial<User> = {}
  userId!: number
  disableDeleteButton = false
  changePassword = false
  savePending = false
  deletePending = false
  loaded = false
  focusField?: string

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private userService: UserService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
  }

  ngOnInit(): void {
    this.userId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.USER_ID))
    this.disableDeleteButton = Number(this.dataService.user!.id) == Number(this.userId)
    if (!this.dataService.configuration.ssoEnabled) {
      this.focusField = 'name'
    }
    this.userService.getUserById(this.userId)
    .subscribe((data) => {
      this.user = data!
      this.user.ssoStatusText = this.dataService.userStatus(this.user.ssoStatus)
      this.user.roleText = this.Constants.USER_ROLE_LABEL[this.user.role!]
      this.routingService.testBedAdminBreadcrumbs(this.userId, this.dataService.userDisplayName(this.user))
    }).add(() => {
      this.loaded = true
    })
    this.routingService.testBedAdminBreadcrumbs(this.userId)
  }

  saveDisabled() {
    return !(this.textProvided(this.user.name) && (!this.changePassword || (this.textProvided(this.user.password))))
  }

  updateAdmin() {
    let newPassword: string|undefined
    if (this.changePassword) {
      newPassword = this.user.password
    }
    this.savePending = true
    this.userService.updateSystemAdminProfile(this.userId, this.user.name!, newPassword)
    .subscribe(() => {
      this.cancelDetailAdmin()
      this.popupService.success('Administrator updated')
      this.dataService.breadcrumbUpdate({ id: this.userId, type: BreadcrumbType.systemAdmin, label: this.dataService.userDisplayName(this.user)})
    }).add(() => {
      this.savePending = false
    })
  }

  deleteAdmin() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this administrator?", "Delete", "Cancel")
    .subscribe(() => {
      this.clearAlerts()
      this.deletePending = true
      this.userService.deleteAdmin(this.userId)
      .subscribe((result) => {
        if (this.isErrorDescription(result)) {
          this.addAlertError(result.error_description)
        } else {
          this.cancelDetailAdmin()
          this.popupService.success('Administrator deleted.')
        }
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailAdmin() {
    this.routingService.toSystemAdministration(SystemAdministrationTab.administrators)
  }

}