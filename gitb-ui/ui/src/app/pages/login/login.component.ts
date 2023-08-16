import { HttpClient, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { CookieService } from 'ngx-cookie-service';
import { forkJoin, Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Constants } from 'src/app/common/constants';
import { ROUTES } from 'src/app/common/global';
import { Utils } from 'src/app/common/utils';
import { PasswordChangeData } from 'src/app/components/change-password-form/password-change-data.type';
import { LinkAccountComponent } from 'src/app/modals/link-account/link-account.component';
import { AuthProviderService } from 'src/app/services/auth-provider.service';
import { AuthService } from 'src/app/services/auth.service';
import { CommunityService } from 'src/app/services/community.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { HttpRequestConfig } from 'src/app/types/http-request-config.type';
import { LoginEventInfo } from 'src/app/types/login-event-info.type';
import { LoginResultActionNeeded } from 'src/app/types/login-result-action-needed';
import { LoginResultOk } from 'src/app/types/login-result-ok';
import { SelfRegistrationModel } from 'src/app/types/self-registration-model.type';
import { BaseComponent } from '../base-component.component';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent extends BaseComponent implements OnInit, AfterViewInit {

  directLogin = false
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

  constructor(
    private authProvider: AuthProviderService,
    private cookieService: CookieService,
    public dataService: DataService,
    private httpClient: HttpClient,
    private routingService: RoutingService,
    private confirmationDialogService: ConfirmationDialogService,
    private communityService: CommunityService,
    private authService: AuthService,
    private errorService: ErrorService,
    private popupService: PopupService,
    private modalService: BsModalService
  ) {
    super()
  }

  ngOnInit(): void {
		if (this.authProvider.isAuthenticated()) {
      this.routingService.toHome()
    } else {
      this.loginOption = this.cookieService.get(Constants.LOGIN_OPTION_COOKIE_KEY)
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
        this.directLogin = true
        this.loginViaSelection(this.dataService.configuration.demosAccount)
      } else {
        if (this.dataService.actualUser && this.dataService.actualUser.accounts && this.dataService.actualUser.accounts.length == 1 && this.loginOption != Constants.LOGIN_OPTION.FORCE_CHOICE) {
          this.directLogin = true
          this.loginViaSelection(this.dataService.actualUser.accounts[0].id)
        }
      }
    }
    this.dataService.setBanner(this.directLogin?'Home':'Welcome to the Interoperability Test Bed')
  }

  ngAfterViewInit(): void {
    if (this.loginOption == Constants.LOGIN_OPTION.NONE && !this.dataService.configuration.ssoEnabled) {
      this.dataService.focus('email')
    }
  }

	createAccount(loginOption?: string) {
		if (loginOption == undefined) {
      loginOption = Constants.LOGIN_OPTION.NONE
    }
    this.createPending = true
    forkJoin([
      this.authService.getUserUnlinkedFunctionalAccounts(),
      this.communityService.getSelfRegistrationOptions()
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
      path: ROUTES.controllers.AuthenticationService.access_token().url,
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
        this.dataService.focus('current')
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
    if (result.headers.get('ITB-PATH')) {
      path = result.headers.get('ITB-PATH')!
    } else if (result.body != undefined && (<LoginResultOk>result.body).path != undefined) {
      path = (<LoginResultOk>result.body).path!
    }
    this.loginState = {
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
        this.confirmationDialogService.invalidSessionNotification().subscribe(() => {
          this.authProvider.signalLogout({full: true})
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
    return throwError(error)
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
			this.textProvided(this.selfRegData.adminPassword) && this.textProvided(this.selfRegData.adminPasswordConfirm)
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
          this.addAlertError(data.error_description)
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
    const checkPasswordConfirmed = this.requireSame(this.selfRegData.adminPassword, this.selfRegData.adminPasswordConfirm, 'Your password was not correctly confirmed.')
    const checkPasswordComplex = this.requireComplexPassword(this.selfRegData.adminPassword, 'Your password does not match required complexity rules. It must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit and one symbol.')
		return checkPasswordConfirmed && checkPasswordComplex
  }

  replaceDisabled() {
    return !this.textProvided(this.passwordChangeData.currentPassword)
      || !this.textProvided(this.passwordChangeData.password1)
      || !this.textProvided(this.passwordChangeData.password2)
  }

  private getPasswordComplexityAlertMessage() {
    if (this.weakPassword) {
      return 'The new password does not match required complexity rules.'
    } else {
      return 'The new password does not match required complexity rules. It must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit and one symbol.'
    }
  }

  replacePassword() {
    if (!this.replaceDisabled()) {
      this.clearAlerts()
      const sameCheck = this.requireDifferent(this.passwordChangeData.currentPassword, this.passwordChangeData.password1, 'The password you provided is the same as the current one.')
      const noConfirmCheck = this.requireSame(this.passwordChangeData.password1, this.passwordChangeData.password2, 'The new password does not match the confirmation.')
      const complexCheck = this.requireComplexPassword(this.passwordChangeData.password1, this.getPasswordComplexityAlertMessage())
      if (sameCheck && noConfirmCheck && complexCheck) {
        // Proceed.
        this.spinner = true
        const data = {
          email: this.email,
          password: this.passwordChangeData.password1,
          old_password: this.passwordChangeData.currentPassword
        }
        this.makeAuthenticationPost(ROUTES.controllers.AuthenticationService.replaceOnetimePassword().url, data).subscribe((result: HttpResponse<any>) => {
          // Password replacement was ok - complete login.
          this.completeLogin(result)
          this.popupService.success('Your password has been updated.')
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