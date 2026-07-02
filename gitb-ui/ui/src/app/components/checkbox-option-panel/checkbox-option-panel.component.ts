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

import {
  Component,
  ElementRef,
  EmbeddedViewRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {CheckboxOptionState} from './checkbox-option-state';
import {CheckboxOption} from './checkbox-option';
import {CheckBoxOptionPanelComponentApi} from './check-box-option-panel-component-api';
import {Observable, of, Subscription, tap} from 'rxjs';
import {share} from 'rxjs/operators';
import {Constants} from '../../common/constants';
import {DataService} from '../../services/data.service';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-checkbox-option-panel',
    templateUrl: './checkbox-option-panel.component.html',
    styleUrls: ['./checkbox-option-panel.component.less'],
    standalone: false
})
export class CheckboxOptionPanelComponent implements OnInit, OnDestroy, CheckBoxOptionPanelComponentApi {

  // If multiple arrays are provided they are displayed with a separator between them.
  @Input() options?: CheckboxOption[][]
  @Input() optionProvider?: () => Observable<CheckboxOption[][]>
  @Input() label!: string
  @Input() icon?: string
  @Input() singleSelection = false
  @Input() pending = false
  @Input() placement: 'left'|'bottom' = 'bottom'
  @Input() referenceItem?: any
  @Input() labelIcon?: string
  @Input() smallButton = false
  @Output() updated = new EventEmitter<CheckboxOptionState>()
  @Output() opening = new EventEmitter<void>()
  @Output() opened = new EventEmitter<void>()
  @Output() closed = new EventEmitter<void>()

  @ViewChild("button") buttonElement?: ElementRef<HTMLButtonElement>
  @ViewChild('popupTemplate') popupTemplate?: TemplateRef<any>;

  private containerDiv?: HTMLElement;
  private embeddedView?: EmbeddedViewRef<any>;
  private popupSubscription?: Subscription;

  currentState!: CheckboxOptionState
  open = false

  protected readonly Constants = Constants;

  constructor(
    private readonly eRef: ElementRef,
    private viewContainerRef: ViewContainerRef,
    private renderer: Renderer2,
    private readonly dataService: DataService
  ) { }

  getReferenceItem() {
    return this.referenceItem
  }

  close() {
    this.open = false;
    if (this.containerDiv) {
      this.renderer.removeChild(document.body, this.containerDiv);
      this.containerDiv = undefined
    }
    if (this.embeddedView) {
      this.embeddedView.destroy();
      this.embeddedView = undefined;
    }
    window.removeEventListener('scroll', this.updatePosition);
    window.removeEventListener('resize', this.updatePosition);
    window.removeEventListener('keydown', this.onPopupKeyDown);
    this.closed.emit()
  }

  buttonClicked(pop?: NgbTooltip) {
    if (pop) {
      pop.disableTooltip = true
      pop.close()
      setTimeout(() => {
        pop.disableTooltip = false
      }, this.Constants.TOOLTIP_DELAY + 50)
    }
    let obs$: Observable<any>
    if (!this.open) {
      this.opening.emit()
      if (this.optionProvider) {
        this.pending = true
        const options$ = this.optionProvider().pipe(
          tap(options => {
            this.refresh(options)
          }),
          share()
        )
        options$.subscribe()
        obs$ = options$
      } else {
        obs$ = of(true)
      }
    } else {
      obs$ = of(true)
    }
    obs$.subscribe(() => {
      if (this.options != undefined && this.options.length > 0 && this.options[0].length > 0) {
        this.open = !this.open
        if (this.open) {
          this.opened.emit()
          this.openPanel()
        } else {
          this.close()
        }
      }
    }).add(() => {
      this.pending = false
    })
  }

