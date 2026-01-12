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

import {Component, EventEmitter, Input, OnInit} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {DataService} from 'src/app/services/data.service';
import {Community} from 'src/app/types/community';
import {Domain} from 'src/app/types/domain';
import {RoutingService} from 'src/app/services/routing.service';
import {ValidationState} from 'src/app/types/validation-state';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';
import {of} from 'rxjs';
import {FilterUpdate} from '../../../../../components/test-filter/filter-update';
import {Organisation} from '../../../../../types/organisation.type';
import {OrganisationService} from '../../../../../services/organisation.service';
import {ConfirmationDialogService} from '../../../../../services/confirmation-dialog.service';

@Component({
    selector: 'app-community-form',
    templateUrl: './community-form.component.html',
    styles: [],
    standalone: false
})
export class CommunityFormComponent extends BaseComponent implements OnInit {

  @Input() community!: Partial<Community>
  @Input() domains: Partial<Domain>[] = []
  @Input() admin = false
  @Input() validation!: ValidationState
  selfRegEnabled = false
  ssoEnabled = false
  emailEnabled = false
  selfRegOptionsCollapsed = false
  userPermissionsCollapsed = false
  selfRegDefaultOrganisationSelectionConfig!: MultiSelectConfig<Organisation>

  domainSelectionConfig: MultiSelectConfig<Domain> = {
    name: "domainChoice",
    singleSelection: true,
    singleSelectionClearable: true,
    singleSelectionPersistent: true,
    showAsFormControl: true,
    textField: "fname",
    filterLabel: "-- Optional --",
    loader: () => of((this.domains as Domain[]))
  }

  constructor(
    public readonly dataService: DataService,
    private readonly routingService: RoutingService,
    private readonly organisationService: OrganisationService,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) { super() }

  ngOnInit(): void {
    if (this.community.id != undefined) {
      // Update case.
      if (this.community.selfRegType != Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED) {
        // If not support we want the self-reg options to be collapsed to avoid a double expand if selected.
        // The options are anyway not displayed as the whole self-reg block is hidden.
        this.selfRegOptionsCollapsed = true
      }
      this.userPermissionsCollapsed = true
    }
    this.selfRegEnabled = this.dataService.configuration.registrationEnabled
    this.ssoEnabled = this.dataService.configuration.ssoEnabled
    this.emailEnabled = this.dataService.configuration.emailEnabled
    this.community.selfRegEnabled = this.community.selfRegType != Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED
    this.community.selfRegTokenEnabled = this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN
    if (this.community.selfRegToken == undefined) {
      this.community.selfRegToken = this.dataService.generateApiKeyValue()
    }
    this.community.selfRegInstructionsEnabled = this.community.selfRegTokenHelpText != undefined && this.community.selfRegTokenHelpText.length > 0
    this.community.sameDescriptionAsDomain = this.community.domain != undefined && !(this.textProvided(this.community.description))
    if (this.community.sameDescriptionAsDomain) {
      this.community.activeDescription = this.community.domain!.description
    } else {
      this.community.activeDescription = this.community.description
    }
    this.selfRegDefaultOrganisationSelectionConfig = {
      name: "selfRegDefaultOrganisation",
      textField: "fname",
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: `Select ${this.dataService.labelOrganisationLower()}...`,
      replaceSelectedItems: new EventEmitter(),
      loader: () => this.organisationService.getOrganisationsByCommunity(this.community.id!)
    }
    if (this.community.selfRegJoinAsAdmin == undefined) {
      this.community.selfRegJoinAsAdmin = true
    }
    if (this.community.selfRegDefaultOrganisation) {
      setTimeout(() => {
        this.selfRegDefaultOrganisationSelectionConfig.replaceSelectedItems!.emit([this.community.selfRegDefaultOrganisation!])
      })
    }
  }

  selfRegEnabledChanged() {
    if (this.community.selfRegEnabled) {
      if (this.community.selfRegTokenEnabled) {
        this.community.selfRegType = Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN
      } else {
        this.community.selfRegType = Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING
      }
    } else {
      this.community.selfRegType = Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED
    }
  }

  selfRegTypeChanged(newValue: number) {
    if (newValue != this.community.selfRegType) {
      if (this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED) {
        // From not supported to supported
        this.community.selfRegEnabled = true
        this.selfRegOptionsCollapsed = false
      } else if (newValue == Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED) {
        // From supported to not supported
        this.community.selfRegEnabled = false
        this.selfRegOptionsCollapsed = false
      } else {
        // One supported type to another
        this.selfRegOptionsCollapsed = false
      }
      this.community.selfRegTokenEnabled = this.community.selfRegEnabled && (newValue == Constants.SELF_REGISTRATION_TYPE.TOKEN || newValue == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN)
    }
  }

  domainChanged() {
    if (this.community.domain?.id != undefined) {
      if (this.community.sameDescriptionAsDomain) {
        this.community.activeDescription = this.community.domain?.description
      }
    } else {
      if (this.community.sameDescriptionAsDomain) {
        this.community.activeDescription = ''
        this.community.sameDescriptionAsDomain = false
      }
    }
  }

  descriptionCheckChanged() {
    if (this.community.sameDescriptionAsDomain) {
      this.community.activeDescription = this.community.domain?.description
    }
  }

  setSameDescription() {
    this.community.sameDescriptionAsDomain = this.community.domainId != undefined && !(this.textProvided(this.community.activeDescription))
    if (this.community.sameDescriptionAsDomain) {
      this.community.activeDescription = this.community.domain?.description
    }
  }

  viewDomain() {
    if (this.community.domain?.id != undefined) {
      this.routingService.toDomain(this.community.domain.id)
    }
  }

  viewSelfRegDefaultOrganisation() {
    if (this.community.id != undefined && this.community.selfRegDefaultOrganisation != undefined) {
      this.routingService.toOrganisationDetails(this.community.id, this.community.selfRegDefaultOrganisation.id)
    }
  }

  selfRegDefaultOrganisationSelected(event: FilterUpdate<Organisation>) {
    if (event.values.active.length != 0) {
      this.community.selfRegDefaultOrganisation = event.values.active[0]
    } else {
      this.community.selfRegDefaultOrganisation = undefined
    }
  }

  updateSelfRegistrationToken() {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the community self-registration token?", "Update", "Cancel").subscribe(() => {
      this.community.selfRegToken = this.dataService.generateApiKeyValue()
    })
  }

  editedSelfRegistrationToken(token: string) {
    this.community.selfRegToken = token
  }

}
