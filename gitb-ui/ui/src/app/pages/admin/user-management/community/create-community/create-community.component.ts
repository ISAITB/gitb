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
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { CommunityService } from 'src/app/services/community.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Community } from 'src/app/types/community';
import { Domain } from 'src/app/types/domain';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-community',
    templateUrl: './create-community.component.html',
    styles: [],
    standalone: false
})
export class CreateCommunityComponent extends BaseComponent implements OnInit {

  community: Partial<Community> = {
    selfRegType: Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED,
    selfRegRestriction: Constants.SELF_REGISTRATION_RESTRICTION.NO_RESTRICTION,
    allowCertificateDownload: false,
    allowSystemManagement: true,
    allowStatementManagement: true,
    allowPostTestOrganisationUpdates: true,
    allowPostTestSystemUpdates: true,
    allowPostTestStatementUpdates: true,
    allowAutomationApi: false,
    allowCommunityView: false,
    interactionNotification: false
  }
  domains: Domain[] = []
  savePending = false
  loaded = false
  validation = new ValidationState()

  constructor(
    private readonly routingService: RoutingService,
    private readonly communityService: CommunityService,
    private readonly conformanceService: ConformanceService,
    private readonly dataService: DataService,
    private readonly popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    this.conformanceService.getDomains()
    .subscribe((data) => {
      this.domains = data
    }).add(() => {
      this.loaded = true
    })
  }

  saveDisabled() {
    return !(this.textProvided(this.community.sname) && this.textProvided(this.community.fname) &&
      (!this.dataService.configuration.registrationEnabled ||
        (this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED ||
          (
            (this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING || this.textProvided(this.community.selfRegToken)) &&
            (!this.dataService.configuration.emailEnabled || (!this.community.selfRegNotification || this.textProvided(this.community.email)))
          )
        )
      ) &&
      (!this.dataService.configuration.emailEnabled || (!this.community.interactionNotification || this.textProvided(this.community.email)))
    )
  }

  createCommunity() {
    this.validation.clearErrors()
    const emailValid = !this.textProvided(this.community.email) || this.isValidEmail(this.community.email)
    if (!emailValid) {
      this.validation.invalid("supportEmail", "Please enter a valid support email.")
    }
    const notificationValid = !this.community.selfRegNotification || this.textProvided(this.community.email)
    if (!notificationValid) {
      this.validation.invalid("supportEmail", "A support email needs to be defined to support notifications.")
    }
    if (emailValid && notificationValid) {
      let descriptionToUse: string|undefined
      if (!this.community.sameDescriptionAsDomain) {
        descriptionToUse = this.community.activeDescription
      }
      this.savePending = true
      this.communityService.createCommunity(this.community.sname!, this.community.fname!, this.community.email,
        this.community.selfRegType!, this.community.selfRegRestriction!, this.community.selfRegToken, this.community.selfRegTokenHelpText, this.community.selfRegNotification,
        this.community.interactionNotification!, descriptionToUse, this.community.selfRegForceTemplateSelection, this.community.selfRegForceRequiredProperties,
        this.community.allowCertificateDownload!, this.community.allowStatementManagement!, this.community.allowSystemManagement!, this.community.allowPostTestOrganisationUpdates!,
        this.community.allowPostTestSystemUpdates!, this.community.allowPostTestStatementUpdates!, this.community.allowAutomationApi, this.community.allowCommunityView!,
        this.community.domain?.id)
      .subscribe(() => {
        this.cancelCreateCommunity()
        this.popupService.success('Community created.')
      }).add(() => {
        this.savePending = false
      })
    }
  }

  cancelCreateCommunity() {
    this.routingService.toUserManagement()
  }

}
