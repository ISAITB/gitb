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
      feedback = 'A user with this username has already been registered.'
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
            return this.userService.createSystemAdmin(this.trimString(this.user.name), this.trimString(this.user.email)!, this.trimString(this.user.password)).pipe(
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
