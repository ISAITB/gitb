import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { Constants } from 'src/app/common/constants';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';

@Component({
  selector: 'app-community-admin-details',
  templateUrl: './community-admin-details.component.html',
  styles: [
  ]
})
export class CommunityAdminDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  userId!: number
  user: Partial<User> = {}
  disableDeleteButton = false
  changePassword = false
  savePending = false
  deletePending = false  

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    public dataService: DataService,
    private userService: UserService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService
  ) { super() }
  
  ngAfterViewInit(): void {
    if (!this.dataService.configuration.ssoEnabled) {
      this.dataService.focus('name')
    }
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    this.userId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.USER_ID))
    this.disableDeleteButton = Number(this.dataService.user!.id) == Number(this.userId)
    this.userService.getUserById(this.userId)
    .subscribe((data) => {
      this.user = data!
      this.user.ssoStatusText = this.dataService.userStatus(this.user.ssoStatus)
      this.user.roleText = this.Constants.USER_ROLE_LABEL[this.user.role!]
      this.routingService.communityAdminBreadcrumbs(this.communityId, this.userId, this.dataService.userDisplayName(this.user))
    })
  }

  saveDisabled() {
    return !(this.textProvided(this.user.name) && (!this.changePassword || this.textProvided(this.user.password)))
  }

  updateAdmin() {
    this.clearAlerts()
    let newPassword: string|undefined
    if (this.changePassword) {
      newPassword = this.user.password
    }
    this.savePending = true
    this.userService.updateCommunityAdminProfile(this.userId, this.user.name!, newPassword)
    .subscribe(() => {
      this.cancelDetailAdmin()
      this.popupService.success('Administrator updated')
      this.dataService.breadcrumbUpdate({ id: this.userId, type: BreadcrumbType.communityAdmin, label: this.dataService.userDisplayName(this.user)})
    }).add(() => {
      this.savePending = false
    })
  }

  deleteAdmin() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this administrator?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.userService.deleteAdmin(this.userId)
      .subscribe(() => {
        this.cancelDetailAdmin()
        this.popupService.success('Administrator deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailAdmin() {
    this.routingService.toCommunity(this.communityId, CommunityTab.administrators)
  }
  

}
