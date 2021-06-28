import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Observable } from 'rxjs';
import { map, mergeMap, share } from 'rxjs/operators';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { UserService } from 'src/app/services/user.service';
import { IdLabel } from 'src/app/types/id-label';
import { User } from 'src/app/types/user.type';

@Component({
  selector: 'app-create-user',
  templateUrl: './create-user.component.html',
  styles: [
  ]
})
export class CreateUserComponent extends BaseComponent implements OnInit, AfterViewInit {

  orgId!: number
  communityId!: number
  user: Partial<User> = {}
  roleCreateChoices!: IdLabel[]
  savePending = false

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private userService: UserService,
    private authService: AuthService,
    private popupService: PopupService,
    public dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    if (this.dataService.configuration.ssoEnabled) {
      this.dataService.focus('email')
    } else {
      this.dataService.focus('name')    
    }
  }

  ngOnInit(): void {
    this.orgId = Number(this.route.snapshot.paramMap.get('org_id'))
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    this.roleCreateChoices = Constants.VENDOR_USER_ROLES
  }

  saveDisabled() {
    if (this.dataService.configuration.ssoEnabled) {
      return !(this.textProvided(this.user.email) && this.user.role != undefined)
    } else {
      return !(this.textProvided(this.user.name) && this.textProvided(this.user.password) && this.textProvided(this.user.passwordConfirmation) && this.textProvided(this.user.email) && this.user.role != undefined)
    }
  }

  createUser() {
    this.clearAlerts()
    const isSSO = this.dataService.configuration.ssoEnabled
    let ok = false
    let emailCheckFunction: (email: string, orgId: number, roleId: number) => Observable<{available: boolean}>
    if (isSSO) {
      emailCheckFunction = this.authService.checkEmailOfOrganisationUser.bind(this.authService)
    } else {
      ok = this.requireSame(this.user.password, this.user.passwordConfirmation, "Please enter equal passwords.")
      emailCheckFunction = this.authService.checkEmail.bind(this.authService)
    }
    if (ok) {
      this.savePending = true
      emailCheckFunction(this.user.email!, this.orgId, this.user.role!).pipe(
        mergeMap((data) => {
          if (data.available) {
            return this.userService.createVendorUser(this.user.name!, this.user.email!, this.user.password!, this.orgId, this.user.role!).pipe(
              map(() => {
                this.cancelCreateUser()
                this.popupService.success('User created.')
              })
            )
          } else {
            if (isSSO) {
              this.addAlertError("A user with this email address has already been registered with the specified role for this organisation.")
            } else {
              this.addAlertError("A user with this username has already been registered.")
            }
            return EMPTY
          }
        }),
        share()
      ).subscribe(() => {}).add(() => {
        this.savePending = false
      })
    }
  }

  cancelCreateUser() {
    return this.router.navigate(['admin', 'users', 'community', this.communityId, 'organisation', this.orgId])
  }

}
