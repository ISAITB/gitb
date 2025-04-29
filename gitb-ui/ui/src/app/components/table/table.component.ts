import {Component, OnInit, ViewChild} from '@angular/core';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { BaseTableComponent } from '../base-table/base-table.component';
import {PagingControlsComponent} from '../paging-controls/paging-controls.component';

@Component({
    selector: '[table-directive]',
    templateUrl: './table.component.html',
    styles: [],
    standalone: false
})
export class TableComponent extends BaseTableComponent implements OnInit {

  columnCount = 0
  allChecked = false
  @ViewChild("pagingControls") pagingControls?: PagingControlsComponent

  constructor() { super() }

  ngOnInit(): void {
    this.columnCount = this.columns!.length
    if (this.expandableRowProperty != undefined) {
      this.columnCount += 1
    }
    if (this.checkboxEnabled) {
      this.columnCount += 1
    }
    if (this.actionVisible || this.operationsVisible || this.exportVisible) {
      this.columnCount += 1
    }
    for (let column of this.columns!) {
      if (column.headerClass == undefined) {
        column.headerClass = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      }
    }
    this.splitColumns()
    this.tableCaptionVisible = this.tableCaption !== undefined
    if (this.clearSelection) {
      this.clearSelection.subscribe(() => {
        if (this.checkboxEnabled) {
          this.allChecked = false
          this.checkAll()
        }
        if (this.data) {
          for (let row of this.data) {
            if (row._selected != undefined && row._selected) {
              row._selected = false
            }
          }
        }
      })
    }
  }

  checkAll() {
    if (this.data) {
      if (this.allChecked) {
        for (let row of this.data) {
          row.checked = true
        }
      } else {
        for (let row of this.data) {
          if (row.checked != undefined) row.checked = false
        }
      }

    }
  }

  rowClass(selectedIndex: number): string {
    let rowClass = ''
    if (this.rowStyle) {
      let row = this.data![selectedIndex]
      let customClass = this.rowStyle(row)
      if (customClass !== undefined) {
        rowClass = rowClass + ' ' + customClass
      }
    }
    if (this.allowSelect || this.allowMultiSelect) {
      rowClass = rowClass + ' selectable'
    }
    return rowClass
  }

  select(rowIndex: number) {
    if (this.allowSelect || this.allowMultiSelect) {
      const row = this.data![rowIndex]
      if (row._selected !== undefined && row._selected!) {
        if (this.allowMultiSelect) {
          row._selected = false
          this.onDeselect.emit(row)
        }
      } else {
        row._selected = true
        this.onSelect.emit(row)
      }
      if (!this.allowMultiSelect) {
        for (let i = 0; i < this.data!.length; i++) {
          if (i != rowIndex && this.data![i]._selected) {
            this.data![i]._selected = false
            this.onDeselect.emit(this.data![i])
          }
        }
      }
    }
  }

  classForColumn(column: TableColumnDefinition): string {
    if (column.headerClass) {
      return column.headerClass!
    } else {
      return ''
    }
  }

}
