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
import {ActivatedRoute} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {RoutingService} from 'src/app/services/routing.service';
import {SystemConfigurationService} from 'src/app/services/system-configuration.service';
import {Theme} from 'src/app/types/theme';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {PopupService} from 'src/app/services/popup.service';
import {Observable, of} from 'rxjs';
import {DataService} from 'src/app/services/data.service';
import {BaseThemeFormComponent} from '../base-theme-form.component';
import {ValidationState} from 'src/app/types/validation-state';
import {BreadcrumbType} from 'src/app/types/breadcrumb-type';
import {ErrorDescription} from 'src/app/types/error-description';

@Component({
    selector: 'app-theme-details',
    templateUrl: './theme-details.component.html',
    styleUrls: ['./theme-details.component.less'],
    standalone: false
})
export class ThemeDetailsComponent extends BaseThemeFormComponent implements OnInit {

  themeId!: number
  theme!: Theme
  savePending = false
  activatePending = false
  deletePending = false
  copyPending = false
  validation = new ValidationState()

  constructor(
    private readonly route: ActivatedRoute,
    public readonly routingService: RoutingService,
    private readonly popupService: PopupService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly systemConfigurationService: SystemConfigurationService,
    private readonly dataService: DataService
  ) { super() }

  ngOnInit(): void {
    this.themeId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.THEME_ID))
    this.theme = this.route.snapshot.data['theme'] as Theme
    this.originalPrimaryButtonColor = this.theme.primaryButtonColor
    this.originalSecondaryButtonColor = this.theme.secondaryButtonColor
    this.originalAlertInfoBackgroundColor = this.theme.alertInfoBackgroundColor
    this.routingService.systemThemeBreadcrumbs(this.themeId, this.theme.key)
  }

  anyPending() {
    return this.savePending || this.activatePending || this.deletePending || this.copyPending
  }

  saveDisabled() {
    return this.anyPending() || !this.textProvided(this.theme.key)
  }

  activateDisabled() {
    return this.anyPending() || (this.theme.custom && !this.textProvided(this.theme.key))
  }

  private afterSave() {
    this.originalPrimaryButtonColor = this.theme.primaryButtonColor
    this.originalSecondaryButtonColor = this.theme.secondaryButtonColor
    this.originalAlertInfoBackgroundColor = this.theme.alertInfoBackgroundColor
    this.dataService.breadcrumbUpdate({id: this.themeId, type: BreadcrumbType.theme, label: this.theme.key})
  }

  save() {
    if (this.theme.custom && !this.saveDisabled()) {
      let proceedObservable: Observable<boolean>
      if (this.theme.active) {
        proceedObservable = this.confirmationDialogService.confirm("Confirm update", "You are about to save changes to the currently active theme. Are you sure you want to proceed?", "Save", "Cancel", Constants.BUTTON_ICON.SAVE)
      } else {
        proceedObservable = of(true)
      }
      proceedObservable.subscribe((proceed) => {
        if (proceed) {
          this.savePending = true
          this.processButtonColors(this.theme)
          this.processAlertColors(this.theme)
          this.validation.clearErrors()
          this.systemConfigurationService.updateTheme(this.theme)
          .subscribe((error) => {
            if (this.isErrorDescription(error)) {
              this.validation.applyError(error)
            } else {
              this.popupService.success("Theme updated.")
              if (this.theme.active) {
                this.dataService.refreshCss()
              }
              this.afterSave()
            }
          })
          .add(() => {
            this.savePending = false
          })
        }
      })
    }
  }

  activate() {
    if (!this.activateDisabled()) {
      this.confirmationDialogService.confirm("Confirm activation", "You are about to change the currently active theme. Are you sure you want to proceed?", "Activate", "Cancel", Constants.BUTTON_ICON.ACTIVATE)
      .subscribe((proceed) => {
        if (proceed) {
          this.activatePending = true
          let resultObservable: Observable<ErrorDescription|void>
          if (this.theme.custom) {
            this.processButtonColors(this.theme)
            this.processAlertColors(this.theme)
            this.validation.clearErrors()
            resultObservable = this.systemConfigurationService.updateTheme({...this.theme, active: true})
          } else {
            resultObservable = this.systemConfigurationService.activateTheme(this.themeId)
          }
          resultObservable.subscribe((error) => {
            if (this.isErrorDescription(error)) {
              this.validation.applyError(error)
            } else {
              this.theme.active = true
              this.popupService.success("Theme activated.")
              this.dataService.refreshCss()
              this.afterSave()
            }
          })
          .add(() => {
            this.activatePending = false
          })
        }
      })
    }
  }

  delete() {
    let message: string
    if (this.theme.active) {
      message = "This is the currently active theme. Are you sure you want to delete it?"
    } else {
      message = "Are you sure you want to delete this theme?"
    }
    this.confirmationDialogService.confirmedDangerous("Confirm delete", message, "Delete", "Cancel", Constants.BUTTON_ICON.DELETE)
    .subscribe(() => {
      this.deletePending = true
      this.systemConfigurationService.deleteTheme(this.themeId)
      .subscribe(() => {
        this.popupService.success("Theme deleted.")
        if (this.theme.active) {
          this.dataService.refreshCss()
        }
        this.back()
      })
      .add(() => {
        this.deletePending = false
      })
    })
  }

  back() {
    this.routingService.toSystemAdministration(Constants.TAB.SYSTEM_ADMINISTRATION.THEMES)
  }

}
