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

@Injectable({
  providedIn: 'root'
})
export class CustomDateParserFormatter extends NgbDateParserFormatter {

  // Convert string from input to NgbDateStruct
  parse(value: string): NgbDateStruct | null {
    if (!value) return null;
    const parts = value.split('-').map(p => parseInt(p, 10));
    if (parts.length !== 3) return null;
    return { day: parts[0], month: parts[1], year: parts[2] };
  }

  // Convert NgbDateStruct to string for input
  format(date: NgbDateStruct | null): string {
    if (!date || date.day == undefined || date.month == undefined || date.year == undefined) return '';
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(date.day)}-${pad(date.month)}-${date.year}`;
  }
}
