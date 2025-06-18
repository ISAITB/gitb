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

import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { AuthProviderService } from 'src/app/services/auth-provider.service';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
    selector: 'app-header-menu',
    templateUrl: './header-menu.component.html',
    styleUrls: ['./header-menu.component.less'],
    standalone: false
})
export class HeaderMenuComponent implements OnInit {

  @Input() logoutInProgress!: boolean
  expanded = false
  isOverPopup = false
  isOverHeader = false

  constructor(
    public dataService: DataService,
    private authProviderService: AuthProviderService,
    public routingService: RoutingService
  ) { }

  ngOnInit(): void {
  }

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
    this.authProviderService.signalLogout({full: true})
  }

	switchAccount() {
    this.dataService.recordLoginOption(Constants.LOGIN_OPTION.FORCE_CHOICE)
    this.authProviderService.signalLogout({full: false, keepLoginOption: true})
  }

  overHeader() {
    this.isOverHeader = true
    this.expanded = true
  }

  leftHeader() {
    this.isOverHeader = false
    setTimeout(() => {
      if (!this.isOverPopup) {
        this.expanded = false
      }
    }, 5)
  }

  overPopup() {
    this.isOverPopup = true
    this.expanded = true
  }

  leftPopup() {
    this.isOverPopup = false
    setTimeout(() => {
      if (!this.isOverHeader) {
        this.expanded = false
      }
    }, 5)
  }

}
