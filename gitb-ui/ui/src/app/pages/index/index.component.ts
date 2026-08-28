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
import {forkJoin, Observable, of, Subscription} from 'rxjs';
import {AuthProviderService} from '../../services/auth-provider.service';
import {ContactSupportComponent} from 'src/app/modals/contact-support/contact-support.component';
import {RoutingService} from 'src/app/services/routing.service';
import {MenuItem} from 'src/app/types/menu-item.enum';
import {PopupService} from 'src/app/services/popup.service';
import {HealthCheckService} from '../../services/health-check.service';
import {MessageService} from '../../services/message.service';
import {HealthStatus} from '../../types/health-status';
import {MenuItemStatus} from '../../types/menu-item-status.enum';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Constants} from '../../common/constants';
import {NavigationTarget} from 'src/app/types/navigation-target';
import {MessageComposeService} from '../../services/message-compose.service';

@Component({
    selector: 'app-index',
    templateUrl: './index.component.html',
    styleUrls: ['./index.component.less'],
    standalone: false
})
export class IndexComponent implements OnInit, OnDestroy {

  version?: string
  pageTitle = ''
  logoutInProgress = false
  MenuItem = MenuItem
  loginSubscription?: Subscription
  userLoadSubscription?: Subscription
  logoutSubscription?: Subscription
  logoutCompleteSubscription?: Subscription
  bannerSubscription?: Subscription
  preparingForShutdownSubscription?: Subscription
  closedNotificationsSubscription?: Subscription
  userPassedLogin = false
  prepareForShutdownNotificationId: string|null = null

  constructor(
    public readonly dataService: DataService,
    private readonly userGuideService: UserGuideService,
    private readonly htmlService: HtmlService,
    private readonly legalNoticeService: LegalNoticeService,
    private readonly authProviderService: AuthProviderService,
    private readonly modalService: NgbModal,
    public readonly routingService: RoutingService,
    private readonly popupService: PopupService,
    private readonly healthCheckService: HealthCheckService,
    private readonly messageService: MessageService,
    private readonly messageComposeService: MessageComposeService
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
      this.messageComposeService.clearDraft()
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
    this.preparingForShutdownSubscription = this.dataService.onPreparingForShutdown$.subscribe(() => {
      this.handlePrepareForShutdown()
    })
    this.closedNotificationsSubscription = this.popupService.closedNotifications$.subscribe((notificationId) => {
      if (notificationId != null && this.prepareForShutdownNotificationId != null && notificationId === this.prepareForShutdownNotificationId) {
        // Needed so that if we trigger an error elsewhere that requires the notification to be displayed, we will not ignore it.
        this.prepareForShutdownNotificationId = null
      }
      if (notificationId != null && this.dataService.unreadMessagesNotificationId != null && notificationId === this.dataService.unreadMessagesNotificationId) {
        // Manually dismissed before the user got to "My messages" - MessagesComponent's own close
        // call is a safe no-op afterwards, but this keeps the stashed id from lingering regardless.
        this.dataService.unreadMessagesNotificationId = null
      }
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
    if (this.preparingForShutdownSubscription) this.preparingForShutdownSubscription.unsubscribe()
    if (this.closedNotificationsSubscription) this.closedNotificationsSubscription.unsubscribe()
  }

  handlePostUserLoad(): void {
    // Lifted out of the (former) isSystemAdmin-only guard - the menuItemStatusMap is written for every
    // user by the beforeunload handler in ngOnInit (it now also carries the unread-messages badge, not
    // just the Test Bed admin's service-health badge), so every user needs to restore it on a refresh.
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
      // Both checks are only ever run once, right after a genuine login (not a refresh - see
      // statusLoaded above), and run in parallel rather than one after the other since they are
      // independent. Order between the two resulting popups still matters though: .notifications-
      // container is a bottom-anchored column-reverse flex container, so whichever popup is raised
      // first ends up lowest - the health popup is raised first so the unread-messages popup (if any)
      // sits above it, as specified.
      const health$: Observable<HealthStatus|undefined> = this.dataService.isSystemAdmin ? this.healthCheckService.runPostLoginChecks() : of(undefined)
      forkJoin([health$, this.messageService.hasUnreadMessages()]).subscribe(([health, unread]) => {
        if (health != undefined) {
          switch (health) {
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
        }
        if (unread.unread) {
          this.dataService.updateMenuItemStatus(MenuItem.myMessages, MenuItemStatus.Info)
          // Stashed on DataService (not a local field, unlike prepareForShutdownNotificationId above)
          // so MessagesComponent can close this specific popup - not just clear the badge - when the
          // user visits "My messages".
          this.dataService.unreadMessagesNotificationId = this.popupService.info("You have unread messages.", true)
        }
      })
    }
    this.handlePrepareForShutdown()
  }

  handlePrepareForShutdown(): void {
    if (this.dataService.configuration.preparingForShutdown) {
      if (this.prepareForShutdownNotificationId == null) {
        this.prepareForShutdownNotificationId = this.popupService.warning("The Test Bed is preparing to be shut down and will not accept new test sessions.", true)
      }
    } else {
      if (this.prepareForShutdownNotificationId != null) {
        this.popupService.close(this.prepareForShutdownNotificationId)
        this.prepareForShutdownNotificationId = null
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
		return !this.showContactUs() && (this.dataService.configuration && this.dataService.configuration.surveyEnabled)
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
    this.dataService.setMenuVisibility(!this.dataService.menuVisibility)
  }

  copyExternalLink() {
    this.dataService.copyExternalLink().subscribe((value) => {
      if (value) {
        this.popupService.success("Link copied to clipboard.")
      }
    })
  }

  /**
   * Bound directly in the template (evaluated on every change detection cycle, unlike the previous
   * click handler), so - unlike the imperative toX() methods this mirrors - this must tolerate
   * dataService.community being momentarily undefined (e.g. before it's resolved) rather than
   * relying on a non-null assertion, to avoid breaking rendering of the whole left-hand menu.
   */
  domainManagementTarget(): NavigationTarget {
    const domainId = this.dataService.community?.domainId
    if (this.dataService.isSystemAdmin || domainId == undefined) {
      return this.routingService.linkToDomains()
    } else {
      return this.routingService.linkToDomain(domainId)
    }
  }

  communityManagementTarget(): NavigationTarget {
    if (this.dataService.isSystemAdmin) {
      return this.routingService.linkToUserManagement()
    } else {
      return this.routingService.linkToCommunity(this.dataService.community?.id ?? -1)
    }
  }

  protected readonly Constants = Constants;
}
