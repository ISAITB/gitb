import { Component, HostListener } from '@angular/core';

@Component({
  selector: 'app-scroll-to-top',
  template: '<a class="scroll-to-top" [ngClass]="{\'visible\': visible}" href id="scrollToTop" (click)="doClick();$event.preventDefault()"><i class="fa fa-chevron-up"></i></a>'
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
