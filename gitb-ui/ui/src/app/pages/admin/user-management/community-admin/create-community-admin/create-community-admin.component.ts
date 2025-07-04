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
import { EMPTY } from 'rxjs';
import { map, mergeMap, share } from 'rxjs/operators';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { Constants } from 'src/app/common/constants';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-community-admin',
    templateUrl: './create-community-admin.component.html',
    styles: [],
    standalone: false
})
export class CreateCommunityAdminComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  user: Partial<User> = {}
  isSSO: boolean = false
  savePending = false
  validation = new ValidationState()

  constructor(
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly userService: UserService,
    private readonly authService: AuthService,
    public readonly dataService: DataService,
    private readonly popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
    if (this.isSSO) {
      this.dataService.focus('email')
    } else {
      this.dataService.focus('name')
    }
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    this.isSSO = this.dataService.configuration.ssoEnabled
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
      return this.authService.checkEmailOfCommunityAdmin(email, this.communityId)
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
            return this.userService.createCommunityAdmin(this.trimString(this.user.name), this.trimString(this.user.email)!, this.trimString(this.user.password), this.communityId).pipe(
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

  private reportThatUserExists() {
    let feedback: string
    if (this.isSSO) {
      feedback = 'An administrator with this email address has already been registered.'
    } else {
      feedback = 'A user with this username has already been registered.'
    }
    this.validation.invalid('email', feedback)
  }

  cancelCreateAdmin() {
    this.routingService.toCommunity(this.communityId, CommunityTab.administrators)
  }

}
