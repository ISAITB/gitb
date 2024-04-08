import { Component, OnDestroy, OnInit } from '@angular/core';
import { DataService } from '../../services/data.service'
import { UserGuideService } from '../../services/user-guide.service'
import { HtmlService } from '../../services/html.service';
import { LegalNoticeService } from '../../services/legal-notice.service';
import { Observable, Subscription } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { AuthProviderService } from '../../services/auth-provider.service'
import { CookieService } from 'ngx-cookie-service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ContactSupportComponent } from 'src/app/modals/contact-support/contact-support.component';
import { RoutingService } from 'src/app/services/routing.service';
import { MenuItem } from 'src/app/types/menu-item.enum';

@Component({
  selector: 'app-index',
  templateUrl: './index.component.html',
  styleUrls: ['./index.component.less']
})
export class IndexComponent implements OnInit, OnDestroy {

  version?: string
  pageTitle = ''
  menuExpanded = false
  logoutInProgress = false
  MenuItem = MenuItem
  logoutSubscription?: Subscription
  logoutCompleteSubscription?: Subscription
  bannerSubscription?: Subscription

  constructor(
    public dataService: DataService,
    private userGuideService: UserGuideService,
    private htmlService: HtmlService,
    private legalNoticeService: LegalNoticeService,
    private authProviderService: AuthProviderService,
    private cookieService: CookieService,
    private modalService: BsModalService,
    public routingService: RoutingService
  ) {}

  ngOnInit(): void {
    this.logoutInProgress = false
    this.bannerSubscription = this.dataService.onBannerChange$.subscribe((newBanner) => {
      setTimeout(() => {
        this.pageTitle = newBanner
      }, 1)
    })
    this.version = this.dataService.configuration.versionNumber
    this.logoutSubscription = this.authProviderService.onLogout$.subscribe(() => {
      this.logoutInProgress = true
    })
    this.logoutCompleteSubscription = this.authProviderService.onLogoutComplete$.subscribe(() => {
      this.logoutInProgress = false
    })    
  }

  ngOnDestroy(): void {
    if (this.logoutSubscription) this.logoutSubscription.unsubscribe()
    if (this.bannerSubscription) this.bannerSubscription.unsubscribe()
  }

	switchAccount() {
    this.cookieService.set(Constants.LOGIN_OPTION_COOKIE_KEY, Constants.LOGIN_OPTION.FORCE_CHOICE)
    this.authProviderService.signalLogout({full: false, keepLoginOption: true})
  }

	logout() {
    this.authProviderService.signalLogout({full: true})
  }

	userLoaded(): boolean {
		return this.dataService.user !== undefined && this.dataService.user.name != undefined
  }

	userFullyLoaded(): boolean {
    return this.userLoaded() && this.dataService.vendor != undefined
  }

  showContactUs(): boolean {
    return this.dataService.configuration && this.dataService.configuration.emailEnabled && this.dataService.configuration.emailContactFormEnabled
  }

  showMoreInfo(): boolean {
    return this.dataService.configuration && this.dataService.configuration.moreInfoEnabled
  }

  showReleaseInfo(): boolean {
    return this.dataService.configuration && this.dataService.configuration.releaseInfoEnabled
  }

  contactUs() {
    this.modalService.show(ContactSupportComponent, {
      class: 'modal-lg'
    })
  }

  showProvideFeedback(): boolean {
		return !this.showContactUs() && (this.dataService.configuration && this.dataService.configuration.surveyEnabled == true)
  }

  provideFeedbackLink(): string {
    return this.dataService.configuration.surveyAddress
  }

  moreInfoLink(): string {
    return this.dataService.configuration.moreInfoAddress
  }

  releaseInfoLink(): string {
    return this.dataService.configuration.releaseInfoAddress
  }

  userGuideLink() {
		let link = this.userGuideService.userGuideLink()
		window.open(link, '_blank')    
  }

  showUserGuide():boolean {
		return this.dataService.configuration != undefined
  }

  onLegalNotice() {
		let vendor = this.dataService.vendor
		if (vendor != undefined && vendor.legalNotices) {
			this.showLegalNotice(vendor.legalNotices.content!)
    } else {
      let response: Observable<any>
			if (vendor) {
				let communityId = vendor.community
				response = this.legalNoticeService.getCommunityDefaultLegalNotice(communityId)
      } else {
        response = this.legalNoticeService.getTestBedDefaultLegalNotice()
      }
      response.subscribe((data) => {
				if (data.exists == true) {
          this.showLegalNotice(data.content)
        }
      })
    }
  }

  showLegalNotice(html: string): void {
    this.htmlService.showHtml('Legal Notice', html)
  }

  isAuthenticated(): boolean {
    return this.authProviderService.isAuthenticated()
  }

  toggleMenu() {
    this.menuExpanded = !this.menuExpanded
  }

}
