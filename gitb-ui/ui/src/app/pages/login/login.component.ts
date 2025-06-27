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

import { HttpClient, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { forkJoin, Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Constants } from 'src/app/common/constants';
import { ROUTES } from 'src/app/common/global';
import { Utils } from 'src/app/common/utils';
import { PasswordChangeData } from 'src/app/components/change-password-form/password-change-data.type';
import { LinkAccountComponent } from 'src/app/modals/link-account/link-account.component';
import { AuthProviderService } from 'src/app/services/auth-provider.service';
import { AuthService } from 'src/app/services/auth.service';
import { CommunityService } from 'src/app/services/community.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { HttpRequestConfig } from 'src/app/types/http-request-config.type';
import { LoginEventInfo } from 'src/app/types/login-event-info.type';
import { LoginResultActionNeeded } from 'src/app/types/login-result-action-needed';
import { LoginResultOk } from 'src/app/types/login-result-ok';
import { SelfRegistrationModel } from 'src/app/types/self-registration-model.type';
import { BaseComponent } from '../base-component.component';
import { SelfRegistrationOption } from 'src/app/types/self-registration-option.type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    standalone: false
})
export class LoginComponent extends BaseComponent implements OnInit, AfterViewInit {

  @ViewChild("emailField", { static: false }) emailField?: ElementRef;

  spinner = false
  createPending = false
  rememberMe = false
  loginOption?: string
  selfRegData: SelfRegistrationModel = {}

  email?: string
  password?: string

  private loginState?: LoginEventInfo
  onetimePassword = false
  weakPassword = false
  passwordChangeData: PasswordChangeData = {}
  loginInProgress = false
  validation = new ValidationState()

  constructor(
    private readonly authProvider: AuthProviderService,
    public readonly dataService: DataService,
    private readonly httpClient: HttpClient,
    private readonly communityService: CommunityService,
    private readonly authService: AuthService,
    private readonly errorService: ErrorService,
    private readonly popupService: PopupService,
    private readonly modalService: BsModalService
  ) {
    super()
  }

  ngOnInit(): void {
    this.loginOption = this.dataService.retrieveLoginOption()
    if (!this.loginOption) {
      this.loginOption = Constants.LOGIN_OPTION.NONE
    }
    if ((this.loginOption == Constants.LOGIN_OPTION.REGISTER && !this.dataService.configuration.registrationEnabled) || (this.loginOption == Constants.LOGIN_OPTION.DEMO && !this.dataService.configuration.demosEnabled) || (this.loginOption == Constants.LOGIN_OPTION.MIGRATE && (!this.dataService.configuration.ssoEnabled || !this.dataService.configuration.ssoInMigration))) {
      // Invalid login option
      this.loginOption = Constants.LOGIN_OPTION.NONE
    }
    if (this.loginOption == Constants.LOGIN_OPTION.REGISTER) {
      if (this.dataService.configuration.ssoEnabled) {
        this.createAccount(this.loginOption)
      }
    } else if (this.loginOption == Constants.LOGIN_OPTION.MIGRATE || this.loginOption == Constants.LOGIN_OPTION.LINK_ACCOUNT) {
      this.createAccount(this.loginOption)
    } else if (this.loginOption == Constants.LOGIN_OPTION.DEMO) {
      this.loginViaSelection(this.dataService.configuration.demosAccount)
    } else if (this.loginOption != Constants.LOGIN_OPTION.FORCE_CHOICE && this.dataService.actualUser && this.dataService.actualUser.accounts) {
      // Check to see if we can automate the functional account selection.
      let userIdToConnectWith: number|undefined
      if (this.dataService.actualUser.accounts.length == 1) {
        // The user is linked to a single functional account. Automate its selection.
        userIdToConnectWith = this.dataService.actualUser.accounts[0].id
      } else if (this.dataService.actualUser.accounts.length > 1) {
        const previousUser = this.dataService.checkLocationUser()
        if (previousUser != undefined) {
          const matchedUser = this.dataService.actualUser.accounts.find((account) => account.id == previousUser)
          if (matchedUser) {
            userIdToConnectWith = matchedUser.id
          }
        }
      }
      if (userIdToConnectWith != undefined) {
        this.loginViaSelection(userIdToConnectWith)
      }
    }
    this.dataService.changeBanner(this.loginInProgress?'Home':'Welcome to the Interoperability Test Bed')
    this.dataService.breadcrumbUpdate({breadcrumbs: []})
  }

