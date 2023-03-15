import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { isBoolean } from 'lodash'
import { TableColumnData } from 'src/app/types/table-column-data.type';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: '[table-row-directive]',
  templateUrl: './table-row.component.html',
  styles: [ 'div.btn-toolbar {display: flex; flex-wrap: nowrap; justify-content: right;}' ]
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
  @Input() deleteIcon = 'fa fa-trash'
  @Input() exportIcon = 'fa fa-file-pdf-o'
  @Input() actionTooltip = ''
  @Input() deleteTooltip = 'Delete'
  @Input() exportTooltip = 'Export'
  @Input() expandableRowProperty?: string

  @Output() onAction: EventEmitter<any> = new EventEmitter()
  @Output() onExport: EventEmitter<any> = new EventEmitter()
  @Output() onCheck: EventEmitter<any> = new EventEmitter()
  @Output() onDelete: EventEmitter<any> = new EventEmitter()

  Constants = Constants

  columnDataItemsAtLeft: TableColumnData[] = []
  columnDataItemsAtRight: TableColumnData[] = []

  constructor() { }

  ngOnInit(): void {
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
