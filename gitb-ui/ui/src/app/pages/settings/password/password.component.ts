import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { PasswordChangeData } from 'src/app/components/change-password-form/password-change-data.type';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { BaseComponent } from '../../base-component.component';

@Component({
  selector: 'app-password',
  templateUrl: './password.component.html'
})
export class PasswordComponent extends BaseComponent implements AfterViewInit {

  passwordChangeData: PasswordChangeData = {}
  spinner = false

  constructor(
    private accountService: AccountService,
    private errorService: ErrorService,
    private dataService: DataService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('current')
  }

  saveDisabled() {
    return !this.textProvided(this.passwordChangeData.currentPassword)
      || !this.textProvided(this.passwordChangeData.password1)
      || !this.textProvided(this.passwordChangeData.password2)
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
      const sameCheck = this.requireDifferent(this.passwordChangeData.currentPassword, this.passwordChangeData.password1, 'The password you provided is the same as the current one.')
      const noConfirmCheck = this.requireSame(this.passwordChangeData.password1, this.passwordChangeData.password2, 'The new password does not match the confirmation.')
      if (sameCheck && noConfirmCheck) {
        // Proceed.
        this.spinner = true
        this.accountService.updatePassword(
          this.passwordChangeData.password1!, 
          this.passwordChangeData.currentPassword!, 
          this.handleUpdateError.bind(this)
        ).subscribe(() => {
          this.popupService.success('Your password has been updated.')
          this.passwordChangeData = {}
          this.dataService.focus('current')
        }).add(() => {
          this.spinner = false
        })
      }
    }
  }

}
