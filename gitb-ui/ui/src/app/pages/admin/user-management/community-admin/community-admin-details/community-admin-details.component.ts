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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { Constants } from 'src/app/common/constants';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';

@Component({
    selector: 'app-community-admin-details',
    templateUrl: './community-admin-details.component.html',
    styles: [],
    standalone: false
})
export class CommunityAdminDetailsComponent extends BaseComponent implements OnInit {

  communityId!: number
  userId!: number
  user: Partial<User> = {}
  disableDeleteButton = false
  changePassword = false
  savePending = false
  deletePending = false
  loaded = false
  focusField?: string

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    public dataService: DataService,
    private userService: UserService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    this.userId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.USER_ID))
    this.disableDeleteButton = Number(this.dataService.user!.id) == Number(this.userId)
    if (!this.dataService.configuration.ssoEnabled) {
      this.focusField = 'name'
    }
    this.userService.getUserById(this.userId)
    .subscribe((data) => {
      this.user = data!
      this.user.ssoStatusText = this.dataService.userStatus(this.user.ssoStatus)
      this.user.roleText = this.Constants.USER_ROLE_LABEL[this.user.role!]
      this.routingService.communityAdminBreadcrumbs(this.communityId, this.userId, this.dataService.userDisplayName(this.user))
    }).add(() => {
      this.loaded = true
    })
  }

  saveDisabled() {
    return !(this.textProvided(this.user.name) && (!this.changePassword || this.textProvided(this.user.password)))
  }

  updateAdmin() {
    let newPassword: string|undefined
    if (this.changePassword) {
      newPassword = this.user.password
    }
    this.savePending = true
    this.userService.updateCommunityAdminProfile(this.userId, this.user.name!, newPassword)
    .subscribe(() => {
      this.cancelDetailAdmin()
      this.popupService.success('Administrator updated')
      this.dataService.breadcrumbUpdate({ id: this.userId, type: BreadcrumbType.communityAdmin, label: this.dataService.userDisplayName(this.user)})
    }).add(() => {
      this.savePending = false
    })
  }

  deleteAdmin() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this administrator?", "Delete", "Cancel")
    .subscribe(() => {
      this.clearAlerts()
      this.deletePending = true
      this.userService.deleteAdmin(this.userId)
      .subscribe((result) => {
        if (this.isErrorDescription(result)) {
          this.addAlertError(result.error_description)
        } else {
          this.cancelDetailAdmin()
          this.popupService.success('Administrator deleted.')
        }
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailAdmin() {
    this.routingService.toCommunity(this.communityId, CommunityTab.administrators)
  }


}
