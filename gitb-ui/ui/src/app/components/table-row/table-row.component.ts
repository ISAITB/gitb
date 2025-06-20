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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { isBoolean } from 'lodash'
import { TableColumnData } from 'src/app/types/table-column-data.type';
import { Constants } from 'src/app/common/constants';

@Component({
    selector: '[table-row-directive]',
    templateUrl: './table-row.component.html',
    styles: ['div.btn-toolbar {display: flex; flex-wrap: nowrap; justify-content: right;}'],
    standalone: false
})
export class TableRowComponent implements OnInit {

  @Input() data?: any
  @Input() columns: TableColumnDefinition[] = []
  @Input() classes?: {[key: string]: string}
  @Input() operationsVisible: boolean = false
  @Input() actionVisible: boolean = false
  @Input() actionVisibleForRow?: (row: any) => boolean
  @Input() actionPendingProperty = 'actionPending'
  @Input() exportVisible: boolean = false
  @Input() exportVisibleForRow?: (row: any) => boolean
  @Input() exportPendingProperty = 'exportPending'
  @Input() checkboxEnabled: boolean = false
  @Input() deleteVisibleForRow?: (row: any) => boolean
  @Input() deletePendingProperty = 'deletePending'
  @Input() actionIcon = ''
  @Input() deleteIcon = 'fa-solid fa-trash'
  @Input() exportIcon = 'fa-regular fa-file-pdf'
  @Input() actionTooltip = ''
  @Input() deleteTooltip = 'Delete'
  @Input() exportTooltip = 'Export'
  @Input() expandableRowProperty?: string
  @Input() refresh?: EventEmitter<void>

  @Output() onAction: EventEmitter<any> = new EventEmitter()
  @Output() onExport: EventEmitter<any> = new EventEmitter()
  @Output() onCheck: EventEmitter<any> = new EventEmitter()
  @Output() onDelete: EventEmitter<any> = new EventEmitter()

  Constants = Constants

  columnDataItemsAtLeft: TableColumnData[] = []
  columnDataItemsAtRight: TableColumnData[] = []

  constructor() { }

  ngOnInit(): void {
    this.refreshData()
    if (this.refresh) {
      this.refresh.subscribe(() => {
        this.refreshData()
      })
    }
  }

  private refreshData() {
    this.columnDataItemsAtLeft = []
    this.columnDataItemsAtRight = []
    for (let column of this.columns) {
      let columnDataItem: TableColumnData = {
        data: this.data[column.field],
        boolean: isBoolean(this.data[column.field]),
        isHiddenFlag: column.isHiddenFlag,
        class: ''
      }
      if (this.classes) {
        columnDataItem.class = this.classes[column.field]
      } else {
        columnDataItem.class = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      }
      if (column.atEnd) {
        this.columnDataItemsAtRight.push(columnDataItem)
      } else {
        this.columnDataItemsAtLeft.push(columnDataItem)
      }
    }
  }

  delete() {
    this.onDelete.emit(this.data)
  }

  export() {
    this.onExport.emit(this.data)
  }

  check() {
    this.onCheck.emit(this.data)
  }

  action() {
    this.onAction.emit(this.data)
  }

  iconForAction(index: number): string {
    if (this.columns[index].iconFn !== undefined) {
      return this.columns[index].iconFn!(this.columnDataItemsAtLeft[index])
    } else {
      return ''
    }
  }

}
