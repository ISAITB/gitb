/*
 * Copyright (C) 2025 European Union
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
  HostListener,
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
import {Observable, of, tap} from 'rxjs';
import {share} from 'rxjs/operators';
import {Constants} from '../../common/constants';

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
  @Output() updated = new EventEmitter<CheckboxOptionState>()
  @Output() opening = new EventEmitter<void>()
  @Output() opened = new EventEmitter<void>()
  @Output() closed = new EventEmitter<void>()

  @ViewChild("button") buttonElement?: ElementRef<HTMLButtonElement>
  @ViewChild('popupTemplate') popupTemplate?: TemplateRef<any>;

  private containerDiv?: HTMLElement;
  private embeddedView?: EmbeddedViewRef<any>;

  currentState!: CheckboxOptionState
  open = false

  protected readonly Constants = Constants;

  constructor(
    private readonly eRef: ElementRef,
    private viewContainerRef: ViewContainerRef,
    private renderer: Renderer2
  ) { }

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
    this.closed.emit()
  }

  buttonClicked() {
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
      this.open = !this.open
      if (this.open) {
        this.opened.emit()
        this.openPanel()
      } else {
        this.close()
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
    }
  }

  private updatePosition = () => {
    if (!this.containerDiv || !this.buttonElement) return;
    const btnRect = this.buttonElement.nativeElement.getBoundingClientRect();
    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    const popup = this.containerDiv.firstElementChild as HTMLElement;
    if (!popup) return;
    if (this.placement == 'left') {
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
      popup.style.left = `${btnRect.left + scrollX - popup.offsetWidth}px`;
    } else {
      popup.style.top = `${btnRect.bottom + scrollY}px`;
      popup.style.left = `${btnRect.left + scrollX}px`;
    }
  };

  @HostListener('document:click', ['$event'])
  clickRegistered(event: any) {
    if (!this.eRef.nativeElement.contains(event.target) && this.open) {
      this.close()
    }
  }

  @HostListener('document:keyup.escape', ['$event'])
  escapeRegistered() {
    if (this.open) {
      this.close()
    }
  }

  ngOnInit(): void {
    this.currentState = {}
    this.applyConfig()
  }

  ngOnDestroy() {
    this.close()
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
      this.updated.emit(this.currentState)
      this.close()
    }
  }

}
