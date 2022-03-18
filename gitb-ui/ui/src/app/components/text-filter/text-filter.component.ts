import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: 'app-text-filter',
  templateUrl: './text-filter.component.html',
  styleUrls: [ './text-filter.component.less' ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TextFilterComponent),
      multi: true
    }
  ],
})
export class TextFilterComponent implements OnInit, ControlValueAccessor {

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

  ngOnInit(): void {
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
