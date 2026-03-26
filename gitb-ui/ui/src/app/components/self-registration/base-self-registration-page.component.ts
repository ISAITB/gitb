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

import {BaseComponent} from '../../pages/base-component.component';
import {Constants} from '../../common/constants';
import {SelfRegistrationModel} from '../../types/self-registration-model.type';
import {DataService} from '../../services/data.service';

export abstract class BaseSelfRegistrationPageComponent extends BaseComponent {

  protected constructor(
    protected readonly dataService: DataService
  ) {
    super();
  }

  selfRegDataOk(selfRegData: SelfRegistrationModel) {
    return this.communityDataOk(selfRegData) && this.organisationDataOk(selfRegData) && this.userDataOk(selfRegData)
  }

  private userDataOk(selfRegData: SelfRegistrationModel) {
    return (this.dataService.configuration.ssoEnabled) ||
           (this.textProvided(selfRegData.adminName) && this.textProvided(selfRegData.adminEmail) && this.textProvided(selfRegData.adminPassword))

  }

  private communityDataOk(selfRegData: SelfRegistrationModel) {
    return (selfRegData.selfRegOption?.communityId != undefined) &&
           (selfRegData.selfRegOption.selfRegType != Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN || this.textProvided(selfRegData.selfRegToken))
  }

  private newOrganisationDataOk(selfRegData: SelfRegistrationModel) {
    return (this.textProvided(selfRegData.orgShortName) && this.textProvided(selfRegData.orgFullName)) &&
           (selfRegData.selfRegOption?.forceTemplateSelection === false || (selfRegData.selfRegOption?.templates == undefined || selfRegData.selfRegOption.templates.length == 0) || selfRegData.template != undefined) &&
           (selfRegData.selfRegOption?.forceRequiredProperties === false || this.dataService.customPropertiesValid(selfRegData.selfRegOption?.organisationProperties, true))
  }

  private organisationDataOk(selfRegData: SelfRegistrationModel) {
    return (selfRegData.organisationChoice === 'new' && this.newOrganisationDataOk(selfRegData)) ||
           (selfRegData.organisationChoice === 'token' && this.textProvided(selfRegData.orgToken)) ||
           (selfRegData.organisationChoice === 'default')
  }

}
