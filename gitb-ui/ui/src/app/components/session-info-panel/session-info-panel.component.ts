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

import {Component, ElementRef, EmbeddedViewRef, Input, OnDestroy, OnInit, Renderer2, TemplateRef, ViewChild, ViewContainerRef} from '@angular/core';
import {Subscription} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {TestResultForDisplay} from 'src/app/types/test-result-for-display';
import {SessionInfoPanelApi} from './session-info-panel-api';

@Component({
    selector: 'app-session-info-panel',
    templateUrl: './session-info-panel.component.html',
    styleUrls: ['./session-info-panel.component.less'],
    standalone: false
})
export class SessionInfoPanelComponent implements OnInit, OnDestroy, SessionInfoPanelApi {

  @Input() row!: TestResultForDisplay

  @ViewChild('button') buttonElement?: ElementRef<HTMLButtonElement>
  @ViewChild('popupTemplate') popupTemplate?: TemplateRef<any>

  private containerDiv?: HTMLElement;
  private embeddedView?: EmbeddedViewRef<any>;
  private popupSubscription?: Subscription;

  open = false

  protected readonly Constants = Constants

  constructor(
    private readonly eRef: ElementRef,
    private readonly viewContainerRef: ViewContainerRef,
    private readonly renderer: Renderer2,
    public readonly dataService: DataService
  ) { }

  ngOnInit(): void {
    // Clicking this button to open its popup stops the click's propagation (see the template) so
    // that this panel's own documentClick() below does not immediately close it again. That also
    // means the click never reaches the table-level document listener that would otherwise notify
    // sibling row panels to close - so panels are told explicitly via this shared subject instead
    // (the same mechanism app-checkbox-option-panel uses), rather than each adding its own listener.
    this.popupSubscription = this.dataService.onButtonPopupOpen$.subscribe((source) => {
      if (source !== this && this.open) {
        this.close()
      }
    })
  }

  toggle() {
    if (this.open) {
      this.close()
    } else {
      this.open = true
      this.openPanel()
    }
  }

  close() {
    this.open = false
    if (this.containerDiv) {
      this.renderer.removeChild(document.body, this.containerDiv)
      this.containerDiv = undefined
    }
    if (this.embeddedView) {
      this.embeddedView.destroy()
      this.embeddedView = undefined
    }
    window.removeEventListener('scroll', this.updatePosition)
    window.removeEventListener('resize', this.updatePosition)
  }

  private openPanel() {
    if (!this.buttonElement) return
    this.containerDiv = this.renderer.createElement('div')
    this.renderer.setStyle(this.containerDiv, 'position', 'absolute')
    // Rendered hidden and off-screen first so its natural size can be measured (see updatePosition)
    // before it is actually placed and revealed - avoids a visible flicker/jump on open.
    this.renderer.setStyle(this.containerDiv, 'visibility', 'hidden')
    this.renderer.setStyle(this.containerDiv, 'top', '0')
    this.renderer.setStyle(this.containerDiv, 'left', '0')
    this.renderer.appendChild(document.body, this.containerDiv)
    this.embeddedView = this.viewContainerRef.createEmbeddedView(this.popupTemplate!)
    this.embeddedView.rootNodes.forEach(node => {
      this.renderer.appendChild(this.containerDiv, node)
    })
    setTimeout(() => {
      this.applyContentWidth()
      this.updatePosition()
      this.embeddedView!.rootNodes.forEach(node => {
        this.renderer.setStyle(node, 'visibility', 'visible')
      })
    }, 0)
    window.addEventListener('scroll', this.updatePosition)
    window.addEventListener('resize', this.updatePosition)
    this.dataService.signalButtonPopup(this)
  }

  /**
   * With the values left free to wrap, a width:auto popup's shrink-to-fit sizing collapses to
   * min-width instead of growing with content (wrappable text does not contribute its full
   * preferred width the way non-wrapping content would). To get "grows with content, up to
   * max-width" sizing, the natural width is measured with the values forced to a single line
   * (which sizes correctly, clamped by min/max-width same as any nowrap content), then that
   * measured width is locked in as an explicit width so the values can be released back to
   * wrapping for the actual rendering without the popup shrinking back down.
   */
  private applyContentWidth() {
    const popup = this.containerDiv?.firstElementChild as HTMLElement
    if (!popup) return
    const values = Array.from(popup.querySelectorAll<HTMLElement>('.session-info-panel-value'))
    values.forEach(value => this.renderer.setStyle(value, 'white-space', 'nowrap'))
    const naturalWidth = popup.getBoundingClientRect().width
    values.forEach(value => this.renderer.removeStyle(value, 'white-space'))
    this.renderer.setStyle(popup, 'width', `${naturalWidth}px`)
  }

  private updatePosition = () => {
    if (!this.containerDiv || !this.buttonElement) return
    const btnRect = this.buttonElement.nativeElement.getBoundingClientRect()
    const scrollX = window.scrollX
    const scrollY = window.scrollY
    const popup = this.containerDiv.firstElementChild as HTMLElement
    if (!popup) return
    // Open below the button by default, flipping above it if there isn't enough room below (and
    // there is room above) - same flip-if-no-room approach used by app-checkbox-option-panel.
    const gap = 2
    const popupHeight = popup.offsetHeight
    let top = btnRect.bottom + scrollY + gap
    if (btnRect.bottom + gap + popupHeight > window.innerHeight) {
      const flippedTop = btnRect.top + scrollY - popupHeight - gap
      if (flippedTop >= scrollY) {
        top = flippedTop
      }
    }
    popup.style.top = `${top}px`
    // Right-align the popup with the button rather than left-align, as the button is pinned to the
    // right edge of the header row.
    popup.style.left = `${btnRect.right + scrollX - popup.offsetWidth}px`
  }

  documentEscape(): void {
    if (this.open) {
      this.close()
    }
  }

  documentClick(event: Event): void {
    if (this.open &&
        !this.eRef.nativeElement.contains(event.target as Node) &&
        !this.containerDiv?.contains(event.target as Node)) {
      this.close()
    }
  }

  ngOnDestroy(): void {
    this.close()
    this.popupSubscription?.unsubscribe()
  }

}
