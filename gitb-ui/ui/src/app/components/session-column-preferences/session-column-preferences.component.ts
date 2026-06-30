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

import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {ColumnId, isOwnCase, SessionColumnCase, SessionColumnsService, WHAT_COLUMNS, WHO_COLUMNS} from '../../services/session-columns.service';

@Component({
  selector: 'app-session-column-preferences',
  templateUrl: './session-column-preferences.component.html',
  standalone: false
})
export class SessionColumnPreferencesComponent implements OnChanges {

  @Input() columnCase: SessionColumnCase = SessionColumnCase.Own
  @Input() isSystemAdmin = false
  @Input() value = ''
  @Input() tooltip = ''
  @Output() valueChange = new EventEmitter<string>()
  @Output() validityChange = new EventEmitter<boolean>()

  // Flat, horizontal list of options (WHAT columns followed by WHO columns) - no group separation.
  options: CheckboxOption[] = []
  whatInvalid = false
  whoInvalid = false
  errorMessage = ''

  /**
   * Authoritative selection, decoupled from the serialised `value` string once initialised. If we
   * instead re-derived the active set from `value` on every change, driving every checkbox in both
   * groups to unchecked would serialise to '' - which is indistinguishable from "no stored
   * preference yet" (meaning "use the computed defaults"). Re-interpreting that on every toggle
   * would silently repopulate the defaults instead of respecting the user's (invalid, but real)
   * empty selection, making unrelated checkboxes appear to randomly re-check themselves.
   */
  private activeIds: string[] = []
  private initialised = false

  constructor(private readonly sessionColumnsService: SessionColumnsService) {}

  ngOnChanges(changes: SimpleChanges): void {
    const contextChanged = (changes['columnCase'] && !changes['columnCase'].firstChange) || (changes['isSystemAdmin'] && !changes['isSystemAdmin'].firstChange)
    if (!this.initialised || contextChanged) {
      // True (re)initialisation from the parent-supplied value - here (and only here) is it correct
      // to interpret an empty string as "unset - use the computed defaults".
      this.activeIds = this.sessionColumnsService.activeIds(this.value, this.columnCase, this.isSystemAdmin)
      this.initialised = true
    }
    this.recompute()
  }

  private recompute(): void {
    const groups = this.sessionColumnsService.buildChooserOptions(this.columnCase, this.activeIds, this.isSystemAdmin, false)
    this.options = groups.flatMap(g => g)

    const activeSet = new Set(this.activeIds)
    const applicableWhat = (WHAT_COLUMNS as readonly ColumnId[]).filter(id => this.sessionColumnsService.getColumnState(id, this.columnCase, this.isSystemAdmin) !== 'NA')
    const applicableWho = (WHO_COLUMNS as readonly ColumnId[]).filter(id => this.sessionColumnsService.getColumnState(id, this.columnCase, this.isSystemAdmin) !== 'NA')

    this.whatInvalid = !applicableWhat.some(id => activeSet.has(id))
    // In the "own sessions" case the WHO group (just the system column) can be fully unchecked,
    // since the owning organisation is implicit in those tables.
    this.whoInvalid = !isOwnCase(this.columnCase) && !applicableWho.some(id => activeSet.has(id))

    if (this.whatInvalid && this.whoInvalid) {
      this.errorMessage = `Select at least one of: ${applicableWhat.map(id => this.sessionColumnsService.columnLabel(id)).join(', ')}, and at least one of: ${applicableWho.map(id => this.sessionColumnsService.columnLabel(id)).join(', ')}.`
    } else if (this.whatInvalid) {
      this.errorMessage = `Select at least one of: ${applicableWhat.map(id => this.sessionColumnsService.columnLabel(id)).join(', ')}.`
    } else if (this.whoInvalid) {
      this.errorMessage = `Select at least one of: ${applicableWho.map(id => this.sessionColumnsService.columnLabel(id)).join(', ')}.`
    } else {
      this.errorMessage = ''
    }
    this.validityChange.emit(!this.whatInvalid && !this.whoInvalid)
  }

  isWhatColumn(key: string): boolean {
    return (WHAT_COLUMNS as readonly string[]).includes(key)
  }

  optionInvalid(option: CheckboxOption): boolean {
    return this.isWhatColumn(option.key) ? this.whatInvalid : this.whoInvalid
  }

  toggle(option: CheckboxOption, checked: boolean): void {
    if (checked) {
      if (!this.activeIds.includes(option.key)) {
        this.activeIds = [...this.activeIds, option.key]
      }
    } else {
      this.activeIds = this.activeIds.filter(id => id !== option.key)
    }
    this.value = this.sessionColumnsService.serialize(this.activeIds)
    this.valueChange.emit(this.value)
    this.recompute()
  }

}
