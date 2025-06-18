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

import {AfterViewInit, Directive, ElementRef, Input, Renderer2} from '@angular/core';

@Directive({
    selector: 'button[pending]',
    standalone: false
})
export class PendingButtonDirective implements AfterViewInit {

  @Input() icon = false

  private _disable: boolean = false
  private _pending: boolean = false
  private pendingSpan: any
  private originalContent: any
  private rendered = false

  constructor(
    private renderer: Renderer2,
    private elementRef: ElementRef
  ) { }

  ngAfterViewInit(): void {
    this.rendered = true
    // Wrap the original content
    this.originalContent = this.renderer.createElement('span')
    this.renderer.addClass(this.originalContent, 'w-100')
    this.originalContent.innerHTML = this.elementRef.nativeElement.innerHTML
    // Clear original content
    this.elementRef.nativeElement.innerHTML = ''
    // Create pending span
    this.pendingSpan = this.renderer.createElement('span')
    if (!this.icon) {
      this.renderer.addClass(this.pendingSpan, 'mini-tab')
    }
    this.pendingSpan.innerHTML = '<i class="fa-solid fa-spinner fa-spin-override fa-lg"></i>'
    // Add pending span followed by original content wrapper
    this.renderer.appendChild(this.elementRef.nativeElement, this.pendingSpan)
    this.renderer.appendChild(this.elementRef.nativeElement, this.originalContent)
    // Apply status
    this.applyStatus()
  }

  @Input() set pending(value: boolean|undefined) {
    this._pending = value != undefined && value
    this.applyStatus()
  }

  @Input() set disable(value: boolean|undefined) {
    this._disable = value != undefined && value
    this.applyStatus()
  }

  private applyStatus() {
    if (this.rendered) {
      if (this._disable) {
        this.renderer.setAttribute(this.elementRef.nativeElement, 'disabled', 'disabled')
      }
      if (this._pending) {
        this.renderer.addClass(this.elementRef.nativeElement, 'pending')
        this.renderer.removeClass(this.pendingSpan, 'hidden')
        this.renderer.setAttribute(this.elementRef.nativeElement, 'disabled', 'disabled')
        if (this.icon) {
          // Hide the original content
          this.renderer.addClass(this.originalContent, 'hidden')
        }
      } else {
        this.renderer.removeClass(this.elementRef.nativeElement, 'pending')
        this.renderer.addClass(this.pendingSpan, 'hidden')
        if (this.icon) {
          // Show the original content
          this.renderer.removeClass(this.originalContent, 'hidden')
        }
        if (!this._disable) {
          this.renderer.removeAttribute(this.elementRef.nativeElement, 'disabled')
        }
      }
    }
  }

}
