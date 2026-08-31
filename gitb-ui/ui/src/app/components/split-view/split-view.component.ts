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

import { AfterViewInit, Component, ElementRef, HostListener, Input, NgZone, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild } from '@angular/core';

/**
 * Generic resizable two-pane split view: a "primary" pane (e.g. a table/list) on top and a "secondary"
 * pane (e.g. a detail view) below, separated by a draggable divider. Content is projected into the two
 * panes via the `splitViewPrimary`/`splitViewSecondary` attribute selectors - the host screen keeps full
 * control of what each pane renders, while this component owns the sizing/dragging mechanics only
 * (extracted from the original My Messages split view - see MessagesComponent's history).
 *
 * While `enabled` is false the two panes render as a plain top-to-bottom flow (no fixed heights, no
 * divider) - the same "continuous view" every consumer already had before adopting split view.
 *
 * Sizing model (only relevant while `enabled` is true):
 * - The available budget is always "from this component's own top down to `bottomBoundary()`" - a
 *   document-relative Y the host computes from whatever page chrome sits below this component (e.g. a
 *   footer that may be pinned to the viewport or pushed below the fold by a taller sidebar). This
 *   component has no opinion of its own on what that chrome is - the host is the only party that knows,
 *   so the boundary is measured *by the host* and handed in as a single number, not derived here from
 *   `window.innerHeight` and then iteratively corrected. That's a deliberate change from an earlier
 *   version of this component, which computed the budget against the viewport and then measured how far
 *   the laid-out page still overflowed it, fed the overflow back into the next pass, and repeated - a
 *   feedback loop that never converges when the overflow comes from something this component can't
 *   influence (e.g. a sidebar taller than the viewport on its own), producing a visible gradual shrink
 *   each time the page re-measures a still-nonzero overflow.
 * - Document-relative, not viewport-relative: using `getBoundingClientRect().top` for this component's
 *   own top would make the computed heights depend on the current scroll position, which itself depends
 *   on the heights just computed - scrolling the page would perpetually feed back into a new layout,
 *   which moves the scroll position again.
 * - On first becoming enabled (initial render with split view already on, or toggling it on), the
 *   primary pane gets 2/3 of the budget and the secondary 1/3, clamped to the primary's own min/max.
 *   From then on the primary pane keeps that pixel height (re-clamped against fresh bounds on every
 *   recalculation) rather than the 2/3 split being re-applied - so the user's chosen split (via dragging,
 *   or simply the initial 2/3) survives reloads/resizes. The one exception: while the clamp is not
 *   actually biting (the pane is neither at its floor nor its natural-content ceiling), the freshly
 *   computed height is persisted as the new "chosen" height - this is what lets the 2/3 split re-apply
 *   once a primary pane that started tiny (e.g. a table still loading) grows into its real content, and
 *   what keeps a primary pane pinned to its own maximum reacting to that maximum changing on reload.
 * - The secondary pane's reserved height is `minSecondaryHeight` while unselected/empty content is
 *   expected to fill it - but unlike the primary pane, the host's secondary content is free to grow
 *   taller than what's reserved (e.g. a long message), in which case the whole page simply scrolls; nothing
 *   here corrects that back.
 */
