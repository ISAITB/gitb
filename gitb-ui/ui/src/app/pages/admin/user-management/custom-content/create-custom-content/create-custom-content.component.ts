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

import {AfterViewInit, ChangeDetectionStrategy, Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {CustomContentService} from 'src/app/services/custom-content.service';
import {DataService} from 'src/app/services/data.service';
import {ErrorService} from 'src/app/services/error.service';
import {HtmlService} from 'src/app/services/html.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {CustomContent} from 'src/app/types/custom-content';
import {ErrorData} from 'src/app/types/error-data.type';
import {KeyValue} from 'src/app/types/key-value';
import {ValidationState} from 'src/app/types/validation-state';
import {Constants} from 'src/app/common/constants';
import {CustomContentTypes} from 'src/app/common/custom-content-types';
import {PreviewLandingPageComponent} from '../preview-landing-page/preview-landing-page.component';

@Component({
    selector: 'app-create-custom-content',
    templateUrl: './create-custom-content.component.html',
    styles: [],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class CreateCustomContentComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  type!: number
  typeLabel!: string
  typeLabelLower!: string
  content: Partial<CustomContent> = {
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
    private readonly customContentService: CustomContentService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly popupService: PopupService,
    public readonly dataService: DataService,
    private readonly modalService: NgbModal,
    private readonly errorService: ErrorService,
    private readonly htmlService: HtmlService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.type = Number(this.route.snapshot.data['contentType'])
    this.typeLabel = CustomContentTypes.label(this.type)
    this.typeLabelLower = CustomContentTypes.labelLower(this.type)
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this '+this.typeLabelLower+' the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this '+this.typeLabelLower+' the default one assumed for all communities.'
    }
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.systemConfigurationBreadcrumbs()
    } else {
      this.routingService.communityChildBreadcrumbs(this.communityId)
    }
    if (this.type == Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE) {
      this.placeholders = [
        { key: Constants.PLACEHOLDER__ERROR_DESCRIPTION, value: "The error message text (a text value that may be empty)." },
        { key: Constants.PLACEHOLDER__ERROR_ID, value: "The error identifier (used to trace the error in the logs)." }
      ]
    }
    const base = this.route.snapshot.data['base'] as CustomContent|undefined
    if (base != undefined) {
      this.content.name = base.name
      this.content.description = base.description
      this.content.content = base.content
    }
  }

  saveDisabled() {
    return this.savePending || !this.textProvided(this.content.name) || !this.textProvided(this.content.content)
  }

  createCustomContent() {
    if (!this.saveDisabled()) {
      if (this.content.default) {
        this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default "+this.typeLabelLower+". Are you sure?", "Change", "Cancel", Constants.BUTTON_ICON.SAVE)
          .subscribe(() => {
            this.doCreate()
          })
      } else {
        this.doCreate()
      }
    }
  }

  private clearCachedLandingPageIfNeeded() {
    if (this.type == Constants.CUSTOM_CONTENT_TYPE.LANDING_PAGE
      && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)
      && this.dataService.vendor!.community == this.communityId && this.content.default) {
        // Update if we are Test Bed or community admins and we are editing the default landing page.
        this.dataService.currentLandingPageContent = undefined
    }
  }

  doCreate() {
    this.validation.clearErrors()
    this.savePending = true
    this.customContentService.createCustomContent(this.type, this.content.name!, this.content.description, this.content.content, this.content.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        this.clearCachedLandingPageIfNeeded()
        this.cancelCreateCustomContent()
        this.popupService.success(this.typeLabel+' created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateCustomContent() {
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      void this.routingService.toSystemAdministration(Constants.TAB.SYSTEM_ADMINISTRATION.CUSTOM_CONTENT)
    } else {
      void this.routingService.toCommunity(this.communityId, Constants.TAB.COMMUNITY.CUSTOM_CONTENT)
    }
  }

  preview() {
    if (this.type == Constants.CUSTOM_CONTENT_TYPE.LANDING_PAGE) {
      const modal = this.modalService.open(PreviewLandingPageComponent, { modalDialogClass: 'landingPagePreview' })
      const modalInstance = modal.componentInstance as PreviewLandingPageComponent
      modalInstance.previewContent = this.content.content!
    } else if (this.type == Constants.CUSTOM_CONTENT_TYPE.LEGAL_NOTICE) {
      this.htmlService.showHtml('Legal Notice', this.content.content!)
    } else {
      const error: ErrorData = {
        statusText: 'Internal server error',
        error: {
          error_description: 'This is a sample error description message.',
          error_id: '0123456789'
        },
        template: this.content.content
      }
      this.errorService.showErrorMessage(error)
    }
  }

}
