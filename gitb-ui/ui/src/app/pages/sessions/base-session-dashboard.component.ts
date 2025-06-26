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

import {AfterViewInit, Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
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
import {ActivatedRoute} from '@angular/router';
import {DiagramLoaderService} from '../../components/diagram/test-session-presentation/diagram-loader.service';
import {RoutingService} from '../../services/routing.service';
import {TestResultSearchCriteria} from '../../types/test-result-search-criteria';
import {mergeMap, Observable, of, share} from 'rxjs';
import {map} from 'lodash';
import {TestResultReport} from '../../types/test-result-report';
import {TestResultForExport} from '../admin/session-dashboard/test-result-for-export';
import {saveAs} from 'file-saver';
import {FieldInfo} from '../../types/field-info';
import {TestResultData} from '../../types/test-result-data';
import {SessionTableComponent} from '../../components/session-table/session-table.component';
import {PagingEvent} from '../../components/paging-controls/paging-event';

@Component({
  template: '',
  standalone: false
})
export abstract class BaseSessionDashboardComponent implements OnInit, AfterViewInit {

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
    filters: [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.SPECIFICATION_GROUP, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.TEST_SUITE, Constants.FILTER_TYPE.TEST_CASE, Constants.FILTER_TYPE.ORGANISATION, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.START_TIME, Constants.FILTER_TYPE.END_TIME, Constants.FILTER_TYPE.SESSION, Constants.FILTER_TYPE.ORGANISATION_PROPERTY, Constants.FILTER_TYPE.SYSTEM_PROPERTY ],
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
  activeSortOrder = "asc"
  activeSortColumn = "startTime"
  completedSortOrder = "desc"
  completedSortColumn = "endTime"
  copyForOtherRoleOption = false

  @ViewChild("completedSessions") completedSessionsTable?: SessionTableComponent
  @ViewChild("activeSessions") activeSessionsTable?: SessionTableComponent

  constructor(
    public readonly dataService: DataService,
    protected readonly conformanceService: ConformanceService,
    protected readonly reportService: ReportService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    protected readonly testService: TestService,
    private readonly popupService: PopupService,
    protected readonly route: ActivatedRoute,
    private readonly diagramLoaderService: DiagramLoaderService,
    protected readonly routingService: RoutingService
  ) { }

  ngOnInit(): void {
    this.showActiveSessions = this.showActiveTestSessions()
    this.showSessionNavigationControls = this.showTestSessionNavigationControls()
    this.showDeleteControls = this.showTestSessionDeleteControls()
    this.showTogglePendingAdminInteraction = this.showTogglePendingAdminInteractionControl()
    this.copyForOtherRoleOption = this.showCopyForOtherRoleOption()
    const sessionIdValue = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID)
    if (sessionIdValue != undefined) {
      this.sessionIdToShow = sessionIdValue
    }
    if (!this.dataService.isSystemAdmin) {
      this.communityId = this.dataService.community!.id
    }
    this.activeTestsColumns = this.getActiveTestsColumns()
    this.completedTestsColumns = this.getCompletedTestsColumns()
    if (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.community!.domain == undefined)) {
      this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
    }
    if (this.dataService.isSystemAdmin) {
      this.filterState.filters.push(Constants.FILTER_TYPE.COMMUNITY)
    }
    this.showDeleteObsoleteControl = !this.dataService.isVendorUser
    this.setBreadcrumbs()
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.filterState.updatePending = true
      this.applyFilters()
    })
  }

  protected showCopyForOtherRoleOption(): boolean {
    return true
  }

  protected showTogglePendingAdminInteractionControl(): boolean {
    return true
  }

  protected getActiveTestsColumns(): TableColumnDefinition[] {
    return [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true, order: 'asc' },
      { field: 'organization', title: this.dataService.labelOrganisation(), sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true }
    ]
  }

  protected getCompletedTestsColumns(): TableColumnDefinition[] {
    return [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true },
      { field: 'endTime', title: 'End time', sortable: true, order: 'desc' },
      { field: 'organization', title: this.dataService.labelOrganisation(), sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true },
      { field: 'result', title: 'Result', sortable: true, iconFn: this.dataService.iconForTestResult, iconTooltipFn: this.dataService.tooltipForTestResult }
    ]
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
      searchCriteria.startTimeBeginStr = filterData.startTimeBeginStr
      searchCriteria.startTimeEndStr = filterData.startTimeEndStr
      searchCriteria.endTimeBeginStr = filterData.endTimeBeginStr
      searchCriteria.endTimeEndStr = filterData.endTimeEndStr
      searchCriteria.sessionId = filterData.sessionId
    } else if (this.sessionIdToShow != undefined) {
      searchCriteria.sessionId = this.sessionIdToShow
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
      this.activeTests = map(data.data, (testResult) => {
        return this.newTestResultForDisplay(testResult, false)
      })
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
      this.completedTests = map(data.data, (testResult) => {
        return this.newTestResultForDisplay(testResult, true)
      })
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
    if (this.sessionIdToShow != undefined && this.sessionIdToShow == testResult.result.sessionId) {
      // We have been asked to open a session. Set it as expand and keep it once.
      result.expanded = true
      this.sessionIdToShow = undefined
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
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate all active sessions?', 'Terminate', 'Cancel').subscribe(() => {
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

  exportVisible(session: TestResultForDisplay) {
    return session.obsolete == undefined || !session.obsolete
  }

  onReportExportPdf(testResult: TestResultForDisplay) {
    if (!testResult.obsolete) {
      testResult.exportPending = true
      this.onReportExport(testResult, 'application/pdf', 'report.pdf')
        .subscribe(() => {}).add(() => {
        testResult.exportPending = false
      })
    }
  }

  onReportExportXml(testResult: TestResultForDisplay) {
    if (!testResult.obsolete) {
      testResult.actionPending = true
      this.onReportExport(testResult, 'application/xml', 'report.xml')
        .subscribe(() => {}).add(() => {
        testResult.actionPending = false
      })
    }
  }

  onReportExport(testResult: TestResultForDisplay, contentType: string, fileName: string) {
    return this.reportService.exportTestCaseReport(testResult.session, testResult.testCaseId!, contentType)
      .pipe(
        mergeMap((data) => {
          const blobData = new Blob([data], {type: contentType});
          saveAs(blobData, fileName);
          return of(data)
        }),
        share()
      )
  }

  exportCompletedSessionsToCsv() {
    this.exportCompletedPending = true
    const params = this.getCurrentSearchCriteria()
    this.loadCompletedTests(1, 100000000, params, true).subscribe((data) => {
      const fields = this.getExportFieldInfoForCompletedTests()
      this.addExtraExportData(data, fields)
      const tests = map(data.data, (testResult) => {
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
      const tests = map(data.data, (testResult) => {
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
    return {
      targetPage: (reset == true)?1:this.completedSessionsTable?.pagingControls?.getCurrentStatus().currentPage!,
      targetPageSize: this.completedSessionsTable?.pagingControls?.getCurrentStatus().pageSize!
    }
  }

  private currentActivePagingInfo(reset?: boolean): PagingEvent {
    return {
      targetPage: (reset == true)?1:this.activeSessionsTable?.pagingControls?.getCurrentStatus().currentPage!,
      targetPageSize: this.activeSessionsTable?.pagingControls?.getCurrentStatus().pageSize!
    }
  }

  deleteObsolete() {
    this.confirmationDialogService.confirmedDangerous('Confirm delete', 'Are you sure you want to delete all obsolete test results?', 'Delete', 'Cancel').subscribe(() => {
      this.deletePending = true
      this.deleteObsoleteOperation().subscribe(() => {
        this.getCompletedTests(this.currentCompletedPagingInfo())
        this.popupService.success('Obsolete test results deleted.')
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
    this.confirmationDialogService.confirmedDangerous('Confirm delete', msg, 'Delete', 'Cancel')
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
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate this session?', 'Terminate', 'Cancel').subscribe(() => {
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
    return true
  }
}
