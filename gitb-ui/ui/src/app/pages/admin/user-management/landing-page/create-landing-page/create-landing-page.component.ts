import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { LandingPageService } from 'src/app/services/landing-page.service';
import { PopupService } from 'src/app/services/popup.service';
import { LandingPage } from 'src/app/types/landing-page';

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

  constructor(
    private router: Router,
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
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
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

  doCreate() {
    this.savePending = true
    this.landingPageService.createLandingPage(this.page.name!, this.page.description, this.page.content, this.page.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.addAlertError(data.error_description)
      } else {
        this.cancelCreateLandingPage()
        this.popupService.success('Landing page created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateLandingPage() {
    this.router.navigate(['admin', 'users', 'community', this.communityId])
  }

}