  ngAfterViewInit(): void {
    if (this.loginOption == Constants.LOGIN_OPTION.NONE && !this.dataService.configuration.ssoEnabled) {
      this.emailField?.nativeElement.focus()
    }
  }

	createAccount(loginOption?: string) {
		if (loginOption == undefined) {
      loginOption = Constants.LOGIN_OPTION.NONE
    }
    let selfRegistrationOptionObservable: Observable<SelfRegistrationOption[]>
    if (this.dataService.configuration.registrationEnabled) {
      selfRegistrationOptionObservable = this.communityService.getSelfRegistrationOptions()
    } else {
      selfRegistrationOptionObservable = of([])
    }
    this.createPending = true
    forkJoin([
      this.authService.getUserUnlinkedFunctionalAccounts(),
      selfRegistrationOptionObservable
    ]).subscribe((data) => {
      const modalRef = this.modalService.show(LinkAccountComponent, {
        class: 'modal-lg',
        initialState: {
          linkedAccounts: data[0],
          createOption: loginOption,
          selfRegOptions: data[1]
        }
      })
      modalRef.onHide!.subscribe(() => {
        this.createPending = false
      })
    })
  }

	loginViaSelection(userId: number) {
    this.loginInProgress = true
    let config: HttpRequestConfig = {
      path: ROUTES.controllers.AuthenticationService.selectFunctionalAccount().url,
      data: {
        id: userId
      },
      headers: Utils.createHttpHeaders()
    }
		this.loginInternal(config)
  }

	loginViaCredentials(userEmail: string, userPassword: string) {
    let config: HttpRequestConfig = {
      path: ROUTES.controllers.AuthenticationService.accessToken().url,
      data: {
        email: userEmail,
        password: userPassword
      },
      headers: Utils.createHttpHeaders()
    }
    this.loginInternal(config)
  }

  private makeAuthenticationPost(path: string, postData: any): Observable<HttpResponse<any>> {
    return this.httpClient.post(
      this.dataService.completePath(path),
      Utils.objectToFormRequest(postData).toString(),
      {
        observe: 'response',
        headers: Utils.createHttpHeaders()
      }
    ).pipe(catchError(error => this.handleLoginAuthenticationError(error)))
  }

	loginInternal(config: HttpRequestConfig) {
    this.clearAlerts()
    this.spinner = true // Start spinner before calling service operation
    this.makeAuthenticationPost(config.path, config.data).subscribe((result: HttpResponse<LoginResultOk|LoginResultActionNeeded>) => {
      // Login successful.
      if (this.isLoginActionNeeded(result.body)) {
        this.spinner = false
        // Correct authentication but we need to replace the password to complete the login.
        this.onetimePassword = result.body.onetime != undefined && result.body.onetime
        this.weakPassword = result.body.weakPassword != undefined && result.body.weakPassword
      } else if (this.isLoginOk(result.body)) {
        this.completeLogin(result)
      } else {
        // This case should never occur.
        this.spinner = false
        this.addAlertError('You are unable to log in at this time due to an unexpected error.')
      }
    })
  }

  private isLoginOk(obj: LoginResultOk|any): obj is LoginResultOk {
    return obj != undefined && obj.access_token != undefined
  }

  private isLoginActionNeeded(obj: LoginResultActionNeeded|any): obj is LoginResultActionNeeded {
    return obj != undefined && (obj.onetime != undefined || obj.weakPassword != undefined)
  }

  private completeLogin(result: HttpResponse<LoginResultOk|LoginResultActionNeeded>) {
    let path = '/'
    let userId: number|undefined
    if (this.isLoginOk(result.body)) {
      userId = result.body.user_id
      if (result.body.path) {
        path = result.body.path
      }
    }
    if (result.headers.get('ITB-PATH')) {
      path = result.headers.get('ITB-PATH')!
    }
    this.loginState = {
      userId: userId!,
      tokens: result.body,
      path: path,
      remember: this.rememberMe
    }
    this.authProvider.signalLogin(this.loginState)
  }

  private handleLoginAuthenticationError(error: any) {
    this.spinner = false
    if (error.status == 401) {
      if (this.dataService.configuration.ssoEnabled) {
        // We need to re-login to EU Login.
        this.errorService.showInvalidSessionNotification().subscribe((shown) => {
          if (shown) {
            this.authProvider.signalLogout({full: true})
          }
        })
      } else {
        this.addAlertError('Invalid credentials.')
      }
    } else {
      this.errorService.showErrorMessage(error)
    }
    // Clear password fields
    this.password = ''
    this.passwordChangeData = {}
    return throwError(() => error)
  }

