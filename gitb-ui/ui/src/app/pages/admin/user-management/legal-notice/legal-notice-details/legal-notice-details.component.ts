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

@Component({
  selector: 'app-legal-notice-details',
  templateUrl: './legal-notice-details.component.html',
  styles: [
  ]
})
export class LegalNoticeDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  noticeId!: number
  notice: Partial<LegalNotice> = {}
  isDefault = false
  savePending = false
  copyPending = false
  deletePending = false
  showContent = true
  tooltipForDefaultCheck!: string

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private legalNoticeService: LegalNoticeService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService
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
    this.noticeId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.LEGAL_NOTICE_ID))
    this.legalNoticeService.getLegalNoticeById(this.noticeId)
    .subscribe((data) => {
      this.notice = data
      this.isDefault = data.default
    })
  }

  saveDisabled() {
    return !this.textProvided(this.notice.name) || !this.textProvided(this.notice.content)
  }

  updateLegalNotice(copy: boolean) {
    if (!this.isDefault && this.notice.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default legal notice. Are you sure?", "Yes", "No")
      .subscribe(() => {
        this.doUpdate(copy)
      })
    } else {
      this.doUpdate(copy)
    }
  }

  doUpdate(copy: boolean) {
    this.savePending = true
    this.legalNoticeService.updateLegalNotice(this.noticeId, this.notice.name!, this.notice.description, this.notice.content, this.notice.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.addAlertError(data.error_description)
      } else {
        if (copy) {
          this.copyLegalNotice()
        } else {
          this.popupService.success('Legal notice updated.')
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
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this legal notice?", "Yes", "No")
    .subscribe(() => {
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

}
