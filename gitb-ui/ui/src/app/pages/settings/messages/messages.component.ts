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

  // Component state only, not persisted - see the task's "Subsequent steps" note about recording this
  // as a user preference later.
  splitView = false
  splitTableHeight = 0
  splitDetailHeight = 0
  // Once the user drags the divider, a data reload or viewport resize only re-clamps splitTableHeight/
  // splitDetailHeight against the new bounds rather than recomputing the initial "fit the empty panel
  // on screen" split from scratch.
  private splitUserAdjusted = false
  private dragging = false
  private dragStartY = 0
  private dragStartTableHeight = 0
  private dragMoveListener?: (event: MouseEvent) => void
  private dragUpListener?: (event: MouseEvent) => void
  private static readonly MIN_EMPTY_DETAIL_HEIGHT = 200
  private static readonly FALLBACK_MIN_TABLE_HEIGHT = 120
  // Matches .split-divider's own padding (messages.component.less) - dragged fully down, the table area
  // stops this far short of the table's natural content height so the divider never ends up flush
  // against the last row.
  private static readonly TABLE_HEIGHT_TRAILING_GAP = 12

  @ViewChild('showMessagesFilter') showMessagesFilter?: CheckboxOptionPanelComponent
  @ViewChild('messageTable') messageTable?: MessageTableComponent
  @ViewChild('messagesPage') messagesPage?: ElementRef
  @ViewChild('controlsContainer') controlsContainerRef?: ElementRef
  @ViewChild('searchControls') searchControlsRef?: ElementRef
  @ViewChild('actionControls') actionControlsRef?: ElementRef
  @ViewChild('tableArea') tableAreaRef?: ElementRef
  @ViewChild('splitDivider') splitDividerRef?: ElementRef

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
      this.zone.run(() => {
        this.calculateWrapping()
        this.recalculateSplitHeights()
      })
    })
    if (this.messagesPage) {
      this.resizeObserver.observe(this.messagesPage.nativeElement)
    }
  }

  ngOnDestroy(): void {
    this.messageSentSubscription?.unsubscribe()
    if (this.resizeObserver && this.messagesPage) {
      this.resizeObserver.unobserve(this.messagesPage.nativeElement)
    }
    this.endDividerDrag()
  }

  // The container ResizeObserver above tracks width changes (its box only grows/shrinks with the page's
  // own layout); the vertical space available to the split view instead follows the viewport height, so
  // a plain window resize listener is the correct signal for it.
  @HostListener('window:resize')
  onWindowResize() {
    this.recalculateSplitHeights()
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

  toggleSplitView() {
    this.splitUserAdjusted = false
    // Deferred a tick so .table-area/.split-divider (present unconditionally/only in split view
    // respectively - the divider via @if) have actually rendered with their new geometry before being
    // measured - the same deferral pattern used elsewhere on this page (see load()'s finish()).
    setTimeout(() => this.recalculateSplitHeights())
  }

  /** Recomputes splitTableHeight/splitDetailHeight so their sum fills the space between the top of the
   * table area and the bottom of the viewport (minus the divider and the card's own bottom padding) -
   * so that with nothing selected the card ends at the viewport bottom with no page scrollbar. Called on
   * entering split view, on data reloads, and on viewport/container resizes. Before the user has ever
   * dragged the divider this always starts from the "just enough room for the empty detail panel" split;
   * afterwards it only re-clamps the user's chosen split against the (possibly changed) table bounds. */
  private recalculateSplitHeights() {
    if (!this.splitView || !this.tableAreaRef || !this.messagesPage) return
    const tableAreaEl: HTMLElement = this.tableAreaRef.nativeElement
    const naturalTableHeight = this.measureNaturalTableHeight(tableAreaEl)
    const maxTableHeight = naturalTableHeight + MessagesComponent.TABLE_HEIGHT_TRAILING_GAP
    const minTableHeight = this.measureMinTableHeight(tableAreaEl)
    const dividerHeight = this.splitDividerRef ? (this.splitDividerRef.nativeElement as HTMLElement).offsetHeight : 12
    const cardBodyEl = (this.messagesPage.nativeElement as HTMLElement).querySelector('.card-body') as HTMLElement | null
    const cardEl = (this.messagesPage.nativeElement as HTMLElement).querySelector('.card') as HTMLElement | null
    // The space below the content flow that still needs to be inside the viewport for the card to end
    // exactly at its bottom - the card body's own bottom padding (below its last child) plus the card's
    // own border - not just the padding, which alone left the card a few pixels past the viewport edge.
    // .page-root (IndexComponent's own wrapper around the routed page, see index.component.less) carries
    // its own margin-bottom below our card - easy to miss since it's outside this component entirely,
    // but it still sits between the card and .child's own bottom edge and has to be reserved too.
    const pageRootEl = (this.messagesPage.nativeElement as HTMLElement).closest('.page-root') as HTMLElement | null
    const pageRootBottomMargin = pageRootEl ? (parseFloat(getComputedStyle(pageRootEl).marginBottom) || 0) : 0
    const cardBottomChrome = (cardBodyEl ? (parseFloat(getComputedStyle(cardBodyEl).paddingBottom) || 0) : 0)
      + (cardEl ? (parseFloat(getComputedStyle(cardEl).borderBottomWidth) || 0) : 0)
      + pageRootBottomMargin
    // .page.index is a flex column (header-bar / .child / .footer-bar, see app.less) with .child set to
    // flex:1 - so .footer-bar normally sits pinned at the viewport bottom on its own, with no JS needed,
    // as long as .child's content doesn't outgrow the space left for it. Sizing the empty detail panel
    // against the literal window bottom (as before) ignored that the footer still needs its own room
    // below .child, pushing the footer off-screen even with nothing selected. footer-bar's own height is
    // stable regardless of whether it's currently pinned in view or already pushed below the fold, so
    // reserving it here keeps the "no scrollbar when nothing is selected" promise while still leaving the
    // footer visible.
    const footerEl = document.querySelector('.footer-bar') as HTMLElement | null
    const footerHeight = footerEl ? footerEl.getBoundingClientRect().height : 0
    const top = tableAreaEl.getBoundingClientRect().top
    const available = Math.max(window.innerHeight - footerHeight - top - dividerHeight - cardBottomChrome, minTableHeight + MessagesComponent.MIN_EMPTY_DETAIL_HEIGHT)
    if (!this.splitUserAdjusted) {
      const detailHeight = MessagesComponent.MIN_EMPTY_DETAIL_HEIGHT
      const tableHeight = this.clampHeight(available - detailHeight, minTableHeight, maxTableHeight)
      this.splitTableHeight = tableHeight
      this.splitDetailHeight = available - tableHeight
    } else {
      // Re-clamped against freshly measured bounds every time (every reload, every toggle-on, every
      // resize) - this is what shrinks splitTableHeight back down (and grows splitDetailHeight to
      // absorb the freed space) whenever the row count drops, e.g. after a search narrows the results.
      this.splitTableHeight = this.clampHeight(this.splitTableHeight, minTableHeight, maxTableHeight)
      this.splitDetailHeight = Math.max(available - this.splitTableHeight, MessagesComponent.MIN_EMPTY_DETAIL_HEIGHT)
    }
    // A second pass, deferred so the DOM actually reflects the heights just computed and the browser has
    // laid out with them, corrects for any small residual page overflow this analytic budget can't fully
    // account for (subpixel rounding, the footer's own flex-wrapped content re-wrapping narrower/taller
    // as a vertical scrollbar appears or disappears, etc.) - see correctSplitOverflow().
    setTimeout(() => this.correctSplitOverflow())
  }

  /** Trims however many pixels the page ends up taller than the viewport once the heights from
   * recalculateSplitHeights() are actually applied and laid out - guaranteeing no unnecessary scrollbar
   * regardless of any subpixel/layout detail that budget's arithmetic can't fully account for on its own
   * (e.g. an ancestor margin outside this component entirely). Skipped once a message is selected, where
   * growing past the viewport is expected (see the "only case the viewport should be extended" rule).
   * The overflow is taken from splitDetailHeight first (down to the empty-placeholder floor - the table
   * already showing its full natural content is the more important thing to preserve), and only once
   * that floor is hit does it fall back to shrinking splitTableHeight (down to the drag-up floor,
   * growing .table-area's own internal scrollbar instead of the page's) - a genuine, unavoidable overflow
   * is left as-is only once both floors are hit (e.g. an exceptionally short viewport). */
  private correctSplitOverflow() {
    if (!this.splitView || this.selectedDetail != undefined || !this.tableAreaRef) return
    const overflow = document.documentElement.scrollHeight - window.innerHeight
    if (overflow <= 0) return
    const fromDetail = Math.min(overflow, Math.max(this.splitDetailHeight - MessagesComponent.MIN_EMPTY_DETAIL_HEIGHT, 0))
    this.splitDetailHeight -= fromDetail
    const remaining = overflow - fromDetail
    if (remaining > 0) {
      const minTableHeight = this.measureMinTableHeight(this.tableAreaRef.nativeElement)
      this.splitTableHeight = Math.max(this.splitTableHeight - remaining, minTableHeight)
    }
  }

  /** The table's true current content height, independent of .table-area's own explicit pixel height
   * (which the caller sets via splitTableHeight) - scrollHeight on an element whose own box is taller
   * than its content returns the element's own (explicitly-set) height, not the shorter content height,
   * so reading it directly off .table-area would never notice the table having gotten shorter (e.g.
   * after a search narrows the results), leaving splitTableHeight stuck too tall with a visible gap
   * below the last row. .table-container (app-message-table's own root) carries no height style of its
   * own, so its rendered height always reflects the table's actual current content. */
  private measureNaturalTableHeight(tableAreaEl: HTMLElement): number {
    const contentEl = tableAreaEl.querySelector('.table-container') as HTMLElement | null
    return contentEl ? contentEl.getBoundingClientRect().height : tableAreaEl.scrollHeight
  }

  /** The drag-up limit: the table area's own header row plus one message row - falls back to a fixed
   * constant while the table is empty/loading (neither element is rendered yet). */
  private measureMinTableHeight(tableAreaEl: HTMLElement): number {
    const thead = tableAreaEl.querySelector('thead')
    const row = tableAreaEl.querySelector('tbody tr')
    if (thead && row) {
      return thead.getBoundingClientRect().height + row.getBoundingClientRect().height
    }
    return MessagesComponent.FALLBACK_MIN_TABLE_HEIGHT
  }

  private clampHeight(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), Math.max(min, max))
  }

  onDividerMouseDown(event: MouseEvent) {
    event.preventDefault()
    this.dragging = true
    this.dragStartY = event.clientY
    this.dragStartTableHeight = this.splitTableHeight
    document.body.style.userSelect = 'none'
    // Registered outside the Angular zone so a drag does not run a full change-detection pass on every
    // mousemove - onDividerMouseMove re-enters the zone itself only to write the two bound heights.
    this.zone.runOutsideAngular(() => {
      this.dragMoveListener = (e: MouseEvent) => this.onDividerMouseMove(e)
      this.dragUpListener = () => this.endDividerDrag()
      document.addEventListener('mousemove', this.dragMoveListener)
      document.addEventListener('mouseup', this.dragUpListener)
    })
  }

  private onDividerMouseMove(event: MouseEvent) {
    if (!this.dragging || !this.tableAreaRef) return
    const tableAreaEl: HTMLElement = this.tableAreaRef.nativeElement
    const naturalTableHeight = this.measureNaturalTableHeight(tableAreaEl)
    const maxTableHeight = naturalTableHeight + MessagesComponent.TABLE_HEIGHT_TRAILING_GAP
    const minTableHeight = this.measureMinTableHeight(tableAreaEl)
    const delta = event.clientY - this.dragStartY
    const newTableHeight = this.clampHeight(this.dragStartTableHeight + delta, minTableHeight, maxTableHeight)
    // The two heights' sum is kept constant through the drag (the divider only redistributes space
    // between the panels, it does not change how much space is available) - a floor on the detail side
    // only kicks in if the available budget itself is too small for it, matching recalculateSplitHeights.
    const totalHeight = this.splitTableHeight + this.splitDetailHeight
    this.zone.run(() => {
      this.splitTableHeight = newTableHeight
      this.splitDetailHeight = Math.max(totalHeight - newTableHeight, MessagesComponent.MIN_EMPTY_DETAIL_HEIGHT)
      this.splitUserAdjusted = true
    })
  }

  private endDividerDrag() {
    this.dragging = false
    document.body.style.userSelect = ''
    if (this.dragMoveListener) { document.removeEventListener('mousemove', this.dragMoveListener); this.dragMoveListener = undefined }
    if (this.dragUpListener) { document.removeEventListener('mouseup', this.dragUpListener); this.dragUpListener = undefined }
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
      setTimeout(() => {
        this.calculateWrapping()
        this.recalculateSplitHeights()
      })
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
    setTimeout(() => this.calculateWrapping())
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
