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

import {AfterViewInit, Component, OnDestroy, OnInit} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DisconnectRoleComponent} from 'src/app/modals/disconnect-role/disconnect-role.component';
import {AccountService} from 'src/app/services/account.service';
import {AuthProviderService} from 'src/app/services/auth-provider.service';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {BaseComponent} from '../../base-component.component';
import {RoutingService} from 'src/app/services/routing.service';
import {ValidationState} from 'src/app/types/validation-state';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {UserPreferences} from '../../../types/user-preferences';
import {Subscription} from 'rxjs';

@Component({
    selector: 'app-profile',
    templateUrl: './profile.component.html',
    styles: [],
    standalone: false
})
export class ProfileComponent extends BaseComponent implements OnInit, AfterViewInit, OnDestroy {

  spinner = false
  edit = false
  data: {
    name?: string,
    email?: string,
    role?: string,
    preferences: UserPreferences
  } = {
    preferences: {
      menuCollapsed: !this.dataService.menuVisibility,
      statementsCollapsed: !this.dataService.conformanceStatementDetailVisibility,
      pageSize: this.dataService.defaultPagingTableSize,
      homePageType: this.dataService.homePageType
    }
  }
  menuVisibilitySubscription?: Subscription
  validation = new ValidationState()

  constructor(
    public readonly dataService: DataService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly authProviderService: AuthProviderService,
    private readonly accountService: AccountService,
    private readonly popupService: PopupService,
    private readonly modalService: NgbModal,
    private readonly routingService: RoutingService
  ) {
    super()
  }

  ngOnInit(): void {
    this.data.name = this.dataService.user!.name
    this.data!.email = this.dataService.user!.email
		this.data!.role = Constants.USER_ROLE_LABEL[this.dataService.user!.role!]
    this.menuVisibilitySubscription = this.dataService.onMenuVisibilityChange$.subscribe((visible) => {
      setTimeout(() => {
        this.data.preferences.menuCollapsed = !visible
      })
    })
    this.routingService.profileBreadcrumbs()
  }

  ngAfterViewInit(): void {
    if (!this.dataService.configuration.ssoEnabled) {
      this.dataService.focus('name')
    }
  }

  ngOnDestroy(): void {
    if (this.menuVisibilitySubscription) this.menuVisibilitySubscription.unsubscribe()
  }

	disconnect() {
    const modalRef = this.modalService.open(DisconnectRoleComponent, { size: 'lg' })
    modalRef.closed.subscribe((choice?: number) => {
      if (choice != undefined) {
        this.dataService.recordLoginOption(Constants.LOGIN_OPTION.FORCE_CHOICE)
        this.dataService.removeLocationData()
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
    this.confirmationDialogService.confirmed("Confirmation", "Before linking another role to your account your current session will be closed. Are you sure you want to proceed?", "Disconnect", "Cancel", Constants.BUTTON_ICON.DISCONNECT)
      .subscribe(() => {
        this.dataService.recordLoginOption(Constants.LOGIN_OPTION.LINK_ACCOUNT_INTERNAL)
        this.dataService.removeLocationData()
        this.authProviderService.signalLogout({full: false, keepLoginOption: true})
      })
  }

	register() {
		this.confirmationDialogService.confirmed("Confirmation", "Before registering another "+this.dataService.labelOrganisationLower()+" your current session will be closed. Are you sure you want to proceed?", "Disconnect", "Cancel", Constants.BUTTON_ICON.DISCONNECT)
		.subscribe(() => {
      this.dataService.recordLoginOption(Constants.LOGIN_OPTION.REGISTER_INTERNAL)
      this.dataService.removeLocationData()
      this.authProviderService.signalLogout({full: false, keepLoginOption: true})
    })
  }

	cancelEdit() {
		this.edit = false
    this.data!.name = this.dataService.user!.name
  }

	saveDisabled() {
    return this.spinner || !this.textProvided(this.data!.name)
  }

	updateProfile() {
		if (this.checkForm()) {
			this.spinner = true // Start spinner before calling service operation
			this.accountService.updateUserProfile(this.data.name, this.data.preferences).subscribe((data) => {
        if (this.dataService.configuration.ssoEnabled) {
          this.dataService.user!.name = this.data!.name
        }
        if (this.data.preferences.menuCollapsed != !this.dataService.menuVisibility) {
          this.dataService.setMenuVisibility(!this.data.preferences.menuCollapsed)
        }
        if (this.data.preferences.statementsCollapsed != !this.dataService.conformanceStatementDetailVisibility) {
          this.dataService.setConformanceStatementDetailVisibility(!this.data.preferences.statementsCollapsed)
        }
        if (this.data.preferences.pageSize != this.dataService.defaultPagingTableSize) {
          this.dataService.setDefaultPageSize(this.data.preferences.pageSize)
        }
        if (this.data.preferences.homePageType != this.dataService.homePageType) {
          this.dataService.setHomePageType(this.data.preferences.homePageType)
        }
        this.popupService.success("Your profile has been updated.")
      }).add(() => {
        this.spinner = false
        this.cancelEdit()
      })
    }
  }

	checkForm() {
		this.validation.clearErrors()
    let valid = true
		if (this.dataService.configuration.ssoEnabled && !this.textProvided(this.data!.name)) {
      this.validation.invalid('name', 'Your name cannot be empty.')
			this.data!.name = this.dataService.user!.name
      valid = false
    }
    return valid
  }

}
