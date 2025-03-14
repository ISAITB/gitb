import { Component, EventEmitter, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { map } from 'lodash';
import { Constants } from 'src/app/common/constants';
import { DiagramLoaderService } from 'src/app/components/diagram/test-session-presentation/diagram-loader.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { TestService } from 'src/app/services/test.service';
import { FilterState } from 'src/app/types/filter-state';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { TestResultReport } from 'src/app/types/test-result-report';
import { TestResultSearchCriteria } from 'src/app/types/test-result-search-criteria';
import { TestResultForDisplay } from '../../../types/test-result-for-display';
import { saveAs } from 'file-saver'
import { mergeMap, share, of } from 'rxjs';
import { RoutingService } from 'src/app/services/routing.service';
import { FieldInfo } from 'src/app/types/field-info';

@Component({
  selector: 'app-organisation-tests',
  templateUrl: './organisation-tests.component.html',
  styles: [
  ]
})
export class OrganisationTestsComponent implements OnInit {

  exportActivePending = false
  exportCompletedPending = false
  activeExpandedCounter = {count: 0}
  completedExpandedCounter = {count: 0}
  activeStatus = {status: Constants.STATUS.PENDING} 
  completedStatus = {status: Constants.STATUS.PENDING}
  organisationId!: number
  domainId?: number
  activeTestsColumns!: TableColumnDefinition[]
  completedTestsColumns!: TableColumnDefinition[]
  activeTests: TestResultForDisplay[] = []
  completedTests: TestResultForDisplay[] = []
  refreshActivePending = false
  refreshCompletedPending = false
  filterState: FilterState = {
    filters: [ Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.SPECIFICATION_GROUP, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.TEST_SUITE, Constants.FILTER_TYPE.TEST_CASE, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.START_TIME, Constants.FILTER_TYPE.END_TIME, Constants.FILTER_TYPE.SESSION ],
    updatePending: false
  }
  currentPage = 1
  completedTestsTotalCount = 0
  activeSortOrder = "asc"
  activeSortColumn = "startTime"
  completedSortOrder = "desc"
  completedSortColumn = "endTime"
  isPreviousPageDisabled = false
  isNextPageDisabled = false
  deletePending = false
  stopAllPending = false
  sessionIdToShow?: string
  sessionRefreshCompleteEmitter = new EventEmitter<TestResultReport|undefined>()
  activeSessionsCollapsed = false
  activeSessionsCollapsedFinished = false
  completedSessionsCollapsed = false
  completedSessionsCollapsedFinished = false

  constructor(
    private route: ActivatedRoute,
    private reportService: ReportService,
    private conformanceService: ConformanceService,
    public dataService: DataService,
    private confirmationDialogService: ConfirmationDialogService,
    private testService: TestService,
    private popupService: PopupService,
    private diagramLoaderService: DiagramLoaderService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
    this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    const sessionIdValue = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID)
    if (sessionIdValue != undefined) {
      this.sessionIdToShow = sessionIdValue
    }
    if (!this.dataService.isSystemAdmin && this.dataService.community?.domainId != undefined) {
      this.domainId = this.dataService.community.domainId
    }
    if (this.domainId == undefined) {
      this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
    }
    this.activeTestsColumns = [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true, order: 'asc' }
    ]
    this.completedTestsColumns = [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true },
      { field: 'endTime', title: 'End time', sortable: true, order: 'desc' },
      { field: 'result', title: 'Result', sortable: true, iconFn: this.dataService.iconForTestResult, iconTooltipFn: this.dataService.tooltipForTestResult }
    ]
    this.routingService.testHistoryBreadcrumbs(this.organisationId)
    this.goFirstPage()
  }

  queryDatabase() {
    this.getActiveTests()
    this.getCompletedTests()
  }

  applyFilters() {
    this.sessionIdToShow = undefined
    this.goFirstPage()
  }

  goFirstPage() {
    this.currentPage = 1
    this.queryDatabase()
  }

  goPreviousPage() {
    this.currentPage -= 1
    this.queryDatabase()
  }

  goNextPage() {
    this.currentPage += 1
    this.queryDatabase()
  }

  goLastPage() {
    this.currentPage = Math.ceil(this.completedTestsTotalCount / Constants.TABLE_PAGE_SIZE)
    this.queryDatabase()
  }

  updatePagination() {
    if (this.currentPage == 1) {
      this.isNextPageDisabled = this.completedTestsTotalCount <= Constants.TABLE_PAGE_SIZE
      this.isPreviousPageDisabled = true
    } else if (this.currentPage == Math.ceil(this.completedTestsTotalCount / Constants.TABLE_PAGE_SIZE)) {
      this.isNextPageDisabled = true
      this.isPreviousPageDisabled = false
    } else {
      this.isNextPageDisabled = false
      this.isPreviousPageDisabled = false
    }
  }

  rowStyle(row: TestResultForDisplay) {
    if (row.obsolete) {
      return 'test-result-obsolete'
    } else {
      return ''
    }
  }

  exportVisible(session: TestResultForDisplay) {
    return session.obsolete === undefined || !session.obsolete
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

  onReportExportXml(testResult: TestResultForDisplay) {
    if (!testResult.obsolete) {
      testResult.actionPending = true
      this.onReportExport(testResult, 'application/xml', 'report.xml')
      .subscribe(() => {}).add(() => {
        testResult.actionPending = false
      })
    }
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

  private onReportExport(testResult: TestResultForDisplay, contentType: string, fileName: string) {
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
      if (this.domainId != undefined) {
        searchCriteria.domainIds = [this.domainId]
      } else {
        searchCriteria.domainIds = filterData[Constants.FILTER_TYPE.DOMAIN]
      }
      searchCriteria.results = filterData[Constants.FILTER_TYPE.RESULT]
      searchCriteria.startTimeBeginStr = filterData.startTimeBeginStr
      searchCriteria.startTimeEndStr = filterData.startTimeEndStr
      searchCriteria.endTimeBeginStr = filterData.endTimeBeginStr
      searchCriteria.endTimeEndStr = filterData.endTimeEndStr
      searchCriteria.sessionId = filterData.sessionId
    }
    if (this.sessionIdToShow != undefined) {
      searchCriteria.sessionId = this.sessionIdToShow
    }
    searchCriteria.activeSortColumn = this.activeSortColumn
    searchCriteria.activeSortOrder = this.activeSortOrder
    searchCriteria.completedSortColumn = this.completedSortColumn
    searchCriteria.completedSortOrder = this.completedSortOrder
    searchCriteria.currentPage = this.currentPage
    return searchCriteria
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

  private newTestResult(testResult: TestResultReport, completed: boolean): TestResultForDisplay {
    const result: Partial<TestResultForDisplay> = {
      session: testResult.result.sessionId,
      domain: testResult.domain?.sname,
      domainId: testResult.domain?.id,
      specification: testResult.specification?.sname,
      actor: testResult.actor?.name,
      testSuite: testResult.testSuite?.sname,
      testCase: testResult.test?.sname,
      startTime: testResult.result.startTime,
      specificationId: testResult.specification?.id,
      actorId: testResult.actor?.id,
      systemId: testResult.system?.id,
      system: testResult.system?.sname,
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
    result.testCaseId = testResult.test?.id
    result.testSuiteId = testResult.testSuite?.id
    if (this.sessionIdToShow != undefined && this.sessionIdToShow == testResult.result.sessionId) {
      // We have been asked to open a session. Set it as expand and keep it once.
      result.expanded = true
      delete this.sessionIdToShow
    }
    return result
  }

  getActiveTests() {
    const params = this.getCurrentSearchCriteria()
    this.refreshActivePending = true
    this.activeExpandedCounter.count = 0
    this.setFilterRefreshState()
    this.reportService.getSystemActiveTestResults(this.organisationId, params)
    .subscribe((data) => {
      this.activeTests = map(data.data, (testResult) => {
        return this.newTestResultForDisplay(testResult, false)
      })
    }).add(() => {
      this.refreshActivePending = false
      this.setFilterRefreshState()
      this.activeStatus.status = Constants.STATUS.FINISHED      
    })
  }

  getCompletedTests() {
    this.completedExpandedCounter.count = 0
    const params = this.getCurrentSearchCriteria()
    this.refreshCompletedPending = true
    this.setFilterRefreshState()
    this.reportService.getTestResults(this.organisationId, params.currentPage!, Constants.TABLE_PAGE_SIZE, params)
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

  exportCompletedSessionsToCsv() {
    this.exportCompletedPending = true
    const params = this.getCurrentSearchCriteria()
    this.reportService.getTestResults(this.organisationId, 1, 1000000, params)
    .subscribe((data) => {
      const tests = map(data.data, (testResult) => {
        return this.newTestResult(testResult, true)
      })
      const fields: FieldInfo[] = [
        { header: 'Session', field: 'session'},
        { header: this.dataService.labelDomain(), field: 'domain'},
        { header: this.dataService.labelSpecification(), field: 'specification'},
        { header: this.dataService.labelActor(), field: 'actor'},
        { header: 'Test suite', field: 'testSuite'},
        { header: 'Test case', field: 'testCase'},
        { header: this.dataService.labelSystem(), field: 'system'},
        { header: 'Start time', field: 'startTime'},
        { header: 'End time', field: 'endTime'},
        { header: 'Result', field: 'result'},
        { header: 'Obsolete', field: 'obsolete'}
      ]
      this.dataService.exportAllAsCsv(fields, tests)
    }).add(() => {
      this.exportCompletedPending = false
    })
  }

  exportActiveSessionsToCsv() {
    this.exportActivePending = true
    const params = this.getCurrentSearchCriteria()
    this.reportService.getSystemActiveTestResults(this.organisationId, params)
    .subscribe((data) => {
      const tests = map(data.data, (testResult) => {
        return this.newTestResult(testResult, false)
      })
      const fields: FieldInfo[] = [
        { header: 'Session', field: 'session'},
        { header: this.dataService.labelDomain(), field: 'domain'},
        { header: this.dataService.labelSpecification(), field: 'specification'},
        { header: this.dataService.labelActor(), field: 'actor'},
        { header: 'Test suite', field: 'testSuite'},
        { header: 'Test case', field: 'testCase'},
        { header: this.dataService.labelSystem(), field: 'system'},
        { header: 'Start time', field: 'startTime'},
      ]
      this.dataService.exportAllAsCsv(fields, tests)
    }).add(() => {
      this.exportActivePending = false
    })
  }

  setFilterRefreshState() {
    this.filterState.updatePending = this.refreshActivePending || this.refreshCompletedPending
  }

  canDelete() {
    return !this.dataService.isVendorUser
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

  stopAll() {
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate all active sessions?', 'Terminate', 'Cancel')
    .subscribe(() => {
      let result = this.testService.stopAllOrganisationSessions(this.organisationId)
      this.stopAllPending = true
      result.subscribe(() => {
        this.queryDatabase()
        this.popupService.success('Test sessions terminated.')
      }).add(() => {
        this.stopAllPending = false
      })
    })
  }
  
  stopSession(session: TestResultForDisplay) {
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate this session?', 'Terminate', 'Cancel')
    .subscribe(() => {
      session.deletePending = true
      this.testService.stop(session.session)
      .subscribe(() => {
        this.queryDatabase()
        this.popupService.success('Test session terminated.')
      }).add(() => {
        session.deletePending = false
      })
    })
  }

  deleteObsolete() {
    this.confirmationDialogService.confirmedDangerous('Confirm delete', 'Are you sure you want to delete all obsolete test results?', 'Delete', 'Cancel')
    .subscribe(() => {
      this.deletePending = true
      this.conformanceService.deleteObsoleteTestResultsForOrganisation(this.organisationId)
      .subscribe(() => {
        this.getCompletedTests()
        this.popupService.success('Obsolete test results deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  refreshForSession(session: TestResultForDisplay) {
    this.reportService.getTestResult(session.session).subscribe((result) => {
      if (result == undefined) {
        // Session was deleted
        this.popupService.warning("The test session has been deleted by an administrator.")
        this.goFirstPage()
        this.sessionRefreshCompleteEmitter.emit(result)
      } else {
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

}
