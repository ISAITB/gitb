/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import { AfterViewInit, Component, EventEmitter, OnInit } from '@angular/core';
import { PasswordChangeData } from 'src/app/components/change-password-form/password-change-data.type';
import { AccountService } from 'src/app/services/account.service';
import { PopupService } from 'src/app/services/popup.service';
import { BaseComponent } from '../../base-component.component';
import { RoutingService } from 'src/app/services/routing.service';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-password',
    templateUrl: './password.component.html',
    standalone: false
})
export class PasswordComponent extends BaseComponent implements OnInit, AfterViewInit {

  passwordChangeData: PasswordChangeData = {}
  spinner = false
  validation = new ValidationState()
  currentPasswordFocusChange = new EventEmitter<boolean>()

  constructor(
    private readonly accountService: AccountService,
    private readonly popupService: PopupService,
    private readonly routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.routingService.changePasswordBreadcrumbs()
  }

  ngAfterViewInit(): void {
    this.currentPasswordFocusChange.emit(true)
  }

  saveDisabled() {
    return !this.textProvided(this.passwordChangeData.currentPassword)
      || !this.textProvided(this.passwordChangeData.newPassword)
  }

  replacePassword() {
    if (!this.saveDisabled()) {
      this.validation.clearErrors()
      if (!this.isDifferent(this.passwordChangeData.currentPassword, this.passwordChangeData.newPassword)) {
        this.validation.invalid('new', 'The password you provided is the same as the current one.')
      } else if (!this.isComplexPassword(this.passwordChangeData.newPassword)) {
        this.validation.invalid('new', 'The new password does not match required complexity rules. It must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit and one symbol.')
      } else {
        // Proceed.
        this.spinner = true
        this.accountService.updatePassword(this.passwordChangeData.newPassword!, this.passwordChangeData.currentPassword!)
        .subscribe((data) => {
          if (this.isErrorDescription(data)) {
            this.validation.applyError(data)
          } else {
            this.popupService.success('Your password has been updated.')
            this.passwordChangeData = {}
            this.currentPasswordFocusChange.emit(true)
          }
        }).add(() => {
          this.spinner = false
        })
      }
    }
  }

}
