import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EMPTY, Observable } from 'rxjs';
import { map, mergeMap, share } from 'rxjs/operators';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { IdLabel } from 'src/app/types/id-label';
import { User } from 'src/app/types/user.type';
import { OrganisationTab } from '../../organisation/organisation-details/OrganisationTab';
import { Constants } from 'src/app/common/constants';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-user-details',
    templateUrl: './user-details.component.html',
    styles: [],
    standalone: false
})
export class UserDetailsComponent extends BaseComponent implements OnInit {

  communityId!: number
  orgId!: number
  userId!: number
  user: Partial<User> = {}
  roleChoices!: IdLabel[]
  savePending = false
  deletePending = false
  originalRoleId!: number
  changePassword = false
  fromCommunityManagement!: boolean
  isSelf = false
  validation = new ValidationState()
  loaded = false
  focusField = "name"

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private confirmationDialogService: ConfirmationDialogService,
    private userService: UserService,
    public dataService: DataService,
    private authService: AuthService,
    private popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    if (this.dataService.configuration.ssoEnabled) {
      this.focusField = "role"
    }
    this.fromCommunityManagement = this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)
    if (this.fromCommunityManagement) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.orgId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    }
    this.userId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.USER_ID))
    this.isSelf = this.dataService.user!.id == this.userId
    this.roleChoices = this.Constants.VENDOR_USER_ROLES
    let result: Observable<User|undefined>
    if (this.fromCommunityManagement) {
      result = this.userService.getUserById(this.userId)
    } else {
      result = this.userService.getOwnOrganisationUserById(this.userId)
    }
    result.subscribe((data) => {
      this.user = data!
      this.user.ssoStatusText = this.dataService.userStatus(this.user.ssoStatus)
      this.user.roleText = this.Constants.USER_ROLE_LABEL[this.user.role!]
      this.originalRoleId = this.user.role!
      this.routingService.organisationUserBreadcrumbs(this.communityId, this.orgId, this.userId, this.dataService.userDisplayName(this.user))
    }).add(() => {
      this.loaded = true
    })
  }

  saveDisabled() {
    if (this.dataService.configuration.ssoEnabled) {
      return !(this.user.role != undefined)
    } else {
      return !(this.textProvided(this.user.name) && (!this.changePassword || this.textProvided(this.user.password))  && this.user.role != undefined)
    }
  }

  private emptyEmailCheck(): Observable<{available: boolean}> {
    return new Observable((observer) => {
      observer.next({available: true})
      observer.complete()
    })
  }

  updateUser() {
    this.clearAlerts()
    this.validation.clearErrors()
    const isSSO = this.dataService.configuration.ssoEnabled
    let ok = true
    let emailCheckResult = this.emptyEmailCheck()
    if (isSSO) {
      if (this.originalRoleId != this.user.role) {
        if (this.fromCommunityManagement) {
          emailCheckResult = this.authService.checkEmailOfOrganisationUser(this.user.email!, this.orgId, this.user.role!)
        } else {
          emailCheckResult = this.authService.checkEmailOfOrganisationMember(this.user.email!, this.user.role!)
        }
      }
    }
    if (ok) {
      this.savePending = true
      emailCheckResult.pipe(
        mergeMap((data) => {
          if (data.available) {
            let newPassword: string|undefined
            let newName: string|undefined
            if (!isSSO) {
              newName = this.user.name!
              if (this.changePassword) {
                newPassword = this.user.password
              }
            }
            return this.userService.updateUserProfile(this.userId, newName, this.user.role!, newPassword).pipe(
              map((result) => {
                if (this.isErrorDescription(result)) {
                  if (!this.validation.applyError(result)) {
                    this.addAlertError(result.error_description)
                  }
                } else {
                  this.cancelDetailUser()
                  this.popupService.success('User updated.')
                  this.dataService.breadcrumbUpdate({ id: this.userId, type: BreadcrumbType.organisationUser, label: this.dataService.userDisplayName(this.user)})
                }
              })
            )
          } else {
            let feedback: string
            if (isSSO) {
              feedback = "A user with this email address has already been registered with the specified role for this organisation."
            } else {
              feedback = "A user with this username has already been registered."
            }
            this.validation.invalid('role', feedback)
            return EMPTY
          }
        }),
        share()
      ).subscribe(() => {}).add(() => {
        this.savePending = false
      })
    }
  }

  deleteUser() {
    this.clearAlerts()
    this.validation.clearErrors()
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this user?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.userService.deleteVendorUser(this.userId)
      .subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.addAlertError(data.error_description)
        } else {
          this.cancelDetailUser()
          this.popupService.success('User deleted.')
        }
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailUser() {
    if (this.fromCommunityManagement) {
      this.routingService.toOrganisationDetails(this.communityId, this.orgId, OrganisationTab.users)
    } else {
      this.routingService.toOwnOrganisationDetails(OrganisationTab.users)
    }
  }

}
