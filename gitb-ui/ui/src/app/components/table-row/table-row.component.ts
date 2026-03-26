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

import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {TableColumnData} from 'src/app/types/table-column-data.type';
import {Constants} from 'src/app/common/constants';
import {Observable, of} from 'rxjs';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';
import {TableRowApi} from './table-row-api';

@Component({
    selector: '[table-row-directive]',
    templateUrl: './table-row.component.html',
    styles: ['div.btn-toolbar {display: flex; flex-wrap: nowrap; justify-content: right;}'],
    standalone: false
})
export class TableRowComponent implements OnInit, TableRowApi {

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
  @Input() deleteIcon = Constants.BUTTON_ICON.DELETE
  @Input() exportIcon = Constants.BUTTON_ICON.REPORT_PDF
  @Input() actionTooltip = ''
  @Input() deleteTooltip = 'Delete'
  @Input() exportTooltip = 'Export'
  @Input() expandableRowProperty?: string
  @Input() refresh?: EventEmitter<void>

  @Input() optionsVisible: boolean = false
  @Input() optionsVisibleForRow?: (row: any) => boolean
  @Input() optionProvider?: (row: any) => Observable<CheckboxOption[][]>
  @Input() optionPendingProperty = 'optionPending'

  @Output() onOption: EventEmitter<{data: any, option: string}> = new EventEmitter()
  @Output() onAction: EventEmitter<any> = new EventEmitter()
  @Output() onExport: EventEmitter<any> = new EventEmitter()
  @Output() onCheck: EventEmitter<any> = new EventEmitter()
  @Output() onDelete: EventEmitter<any> = new EventEmitter()

  @ViewChild("optionButton") optionButton?: CheckBoxOptionPanelComponentApi

  Constants = Constants

  columnDataItemsAtLeft: TableColumnData[] = []
  columnDataItemsAtRight: TableColumnData[] = []
  showOptions = false

  protected static EXPORT_OPTION = '0'
  protected static ACTION_OPTION = '1'
  protected static DELETE_OPTION = '2'

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
        boolean: typeof this.data[column.field] === 'boolean',
        isHiddenFlag: column.isHiddenFlag,
        class: ''
      }
      if (column.cellClass != undefined) {
        columnDataItem.class = column.cellClass
      }
      if (this.classes) {
        if (columnDataItem.class == undefined) {
          columnDataItem.class = this.classes[column.field]
        } else {
          columnDataItem.class += ' ' + this.classes[column.field]
        }
      }
      if (columnDataItem.class == undefined) {
        columnDataItem.class = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      }
      if (column.atEnd) {
        this.columnDataItemsAtRight.push(columnDataItem)
      } else {
        this.columnDataItemsAtLeft.push(columnDataItem)
      }
    }
    this.showOptions = (this.optionProvider != undefined && this.optionsVisible && (this.optionsVisibleForRow == undefined || this.optionsVisibleForRow(this.data)))
        || (this.actionVisible && (this.exportVisible || this.operationsVisible))
        || (this.exportVisible && (this.actionVisible || this.operationsVisible))
        || (this.operationsVisible && (this.actionVisible || this.exportVisible))
  }

  delete() {
    this.onDelete.emit(this.data)
    this.onOption.emit({data: this.data, option: TableRowComponent.DELETE_OPTION})
  }

  export() {
    this.onExport.emit(this.data)
    this.onOption.emit({data: this.data, option: TableRowComponent.EXPORT_OPTION})
  }

  check() {
    this.onCheck.emit(this.data)
  }

  action() {
    this.onAction.emit(this.data)
    this.onOption.emit({data: this.data, option: TableRowComponent.ACTION_OPTION})
  }

  iconForAction(index: number): string {
    if (this.columns[index].iconFn !== undefined) {
      return this.columns[index].iconFn!(this.columnDataItemsAtLeft[index])
    } else {
      return ''
    }
  }

  loadAvailableOptionsFactory() {
    return () => this.loadAvailableOptions()
  }

  private loadAvailableOptions(): Observable<CheckboxOption[][]> {
    if (this.optionProvider != undefined) {
      return this.optionProvider(this.data)
    } else {
      const options: CheckboxOption[] = []
      if (this.actionVisible && (!this.actionVisibleForRow || this.actionVisibleForRow(this.data))) {
        options.push({ key: TableRowComponent.ACTION_OPTION, label: this.actionTooltip, default: true, iconClass: this.actionIcon })
      }
      if (this.exportVisible && (!this.exportVisibleForRow || this.exportVisibleForRow(this.data))) {
        options.push({ key: TableRowComponent.EXPORT_OPTION, label: this.exportTooltip, default: true, iconClass: this.exportIcon })
      }
      if (this.operationsVisible && (!this.deleteVisibleForRow || this.deleteVisibleForRow(this.data))) {
        options.push({ key: TableRowComponent.DELETE_OPTION, label: this.deleteTooltip, default: true, iconClass: this.deleteIcon })
      }
      return of([options])
    }
  }

  handleOption(event: CheckboxOptionState) {
    if (event[TableRowComponent.ACTION_OPTION]) {
      this.action()
    } else if (event[TableRowComponent.DELETE_OPTION]) {
      this.delete()
    } else if (event[TableRowComponent.EXPORT_OPTION]) {
      this.export()
    } else {
      this.onOption.emit({data: this.data, option: Object.keys(event)[0]})
    }
  }

  documentEscape(): void {
    this.optionButton?.documentEscape()
  }

  documentClick(event: Event): void {
    this.optionButton?.documentClick(event)
  }

}
