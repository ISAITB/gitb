import { AfterViewInit, Component, OnInit } from '@angular/core';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';
import { mergeMap, map, share } from 'rxjs/operators'
import { EMPTY } from 'rxjs';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-admin',
    templateUrl: './create-admin.component.html',
    styles: [],
    standalone: false
})
export class CreateAdminComponent extends BaseComponent implements OnInit, AfterViewInit {

  user: Partial<User> = {}
  isSSO = false
  savePending = false
  validation = new ValidationState()

  constructor(
    public dataService: DataService,
    private userService: UserService,
    private authService: AuthService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngAfterViewInit(): void {
    if (this.isSSO) {
      this.dataService.focus('email')
    } else {
      this.dataService.focus('name')
    }
  }

  ngOnInit(): void {
    this.isSSO = this.dataService.configuration.ssoEnabled
  }

  private reportThatUserExists() {
    let feedback: string
    if (this.isSSO) {
      feedback = 'An administrator with this email address has already been registered.'
    } else {
      feedback = 'An administrator with this username has already been registered.'
    }
    this.validation.invalid('email', feedback)
  }

  saveDisabled() {
    if (this.isSSO) {
      return !this.textProvided(this.user.email)
    } else {
      return !(this.textProvided(this.user.name) 
        && this.textProvided(this.user.email) 
        && this.textProvided(this.user.password))
    }
  }

  checkEmail(email: string) {
    if (this.isSSO) {
      return this.authService.checkEmailOfSystemAdmin(email)
    } else {
      return this.authService.checkEmail(email)
    }
  }

  createAdmin() {
    this.validation.clearErrors()
    let ok = true
    if (this.isSSO) {
      ok = this.isValidEmail(this.user.email)
      if (!ok) {
        this.validation.invalid('email', 'Please enter a valid email address.')
      }
    } else {
      ok = this.isValidUsername(this.user.email)
      if (!ok) {
        this.validation.invalid('email', 'The username cannot contain spaces.')
      }
    }
    if (ok) {
      this.savePending = true
      this.checkEmail(this.user.email!).pipe(
        mergeMap((data) => {
          if (data.available) {
            return this.userService.createSystemAdmin(this.user.name!.trim(), this.user.email!.trim(), this.user.password!.trim()).pipe(
              map(() => {
                this.cancelCreateAdmin()
                this.popupService.success('Administrator created.')
              })
            )
          } else {
            this.reportThatUserExists()
            return EMPTY
          }
        }),
        share()
      ).subscribe(() => {}).add(() => {
        this.savePending = false
      })
    }
  }

  cancelCreateAdmin() {
    this.routingService.toSystemAdministration(SystemAdministrationTab.administrators)
  }

}
