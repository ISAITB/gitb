import { Component, Input, forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';

@Component({
  selector: 'app-color-picker',
  templateUrl: './color-picker.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ColorPickerComponent),
      multi: true
    }
  ],  
  styleUrls: [ './color-picker.component.less' ]
})
export class ColorPickerComponent {

  @Input() label!: string
  @Input() tooltipText?: string
  @Input() readonly = false
  Constants = Constants
  _value?: string
  onChange = (_: any) => {}
  onTouched = () => {}

  constructor(
    private popupService: PopupService,
    private dataService: DataService
  ) {}

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

  copyColor() {
    this.dataService.copyToClipboard(this._value).subscribe(() => {
      this.popupService.success("Colour copied to clipboard.")
    })
  }
}
