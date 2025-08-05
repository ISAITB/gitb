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

import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Constants} from '../../common/constants';
import {DataService} from '../../services/data.service';
import {PagingControlsApi} from '../paging-controls/paging-controls-api';
import {PagingEvent} from '../paging-controls/paging-event';
import {ConformanceResultFullWithTestSuites} from '../../types/conformance-result-full-with-test-suites';
import {ConformanceResultFullList} from '../../types/conformance-result-full-list';
import {Observable, ReplaySubject} from 'rxjs';
import {ConformanceStatementTableApi} from './conformance-statement-table-api';
import {ConformanceResultFull} from '../../types/conformance-result-full';
import {ConformanceService} from '../../services/conformance.service';
import {ConformanceSnapshot} from '../../types/conformance-snapshot';
import {ConformanceTestSuite} from '../../pages/organisation/conformance-statement/conformance-test-suite';
import {StatementFilterState} from '../statement-controls/statement-filter-state';
import {BaseComponent} from '../../pages/base-component.component';
import {ReportSupportService} from '../../services/report-support.service';
import {RoutingService} from '../../services/routing.service';
import {FilterState} from '../../types/filter-state';
import {TestResultSearchCriteria} from '../../types/test-result-search-criteria';

@Component({
  selector: 'app-conformance-statement-table',
  standalone: false,
  templateUrl: './conformance-statement-table.component.html',
  styleUrl: './conformance-statement-table.component.less'
})
export class ConformanceStatementTableComponent extends BaseComponent implements OnInit, ConformanceStatementTableApi {

  @Input() communityId?: number
  @Input() organisationId?: number
  @Input() statementLoader!: (searchCriteria: TestResultSearchCriteria, pagingInfo: PagingEvent, fullResults: boolean, forExport: boolean, sortColumn: string, sortOrder: string) => Observable<ConformanceResultFullList>;
  @Input() snapshot?: ConformanceSnapshot
  @Output() collapseChange = new EventEmitter<boolean>()
  @Output() exportChange = new EventEmitter<boolean>()
  @Output() searchChange = new EventEmitter<boolean>()
  @Output() communityChange = new EventEmitter<number|undefined>()

  @ViewChild("pagingControls") pagingControls?: PagingControlsApi
  readonly Constants = Constants
  dataStatus = {status: Constants.STATUS.PENDING}
  expandedStatements: { [key: string]: any, count: number } = {
    count: 0
  }
  filterCommands = new EventEmitter<number>()
  conformanceStatements: ConformanceResultFullWithTestSuites[] = []
  sortOrder = Constants.ORDER.ASC
  sortColumn!: string
  columnCount!: number
  filterState: FilterState = {
    filters: [],
    updatePending: false,
    updateDisabled: false
  }

  constructor(
    protected readonly dataService: DataService,
    private readonly conformanceService: ConformanceService,
    private readonly reportSupportService: ReportSupportService,
    private readonly routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.filterState.names = {}
    this.filterState.names[Constants.FILTER_TYPE.RESULT] = 'Status'
    this.filterState.names[Constants.FILTER_TYPE.END_TIME] = 'Last update time'
    this.sortColumn = Constants.FILTER_TYPE.COMMUNITY
    if (this.organisationId == undefined) {
      this.filterState.filters = [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.SPECIFICATION_GROUP, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.ORGANISATION, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.ORGANISATION_PROPERTY, Constants.FILTER_TYPE.SYSTEM_PROPERTY, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.END_TIME ]
      if (this.dataService.isSystemAdmin) {
        this.columnCount = 11
        this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
        this.filterState.filters.push(Constants.FILTER_TYPE.COMMUNITY)
      } else if (this.dataService.isCommunityAdmin) {
        this.sortColumn = Constants.FILTER_TYPE.ORGANISATION
        if (this.dataService.community!.domain == undefined) {
          this.columnCount = 10
          this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
        } else {
          this.columnCount = 9
        }
      }
    } else {
      this.filterState.filters = [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.SPECIFICATION_GROUP, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.END_TIME ]
      if (this.dataService.isSystemAdmin) {
        this.columnCount = 9
        this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
      } else {
        this.sortColumn = Constants.FILTER_TYPE.SYSTEM
        if (this.dataService.community!.domain == undefined) {
          this.columnCount = 9
          this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
        } else {
          this.columnCount = 8
        }
      }
    }
    this.getConformanceStatements()
  }

