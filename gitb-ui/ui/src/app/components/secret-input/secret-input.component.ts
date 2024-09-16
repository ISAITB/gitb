import { AfterViewInit, Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-secret-input',
  templateUrl: './secret-input.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecretInputComponent),
      multi: true
    }
  ],
  styles: ['button {box-shadow: none;outline: none !important; width: 43px; }']
})
export class SecretInputComponent implements OnInit, AfterViewInit,  ControlValueAccessor {

  @Input() id!: string
  @Input() name!: string
  @Input() autoFocus = false
  @Input() showAsInvalid? = false
  @Input() invalidFeedback?: string
  @Input() focusChange?: EventEmitter<boolean>

  @ViewChild("passwordField", { static: false }) passwordField?: ElementRef;
  _value?: string
  display = false
  onChange = (_: any) => {}
  onTouched = () => {}

  constructor() { }

  ngAfterViewInit(): void {
    if (this.autoFocus) {
      if (this.passwordField != undefined) {
        setTimeout(() => {
          this.passwordField?.nativeElement.focus()
        }, 1)
      }
    }
  }

  set value(v: string|undefined) {
    this._value = v
    this.emitChanges()
  }

  get value() {
    return this._value
  }

  emitChanges() {
    this.onChange(this._value)
    this.onTouched()
  }

  writeValue(v: string|undefined): void {
    this._value = v
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

  ngOnInit(): void {
    if (this.focusChange) {
      this.focusChange.subscribe((focus) => {
        if (this.passwordField != undefined) {
          setTimeout(() => {
            if (focus) {
              this.passwordField?.nativeElement.focus()
            } else {
              this.passwordField?.nativeElement.blur()
            }
          }, 1)
        }        
      })
    }
  }

  toggleDisplay() {
    this.display = !this.display
    if (this.display) {
      setTimeout(() => {
        this.passwordField?.nativeElement.focus()
      }, 1)
    }
  }

}