@Component({
    selector: 'app-split-view',
    templateUrl: './split-view.component.html',
    styleUrls: ['./split-view.component.less'],
    standalone: false
})
export class SplitViewComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  @Input() enabled = false

  /** Floor for the primary pane's height - e.g. a table's header plus one row, so dragging up never hides all its rows. */
  @Input() minPrimaryHeight = 100
  /** Floor for the secondary pane's height, and (while nothing has been dragged and the primary pane
   * isn't otherwise capped) the actual reserved height, not just a floor: the host's secondary content is
   * expected to stretch to fill it (e.g. via a `flex: 1` chain down to the projected element, see
   * MessagesComponent/message-detail.component.less) while nothing is selected, so this is the source of
   * truth for how tall that content ends up rather than something derived by measuring the content
   * itself. Kept visible (even empty) so the divider is always reachable. */
  @Input() minSecondaryHeight = 200
  /** Document-relative Y (not viewport-relative - see the class comment) that the secondary pane's
   * bottom should meet, supplied by the host from whatever page chrome sits below this component (e.g. a
   * footer). Read fresh on every recalculation rather than cached, since the host's own chrome can change
   * height (window resize) independently of anything this component does. */
  @Input() bottomBoundary?: () => number

  @ViewChild('primaryArea') primaryAreaRef?: ElementRef
  @ViewChild('divider') dividerRef?: ElementRef

  primaryHeight = 0
  secondaryHeight = 0
  // Guards the template's [style.height.px]/[style.min-height.px] bindings until the first real
  // recalculation has run - without this the primary pane briefly renders at height:0 (its unset initial
  // value) and then snaps to its computed height once ngAfterViewInit's measurement pass completes.
  sized = false

  // Set once the user has dragged the divider, or once an unclamped recalculation has run - from then on
  // the primary pane's height comes from here (re-clamped against freshly measured bounds on every
  // recalculation) instead of being derived from the 2/3 split, so the chosen split survives reloads/
  // resizes rather than being overwritten. See the class comment for when this is persisted automatically.
  private primaryHeightPx?: number
  private dragging = false
  private dragStartY = 0
  private dragStartPrimaryHeight = 0
  private dragMoveListener?: (event: MouseEvent) => void
  private dragUpListener?: (event: MouseEvent) => void
  private resizeObserver!: ResizeObserver
  private pendingFrame?: number
  // Matches the divider's own padding (see .less) - dragged fully down, the primary pane stops this far
  // short of its natural content height so the divider never ends up flush against its last element.
  private static readonly PRIMARY_HEIGHT_TRAILING_GAP = 12
  // Below this, two heights are treated as the same value - guards the "clamp did not bite" comparison
  // in recalculate() against sub-pixel measurement noise never settling into equality.
  private static readonly HEIGHT_EPSILON = 0.5

  constructor(
    private readonly eRef: ElementRef,
    private readonly zone: NgZone
  ) {}

  ngOnInit(): void {
    // Observes the primary pane's own projected content only (not this component's own host or pane
    // wrappers, which this component itself resizes, and not the secondary pane's projected content,
    // whose size is itself a direct consequence of the min-height this component just wrote onto it) so
    // that applying a computed height can never itself re-trigger this observer - only a genuine change in
    // the primary content's size (e.g. rows added/removed) does.
    this.resizeObserver = new ResizeObserver(() => this.scheduleRecalculate())
  }

  ngAfterViewInit(): void {
    if (this.primaryAreaRef?.nativeElement.firstElementChild) {
      this.resizeObserver.observe(this.primaryAreaRef.nativeElement.firstElementChild)
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['enabled'] && !changes['enabled'].firstChange && this.enabled) {
      this.primaryHeightPx = undefined
      // Recalculated synchronously, before Angular repaints this change, so the very next paint already
      // shows the fresh split - `sized` is never reset to false around a disable/enable cycle (it stays
      // true throughout, matching how it behaves within a single continuous "enabled" period), so without
      // this the bound height styles would otherwise still be evaluated with primaryHeight/secondaryHeight
      // left over from before this component was last disabled (e.g. a dragged height), producing a
      // one-frame flicker at that stale position. The divider isn't back in the DOM yet at this point (it's
      // behind `@if (enabled)`, not yet re-rendered for this change), so this pass measures it with the
      // same fallback recalculate() always uses when dividerRef is unset - close enough for one frame.
      this.recalculate()
      // Deferred a tick so the divider has actually re-rendered with its real geometry, to correct that
      // fallback precisely.
      setTimeout(() => this.scheduleRecalculate())
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver.disconnect()
    if (this.pendingFrame != undefined) {
      cancelAnimationFrame(this.pendingFrame)
    }
    this.endDrag()
  }

  @HostListener('window:resize')
  onWindowResize() {
    this.scheduleRecalculate()
  }

  /** Called by the host after its own primary content has reloaded, in case the reload didn't itself
   * change the primary content's rendered size (ResizeObserver would otherwise miss a same-size reload),
   * and whenever the host's own bottomBoundary()/minPrimaryHeight inputs change (neither triggers
   * ngOnChanges, both being read fresh on every pass rather than bound). */
  refresh() {
    setTimeout(() => this.scheduleRecalculate())
  }

  /** Coalesces window:resize, the ResizeObserver, and refresh() onto a single pending animation frame, so
   * a burst of events (e.g. dragging the OS window edge) produces at most one layout write per frame
   * instead of one per event. */
  private scheduleRecalculate() {
    if (this.pendingFrame != undefined) return
    this.pendingFrame = requestAnimationFrame(() => {
      this.pendingFrame = undefined
      this.zone.run(() => this.recalculate())
    })
  }

  private recalculate() {
    // Skipped entirely while the divider is being dragged - onDividerMouseMove already keeps its own
    // sizing consistent (clamped against freshly measured bounds on every step), so nothing here needs to
    // run again until the drag ends; recalculating mid-drag would fight the drag back toward whatever this
    // method computes, visible as a flashing/snap-back.
    if (this.dragging || !this.enabled || !this.primaryAreaRef) return
    const primaryAreaEl: HTMLElement = this.primaryAreaRef.nativeElement
    const naturalPrimaryHeight = this.measureNaturalPrimaryHeight(primaryAreaEl)
    const maxPrimaryHeight = naturalPrimaryHeight + SplitViewComponent.PRIMARY_HEIGHT_TRAILING_GAP
    const minPrimaryHeight = Math.max(this.minPrimaryHeight, 0)
    const dividerHeight = this.dividerRef ? (this.dividerRef.nativeElement as HTMLElement).offsetHeight : 12
    // Document-relative, not getBoundingClientRect().top (viewport-relative) - see the class comment on
    // why the budget must not depend on the current scroll position.
    const docTop = this.eRef.nativeElement.getBoundingClientRect().top + window.scrollY
    const bottom = this.bottomBoundary ? this.bottomBoundary() : window.innerHeight
    const budget = Math.max(bottom - docTop - dividerHeight, minPrimaryHeight + this.minSecondaryHeight)
    // The primary pane may never take more than would leave the secondary below its own floor - a
    // narrower ceiling than maxPrimaryHeight alone whenever the budget itself is tight.
    const upperBound = Math.max(minPrimaryHeight, Math.min(maxPrimaryHeight, budget - this.minSecondaryHeight))
    const desired = this.primaryHeightPx ?? (budget * 2 / 3)
    this.primaryHeight = this.clampHeight(desired, minPrimaryHeight, upperBound)
    this.secondaryHeight = Math.max(budget - this.primaryHeight, this.minSecondaryHeight)
    // Persisted as the new "chosen" height only when the clamp didn't actually change the desired value -
    // see the class comment for why this is what lets the 2/3 split settle in once the primary pane's own
    // content has finished loading, and keeps a maxed-out primary pane's ceiling tracking its own content.
    if (Math.abs(this.primaryHeight - desired) < SplitViewComponent.HEIGHT_EPSILON) {
      this.primaryHeightPx = this.primaryHeight
    }
    this.sized = true
  }

  /** The primary pane's true current content height, independent of its own explicit pixel height (set
   * via primaryHeight) - scrollHeight on an element whose own box is taller than its content returns the
   * element's own (explicitly-set) height, not the shorter content height, so reading it directly off
   * the wrapper would never notice the content having gotten shorter. Reads the wrapper's single
   * projected child instead, which (as long as it carries no height style of its own) always reflects
   * the content's actual current size. */
  private measureNaturalPrimaryHeight(primaryAreaEl: HTMLElement): number {
    const contentEl = primaryAreaEl.firstElementChild as HTMLElement | null
    return contentEl ? contentEl.getBoundingClientRect().height : primaryAreaEl.scrollHeight
  }

  private clampHeight(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), Math.max(min, max))
  }

  onDividerMouseDown(event: MouseEvent) {
    event.preventDefault()
    this.dragging = true
    this.dragStartY = event.clientY
    this.dragStartPrimaryHeight = this.primaryHeight
    document.body.style.userSelect = 'none'
    // Registered outside the Angular zone so a drag does not run a full change-detection pass on every
    // mousemove - onDividerMouseMove re-enters the zone itself only to write the two bound heights.
    this.zone.runOutsideAngular(() => {
      this.dragMoveListener = (e: MouseEvent) => this.onDividerMouseMove(e)
      this.dragUpListener = () => this.endDrag()
      document.addEventListener('mousemove', this.dragMoveListener)
      document.addEventListener('mouseup', this.dragUpListener)
    })
  }

  private onDividerMouseMove(event: MouseEvent) {
    if (!this.dragging || !this.primaryAreaRef) return
    const primaryAreaEl: HTMLElement = this.primaryAreaRef.nativeElement
    const naturalPrimaryHeight = this.measureNaturalPrimaryHeight(primaryAreaEl)
    const maxPrimaryHeight = naturalPrimaryHeight + SplitViewComponent.PRIMARY_HEIGHT_TRAILING_GAP
    const minPrimaryHeight = Math.max(this.minPrimaryHeight, 0)
    const delta = event.clientY - this.dragStartY
    const newPrimaryHeight = this.clampHeight(this.dragStartPrimaryHeight + delta, minPrimaryHeight, maxPrimaryHeight)
    // The two heights' sum is kept constant through the drag (the divider only redistributes space
    // between the panes, it does not change how much space is available) - a floor on the secondary
    // side only kicks in if the available budget itself is too small for it, matching recalculate().
    const totalHeight = this.primaryHeight + this.secondaryHeight
    this.zone.run(() => {
      this.primaryHeight = newPrimaryHeight
      this.secondaryHeight = Math.max(totalHeight - newPrimaryHeight, this.minSecondaryHeight)
      this.primaryHeightPx = newPrimaryHeight
    })
  }

  private endDrag() {
    this.dragging = false
    document.body.style.userSelect = ''
    if (this.dragMoveListener) { document.removeEventListener('mousemove', this.dragMoveListener); this.dragMoveListener = undefined }
    if (this.dragUpListener) { document.removeEventListener('mouseup', this.dragUpListener); this.dragUpListener = undefined }
  }

}