  private openPanel() {
    if (this.buttonElement) {
      this.containerDiv = this.renderer.createElement("div");
      this.renderer.setStyle(this.containerDiv, 'position', 'absolute');
      this.renderer.setStyle(this.containerDiv, 'visibility', 'hidden');
      this.renderer.setStyle(this.containerDiv, 'top', '0');
      this.renderer.setStyle(this.containerDiv, 'left', '0');
      this.renderer.appendChild(document.body, this.containerDiv);
      this.embeddedView = this.viewContainerRef.createEmbeddedView(this.popupTemplate!);
      this.embeddedView.rootNodes.forEach(node => {
        this.renderer.appendChild(this.containerDiv, node);
      });
      setTimeout(() => {
        this.updatePosition();
        this.embeddedView!.rootNodes.forEach(node => {
          this.renderer.setStyle(node, 'visibility', 'visible');
        });
      }, 0)
      window.addEventListener('scroll', this.updatePosition);
      window.addEventListener('resize', this.updatePosition);
      window.addEventListener('keydown', this.onPopupKeyDown);
      this.dataService.signalButtonPopup(this)
    }
  }

  /**
   * Roving keyboard navigation within the open popup, mirroring native listbox/dropdown behaviour
   * (and matching the page-size dropdown): arrow keys move focus between the enabled checkboxes,
   * starting at the first one if none is focused yet. Space/Enter then toggle the focused checkbox
   * natively - no extra handling needed for that part.
   */
  private onPopupKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      this.moveFocus(event.key === 'ArrowDown' ? 1 : -1)
    }
  };

  private moveFocus(direction: number): void {
    if (!this.containerDiv) return;
    const inputs = Array.from(this.containerDiv.querySelectorAll('input[type="checkbox"]:not(:disabled)')) as HTMLInputElement[];
    if (inputs.length === 0) return;
    const currentIndex = inputs.indexOf(document.activeElement as HTMLInputElement);
    const nextIndex = currentIndex === -1 ? 0 : Math.min(inputs.length - 1, Math.max(0, currentIndex + direction));
    inputs[nextIndex].focus();
  }

  private updatePosition = () => {
    if (!this.containerDiv || !this.buttonElement) return;
    const btnRect = this.buttonElement.nativeElement.getBoundingClientRect();
    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    const popup = this.containerDiv.firstElementChild as HTMLElement;
    if (!popup) return;
    if (this.placement == 'left') {
      // The 2px gap matches ng-bootstrap's dropdown default Popper offset, kept consistent with
      // the 'bottom' placement's gap below.
      const gap = 2;
      const popupHeight = popup.offsetHeight;
      let top = btnRect.top + scrollY;
      if (btnRect.top + popupHeight > window.innerHeight) {
        const flippedTop = btnRect.bottom + scrollY - popupHeight;
        if (flippedTop >= scrollY) {
          // Fits if aligned at the bottom of the button
          top = flippedTop;
        }
      }
      popup.style.top = `${top}px`;
      popup.style.left = `${btnRect.left + scrollX - popup.offsetWidth - gap}px`;
    } else {
      // Same flip-if-no-room approach as the 'left' placement above, but along the vertical axis:
      // open below the button by default, flipping to open above it if there isn't enough room
      // below (and there is room above). The 2px gap matches ng-bootstrap's dropdown (e.g. the
      // page-size selector), which uses a default Popper offset of 2px between button and menu.
      const gap = 2;
      const popupHeight = popup.offsetHeight;
      let top = btnRect.bottom + scrollY + gap;
      if (btnRect.bottom + gap + popupHeight > window.innerHeight) {
        const flippedTop = btnRect.top + scrollY - popupHeight - gap;
        if (flippedTop >= scrollY) {
          top = flippedTop;
        }
      }
      popup.style.top = `${top}px`;
      popup.style.left = `${btnRect.left + scrollX}px`;
    }
  };

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

  ngOnInit(): void {
    this.currentState = {}
    this.applyConfig()
    this.popupSubscription = this.dataService.onButtonPopupOpen$.subscribe((source => {
      if (source !== this && this.open) {
        this.close()
      }
    }))
  }

  ngOnDestroy() {
    this.close()
    if (this.popupSubscription) this.popupSubscription.unsubscribe()
  }

  refresh(newConfig: CheckboxOption[][]) {
    this.options = newConfig
    this.applyConfig()
  }

  private applyConfig() {
    if (this.options) {
      for (let optionSet of this.options) {
        for (let option of optionSet) {
          this.currentState[option.key] = option.default
        }
      }
    }
  }

  handleClick(key: string) {
    if (this.singleSelection) {
      this.currentState = {}
      this.currentState[key] = true
      const event:CheckboxOptionState = {}
      event[key] = true
      this.updated.emit(event)
      this.close()
    }
  }

}
