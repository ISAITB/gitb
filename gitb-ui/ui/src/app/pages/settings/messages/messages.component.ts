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

import { AfterViewInit, Component, ElementRef, HostListener, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { formatDate, Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription, forkJoin, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseComponent } from '../../base-component.component';
import { Constants } from '../../../common/constants';
import { DataService } from '../../../services/data.service';
import { MessageService } from '../../../services/message.service';
import { MessageComposeService } from '../../../services/message-compose.service';
import { RoutingService } from '../../../services/routing.service';
import { ConfirmationDialogService } from '../../../services/confirmation-dialog.service';
import { PopupService } from '../../../services/popup.service';
import { OrganisationService } from '../../../services/organisation.service';
import { CommunityService } from '../../../services/community.service';
import { MessageRowView } from '../../../types/message-row-view';
import { MessageDetailView } from '../../../types/message-detail-view';
import { ReceivedMessage } from '../../../types/received-message';
import { SentMessage } from '../../../types/sent-message';
import { MessageTarget } from '../../../types/message-target';
import { RecipientOption } from '../../../types/recipient-option';
import { ReplyTargetInfo } from '../../../types/reply-target-info';
import { MessageChainItem } from '../../../types/message-chain-item';
import { MenuItem } from '../../../types/menu-item.enum';
import { MenuItemStatus } from '../../../types/menu-item-status.enum';
import { DateRange } from '../../../components/date-range/date-range';
import { CheckboxOption } from '../../../components/checkbox-option-panel/checkbox-option';
import { CheckboxOptionState } from '../../../components/checkbox-option-panel/checkbox-option-state';
import { CheckboxOptionPanelComponent } from '../../../components/checkbox-option-panel/checkbox-option-panel.component';
import { MessageTableComponent } from '../../../components/message-table/message-table.component';
import { PagingEvent } from '../../../components/paging-controls/paging-event';
import { MultiSelectConfig } from '../../../components/multi-select-filter/multi-select-config';
import { FilterUpdate } from '../../../components/test-filter/filter-update';
import { SplitViewComponent } from '../../../components/split-view/split-view.component';

@Component({
  selector: 'app-messages',
  standalone: false,
  templateUrl: './messages.component.html',
  styleUrl: './messages.component.less'
})
export class MessagesComponent extends BaseComponent implements OnInit, AfterViewInit, OnDestroy {

  sentView = false
  messages: MessageRowView[] = []
  selectedDetail?: MessageDetailView
  selectedChain: MessageChainItem[] = []
  detailLoading = false
  detailActionPending = false
  loadingStatus = { status: Constants.STATUS.PENDING }
  contentRefreshing = false
  deletePending = false
  markReadPending = false
  markUnreadPending = false
  replyPending = false

  filterText?: string
  showRead = true
  showUnread = true
  showImportant = false
  dateRange: DateRange = {}

  sortColumn = 'date'
  sortOrder: 'asc'|'desc' = 'desc'
  currentPage = 1

  statusOptions: CheckboxOption[][] = []
  sentStatusOptions: CheckboxOption[][] = []
  controlsWrapped = false
  dividerTop = 0

  isOrganisationUser = false
  isCommunityAdminRole = false
  isTestBedAdminRole = false
  peerFilterTargets: MessageTarget[] = []
  peerFilterConfig?: MultiSelectConfig<RecipientOption>
  peerFilterConfig1?: MultiSelectConfig<RecipientOption>
  peerFilterConfig2?: MultiSelectConfig<RecipientOption>
  peerFilterShowStage2 = false

  // Seeded from the user's persisted messagesSplitView preference in ngOnInit; toggleSplitView() writes
  // it back via DataService.setMessagesSplitView so it survives reloads/logins (see account.service.ts).
  splitView = false
  private static readonly FALLBACK_MIN_TABLE_HEIGHT = 120

  @ViewChild('showMessagesFilter') showMessagesFilter?: CheckboxOptionPanelComponent
  @ViewChild('messageTable') messageTable?: MessageTableComponent
  @ViewChild('messagesPage') messagesPage?: ElementRef
  @ViewChild('controlsContainer') controlsContainerRef?: ElementRef
  @ViewChild('searchControls') searchControlsRef?: ElementRef
  @ViewChild('actionControls') actionControlsRef?: ElementRef
  @ViewChild('tableArea') tableAreaRef?: ElementRef
  @ViewChild('splitViewComponent') splitViewComponent?: SplitViewComponent

  private messageSentSubscription?: Subscription
  private resizeObserver!: ResizeObserver

  constructor(
    protected readonly dataService: DataService,
    private readonly messageService: MessageService,
    protected readonly messageComposeService: MessageComposeService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly popupService: PopupService,
    private readonly organisationService: OrganisationService,
    private readonly communityService: CommunityService,
    private readonly routingService: RoutingService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly location: Location,
    private readonly zone: NgZone
  ) { super() }

  ngOnInit(): void {
    this.routingService.myMessagesBreadcrumbs()
    this.sentView = this.route.snapshot.queryParamMap.get('sent') === 'true'
    // load()'s finish() (below) already refreshes app-split-view once data has loaded, which covers the
    // initial render whether splitView starts true (from the persisted preference) or false.
    this.splitView = this.dataService.messagesSplitView
    this.statusOptions = [
      [
        { key: 'read', label: 'Read messages', default: true, iconClass: Constants.BUTTON_ICON.MESSAGE_READ+' fa-fw' },
        { key: 'unread', label: 'Unread messages', default: true, iconClass: Constants.BUTTON_ICON.MESSAGE_UNREAD+' fa-fw' }
      ],
      [
        { key: 'important', label: 'Only important messages', default: false, iconClass: Constants.BUTTON_ICON.MESSAGE_IMPORTANT+' fa-fw' }
      ]
    ]
    this.sentStatusOptions = [
      [
        { key: 'important', label: 'Only important messages', default: false, iconClass: Constants.BUTTON_ICON.MESSAGE_IMPORTANT+' fa-fw' }
      ]
    ]
    this.isTestBedAdminRole = this.dataService.isSystemAdmin
    this.isCommunityAdminRole = this.dataService.isCommunityAdmin
    this.isOrganisationUser = !this.isTestBedAdminRole && !this.isCommunityAdminRole
    // Opening the screen unconditionally clears the post-login unread-messages badge AND, if it's still
    // open, its popup notification (see IndexComponent.handlePostUserLoad, which created it and stashed
    // its id on DataService since these are two different components) - by design there is no re-check
    // against actual unread state here or anywhere else on this page; both simply reappear at the next
    // login if unread messages still exist.
    this.dataService.updateMenuItemStatus(MenuItem.myMessages, MenuItemStatus.None)
    if (this.dataService.unreadMessagesNotificationId != null) {
      this.popupService.close(this.dataService.unreadMessagesNotificationId)
      this.dataService.unreadMessagesNotificationId = null
    }
    this.setupPeerFilter()
    this.messageSentSubscription = this.messageComposeService.onMessageSent.subscribe(() => this.load(1))
    this.load(1)
  }

  ngAfterViewInit(): void {
    // Observes the whole page container, not .controls itself - see BaseConformanceItemDisplayComponent's
    // identical approach. Observing .controls directly would create a self-referential feedback loop: its
    // own height changes (border/margin/padding differences) whenever the wrapped class toggles, which
    // would immediately re-trigger the observer with stale/mid-reflow geometry, causing the wrapped state
    // to never settle back to "not wrapped" once triggered by a real resize.
    this.resizeObserver = new ResizeObserver(() => {
      this.zone.run(() => this.calculateWrapping())
    })
    if (this.messagesPage) {
      this.resizeObserver.observe(this.messagesPage.nativeElement)
    }
    setTimeout(() => this.splitViewComponent?.refresh())
  }

  ngOnDestroy(): void {
    this.messageSentSubscription?.unsubscribe()
    if (this.resizeObserver && this.messagesPage) {
      this.resizeObserver.unobserve(this.messagesPage.nativeElement)
    }
  }

  private calculateWrapping() {
    if (this.searchControlsRef && this.actionControlsRef) {
      const searchRect = this.searchControlsRef.nativeElement.getBoundingClientRect()
      const actionRect = this.actionControlsRef.nativeElement.getBoundingClientRect()
      this.controlsWrapped = searchRect.top != actionRect.top
      if (this.controlsWrapped && this.controlsContainerRef) {
        // Computed fresh on every pass from the actual rendered geometry (not from the wrapped CSS
        // class's own styling) - see the comment on .controls-divider in the stylesheet for why this
        // has to stay decoupled from calculateWrapping()'s own comparison above.
        this.dividerTop = searchRect.bottom - this.controlsContainerRef.nativeElement.getBoundingClientRect().top
      }
    } else {
      this.controlsWrapped = false
    }
  }

  /** Document-relative Y that app-split-view's secondary pane should stop at - bound to its
   * [bottomBoundary] input as a stable arrow function (see the field below) rather than computed once and
   * written imperatively: app-split-view re-reads this on every recalculation (window resize included),
   * since unlike the old per-page bottomOffset this now depends on where the page menu/footer actually
   * ended up, which does change with viewport height. */
  private contentBottom(): number {
    if (!this.messagesPage) return window.innerHeight
    const pageEl: HTMLElement = this.messagesPage.nativeElement
    const cardBodyEl = pageEl.querySelector('.card-body') as HTMLElement | null
    const cardEl = pageEl.querySelector('.card') as HTMLElement | null
    // The card body's own bottom padding (below its last child) plus the card's own border - not just
    // the padding, which alone left the card a few pixels past the footer's top edge.
    const cardBottomChrome = (cardBodyEl ? (parseFloat(getComputedStyle(cardBodyEl).paddingBottom) || 0) : 0)
      + (cardEl ? (parseFloat(getComputedStyle(cardEl).borderBottomWidth) || 0) : 0)
    // .page-root (IndexComponent's own wrapper around the routed page, see index.component.less) carries
    // its own margin-bottom below our card - easy to miss since it's outside this component entirely. Not
    // sticky itself, so its own document-relative top is stable regardless of scroll position (see
    // app-split-view's class comment for why that matters).
    const pageRootEl = pageEl.closest('.page-root') as HTMLElement | null
    const pageRootBottomMargin = pageRootEl ? (parseFloat(getComputedStyle(pageRootEl).marginBottom) || 0) : 0
    const pageRootDocTop = pageRootEl ? (pageRootEl.getBoundingClientRect().top + window.scrollY) : 0
    // .page.index is a flex column (header-bar / .child / .footer-bar, see app.less) with .child set to
    // flex:1 - so while page-root's content is shorter than the space available to .child, flex-grow
    // stretches .child (with blank filler below page-root) to reach exactly down to the viewport bottom,
    // and .footer-bar sits flush after it. But .page-menu (the left sidebar, see index.component.less) can
    // be taller than the viewport by itself, in which case page-root's own content now needs more room than
    // that available space, and .child's height instead follows page-root's own (page-root's own
    // margin-bottom included) - pushing .footer-bar down below the fold. Taking the two possible positions'
    // max (rather than just measuring .footer-bar's own current rect, which is exactly the self-referential
    // measurement that produced the old shrink-loop: the footer's rect depends on how tall .page-content
    // currently is, which is what this component is about to compute) gives the desired footer position
    // independent of this component's own panes - "desired" because, as covered next, our own card is what
    // actually determines which of the two applies once it's sized to reach it.
    const footerEl = document.querySelector('.footer-bar') as HTMLElement | null
    const footerHeight = footerEl ? footerEl.getBoundingClientRect().height : 0
    // .page-menu is position:sticky, so its own rect only reflects a viewport-relative position while
    // scrolled into view - offsetHeight (a layout size, not a position) is used instead.
    const pageMenuEl = pageRootEl?.querySelector('.page-menu') as HTMLElement | null
    const menuHeight = pageMenuEl ? pageMenuEl.offsetHeight : 0
    const desiredFooterTop = Math.max(window.innerHeight - footerHeight, pageRootDocTop + menuHeight + pageRootBottomMargin)
    // page-content (page-root's other child, wrapping our own card) has align-self:stretch by default, so
    // whichever of page-menu/page-content ends up taller *before* stretch is what actually determines
    // page-root's own height - and since our own card is what we're about to size to reach desiredFooterTop,
    // page-content (not page-menu) ends up being that taller sibling as soon as our own target exceeds
    // menuHeight, which it does as soon as there's any meaningful vertical budget to work with. In other
    // words, our own card's height is what determines page-root's (and so page-menu's max() aside,
    // .footer-bar's) *true* position from this point on - so reaching desiredFooterTop exactly means sizing
    // our card to leave room for pageRootBottomMargin *again* on top of cardBottomChrome (once for page-root
    // itself, once for the gap from page-root's own border-box to .child's), not just once.
    return desiredFooterTop - cardBottomChrome - pageRootBottomMargin
  }

  // Stable reference for the [bottomBoundary] template binding - app-split-view calls this itself on
  // every recalculation rather than being handed a single pre-computed number, so a bound method (not
  // a bound property re-evaluated by Angular's own change detection) is what avoids NG0100 here.
  contentBottomProvider = () => this.contentBottom()

  toggleSplitView() {
    this.dataService.setMessagesSplitView(this.splitView)
    this.updateMinTableHeightAndRefresh()
  }

  /** The table's drag-up floor (its header plus one row), passed into app-split-view's
   * [minPrimaryHeight] - falls back to a fixed constant while the table is empty/loading (neither
   * element is rendered yet). Recomputed after every load() and split-view toggle since app-split-view
   * has no way to introspect what its projected primary content actually looks like; deferred a tick so
   * .table-area (present unconditionally) and its rows (present once split view is toggled on) have
   * actually rendered with their new geometry before being measured. */
  private updateMinTableHeightAndRefresh() {
    setTimeout(() => {
      if (!this.splitViewComponent) return
      const tableAreaEl: HTMLElement | undefined = this.tableAreaRef?.nativeElement
      const thead = tableAreaEl?.querySelector('thead')
      const row = tableAreaEl?.querySelector('tbody tr')
      this.splitViewComponent.minPrimaryHeight = (thead && row) ? thead.getBoundingClientRect().height + row.getBoundingClientRect().height : MessagesComponent.FALLBACK_MIN_TABLE_HEIGHT
      this.splitViewComponent.refresh()
    })
  }

  @HostListener('document:click', ['$event'])
  documentClick(event: Event) {
    this.showMessagesFilter?.documentClick(event)
  }

  @HostListener('document:keyup.escape')
  documentEscape() {
    this.showMessagesFilter?.documentEscape()
  }

  newMessage() {
    this.messageComposeService.openNew()
  }

  refresh() {
    this.load(this.currentPage)
  }

  toggleView() {
    this.filterText = undefined
    this.dateRange = {}
    this.sortColumn = 'date'
    this.sortOrder = 'desc'
    this.setupPeerFilter()
    // Ensures a refresh reopens on the same (received/sent) view rather than always resetting to received.
    const urlTree = this.router.createUrlTree([], { queryParams: { sent: this.sentView ? 'true' : null }, queryParamsHandling: 'merge' })
    this.location.replaceState(this.router.serializeUrl(urlTree))
    this.load(1)
  }

  applyTextFilter() {
    this.load(1)
  }

  filterByStatus(choices: CheckboxOptionState) {
    if (!this.sentView) {
      this.showRead = choices['read']
      this.showUnread = choices['unread']
    }
    this.showImportant = choices['important']
    this.load(1)
  }

  dateRangeChanged() {
    this.load(1)
  }

  private get peerFilterLabel(): string {
    return this.sentView ? 'To...' : 'From...'
  }

  private setupPeerFilter() {
    this.peerFilterTargets = []
    this.peerFilterShowStage2 = false
    if (this.isOrganisationUser) {
      this.setupOrganisationUserPeerFilter()
    } else if (this.isCommunityAdminRole) {
      this.setupCommunityAdminPeerFilter()
    } else if (this.isTestBedAdminRole) {
      this.setupTestBedAdminPeerFilter()
    }
  }

  private setupOrganisationUserPeerFilter() {
    const options: RecipientOption[] = [
      { id: -1, fname: 'Community administrator', targetType: Constants.MESSAGE_TARGET_TYPE.COMMUNITY_ADMIN },
      { id: -2, fname: 'My '+this.dataService.labelOrganisationLower(), targetType: Constants.MESSAGE_TARGET_TYPE.OWN_ORGANISATION }
    ]
    this.peerFilterConfig = {
      name: 'peerFilter',
      textField: 'fname',
      countLabel: this.sentView ? 'recipients' : 'senders',
      maxWidth: 300,
      filterLabel: this.peerFilterLabel,
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'None available.',
      searchPlaceholder: 'Search '+(this.sentView ? 'recipients' : 'senders')+'...',
      loader: () => of(options)
    }
  }

  private setupCommunityAdminPeerFilter() {
    const communityId = this.dataService.vendor!.community
    this.peerFilterConfig = {
      name: 'peerFilter',
      textField: 'fname',
      countLabel: this.sentView ? 'recipients' : 'senders',
      maxWidth: 300,
      filterLabel: this.peerFilterLabel,
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'None available.',
      searchPlaceholder: 'Search '+(this.sentView ? 'recipients' : 'senders')+'...',
      loader: () => this.organisationService.getOrganisationsByCommunity(communityId).pipe(
        map(orgs => {
          const groupOptions: RecipientOption[] = [
            { id: -1, fname: 'Test Bed administrator', targetType: Constants.MESSAGE_TARGET_TYPE.TESTBED_ADMIN },
            { id: -2, fname: 'Community administrator', targetType: Constants.MESSAGE_TARGET_TYPE.COMMUNITY_ADMIN }
          ]
          const orgOptions: RecipientOption[] = orgs
            .filter(o => !o.adminOrganization)
            .sort((a, b) => a.fname.localeCompare(b.fname))
            .map(o => ({ id: o.id, fname: o.fname, targetType: Constants.MESSAGE_TARGET_TYPE.ORGANISATION, organisationId: o.id }))
          return groupOptions.concat(orgOptions)
        })
      ),
      placeholderItemIds: new Set([-2])
    }
  }

  private setupTestBedAdminPeerFilter() {
    this.peerFilterConfig1 = {
      name: 'peerFilter1',
      textField: 'fname',
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      maxWidth: 300,
      filterLabel: this.peerFilterLabel,
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'None available.',
      searchPlaceholder: 'Search '+(this.sentView ? 'recipients' : 'senders')+'...',
      loader: () => this.communityService.getCommunities(undefined, true).pipe(
        map(communities => {
          const communityOptions: RecipientOption[] = communities
            .slice()
            .sort((a, b) => a.fname.localeCompare(b.fname))
            .map(c => ({ id: c.id, fname: c.fname }))
          return ([{ id: -1, fname: 'Test Bed administrator', targetType: Constants.MESSAGE_TARGET_TYPE.TESTBED_ADMIN }] as RecipientOption[]).concat(communityOptions)
        })
      ),
      placeholderItemIds: new Set([-1])
    }
  }

  private setupPeerFilterStage2(communityId: number) {
    this.peerFilterConfig2 = {
      name: 'peerFilter2',
      textField: 'fname',
      countLabel: this.sentView ? 'recipients' : 'senders',
      maxWidth: 300,
      filterLabel: this.peerFilterLabel,
      filterLabelIcon: Constants.BUTTON_ICON.MESSAGE,
      noItemsMessage: 'None available.',
      searchPlaceholder: 'Search '+(this.sentView ? 'recipients' : 'senders')+'...',
      loader: () => this.organisationService.getOrganisationsByCommunity(communityId).pipe(
        map(orgs => {
          const groupOptions: RecipientOption[] = [
            { id: -1, fname: 'Community administrator', targetType: Constants.MESSAGE_TARGET_TYPE.COMMUNITY_ADMIN, communityId: communityId }
          ]
          const orgOptions: RecipientOption[] = orgs
            .filter(o => !o.adminOrganization)
            .sort((a, b) => a.fname.localeCompare(b.fname))
            .map(o => ({ id: o.id, fname: o.fname, targetType: Constants.MESSAGE_TARGET_TYPE.ORGANISATION, communityId: communityId, organisationId: o.id }))
          return groupOptions.concat(orgOptions)
        })
      ),
      placeholderItemIds: new Set([-1])
    }
  }

  peerFilterChanged(event: FilterUpdate<RecipientOption>) {
    this.peerFilterTargets = event.values.active.map(o => ({ targetType: o.targetType!, organisationId: o.organisationId }))
    this.load(1)
  }

  peerFilter1Changed(event: FilterUpdate<RecipientOption>) {
    const selected = event.values.active.length > 0 ? event.values.active[0] : undefined
    if (selected == undefined) {
      this.peerFilterShowStage2 = false
      this.peerFilterTargets = []
    } else if (selected.targetType != undefined) {
      this.peerFilterShowStage2 = false
      this.peerFilterTargets = [{ targetType: selected.targetType }]
    } else {
      this.peerFilterShowStage2 = true
      this.peerFilterTargets = []
      this.setupPeerFilterStage2(selected.id)
    }
    this.load(1)
  }

  peerFilter2Changed(event: FilterUpdate<RecipientOption>) {
    this.peerFilterTargets = event.values.active.map(o => ({ targetType: o.targetType!, communityId: o.communityId, organisationId: o.organisationId }))
    this.load(1)
  }

  sortChanged(event: { column: string, order: 'asc'|'desc' }) {
    this.sortColumn = event.column
    this.sortOrder = event.order
    this.load(1)
  }

  doPaging(event: PagingEvent) {
    this.load(event.targetPage, event.targetPageSize)
  }

  private load(page: number, pageSize?: number) {
    // Whenever the table's data is (re)loaded - paging, sorting, filtering, view toggle, refresh, or a
    // post-delete reload - the selected message may no longer be among the results, so the detail panel
    // is always cleared here rather than only when it's known to be affected.
    this.selectedDetail = undefined
    this.selectedChain = []
    if (this.loadingStatus.status == Constants.STATUS.FINISHED) {
      this.contentRefreshing = true
    } else {
      this.loadingStatus.status = Constants.STATUS.PENDING
    }
    this.currentPage = page
    const limit = pageSize ?? this.dataService.defaultPagingTableSize
    const dateAfter = this.dateRange.start ? this.serializeDate(this.dateRange.start) : undefined
    const dateBefore = this.dateRange.end ? this.serializeDate(this.dateRange.end) : undefined
    const finish = () => {
      this.loadingStatus.status = Constants.STATUS.FINISHED
      this.contentRefreshing = false
      setTimeout(() => this.calculateWrapping())
      this.updateMinTableHeightAndRefresh()
    }
    if (!this.sentView) {
      this.messageService.getReceivedMessages(page, limit, this.filterText, this.showRead, this.showUnread, this.showImportant, dateAfter, dateBefore, this.sortColumn, this.sortOrder, this.peerFilterTargets)
        .subscribe((result) => {
          this.messages = result.data.map((m) => this.toReceivedRow(m))
          this.messageTable?.pagingControls?.updateStatus(page, result.count)
        }).add(finish)
    } else {
      this.messageService.getSentMessages(page, limit, this.filterText, this.showImportant, dateAfter, dateBefore, this.sortColumn, this.sortOrder, this.peerFilterTargets)
        .subscribe((result) => {
          this.messages = result.data.map((m) => this.toSentRow(m))
          this.messageTable?.pagingControls?.updateStatus(page, result.count)
        }).add(finish)
    }
  }

  private serializeDate(d: Date): string {
    return formatDate(d, this.dataService.configuration.dateTimeFormat, 'en')
  }

  private toReceivedRow(m: ReceivedMessage): MessageRowView {
    return { id: m.id, subject: m.subject, bodyPreview: m.bodyPreview, peerName: m.senderName, peerCount: 1, date: m.date, important: m.important, read: m.read, parentMessageId: m.parentMessageId }
  }

  private toSentRow(m: SentMessage): MessageRowView {
    return { id: m.id, subject: m.subject, bodyPreview: m.bodyPreview, peerName: m.recipientName, peerCount: m.recipientCount, date: m.date, important: m.important, parentMessageId: m.parentMessageId }
  }

  selectMessage(row: MessageRowView) {
    if (this.selectedDetail != undefined && this.selectedDetail.id == row.id) {
      // Clicking the already-selected message again deselects it - and since it's already displayed,
      // there is nothing to (re-)fetch either way.
      this.selectedDetail = undefined
      this.selectedChain = []
      return
    }
    this.detailLoading = true
    const detail$: Observable<MessageDetailView> = this.sentView
      ? this.messageService.getSentMessage(row.id).pipe(map((detail) => ({ id: detail.id, subject: detail.subject, body: detail.body, peerName: detail.singleRecipientName ?? '', peerCount: detail.recipientCount, date: detail.date, important: detail.important, parentMessageId: detail.parentMessageId })))
      : this.messageService.getReceivedMessage(row.id).pipe(map((detail) => ({ id: detail.id, subject: detail.subject, body: detail.body, peerName: detail.senderName, peerCount: 1, date: detail.date, important: detail.important, parentMessageId: detail.parentMessageId, read: true })))
    // The chain's starting point (the row's own parentMessageId) is already known from the row itself,
    // so the chain no longer needs to wait for the detail response - both requests are issued together
    // here and the display below is replaced only once both have resolved (no intermediate state where
    // the message is shown before its chain has loaded).
    const chain$: Observable<MessageChainItem[]> = row.parentMessageId == undefined ? of([]) : this.messageService.getMessageChain(row.parentMessageId)
    forkJoin([detail$, chain$]).subscribe(([detail, chain]) => {
      this.selectedDetail = detail
      // Reversed (immediate parent first, root last) - the connecting line runs from the message
      // content above down through the chain, so the item adjacent to the content must be the
      // immediate parent.
      this.selectedChain = chain.map((item) => ({ ...item, collapsed: true })).reverse()
      if (!this.sentView && row.read === false) {
        row.read = true
      }
    }).add(() => { this.detailLoading = false })
  }

  replyToSelected() {
    if (this.selectedDetail == undefined) return
    this.reply(this.selectedDetail.id, this.selectedDetail.subject)
  }

  reply(id: number, subject: string|undefined) {
    const row = this.messages.find((m) => m.id == id)
    const setters: ((v: boolean) => void)[] = row ? [(v) => { row.actionPending = v }] : []
    if (this.selectedDetail != undefined && this.selectedDetail.id == id) { setters.push((v) => { this.detailActionPending = v }) }
    this.startReply(id, subject, setters)
  }

  replyChecked() {
    const checked = this.checkedMessages()
    if (checked.length != 1) return
    const row = checked[0]
    const setters: ((v: boolean) => void)[] = [(v) => { this.replyPending = v }, (v) => { row.actionPending = v }]
    if (this.selectedDetail != undefined && this.selectedDetail.id == row.id) { setters.push((v) => { this.detailActionPending = v }) }
    this.startReply(row.id, row.subject, setters)
  }

  showReply(): boolean {
    return this.checkedMessages().length == 1
  }

  /** Loads everything a reply needs - the earlier chain and the role-appropriate default recipient -
   * before opening the compose modal, so the modal never shows its own loading state (see #5 in the
   * review). The triggering control (row/detail options menu, or the aggregate Reply button) is shown
   * pending for the duration via whichever setters the caller supplies. */
  private startReply(id: number, subject: string|undefined, pendingSetters: ((v: boolean) => void)[]) {
    pendingSetters.forEach((f) => f(true))
    forkJoin([this.messageService.getMessageChain(id), this.messageService.getReplyTarget(id)]).subscribe(([rawChain, target]) => {
      // Reversed (immediate parent first, root last) - the connecting line runs from the body editor
      // above down through the chain, so the item adjacent to the editor must be the immediate parent.
      const chain = rawChain.map((item) => ({ ...item, collapsed: true })).reverse()
      const seed = this.buildReplySeed(target)
      this.messageComposeService.openReply(id, 'RE: '+(subject || ''), chain, seed.recipients, seed.recipientDisplay, seed.adminCommunitySelection)
    }).add(() => pendingSetters.forEach((f) => f(false)))
  }

  /** Maps the backend's role-agnostic reply-target descriptor onto the specific static option ids/
   * labels the replying role's own picker already uses (see setupOrganisationUserPicker /
   * setupCommunityAdminPicker / setupTestBedAdminPicker in ComposeMessageModalComponent) - these ids
   * differ per role (e.g. "Community administrator" is -1 for an organisation user but -2 for a
   * community admin), so they cannot be hardcoded once. recipients mirrors what the corresponding
   * picker's own *Changed handler would compute, since replying now sends exactly like a new message. */
  private buildReplySeed(info?: ReplyTargetInfo): { recipients: MessageTarget[], recipientDisplay: RecipientOption[], adminCommunitySelection?: RecipientOption } {
    const TT = Constants.MESSAGE_TARGET_TYPE
    if (!info || info.targetType == undefined) {
      return { recipients: [], recipientDisplay: [] }
    }
    if (this.isTestBedAdminRole) {
      if (info.targetType == TT.TESTBED_ADMIN) {
        const option: RecipientOption = { id: -1, fname: 'Test Bed administrator', targetType: TT.TESTBED_ADMIN }
        return { recipients: [{ targetType: TT.TESTBED_ADMIN }], recipientDisplay: [], adminCommunitySelection: option }
      }
      const communityEntry: RecipientOption = { id: info.communityId!, fname: info.communityName ?? '' }
      const recipientEntry: RecipientOption = info.targetType == TT.COMMUNITY_ADMIN
        ? { id: -1, fname: 'Community administrator', targetType: TT.COMMUNITY_ADMIN, communityId: info.communityId }
        : { id: info.organisationId!, fname: info.organisationName ?? '', targetType: TT.ORGANISATION, communityId: info.communityId, organisationId: info.organisationId }
      return {
        recipients: [{ targetType: recipientEntry.targetType!, communityId: recipientEntry.communityId, organisationId: recipientEntry.organisationId }],
        recipientDisplay: [recipientEntry],
        adminCommunitySelection: communityEntry
      }
    } else if (this.isCommunityAdminRole) {
      let option: RecipientOption
      if (info.targetType == TT.TESTBED_ADMIN) {
        option = { id: -1, fname: 'Test Bed administrator', targetType: TT.TESTBED_ADMIN }
      } else if (info.targetType == TT.COMMUNITY_ADMIN) {
        option = { id: -2, fname: 'Community administrator', targetType: TT.COMMUNITY_ADMIN }
      } else {
        option = { id: info.organisationId!, fname: info.organisationName ?? '', targetType: TT.ORGANISATION, organisationId: info.organisationId }
      }
      return { recipients: [{ targetType: option.targetType!, organisationId: option.organisationId }], recipientDisplay: [option] }
    } else {
      const option: RecipientOption = info.targetType == TT.OWN_ORGANISATION
        ? { id: -2, fname: 'My '+this.dataService.labelOrganisationLower(), targetType: TT.OWN_ORGANISATION }
        : { id: -1, fname: 'Community administrator', targetType: TT.COMMUNITY_ADMIN }
      return { recipients: [{ targetType: option.targetType!, organisationId: option.organisationId }], recipientDisplay: [option] }
    }
  }

  checkedMessages(): MessageRowView[] {
    return this.messages.filter((m) => m.checked === true)
  }

  hasChecked(): boolean {
    return this.checkedMessages().length > 0
  }

  showMarkRead(): boolean {
    return !this.sentView && this.checkedMessages().some((m) => m.read === false)
  }

  showMarkUnread(): boolean {
    return !this.sentView && this.checkedMessages().some((m) => m.read === true)
  }

  onCheckChange() {
    // Showing/hiding the action toolbar (Reply/Mark read/.../Delete) changes .controls' own height, and
    // therefore app-split-view's document-relative top - nothing else notices that on its own since
    // neither pane's projected content changes size as a result.
    setTimeout(() => { this.calculateWrapping(); this.splitViewComponent?.refresh() })
  }

  markCheckedRead(read: boolean) {
    const checked = this.checkedMessages()
    if (checked.length == 0) return
    const ids = checked.map((m) => m.id)
    if (read) { this.markReadPending = true } else { this.markUnreadPending = true }
    checked.forEach((m) => { m.actionPending = true })
    this.messageService.updateMessageReadStatus(ids, read).subscribe(() => {
      checked.forEach((m) => { m.read = read })
      if (this.selectedDetail != undefined && ids.includes(this.selectedDetail.id)) {
        this.selectedDetail.read = read
      }
    }).add(() => {
      if (read) { this.markReadPending = false } else { this.markUnreadPending = false }
      checked.forEach((m) => { m.actionPending = false })
    })
  }

  markOneRead(event: { id: number, read: boolean }) {
    const row = this.messages.find((m) => m.id == event.id)
    if (row != undefined) { row.actionPending = true }
    if (this.selectedDetail != undefined && this.selectedDetail.id == event.id) { this.detailActionPending = true }
    this.messageService.updateMessageReadStatus([event.id], event.read).subscribe(() => {
      if (row != undefined) {
        row.read = event.read
      }
      if (this.selectedDetail != undefined && this.selectedDetail.id == event.id) {
        this.selectedDetail.read = event.read
      }
    }).add(() => {
      if (row != undefined) { row.actionPending = false }
      if (this.selectedDetail != undefined && this.selectedDetail.id == event.id) { this.detailActionPending = false }
    })
  }

  toggleSelectedRead() {
    if (this.selectedDetail == undefined) return
    this.markOneRead({ id: this.selectedDetail.id, read: !this.selectedDetail.read })
  }

  deleteOne(id: number) {
    this.confirmDeleteAndProceed([id])
  }

  deleteSelected() {
    if (this.selectedDetail == undefined) return
    this.confirmDeleteAndProceed([this.selectedDetail.id])
  }

  deleteChecked() {
    const ids = this.checkedMessages().map((m) => m.id)
    if (ids.length == 0) return
    this.confirmDeleteAndProceed(ids)
  }

  private confirmDeleteAndProceed(ids: number[]) {
    const message = ids.length == 1 ? 'Are you sure you want to delete this message?' : `Are you sure you want to delete these ${ids.length} messages?`
    this.confirmationDialogService.confirmedDangerous('Confirm delete', message, 'Delete', 'Cancel', Constants.BUTTON_ICON.DELETE, Constants.BUTTON_ICON.CANCEL).subscribe(() => {
      this.deletePending = true
      const affectedRows = this.messages.filter((m) => ids.includes(m.id))
      affectedRows.forEach((m) => { m.actionPending = true })
      if (this.selectedDetail != undefined && ids.includes(this.selectedDetail.id)) { this.detailActionPending = true }
      this.messageService.deleteMessages(ids, this.sentView).subscribe(() => {
        this.popupService.success(ids.length == 1 ? 'Message deleted.' : 'Messages deleted.')
        this.load(this.currentPage)
      }).add(() => {
        this.deletePending = false
        affectedRows.forEach((m) => { m.actionPending = false })
        this.detailActionPending = false
      })
    })
  }

}
