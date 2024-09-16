import { AfterViewInit, Directive, ElementRef, Input, OnDestroy, Renderer2 } from '@angular/core';

@Directive({
  selector: '[invalid]'
})
export class InvalidFormControlDirective implements AfterViewInit, OnDestroy {

  @Input() set invalid(value: boolean|undefined) {
    this._invalid = value != undefined && value
    this.apply()
  }
  @Input() feedback?: string

  _invalid = false
  rendered = false

  highlightElement?: ElementRef

  constructor(
    private renderer: Renderer2,
    private elementRef: ElementRef
  ) { }

  ngAfterViewInit(): void {
    this.rendered = true
    this.apply()
  }

  private apply() {
    if (this._invalid) {
      this.renderer.addClass(this.elementRef.nativeElement, 'is-invalid')
      this.createFeedbackElement()
    } else {
      this.renderer.removeClass(this.elementRef.nativeElement, 'is-invalid')
      this.removeFeedbackElement()
    }
  }

  private createFeedbackElement() {
    if (this.feedback) {
      this.highlightElement = this.renderer.createElement("div")
      this.renderer.addClass(this.highlightElement, 'invalid-feedback')
      const textElement = this.renderer.createText(this.feedback)
      this.renderer.appendChild(this.highlightElement, textElement)
      this.renderer.appendChild(this.renderer.parentNode(this.elementRef.nativeElement), this.highlightElement)
    }
  }

  private removeFeedbackElement() {
    if (this.highlightElement) {
      this.renderer.removeChild(this.renderer.parentNode(this.elementRef.nativeElement), this.highlightElement)
    }
  }

  ngOnDestroy(): void {
    this.removeFeedbackElement()
  }

}
