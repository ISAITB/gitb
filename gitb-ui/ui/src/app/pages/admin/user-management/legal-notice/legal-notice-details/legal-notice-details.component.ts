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
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { LegalNoticeService } from 'src/app/services/legal-notice.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { LegalNotice } from 'src/app/types/legal-notice';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { Constants } from 'src/app/common/constants';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { HtmlService } from 'src/app/services/html.service';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-legal-notice-details',
    templateUrl: './legal-notice-details.component.html',
    standalone: false
})
export class LegalNoticeDetailsComponent extends BaseComponent implements OnInit {

  communityId!: number
  noticeId!: number
  notice: Partial<LegalNotice> = {}
  isDefault = false
  savePending = false
  copyPending = false
  deletePending = false
  tooltipForDefaultCheck!: string
  validation = new ValidationState()
  loaded = false
  Constants = Constants

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private legalNoticeService: LegalNoticeService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    private htmlService: HtmlService
    ) { super() }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this legal notice the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this legal notice the default one assumed for all communities.'
    }
    this.noticeId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.LEGAL_NOTICE_ID))
    this.legalNoticeService.getLegalNoticeById(this.noticeId)
    .subscribe((data) => {
      this.notice = data
      this.isDefault = data.default
      if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
        this.routingService.systemLegalNoticeBreadcrumbs(this.noticeId, this.notice.name!)
      } else {
        this.routingService.legalNoticeBreadcrumbs(this.communityId, this.noticeId, this.notice.name!)
      }
    }).add(() => {
      this.loaded = true
    })
  }

  saveDisabled() {
    return !this.loaded || !this.textProvided(this.notice.name) || !this.textProvided(this.notice.content)
  }

  updateLegalNotice(copy: boolean) {
    if (!this.isDefault && this.notice.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default legal notice. Are you sure?", "Change", "Cancel")
      .subscribe(() => {
        this.doUpdate(copy)
      })
    } else {
      this.doUpdate(copy)
    }
  }

  doUpdate(copy: boolean) {
    this.validation.clearErrors()
    this.savePending = true
    this.legalNoticeService.updateLegalNotice(this.noticeId, this.notice.name!, this.notice.description, this.notice.content, this.notice.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        if (copy) {
          this.copyLegalNotice()
        } else {
          this.popupService.success('Legal notice updated.')
          if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
            this.dataService.breadcrumbUpdate({id: this.noticeId, type: BreadcrumbType.systemLegalNotice, label: this.notice.name})
          } else {
            this.dataService.breadcrumbUpdate({id: this.noticeId, type: BreadcrumbType.legalNotice, label: this.notice.name})
          }
        }
      }
    }).add(() => {
      this.savePending = false
    })
  }

  copyLegalNotice() {
    this.copyPending = true
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toCreateLegalNotice(undefined, undefined, this.noticeId)
    } else {
      this.routingService.toCreateLegalNotice(this.communityId, false, this.noticeId)
    }
  }

  deleteLegalNotice() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this legal notice?", "Delete", "Cancel")
    .subscribe(() => {
      this.validation.clearErrors()
      this.deletePending = true
      this.legalNoticeService.deleteLegalNotice(this.noticeId)
      .subscribe(() => {
        this.cancelDetailLegalNotice()
        this.popupService.success('Legal notice deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailLegalNotice() {
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toSystemAdministration(SystemAdministrationTab.legalNotices)
    } else {
      this.routingService.toCommunity(this.communityId, CommunityTab.legalNotices)
    }
  }

  preview() {
    this.htmlService.showHtml('Legal Notice', this.notice.content!)
  }

}
