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

import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorTemplateService } from 'src/app/services/error-template.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { ErrorData } from 'src/app/types/error-data.type';
import { ErrorTemplate } from 'src/app/types/error-template';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { KeyValue } from 'src/app/types/key-value';
import { Constants } from 'src/app/common/constants';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-error-template',
    templateUrl: './create-error-template.component.html',
    styles: [],
    standalone: false
})
export class CreateErrorTemplateComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  template: Partial<ErrorTemplate> = {
    default: false
  }
  placeholders: KeyValue[] = []
  savePending = false
  tooltipForDefaultCheck!: string
  validation = new ValidationState()
  Constants = Constants

  constructor(
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly errorTemplateService: ErrorTemplateService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly popupService: PopupService,
    public readonly dataService: DataService,
    private readonly errorService: ErrorService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this error template the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this error template the default one assumed for all communities.'
    }
    this.placeholders = [
      { key: Constants.PLACEHOLDER__ERROR_DESCRIPTION, value: "The error message text (a text value that may be empty)." },
      { key: Constants.PLACEHOLDER__ERROR_ID, value: "The error identifier (used to trace the error in the logs)." }
    ]
    const base = this.route.snapshot.data['base'] as ErrorTemplate|undefined
    if (base != undefined) {
      this.template.name = base.name
      this.template.description = base.description
      this.template.content = base.content
    }
  }

  saveDisabled() {
    return !this.textProvided(this.template.name) || !this.textProvided(this.template.content)
  }

  createErrorTemplate() {
    if (this.template.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default error template. Are you sure?", "Change", "Cancel")
      .subscribe(() => {
        this.doCreate()
      })
    } else {
      this.doCreate()
    }
  }

  doCreate() {
    this.validation.clearErrors()
    this.savePending = true
    this.errorTemplateService.createErrorTemplate(this.template.name!, this.template.description, this.template.content, this.template.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        this.cancelCreateErrorTemplate()
        this.popupService.success('Error template created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateErrorTemplate() {
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toSystemAdministration(SystemAdministrationTab.errorTemplates)
    } else {
      this.routingService.toCommunity(this.communityId, CommunityTab.errorTemplates)
    }
  }

preview() {
    const error: ErrorData = {
      statusText: 'Internal server error',
      error: {
        error_description: 'This is a sample error description message.',
        error_id: '0123456789'
      },
      template: this.template.content
    }
    this.errorService.showErrorMessage(error)
  }

}
