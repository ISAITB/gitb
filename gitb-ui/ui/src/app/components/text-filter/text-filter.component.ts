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

import {Component, ElementRef, EventEmitter, forwardRef, HostListener, Input, Output, ViewChild} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {Constants} from 'src/app/common/constants';

@Component({
    selector: 'app-text-filter',
    templateUrl: './text-filter.component.html',
    styleUrls: ['./text-filter.component.less'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TextFilterComponent),
            multi: true
        }
    ],
    standalone: false
})
export class TextFilterComponent implements ControlValueAccessor {

  @Input() name!: string
  @Input() placeholder = ''
  @Input() width?: number
  @Output() apply = new EventEmitter<string|undefined>()
  @ViewChild('filterText') filterTextElement?: ElementRef
  @ViewChild('filterButtonSearch') filterButtonSearchElement?: ElementRef
  @ViewChild('filterButtonClear') filterButtonClearElement?: ElementRef
  Constants = Constants
  _filterValue?: string
  readonly = true
  submitOngoing = false
  onChange = (_: any) => {}
  onTouched = () => {}

  constructor() { }

  set value(v: string|undefined) {
    this._filterValue = v
    this.emitChanges()
  }

  get value() {
    return this._filterValue
  }

  @HostListener('document:keyup.escape', ['$event'])
  escapeRegistered(event: KeyboardEvent) {
    if (!this.readonly) {
      this.clear()
      this.readonly = true
      this.filterTextElement?.nativeElement.blur()
    }
  }

  emitChanges() {
    this.onChange(this._filterValue)
    this.onTouched()
  }

  writeValue(v: string|undefined): void {
    this._filterValue = v
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

  filterClicked() {
    if (this.readonly) {
      this.readonly = false
      if (this.value === undefined) {
        this.value = ''
      }
    }
  }

  filterBlurred() {
    if (!this.submitOngoing) {
      this.applyFilter(true)
    }
  }

  clear() {
    this.submitOngoing = true
    this.applyFilter(false)
    this.submitOngoing = false
  }

  search() {
    this.submitOngoing = true
    this.applyFilter(true)
    this.submitOngoing = false
  }

  applyFilter(isSearch: boolean) {
    if (this.value != undefined) {
      if (isSearch) {
        // Apply
        this.value = this.value.trim()
        if (this.value.length == 0) {
          this.value = undefined
        }
        this.readonly = true
        this.apply.emit(this.value)
      } else {
        // Clear
        this.value = undefined
        this.apply.emit(this.value)
      }
      if (this.readonly) {
        this.filterTextElement?.nativeElement.blur()
        this.filterButtonSearchElement?.nativeElement.blur()
        this.filterButtonClearElement?.nativeElement.blur()
      }
    }
    this.submitOngoing = false
  }

}
