import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';
import { mergeMap, map, share } from 'rxjs/operators'
import { EMPTY } from 'rxjs';

@Component({
  selector: 'app-create-admin',
  templateUrl: './create-admin.component.html',
  styles: [
  ]
})
export class CreateAdminComponent extends BaseComponent implements OnInit, AfterViewInit {

  user: Partial<User> = {}
  isSSO = false
  savePending = false

  constructor(
    public dataService: DataService,
    private userService: UserService,
    private authService: AuthService,
    private popupService: PopupService,
    private router: Router
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

  saveDisabled() {
    if (this.isSSO) {
      return !this.textProvided(this.user.email)
    } else {
      return !(this.textProvided(this.user.name) 
        && this.textProvided(this.user.email) 
        && this.textProvided(this.user.password) 
        && this.textProvided(this.user.passwordConfirmation))
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
    this.clearAlerts()
    let ok = false
    if (this.isSSO) {
      ok = this.requireValidEmail(this.user.email, "Please enter a valid email address.")
    } else {
      ok = this.requireSame(this.user.password, this.user.passwordConfirmation, "Please enter equal passwords.")
    }
    if (ok) {
      this.savePending = true
      this.checkEmail(this.user.email!).pipe(
        mergeMap((data) => {
          if (data.available) {
            return this.userService.createSystemAdmin(this.user.name!, this.user.email!, this.user.password!).pipe(
              map(() => {
                this.cancelCreateAdmin()
                this.popupService.success('Administrator created.')
              })
            )
          } else {
            if (this.dataService.configuration.ssoEnabled) {
              this.addAlertError('An administrator with this email address has already been registered.')
            } else {
              this.addAlertError('An administrator with this username has already been registered.')
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

  cancelCreateAdmin() {
    this.router.navigate(['admin', 'users'])
  }

}