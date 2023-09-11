import { AfterViewInit, Component, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { CookieService } from 'ngx-cookie-service';
import { Constants } from 'src/app/common/constants';
import { DisconnectRoleComponent } from 'src/app/modals/disconnect-role/disconnect-role.component';
import { AccountService } from 'src/app/services/account.service';
import { AuthProviderService } from 'src/app/services/auth-provider.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { BaseComponent } from '../../base-component.component';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styles: [
  ]
})
export class ProfileComponent extends BaseComponent implements OnInit, AfterViewInit {

  spinner = false
  edit = false
  data:{name?: string, email?: string, role?: string} = {}

  constructor(
    public dataService: DataService,
    private confirmationDialogService: ConfirmationDialogService,
    private authProviderService: AuthProviderService,
    private accountService: AccountService,
    private cookieService: CookieService,
    private popupService: PopupService,
    private modalService: BsModalService
  ) {
    super()
  }

  ngOnInit(): void {
    this.data.name = this.dataService.user!.name
    this.data!.email = this.dataService.user!.email
		this.data!.role = Constants.USER_ROLE_LABEL[this.dataService.user!.role!]
  }

  ngAfterViewInit(): void {
    if (!this.dataService.configuration.ssoEnabled) {
      this.dataService.focus('name')
    }
  }

	disconnect() {
    const modalRef = this.modalService.show(DisconnectRoleComponent)
    modalRef.content!.result.subscribe((choice?: number) => {
      if (choice != undefined) {
        this.cookieService.set(Constants.LOGIN_OPTION_COOKIE_KEY, Constants.LOGIN_OPTION.FORCE_CHOICE)
        this.authProviderService.signalLogout({ full: false, keepLoginOption: true })
        if (choice == Constants.DISCONNECT_ROLE_OPTION.CURRENT_PARTIAL) {
				  this.popupService.success("Role disconnected from your account.")
        } else if (choice == Constants.DISCONNECT_ROLE_OPTION.CURRENT_FULL) {
				  this.popupService.success("Role fully removed from your account.")
        } else {
          this.popupService.success("All your role assignments were removed and information deleted.")        
        }
      }
    })
  }

	linkOtherRole() {
    this.confirmationDialogService.confirmed("Confirmation", "Before linking another role to your account your current session will be closed. Are you sure you want to proceed?", "Disconnect", "Cancel")
      .subscribe(() => {
        this.cookieService.set(Constants.LOGIN_OPTION_COOKIE_KEY, Constants.LOGIN_OPTION.LINK_ACCOUNT)
        this.authProviderService.signalLogout({full: false, keepLoginOption: true})
      })
  }

	register() {
		this.confirmationDialogService.confirmed("Confirmation", "Before registering another "+this.dataService.labelOrganisationLower()+" your current session will be closed. Are you sure you want to proceed?", "Disconnect", "Cancel")
		.subscribe(() => {
      this.cookieService.set(Constants.LOGIN_OPTION_COOKIE_KEY, Constants.LOGIN_OPTION.REGISTER)
      this.authProviderService.signalLogout({full: false, keepLoginOption: true})
    })
  }

	cancelEdit() {
		this.edit = false
    this.data!.name = this.dataService.user!.name
  }

	editProfile() {
		this.edit = true
  }

	saveDisabled() {
    return this.spinner || !this.textProvided(this.data!.name)
  }

	updateProfile() {
		if (this.checkForm()) {
			this.spinner = true // Start spinner before calling service operation
			this.accountService.updateUserProfile(this.data!.name!).subscribe((data) => {
        this.dataService.user!.name = this.data!.name
        this.popupService.success("Your name has been updated.")
      }).add(() => {
        this.spinner = false
        this.cancelEdit()
      })
    }
  }

	checkForm() {
		this.clearAlerts()
    let valid = true
		if (!this.textProvided(this.data!.name)) {
			this.addAlertError('Your name can not be empty.')
			this.data!.name = this.dataService.user!.name
      valid = false
    }
    return valid
  }

}
