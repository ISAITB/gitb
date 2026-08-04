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

import {Component, Input} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {AuthProviderService} from 'src/app/services/auth-provider.service';
import {DataService} from 'src/app/services/data.service';
import {RoutingService} from 'src/app/services/routing.service';
import {Utils} from 'src/app/common/utils';

@Component({
    selector: 'app-header-menu',
    templateUrl: './header-menu.component.html',
    styleUrls: ['./header-menu.component.less'],
    standalone: false
})
export class HeaderMenuComponent {

  @Input() logoutInProgress!: boolean
  expanded = false
  isOverPopup = false
  isOverHeader = false
  private allowedToHide = false

  constructor(
    public readonly dataService: DataService,
    private readonly authProviderService: AuthProviderService,
    public readonly routingService: RoutingService
  ) { }

	userLoaded(): boolean {
		return this.dataService.user != undefined && this.dataService.user.name != undefined
  }

	userFullyLoaded(): boolean {
    return this.userLoaded() && this.dataService.vendor != undefined
  }

  isAuthenticated(): boolean {
    return this.authProviderService.isAuthenticated()
  }

	logout() {
    this.dataService.clearAllDisplayStates()
    this.authProviderService.signalLogout({full: true})
    this.expanded = false
  }

  /**
   * Called on click of the "My profile"/"Change password" links (which navigate via their own
   * [navTarget]), so the popup still closes and (for a plain, unmodified, primary-button click)
   * display state still gets cleared before leaving this page - a modified click opens the
   * destination in a new tab/window rather than navigating away from this one, so it must not
   * clear state here.
   */
  profileOptionNavigating(event: MouseEvent) {
    if (Utils.isPlainNavigationClick(event)) {
      this.dataService.clearAllDisplayStates()
    }
    this.expanded = false
  }

	switchAccount() {
    this.dataService.clearAllDisplayStates()
    this.dataService.recordLoginOption(Constants.LOGIN_OPTION.FORCE_CHOICE)
    this.dataService.removeLocationData()
    this.authProviderService.signalLogout({full: false, keepLoginOption: true})
    this.expanded = false
  }

  overHeader() {
    this.isOverHeader = true
    this.expandPopup()
  }

  leftHeader() {
    this.isOverHeader = false
    setTimeout(() => {
      this.checkToHide()
    }, 5)
  }

  overPopup() {
    this.isOverPopup = true
    this.expandPopup()
  }

  leftPopup() {
    this.isOverPopup = false
    setTimeout(() => {
      this.checkToHide()
    }, 5)
  }

  private checkToHide() {
    if (!this.isOverHeader && !this.isOverPopup && this.allowedToHide) {
      this.expanded = false
    }
  }

  private expandPopup() {
    if (!this.expanded) {
      this.expanded = true
      setTimeout(() => {
        this.allowedToHide = true
        this.checkToHide()
      }, 50)
    }
  }

  protected readonly Constants = Constants;
}
