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

import {ChangeDetectionStrategy, Component, OnInit} from '@angular/core';
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
import {BreadcrumbType} from 'src/app/types/breadcrumb-type';
import {ValidationState} from 'src/app/types/validation-state';
import {Constants} from 'src/app/common/constants';
import {CustomContentTypes} from 'src/app/common/custom-content-types';
import {PreviewLandingPageComponent} from '../preview-landing-page/preview-landing-page.component';

@Component({
    selector: 'app-custom-content-details',
    templateUrl: './custom-content-details.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class CustomContentDetailsComponent extends BaseComponent implements OnInit {

  communityId!: number
  contentId!: number
  type = 0
  typeLabel = 'Custom content'
  typeLabelLower = 'custom content'
  isDefault = false
  savePending = false
  deletePending = false
  copyPending = false
  content: Partial<CustomContent> = {}
  placeholders: KeyValue[] = []
  tooltipForDefaultCheck = ''
  validation = new ValidationState()
  loaded = false
  Constants = Constants

  constructor(
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    public readonly dataService: DataService,
    private readonly customContentService: CustomContentService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly popupService: PopupService,
    private readonly modalService: NgbModal,
    private readonly errorService: ErrorService,
    private readonly htmlService: HtmlService
  ) { super() }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
    }
    // The type is known from the route.
    this.setType(Number(this.route.snapshot.data['contentType']))
    this.contentId = Number(this.route.snapshot.paramMap.get(CustomContentTypes.pathParam(this.type)))
    this.customContentService.getCustomContentById(this.contentId)
    .subscribe((data) => {
      this.content = data
      this.isDefault = data.default
      if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
        this.tooltipForDefaultCheck = 'Check this to make this '+this.typeLabelLower+' the default one assumed for all communities.'
        this.routingService.systemCustomContentBreadcrumbs(this.type, this.contentId, this.content.name!)
      } else {
        this.tooltipForDefaultCheck = 'Check this to make this '+this.typeLabelLower+' the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
        this.routingService.customContentBreadcrumbs(this.type, this.communityId, this.contentId, this.content.name!)
      }
      if (this.type == Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE) {
        this.placeholders = [
          { key: Constants.PLACEHOLDER__ERROR_DESCRIPTION, value: "The error message text (a text value that may be empty)." },
          { key: Constants.PLACEHOLDER__ERROR_ID, value: "The error identifier (used to trace the error in the logs)." }
        ]
      }
    }).add(() => {
      this.loaded = true
    })
  }

  private setType(type: number) {
    this.type = type
    this.typeLabel = CustomContentTypes.label(type)
    this.typeLabelLower = CustomContentTypes.labelLower(type)
  }

  saveDisabled() {
    return !this.loaded || this.savePending || !this.textProvided(this.content.name) || !this.textProvided(this.content.content)
  }

  updateCustomContent(copy: boolean) {
    if (!this.saveDisabled()) {
      if (!this.isDefault && this.content.default) {
        this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default "+this.typeLabelLower+". Are you sure?", "Change", "Cancel", Constants.BUTTON_ICON.SAVE)
          .subscribe(() => {
            this.doUpdate(copy)
          })
      } else {
        this.doUpdate(copy)
      }
    }
  }

  private clearCachedLandingPageIfNeeded() {
    if (this.type == Constants.CUSTOM_CONTENT_TYPE.LANDING_PAGE && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) &&
        (
          (this.dataService.vendor!.landingPage == this.content.id) ||
          (
            this.dataService.vendor!.landingPage == undefined &&
            this.dataService.vendor!.community == this.communityId &&
            this.content.default
          )
        )
      ) {
        // Update if we are Test Bed or community admins and we are editing the default landing page.
        this.dataService.currentLandingPageContent = undefined
    }
  }

  doUpdate(copy: boolean) {
    this.validation.clearErrors()
    this.savePending = true
    this.customContentService.updateCustomContent(this.contentId, this.content.name!, this.content.description, this.content.content, this.content.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        if (copy) {
          this.copyCustomContent()
        } else {
          this.clearCachedLandingPageIfNeeded()
          this.popupService.success(this.typeLabel+' updated.')
          if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
            this.dataService.breadcrumbUpdate({id: this.contentId, type: BreadcrumbType.systemCustomContent, label: this.content.name})
          } else {
            this.dataService.breadcrumbUpdate({id: this.contentId, type: BreadcrumbType.customContent, label: this.content.name})
          }
        }
      }
    }).add(() => {
      this.savePending = false
    })
  }

  copyCustomContent() {
    this.copyPending = true
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      void this.routingService.toCreateCustomContent(this.type, undefined, undefined, this.contentId)
    } else {
      void this.routingService.toCreateCustomContent(this.type, this.communityId, false, this.contentId)
    }
  }

  deleteCustomContent() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+this.typeLabelLower+"?", "Delete", "Cancel", Constants.BUTTON_ICON.DELETE)
    .subscribe(() => {
      this.validation.clearErrors()
      this.deletePending = true
      this.customContentService.deleteCustomContent(this.contentId)
      .subscribe(() => {
        this.clearCachedLandingPageIfNeeded()
        this.cancelDetailCustomContent()
        this.popupService.success(this.typeLabel+' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailCustomContent() {
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
