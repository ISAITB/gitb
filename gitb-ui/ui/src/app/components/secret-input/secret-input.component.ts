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

import { AfterViewInit, Component, ElementRef, EventEmitter, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ReplaySubject, Subscription } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { InvalidFormControlConfig } from 'src/app/types/invalid-form-control-config';

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
    styles: ['button {box-shadow: none;outline: none !important; width: 43px; }'],
    standalone: false
})
export class SecretInputComponent implements OnInit, AfterViewInit, ControlValueAccessor, OnDestroy {

  @Input() inputId!: string
  @Input() inputName!: string
  @Input() autoFocus = false
  @Input() validation?: ReplaySubject<InvalidFormControlConfig>
  @Input() autocomplete: 'new-password' | 'current-password' | 'off' = 'off'
  @Input() focusChange?: EventEmitter<boolean>

  @ViewChild("passwordField", { static: false }) passwordField?: ElementRef;
  _value?: string
  display = false
  hasValidation = false
  validationStateSubscription?: Subscription
  onChange = (_: any) => {}
  onTouched = () => {}
  Constants = Constants
  maskValue = false
  maskedValue?: string

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
    this.maskedValue = this.maskIfNeeded(this._value)
    this.emitChanges()
  }

  get value() {
    return this._value
  }

  onMaskedValueBeforeInput(e: InputEvent) {
    this._value = this._value ?? ''
    const input = e.target as HTMLInputElement;
    const start = input.selectionStart ?? 0;
    const end = input.selectionEnd ?? start;
    const inserted = e.data ?? '';
    let preventDefaultBehaviour = false;
    switch (e.inputType) {
      case 'insertText':
      case 'insertFromPaste':
      case 'insertReplacementText':
      case 'insertFromComposition':
        preventDefaultBehaviour = true
        // Replace selection or insert at caret
        this._value = this._value.slice(0, start) + inserted + this._value.slice(end);
        setTimeout(() => {
          const el = this.passwordField!.nativeElement;
          const caretPos = start + inserted.length;
          el.setSelectionRange(caretPos, caretPos);
        });
        break;
      case 'deleteContentBackward':
        preventDefaultBehaviour = true
        if (start === end && start > 0) {
          this._value = this._value.slice(0, start - 1) + this._value.slice(end);
          setTimeout(() => this.passwordField!.nativeElement.setSelectionRange(start - 1, start - 1));
        } else {
          this._value = this._value.slice(0, start) + this._value.slice(end);
          setTimeout(() => this.passwordField!.nativeElement.setSelectionRange(start, start));
        }
        break;
      case 'deleteContentForward':
      case 'deleteByCut':
        preventDefaultBehaviour = true
        if (start === end && start < this._value.length) {
          this._value = this._value.slice(0, start) + this._value.slice(start + 1);
          setTimeout(() => this.passwordField!.nativeElement.setSelectionRange(start, start));
        } else {
          this._value = this._value.slice(0, start) + this._value.slice(end);
          setTimeout(() => this.passwordField!.nativeElement.setSelectionRange(start, start));
        }
        break;
    }
    if (preventDefaultBehaviour) {
      // Prevent the browser from applying the change automatically
      e.preventDefault();
    }

    // update masked value
    this.maskedValue = this.maskIfNeeded(this._value);

    // emit changes
    this.emitChanges();
  }

  emitChanges() {
    this.onChange(this._value)
    this.onTouched()
  }

  writeValue(v: string|undefined): void {
    this._value = v
    this.maskedValue = this.maskIfNeeded(this._value)
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
          if (focus) {
            this.passwordField.nativeElement.focus()
          } else {
            this.passwordField.nativeElement.blur()
          }
        }
      })
    }
    if (this.validation) {
      this.validationStateSubscription = this.validation.subscribe((status) => {
        this.hasValidation = status.invalid == true && status.feedback != undefined
      })
    }
    if (this.autocomplete == 'off') {
      this.maskValue = true
    }
  }

  toggleDisplay() {
    this.display = !this.display
  }

  ngOnDestroy(): void {
    if (this.validationStateSubscription) {
      this.validationStateSubscription.unsubscribe()
    }
  }

  private maskIfNeeded(v: string|undefined): string|undefined {
    if (v == undefined) {
      return undefined
    } else {
      if (this.maskValue) {
        return '•'.repeat(v.length)
      } else {
        return v
      }
    }
  }

}
