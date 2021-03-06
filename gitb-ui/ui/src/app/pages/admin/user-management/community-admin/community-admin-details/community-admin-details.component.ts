import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';

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
    private router: Router,
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
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    this.userId = Number(this.route.snapshot.paramMap.get('admin_id'))
    this.disableDeleteButton = Number(this.dataService.user!.id) == Number(this.userId)
    this.userService.getUserById(this.userId)
    .subscribe((data) => {
      this.user = data
      this.user.ssoStatusText = this.dataService.userStatus(this.user.ssoStatus)
      this.user.roleText = this.Constants.USER_ROLE_LABEL[this.user.role!]
    })
  }

  saveDisabled() {
    return !(this.textProvided(this.user.name) && (!this.changePassword || (this.textProvided(this.user.password) && this.textProvided(this.user.passwordConfirmation))))
  }

  updateAdmin() {
    this.clearAlerts()
    if (!this.changePassword || this.requireSame(this.user.password, this.user.passwordConfirmation, "Passwords do not match.")) {
      let newPassword: string|undefined
      if (this.changePassword) {
        newPassword = this.user.password
      }
      this.savePending = true
      this.userService.updateCommunityAdminProfile(this.userId, this.user.name!, newPassword)
      .subscribe(() => {
        this.cancelDetailAdmin()
        this.popupService.success('Administrator updated')
      }).add(() => {
        this.savePending = false
      })
    }
  }

  deleteAdmin() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this administrator?", "Yes", "No")
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
    this.router.navigate(['admin', 'users', 'community', this.communityId])
  }
  

}
