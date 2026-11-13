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

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  forwardRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {ValueLabel} from '../../types/value-label';

/**
 * A group of radio buttons or checkboxes (control before label). The options are displayed on a single line if they
 * fit without wrapping and otherwise one below the other.
 *
 * The model depends on the mode:
 * - radio: the selected option's value (string) or undefined.
 * - checkbox with options: the array of the selected option values (in option order).
 * - checkbox without options: a single, label-less checkbox with a boolean model.
 */
@Component({
  selector: 'app-option-group',
  standalone: false,
  templateUrl: './option-group.component.html',
  styleUrl: './option-group.component.less',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OptionGroupComponent),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.Eager
})
export class OptionGroupComponent implements ControlValueAccessor, AfterViewInit, OnChanges, OnDestroy {

  @Input() mode: 'radio'|'checkbox' = 'radio'
  @Input() options?: (ValueLabel|{value: string, label?: string})[]
  @Input() name!: string
  @Input() inputId!: string
  @Input() labelledBy?: string
  @Input() disabled = false
  @Input() bordered = false
  // Optional help text shown as an inline tooltip at the end of the options' line (after the last option if they are
  // all on one line, otherwise after the first option, i.e. at the end of the first line).
  @Input() tooltip?: string
  @ViewChild('container') container?: ElementRef<HTMLElement>

  stacked = false
  selectedValue?: string
  selectedValues: string[] = []
  checked = false

  private inlineWidth?: number
  private resizeObserver?: ResizeObserver
  private viewReady = false
  private onChange: (_: any) => void = () => {}
  private onTouched: () => void = () => {}

  constructor(private readonly zone: NgZone) {}

  get lone(): boolean {
    return this.mode == 'checkbox' && (this.options == undefined || this.options.length == 0)
  }

  ngAfterViewInit(): void {
    this.viewReady = true
    if (this.container) {
      this.resizeObserver = new ResizeObserver(() => {
        this.zone.run(() => this.evaluateLayout())
      })
      this.resizeObserver.observe(this.container.nativeElement)
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.viewReady && (changes['options'] || changes['mode'])) {
      // The content changed so determine once again whether it fits on one line.
      this.stacked = false
      this.inlineWidth = undefined
      setTimeout(() => this.evaluateLayout())
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect()
  }

  /*
   * All options are shown on one line (no wrapping) if they fit. When they do not, they are stacked. The width needed
   * for the inline display is remembered at the time of stacking, so that the display returns to a single line only
   * once that width is available again (this avoids oscillating at the boundary).
   */
  private evaluateLayout() {
    const element = this.container?.nativeElement
    if (element && element.clientWidth > 0) {
      if (!this.stacked) {
        if (element.scrollWidth > element.clientWidth) {
          this.inlineWidth = element.scrollWidth
          this.stacked = true
        }
      } else if (this.inlineWidth == undefined || element.clientWidth >= this.inlineWidth) {
        this.inlineWidth = undefined
        this.stacked = false
        // Check once again as the width may now be needed for a different content.
        setTimeout(() => this.evaluateLayout())
      }
    }
  }

  isSelected(value: string): boolean {
    if (this.mode == 'radio') {
      return this.selectedValue === value
    }
    return this.selectedValues.includes(value)
  }

  optionChanged(value: string, event: Event) {
    const checked = (event.target as HTMLInputElement).checked
    if (this.mode == 'radio') {
      this.selectedValue = value
      this.onChange(this.selectedValue)
    } else {
      const selected = new Set(this.selectedValues)
      if (checked) {
        selected.add(value)
      } else {
        selected.delete(value)
      }
      // Keep the selected values in the order of the options.
      this.selectedValues = (this.options ?? []).map(option => option.value).filter(optionValue => selected.has(optionValue))
      this.onChange([...this.selectedValues])
    }
    this.onTouched()
  }

  loneChanged(event: Event) {
    this.checked = (event.target as HTMLInputElement).checked
    this.onChange(this.checked)
    this.onTouched()
  }

  writeValue(value: any): void {
    if (this.mode == 'radio') {
      this.selectedValue = (value == undefined) ? undefined : String(value)
    } else if (this.lone) {
      this.checked = value === true
    } else {
      this.selectedValues = Array.isArray(value) ? [...value] : []
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled
  }

}
