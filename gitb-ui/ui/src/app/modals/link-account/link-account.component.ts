/*
 * Copyright (C) 2026 European Union
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

import {AfterViewInit, Component, OnInit} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {Constants} from 'src/app/common/constants';
import {AuthService} from 'src/app/services/auth.service';
import {CommunityService} from 'src/app/services/community.service';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {ActualUserInfo} from 'src/app/types/actual-user-info';
import {SelfRegistrationModel} from 'src/app/types/self-registration-model.type';
import {SelfRegistrationOption} from 'src/app/types/self-registration-option.type';
import {UserAccount} from 'src/app/types/user-account';
import {ValidationState} from 'src/app/types/validation-state';
import {BaseSelfRegistrationPageComponent} from '../../components/self-registration/base-self-registration-page.component';

@Component({
    selector: 'app-link-account',
    templateUrl: './link-account.component.html',
    styles: [],
    standalone: false
})
export class LinkAccountComponent extends BaseSelfRegistrationPageComponent implements OnInit, AfterViewInit {

  createOption!: string
  selfRegOptions!: SelfRegistrationOption[]
  linkedAccounts!: UserAccount[]
  choice = -1
  selectedAccountId = -1
  createPending = false
  selfRegData: SelfRegistrationModel = {}
  email?:string
  password?:string
  validation = new ValidationState()

  constructor(
    dataService: DataService,
    private readonly authService: AuthService,
    private readonly communityService: CommunityService,
    public readonly modalRef: BsModalRef,
    private readonly popupService: PopupService
  ) { super(dataService) }

  ngOnInit(): void {
    if (this.createOption == Constants.LOGIN_OPTION.REGISTER || this.createOption == Constants.LOGIN_OPTION.REGISTER_INTERNAL) {
      this.choice = Constants.CREATE_ACCOUNT_OPTION.SELF_REGISTER
    } else if (this.createOption == Constants.LOGIN_OPTION.MIGRATE) {
      this.choice = Constants.CREATE_ACCOUNT_OPTION.MIGRATE
    } else if (this.createOption == Constants.LOGIN_OPTION.LINK_ACCOUNT || this.createOption == Constants.LOGIN_OPTION.LINK_ACCOUNT_INTERNAL) {
      this.choice = Constants.CREATE_ACCOUNT_OPTION.LINK
    }
  }

  ngAfterViewInit() {
    this.choiceChanged()
  }

  resetSelfRegOptions() {
    this.selfRegData = {}
    for (let option of this.selfRegOptions) {
      (option as any)._selected = undefined
    }
  }

  selectAccount(accountId: number) {
    if (this.selectedAccountId == accountId) {
        this.selectedAccountId = -1
    } else {
      this.selectedAccountId = accountId
    }
  }

  createEnabled(): boolean {
    if (this.choice == Constants.CREATE_ACCOUNT_OPTION.LINK) {
        return this.selectedAccountId != -1
    } else if (this.choice == Constants.CREATE_ACCOUNT_OPTION.MIGRATE) {
        return this.textProvided(this.email) && this.textProvided(this.password)
    } else if (this.choice == Constants.CREATE_ACCOUNT_OPTION.SELF_REGISTER) {
        return this.selfRegDataOk(this.selfRegData)
    } else {
        return false
    }
  }

  choiceChanged() {
    this.selectedAccountId = -1
    this.email = undefined
    this.password = undefined
    this.resetSelfRegOptions()
    if (this.dataService.configuration.ssoInMigration && this.choice == Constants.CREATE_ACCOUNT_OPTION.MIGRATE) {
        this.dataService.focus('username')
    }
  }

  create() {
    this.createPending = true
    this.validation.clearErrors()
    if (this.choice == Constants.CREATE_ACCOUNT_OPTION.LINK) {
      this.authService.linkFunctionalAccount(this.selectedAccountId).subscribe((data) => {
        this.dataService.setActualUser(data as ActualUserInfo)
        this.modalRef.hide()
        this.popupService.success('Role successfully linked.')
      }).add(() => {
        this.createPending = false
      })
    } else if (this.choice == Constants.CREATE_ACCOUNT_OPTION.MIGRATE) {
      this.authService.migrateFunctionalAccount(this.email!, this.password!).subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.validation.applyError(data)
        } else {
          this.dataService.setActualUser(data as ActualUserInfo)
          this.modalRef.hide()
          this.popupService.success('Account successfully migrated.')
        }
      }).add(() => {
        this.createPending = false
      })
    } else if (this.choice == Constants.CREATE_ACCOUNT_OPTION.SELF_REGISTER) {
      let token:string|undefined
      if (this.selfRegData.selfRegOption!.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN) {
        token = this.selfRegData.selfRegToken
      }
      let templateId:number|undefined
      if (this.selfRegData.template) {
        templateId = this.selfRegData.template.id
      }
      this.communityService.selfRegister(this.selfRegData.selfRegOption!.communityId!, token, this.selfRegData.organisationChoice!, this.selfRegData.orgToken, this.selfRegData.orgShortName!, this.selfRegData.orgFullName!, templateId, this.selfRegData.selfRegOption!.organisationProperties, undefined, undefined, undefined)
      .subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.validation.applyError(data)
        } else {
          this.dataService.setActualUser(data as ActualUserInfo)
          this.modalRef.hide()
          this.popupService.success('Registration successful.')
        }
      }).add(() => {
        this.createPending = false
      })
    }
  }

  cancel() {
    this.modalRef.hide()
  }

}
