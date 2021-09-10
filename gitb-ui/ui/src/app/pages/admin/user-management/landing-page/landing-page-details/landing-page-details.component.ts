import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { LandingPageService } from 'src/app/services/landing-page.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { LandingPage } from 'src/app/types/landing-page';

@Component({
  selector: 'app-landing-page-details',
  templateUrl: './landing-page-details.component.html',
  styles: [
  ]
})
export class LandingPageDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  pageId!: number
  isDefault = false
  savePending = false
  deletePending = false
  copyPending = false
  page: Partial<LandingPage> = {}

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    public dataService: DataService,
    private landingPageService: LandingPageService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    this.pageId = Number(this.route.snapshot.paramMap.get('page_id'))
    this.landingPageService.getLandingPageById(this.pageId)
    .subscribe((data) => {
      this.page = data
      this.isDefault = data.default
    })
  }

  saveDisabled() {
    return !this.textProvided(this.page.name) || !this.textProvided(this.page.content)
  }

  updateLandingPage(copy: boolean) {
    this.clearAlerts()
    if (!this.isDefault && this.page.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default landing page. Are you sure?", "Yes", "No")
      .subscribe(() => {
        this.doUpdate(copy)
      })
    } else {
      this.doUpdate(copy)
    }
  }

  doUpdate(copy: boolean) {
    this.savePending = true
    this.landingPageService.updateLandingPage(this.pageId, this.page.name!, this.page.description, this.page.content, this.page.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.addAlertError(data.error_description)
      } else {
        if (copy) {
          this.copyLandingPage()
        } else {
          this.popupService.success('Landing page updated.')
        }
      }
    }).add(() => {
      this.savePending = false
    })
  }

  copyLandingPage() {
    this.copyPending = true
    this.routingService.toCreateLandingPage(this.communityId, false, this.pageId)
  }

  deleteLandingPage() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this landing page?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.landingPageService.deleteLandingPage(this.pageId)
      .subscribe(() => {
        this.cancelDetailLandingPage()
        this.popupService.success('Landing page deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailLandingPage() {
    this.routingService.toCommunity(this.communityId)
  }

}
