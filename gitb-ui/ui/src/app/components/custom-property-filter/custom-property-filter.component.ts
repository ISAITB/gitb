import {Component, EventEmitter, HostListener, Input, OnInit, Output} from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { CustomPropertyPresetValue } from 'src/app/types/custom-property-preset-value.type';
import { CustomProperty } from './custom-property';

@Component({
    selector: 'app-custom-property-filter',
    templateUrl: './custom-property-filter.component.html',
    styleUrls: ['./custom-property-filter.component.less'],
    standalone: false
})
export class CustomPropertyFilterComponent implements OnInit {

  @Input() propertyFilter!: CustomProperty
  @Input() properties: CustomProperty[] = []

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

  ngOnInit(): void {
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
