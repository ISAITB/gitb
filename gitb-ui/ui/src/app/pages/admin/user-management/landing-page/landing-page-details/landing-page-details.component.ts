import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { LandingPageService } from 'src/app/services/landing-page.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { LandingPage } from 'src/app/types/landing-page';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { Constants } from 'src/app/common/constants';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { BsModalService } from 'ngx-bootstrap/modal';
import { PreviewLandingPageComponent } from '../preview-landing-page/preview-landing-page.component';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-landing-page-details',
    templateUrl: './landing-page-details.component.html',
    standalone: false
})
export class LandingPageDetailsComponent extends BaseComponent implements OnInit {

  communityId!: number
  pageId!: number
  isDefault = false
  savePending = false
  deletePending = false
  copyPending = false
  page: Partial<LandingPage> = {}
  tooltipForDefaultCheck!: string
  validation = new ValidationState()
  loaded = false
  Constants = Constants

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    public dataService: DataService,
    private landingPageService: LandingPageService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    private modalService: BsModalService
  ) { super() }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this landing page the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this landing page the default one assumed for all communities.'
    }
    this.pageId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.LANDING_PAGE_ID))
    this.landingPageService.getLandingPageById(this.pageId)
    .subscribe((data) => {
      this.page = data
      this.isDefault = data.default
      if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
        this.routingService.systemLandingPageBreadcrumbs(this.pageId, this.page.name!)
      } else {
        this.routingService.landingPageBreadcrumbs(this.communityId, this.pageId, this.page.name!)
      }
    }).add(() => {
      this.loaded = true
    })
  }

  saveDisabled() {
    return !this.loaded || !this.textProvided(this.page.name) || !this.textProvided(this.page.content)
  }

  updateLandingPage(copy: boolean) {
    if (!this.isDefault && this.page.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default landing page. Are you sure?", "Change", "Cancel")
      .subscribe(() => {
        this.doUpdate(copy)
      })
    } else {
      this.doUpdate(copy)
    }
  }

  private clearCachedLandingPageIfNeeded() {
    if ((this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) &&
        (
          (this.dataService.vendor!.landingPage == this.page.id) ||
          (
            this.dataService.vendor!.landingPage == undefined &&
            this.dataService.vendor!.community == this.communityId &&
            this.page.default
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
    this.landingPageService.updateLandingPage(this.pageId, this.page.name!, this.page.description, this.page.content, this.page.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        if (copy) {
          this.copyLandingPage()
        } else {
          this.clearCachedLandingPageIfNeeded()
          this.popupService.success('Landing page updated.')
          if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
            this.dataService.breadcrumbUpdate({id: this.pageId, type: BreadcrumbType.systemLandingPage, label: this.page.name})
          } else {
            this.dataService.breadcrumbUpdate({id: this.pageId, type: BreadcrumbType.landingPage, label: this.page.name})
          }
        }
      }
    }).add(() => {
      this.savePending = false
    })
  }

  copyLandingPage() {
    this.copyPending = true
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toCreateLandingPage(undefined, undefined, this.pageId)
    } else {
      this.routingService.toCreateLandingPage(this.communityId, false, this.pageId)
    }
  }

  deleteLandingPage() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this landing page?", "Delete", "Cancel")
    .subscribe(() => {
      this.validation.clearErrors()
      this.deletePending = true
      this.landingPageService.deleteLandingPage(this.pageId)
      .subscribe(() => {
        this.clearCachedLandingPageIfNeeded()
        this.cancelDetailLandingPage()
        this.popupService.success('Landing page deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailLandingPage() {
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toSystemAdministration(SystemAdministrationTab.landingPages)
    } else {
      this.routingService.toCommunity(this.communityId, CommunityTab.landingPages)
    }
  }

  preview() {
    this.modalService.show(PreviewLandingPageComponent, {
      initialState: {
        previewContent: this.page.content!
      }
    })
  }

}
