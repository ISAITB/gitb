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

import {AfterViewInit, Component, ElementRef, EventEmitter, NgZone, OnDestroy, ViewChild} from '@angular/core';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {DataService} from 'src/app/services/data.service';
import {Constants} from 'src/app/common/constants';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {ExportReportEvent} from '../../types/export-report-event';
import {BaseComponent} from '../../pages/base-component.component';
import {ConformanceSnapshot} from '../../types/conformance-snapshot';
import {ConformanceStatementTableApi} from '../conformance-statement-table/conformance-statement-table-api';
import {TestResultSearchCriteria} from '../../types/test-result-search-criteria';
import {PagingEvent} from '../paging-controls/paging-event';
import {Observable} from 'rxjs';
import {ConformanceResultFullList} from '../../types/conformance-result-full-list';
import {ConformanceService} from '../../services/conformance.service';
import {FilterControlApi} from '../filter-control/filter-control-api';
import {PagingControlsApi} from '../paging-controls/paging-controls-api';
import {ConformanceStatementSearchCriteria} from '../../types/conformance-statement-search-criteria';
import {
  ConformanceStatementItemsDisplayComponentApi
} from '../conformance-statement-items-display/conformance-statement-items-display-component-api';
import {ConformanceStatementSearchResult} from '../../types/conformance-statement-search-result';
import {PagingPlacement} from '../paging-controls/paging-placement';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseConformanceItemDisplayComponent extends BaseComponent implements AfterViewInit, OnDestroy {

  @ViewChild("conformanceItemTree") conformanceItemTree?: ConformanceStatementItemsDisplayComponentApi
  @ViewChild("filterControl") filterControl?: FilterControlApi
  @ViewChild("listViewTable") listViewTable?: ConformanceStatementTableApi
  @ViewChild('searchControls') searchControls?: ElementRef
  @ViewChild('selectorControls') selectorControls?: ElementRef
  @ViewChild('conformanceItemPage') conformanceItemPage?: ElementRef
  @ViewChild("pagingControls") pagingControls?: PagingControlsApi

  protected static SHOW_SUCCEEDED = '0'
  protected static SHOW_FAILED = '1'
  protected static SHOW_INCOMPLETE = '2'
  protected readonly PagingPlacement = PagingPlacement;

  readonly Constants = Constants
  searchCriteria: ConformanceStatementSearchCriteria = {
    succeeded: true,
    failed: true,
    incomplete: true
  }
  statusOptions: CheckboxOption[][] = [
    [
      {key: BaseConformanceItemDisplayComponent.SHOW_SUCCEEDED, label: 'Succeeded statements', default: this.searchCriteria.succeeded, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)},
      {key: BaseConformanceItemDisplayComponent.SHOW_FAILED, label: 'Failed statements', default: this.searchCriteria.failed, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)},
      {key: BaseConformanceItemDisplayComponent.SHOW_INCOMPLETE, label: 'Incomplete statements', default: this.searchCriteria.incomplete, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)},
    ]
  ]
  hasStatementsBeforeFiltering = false
  animated = true
  resizeObserver!: ResizeObserver
  searchControlsWrapped = false
  statements: ConformanceStatementItem[] = []
  latestSnapshotButtonLabel?: string
  snapshotButtonLabel?: string
  activeConformanceSnapshot?: ConformanceSnapshot
  dataStatus = {status: Constants.STATUS.PENDING}
  filterCommands = new EventEmitter<number>()
  showExport = false
  updatePending = false
  exportPending = false
  organisationId?: number
  communityId?: number
  listView = false

  protected constructor(
    public readonly dataService: DataService,
    private readonly zone: NgZone,
    protected readonly conformanceService: ConformanceService
  ) {
    super()
  }

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => {
      this.zone.run(() => {
        this.calculateWrapping()
      })
    })
    if (this.conformanceItemPage) {
      this.resizeObserver.observe(this.conformanceItemPage.nativeElement)
    }
  }

  resetStatementFilters() {
    this.searchCriteria.filterText = undefined
    this.searchCriteria.succeeded = true
    this.searchCriteria.failed = true
    this.searchCriteria.incomplete = true
  }

  filterByStatus(choices: CheckboxOptionState) {
    this.searchCriteria.succeeded = choices[BaseConformanceItemDisplayComponent.SHOW_SUCCEEDED]
    this.searchCriteria.failed = choices[BaseConformanceItemDisplayComponent.SHOW_FAILED]
    this.searchCriteria.incomplete = choices[BaseConformanceItemDisplayComponent.SHOW_INCOMPLETE]
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      if (this.conformanceItemPage) {
        this.resizeObserver.unobserve(this.conformanceItemPage.nativeElement)
      }
    }
  }

  protected calculateWrapping() {
    if (this.selectorControls && this.searchControls) {
      this.searchControlsWrapped = this.statements.length > 0 && this.selectorControls.nativeElement.getBoundingClientRect().top != this.searchControls.nativeElement.getBoundingClientRect().top
    }
  }

  protected determineReportLevel(event: ExportReportEvent) {
    let reportLevel: 'all'|'domain'|'specification'|'group'
    if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN) {
      reportLevel = "domain"
    } else if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP) {
      reportLevel = "group"
    } else if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION) {
      reportLevel = "specification"
    } else {
      reportLevel = "all"
    }
    return reportLevel
  }

  toggleFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.TOGGLE)
    this.listViewTable?.toggleFilters()
  }

  clearFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.CLEAR)
    this.listViewTable?.clearFilters()
  }

  refreshFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.REFRESH)
    this.listViewTable?.refreshFilters()
  }

  onExportConformanceStatementsAsCsv() {
    this.exportPending = true
    this.listViewTable?.exportAsCsv().subscribe().add(() => {
      this.exportPending = false
    })
  }

  listViewExportChange(showExport: boolean) {
    this.showExport = showExport
  }

  listViewSearching(pending: boolean) {
    setTimeout(() => {
      this.updatePending = pending
    })
  }

  loadConformanceStatementsForListViewFactory() {
    return (this.loadConformanceStatementsForListView).bind(this)
  }

  loadConformanceStatementsForListView(params: TestResultSearchCriteria, pagingInfo: PagingEvent, fullResults: boolean, forExport: boolean, sortColumn: string, sortOrder: string): Observable<ConformanceResultFullList> {
    return this.conformanceService.getConformanceOverview(params, this.activeConformanceSnapshot?.id, fullResults, forExport, sortColumn, sortOrder, pagingInfo.targetPage, pagingInfo.targetPageSize, this.organisationId)
  }

  updateTreeViewPagination(page: number, count: number) {
    this.pagingControls?.updateStatus(page, count)
  }

  resetConformanceItemTree() {
    setTimeout(() => {
      if (this.conformanceItemTree) {
        this.conformanceItemTree.reset()
      }
    })
  }

  applyTreeViewSearchResult(data: ConformanceStatementSearchResult, pagingInfo: PagingEvent) {
    this.statements = this.dataService.prepareConformanceStatementItemsForDisplay(data.data)
    this.hasStatementsBeforeFiltering = data.hasStatements
    this.updateTreeViewPagination(pagingInfo.targetPage, data.count)
    this.resetConformanceItemTree()
  }

  onStatementOptionsActivated(statementId: number) {
    this.conformanceItemTree?.statementSelected(statementId)
  }

}
