/*
 * Copyright (C) 2025 European Union
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

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {LoadingStatus} from 'src/app/types/loading-status.type';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {PagingEvent} from '../paging-controls/paging-event';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseTableComponent extends BaseComponent {

	@Input() data?: any[]
	@Input() columns: TableColumnDefinition[] = [] // e.g.: {'sname': 'Short Name', 'fname': 'Full Name'}
	@Input() classes?: {[key: string]: string} // e.g.: {'sname': 'short-name', 'fname': 'full-name'}
  @Input() loadingStatus?: LoadingStatus
	@Input() noDataMessage = 'No data found'
	@Input() rowStyle?: (row: any) => string
	@Input() actionVisible = false
	@Input() actionVisibleForRow?: (row: any) => boolean
	@Input() actionPendingProperty = 'actionPending'
	@Input() actionIcon = ''
  @Input() deleteIcon = 'fa-solid fa-trash'
  @Input() exportIcon = 'fa-regular fa-file-pdf'
	@Input() operationsVisible = false
	@Input() deleteVisibleForRow?: (row: any) => boolean
	@Input() deletePendingProperty = 'deletePending'
	@Input() exportVisible = false
	@Input() exportVisibleForRow?: (row: any) => boolean
	@Input() exportPendingProperty = 'exportPending'
	@Input() checkboxEnabled = false
	@Input() tableCaption?: string
  @Input() allowSelect = false
  @Input() allowMultiSelect = false
  @Input() actionTooltip = ''
  @Input() deleteTooltip = 'Delete'
  @Input() exportTooltip = 'Export'
  @Input() contentRefreshing = false
  @Input() expandableRowProperty?: string
  @Input() clearSelection?: EventEmitter<void>
  @Input() refreshRows?: EventEmitter<void>
  @Input() supportPaging = false

  @Output() onSelect: EventEmitter<any> = new EventEmitter()
  @Output() onDeselect: EventEmitter<any> = new EventEmitter()
  @Output() onAction: EventEmitter<any> = new EventEmitter()
  @Output() onExport: EventEmitter<any> = new EventEmitter()
  @Output() onCheck: EventEmitter<any> = new EventEmitter()
  @Output() onDelete: EventEmitter<any> = new EventEmitter()
  @Output() pageNavigation: EventEmitter<PagingEvent> = new EventEmitter()
  @Output() onSort: EventEmitter<TableColumnDefinition> = new EventEmitter()

  tableCaptionVisible = false
  columnsLeft: TableColumnDefinition[] = []
  columnsRight: TableColumnDefinition[] = []

  constructor() { super() }

  splitColumns() {
    for (let column of this.columns) {
      if (column.atEnd) {
        this.columnsRight.push(column)
      } else {
        this.columnsLeft.push(column)
      }
    }
  }

  headerColumnClicked(column: TableColumnDefinition) {
    if (column.sortable) {
      for (let col of this.columns!) {
        if (col.field == column.field) {
          if (!col.order) {
            col.order = 'asc'
          } else if (col.order == 'asc') {
            col.order = 'desc'
          } else {
            col.order = 'asc'
          }
        } else {
          col.order = null
        }
      }
      this.onSort.emit(column)
    }
  }

  handleAction(row: any) {
    this.onAction.emit(row)
  }

  handleDelete(row: any) {
    this.onDelete.emit(row)
  }

  handleExport(row: any) {
    this.onExport.emit(row)
  }

  handleCheck(row: any) {
    this.onCheck.emit(row)
  }

  doPageNavigation(event: PagingEvent) {
    this.pageNavigation.emit(event)
  }

}
