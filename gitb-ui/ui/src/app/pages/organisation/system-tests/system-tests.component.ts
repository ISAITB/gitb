import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { map } from 'lodash';
import { Observable } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { TestSuiteService } from 'src/app/services/test-suite.service';
import { TestService } from 'src/app/services/test.service';
import { Actor } from 'src/app/types/actor';
import { Domain } from 'src/app/types/domain';
import { FilterState } from 'src/app/types/filter-state';
import { Organisation } from 'src/app/types/organisation.type';
import { Specification } from 'src/app/types/specification';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { TestCase } from 'src/app/types/test-case';
import { TestResultReport } from 'src/app/types/test-result-report';
import { TestResultSearchCriteria } from 'src/app/types/test-result-search-criteria';
import { TestSuiteWithTestCases } from 'src/app/types/test-suite-with-test-cases';
import { TestResultForDisplay } from '../../../types/test-result-for-display';

@Component({
  selector: 'app-system-tests',
  templateUrl: './system-tests.component.html',
  styles: [
  ]
})
export class SystemTestsComponent implements OnInit {

  systemId!: number
  exportActivePending = false
  exportCompletedPending = false
  activeExpandedCounter = {count: 0}
  completedExpandedCounter = {count: 0}
  activeStatus = {status: Constants.STATUS.PENDING} 
  completedStatus = {status: Constants.STATUS.PENDING}
  organisation!: Organisation
  domainId?: number
  activeTestsColumns!: TableColumnDefinition[]
  completedTestsColumns!: TableColumnDefinition[]
  activeTests: TestResultForDisplay[] = []
  completedTests: TestResultForDisplay[] = []
  refreshActivePending = false
  refreshCompletedPending = false
  filterState: FilterState = {
    filters: [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.TEST_SUITE, Constants.FILTER_TYPE.TEST_CASE, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.TIME, Constants.FILTER_TYPE.SESSION ],
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

  domainLoader?: () => Observable<Domain[]>
  specificationLoader?: () => Observable<Specification[]>
  actorLoader?: () => Observable<Actor[]>
  testSuiteLoader?: () => Observable<TestSuiteWithTestCases[]>
  testCaseLoader?: () => Observable<TestCase[]>

  constructor(
    private route: ActivatedRoute,
    private reportService: ReportService,
    private testSuiteService: TestSuiteService,
    private conformanceService: ConformanceService,
    private dataService: DataService,
    private confirmationDialogService: ConfirmationDialogService,
    private testService: TestService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
    this.organisation = JSON.parse(localStorage.getItem(Constants.LOCAL_DATA.ORGANISATION)!)
    if (!this.dataService.isSystemAdmin && this.dataService.community?.domainId != undefined) {
      this.domainId = this.dataService.community.domainId
    }
    this.initFilterDataLoaders()
    if (this.domainId == undefined) {
      this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
    }
    this.activeTestsColumns = [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true, order: 'asc' }
    ]
    this.completedTestsColumns = [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true },
      { field: 'endTime', title: 'End time', sortable: true, order: 'desc' },
      { field: 'result', title: 'Result', sortable: true, iconFn: this.dataService.iconForTestResult }
    ]
    this.goFirstPage()
  }

  private initFilterDataLoaders() {
    // Domains
    this.domainLoader = (() => {
      return this.conformanceService.getDomainsForSystem(this.systemId)
    }).bind(this)
    // Specifications
    this.specificationLoader = (() => {
      return this.conformanceService.getSpecificationsForSystem(this.systemId)
    }).bind(this)
    // Actors
    this.actorLoader = (() => {
      return this.conformanceService.getActorsForSystem(this.systemId)
      }).bind(this)
    // Test cases
    this.testCaseLoader = (() => {
      return this.reportService.getTestCasesForSystem(this.systemId)
    }).bind(this)
    // Test suites
    this.testSuiteLoader = (() => {
      return this.testSuiteService.getTestSuitesWithTestCasesForSystem(this.systemId)
    }).bind(this)
  }

  queryDatabase() {
    this.getActiveTests()
    this.getCompletedTests()
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

  onReportExport(testResult: TestResultForDisplay) {
    if (!testResult.obsolete) {
      testResult.exportPending = true
      this.reportService.exportTestCaseReport(testResult.session, testResult.testCaseId!)
      .subscribe((data) => {
        const blobData = new Blob([data], {type: 'application/pdf'});
        saveAs(blobData, "report.pdf");
      }).add(() => {
        testResult.exportPending = false
      }) 
    }
  }

  getCurrentSearchCriteria() {
    let searchCriteria: TestResultSearchCriteria = {}
    let filterData:{[key: string]: any}|undefined = undefined
    if (this.filterState?.filterData) {
      filterData = this.filterState.filterData()
    }
    if (filterData) {
      searchCriteria.specIds = filterData[Constants.FILTER_TYPE.SPECIFICATION]
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
    searchCriteria.activeSortColumn = this.activeSortColumn
    searchCriteria.activeSortOrder = this.activeSortOrder
    searchCriteria.completedSortColumn = this.completedSortColumn
    searchCriteria.completedSortOrder = this.completedSortOrder
    searchCriteria.currentPage = this.currentPage
    return searchCriteria
  }

  private newTestResult(testResult: TestResultReport, completed: boolean): TestResultForDisplay {
    const result: Partial<TestResultForDisplay> = {
      session: testResult.result.sessionId,
      domain: testResult.domain?.sname,
      specification: testResult.specification?.sname,
      actor: testResult.actor?.name,
      testSuite: testResult.testSuite?.sname,
      testCase: testResult.test?.sname,
      startTime: testResult.result.startTime,
      specificationId: testResult.specification?.id,
      actorId: testResult.actor?.id,
      systemId: testResult.system?.id
    }
    if (completed) {
      result.endTime = testResult.result.endTime
      result.result = testResult.result.result
      result.obsolete = testResult.result.obsolete
    }
    return result as TestResultForDisplay
  }

  private newTestResultForDisplay(testResult: TestResultReport, completed: boolean) {
    const result: TestResultForDisplay = this.newTestResult(testResult, completed)
    result.testCaseId = testResult.test?.id
    return result
  }

  getActiveTests() {
    const params = this.getCurrentSearchCriteria()
    this.refreshActivePending = true
    this.activeExpandedCounter.count = 0
    this.setFilterRefreshState()
    this.reportService.getSystemActiveTestResults(this.systemId, params)
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
    this.reportService.getTestResults(this.systemId, params.currentPage!, Constants.TABLE_PAGE_SIZE, params)
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
    this.reportService.getTestResults(this.systemId, 1, 1000000, params)
    .subscribe((data) => {
      const tests = map(data.data, (testResult) => {
        return this.newTestResult(testResult, true)
      })
      const headers = ['Session', this.dataService.labelDomain(), this.dataService.labelSpecification(), this.dataService.labelActor(), 'Test suite', 'Test case', 'Start time', 'End time', 'Result', 'Obsolete']
      this.dataService.exportAllAsCsv(headers, tests)
    }).add(() => {
      this.exportCompletedPending = false
    })
  }

  exportActiveSessionsToCsv() {
    this.exportActivePending = true
    const params = this.getCurrentSearchCriteria()
    this.reportService.getSystemActiveTestResults(this.systemId, params)
    .subscribe((data) => {
      const tests = map(data.data, (testResult) => {
        return this.newTestResult(testResult, false)
      })
      const headers = ['Session', this.dataService.labelDomain(), this.dataService.labelSpecification(), this.dataService.labelActor(), 'Test suite', 'Test case', 'Start time']
      this.dataService.exportAllAsCsv(headers, tests)
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

  stopSession(session: TestResultForDisplay) {
    this.confirmationDialogService.confirmed('Confirm delete', 'Are you certain you want to terminate this session?', 'Yes', 'No')
    .subscribe(() => {
      session.deletePending = true
      this.testService.stop(session.session)
      .subscribe(() => {
        this.getActiveTests()
        this.popupService.success('Test session terminated.')
      }).add(() => {
        session.deletePending = false
      })
    })
  }

  deleteObsolete() {
    this.confirmationDialogService.confirmed('Confirm delete', 'Are you sure you want to delete all obsolete test results?', 'Yes', 'No')
    .subscribe(() => {
      this.deletePending = true
      this.conformanceService.deleteObsoleteTestResultsForSystem(this.systemId)
      .subscribe(() => {
        this.getCompletedTests()
        this.popupService.success('Obsolete test results deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

}
