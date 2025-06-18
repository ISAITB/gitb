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
import { LegalNoticeService } from 'src/app/services/legal-notice.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { LegalNotice } from 'src/app/types/legal-notice';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { Constants } from 'src/app/common/constants';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { HtmlService } from 'src/app/services/html.service';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-legal-notice',
    templateUrl: './create-legal-notice.component.html',
    styles: [],
    standalone: false
})
export class CreateLegalNoticeComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  notice: Partial<LegalNotice> = {
    default: false
  }
  savePending = false
  tooltipForDefaultCheck!: string
  validation = new ValidationState()
  Constants = Constants

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private legalNoticeService: LegalNoticeService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    public dataService: DataService,
    private htmlService: HtmlService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this legal notice the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this legal notice the default one assumed for all communities.'
    }
    const base = this.route.snapshot.data['base'] as LegalNotice|undefined
    if (base != undefined) {
      this.notice.name = base.name
      this.notice.description = base.description
      this.notice.content = base.content
    }
  }

  saveDisabled() {
    return !this.textProvided(this.notice.name) || !this.textProvided(this.notice.content)
  }

  createLegalNotice() {
    if (this.notice.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default legal notice. Are you sure?", "Change", "Cancel")
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
    this.legalNoticeService.createLegalNotice(this.notice.name!, this.notice.description, this.notice.content, this.notice.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        this.cancelCreateLegalNotice()
        this.popupService.success('Legal notice created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateLegalNotice() {
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
