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

import { Injectable } from '@angular/core';
import { DataService } from './data.service';
import { Constants } from '../common/constants';
import { TableColumnDefinition } from '../types/table-column-definition.type';
import { CheckboxOption } from '../components/checkbox-option-panel/checkbox-option';

/**
 * Each value is also the preference key stored in the database, so the enum can be used directly
 * as a key for DataService.getSessionColumnPreference / setSessionColumnPreference.
 */
export enum SessionColumnCase {
  Own = 'own_sessions',
  All = 'all_sessions',
}

/** Column state for a given context. MUST = always shown. NA = never shown. ON/OFF = user-adaptable. */
type ColState = 'MUST' | 'ON' | 'OFF' | 'NA'

/** Column IDs for the four adaptable column groups (WHAT and WHO). */
export const WHAT_COLUMNS = ['domain', 'specification', 'actor', 'testSuite', 'testCase'] as const
export const WHO_COLUMNS  = ['community', 'organization', 'system'] as const
export type ColumnId = typeof WHAT_COLUMNS[number] | typeof WHO_COLUMNS[number]

export function isOwnCase(c: SessionColumnCase): boolean {
  return c === SessionColumnCase.Own
}

@Injectable({ providedIn: 'root' })
export class SessionColumnsService {

  constructor(private readonly dataService: DataService) {}

  /** Resolve the state of a column given context. */
  getColumnState(colId: ColumnId, columnCase: SessionColumnCase, isSystemAdmin: boolean): ColState {
    const own = isOwnCase(columnCase)
    switch (colId) {
      // WHAT group - identical regardless of case or role.
      case 'domain':        return 'OFF'
      case 'specification': return 'ON'
      case 'actor':         return 'OFF'
      case 'testSuite':     return 'OFF'
      case 'testCase':      return 'ON'
      // WHO group
      case 'community':     return (!own && isSystemAdmin) ? 'OFF' : 'NA'
      case 'organization':  return own ? 'NA' : 'ON'
      case 'system':        return own ? 'ON' : 'OFF'
    }
  }

  /** Returns the set of adaptable column IDs applicable for the given context (state ON or OFF). */
  adaptableColumns(columnCase: SessionColumnCase, isSystemAdmin: boolean): ColumnId[] {
    const all: ColumnId[] = [...WHAT_COLUMNS, ...WHO_COLUMNS]
    return all.filter(id => {
      const s = this.getColumnState(id, columnCase, isSystemAdmin)
      return s === 'ON' || s === 'OFF'
    })
  }

  /**
   * Parse a stored value string into the set of active column IDs.
   * Empty string → use the ON-by-default columns.
   */
  activeIds(storedValue: string, columnCase: SessionColumnCase, isSystemAdmin: boolean): string[] {
    if (!storedValue) {
      return this.adaptableColumns(columnCase, isSystemAdmin)
        .filter(id => this.getColumnState(id, columnCase, isSystemAdmin) === 'ON')
    }
    const stored = storedValue.split(',').filter(s => s.length > 0)
    const applicable = new Set(this.adaptableColumns(columnCase, isSystemAdmin))
    return stored.filter(id => applicable.has(id as ColumnId))
  }

  /** Serialise a list of active column IDs to a preference string. */
  serialize(ids: string[]): string {
    return ids.join(',')
  }

  /**
   * Validate that a selection includes at least one WHAT column, and (for the "all sessions" case)
   * at least one WHO column. In the "own sessions" case the WHO group (just the system column) can
   * be fully unchecked, since the owning organisation is implicit in those tables.
   */
  validate(ids: string[], columnCase: SessionColumnCase, isSystemAdmin: boolean): boolean {
    const set = new Set(ids)
    const hasWhat = (WHAT_COLUMNS as readonly string[])
      .some(id => set.has(id) && this.getColumnState(id as ColumnId, columnCase, isSystemAdmin) !== 'NA')
    if (!hasWhat) return false
    if (isOwnCase(columnCase)) return true
    return (WHO_COLUMNS as readonly string[])
      .some(id => set.has(id) && this.getColumnState(id as ColumnId, columnCase, isSystemAdmin) !== 'NA')
  }

