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

import {Component, OnInit} from '@angular/core';
import {RoutingService} from 'src/app/services/routing.service';
import {Theme} from 'src/app/types/theme';
import {ActivatedRoute} from '@angular/router';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {SystemConfigurationService} from 'src/app/services/system-configuration.service';
import {PopupService} from 'src/app/services/popup.service';
import {DataService} from 'src/app/services/data.service';
import {BaseThemeFormComponent} from '../base-theme-form.component';
import {ValidationState} from 'src/app/types/validation-state';
import {Constants} from '../../../../common/constants';

@Component({
    selector: 'app-create-theme',
    templateUrl: './create-theme.component.html',
    styles: [],
    standalone: false
})
export class CreateThemeComponent extends BaseThemeFormComponent implements OnInit {

  theme!: Theme
  referenceThemeId!: number
  savePending = false
  activatePending = false
  validation = new ValidationState()

  constructor(
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly systemConfigurationService: SystemConfigurationService,
    private readonly popupService: PopupService,
    private readonly dataService: DataService
  ) { super() }

  ngOnInit(): void {
    const referenceTheme = this.route.snapshot.data['theme'] as Theme
    this.referenceThemeId = referenceTheme.id
    this.theme = {
      id: -1,
      active: false,
      custom: true,
      key: '',
      separatorTitleColor: referenceTheme.separatorTitleColor,
      cardTitleColor: referenceTheme.cardTitleColor,
      faviconPath: referenceTheme.faviconPath,
      footerBackgroundColor: referenceTheme.footerBackgroundColor,
      footerBorderColor: referenceTheme.footerBorderColor,
      footerLogoDisplay: referenceTheme.footerLogoDisplay,
      footerLogoPath: referenceTheme.footerLogoPath,
      footerTextColor: referenceTheme.footerTextColor,
      headerBackgroundColor: referenceTheme.headerBackgroundColor,
      headerBorderColor: referenceTheme.headerBorderColor,
      headerSeparatorColor: referenceTheme.headerSeparatorColor,
      headerLogoPath: referenceTheme.headerLogoPath,
      headingColor: referenceTheme.headingColor,
      modalTitleColor: referenceTheme.modalTitleColor,
      pageTitleColor: referenceTheme.pageTitleColor,
      tabLinkColor: referenceTheme.tabLinkColor,
      tableTitleColor: referenceTheme.tableTitleColor,
      primaryButtonColor: referenceTheme.primaryButtonColor,
      primaryButtonLabelColor: referenceTheme.primaryButtonLabelColor,
      primaryButtonHoverColor: referenceTheme.primaryButtonHoverColor,
      primaryButtonActiveColor: referenceTheme.primaryButtonActiveColor,
      secondaryButtonColor: referenceTheme.secondaryButtonColor,
      secondaryButtonLabelColor: referenceTheme.secondaryButtonLabelColor,
      secondaryButtonHoverColor: referenceTheme.secondaryButtonHoverColor,
      secondaryButtonActiveColor: referenceTheme.secondaryButtonActiveColor,
      welcomeLoginColor: referenceTheme.welcomeLoginColor,
      welcomeLoginLabelColor: referenceTheme.welcomeLoginLabelColor,
      welcomeOptionLabelColor: referenceTheme.welcomeOptionLabelColor,
      alertInfoBackgroundColor: referenceTheme.alertInfoBackgroundColor,
      alertInfoTextColor: referenceTheme.alertInfoTextColor,
      alertInfoBorderColor: referenceTheme.alertInfoBorderColor
    }
    this.originalPrimaryButtonColor = this.theme.primaryButtonColor
    this.originalSecondaryButtonColor = this.theme.secondaryButtonColor
    this.originalAlertInfoBackgroundColor = this.theme.alertInfoBackgroundColor
    this.routingService.systemConfigurationBreadcrumbs()
  }

  saveDisabled() {
    return this.savePending || this.activatePending || !this.textProvided(this.theme.key)
  }

  save() {
    if (!this.saveDisabled()) {
      this.savePending = true
      this.processButtonColors(this.theme)
      this.processAlertColors(this.theme)
      this.validation.clearErrors()
      this.systemConfigurationService.createTheme(this.theme, this.referenceThemeId)
        .subscribe((error) => {
          if (this.isErrorDescription(error)) {
            this.validation.applyError(error)
          } else {
            this.popupService.success("Theme created.")
            this.back()
          }
        })
        .add(() => {
          this.savePending = false
        })
    }
  }

  activate() {
    if (!this.saveDisabled()) {
      this.confirmationDialogService.confirm("Confirm activation", "You are about to change the currently active theme. Are you sure you want to proceed?", "Activate", "Cancel", Constants.BUTTON_ICON.ACTIVATE)
      .subscribe((proceed) => {
        if (proceed) {
          this.activatePending = true
          this.processButtonColors(this.theme)
          this.processAlertColors(this.theme)
          this.validation.clearErrors()
          this.systemConfigurationService.createTheme({...this.theme, active: true}, this.referenceThemeId)
            .subscribe((error) => {
              if (this.isErrorDescription(error)) {
                this.validation.applyError(error)
              } else {
                this.popupService.success("Theme created and activated.")
                this.dataService.refreshCss()
                this.back()
              }
            })
            .add(() => {
              this.activatePending = false
            })
        }
      })
    }
  }

  back() {
    this.routingService.toSystemAdministration(Constants.TAB.SYSTEM_ADMINISTRATION.THEMES)
  }

}
