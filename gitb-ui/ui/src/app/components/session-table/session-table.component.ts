import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { BaseTableComponent } from '../base-table/base-table.component';
import { SessionData } from '../diagram/test-session-presentation/session-data';

@Component({
  selector: '[app-session-table]',
  templateUrl: './session-table.component.html',
  styles: [
  ]
})
export class SessionTableComponent extends BaseTableComponent implements OnInit {

  @Input() sessionTableId = 'session-table'
  @Input() expandedCounter?: { count: number }

  Constants = Constants
  columnCount = 0

  constructor() { super() }

  ngOnInit(): void {
    for (let column of this.columns) {
      column.headerClass = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      if (column.sortable) {
        column.headerClass = column.headerClass + ' sortable'
      }
    }
    this.columnCount = this.columns.length
    if (this.checkboxEnabled) this.columnCount += 1
    if (this.actionVisible) this.columnCount += 1
    if (this.operationsVisible) this.columnCount += 1
    if (this.exportVisible) this.columnCount += 1
  }

  getSessionId(data: SessionData) {
    return data.session
  }

  diagramReady(test: SessionData) {
    test.diagramLoaded = true;
    setTimeout(() => {
      test.hideLoadingIcon = true;
      test.diagramExpanded = true;
    }, 200)
  }

  onExpand(data: SessionData) {
    data.expanded = data.expanded === undefined || !data.expanded
    if (this.expandedCounter !== undefined) {
      if (data.expanded) {
        this.expandedCounter.count = this.expandedCounter.count + 1
      } else {
        this.expandedCounter.count = this.expandedCounter.count - 1
      }
    }
  }

  rowClass(row: SessionData) {
    let rowClass = ''
    if (this.rowStyle) {
      let customClass = this.rowStyle(row)
      if (customClass !== undefined) {
        rowClass = rowClass + ' ' + customClass
      }
    }
    if (this.allowSelect || this.allowMultiSelect || this.onSelect) {
      rowClass = rowClass + ' selectable'
    }
    return rowClass
  }

}
