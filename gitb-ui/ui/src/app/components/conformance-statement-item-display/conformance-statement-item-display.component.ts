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

import {Component, EventEmitter, Input, OnInit, Output, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {filter} from 'lodash';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {ConformanceStatementResult} from 'src/app/types/conformance-statement-result';
import {Counters} from '../test-status-icons/counters';
import {DataService} from 'src/app/services/data.service';
import {ExportReportEvent} from 'src/app/types/export-report-event';
import {BaseComponent} from '../../pages/base-component.component';
import {ConformanceStatementItemDisplayComponentApi} from './conformance-statement-item-display-component-api';
import {
  ConformanceStatementItemsDisplayComponentApi
} from '../conformance-statement-items-display/conformance-statement-items-display-component-api';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';
import {Constants} from '../../common/constants';
import {StatementOptionsButtonApi} from '../statement-options-button/statement-options-button-api';
import {ConformanceIds} from '../../types/conformance-ids';

@Component({
    selector: 'app-conformance-statement-item-display',
    templateUrl: './conformance-statement-item-display.component.html',
    styleUrls: ['./conformance-statement-item-display.component.less'],
    standalone: false
})
export class ConformanceStatementItemDisplayComponent extends BaseComponent implements OnInit, ConformanceStatementItemDisplayComponentApi {

  @Input() item!: ConformanceStatementItem
  @Input() shade = false
  @Input() hideSelf = false
  @Input() animated = true
  @Input() expandable = true
  @Input() wrapDescriptions = false
  @Input() withCheck = true
  @Input() withExport = false
  @Input() withResults = false
  @Input() withOptions = false
  @Input() filtering = true

  // Inputs needed when showing options
  @Input() communityId?: number
  @Input() organisationId?: number
  @Input() systemId?: number
  @Input() domainId?: number
  @Input() parentItem?: ConformanceStatementItem
  @Input() snapshotId?: number

  @Output() selectionChanged = new EventEmitter<ConformanceStatementItem>()
  @Output() export = new EventEmitter<ExportReportEvent>()
  @Output() selected = new EventEmitter<number>()

  @ViewChildren('itemsComponent') itemsComponents?: QueryList<ConformanceStatementItemsDisplayComponentApi>
  @ViewChild('optionButton') optionButton?: CheckBoxOptionPanelComponentApi
  @ViewChild('statementOptionsButton') statementOptionsButton?: StatementOptionsButtonApi<ConformanceIds>

  protected static EXPORT_XML_OVERVIEW = '0'
  protected static EXPORT_PDF_OVERVIEW = '1'

  optionPending = false
  hasChildren = false
  allChildrenHidden = false
  showCheck = false
  showResults = false
  results?: ConformanceStatementResult
  counters?: Counters
  status?: string
  updateTime?: string
  pending = false
  refreshCounters = new EventEmitter<Counters>()
  actorId?: number
  specificationId?: number
  conformanceIds?: ConformanceIds

  parentItemOptions?: CheckboxOption[][]

  constructor(
    public readonly dataService: DataService
  ) { super() }

  statementSelected(source: number) {
    if (this.item.id != source) {
      if (this.optionButton) this.optionButton.close()
      if (this.statementOptionsButton) this.statementOptionsButton.close()
    }
    this.itemsComponents?.forEach((item: ConformanceStatementItemDisplayComponentApi) => {
      item.statementSelected(source)
    })
  }

  reset() {
    this.ngOnInit()
    if (this.itemsComponents) {
      this.itemsComponents.forEach(item => item.reset())
    }
    if (this.counters) {
      this.refreshCounters.emit(this.counters)
    }
  }

  ngOnInit(): void {
    this.hasChildren = this.item.items != undefined && this.item.items.length > 0
    this.allChildrenHidden = false
    if (this.hasChildren) {
      const hiddenChildren = filter(this.item.items, (item) => {
        return item.hidden == true
      })
      this.allChildrenHidden = this.item.items!.length == hiddenChildren.length
    } else {
      this.allChildrenHidden = true
    }
    this.showCheck = this.withCheck && (!this.hasChildren || this.allChildrenHidden)
    this.showResults = false
    this.results = undefined
    this.counters = undefined
    this.status = undefined
    this.updateTime = undefined
    this.pending = false
    if (this.withResults && this.allChildrenHidden) {
      this.results = this.findResults(this.item)
      if (this.results) {
        this.showResults = true
        this.updateResults(false)
      }
    }
    if (this.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION && this.item.items && this.item.items.length > 0) {
      this.actorId = this.item.items[0].id
      this.specificationId = this.item.id
    } else if (this.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.ACTOR) {
      this.actorId = this.item.id
      this.specificationId = this.parentItem?.id
    }
    if (this.hasChildren && !this.item.hidden && this.withExport) {
      this.parentItemOptions = [[
        {key: ConformanceStatementItemDisplayComponent.EXPORT_PDF_OVERVIEW, label: 'Download overview report', default: true, iconClass: 'fa-solid fa-file-pdf'},
        {key: ConformanceStatementItemDisplayComponent.EXPORT_XML_OVERVIEW, label: 'Download overview report as XML', default: true, iconClass: 'fa-solid fa-file-lines'}
      ]]
    }
    if (this.withOptions) {
      this.conformanceIds = {
        systemId: this.systemId!,
        actorId: this.actorId!,
        specificationId: this.specificationId!,
        domainId: this.domainId!,
      }
    }
  }

  updateResults(signalUpdate: boolean) {
    if (this.results) {
      this.counters = {
        completed: this.results.completed,
        failed: this.results.failed,
        other: this.results.undefined,
        completedOptional: this.results.completedOptional,
        failedOptional: this.results.failedOptional,
        otherOptional: this.results.undefinedOptional,
        completedToConsider: this.results.completedToConsider,
        failedToConsider: this.results.failedToConsider,
        otherToConsider: this.results.undefinedToConsider,
      }
      this.updateTime = this.results.updateTime
      this.status = this.dataService.conformanceStatusForTests(this.results.completedToConsider, this.results.failedToConsider, this.results.undefinedToConsider)
      if (signalUpdate) {
        setTimeout(() => {
          this.refreshCounters.emit(this.counters)
        })
      }
    }
  }

  findResults(item: ConformanceStatementItem):ConformanceStatementResult|undefined  {
    if (item.results) {
      return item.results
    } else if (item.items) {
      for (let child of item.items) {
        if (child.results) {
          return child.results
        }
      }
    }
    return undefined
  }

  clickHeader() {
    if (this.hasChildren && !this.allChildrenHidden) {
      this.item.collapsed = this.expandable && !this.item.collapsed
    } else {
      if (this.showCheck) {
        this.item.checked = !this.item.checked
      }
      if (this.hasChildren) {
        this.itemSelected(this.item.items![0])
      } else {
        this.itemSelected()
      }
    }
  }

  itemSelected(otherItem?: ConformanceStatementItem) {
    this.notifyForSelectionChange(otherItem)
  }

  notifyForSelectionChange(otherItem?: ConformanceStatementItem) {
    if (otherItem) {
      this.selectionChanged.emit(otherItem)
    } else {
      this.selectionChanged.emit(this.item)
    }
  }

  updateChecked() {
    this.notifyForSelectionChange()
  }

  expanded() {
    setTimeout(() => {
      if (this.item.items && this.item.items.length == 1) {
        this.item.items[0].collapsed = false
      }
    })
  }

  collapses() {
    setTimeout(() => {
      if (this.item.items) {
        for (let child of this.item.items) {
          child.collapsed = true
        }
      }
    })
  }

  childSelectionChanged(item: ConformanceStatementItem) {
    this.notifyForSelectionChange(item)
  }

  childSelected(event: number) {
    this.selected.emit(event)
  }

  onExport(event: ExportReportEvent) {
    this.export.emit(event)
  }

  handleOption(event: CheckboxOptionState) {
    if (event[ConformanceStatementItemDisplayComponent.EXPORT_XML_OVERVIEW]) {
      this.onExport({statementReport: false, item: this.item, format: 'xml'})
    } else if (event[ConformanceStatementItemDisplayComponent.EXPORT_PDF_OVERVIEW]) {
      this.onExport({statementReport: false, item: this.item, format: 'pdf'})
    }
  }

  optionsOpening() {
    this.selected.emit(this.item.id)
  }

}
