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

import {Component, Input, OnInit} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {DataService} from 'src/app/services/data.service';
import {Community} from 'src/app/types/community';
import {Domain} from 'src/app/types/domain';
import {IdLabel} from 'src/app/types/id-label';
import {RoutingService} from 'src/app/services/routing.service';
import {ValidationState} from 'src/app/types/validation-state';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';
import {of} from 'rxjs';

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
  selfRegTypes: IdLabel[] = [
    {id: Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED, label: 'Not supported'},
    {id: Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING, label: 'Select from public communities'},
    {id: Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN, label: 'Select from public communities and provide token'}
  ]
  selfRegRestrictions: IdLabel[] = []
  selfRegOptionsVisible = false
  selfRegOptionsCollapsed = false
  userPermissionsCollapsed = false

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
    private readonly routingService: RoutingService
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
    if (this.ssoEnabled) {
      this.selfRegRestrictions = [
        {id: Constants.SELF_REGISTRATION_RESTRICTION.NO_RESTRICTION, label: 'No restrictions'},
        {id: Constants.SELF_REGISTRATION_RESTRICTION.USER_EMAIL, label: 'One registration allowed per user'},
        {id: Constants.SELF_REGISTRATION_RESTRICTION.USER_EMAIL_DOMAIN, label: 'One registration allowed per user email domain'}
      ]
    }
    this.selfRegOptionsVisible = this.community.selfRegType != Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED
    this.community.sameDescriptionAsDomain = this.community.domain != undefined && !(this.textProvided(this.community.description))
    if (this.community.sameDescriptionAsDomain) {
      this.community.activeDescription = this.community.domain!.description
    } else {
      this.community.activeDescription = this.community.description
    }
  }

  selfRegTypeChanged(newValue: number) {
    if (newValue != this.community.selfRegType) {
      if (this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED) {
        // From not supported to supported
        this.selfRegOptionsVisible = true
        this.selfRegOptionsCollapsed = false
      } else if (newValue == Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED) {
        // From supported to not supported
        this.selfRegOptionsVisible = false
        this.selfRegOptionsCollapsed = false
      } else {
        // One supported type to another
        this.selfRegOptionsCollapsed = false
      }
      if (this.selfRegOptionsVisible && !this.selfRegOptionsCollapsed && (newValue == Constants.SELF_REGISTRATION_TYPE.TOKEN || newValue == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN)) {
        this.dataService.focus('selfRegToken', 200)
      }
    }
  }

  domainChanged() {
    if (this.community.domainId != undefined) {
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

}
