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

import {Injectable} from '@angular/core';
import {DataService} from './data.service';
import {UsageTipModalComponent} from '../modals/usage-tip-modal/usage-tip-modal.component';
import {BsModalService} from 'ngx-bootstrap/modal';
import {UsageTipsConfiguration} from '../types/usage-tips-configuration';
import {Constants} from '../common/constants';
import {SystemConfigurationService} from './system-configuration.service';

@Injectable({
  providedIn: 'root'
})
export class UsageTipService {

  constructor(
    private readonly dataService: DataService,
    private readonly modalService: BsModalService,
    private readonly systemConfigurationService: SystemConfigurationService
  ) {}

  showUsageTip(tip: number) {
    if (this.dataService.configuration.usageTipsEnabled && !this.dataService.configuration.usageTipsDisabledForScreens.includes(tip)) {
      this.modalService.show(UsageTipModalComponent, {
        class: 'modal-lg',
        initialState: {
          tip: tip
        },
        backdrop: 'static'
      })
    }
  }

  disableUsageTip(tip: number) {
    if (!this.dataService.configuration.usageTipsDisabledForScreens.includes(tip)) {
      this.dataService.configuration.usageTipsDisabledForScreens.push(tip)
    }
    const configValue: UsageTipsConfiguration = {
      enabled: this.dataService.configuration.usageTipsEnabled,
      disabledForScreens: this.dataService.configuration.usageTipsDisabledForScreens
    }
    return this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.USAGE_TIPS, JSON.stringify(configValue))
  }

  disableUsageTips() {
    const configValue: UsageTipsConfiguration = {
      enabled: false,
      disabledForScreens: []
    }
    this.dataService.configuration.usageTipsEnabled = configValue.enabled
    this.dataService.configuration.usageTipsDisabledForScreens = configValue.disabledForScreens
    return this.systemConfigurationService.updateConfigurationValue(Constants.SYSTEM_CONFIG.USAGE_TIPS, JSON.stringify(configValue))
  }

}
