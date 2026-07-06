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

import {Component, EventEmitter, HostListener, Input, OnChanges, Output} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {CustomPropertyPresetValue} from 'src/app/types/custom-property-preset-value.type';
import {CustomProperty} from './custom-property';

@Component({
    selector: 'app-custom-property-filter',
    templateUrl: './custom-property-filter.component.html',
    styleUrls: ['./custom-property-filter.component.less'],
    standalone: false
})
export class CustomPropertyFilterComponent implements OnChanges {

  @Input() propertyFilter!: CustomProperty
  @Input() properties: CustomProperty[] = []
  @Input() embedded: boolean = false

  @Output() onApply = new EventEmitter<CustomProperty>()
  @Output() onClear = new EventEmitter<CustomProperty>()
  @Output() onCancel = new EventEmitter<CustomProperty>()

  Constants = Constants

  applied = false
  appliedName?: string
  appliedValue?: string
  appliedValueLabel?: string
  propertyInfo:{
    property?: CustomProperty,
    valueObj?: CustomPropertyPresetValue,
    value?: string
  } = {}

  constructor() { }

  ngOnChanges(): void {
    // Supports restoring a previously-applied filter (id + value already set on propertyFilter,
    // e.g. when returning to a page whose filters were saved): resolve the display labels once the
    // matching property definition is available, showing the entry as already "applied".
    if (!this.applied && this.propertyFilter?.id != undefined && this.propertyFilter?.value != undefined) {
      const property = this.properties.find(p => p.id === this.propertyFilter.id)
      if (property != undefined) {
        this.appliedName = property.name
        if (property.presetValues) {
          const match = property.presetValues.find(v => v.value === this.propertyFilter.value)
          this.appliedValueLabel = match?.label ?? this.propertyFilter.value
        } else {
          this.appliedValueLabel = this.propertyFilter.value
        }
        this.appliedValue = this.propertyFilter.value
        this.applied = true
      }
    }
  }

  @HostListener('document:keydown', ['$event'])
  keyDownRegistered(event: KeyboardEvent) {
    if (!this.applied) {
      switch (event.key) {
        case 'Escape': {
          this.cancel()
          break
        }
        case 'Enter': {
          this.apply()
          break;
        }
      }
    }
  }

  propertyChanged() {
    this.propertyInfo.valueObj = undefined
    this.propertyInfo.value = undefined
  }

  apply() {
    if (this.propertyInfo.property == undefined || (this.propertyInfo.valueObj == undefined && (this.propertyInfo.value == undefined || this.propertyInfo.value.trim() == ''))) {
      this.cancel()
    } else {
      this.appliedName = this.propertyInfo.property!.name
      if (this.propertyInfo.valueObj !== undefined) {
        this.appliedValue = this.propertyInfo.valueObj.value
        this.appliedValueLabel = this.propertyInfo.valueObj!.label
      } else {
        this.appliedValue = this.propertyInfo.value!.trim()
        this.appliedValueLabel = this.appliedValue
      }
      this.propertyFilter.id = this.propertyInfo.property.id
      this.propertyFilter.value = this.appliedValue
      this.onApply.emit(this.propertyFilter)
      this.applied = true
    }
  }

  clear() {
    this.onClear.emit(this.propertyFilter)
  }

  cancel() {
    this.onCancel.emit(this.propertyFilter)
  }

}
