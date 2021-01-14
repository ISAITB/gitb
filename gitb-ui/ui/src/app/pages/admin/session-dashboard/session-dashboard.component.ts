import { Component, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { CommunityService } from 'src/app/services/community.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { ReportService } from 'src/app/services/report.service';
import { SystemConfigurationService } from 'src/app/services/system-configuration.service';
import { SystemService } from 'src/app/services/system.service';
import { TestSuiteService } from 'src/app/services/test-suite.service';
import { FilterState } from 'src/app/types/filter-state';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { TestResultSearchCriteria } from 'src/app/types/test-result-search-criteria';
import { map } from 'lodash'
import { TestResultReport } from 'src/app/types/test-result-report';
import { TestResultForExport } from './test-result-for-export';
import { TestResultForDisplay } from './test-result-for-display';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { TestService } from 'src/app/services/test.service';
import { PopupService } from 'src/app/services/popup.service';
import { Observable } from 'rxjs';
import { Domain } from 'src/app/types/domain';
import { Specification } from 'src/app/types/specification';
import { Actor } from 'src/app/types/actor';
import { TestCase } from 'src/app/types/test-case';
import { Community } from 'src/app/types/community';
import { Organisation } from 'src/app/types/organisation.type';
import { System } from 'src/app/types/system';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { SystemParameter } from 'src/app/types/system-parameter';
import { TestSuiteWithTestCases } from 'src/app/types/test-suite-with-test-cases';

@Component({
  selector: 'app-session-dashboard',
  templateUrl: './session-dashboard.component.html',
  styles: [
  ]
})
export class SessionDashboardComponent implements OnInit {

  exportActivePending = false
  exportCompletedPending = false
  viewCheckbox = false
  selectingForDelete = false
  activeExpandedCounter = {count: 0}
  completedExpandedCounter = {count: 0}
  activeStatus = {status: Constants.STATUS.PENDING} 
  completedStatus = {status: Constants.STATUS.PENDING}
  communityId?: number
  activeTestsColumns!: TableColumnDefinition[]
  completedTestsColumns!: TableColumnDefinition[]
  activeTests: TestResultForDisplay[] = []
  completedTests: TestResultForDisplay[] = []
  completedTestsTotalCount = 0
  activeSortOrder = "asc"
  activeSortColumn = "startTime"
  completedSortOrder = "desc"
  completedSortColumn = "endTime"
  currentPage = 1
  isPreviousPageDisabled = false
  isNextPageDisabled = false
  action = false
  stop = false
  ttlEnabled = false
  prevParameter?: number
  showFilters = false
  refreshActivePending = false
  refreshCompletedPending = false
  filterState: FilterState = {
    filters: [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.TEST_SUITE, Constants.FILTER_TYPE.TEST_CASE, Constants.FILTER_TYPE.ORGANISATION, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.TIME, Constants.FILTER_TYPE.SESSION, Constants.FILTER_TYPE.ORGANISATION_PROPERTY, Constants.FILTER_TYPE.SYSTEM_PROPERTY ],
    updatePending: false
  }
  deletePending = false
  deleteSessionsPending = false

  domainLoader?: () => Observable<Domain[]>
  specificationLoader?: () => Observable<Specification[]>
  actorLoader?: () => Observable<Actor[]>
  testSuiteLoader?: () => Observable<TestSuiteWithTestCases[]>
  testCaseLoader?: () => Observable<TestCase[]>
  communityLoader?: () => Observable<Community[]>
  organisationLoader?: () => Observable<Organisation[]>
  systemLoader?: () => Observable<System[]>
  organisationPropertyLoader?: (_:number) => Observable<OrganisationParameter[]>
  systemPropertyLoader?: (_:number) => Observable<SystemParameter[]>
  
  constructor(
    public dataService: DataService,
    private systemConfigurationService: SystemConfigurationService,
    private communityService: CommunityService,
    private conformanceService: ConformanceService,
    private reportService: ReportService,
    private testSuiteService: TestSuiteService,
    private organisationService: OrganisationService,
    private systemService: SystemService,
    private confirmationDialogService: ConfirmationDialogService,
    private testService: TestService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
    this.initFilterDataLoaders()
    if (this.dataService.isCommunityAdmin) {
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
      { field: 'result', title: 'Result', sortable: true, iconFn: this.dataService.iconForTestResult }
    ]
    if (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.community!.domain == undefined)) {
      this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
    }
    if (this.dataService.isSystemAdmin) {
      this.filterState.filters.push(Constants.FILTER_TYPE.COMMUNITY)
    }
    if (this.dataService.isSystemAdmin) {
      this.systemConfigurationService.getSessionAliveTime().subscribe((data) => {
        this.ttlEnabled = data.parameter != undefined
        if (this.ttlEnabled) {
          this.prevParameter = parseInt(data.parameter!)
        }
      })
    }
    this.goFirstPage()
  }

  private initFilterDataLoaders() {
    // Domains
    this.domainLoader = (() => {
      return this.conformanceService.getDomains()
    }).bind(this)
    // Specifications
    this.specificationLoader = (() => {
      if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId !== undefined) {
        return this.conformanceService.getSpecifications(this.dataService.community!.domainId)
      } else {
        return this.conformanceService.getSpecificationsWithIds()
      }
    }).bind(this)
    // Actors
    this.actorLoader = (() => {
        if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId !== undefined) {
          return this.conformanceService.getActorsForDomain(this.dataService.community!.domainId)
        } else {
          return this.conformanceService.getActorsWithIds()
        }
      }).bind(this)
    // Communities
    this.communityLoader = (() => {
      return this.communityService.getCommunities()
    }).bind(this)
    // Test cases
    this.testCaseLoader = (() => {
      if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId !== undefined) {
        return this.reportService.getTestCasesForCommunity()
      } else {
        return this.reportService.getAllTestCases()
      }
    }).bind(this)
    // Test suites
    this.testSuiteLoader = (() => {
      if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId !== undefined) {
        return this.testSuiteService.getTestSuitesWithTestCasesForCommunity()
      } else {
        return this.testSuiteService.getAllTestSuitesWithTestCases()
      }
    }).bind(this)
    // Organisations
    this.organisationLoader = (() => {
      if (this.dataService.isCommunityAdmin) {
        return this.organisationService.getOrganisationsByCommunity(this.dataService.community!.id)
      } else {
        return this.organisationService.getOrganisations()
      }
    }).bind(this)
    // Systems
    this.systemLoader = (() => {
      if (this.dataService.isSystemAdmin) {
        return this.systemService.getSystems()
      } else {
        return this.systemService.getSystemsByCommunity()
      }
    }).bind(this)
    // Organisation properties
    this.organisationPropertyLoader = ((communityId: number) => {
      return this.communityService.getOrganisationParameters(communityId, true)
    }).bind(this)
    // System properties
    this.systemPropertyLoader = ((communityId: number) => {
      return this.communityService.getSystemParameters(communityId, true)
    }).bind(this)
  }

  ttlToggled() {
    if (this.ttlEnabled) {
      this.dataService.focus('parameter')
    } else {
      this.turnOff()    
    }
  }

  setFilterRefreshState() {
    this.filterState.updatePending = this.refreshActivePending || this.refreshCompletedPending
  }

  getCurrentSearchCriteria() {
    let searchCriteria: TestResultSearchCriteria = {}
    if (this.dataService.isCommunityAdmin) {
      searchCriteria.communityIds = [this.dataService.community!.id]
      if (this.dataService.community?.domain !== undefined) {
        searchCriteria.domainIds = [this.dataService.community.domain.id]
      }
    }
    let filterData:{[key: string]: any}|undefined = undefined
    if (this.filterState?.filterData) {
      filterData = this.filterState.filterData()
    }
    if (filterData) {
      if (this.dataService.isCommunityAdmin) {
        if (this.dataService.community!.domain === undefined) {
          searchCriteria.domainIds = filterData[Constants.FILTER_TYPE.DOMAIN]
        }
      } else {
        searchCriteria.communityIds = filterData[Constants.FILTER_TYPE.COMMUNITY]
        searchCriteria.domainIds = filterData[Constants.FILTER_TYPE.DOMAIN]
      }
      searchCriteria.specIds = filterData[Constants.FILTER_TYPE.SPECIFICATION]
      searchCriteria.actorIds = filterData[Constants.FILTER_TYPE.ACTOR]
      searchCriteria.testSuiteIds = filterData[Constants.FILTER_TYPE.TEST_SUITE]
      searchCriteria.testCaseIds = filterData[Constants.FILTER_TYPE.TEST_CASE]
      searchCriteria.organisationIds = filterData[Constants.FILTER_TYPE.ORGANISATION]
      searchCriteria.systemIds = filterData[Constants.FILTER_TYPE.SYSTEM]
      searchCriteria.results = filterData[Constants.FILTER_TYPE.RESULT]
      searchCriteria.startTimeBeginStr = filterData.startTimeBeginStr
      searchCriteria.startTimeEndStr = filterData.startTimeEndStr
      searchCriteria.endTimeBeginStr = filterData.endTimeBeginStr
      searchCriteria.endTimeEndStr = filterData.endTimeEndStr
      searchCriteria.sessionId = filterData.sessionId
      searchCriteria.organisationProperties = filterData.organisationProperties
      searchCriteria.systemProperties = filterData.systemProperties
    }
    searchCriteria.activeSortColumn = this.activeSortColumn
    searchCriteria.activeSortOrder = this.activeSortOrder
    searchCriteria.completedSortColumn = this.completedSortColumn
    searchCriteria.completedSortOrder = this.completedSortOrder
    searchCriteria.currentPage = this.currentPage
    return searchCriteria
  }

  getActiveTests() {
    const params = this.getCurrentSearchCriteria()
    this.refreshActivePending = true
    this.activeExpandedCounter.count = 0
    this.setFilterRefreshState()
    this.reportService.getActiveTestResults(params).subscribe((data) => {
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
    this.viewCheckbox = false
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
      specification: testResult.specification?.sname,
      actor: testResult.actor?.name,
      testSuite: testResult.testSuite?.sname,
      testCase: testResult.test?.sname,
      organization: testResult.organization?.sname,
      system: testResult.system?.sname,
      startTime: testResult.result.startTime
    }
    if (completed) {
      result.endTime = testResult.result.endTime
      result.result = testResult.result.result
      result.obsolete = testResult.result.obsolete
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
    result.testCaseId = testResult.test?.id
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

  turnOff() {
    if (this.prevParameter !== undefined) {
      this.systemConfigurationService.updateSessionAliveTime()
      .subscribe(() => {
        this.prevParameter = undefined
        this.popupService.success('Automatic session termination disabled.')
      })
    }
  }

  apply() {
    if (this.prevParameter !== undefined) {
      this.systemConfigurationService.updateSessionAliveTime(this.prevParameter)
      .subscribe(() => {
        this.popupService.success('Maximum session time set to '+this.prevParameter+' seconds.')
      })
    } else {
      this.turnOff()
      this.ttlEnabled = false
    }
  }

  exportVisible(session: TestResultForDisplay) {
    return session.obsolete === undefined || !session.obsolete
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

  exportCompletedSessionsToCsv() {
    this.exportCompletedPending = true
    const params = this.getCurrentSearchCriteria()
    this.reportService.getCompletedTestResults(1, 1000000, params, true)
    .subscribe((data) => {
      const headers = ['Session', this.dataService.labelDomain(), this.dataService.labelSpecification(), this.dataService.labelActor(), 'Test suite', 'Test case', this.dataService.labelOrganisation(), this.dataService.labelSystem(), 'Start time', 'End time', 'Result', 'Obsolete']
      if (data.orgParameters !== undefined) {
        for (let param of data.orgParameters) {
          headers.push(this.dataService.labelOrganisation() + ' ('+param+')')
        }
      }
      if (data.sysParameters !== undefined) {
        for (let param of data.sysParameters) {
          headers.push(this.dataService.labelSystem() + ' ('+param+')')
        }
      }
      const tests = map(data.data, (testResult) => {
        return this.newTestResultForExport(testResult, true, data.orgParameters, data.sysParameters)
      })
      this.dataService.exportAllAsCsv(headers, tests)
    }).add(() => {
      this.exportCompletedPending = false
    })
  }

  exportActiveSessionsToCsv() {
    this.exportActivePending = true
    const params = this.getCurrentSearchCriteria()
    this.reportService.getActiveTestResults(params, true)
    .subscribe((data) => {
      const headers = ['Session', this.dataService.labelDomain(), this.dataService.labelSpecification(), this.dataService.labelActor(), 'Test suite', 'Test case', this.dataService.labelOrganisation(), this.dataService.labelSystem(), 'Start time', 'End time', 'Result', 'Obsolete']
      if (data.orgParameters !== undefined) {
        for (let param of data.orgParameters) {
          headers.push(this.dataService.labelOrganisation() + ' ('+param+')')
        }
      }
      if (data.sysParameters !== undefined) {
        for (let param of data.sysParameters) {
          headers.push(this.dataService.labelSystem() + ' ('+param+')')
        }
      }
      const tests = map(data.data, (testResult) => {
        return this.newTestResultForExport(testResult, false, data.orgParameters, data.sysParameters)
      })
      this.dataService.exportAllAsCsv(headers, tests)
    }).add(() => {
      this.exportActivePending = false
    })
  }

  rowStyle(row: TestResultForDisplay) {
    if (row.obsolete) {
      return 'test-result-obsolete'
    } else {
      return ''
    }
  }

  deleteObsolete() {
    this.confirmationDialogService.confirmed('Confirm delete', 'Are you sure you want to delete all obsolete test results?', 'Yes', 'No')
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
    this.viewCheckbox = true
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
    const dialog = this.confirmationDialogService.confirmed('Confirm delete', msg, 'Yes', 'No')
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
    this.viewCheckbox = false
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

}
