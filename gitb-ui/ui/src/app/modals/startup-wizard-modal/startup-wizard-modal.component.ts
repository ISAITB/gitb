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

import {Component} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {WizardStep} from './wizard-step';
import {StartupWizardOptions} from '../../types/startup-wizard-options';
import {DataService} from '../../services/data.service';
import {SystemConfigurationService} from '../../services/system-configuration.service';
import {PopupService} from '../../services/popup.service';

@Component({
  selector: 'app-startup-wizard-modal',
  standalone: false,
  templateUrl: './startup-wizard-modal.component.html',
  styleUrl: './startup-wizard-modal.component.less'
})
export class StartupWizardModalComponent {

  pending = false
  step = 0
  steps: WizardStep[] = [
    WizardStep.Introduction,
    WizardStep.ImportSamples,
    WizardStep.EnableRestApi,
    WizardStep.CheckForUpdates,
    WizardStep.Finalise
  ]
  options: StartupWizardOptions = {}
  visitedEnd = false
  protected readonly WizardStep = WizardStep;

  constructor(
    private readonly modalInstance: BsModalRef,
    public readonly dataService: DataService,
    private readonly systemConfigurationService: SystemConfigurationService,
    private readonly popupService: PopupService
  ) {
  }

  finish() {
    this.pending = true
    this.systemConfigurationService.completeStartupWizard(this.options).subscribe(() => {
      // Apply settings to current session
      this.dataService.configuration.automationApiEnabled = this.options.enableRestApi === true
      this.dataService.configuration.startupWizardEnabled = false
      this.popupService.success("Configuration completed.")
    }).add(() => {
      this.pending = false
      this.modalInstance.hide()
    })
  }

  stepIncomplete(): boolean {
    switch (this.steps[this.step]) {
      case WizardStep.ImportSamples: return this.options.importSamples == undefined;
      case WizardStep.EnableRestApi: return this.options.enableRestApi == undefined;
      case WizardStep.CheckForUpdates: return this.options.enableSoftwareChecks == undefined;
      default: return false;
    }
  }

  next() {
    this.step = this.step + 1
    if (this.steps[this.step] == WizardStep.Finalise) {
      this.visitedEnd = true
    }
  }

  nextLabel(): string {
    switch (this.steps[this.step]) {
      case WizardStep.Introduction: return "Next - samples";
      case WizardStep.ImportSamples: return "Next - REST API";
      case WizardStep.EnableRestApi: return "Next - updates";
      default: return "Next - feedback";
    }
  }

}
