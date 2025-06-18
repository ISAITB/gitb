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

import { Component, HostListener } from '@angular/core';

@Component({
    selector: 'app-scroll-to-top',
    template: '<a class="scroll-to-top" [ngClass]="{\'visible\': visible}" href id="scrollToTop" (click)="doClick();$event.preventDefault()"><i class="fa-solid fa-chevron-up"></i></a>',
    standalone: false
})
export class ScrollToTopComponent {

  scrollListener: any
  timeout?: any
  visible = false

  @HostListener('window:scroll') onScroll(e: Event): void {
    clearTimeout(this.timeout)
    this.timeout = setTimeout(() => {
      if (document.documentElement.scrollTop > 150) {
        this.visible = true
      } else {
        this.visible = false
      }
    }, 200)
  }

  constructor() { }

  doClick() {
    const duration = 250
    const startingY = window.pageYOffset;
    const diff = 0 - startingY;
    let start:number
    window.requestAnimationFrame(function step(timestamp) {
      start = (!start)?timestamp:start;
      const time = timestamp - start;
      let percent = Math.min(time / duration, 1);
      window.scrollTo(0, startingY + diff * percent);
      if (time < duration) {
        window.requestAnimationFrame(step);
      }
    })
  }
}
