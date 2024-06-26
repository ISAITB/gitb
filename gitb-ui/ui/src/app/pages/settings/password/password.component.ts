import { Component, OnInit } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { PasswordChangeData } from 'src/app/components/change-password-form/password-change-data.type';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { BaseComponent } from '../../base-component.component';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
  selector: 'app-password',
  templateUrl: './password.component.html'
})
export class PasswordComponent extends BaseComponent implements OnInit {

  passwordChangeData: PasswordChangeData = {}
  spinner = false

  constructor(
    private accountService: AccountService,
    private errorService: ErrorService,
    private dataService: DataService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.routingService.changePasswordBreadcrumbs()
  }

  saveDisabled() {
    return !this.textProvided(this.passwordChangeData.currentPassword)
      || !this.textProvided(this.passwordChangeData.newPassword)
  }

  handleUpdateError(data: any): Observable<any> {
    if (data.error && data.error.error_code == Constants.ERROR_CODES.INVALID_CREDENTIALS) {
      this.addAlertError("You entered a wrong password.")
    } else {
      this.errorService.showErrorMessage(data)
    }
    return of()
  }

  replacePassword() {
    if (!this.saveDisabled()) {
      this.clearAlerts()
      const sameCheck = this.requireDifferent(this.passwordChangeData.currentPassword, this.passwordChangeData.newPassword, 'The password you provided is the same as the current one.')
      const complexCheck = this.requireComplexPassword(this.passwordChangeData.newPassword, 'The new password does not match required complexity rules. It must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit and one symbol.')
      if (sameCheck && complexCheck) {
        // Proceed.
        this.spinner = true
        this.accountService.updatePassword(
          this.passwordChangeData.newPassword!, 
          this.passwordChangeData.currentPassword!, 
          this.handleUpdateError.bind(this)
        ).subscribe(() => {
          this.popupService.success('Your password has been updated.')
          this.passwordChangeData = {}
          this.dataService.focus('currentPassword')
        }).add(() => {
          this.spinner = false
        })
      }
    }
  }

}
