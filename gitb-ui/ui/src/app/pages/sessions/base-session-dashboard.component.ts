import {Component, EventEmitter, OnInit} from '@angular/core';
import {BaseSessionDashboardSupportComponent} from '../organisation/base-session-dashboard-support.component';
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
import {filter, map} from 'lodash';
import {TestResultReport} from '../../types/test-result-report';
import {TestResultForExport} from '../admin/session-dashboard/test-result-for-export';
import {saveAs} from 'file-saver';
import {FieldInfo} from '../../types/field-info';
import {TestResultData} from '../../types/test-result-data';
import {SessionData} from '../../components/diagram/test-session-presentation/session-data';

@Component({
  template: '',
  standalone: false
})
export abstract class BaseSessionDashboardComponent extends BaseSessionDashboardSupportComponent implements OnInit {

  showActiveSessions = false
  showSessionNavigationControls = false
  showDeleteControls = false
  exportActivePending = false
  exportCompletedPending = false
  interactionLoadPending = false
  pendingAdminInteraction = false
  sessionsPendingAdminInteraction: Set<string>|undefined
  selectingForDelete = false
  activeExpandedCounter = {count: 0}
  completedExpandedCounter = {count: 0}
  activeStatus = {status: Constants.STATUS.PENDING}
  completedStatus = {status: Constants.STATUS.PENDING}
  communityId?: number
  activeTestsColumns!: TableColumnDefinition[]
  completedTestsColumns!: TableColumnDefinition[]
  activeTests: TestResultForDisplay[] = []
  activeTestsToDisplay: TestResultForDisplay[] = []
  completedTests: TestResultForDisplay[] = []
  completedTestsCheckboxEmitter = new EventEmitter<boolean>()
  action = false
  refreshActivePending = false
  refreshCompletedPending = false
  filterState: FilterState = {
    filters: [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.SPECIFICATION_GROUP, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.TEST_SUITE, Constants.FILTER_TYPE.TEST_CASE, Constants.FILTER_TYPE.ORGANISATION, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.START_TIME, Constants.FILTER_TYPE.END_TIME, Constants.FILTER_TYPE.SESSION, Constants.FILTER_TYPE.ORGANISATION_PROPERTY, Constants.FILTER_TYPE.SYSTEM_PROPERTY ],
    updatePending: false
  }
  deletePending = false
  deleteSessionsPending = false
  stopAllPending = false
  activeSessionsCollapsed = false
  activeSessionsCollapsedFinished = false
  completedSessionsCollapsed = false
  completedSessionsCollapsedFinished = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private reportService: ReportService,
    confirmationDialogService: ConfirmationDialogService,
    testService: TestService,
    popupService: PopupService,
    private route: ActivatedRoute,
    diagramLoaderService: DiagramLoaderService,
    protected routingService: RoutingService
  ) { super(testService, confirmationDialogService, popupService, diagramLoaderService) }

  ngOnInit(): void {
    this.showActiveSessions = this.showActiveTestSessions()
    this.showSessionNavigationControls = this.showTestSessionNavigationControls()
    this.showDeleteControls = this.showTestSessionDeleteControls()
    const sessionIdValue = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID)
    if (sessionIdValue != undefined) {
      this.sessionIdToShow = sessionIdValue
    }
    if (!this.dataService.isSystemAdmin) {
      this.communityId = this.dataService.community!.id
    }
    this.activeTestsColumns = [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true, order: 'asc' },
      { field: 'organization', title: this.dataService.labelOrganisation(), sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true }
    ]
    this.completedTestsColumns = [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true },
      { field: 'endTime', title: 'End time', sortable: true, order: 'desc' },
      { field: 'organization', title: this.dataService.labelOrganisation(), sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true },
      { field: 'result', title: 'Result', sortable: true, iconFn: this.dataService.iconForTestResult, iconTooltipFn: this.dataService.tooltipForTestResult }
    ]
    if (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.community!.domain == undefined)) {
      this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
    }
    if (this.dataService.isSystemAdmin) {
      this.filterState.filters.push(Constants.FILTER_TYPE.COMMUNITY)
    }
    this.setBreadcrumbs()
    this.applyFilters()
  }

  protected setBreadcrumbs() {
    this.routingService.sessionDashboardBreadcrumbs()
  }

  setFilterRefreshState() {
    this.filterState.updatePending = this.refreshActivePending || this.refreshCompletedPending
  }

  getCurrentSearchCriteria() {
    let searchCriteria: TestResultSearchCriteria = {}
    if (!this.dataService.isSystemAdmin) {
      searchCriteria.communityIds = [this.dataService.community!.id]
      if (this.dataService.community?.domain != undefined) {
        searchCriteria.domainIds = [this.dataService.community.domain.id]
      }
    }
    let filterData:{[key: string]: any}|undefined = undefined
    if (this.filterState?.filterData) {
      filterData = this.filterState.filterData()
    }
    if (filterData) {
      this.dataService.addAdminCriteriaToTestResultSearchCriteria(searchCriteria, filterData)
      searchCriteria.organisationIds = filterData[Constants.FILTER_TYPE.ORGANISATION]
      searchCriteria.systemIds = filterData[Constants.FILTER_TYPE.SYSTEM]
      searchCriteria.organisationProperties = filterData.organisationProperties
      searchCriteria.systemProperties = filterData.systemProperties
    }
    this.addSessionDashboardCriteriaToTestResultSearchCriteria(searchCriteria, filterData)
    return searchCriteria
  }

  getActiveTests() {
    const params = this.getCurrentSearchCriteria()
    this.refreshActivePending = true
    this.activeExpandedCounter.count = 0
    this.setFilterRefreshState()
    this.reportService.getActiveTestResults(params)
      .pipe(
        mergeMap((data) => {
          this.activeTests = map(data.data, (testResult) => {
            return this.newTestResultForDisplay(testResult, false)
          })
          this.sessionsPendingAdminInteraction = undefined
          return this.filterActiveTests(this.activeTests, false)
        })
      )
      .subscribe((tests) => {
        this.activeTestsToDisplay = tests
      })
      .add(() => {
        this.interactionLoadPending = false
        this.refreshActivePending = false
        this.setFilterRefreshState()
        this.activeStatus.status = Constants.STATUS.FINISHED
      })
  }

  getCompletedTests() {
    this.completedTestsCheckboxEmitter.emit(false)
    this.completedExpandedCounter.count = 0
    this.selectingForDelete = false
    const params = this.getCurrentSearchCriteria()
    this.refreshCompletedPending = true
    this.setFilterRefreshState()
    this.reportService.getCompletedTestResults(params.currentPage!, Constants.TABLE_PAGE_SIZE, params)
      .subscribe((data) => {
        this.completedTestsTotalCount = data.count!
        this.completedTests = map(data.data, (testResult) => {
          return this.newTestResultForDisplay(testResult, true)
        })
        this.updatePagination()
      }).add(() => {
      this.refreshCompletedPending = false
      this.setFilterRefreshState()
      this.completedStatus.status = Constants.STATUS.FINISHED
    })
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

  private newTestResultForExport(testResult: TestResultReport, completed: boolean, orgParameters?: string[], sysParameters?: string[]) {
    const result: TestResultForExport = this.newTestResult(testResult, completed)
    if (orgParameters !== undefined) {
      for (let param of orgParameters) {
        if (testResult.organization && testResult.organization.parameters) {
          result['organization_'+param] = testResult.organization.parameters[param]
        }
      }
    }
    if (sysParameters !== undefined) {
      for (let param of sysParameters) {
        if (testResult.system && testResult.system.parameters) {
          result['system_'+param] = testResult.system.parameters[param]
        }
      }
    }
    return result
  }

  private newTestResultForDisplay(testResult: TestResultReport, completed: boolean) {
    const result: TestResultForDisplay = this.newTestResult(testResult, completed)
    result.testSuiteId = testResult.testSuite?.id
    result.testCaseId = testResult.test?.id
    if (this.sessionIdToShow != undefined && this.sessionIdToShow == testResult.result.sessionId) {
      // We have been asked to open a session. Set it as expand and keep it once.
      result.expanded = true
      delete this.sessionIdToShow
    }
    return result
  }

  sortActiveSessions(column: TableColumnDefinition) {
    this.activeSortColumn = column.field
    this.activeSortOrder = column.order!
    this.getActiveTests()
  }

  sortCompletedSessions(column: TableColumnDefinition) {
    this.completedSortColumn = column.field
    this.completedSortOrder = column.order!
    this.getCompletedTests()
  }

  stopAll() {
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate all active sessions?', 'Terminate', 'Cancel')
      .subscribe(() => {
        let result: Observable<void>
        if (this.dataService.isSystemAdmin) {
          result = this.testService.stopAll()
        } else {
          result = this.testService.stopAllCommunitySessions(this.communityId!)
        }
        this.stopAllPending = true
        result.subscribe(() => {
          this.queryDatabase()
          this.popupService.success('Test sessions terminated.')
        }).add(() => {
          this.stopAllPending = false
        })
      })
  }

  queryDatabase(onlyCompleted?: boolean) {
    if (this.showActiveSessions && onlyCompleted != false) {
      this.getActiveTests()
    }
    this.getCompletedTests()
  }

  filterControlApplied() {
    this.sessionIdToShow = undefined
    this.applyFilters()
  }

  applyFilters() {
    this.currentPage = 1
    this.queryDatabase()
  }

  goFirstPage() {
    this.currentPage = 1
    this.queryDatabase(true)
  }

  goPreviousPage() {
    this.currentPage -= 1
    this.queryDatabase(true)
  }

  goNextPage() {
    this.currentPage += 1
    this.queryDatabase(true)
  }

  goLastPage() {
    this.currentPage = Math.ceil(this.completedTestsTotalCount / Constants.TABLE_PAGE_SIZE)
    this.queryDatabase(true)
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
    this.reportService.getCompletedTestResults(1, 1000000, params, true)
      .subscribe((data) => {
        const fields: FieldInfo[] = [
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
        this.addCustomPropertiesToExportFields(data, fields)
        const tests = map(data.data, (testResult) => {
          return this.newTestResultForExport(testResult, true, data.orgParameters, data.sysParameters)
        })
        this.dataService.exportAllAsCsv(fields, tests)
      }).add(() => {
      this.exportCompletedPending = false
    })
  }

  exportActiveSessionsToCsv() {
    this.exportActivePending = true
    const params = this.getCurrentSearchCriteria()
    this.reportService.getActiveTestResults(params, true)
      .pipe(
        mergeMap((data) => {
          const fields: FieldInfo[] = [
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
          this.addCustomPropertiesToExportFields(data, fields)
          const tests = map(data.data, (testResult) => {
            return this.newTestResultForExport(testResult, false, data.orgParameters, data.sysParameters)
          })
          return this.filterActiveTests(tests, false).pipe(
            mergeMap((testsToExport) => {
              return of({fields: fields, tests: testsToExport})
            })
          )
        })
      )
      .subscribe((exportData) => {
        this.dataService.exportAllAsCsv(exportData.fields, exportData.tests)
      })
      .add(() => {
        this.exportActivePending = false
      })
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

  deleteObsolete() {
    this.confirmationDialogService.confirmedDangerous('Confirm delete', 'Are you sure you want to delete all obsolete test results?', 'Delete', 'Cancel')
      .subscribe(() => {
        this.deletePending = true
        let result: Observable<any>
        if (this.dataService.isCommunityAdmin && this.dataService.community?.id !== undefined) {
          result = this.conformanceService.deleteObsoleteTestResultsForCommunity(this.dataService.community.id)
        } else {
          result = this.conformanceService.deleteObsoleteTestResults()
        }
        result.subscribe(() => {
          this.getCompletedTests()
          this.popupService.success('Obsolete test results deleted.')
        }).add(() => {
          this.deletePending = false
        })
      })
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
            this.getCompletedTests()
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

  private filterActiveTests<Type extends SessionData>(tests: Type[], showToggleAsBusy: boolean): Observable<Type[]> {
    if (this.pendingAdminInteraction) {
      return this.getPendingSessionsForAdminInput(showToggleAsBusy)
        .pipe(
          mergeMap((sessionIds) => {
            if (sessionIds.size == 0) {
              return of([])
            } else {
              return of(filter(tests, (test) => this.sessionsPendingAdminInteraction!.has(test.session)))
            }
          })
        )
    } else {
      return of(tests)
    }
  }

  private getPendingSessionsForAdminInput(showToggleAsBusy: boolean): Observable<Set<string>> {
    if (this.sessionsPendingAdminInteraction) {
      return of(this.sessionsPendingAdminInteraction)
    } else {
      if (showToggleAsBusy) {
        this.interactionLoadPending = true
      }
      return this.reportService.getPendingTestSessionsForAdminInteraction(this.communityId)
        .pipe(
          mergeMap((data) => {
            const sessionIds = new Set<string>()
            for (let sessionId of data) {
              sessionIds.add(sessionId)
            }
            this.sessionsPendingAdminInteraction = sessionIds
            return of(sessionIds)
          })
        )
    }
  }

  togglePendingAdminInteraction() {
    this.filterActiveTests(this.activeTests, true)
      .subscribe((filteredTests) => {
        this.activeTestsToDisplay = filteredTests
      })
      .add(() => {
        this.interactionLoadPending = false
      })
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