  getConformanceStatements() {
    this.selectPage({ targetPage: 1, targetPageSize: this.pagingControls?.getCurrentStatus().pageSize! })
  }

  doPageNavigation(event: PagingEvent) {
    this.selectPage(event)
  }

  private selectPage(pagingInfo: PagingEvent) {
    this.updateFilterState(true)
    this.getConformanceStatementsInternal(pagingInfo, false, false)
      .subscribe((data) => {
        this.conformanceStatements = data.data
        this.pagingControls?.updateStatus(pagingInfo.targetPage, data.count)
        this.collapseAll()
      }).add(() => {
      this.updateFilterState(false)
      this.dataStatus.status = Constants.STATUS.FINISHED
      this.exportChange.emit(this.conformanceStatements.length > 0)
    })
  }

  private updateFilterState(pending: boolean) {
    this.filterState.updatePending = pending
    this.searchChange.emit(pending)
  }

  private getCurrentSearchCriteria() {
    let searchCriteria: TestResultSearchCriteria = {}
    if (this.dataService.isCommunityAdmin) {
      searchCriteria.communityIds = [this.dataService.community!.id]
      if (this.dataService.community!.domain != undefined) {
        searchCriteria.domainIds = [this.dataService.community!.domain.id]
      }
    }
    let filterData:{[key: string]: any}|undefined = undefined
    if (this.filterState?.filterData) {
      filterData = this.filterState.filterData()
    }
    if (filterData) {
      if (this.dataService.isSystemAdmin) {
        searchCriteria.communityIds = filterData[Constants.FILTER_TYPE.COMMUNITY]
        searchCriteria.domainIds = filterData[Constants.FILTER_TYPE.DOMAIN]
      } else {
        if (this.dataService.community!.domain == undefined) {
          searchCriteria.domainIds = filterData[Constants.FILTER_TYPE.DOMAIN]
        }
      }
      searchCriteria.specIds = filterData[Constants.FILTER_TYPE.SPECIFICATION]
      searchCriteria.specGroupIds = filterData[Constants.FILTER_TYPE.SPECIFICATION_GROUP]
      searchCriteria.actorIds = filterData[Constants.FILTER_TYPE.ACTOR]
      searchCriteria.organisationIds = filterData[Constants.FILTER_TYPE.ORGANISATION]
      searchCriteria.systemIds = filterData[Constants.FILTER_TYPE.SYSTEM]
      searchCriteria.results = filterData[Constants.FILTER_TYPE.RESULT]
      searchCriteria.endTimeBeginStr = filterData.endTimeBeginStr
      searchCriteria.endTimeEndStr = filterData.endTimeEndStr
      searchCriteria.organisationProperties = filterData.organisationProperties
      searchCriteria.systemProperties = filterData.systemProperties
    }
    if (this.communityId == undefined) {
      if (searchCriteria.communityIds != undefined && searchCriteria.communityIds.length == 1) {
        this.communityChange.emit(searchCriteria.communityIds[0])
      } else {
        this.communityChange.emit(undefined)
      }
    }
    return searchCriteria
  }