  /**
   * Build the TableColumnDefinition array for a session table.
   * Inserts adaptable columns in matrix order then appends the fixed MUST columns: for the active
   * table this is just "Start time", for the completed table it is "End time" and "Result" (completed
   * tables never show "Start time").
   */
  buildTableColumns(columnCase: SessionColumnCase, storedValue: string, isSystemAdmin: boolean, completed: boolean): TableColumnDefinition[] {
    const active = new Set(this.activeIds(storedValue, columnCase, isSystemAdmin))
    const cols: TableColumnDefinition[] = []

    const allAdaptable: ColumnId[] = [...WHAT_COLUMNS, ...WHO_COLUMNS]
    for (const id of allAdaptable) {
      const state = this.getColumnState(id, columnCase, isSystemAdmin)
      if (state === 'NA' || state === 'MUST') continue
      if (!active.has(id)) continue
      cols.push(this.buildColumnDef(id))
    }

    if (!completed) {
      cols.push({ field: 'startTime', title: 'Start time', sortable: true, order: 'asc', tag: true, tagIcon: Constants.BUTTON_ICON.TIME, headerClass: 'th-min centered', cellClass: 'td-min centered' })
    } else {
      cols.push({ field: 'endTime', title: 'End time', sortable: true, order: 'desc', tag: true, tagIcon: Constants.BUTTON_ICON.TIME, headerClass: 'th-min centered', cellClass: 'td-min centered' })
      cols.push({ field: 'result', title: 'Result', sortable: true, iconFn: this.dataService.iconForTestResult, iconTooltipFn: this.dataService.tooltipForTestResult, headerClass: 'th-min centered', cellClass: 'td-min centered' })
    }

    return cols
  }

  /**
   * Build the options for the column chooser panel, reflecting the current active column set.
   * When disableLastRemaining is true (the default, used by the live table popup) the last
   * remaining WHAT is rendered disabled so it cannot be unchecked, and likewise for the last
   * remaining WHO - except in the "own sessions" case, where the WHO group (just the system
   * column) can always be fully unchecked since the owning organisation is implicit in those
   * tables. Editors that show inline validation instead (rather than disabling checkboxes) pass
   * disableLastRemaining=false.
   */
  buildChooserOptions(
    columnCase: SessionColumnCase,
    currentActiveIds: string[],
    isSystemAdmin: boolean,
    disableLastRemaining = true
  ): CheckboxOption[][] {
    const activeSet = new Set(currentActiveIds)

    const whatOptions: CheckboxOption[] = []
    const whoOptions:  CheckboxOption[] = []

    for (const id of WHAT_COLUMNS) {
      const state = this.getColumnState(id, columnCase, isSystemAdmin)
      if (state === 'NA' || state === 'MUST') continue
      whatOptions.push({ key: id, label: this.columnLabel(id), default: activeSet.has(id) })
    }
    for (const id of WHO_COLUMNS) {
      const state = this.getColumnState(id, columnCase, isSystemAdmin)
      if (state === 'NA' || state === 'MUST') continue
      whoOptions.push({ key: id, label: this.columnLabel(id), default: activeSet.has(id) })
    }

    // Disabled-last-in-group: if only one WHAT/WHO is checked, that one cannot be unchecked.
    if (disableLastRemaining) {
      const checkedWhat = whatOptions.filter(o => o.default)
      if (checkedWhat.length === 1) {
        checkedWhat[0].disabled = true
      }
      if (!isOwnCase(columnCase)) {
        const checkedWho = whoOptions.filter(o => o.default)
        if (checkedWho.length === 1) {
          checkedWho[0].disabled = true
        }
      }
    }

    const groups: CheckboxOption[][] = []
    if (whatOptions.length > 0) groups.push(whatOptions)
    if (whoOptions.length > 0) groups.push(whoOptions)
    return groups
  }

  /** Human-readable label for a column id. Uses DataService helpers for community-customisable terms. */
  columnLabel(id: string): string {
    switch (id) {
      case 'domain':        return this.dataService.labelDomain()
      case 'specification': return this.dataService.labelSpecification()
      case 'actor':         return this.dataService.labelActor()
      case 'testSuite':     return 'Test suite'
      case 'testCase':      return 'Test case'
      case 'community':     return 'Community'
      case 'organization':  return this.dataService.labelOrganisation()
      case 'system':        return this.dataService.labelSystem()
      default:              return id
    }
  }

  private buildColumnDef(id: ColumnId): TableColumnDefinition {
    switch (id) {
      case 'domain':
        return { field: 'domain', title: this.dataService.labelDomain(), sortable: true }
      case 'specification':
        return { field: 'specification', title: this.dataService.labelSpecification(), sortable: true }
      case 'actor':
        return { field: 'actor', title: this.dataService.labelActor(), sortable: true }
      case 'testSuite':
        return { field: 'testSuite', title: 'Test suite', sortable: true }
      case 'testCase':
        return { field: 'testCase', title: 'Test case', sortable: true }
      case 'community':
        return { field: 'community', title: 'Community', sortable: true }
      case 'organization':
        return { field: 'organization', title: this.dataService.labelOrganisation(), sortable: true }
      case 'system':
        return { field: 'system', title: this.dataService.labelSystem(), sortable: true }
    }
  }

}
