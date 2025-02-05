import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { LoadingStatus } from 'src/app/types/loading-status.type';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';

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
	@Input() nextDisabled = true
  @Input() prevDisabled = true
  @Input() allowSelect = false
  @Input() allowMultiSelect = false
  @Input() actionTooltip = ''
  @Input() deleteTooltip = 'Delete'
  @Input() exportTooltip = 'Export'
  @Input() contentRefreshing = false
  @Input() expandableRowProperty?: string
  @Input() clearSelection?: EventEmitter<void>
  @Input() refreshRows?: EventEmitter<void>

  @Output() onSelect: EventEmitter<any> = new EventEmitter()
  @Output() onDeselect: EventEmitter<any> = new EventEmitter()
  @Output() onAction: EventEmitter<any> = new EventEmitter()
  @Output() onExport: EventEmitter<any> = new EventEmitter()
  @Output() onCheck: EventEmitter<any> = new EventEmitter()
  @Output() onDelete: EventEmitter<any> = new EventEmitter()
  @Output() firstPage: EventEmitter<void> = new EventEmitter()
  @Output() prevPage: EventEmitter<void> = new EventEmitter()
  @Output() nextPage: EventEmitter<void> = new EventEmitter()
  @Output() lastPage: EventEmitter<void> = new EventEmitter()
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

  doFirstPage() {
    if (!this.prevDisabled) {
      this.firstPage.emit()
    }
  }

  doPrevPage() {
    if (!this.prevDisabled) {
      this.prevPage.emit()
    }
  }

  doNextPage() {
    if (!this.nextDisabled) {
      this.nextPage.emit()
    }
  }

  doLastPage() {
    if (!this.nextDisabled) {
      this.lastPage.emit()
    }
  }

}
