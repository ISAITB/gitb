import { AfterViewInit, Directive, ElementRef, Input, OnDestroy, Renderer2 } from '@angular/core';
import { InvalidFormControlConfig } from '../types/invalid-form-control-config';
import { ReplaySubject, Subscription } from 'rxjs';

@Directive({
  selector: '[invalid]'
})
export class InvalidFormControlDirective implements AfterViewInit, OnDestroy {

  private config: InvalidFormControlConfig = {}
  private highlightElement?: ElementRef
  private emitterSubscription?: Subscription

  @Input() set invalid(_config: undefined|InvalidFormControlConfig|ReplaySubject<InvalidFormControlConfig>) {
    if (this.isEmitter(_config)) {
      this.emitterSubscription = _config.subscribe((newConfig) => {
        const invalidBefore = this.config.invalid
        const feedbackBefore = this.config.feedback
        if (invalidBefore != newConfig.invalid || feedbackBefore != newConfig.feedback) {
          this.config.invalid = newConfig.invalid
          this.config.feedback = newConfig.feedback
          this.apply()
        }
      })
    } else if (_config) {
      this.config.invalid = _config.invalid
      this.config.feedback = _config.feedback
      this.apply()
    } else {
      this.config.invalid = false
      this.config.feedback = undefined
    }
  }

  private isEmitter(obj: undefined|InvalidFormControlConfig|ReplaySubject<InvalidFormControlConfig>): obj is ReplaySubject<InvalidFormControlConfig> {
    return obj != undefined && (obj as ReplaySubject<InvalidFormControlConfig>).subscribe != undefined
  }

  constructor(
    private renderer: Renderer2,
    private elementRef: ElementRef
  ) { }

  ngAfterViewInit(): void {
    this.apply()
  }

  private apply() {
    this.removeFeedbackElement()
    if (this.config.invalid) {
      this.renderer.addClass(this.elementRef.nativeElement, 'is-invalid')
      this.createFeedbackElement()
    } else {
      this.renderer.removeClass(this.elementRef.nativeElement, 'is-invalid')
    }
  }

  private createFeedbackElement() {
    if (this.config.feedback) {
      this.highlightElement = this.renderer.createElement("div")
      this.renderer.addClass(this.highlightElement, 'invalid-feedback')
      const textElement = this.renderer.createText(this.config.feedback)
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
    if (this.emitterSubscription) {
      this.emitterSubscription.unsubscribe()
    }
  }

}
