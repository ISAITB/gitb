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
import { Constants } from 'src/app/common/constants';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemConfigurationService } from 'src/app/services/system-configuration.service';
import { Theme } from 'src/app/types/theme';
import { SystemAdministrationTab } from '../system-administration-tab.enum';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { PopupService } from 'src/app/services/popup.service';
import { Observable, of } from 'rxjs';
import { DataService } from 'src/app/services/data.service';
import { BaseThemeFormComponent } from '../base-theme-form.component';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-theme-details',
    templateUrl: './theme-details.component.html',
    styles: [],
    standalone: false
})
export class ThemeDetailsComponent extends BaseThemeFormComponent implements OnInit {

  themeId!: number
  theme!: Theme
  savePending = false
  deletePending = false
  copyPending = false
  initiallyActive!: boolean
  validation = new ValidationState()

  constructor(
    private readonly route: ActivatedRoute,
    private readonly routingService: RoutingService,
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
    this.initiallyActive = this.theme.active
    this.routingService.systemThemeBreadcrumbs(this.themeId, this.theme.key)
  }

  private confirmActiveChange() {
    return this.confirmationDialogService.confirm("Active theme change", "You are about the change the currently active theme. Are you sure you want to proceed?", "Change", "Cancel")
  }

  save() {
    let proceedObservable: Observable<boolean>
    if (this.initiallyActive && !this.theme.active || !this.initiallyActive && this.theme.active) {
      proceedObservable = this.confirmActiveChange()
    } else {
      proceedObservable = of(true)
    }
    proceedObservable.subscribe((proceed) => {
      if (proceed) {
        this.savePending = true
        this.processButtonColors(this.theme)
        this.validation.clearErrors()
        this.systemConfigurationService.updateTheme(this.theme)
        .subscribe((error) => {
          if (this.isErrorDescription(error)) {
            this.validation.applyError(error)
          } else {
            this.popupService.success("Theme updated.")
            if (this.initiallyActive || this.theme.active) {
              this.dataService.refreshCss()
            }
            this.back()
          }
        })
        .add(() => {
          this.savePending = false
        })
      }
    })
  }

  activate() {
    this.confirmActiveChange().subscribe((proceed) => {
      if (proceed) {
        this.savePending = true
        this.systemConfigurationService.activateTheme(this.themeId)
        .subscribe(() => {
          this.popupService.success("Theme activated.")
          this.dataService.refreshCss()
          this.back()
        })
        .add(() => {
          this.savePending = false
        })
      }
    })
  }

  delete() {
    let message: string
    if (this.initiallyActive) {
      message = "This is the currently active theme. Are you sure you want to delete it?"
    } else {
      message = "Are you sure you want to delete this theme?"
    }
    this.confirmationDialogService.confirmedDangerous("Confirm delete", message, "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.systemConfigurationService.deleteTheme(this.themeId)
      .subscribe(() => {
        this.popupService.success("Theme deleted.")
        if (this.initiallyActive) {
          this.dataService.refreshCss()
        }
        this.back()
      })
      .add(() => {
        this.deletePending = false
      })
    })
  }

  copy() {
    this.copyPending = true
    this.routingService.toCreateTheme(this.themeId)
  }

  back() {
    this.routingService.toSystemAdministration(SystemAdministrationTab.themes)
  }

}
