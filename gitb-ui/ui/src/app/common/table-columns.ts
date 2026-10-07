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

import {TableColumnDefinition} from '../types/table-column-definition.type';

/**
 * Helpers to define header-less marker (flag icon) and pill (preset value) columns in a consistent way. See
 * TableColumnDefinition.marker.
 */
export class TableColumns {

  /**
   * An icon column for a boolean field, displayed (with its tooltip) only when the field's value equals `whenValue`.
   */
  static flag(field: string, icon: string, tooltip: string, whenValue = true): TableColumnDefinition {
    return {
      field: field,
      title: '',
      marker: true,
      iconFn: (value: boolean) => (value == whenValue) ? icon : '',
      iconTooltipFn: (value: boolean) => (value == whenValue) ? tooltip : ''
    }
  }

  /**
   * A titled pill column for a field with an enumerated value, kept as narrow as its content. The pill function
   * maps the field's value to a pill (or to nothing).
   */
  static valuePill(field: string, title: string, pillFn: NonNullable<TableColumnDefinition['pillFn']>): TableColumnDefinition {
    return { field: field, title: title, headerClass: 'th-min', cellClass: 'td-min', pillFn: pillFn }
  }

  /** Pill for a user's status. */
  static userStatus(): TableColumnDefinition {
    return TableColumns.valuePill('ssoStatusText', 'Status', (status: string) => status ? { text: status } : undefined)
  }

  /** Pill for a user's role within an organisation. */
  static userRole(): TableColumnDefinition {
    return TableColumns.valuePill('roleText', 'Role', (role: string) => role ? { text: role } : undefined)
  }

}
