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
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { UserService } from 'src/app/services/user.service';
import { OrganisationDetailsComponent } from '../../admin/user-management/organisation/organisation-details/organisation-details.component';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemService } from 'src/app/services/system.service';
import { map } from 'rxjs';
import { AccountService } from 'src/app/services/account.service';
import { CommunityService } from 'src/app/services/community.service';
import { LandingPageService } from 'src/app/services/landing-page.service';
import { LegalNoticeService } from 'src/app/services/legal-notice.service';
import { ErrorTemplateService } from 'src/app/services/error-template.service';

@Component({
    selector: 'app-organisation',
    templateUrl: './../../admin/user-management/organisation/organisation-details/organisation-details.component.html',
    standalone: false
})
export class OrganisationComponent extends OrganisationDetailsComponent implements OnInit {

  constructor(
    route: ActivatedRoute,
    confirmationDialogService: ConfirmationDialogService,
    organisationService: OrganisationService,
    userService: UserService,
    dataService: DataService,
    popupService: PopupService,
    routingService: RoutingService,
    systemService: SystemService,
    communityService: CommunityService,
    landingPageService: LandingPageService,
    legalNoticeService: LegalNoticeService,
    errorTemplateService: ErrorTemplateService,
    router: Router,
    private readonly accountService: AccountService
  ) {
    super(route, confirmationDialogService, organisationService, userService, dataService, popupService, routingService, systemService, communityService, landingPageService, legalNoticeService, errorTemplateService, router)
  }

  override getOrganisationId() {
    return this.dataService.user?.organization?.id!
  }

  override getCommunityId(): number {
    return this.dataService.community!.id
  }

  override isShowAdminInfo() {
    return false
  }

  protected isShowLandingPage() {
    return this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin
  }

  override isReadonly() {
    return !this.route.snapshot.data.canEditOwnOrganisation
  }

  override isShowCreateUser() {
    return this.dataService.isVendorAdmin
  }

  override isShowCreateSystem() {
    return (this.dataService.isVendorAdmin && this.dataService.community!.allowSystemManagement) || this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin
  }

  override showUserStatus() {
    return !this.dataService.isVendorUser
  }

  override isShowUsersTab() {
    return (this.dataService.isVendorAdmin || this.dataService.isVendorUser) && !this.dataService.isDemoAccount()
  }

  override isApiInfoVisible() {
    return super.isApiInfoVisible() && this.dataService.community?.allowAutomationApi == true
  }

  override breadcrumbInit() {
    this.routingService.ownOrganisationBreadcrumbs()
  }

  override getUsers() {
    return this.accountService.getVendorUsers()
      .pipe(
        map((data) => {
          return data.map((user) => {
            if (user.id == this.dataService.user!.id) {
              user.name = user.name + ' (You)'
            }
            return user
          })
        })
      )
  }

  override ngOnInit(): void {
    super.ngOnInit()
  }

  override doUpdate() {
    this.savePending = true
    let landingPageIdToUse: number|undefined = undefined
    if (this.showAdminInfo || this.showLandingPage) {
      landingPageIdToUse = this.organisation.landingPage
      if (landingPageIdToUse == undefined) {
        landingPageIdToUse = -1
      }
    }
    this.accountService.updateVendorProfile(this.organisation.fname, this.organisation.sname, this.propertyData.edit, this.propertyData.properties, landingPageIdToUse)
    .subscribe(() => {
      this.dataService.user!.organization!.fname = this.organisation.fname!
      this.dataService.user!.organization!.sname = this.organisation.sname!
      this.dataService.vendor!.fname = this.organisation.fname!
      this.dataService.vendor!.sname = this.organisation.sname!
      if (landingPageIdToUse != undefined) {
        this.dataService.currentLandingPageContent = undefined
      }
      this.popupService.success(this.dataService.labelOrganisation()+" information updated.")
    }).add(() => {
      this.savePending = false
    })
  }

}
