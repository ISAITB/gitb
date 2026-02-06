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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {DataService} from '../../services/data.service';
import {UserGuideService} from '../../services/user-guide.service';
import {HtmlService} from '../../services/html.service';
import {LegalNoticeService} from '../../services/legal-notice.service';
import {Observable, Subscription} from 'rxjs';
import {AuthProviderService} from '../../services/auth-provider.service';
import {ContactSupportComponent} from 'src/app/modals/contact-support/contact-support.component';
import {RoutingService} from 'src/app/services/routing.service';
import {MenuItem} from 'src/app/types/menu-item.enum';
import {PopupService} from 'src/app/services/popup.service';
import {HealthCheckService} from '../../services/health-check.service';
import {HealthStatus} from '../../types/health-status';
import {MenuItemStatus} from '../../types/menu-item-status.enum';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-index',
    templateUrl: './index.component.html',
    styleUrls: ['./index.component.less'],
    standalone: false
})
export class IndexComponent implements OnInit, OnDestroy {

  version?: string
  pageTitle = ''
  menuExpanded = false
  logoutInProgress = false
  MenuItem = MenuItem
  loginSubscription?: Subscription
  userLoadSubscription?: Subscription
  logoutSubscription?: Subscription
  logoutCompleteSubscription?: Subscription
  bannerSubscription?: Subscription
  menuVisibilitySubscription?: Subscription
  userPassedLogin = false

  constructor(
    public readonly dataService: DataService,
    private readonly userGuideService: UserGuideService,
    private readonly htmlService: HtmlService,
    private readonly legalNoticeService: LegalNoticeService,
    private readonly authProviderService: AuthProviderService,
    private readonly modalService: NgbModal,
    public readonly routingService: RoutingService,
    private readonly popupService: PopupService,
    private readonly healthCheckService: HealthCheckService
  ) {}

  ngOnInit(): void {
    this.logoutInProgress = false
    this.menuExpanded = this.dataService.menuVisibility
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
    this.loginSubscription = this.authProviderService.afterLogin$.subscribe(() => {
      this.userPassedLogin = true
    })
    this.userLoadSubscription = this.dataService.onUserLoaded$.subscribe(() => {
      this.handlePostUserLoad()
    })
    this.menuVisibilitySubscription = this.dataService.onMenuVisibilityChange$.subscribe((visible) => {
      setTimeout(() => {
        this.menuExpanded = visible
      })
    })
    if (sessionStorage) {
      window.addEventListener("beforeunload", () => {
        sessionStorage.setItem("menuItemStatusMap", JSON.stringify(Array.from(this.dataService.getMenuItemStatusMap())))
      })
    }
  }

  ngOnDestroy(): void {
    if (this.loginSubscription) this.loginSubscription.unsubscribe()
    if (this.userLoadSubscription) this.userLoadSubscription.unsubscribe()
    if (this.logoutSubscription) this.logoutSubscription.unsubscribe()
    if (this.bannerSubscription) this.bannerSubscription.unsubscribe()
    if (this.menuVisibilitySubscription) this.menuVisibilitySubscription.unsubscribe()
  }

  handlePostUserLoad(): void {
    if (this.dataService.isSystemAdmin) {
      let statusLoaded = false
      if (sessionStorage) {
        if (!this.userPassedLogin) {
          // This is a refresh
          const serialisedStatusMap = sessionStorage.getItem("menuItemStatusMap")
          if (serialisedStatusMap) {
            const statusMap = new Map<MenuItem, MenuItemStatus>(JSON.parse(serialisedStatusMap))
            statusMap.forEach((value, key) => {
              this.dataService.updateMenuItemStatus(key, value)
            })
            statusLoaded = true
          }
        }
        sessionStorage.removeItem("menuItemStatusMap")
      }
      if (!statusLoaded) {
        this.healthCheckService.runPostLoginChecks().subscribe((status) => {
          switch (status) {
            case HealthStatus.ERROR:
              this.dataService.updateMenuItemStatus(MenuItem.serviceHealthDashboard, MenuItemStatus.Error)
              this.popupService.error("Service health errors reported.<br/>Check the health dashboard for details.", true)
              break;
            case HealthStatus.WARNING:
              this.dataService.updateMenuItemStatus(MenuItem.serviceHealthDashboard, MenuItemStatus.Warning)
              this.popupService.warning("Service health warnings reported.<br/>Check the health dashboard for details.", true)
              break;
            default:
              this.dataService.updateMenuItemStatus(MenuItem.serviceHealthDashboard, MenuItemStatus.None)
          }
        })
      }
    }
  }

  showRestApi(): boolean {
    return this.dataService.configuration && this.dataService.configuration.automationApiEnabled
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
    this.modalService.open(ContactSupportComponent, { size: 'lg' })
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

  showLegalNotice():boolean {
		let vendor = this.dataService.vendor
		if (vendor != undefined && (vendor.legalNotices || vendor.communityLegalNoticeAppliesAndExists)) {
      return true
    } else {
      return this.dataService.configuration?.hasDefaultLegalNotice
    }
  }

  onLegalNotice() {
		let vendor = this.dataService.vendor
		if (vendor != undefined && vendor.legalNotices) {
			this.doShowLegalNotice(vendor.legalNotices.content!)
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
          this.doShowLegalNotice(data.content)
        }
      })
    }
  }

  doShowLegalNotice(html: string): void {
    this.htmlService.showHtml('Legal Notice', html)
  }

  isAuthenticated(): boolean {
    return this.authProviderService.isAuthenticated()
  }

  toggleMenu() {
    this.dataService.setMenuVisibility(!this.menuExpanded)
  }

  copyExternalLink() {
    this.dataService.copyExternalLink().subscribe((value) => {
      if (value) {
        this.popupService.success("Link copied to clipboard.")
      }
    })
  }

  toDomainManagement() {
    if (this.dataService.isSystemAdmin || this.dataService.community!.domainId == undefined) {
      return this.routingService.toDomains()
    } else {
      return this.routingService.toDomain(this.dataService.community!.domainId!)
    }
  }

}
