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
import { ActivatedRoute } from '@angular/router';
import { EMPTY, Observable } from 'rxjs';
import { map, mergeMap, share } from 'rxjs/operators';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AccountService } from 'src/app/services/account.service';
import { AuthService } from 'src/app/services/auth.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { IdLabel } from 'src/app/types/id-label';
import { User } from 'src/app/types/user.type';
import { OrganisationTab } from '../../organisation/organisation-details/OrganisationTab';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-user',
    templateUrl: './create-user.component.html',
    styles: [],
    standalone: false
})
export class CreateUserComponent extends BaseComponent implements OnInit, AfterViewInit {

  orgId?: number
  communityId?: number
  user: Partial<User> = {}
  roleCreateChoices!: IdLabel[]
  savePending = false
  fromCommunityManagement?: boolean
  validation = new ValidationState()

  constructor(
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly popupService: PopupService,
    public readonly dataService: DataService,
    private readonly accountService: AccountService
  ) { super() }

  ngAfterViewInit(): void {
    if (this.dataService.configuration.ssoEnabled) {
      this.dataService.focus('email')
    } else {
      this.dataService.focus('name')
    }
  }

  ngOnInit(): void {
    this.fromCommunityManagement = this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)
    if (this.fromCommunityManagement) {
      this.orgId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    }
    this.roleCreateChoices = Constants.VENDOR_USER_ROLES
  }

  saveDisabled() {
    if (this.dataService.configuration.ssoEnabled) {
      return !(this.textProvided(this.user.email) && this.user.role != undefined)
    } else {
      return !(this.textProvided(this.user.name) && this.textProvided(this.user.password) && this.textProvided(this.user.email) && this.user.role != undefined)
    }
  }

  createUser() {
    this.validation.clearErrors()
    const isSSO = this.dataService.configuration.ssoEnabled
    let ok = true
    let emailCheckResult: Observable<{available: boolean}>
    if (isSSO) {
      ok = this.isValidEmail(this.user.email)
      if (!ok) {
        this.validation.invalid('email', 'Please enter a valid email address.')
      }
      if (this.fromCommunityManagement) {
        emailCheckResult = this.authService.checkEmailOfOrganisationUser(this.user.email!, this.orgId!, this.user.role!)
      } else {
        emailCheckResult = this.authService.checkEmailOfOrganisationMember(this.user.email!, this.user.role!)
      }
    } else {
      ok = this.isValidUsername(this.user.email)
      if (!ok) {
        this.validation.invalid('email', 'The username cannot contain spaces.')
      }
      emailCheckResult = this.authService.checkEmail(this.user.email!)
    }
    if (ok) {
      this.savePending = true
      emailCheckResult.pipe(
        mergeMap((data) => {
          if (data.available) {
            let result: Observable<void>
            if (this.fromCommunityManagement) {
              result = this.userService.createVendorUser(this.trimString(this.user.name), this.trimString(this.user.email)!, this.trimString(this.user.password), this.orgId!, this.user.role!)
            } else {
              result = this.accountService.registerUser(this.trimString(this.user.name), this.trimString(this.user.email)!, this.trimString(this.user.password), this.user.role!)
            }
            return result.pipe(
              map(() => {
                this.cancelCreateUser()
                this.popupService.success('User created.')
              })
            )
          } else {
            let feedback: string
            if (isSSO) {
              feedback = "A user with this email address has already been registered with the specified role for this organisation."
            } else {
              feedback = "A user with this username has already been registered."
            }
            this.validation.invalid('email', feedback)
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
    if (this.fromCommunityManagement) {
      this.routingService.toOrganisationDetails(this.communityId!, this.orgId!, OrganisationTab.users)
    } else {
      this.routingService.toOwnOrganisationDetails(OrganisationTab.users)
    }
  }

}
