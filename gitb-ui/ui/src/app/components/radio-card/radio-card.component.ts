/*
 * Copyright (C) 2026 European Union
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

import {Component, EventEmitter, forwardRef, Input, Output} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';

@Component({
  selector: 'app-radio-card',
  standalone: false,
  templateUrl: './radio-card.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RadioCardComponent),
      multi: true
    }
  ],
  styleUrl: './radio-card.component.less'
})
export class RadioCardComponent<T> implements ControlValueAccessor {

  @Input() radioValue!: T
  @Input() radioId!: string
  @Input() radioName!: string
  @Input() radioLabel!: string
  @Output() select = new EventEmitter<T>();

  onChange = (_: any) => {}
  onTouched = () => {}
  _value?: T

  set value(v: T|undefined) {
    this._value = v
    this.emitChanges()
  }

  get value() {
    return this._value
  }

  activate() {
    this._value = this.radioValue
    this.emitChanges()
  }

  emitChanges() {
    this.onChange(this._value)
    this.onTouched()
    this.select.emit()
  }

  writeValue(v: T|undefined): void {
    this._value = v
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

}