  cancelLogin() {
    let url = window.location.href
    window.location.href = url.substring(0, url.indexOf('app#'))
  }

	loginDisabled() {
		return this.spinner || !this.textProvided(this.email) || !this.textProvided(this.password)
  }

	registerDisabled(): boolean {
    return this.spinner || !(
			this.selfRegData.selfRegOption != undefined && this.selfRegData.selfRegOption.communityId &&
			(this.selfRegData.selfRegOption.selfRegType != Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN || this.textProvided(this.selfRegData.selfRegToken)) &&
			(!this.selfRegData.selfRegOption.forceTemplateSelection || (this.selfRegData.selfRegOption.templates == undefined || this.selfRegData.selfRegOption.templates.length == 0) || this.selfRegData.template != undefined) &&
			(!this.selfRegData.selfRegOption.forceRequiredProperties || this.dataService.customPropertiesValid(this.selfRegData.selfRegOption.organisationProperties, true)) &&
			this.textProvided(this.selfRegData.orgShortName) && this.textProvided(this.selfRegData.orgFullName) &&
			this.textProvided(this.selfRegData.adminName) && this.textProvided(this.selfRegData.adminEmail) &&
			this.textProvided(this.selfRegData.adminPassword)
		)
  }

	register() {
		if (this.checkRegisterForm() && !this.registerDisabled()) {
      let token:string|undefined, templateId:number|undefined
      this.spinner = true
      if (this.selfRegData.selfRegOption!.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN) {
        token = this.selfRegData.selfRegToken
      }
      if (this.selfRegData.template) {
        templateId = this.selfRegData.template.id
      }
      this.communityService.selfRegister(this.selfRegData.selfRegOption!.communityId!, token, this.selfRegData.orgShortName!, this.selfRegData.orgFullName!, templateId, this.selfRegData.selfRegOption!.organisationProperties, this.selfRegData.adminName!, this.selfRegData.adminEmail!, this.selfRegData.adminPassword!)
      .subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.validation.applyError(data)
        } else {
          // All ok.
          this.loginViaCredentials(this.selfRegData.adminEmail!, this.selfRegData.adminPassword!)
          this.popupService.success('Registration successful.')
        }
      }).add(() => {
        this.spinner = false
      })
    }
  }

	login() {
		if (this.checkLoginForm()) {
      this.loginViaCredentials(this.email!, this.password!)
    }
  }

	checkLoginForm() {
		this.clearAlerts()
    return true
  }

	checkRegisterForm() {
    this.clearAlerts()
    this.validation.clearErrors()
    const passwordValid = this.isComplexPassword(this.selfRegData.adminPassword)
    if (!passwordValid) {
      this.validation.invalid('new', this.getPasswordComplexityAlertMessage())
    }
    const usernameValid = this.isValidUsername(this.selfRegData.adminEmail)
    if (!usernameValid) {
      this.validation.invalid('adminEmail', 'The username cannot contain spaces.')
    }
		return passwordValid && usernameValid
  }

  replaceDisabled() {
    return !this.textProvided(this.passwordChangeData.currentPassword)
      || !this.textProvided(this.passwordChangeData.newPassword)
  }

  private getPasswordComplexityAlertMessage() {
    if (this.weakPassword) {
      return 'Password does not match required complexity rules.'
    } else {
      return 'Password does not match required complexity rules. It must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit and one symbol.'
    }
  }

  replacePassword() {
    if (!this.replaceDisabled()) {
      this.clearAlerts()
      this.validation.clearErrors()
      if (!this.isDifferent(this.passwordChangeData.currentPassword, this.passwordChangeData.newPassword)) {
        this.validation.invalid('new', 'The password you provided is the same as the current one.')
      } else if (!this.isComplexPassword(this.passwordChangeData.newPassword)) {
        this.validation.invalid('new', this.getPasswordComplexityAlertMessage())
      } else {
        // Proceed.
        this.spinner = true
        const data = {
          email: this.email,
          password: this.passwordChangeData.newPassword,
          old_password: this.passwordChangeData.currentPassword
        }
        this.makeAuthenticationPost(ROUTES.controllers.AuthenticationService.replaceOnetimePassword().url, data).subscribe((result: HttpResponse<any>) => {
          if (this.isErrorDescription(result.body)) {
            this.validation.applyError(result.body)
          } else {
            // Password replacement was ok - complete login.
            this.completeLogin(result)
            this.popupService.success('Your password has been updated.')
          }
        }).add(() => {
          this.spinner = false
        })
      }
    }
  }

  isAuthenticated(): boolean {
    return this.authProvider.isAuthenticated()
  }

}
