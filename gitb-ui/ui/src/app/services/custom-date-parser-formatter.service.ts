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

import {Injectable} from '@angular/core';
import {NgbDateParserFormatter, NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';
import {DataService} from './data.service';

type DatePart = 'day'|'month'|'year'

@Injectable({
  providedIn: 'root'
})
export class CustomDateParserFormatter extends NgbDateParserFormatter {

  constructor(private dataService: DataService) {
    super();
  }

  // The Test Bed-wide configured date-only display pattern (e.g. "dd/MM/yyyy"), falling back to the
  // built-in default until the application configuration has been loaded.
  get pattern(): string {
    return this.dataService.configuration?.dateFormat ?? 'dd/MM/yyyy'
  }

  // The order in which day/month/year components appear in the configured pattern.
  private partOrder(): DatePart[] {
    const parts: DatePart[] = []
    for (const token of this.pattern.match(/[dMy]+/g) ?? []) {
      switch (token.charAt(0)) {
        case 'd': parts.push('day'); break
        case 'M': parts.push('month'); break
        case 'y': parts.push('year'); break
      }
    }
    return parts.length === 3 ? parts : ['day', 'month', 'year']
  }

  // The separator used between day/month/year components in the configured pattern.
  private separator(): string {
    return this.pattern.match(/[^dMy]/)?.[0] ?? '/'
  }

  // Convert string from input to NgbDateStruct
  parse(value: string): NgbDateStruct | null {
    if (!value) return null;
    const parts = value.split(/\D+/).filter(p => p.length > 0).map(p => parseInt(p, 10));
    if (parts.length !== 3 || parts.some(p => isNaN(p))) return null;
    const order = this.partOrder()
    const result: Partial<Record<DatePart, number>> = {}
    order.forEach((part, index) => result[part] = parts[index])
    if (result.day == undefined || result.month == undefined || result.year == undefined) return null;
    return { day: result.day, month: result.month, year: result.year };
  }

  // Convert NgbDateStruct to string for input
  format(date: NgbDateStruct | null): string {
    if (!date || date.day == undefined || date.month == undefined || date.year == undefined) return '';
    const pad = (n: number) => n.toString().padStart(2, '0');
    const values: Record<DatePart, string> = { day: pad(date.day), month: pad(date.month), year: date.year.toString() }
    return this.partOrder().map(part => values[part]).join(this.separator());
  }
}
