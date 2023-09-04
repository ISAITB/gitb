import { AfterViewInit, Component, OnInit } from '@angular/core';
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

@Component({
  selector: 'app-create-landing-page',
  templateUrl: './create-landing-page.component.html',
  styles: [
  ]
})
export class CreateLandingPageComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  page: Partial<LandingPage> = {
    default: false
  }
  savePending = false
  showContent = true
  tooltipForDefaultCheck!: string

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private landingPageService: LandingPageService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    public dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this landing page the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this landing page the default one assumed for all communities.'
    }
    const base = this.route.snapshot.data['base'] as LandingPage|undefined
    if (base != undefined) {
      this.page.name = base.name
      this.page.description = base.description
      this.page.content = base.content
    }
  }

  saveDisabled() {
    return !this.textProvided(this.page.name) || !this.textProvided(this.page.content)
  }

  createLandingPage() {
    this.clearAlerts()
    if (this.page.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default landing page. Are you sure?", "Yes", "No")
      .subscribe(() => {
        this.doCreate()
      })
    } else {
      this.doCreate()
    }
  }

  private clearCachedLandingPageIfNeeded() {
    if ((this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) 
      && this.dataService.vendor!.community == this.communityId && this.page.default) {
        // Update if we are Test Bed or community admins and we are editing the default landing page.
        this.dataService.currentLandingPageContent = undefined
    }
  }

  doCreate() {
    this.savePending = true
    this.landingPageService.createLandingPage(this.page.name!, this.page.description, this.page.content, this.page.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.addAlertError(data.error_description)
      } else {
        this.clearCachedLandingPageIfNeeded()
        this.cancelCreateLandingPage()
        this.popupService.success('Landing page created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateLandingPage() {
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toSystemAdministration(SystemAdministrationTab.landingPages)
    } else {
      this.routingService.toCommunity(this.communityId, CommunityTab.landingPages)
    }
  }

}
