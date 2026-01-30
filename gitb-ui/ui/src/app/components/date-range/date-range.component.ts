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
  Component,
  ElementRef,
  EmbeddedViewRef,
  forwardRef,
  HostListener,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {Constants} from '../../common/constants';
import {NgbDate, NgbDateParserFormatter, NgbDateStruct, NgbInputDatepicker} from '@ng-bootstrap/ng-bootstrap';
import {CustomDateParserFormatter} from '../../services/custom-date-parser-formatter.service';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {DateRange} from './date-range';

@Component({
  selector: 'app-date-range',
  standalone: false,
  templateUrl: './date-range.component.html',
  styleUrl: './date-range.component.less',
  providers: [
    { provide: NgbDateParserFormatter, useClass: CustomDateParserFormatter },
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DateRangeComponent), multi: true }
  ]
})
export class DateRangeComponent implements ControlValueAccessor {

  @ViewChild("input") inputElement?: ElementRef<HTMLElement>;
  @ViewChild('popupTemplate') popupTemplate?: TemplateRef<any>;
  @ViewChild('customBegin') customBegin?: NgbInputDatepicker;
  @ViewChild('customEnd') customEnd?: NgbInputDatepicker;

  private onChange: (value: DateRange) => void = () => {};
  private onTouched: () => void = () => {};

  private containerDiv?: HTMLElement;
  private embeddedView?: EmbeddedViewRef<any>;
  protected readonly Constants = Constants;
  private today = this.toStruct(new Date());
  private todayDate = NgbDate.from(this.today)!;
  private defaultMinDate: NgbDateStruct = { year: this.today.year - 50, month: 1, day: 1 }
  private defaultMaxDate: NgbDateStruct = { year: this.today.year + 50, month: 12, day: 31 }
  private open = false

  beginDate?: NgbDateStruct
  endDate?: NgbDateStruct

  displayValue?: string

  constructor(
    private viewContainerRef: ViewContainerRef,
    private renderer: Renderer2,
    private dateFormatter: CustomDateParserFormatter
  ) {}

  writeValue(value: DateRange|undefined): void {
    if (!value) {
      this.beginDate = undefined;
      this.endDate = undefined;
    } else {
      this.beginDate = value.start ? this.toStruct(value.start) : undefined;
      this.endDate = value.end ? this.toStruct(value.end) : undefined;
    }
    this.updateDisplayValue()
  }

  emitChanges() {
    if (this.beginDate && (this.beginDate.day == undefined || this.beginDate.month == undefined || this.beginDate.year == undefined)) {
      this.beginDate = undefined
    }
    if (this.endDate && (this.endDate.day == undefined || this.endDate.month == undefined || this.endDate.year == undefined)) {
      this.endDate = undefined
    }
    const range: DateRange = {
      start: this.beginDate ? this.toDateStart(this.beginDate) : undefined,
      end: this.endDate ? this.toDateEnd(this.endDate) : undefined,
    };
    this.updateDisplayValue()
    this.onChange(range)
    this.onTouched()
  }

  private updateDisplayValue() {
    let newValue: string
    if (this.beginDate && this.endDate) {
      newValue = `${this.dateFormatter.format(this.beginDate)} to ${this.dateFormatter.format(this.endDate)}`
    } else if (this.beginDate) {
      newValue = `As of ${this.dateFormatter.format(this.beginDate)}`
    } else if (this.endDate) {
      newValue = `Up to ${this.dateFormatter.format(this.endDate)}`
    } else {
      newValue = ''
    }
    this.displayValue = newValue
  }

