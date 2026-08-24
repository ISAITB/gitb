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

import {AfterViewInit, Component, EventEmitter, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Constants} from '../../common/constants';
import {TableColumnDefinition} from '../../types/table-column-definition.type';
import {TestResultForDisplay} from '../../types/test-result-for-display';
import {FilterState} from '../../types/filter-state';
import {DataService} from '../../services/data.service';
import {ConformanceService} from '../../services/conformance.service';
import {ReportService} from '../../services/report.service';
import {ConfirmationDialogService} from '../../services/confirmation-dialog.service';
import {TestService} from '../../services/test.service';
import {PopupService} from '../../services/popup.service';
import {ActivatedRoute, Router} from '@angular/router';
import {DiagramLoaderService} from '../../components/diagram/test-session-presentation/diagram-loader.service';
import {RoutingService} from '../../services/routing.service';
import {TestResultSearchCriteria} from '../../types/test-result-search-criteria';
import {mergeMap, Observable, of, share} from 'rxjs';
import {TestResultReport} from '../../types/test-result-report';
import {TestResultForExport} from '../admin/session-dashboard/test-result-for-export';
import {saveAs} from 'file-saver';
import {FieldInfo} from '../../types/field-info';
import {TestResultData} from '../../types/test-result-data';
import {SessionTableComponent} from '../../components/session-table/session-table.component';
import {PagingEvent} from '../../components/paging-controls/paging-event';
import {CheckboxOption} from '../../components/checkbox-option-panel/checkbox-option';
import {CheckboxOptionState} from '../../components/checkbox-option-panel/checkbox-option-state';
import {SessionColumnCase, SessionColumnsService} from '../../services/session-columns.service';
import {BaseComponent} from '../base-component.component';
import {Utils} from '../../common/utils';

/** The full state of a session dashboard page, persisted so that returning here via a "View XYZ"
 * Back control restores filters, paging, sort and the previously expanded session. */
interface SessionDashboardState {
  filters?: {[key: string]: any}
  activeSortColumn: string
  activeSortOrder: string
  completedSortColumn: string
  completedSortOrder: string
  activePaging?: PagingEvent
  completedPaging?: PagingEvent
  expandedSessionId?: string
}

@Component({
  template: '',
  standalone: false
})
export abstract class BaseSessionDashboardComponent extends BaseComponent implements OnInit, AfterViewInit, OnDestroy {

  protected static EXPORT_PDF_OPTION = '0'
  protected static EXPORT_XML_OPTION = '1'
  protected static EXPORT_DATA_OPTION = '2'

  showActiveSessions = false
  showSessionNavigationControls = false
  showDeleteControls = false
  showDeleteObsoleteControl = false
  showTogglePendingAdminInteraction = false
  exportActivePending = false
  exportCompletedPending = false
  interactionLoadPending = false
  pendingAdminInteraction = false
  selectingForDelete = false
  activeExpandedCounter = {count: 0}
  completedExpandedCounter = {count: 0}
  activeStatus = {status: Constants.STATUS.PENDING}
  completedStatus = {status: Constants.STATUS.PENDING}
  communityId?: number
  organisationId?: number
  activeTestsColumns!: TableColumnDefinition[]
  completedTestsColumns!: TableColumnDefinition[]
  activeTests: TestResultForDisplay[] = []
  completedTests: TestResultForDisplay[] = []
  completedTestsCheckboxEmitter = new EventEmitter<boolean>()
  action = false
  refreshActivePending = false
  refreshCompletedPending = false
  filterState: FilterState = {
    filters: [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.SPECIFICATION_GROUP, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.TEST_SUITE, Constants.FILTER_TYPE.TEST_CASE, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.FLAG, Constants.FILTER_TYPE.START_TIME, Constants.FILTER_TYPE.SESSION, Constants.FILTER_TYPE.COMMENTS ],
    updatePending: false,
    updateDisabled: false
  }
  deletePending = false
  deleteSessionsPending = false
  stopAllPending = false
  activeSessionsCollapsed = false
  activeSessionsCollapsedFinished = false
  completedSessionsCollapsed = false
  completedSessionsCollapsedFinished = false
  sessionRefreshCompleteEmitter = new EventEmitter<TestResultReport|undefined>()
  sessionIdToShow?: string
  systemIdToShow?: number
  testCaseIdToShow?: number
  activeSortOrder = "asc"
  activeSortColumn = "startTime"
  completedSortOrder = "desc"
  completedSortColumn = "endTime"
  copyForOtherRoleOption = false
  expandFirstSession = false
  restoredState?: SessionDashboardState
  restoredFilters?: {[key: string]: any}
  private restoredExpandSessionId?: string

  @ViewChild("completedSessions") completedSessionsTable?: SessionTableComponent
  @ViewChild("activeSessions") activeSessionsTable?: SessionTableComponent

