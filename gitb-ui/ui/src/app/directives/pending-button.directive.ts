import { AfterViewInit, Directive, ElementRef, Input, Renderer2 } from '@angular/core';

@Directive({
  selector: 'button[pending]'
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
    this.originalContent.innerHTML = this.elementRef.nativeElement.innerHTML
    // Clear original content
    this.elementRef.nativeElement.innerHTML = ''
    // Create pending span
    this.pendingSpan = this.renderer.createElement('span')
    if (!this.icon) {
      this.renderer.addClass(this.pendingSpan, 'mini-tab')
    }
    this.pendingSpan.innerHTML = '<i class="fa fa-spinner fa-spin fa-lg fa-fw"></i>'
    // Add pending span followed by original content wrapper
    this.renderer.appendChild(this.elementRef.nativeElement, this.pendingSpan)
    this.renderer.appendChild(this.elementRef.nativeElement, this.originalContent)
    // Apply status
    this.applyStatus()
  }

  @Input() set pending(value: boolean) {
    this._pending = value
    this.applyStatus()
  }

  @Input() set disable(value: boolean) {
    this._disable = value
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
