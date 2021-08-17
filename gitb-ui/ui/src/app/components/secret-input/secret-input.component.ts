import { Component, forwardRef, Input, OnInit } from '@angular/core';
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
  styles: ['button {box-shadow: none;outline: none !important;}']
})
export class SecretInputComponent implements OnInit, ControlValueAccessor {

  @Input() id!: string
  @Input() name!: string
  _value?: string
  display = false
  onChange = (_: any) => {}
  onTouched = () => {}

  constructor() { }

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
  }

}