  columnChooserOptions?: CheckboxOption[][]
  private currentColumnIds: string[] = []
  /** Whether we arrived here via RoutingService.returnToSource() (a "View XYZ" Back navigation).
   * Captured from transient router state (not sessionStorage) so it can never leak into a later,
   * unrelated visit to this page. */
  private readonly arrivedViaReturn: boolean

  constructor(
    public readonly dataService: DataService,
    protected readonly conformanceService: ConformanceService,
    protected readonly reportService: ReportService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    protected readonly testService: TestService,
    private readonly popupService: PopupService,
    protected readonly route: ActivatedRoute,
    private readonly diagramLoaderService: DiagramLoaderService,
    protected readonly routingService: RoutingService,
    protected readonly sessionColumnsService: SessionColumnsService,
    router: Router
  ) {
    super()
    this.arrivedViaReturn = router.currentNavigation()?.extras?.state?.['restore'] === true
  }

  ngOnInit(): void {
    this.showActiveSessions = this.showActiveTestSessions()
    this.showSessionNavigationControls = this.showTestSessionNavigationControls()
    this.showDeleteControls = this.showTestSessionDeleteControls()
    this.showTogglePendingAdminInteraction = this.showTogglePendingAdminInteractionControl()
    this.copyForOtherRoleOption = this.showCopyForOtherRoleOption()
    if (!this.dataService.isSystemAdmin) {
      this.communityId = this.dataService.community!.id
    }
    // Only pages that offer "View XYZ" navigation controls can be the target of a recorded return -
    // this also keeps community session dashboard's state from clashing with the admin one's, as
    // both currently share the same base state-restoration logic.
    if (this.showSessionNavigationControls) {
      this.restoredState = this.consumeRestoredState()
    }
    if (this.restoredState) {
      // Returning here from a "View XYZ" navigation: restore the full previous state instead of
      // the query-param based deep link below.
      this.restoredFilters = this.restoredState.filters
      this.activeSortColumn = this.restoredState.activeSortColumn
      this.activeSortOrder = this.restoredState.activeSortOrder
      this.completedSortColumn = this.restoredState.completedSortColumn
      this.completedSortOrder = this.restoredState.completedSortOrder
      this.restoredExpandSessionId = this.restoredState.expandedSessionId
    } else {
      const sessionIdValue = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID)
      if (sessionIdValue != undefined) {
        this.sessionIdToShow = sessionIdValue
      }
      const testCaseIdValue = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.TEST_CASE_ID)
      if (testCaseIdValue != undefined) {
        this.testCaseIdToShow = Number(testCaseIdValue)
      }
      const systemIdValue = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID)
      if (systemIdValue != undefined) {
        this.systemIdToShow = Number(systemIdValue)
      }
      this.expandFirstSession = this.sessionIdToShow != undefined || (this.systemIdToShow != undefined && this.testCaseIdToShow != undefined)
    }
    this.activeTestsColumns = this.getActiveTestsColumns()
    this.completedTestsColumns = this.getCompletedTestsColumns()
    this.currentColumnIds = this.sessionColumnsService.activeIds(this.dataService.getSessionColumnPreference(this.getColumnCase()), this.getColumnCase(), this.dataService.isSystemAdmin)
    this.columnChooserOptions = this.buildColumnChooserOptions()
    if (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.community!.domain == undefined)) {
      this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
    }
    if (this.dataService.isSystemAdmin) {
      this.filterState.filters.push(Constants.FILTER_TYPE.COMMUNITY)
    }
    if (this.includeOrganisationFilter()) {
      this.filterState.filters.push(Constants.FILTER_TYPE.ORGANISATION)
    }
    if (this.includeCustomPropertyFilters()) {
      this.filterState.filters.push(Constants.FILTER_TYPE.ORGANISATION_PROPERTY, Constants.FILTER_TYPE.SYSTEM_PROPERTY)
    }
    if (this.restoredState) {
      // Reflect the restored sort on the column headers (same approach as onColumnChooserUpdated).
      const activeOrder = this.activeSortOrder as 'asc'|'desc'
      const completedOrder = this.completedSortOrder as 'asc'|'desc'
      this.activeTestsColumns = this.activeTestsColumns.map(c => ({...c, order: c.field === this.activeSortColumn ? activeOrder : null})) as TableColumnDefinition[]
      this.completedTestsColumns = this.completedTestsColumns.map(c => ({...c, order: c.field === this.completedSortColumn ? completedOrder : null})) as TableColumnDefinition[]
    }
    this.showDeleteObsoleteControl = this.showDeleteObsolete()
    this.setBreadcrumbs()
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.filterState.updatePending = true
      if (this.restoredState) {
        if (this.showActiveSessions) {
          this.getActiveTests(this.restoredState.activePaging ?? { targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
        }
        this.getCompletedTests(this.restoredState.completedPaging ?? { targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
      } else {
        this.applyFilters()
      }
    })
  }

  ngOnDestroy(): void {
    if (this.showSessionNavigationControls) {
      this.saveCurrentState()
    }
  }

  /** The key under which this page's display state is saved/restored - distinct per page (and, for
   * organisation tests, per organisation) so unrelated pages/organisations don't clash. */
  protected stateKey(): string {
    return Constants.DISPLAY_STATE_KEY.SESSION_DASHBOARD
  }

  private consumeRestoredState(): SessionDashboardState|undefined {
    if (this.arrivedViaReturn) {
      const saved = this.getDisplayState<SessionDashboardState>(this.stateKey(), true)
      if (saved?.state) {
        return saved.state
      }
    }
    return undefined
  }

  private saveCurrentState() {
    // Among all currently expanded rows (there may be several, across both tables), restore the one
    // most recently expanded - this is the one the user was acting on (e.g. clicked "View" from).
    let expandedRow: TestResultForDisplay|undefined
    for (const row of this.activeTests.concat(this.completedTests)) {
      if (row.expanded && (expandedRow == undefined || (row.expandedOrder ?? 0) > (expandedRow.expandedOrder ?? 0))) {
        expandedRow = row
      }
    }
    const state: SessionDashboardState = {
      filters: this.filterState?.filterData ? this.filterState.filterData() : undefined,
      activeSortColumn: this.activeSortColumn,
      activeSortOrder: this.activeSortOrder,
      completedSortColumn: this.completedSortColumn,
      completedSortOrder: this.completedSortOrder,
      activePaging: this.currentActivePagingInfo(),
      completedPaging: this.currentCompletedPagingInfo(),
      expandedSessionId: expandedRow?.session
    }
    this.saveDisplayState(this.stateKey(), { key: this.stateKey(), state: state })
  }

  protected includeOrganisationFilter(): boolean {
    return true
  }

  protected includeCustomPropertyFilters(): boolean {
    return true
  }

  protected showCopyForOtherRoleOption(): boolean {
    return true
  }

  protected showTogglePendingAdminInteractionControl(): boolean {
    return true
  }

  /** Column case shared by the active and completed session tables. Override in subclasses for own-sessions views. */
  protected getColumnCase(): SessionColumnCase {
    return SessionColumnCase.All
  }

  protected getActiveTestsColumns(): TableColumnDefinition[] {
    const cc = this.getColumnCase()
    return this.sessionColumnsService.buildTableColumns(cc, this.dataService.getSessionColumnPreference(cc), this.dataService.isSystemAdmin, false)
  }

  protected getCompletedTestsColumns(): TableColumnDefinition[] {
    const cc = this.getColumnCase()
    return this.sessionColumnsService.buildTableColumns(cc, this.dataService.getSessionColumnPreference(cc), this.dataService.isSystemAdmin, true)
  }

  protected buildColumnChooserOptions(): CheckboxOption[][] {
    const cc = this.getColumnCase()
    return this.sessionColumnsService.buildChooserOptions(cc, this.currentColumnIds, this.dataService.isSystemAdmin)
  }

  /** Handles a live toggle in the column chooser - applies the new column set to both the active and completed tables. */
  onColumnChooserUpdated(state: CheckboxOptionState): void {
    const cc = this.getColumnCase()
    this.currentColumnIds = Object.keys(state).filter(k => state[k])
    const serialized = this.sessionColumnsService.serialize(this.currentColumnIds)
    this.activeTestsColumns = this.sessionColumnsService.buildTableColumns(cc, serialized, this.dataService.isSystemAdmin, false)
    this.completedTestsColumns = this.sessionColumnsService.buildTableColumns(cc, serialized, this.dataService.isSystemAdmin, true)
    // Reset sort (and refetch) if the currently sorted column is no longer visible.
    if (this.showActiveSessions && !this.activeTestsColumns.some(c => c.field === this.activeSortColumn)) {
      this.activeSortColumn = 'startTime'
      this.activeSortOrder = 'asc'
      this.activeTestsColumns = this.activeTestsColumns.map(c => ({...c, order: c.field === 'startTime' ? 'asc' : null})) as TableColumnDefinition[]
      this.getActiveTests(this.currentActivePagingInfo())
    }
    if (!this.completedTestsColumns.some(c => c.field === this.completedSortColumn)) {
      this.completedSortColumn = 'endTime'
      this.completedSortOrder = 'desc'
      this.completedTestsColumns = this.completedTestsColumns.map(c => ({...c, order: c.field === 'endTime' ? 'desc' : null})) as TableColumnDefinition[]
      this.getCompletedTests(this.currentCompletedPagingInfo())
    }
    // Refresh both tables' chooser popups to update disabled flags.
    const refreshed = this.sessionColumnsService.buildChooserOptions(cc, this.currentColumnIds, this.dataService.isSystemAdmin)
    this.columnChooserOptions = refreshed
    this.activeSessionsTable?.refreshColumnChooser(refreshed)
    this.completedSessionsTable?.refreshColumnChooser(refreshed)
  }

  onColumnChooserClosed(): void {
    const cc = this.getColumnCase()
    this.dataService.setSessionColumnPreference(cc, this.sessionColumnsService.serialize(this.currentColumnIds))
  }

  protected setBreadcrumbs() {
    this.routingService.sessionDashboardBreadcrumbs()
  }

  setFilterRefreshState() {
    setTimeout(() => {
      this.filterState.updateDisabled = this.refreshActivePending || this.refreshCompletedPending
      if (!this.filterState.updateDisabled) {
        this.filterState.updatePending = false
      }
    })
  }

  getCurrentSearchCriteria() {
    let searchCriteria: TestResultSearchCriteria = {}
    let filterData:{[key: string]: any}|undefined = undefined
    if (this.filterState?.filterData) {
      filterData = this.filterState.filterData()
    }
    if (filterData) {
      searchCriteria.systemIds = filterData[Constants.FILTER_TYPE.SYSTEM]
      searchCriteria.specIds = filterData[Constants.FILTER_TYPE.SPECIFICATION]
      searchCriteria.specGroupIds = filterData[Constants.FILTER_TYPE.SPECIFICATION_GROUP]
      searchCriteria.actorIds = filterData[Constants.FILTER_TYPE.ACTOR]
      searchCriteria.testSuiteIds = filterData[Constants.FILTER_TYPE.TEST_SUITE]
      searchCriteria.testCaseIds = filterData[Constants.FILTER_TYPE.TEST_CASE]
      searchCriteria.results = filterData[Constants.FILTER_TYPE.RESULT]
      searchCriteria.flagIds = filterData[Constants.FILTER_TYPE.FLAG]
      searchCriteria.includeUnflagged = filterData.includeUnflagged
      searchCriteria.startTimeBeginStr = filterData.startTimeBeginStr
      searchCriteria.startTimeEndStr = filterData.startTimeEndStr
      searchCriteria.endTimeBeginStr = filterData.endTimeBeginStr
      searchCriteria.endTimeEndStr = filterData.endTimeEndStr
      searchCriteria.sessionId = filterData.sessionId
      searchCriteria.hasComments = filterData.hasComments
      searchCriteria.commentText = filterData.commentText
    } else if (this.sessionIdToShow != undefined || this.systemIdToShow != undefined || this.testCaseIdToShow != undefined) {
      if (this.sessionIdToShow != undefined) {
        searchCriteria.sessionId = this.sessionIdToShow
      }
      if (this.systemIdToShow != undefined) {
        searchCriteria.systemIds = [this.systemIdToShow]
      }
      if (this.testCaseIdToShow != undefined) {
        searchCriteria.testCaseIds = [this.testCaseIdToShow]
      }
    }
    searchCriteria.activeSortColumn = this.activeSortColumn
    searchCriteria.activeSortOrder = this.activeSortOrder
    searchCriteria.completedSortColumn = this.completedSortColumn
    searchCriteria.completedSortOrder = this.completedSortOrder
    this.addExtraSearchCriteria(searchCriteria, filterData)
    return searchCriteria
  }

  protected addExtraSearchCriteria(searchCriteria: TestResultSearchCriteria, filterData:{[key: string]: any}|undefined): void {
    if (!this.dataService.isSystemAdmin) {
      searchCriteria.communityIds = [this.dataService.community!.id]
      if (this.dataService.community?.domain != undefined) {
        searchCriteria.domainIds = [this.dataService.community.domain.id]
      }
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
      searchCriteria.organisationIds = filterData[Constants.FILTER_TYPE.ORGANISATION]
      searchCriteria.organisationProperties = filterData.organisationProperties
      searchCriteria.systemProperties = filterData.systemProperties
    }
  }

  getActiveTests(pagingInfo: PagingEvent) {
    const params = this.getCurrentSearchCriteria()
    this.refreshActivePending = true
    this.activeExpandedCounter.count = 0
    this.setFilterRefreshState()
    this.loadActiveTests(pagingInfo.targetPage, pagingInfo.targetPageSize, params).subscribe((data) => {
      this.activeTests = data.data.map((testResult) => this.newTestResultForDisplay(testResult, false))
      this.activeSessionsTable!.pagingControls!.updateStatus(pagingInfo.targetPage, data.count)
    }).add(() => {
      this.interactionLoadPending = false
      this.refreshActivePending = false
      this.setFilterRefreshState()
      this.activeStatus.status = Constants.STATUS.FINISHED
    })
  }

  protected loadActiveTests(page: number, pageSize: number, params: TestResultSearchCriteria, forExport?: boolean): Observable<TestResultData> {
    return this.reportService.getActiveTestResults(page, pageSize, params, this.pendingAdminInteraction, forExport)
  }

  getCompletedTests(pagingInfo: PagingEvent) {
    this.completedTestsCheckboxEmitter.emit(false)
    this.completedExpandedCounter.count = 0
    this.selectingForDelete = false
    const params = this.getCurrentSearchCriteria()
    this.refreshCompletedPending = true
    this.setFilterRefreshState()
    this.loadCompletedTests(pagingInfo.targetPage, pagingInfo.targetPageSize, params)
    .subscribe((data) => {
      this.completedTests = data.data.map((testResult) => this.newTestResultForDisplay(testResult, true))
      this.completedSessionsTable!.pagingControls!.updateStatus(pagingInfo.targetPage, data.count)
    }).add(() => {
      this.refreshCompletedPending = false
      this.setFilterRefreshState()
      this.completedStatus.status = Constants.STATUS.FINISHED
    })
  }

  protected loadCompletedTests(page: number, pageSize: number, params: TestResultSearchCriteria, forExport?: boolean): Observable<TestResultData> {
    return this.reportService.getCompletedTestResults(page, pageSize, params, forExport)
  }

  private newTestResult(testResult: TestResultReport, completed: boolean): TestResultForDisplay {
    const result: Partial<TestResultForDisplay> = {
      session: testResult.result.sessionId,
      domain: testResult.domain?.sname,
      domainId: testResult.domain?.id,
      specification: testResult.specification?.sname,
      actor: testResult.actor?.name,
      testSuite: testResult.testSuite?.sname,
      testCase: testResult.test?.sname,
      organization: testResult.organization?.sname,
      system: testResult.system?.sname,
      community: testResult.community?.sname,
      startTime: testResult.result.startTime,
      specificationId: testResult.specification?.id,
      actorId: testResult.actor?.id,
      systemId: testResult.system?.id,
      organizationId: testResult.organization?.id,
      communityId: testResult.organization?.community
    }
    if (completed) {
      this.applyCompletedDataToTestSession(result as TestResultForDisplay, testResult)
    }
    return result as TestResultForDisplay
  }

  private newTestResultForDisplay(testResult: TestResultReport, completed: boolean) {
    const result: TestResultForDisplay = this.newTestResult(testResult, completed)
    result.testSuiteId = testResult.testSuite?.id
    result.testCaseId = testResult.test?.id
    if (this.expandFirstSession || (this.restoredExpandSessionId != undefined && result.session === this.restoredExpandSessionId)) {
      // We have been asked to open a session (either the first result of a query-param deep link, or
      // the session that was expanded when the user last left this page). Defer the expansion until
      // the diagram has loaded (shows a spinner on the row and then animates open), matching a
      // user-initiated expansion. Keep it once.
      this.expandFirstSession = false
      this.restoredExpandSessionId = undefined
      result.expansionPending = true
      this.sessionIdToShow = undefined
      this.testCaseIdToShow = undefined
      this.systemIdToShow = undefined
      if (completed) {
        setTimeout(() => {
          this.completedSessionsTable?.loadSessionComments(result)
        })
      }
    }
    return result
  }

  sortActiveSessions(column: TableColumnDefinition) {
    this.activeSortColumn = column.field
    this.activeSortOrder = column.order!
    this.getActiveTests(this.currentActivePagingInfo())
  }

  sortCompletedSessions(column: TableColumnDefinition) {
    this.completedSortColumn = column.field
    this.completedSortOrder = column.order!
    this.getCompletedTests(this.currentCompletedPagingInfo())
  }

  stopAll() {
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate all active sessions?', 'Terminate', 'Cancel', Constants.BUTTON_ICON.DELETE).subscribe(() => {
      this.stopAllPending = true
      this.stopAllOperation().subscribe(() => {
        this.applyFilters()
        this.popupService.success('Test sessions terminated.')
      }).add(() => {
        this.stopAllPending = false
      })
    })
  }

  protected stopAllOperation(): Observable<void> {
    if (this.dataService.isSystemAdmin) {
      return this.testService.stopAll()
    } else {
      return this.testService.stopAllCommunitySessions(this.communityId!)
    }
  }

  queryDatabase(reset?: boolean) {
    if (this.showActiveSessions) {
      this.getActiveTests(this.currentActivePagingInfo(reset))
    }
    this.getCompletedTests(this.currentCompletedPagingInfo(reset))
  }

  filterControlApplied() {
    this.applyFilters()
  }

  applyFilters() {
    this.queryDatabase(true)
  }

  doCompletedPageNavigation(event: PagingEvent) {
    this.getCompletedTests(event)
  }

  doActivePageNavigation(event: PagingEvent) {
    this.getActiveTests(event)
  }

  optionsVisibleForRow(session: TestResultForDisplay) {
    return session.obsolete == undefined || !session.obsolete
  }

  loadCompletedSessionOptionFactory() {
    return (session: TestResultForDisplay) => {
      const options: CheckboxOption[][] = []
      const reportOptions: CheckboxOption[] = []
      reportOptions.push({ key:  BaseSessionDashboardComponent.EXPORT_PDF_OPTION, label: "Download report", default: true, iconClass: Constants.BUTTON_ICON.REPORT_PDF })
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || this.dataService.community?.allowXmlReports === true) {
        reportOptions.push({ key:  BaseSessionDashboardComponent.EXPORT_XML_OPTION, label: "Download report as XML", default: true, iconClass: Constants.BUTTON_ICON.REPORT_XML })
      }
      options.push(reportOptions)
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        options.push([{ key:  BaseSessionDashboardComponent.EXPORT_DATA_OPTION, label: "Download test data", default: true, iconClass: Constants.BUTTON_ICON.REPORT_ZIP }])
      }
      return of(options)
    }
  }

  handleOption(event: { data: TestResultForDisplay, option: string }) {
    if (event.option === BaseSessionDashboardComponent.EXPORT_PDF_OPTION) {
      this.onReportExportPdf(event.data)
    } else if (event.option === BaseSessionDashboardComponent.EXPORT_XML_OPTION) {
      this.onReportExportXml(event.data)
    } else if (event.option === BaseSessionDashboardComponent.EXPORT_DATA_OPTION) {
      this.onExportTestData(event.data)
    }
  }

  onReportExportPdf(testResult: TestResultForDisplay) {
    if (!testResult.obsolete) {
      testResult.optionPending = true
      testResult.exportPending = true
      this.onReportExport(testResult, 'application/pdf', 'report.pdf')
        .subscribe(() => {})
        .add(() => {
          testResult.exportPending = false
          testResult.optionPending = false
        })
    }
  }

  private onReportExportXml(testResult: TestResultForDisplay) {
    if (!testResult.obsolete && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || this.dataService.community?.allowXmlReports === true)) {
      testResult.optionPending = true
      testResult.actionPending = true
      this.onReportExport(testResult, 'application/xml', 'report.xml')
        .subscribe(() => {})
        .add(() => {
          testResult.actionPending = false
          testResult.optionPending = false
        })
    }
  }

  private onReportExport(testResult: TestResultForDisplay, contentType: string, fallbackFileName: string) {
    return this.reportService.exportTestCaseReport(testResult.session, testResult.testCaseId!, contentType)
      .pipe(
        mergeMap((response) => {
          const blobData = new Blob([response.body as ArrayBuffer], {type: contentType});
          saveAs(blobData, Utils.fileNameFromContentDisposition(response, fallbackFileName));
          return of(response)
        }),
        share()
      )
  }

  private onExportTestData(testResult: TestResultForDisplay) {
    testResult.optionPending = true
    this.reportService.exportTestSessionData(testResult.session).subscribe((response) => {
      const blobData = new Blob([response.body as ArrayBuffer], {type: 'application/zip'});
      saveAs(blobData, Utils.fileNameFromContentDisposition(response, 'test_data.zip'));
    }).add(() => {
      testResult.optionPending = false
    })
  }

  exportCompletedSessionsToCsv() {
    this.exportCompletedPending = true
    const params = this.getCurrentSearchCriteria()
    this.loadCompletedTests(1, 100000000, params, true).subscribe((data) => {
      const fields = this.getExportFieldInfoForCompletedTests()
      this.addExtraExportData(data, fields)
      const tests = data.data.map((testResult) => {
        const resultForExport = this.newTestResult(testResult, true)
        this.mapExtraDataToResult(resultForExport, testResult, data)
        return resultForExport
      })
      this.dataService.exportAllAsCsv(fields, tests)
    }).add(() => {
      this.exportCompletedPending = false
    })
  }

  protected addExtraExportData(data: TestResultData, fields: FieldInfo[]) {
    this.addCustomPropertiesToExportFields(data, fields)
  }

  protected mapExtraDataToResult(result: TestResultForExport, originalResult: TestResultReport, data: TestResultData) {
    if (data.orgParameters !== undefined) {
      for (let param of data.orgParameters) {
        if (originalResult.organization && originalResult.organization.parameters) {
          result['organization_'+param] = originalResult.organization.parameters[param]
        }
      }
    }
    if (data.sysParameters !== undefined) {
      for (let param of data.sysParameters) {
        if (originalResult.system && originalResult.system.parameters) {
          result['system_'+param] = originalResult.system.parameters[param]
        }
      }
    }
  }

  protected getExportFieldInfoForCompletedTests(): FieldInfo[] {
    return [
      { header: 'Session', field: 'session' },
      { header: this.dataService.labelDomain(), field: 'domain' },
      { header: this.dataService.labelSpecification(), field: 'specification' },
      { header: this.dataService.labelActor(), field: 'actor' },
      { header: 'Test suite', field: 'testSuite' },
      { header: 'Test case', field: 'testCase' },
      { header: this.dataService.labelOrganisation(), field: 'organization' },
      { header: this.dataService.labelSystem(), field: 'system' },
      { header: 'Start time', field: 'startTime' },
      { header: 'End time', field: 'endTime' },
      { header: 'Result', field: 'result' },
      { header: 'Obsolete', field: 'obsolete' }
    ]
  }

  exportActiveSessionsToCsv() {
    this.exportActivePending = true
    const params = this.getCurrentSearchCriteria()
    this.loadActiveTests(1, 100000000, params, true).subscribe((data) => {
      const fields = this.getExportFieldInfoForActiveTests()
      this.addExtraExportData(data, fields)
      const tests = data.data.map((testResult) => {
        const resultForExport = this.newTestResult(testResult, true)
        this.mapExtraDataToResult(resultForExport, testResult, data)
        return resultForExport
      })
      this.dataService.exportAllAsCsv(fields, tests)
    }).add(() => {
      this.exportActivePending = false
    })
  }

  protected getExportFieldInfoForActiveTests(): FieldInfo[] {
    return [
      { header: 'Session', field: 'session' },
      { header: this.dataService.labelDomain(), field: 'domain' },
      { header: this.dataService.labelSpecification(), field: 'specification' },
      { header: this.dataService.labelActor(), field: 'actor' },
      { header: 'Test suite', field: 'testSuite' },
      { header: 'Test case', field: 'testCase' },
      { header: this.dataService.labelOrganisation(), field: 'organization' },
      { header: this.dataService.labelSystem(), field: 'system' },
      { header: 'Start time', field: 'startTime' }
    ]
  }

  private addCustomPropertiesToExportFields(data: TestResultData, fields: FieldInfo[]) {
    if (data.orgParameters !== undefined) {
      for (let param of data.orgParameters) {
        fields.push({ header: this.dataService.labelOrganisation() + ' ('+param+')', field: 'organization_'+param})
      }
    }
    if (data.sysParameters !== undefined) {
      for (let param of data.sysParameters) {
        fields.push({ header: this.dataService.labelSystem() + ' ('+param+')', field: 'system_'+param})
      }
    }
  }

  rowStyle(row: TestResultForDisplay) {
    if (row.obsolete) {
      return 'test-result-obsolete'
    } else {
      return ''
    }
  }

  private currentCompletedPagingInfo(reset?: boolean): PagingEvent {
    if (reset || this.completedSessionsTable?.pagingControls?.getCurrentStatus() == undefined) {
      return { targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize }
    } else {
      return { targetPage: this.completedSessionsTable.pagingControls.getCurrentStatus().currentPage, targetPageSize: this.completedSessionsTable.pagingControls.getCurrentStatus().pageSize }
    }
  }

  private currentActivePagingInfo(reset?: boolean): PagingEvent {
    if (reset || this.activeSessionsTable?.pagingControls?.getCurrentStatus() == undefined) {
      return { targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize }
    } else {
      return { targetPage: this.activeSessionsTable.pagingControls.getCurrentStatus().currentPage, targetPageSize: this.activeSessionsTable.pagingControls.getCurrentStatus().pageSize }
    }
  }

  deleteObsolete() {
    this.confirmationDialogService.confirmedDangerous('Confirm delete', 'Are you sure you want to delete all obsolete test results?', 'Delete', 'Cancel', Constants.BUTTON_ICON.DELETE).subscribe(() => {
      this.deletePending = true
      this.deleteObsoleteOperation().subscribe(() => {
        this.popupService.success('Obsolete test results scheduled for deletion.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  protected deleteObsoleteOperation(): Observable<void> {
    if (this.dataService.isCommunityAdmin && this.dataService.community?.id !== undefined) {
      return this.conformanceService.deleteObsoleteTestResultsForCommunity(this.dataService.community.id)
    } else {
      return this.conformanceService.deleteObsoleteTestResults()
    }
  }

  showCollapseAll() {
    return this.completedExpandedCounter.count > 0
  }

  showCollapseAllActive() {
    return this.activeExpandedCounter.count > 0
  }

  onCollapseAll() {
    for (let test of this.completedTests) {
      test.expanded = false
    }
    this.completedExpandedCounter.count = 0
  }

  onCollapseAllActive() {
    for (let test of this.activeTests) {
      test.expanded = false
    }
    this.activeExpandedCounter.count = 0
  }

  selectDeleteSessions() {
    this.completedTestsCheckboxEmitter.emit(true)
    this.selectingForDelete = true
  }

  confirmDeleteSessions() {
    const testsToDelete: string[] = []
    for (let test of this.completedTests) {
      if (test.checked !== undefined && test.checked) {
        testsToDelete.push(test.session)
      }
    }
    let msg: string
    if (testsToDelete.length == 1) {
      msg = 'Are you sure you want to delete the selected test result?'
    } else {
      msg = 'Are you sure you want to delete the selected test results?'
    }
    this.confirmationDialogService.confirmedDangerous('Confirm delete', msg, 'Delete', 'Cancel', Constants.BUTTON_ICON.DELETE)
      .subscribe(() => {
        this.deleteSessionsPending = true
        this.conformanceService.deleteTestResults(testsToDelete)
          .subscribe(() => {
            this.popupService.success('Test results deleted.')
            this.getCompletedTests(this.currentCompletedPagingInfo())
          }).add(() => {
          this.deleteSessionsPending = false
          this.cancelDeleteSessions()
        })
      })
  }

  cancelDeleteSessions() {
    this.completedTestsCheckboxEmitter.emit(false)
    this.selectingForDelete = false
    for (let test of this.completedTests) {
      test.checked = false
    }
  }

  testsChecked() {
    for (let test of this.completedTests) {
      if (test.checked !== undefined && test.checked) {
        return true
      }
    }
    return false
  }

  refreshForSession(session: TestResultForDisplay) {
    this.reportService.getTestResult(session.session).subscribe((result) => {
      if (result == undefined) {
        // Session was deleted
        this.popupService.warning("The test session has been deleted by an administrator.")
        this.applyFilters()
        this.sessionRefreshCompleteEmitter.emit(result)
      } else {
        this.refreshSessionDiagram(session, result)
      }
    })
  }

  togglePendingAdminInteraction() {
    this.interactionLoadPending = true
    this.getActiveTests(this.currentActivePagingInfo())
  }

  toggleActiveSessionsCollapsedFinished(value: boolean) {
    setTimeout(() => {
      this.activeSessionsCollapsedFinished = value
    }, 1)
  }

  toggleCompletedSessionsCollapsedFinished(value: boolean) {
    setTimeout(() => {
      this.completedSessionsCollapsedFinished = value
    }, 1)
  }

  stopSession(session: TestResultForDisplay) {
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate this session?', 'Terminate', 'Cancel', Constants.BUTTON_ICON.DELETE).subscribe(() => {
      session.deletePending = true
      this.testService.stop(session.session).subscribe(() => {
        this.applyFilters()
        this.popupService.success('Test session terminated.')
      }).add(() => {
        session.deletePending = false
      })
    })
  }

  private applyCompletedDataToTestSession(displayedResult: TestResultForDisplay, loadedResult: TestResultReport) {
    displayedResult.endTime = loadedResult.result.endTime
    displayedResult.result = loadedResult.result.result
    displayedResult.obsolete = loadedResult.result.obsolete
    displayedResult.flagId = loadedResult.result.flagId
    const flag = this.dataService.getTestFlag(displayedResult.communityId, displayedResult.flagId)
    displayedResult.flagDisplay = flag ? { colour: flag.colour, name: flag.name } : undefined
    if (displayedResult.diagramState && loadedResult.result.outputMessage) {
      displayedResult.diagramState.outputMessage = loadedResult.result.outputMessage
      displayedResult.diagramState.outputMessageType = this.diagramLoaderService.determineOutputMessageType(loadedResult.result.result)
    }
  }

  private refreshSessionDiagram(session: TestResultForDisplay, result: TestResultReport) {
    this.diagramLoaderService.loadTestStepResults(session.session)
      .subscribe((data) => {
        const currentState = session.diagramState!
        if (result.result.endTime) {
          // Session completed
          this.popupService.info("The test session has completed.")
          this.applyCompletedDataToTestSession(session, result)
        }
        this.diagramLoaderService.updateStatusOfSteps(session, currentState.stepsOfTests[session.session], data)
      }).add(() => {
      this.sessionRefreshCompleteEmitter.emit(result)
    })
  }

  protected showActiveTestSessions() {
    return true
  }

  protected showTestSessionNavigationControls() {
    return true
  }

  protected showTestSessionDeleteControls() {
    return this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
  }

  protected showDeleteObsolete() {
    return !this.dataService.isVendorUser
  }

  activeSessionsHeaderClicked() {
    this.activeSessionsCollapsed = !this.activeSessionsCollapsed
    if (!this.activeSessionsCollapsed) {
      this.toggleActiveSessionsCollapsedFinished(false)
    }
  }

  completedSessionsHeaderClicked() {
    this.completedSessionsCollapsed = !this.completedSessionsCollapsed
    if (!this.completedSessionsCollapsed) {
      this.toggleCompletedSessionsCollapsedFinished(false)
    }
  }

}