  private getConformanceStatementsInternal(pagingInfo: PagingEvent, fullResults: boolean, forExport: boolean) {
    return new Observable<ConformanceResultFullList>((subscriber) => {
      let params = this.getCurrentSearchCriteria()
      this.statementLoader(params, pagingInfo, fullResults, forExport, this.sortColumn, this.sortOrder)
        .subscribe((data: ConformanceResultFullList) => {
          for (let conformanceStatement of data.data) {
            const completedCountToConsider = Number(conformanceStatement.completedToConsider)
            const failedCountToConsider = Number(conformanceStatement.failedToConsider)
            const undefinedCountToConsider = Number(conformanceStatement.undefinedToConsider)
            conformanceStatement.counters = {
              completed: Number(conformanceStatement.completed),
              failed: Number(conformanceStatement.failed),
              other: Number(conformanceStatement.undefined),
              completedOptional: Number(conformanceStatement.completedOptional),
              failedOptional: Number(conformanceStatement.failedOptional),
              otherOptional: Number(conformanceStatement.undefinedOptional),
              completedToConsider: completedCountToConsider,
              failedToConsider: failedCountToConsider,
              otherToConsider: undefinedCountToConsider
            }
            conformanceStatement.overallStatus = this.dataService.conformanceStatusForTests(completedCountToConsider, failedCountToConsider, undefinedCountToConsider)
          }
          subscriber.next(data)
        }).add(() => {
        this.dataStatus = {status: Constants.STATUS.FINISHED}
        subscriber.complete()
      })
    })
  }

  collapseAll() {
    this.expandedStatements = { count: 0 }
    this.collapseChange.emit(this.expandedStatements.count > 0)
  }

  toggleFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.TOGGLE)
  }

  clearFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.CLEAR)
  }

  snapshotChanged() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.CLEAR_WITHOUT_RELOAD)
    this.reloadData()
  }

  refreshFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.REFRESH)
  }

  isExpanded(statement: ConformanceResultFull) {
    return this.expandedStatements[statement.systemId+"_"+statement.actorId] != undefined
  }

  collapse(statement: ConformanceResultFull) {
    delete this.expandedStatements[statement.systemId+"_"+statement.actorId]
    this.expandedStatements.count -= 1
    this.notifyCollapseChange()
  }

  expand(statement: ConformanceResultFull) {
    this.expandedStatements[statement.systemId+"_"+statement.actorId] = true
    this.expandedStatements.count += 1
    this.notifyCollapseChange()
  }

  setCollapseAllStatus(statement: ConformanceResultFullWithTestSuites) {
    statement.hasExpandedTestSuites = this.hasExpandedTestSuite(statement)
  }

  private hasExpandedTestSuite(statement: ConformanceResultFullWithTestSuites) {
    if (statement.displayedTestSuites) {
      for (let testSuite of statement.displayedTestSuites) {
        if (testSuite.expanded) {
          return true
        }
      }
    }
    return false
  }

  onExpand(statement: ConformanceResultFullWithTestSuites) {
    if (this.isExpanded(statement)) {
      this.collapse(statement)
    } else {
      this.expand(statement)
      if (statement.testSuites == undefined) {
        statement.testSuitesLoaded = false
        this.conformanceService.getConformanceStatus(statement.actorId, statement.systemId, this.snapshot?.id)
          .subscribe((data) => {
            if (data) {
              this.dataService.organiseTestSuitesForDisplay(data.testSuites)
              statement.hasBadge = data.summary.hasBadge
              statement.testSuites = data.testSuites
              statement.displayedTestSuites = data.testSuites
              this.setCollapseAllStatus(statement)
              statement.refreshTestSuites = new EventEmitter<void>()
            }
          }).add(() => {
          statement.testSuitesLoaded = true
        })
      }
    }
  }

  notifyCollapseChange() {
    this.collapseChange.emit(this.expandedStatements.count > 0)
  }

  sort(column: string) {
    if (column == this.sortColumn) {
      if (this.sortOrder == Constants.ORDER.DESC) {
        this.sortOrder = Constants.ORDER.ASC
      } else {
        this.sortOrder = Constants.ORDER.DESC
      }
    } else {
      this.sortColumn = column
      this.sortOrder = Constants.ORDER.DESC
    }
    this.getConformanceStatements()
  }

  trackStatement(index: number, statement: ConformanceResultFullWithTestSuites): string {
    return `${statement.actorId}_${statement.systemId}_${this.snapshot?.id}`;
  }

  applyStatementSearchFilters(statement: ConformanceResultFullWithTestSuites, state: StatementFilterState) {
    if (statement.testSuites && statement.testSuites.length > 0) {
      const testSuiteFilter = this.trimSearchString(state.testSuiteFilter)
      const testCaseFilter = this.trimSearchString(state.testCaseFilter)
      statement.displayedTestSuites = this.dataService.filterTestSuites(statement.testSuites, testSuiteFilter, testCaseFilter, state)
      this.setCollapseAllStatus(statement)
      setTimeout(() => {
        if (statement.refreshTestSuites) {
          statement.refreshTestSuites.emit()
        }
      })
    }
  }

  collapseAllTestSuites(statement: ConformanceResultFullWithTestSuites) {
    if (statement.displayedTestSuites) {
      statement.displayedTestSuites.forEach((testSuite: ConformanceTestSuite) => {
        testSuite.expanded = false
      })
      this.setCollapseAllStatus(statement)
    }
  }

  onExportConformanceStatement(statement: ConformanceResultFull, format: 'xml'|'pdf') {
    if (format == 'xml') {
      statement.exportXmlPending = true
    } else {
      statement.exportPdfPending = true
    }
    const testCaseCount = statement.completed + statement.failed + statement.undefined
    this.reportSupportService.handleConformanceStatementReport(statement.communityId, statement.actorId, statement.systemId, this.snapshot?.id, format, true, testCaseCount)
      .subscribe(() => {
        // Do nothing further
      })
      .add(() => {
        if (format == 'xml') {
          statement.exportXmlPending = false
        } else {
          statement.exportPdfPending = false
        }
      })
  }

  toTestSession(sessionId: string) {
    this.routingService.toSessionDashboard(sessionId)
  }

  exportAsCsv(): Observable<void> {
    const status$ = new ReplaySubject<void>(1);
    const pagingInfo = {
      targetPage: 1,
      targetPageSize: 100000000
    }
    this.getConformanceStatementsInternal(pagingInfo, true, true)
      .subscribe((data) => {
        let headers: string[] = []
        let columnMap: string[] = []
        if (!this.dataService.isCommunityAdmin) {
          headers.push("Community")
          columnMap.push("communityName")
        }
        headers.push(this.dataService.labelOrganisation())
        columnMap.push("organizationName")
        if (data.orgParameters != undefined) {
          for (let param of data.orgParameters) {
            headers.push(this.dataService.labelOrganisation() + " ("+param+")")
            columnMap.push("orgparam_"+param)
          }
        }
        headers.push(this.dataService.labelSystem())
        columnMap.push("systemName")
        if (data.sysParameters != undefined) {
          for (let param of data.sysParameters) {
            headers.push(this.dataService.labelSystem() + " ("+param+")")
            columnMap.push("sysparam_"+param)
          }
        }
        if (!this.dataService.isCommunityAdmin || this.dataService.isCommunityAdmin && this.dataService.community!.domain == undefined) {
          headers.push(this.dataService.labelDomain())
          columnMap.push("domainName")
        }
        headers = headers.concat([this.dataService.labelSpecification(), this.dataService.labelActor(), "Test suite", "Test case", "Result"])
        columnMap = columnMap.concat(["specName", "actorName", "testSuiteName", "testCaseName", "result"])
        this.dataService.exportPropertiesAsCsv(headers, columnMap, data.data)
        status$.next()
      }).add(() => {
      status$.complete()
    })
    return status$.asObservable()
  }

  reloadData() {
    this.dataStatus.status = Constants.STATUS.PENDING
    this.getConformanceStatements()
  }

}