  registerOnChange(fn: (value: DateRange) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  toggle() {
    this.open = !this.open;
    if (this.open) {
      this.openPanel()
    } else {
      this.close()
    }
  }

  clear() {
    this.beginDate = undefined
    this.endDate = undefined
    this.emitChanges()
    this.close()
  }

  setToday() {
    this.beginDate = this.copyToday()
    this.endDate = this.copyToday()
    this.emitChanges()
    this.close()
  }

  setYesterday() {
    this.beginDate = this.offsetDate(this.copyToday(), -1)
    this.endDate = this.offsetDate(this.copyToday(), -1)
    this.emitChanges()
    this.close()
  }

  setLastDays(dayCount: number) {
    this.beginDate = this.offsetDate(this.copyToday(), -1 * dayCount)
    this.endDate = this.copyToday()
    this.emitChanges()
    this.close()
  }

  setCurrentWeek() {
    const jsToday = new Date(this.today.year, this.today.month - 1, this.today.day);
    // getDay(): 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    const dayOfWeek = jsToday.getDay();
    // Calculate offset to Monday
    // if today is Sunday (0), we want previous Monday => -6
    const offsetToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    // Start of week (Monday)
    const mondayDate = new Date(jsToday);
    mondayDate.setDate(jsToday.getDate() + offsetToMonday);
    this.beginDate = { year: mondayDate.getFullYear(), month: mondayDate.getMonth() + 1, day: mondayDate.getDate() };
    this.endDate = this.copyToday();
    this.emitChanges();
    this.close();
  }

  setCurrentMonth() {
    this.beginDate = { year: this.today.year, month: this.today.month, day: 1 }
    this.endDate = this.copyToday()
    this.emitChanges()
    this.close()
  }

  setCurrentYear() {
    this.beginDate = { year: this.today.year, month: 1, day: 1 }
    this.endDate = this.copyToday()
    this.emitChanges()
    this.close()
  }

  setPreviousWeek() {
    const today = new Date(this.today.year, this.today.month - 1, this.today.day);
    // getDay(): 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    const dayOfWeek = today.getDay();
    // Offset to current week's Monday
    const offsetToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    // Current week's Monday
    const currentMonday = new Date(today);
    currentMonday.setDate(today.getDate() + offsetToMonday);
    // Previous week's Monday
    const previousMonday = new Date(currentMonday);
    previousMonday.setDate(currentMonday.getDate() - 7);
    // Previous week's Sunday
    const previousSunday = new Date(previousMonday);
    previousSunday.setDate(previousMonday.getDate() + 6);
    this.beginDate = {
      year: previousMonday.getFullYear(),
      month: previousMonday.getMonth() + 1,
      day: previousMonday.getDate(),
    };
    this.endDate = {
      year: previousSunday.getFullYear(),
      month: previousSunday.getMonth() + 1,
      day: previousSunday.getDate(),
    };
    this.emitChanges();
    this.close();
  }

  setPreviousMonth() {
    // First day of current month
    const firstOfCurrentMonth = new Date(this.today.year, this.today.month - 1, 1);
    // Last day of previous month
    const lastOfPreviousMonth = new Date(firstOfCurrentMonth);
    lastOfPreviousMonth.setDate(0);
    // First day of previous month
    const firstOfPreviousMonth = new Date(
      lastOfPreviousMonth.getFullYear(),
      lastOfPreviousMonth.getMonth(),
      1
    );
    this.beginDate = {
      year: firstOfPreviousMonth.getFullYear(),
      month: firstOfPreviousMonth.getMonth() + 1,
      day: 1,
    };
    this.endDate = {
      year: lastOfPreviousMonth.getFullYear(),
      month: lastOfPreviousMonth.getMonth() + 1,
      day: lastOfPreviousMonth.getDate(),
    };
    this.emitChanges();
    this.close();
  }

  setPreviousYear() {
    const year = this.today.year - 1;
    this.beginDate = {
      year,
      month: 1,
      day: 1,
    };
    this.endDate = {
      year,
      month: 12,
      day: 31,
    };
    this.emitChanges();
    this.close();
  }

  applyCustomRange() {
    this.emitChanges()
    this.close()
  }

  close() {
    this.open = false;
    if (this.containerDiv) {
      this.renderer.removeChild(document.body, this.containerDiv);
      this.containerDiv = undefined
    }
    if (this.embeddedView) {
      this.embeddedView.destroy();
      this.embeddedView = undefined;
    }
    window.removeEventListener('scroll', this.updatePosition);
    window.removeEventListener('resize', this.updatePosition);
  }

  private openPanel() {
    if (this.inputElement) {
      this.containerDiv = this.renderer.createElement("div");
      this.renderer.setStyle(this.containerDiv, 'position', 'absolute');
      this.renderer.setStyle(this.containerDiv, 'visibility', 'hidden');
      this.renderer.setStyle(this.containerDiv, 'top', '0');
      this.renderer.setStyle(this.containerDiv, 'left', '0');
      this.renderer.appendChild(document.body, this.containerDiv);
      this.embeddedView = this.viewContainerRef.createEmbeddedView(this.popupTemplate!);
      this.embeddedView.rootNodes.forEach(node => {
        this.renderer.appendChild(this.containerDiv, node);
      });
      setTimeout(() => {
        this.updatePosition();
        this.embeddedView!.rootNodes.forEach(node => {
          this.renderer.setStyle(node, 'visibility', 'visible');
        });
      }, 0);
      window.addEventListener('scroll', this.updatePosition);
      window.addEventListener('resize', this.updatePosition);
    }
  }

  private updatePosition = () => {
    if (!this.containerDiv || !this.inputElement) return;
    const inputRect = this.inputElement.nativeElement.getBoundingClientRect();
    const popup = this.containerDiv.firstElementChild as HTMLElement;
    if (!popup) return;
    const popupHeight = popup.offsetHeight;
    const popupWidth = popup.offsetWidth;
    let top = inputRect.bottom;
    let left = inputRect.left;
    // Vertical flip if needed.
    if (top + popupHeight > window.innerHeight) {
      const flippedTop = inputRect.top - popupHeight;
      if (flippedTop >= 0) {
        top = flippedTop;
      }
    }
    // Horizontal flip if needed.
    if (left + popupWidth > window.innerWidth) {
      const flippedLeft = inputRect.right - popupWidth;
      if (flippedLeft >= 0) {
        left = flippedLeft;
      }
    }
    top = Math.max(0, Math.min(top, window.innerHeight - popupHeight));
    left = Math.max(0, Math.min(left, window.innerWidth - popupWidth));
    popup.style.top = `${top + window.scrollY}px`;
    popup.style.left = `${left + window.scrollX}px`;
  }

  @HostListener('document:pointerdown', ['$event'])
  clickRegistered(event: any) {
    // Listening on pointerdown to avoid closing on click to select a date from the datepicker.
    if (this.open && this.inputElement && this.containerDiv) {
      const target = event.target as HTMLElement;
      const inPanel = this.inputElement.nativeElement.contains(target) || this.containerDiv.contains(target)
      if (!inPanel && !this.customBegin?.isOpen() && !this.customEnd?.isOpen()) {
        this.close();
      }
    }
  }

  @HostListener('document:keydown.escape')
  escapeRegistered() {
    // Listening on keydown to avoid closing on escape with an open datepicker.
    if (this.open && !this.customBegin?.isOpen() && !this.customEnd?.isOpen()) {
      this.close()
    }
  }

  copyToday(): NgbDateStruct {
    return { ...this.today }
  }

  private toDateStart(model: NgbDateStruct|undefined) {
    return model ? new Date(model.year, model.month - 1, model.day, 0, 0, 0, 0) : undefined;
  }

  private toDateEnd(model: NgbDateStruct|undefined) {
    return model ? new Date(model.year, model.month - 1, model.day, 23, 59, 59, 999) : undefined;
  }

  get endMinDate(): NgbDateStruct {
    return this.beginDate ?? this.defaultMinDate;
  }

  get startMaxDate(): NgbDateStruct {
    return this.endDate ?? this.defaultMaxDate;
  }

  todayEnabledForBegin() {
    return !this.todayDate.after(NgbDate.from(this.startMaxDate))
  }

  todayEnabledForEnd() {
    return !this.todayDate.before(NgbDate.from(this.endMinDate))
  }

  private toStruct(d: Date): NgbDateStruct {
    return {
      year: d.getFullYear(),
      month: d.getMonth() + 1,
      day: d.getDate(),
    };
  }

  private offsetDate(date: NgbDateStruct, offsetDays: number): NgbDateStruct {
    const jsDate = new Date(date.year, date.month - 1, date.day);
    jsDate.setDate(jsDate.getDate() + offsetDays);
    return this.toStruct(jsDate)
  }

}
