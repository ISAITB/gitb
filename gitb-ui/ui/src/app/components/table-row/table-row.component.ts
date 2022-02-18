import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { isBoolean, map } from 'lodash'
import { TableColumnData } from 'src/app/types/table-column-data.type';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: '[table-row-directive]',
  templateUrl: './table-row.component.html',
  styles: [
  ]
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
  @Input() actionTooltip = ''
  @Input() deleteTooltip = 'Delete'
  @Input() exportTooltip = 'Export'

  @Output() onAction: EventEmitter<any> = new EventEmitter()
  @Output() onExport: EventEmitter<any> = new EventEmitter()
  @Output() onCheck: EventEmitter<any> = new EventEmitter()
  @Output() onDelete: EventEmitter<any> = new EventEmitter()

  Constants = Constants

  columnDataItems: TableColumnData[] = []

  constructor() { }

  ngOnInit(): void {
    this.columnDataItems = map(this.columns, (column) => {
      let columnDataItem: TableColumnData = {
        data: this.data[column.field],
        boolean: isBoolean(this.data[column.field]),
        class: ''
      }
      if (this.classes) {
        columnDataItem.class = this.classes[column.field]
      } else {
        columnDataItem.class = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      }
      return columnDataItem
    })
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
      return this.columns[index].iconFn!(this.columnDataItems[index])
    } else {
      return ''
    }
  }

}
