import { Directive, ElementRef, forwardRef, HostListener, Input, Renderer2 } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Directive({
    selector: 'input[type=checkbox][appTrueFalseValue]',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TrueFalseValueDirective),
            multi: true
        }
    ],
    standalone: false
})
export class TrueFalseValueDirective implements ControlValueAccessor {

  @Input() trueValue:any = true
  @Input() falseValue:any = false
  private onChange = (_: any) => {}
  private onTouched = () => {}

  constructor(private elementRef: ElementRef, private renderer: Renderer2) { }

  @HostListener('change', ['$event'])
  onHostChange(ev: any) {
    this.onChange(ev.target.checked ? this.trueValue : this.falseValue)
    this.onTouched()
  }

  writeValue(obj: any): void {
    if (obj === this.trueValue) {
      this.renderer.setProperty(this.elementRef.nativeElement, 'checked', true)
    } else {
      this.renderer.setProperty(this.elementRef.nativeElement, 'checked', false)
    }      
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

}
