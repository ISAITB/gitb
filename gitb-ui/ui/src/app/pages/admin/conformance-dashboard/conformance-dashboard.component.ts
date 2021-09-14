import { Component, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Observable } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ConformanceCertificateModalComponent } from 'src/app/modals/conformance-certificate-modal/conformance-certificate-modal.component';
import { CommunityService } from 'src/app/services/community.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { ReportService } from 'src/app/services/report.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemService } from 'src/app/services/system.service';
import { Actor } from 'src/app/types/actor';
import { Community } from 'src/app/types/community';
import { ConformanceCertificateSettings } from 'src/app/types/conformance-certificate-settings';
import { ConformanceResultFull } from 'src/app/types/conformance-result-full';
import { ConformanceResultFullList } from 'src/app/types/conformance-result-full-list';
import { ConformanceResultFullWithTestSuites } from 'src/app/types/conformance-result-full-with-test-suites';
import { ConformanceResultTestSuite } from 'src/app/types/conformance-result-test-suite';
import { ConformanceStatusItem } from 'src/app/types/conformance-status-item';
import { Domain } from 'src/app/types/domain';
import { FilterState } from 'src/app/types/filter-state';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { Organisation } from 'src/app/types/organisation.type';
import { Specification } from 'src/app/types/specification';
import { System } from 'src/app/types/system';
import { SystemParameter } from 'src/app/types/system-parameter';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { TestResultSearchCriteria } from 'src/app/types/test-result-search-criteria';

@Component({
  selector: 'app-conformance-dashboard',
  templateUrl: './conformance-dashboard.component.html',
  styleUrls: [ './conformance-dashboard.component.less' ]
})
export class ConformanceDashboardComponent implements OnInit {

  exportPending = false
  dataStatus = {status: Constants.STATUS.PENDING}
  filterState: FilterState = {
    filters: [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.ORGANISATION, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.ORGANISATION_PROPERTY, Constants.FILTER_TYPE.SYSTEM_PROPERTY ],
    updatePending: false
  }
  communityId?: number
  columnCount!: number
  tableColumns: TableColumnDefinition[] = []
  expandedStatements: { [key: string]: any, count: number } = {
    count: 0
  }
  conformanceStatements: ConformanceResultFullWithTestSuites[] = []
  conformanceStatementsPage: ConformanceResultFullWithTestSuites[] = []
  settings?: Partial<ConformanceCertificateSettings>
  domainLoader?: () => Observable<Domain[]>
  specificationLoader?: () => Observable<Specification[]>
  actorLoader?: () => Observable<Actor[]>
  communityLoader?: () => Observable<Community[]>
  organisationLoader?: () => Observable<Organisation[]>
  systemLoader?: () => Observable<System[]>
  organisationPropertyLoader?: (_:number) => Observable<OrganisationParameter[]>
  systemPropertyLoader?: (_:number) => Observable<SystemParameter[]>
  Constants = Constants

  conformanceStatementsTotalCount = 0
  currentPage = 1
  prevDisabled = false
  nextDisabled = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private communityService: CommunityService,
    private organisationService: OrganisationService,
    private systemService: SystemService,
    private reportService: ReportService,
    private modalService: BsModalService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
		if (this.dataService.isCommunityAdmin)
			this.communityId = this.dataService.community!.id
		if (this.dataService.isSystemAdmin) {
			this.columnCount = 9
			this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
			this.filterState.filters.push(Constants.FILTER_TYPE.COMMUNITY)
    } else if (this.dataService.isCommunityAdmin) {
			if (this.dataService.community!.domain == undefined) {
				this.columnCount = 8
				this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
      } else {
				this.columnCount = 7
      }
    }
		if (this.dataService.isCommunityAdmin) {
			this.tableColumns.push({
				field: 'communityName',
				title: 'Community'
			})
    }
		this.tableColumns.push({
			field: 'organizationName',
			title: this.dataService.labelOrganisation()
		})
		this.tableColumns.push({
			field: 'systemName',
			title: this.dataService.labelSystem()
		})
		if (this.dataService.community?.domainId == undefined) {
			this.tableColumns.push({
				field: 'domainName',
				title: this.dataService.labelDomain()
			})
    }
		this.tableColumns.push({
			field: 'specName',
			title: this.dataService.labelSpecification()
		})
		this.tableColumns.push({
			field: 'actorName',
			title: this.dataService.labelActor()
		})
		this.tableColumns.push({
			field: 'status',
			title: 'Status'
		})
    this.initFilterDataLoaders()
    this.getConformanceStatements()
  }

  private initFilterDataLoaders() {
    // Domains
    this.domainLoader = (() => {
      return this.conformanceService.getDomains()
    }).bind(this)
    // Specifications
    this.specificationLoader = (() => {
      if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId != undefined) {
        return this.conformanceService.getSpecifications(this.dataService.community!.domainId)
      } else {
        return this.conformanceService.getSpecificationsWithIds()
      }
    }).bind(this)
    // Actors
    this.actorLoader = (() => {
        if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId != undefined) {
          return this.conformanceService.getActorsForDomain(this.dataService.community!.domainId)
        } else {
          return this.conformanceService.getActorsWithIds()
        }
      }).bind(this)
    // Communities
    this.communityLoader = (() => {
      return this.communityService.getCommunities()
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

	getCurrentSearchCriteria() {
    let searchCriteria: TestResultSearchCriteria = {}
    if (this.dataService.isCommunityAdmin) {
      searchCriteria.communityIds = [this.dataService.community!.id]
      if (this.dataService.community!.domain !== undefined) {
        searchCriteria.domainIds = [this.dataService.community!.domain.id]
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
      searchCriteria.organisationIds = filterData[Constants.FILTER_TYPE.ORGANISATION]
      searchCriteria.systemIds = filterData[Constants.FILTER_TYPE.SYSTEM]
      searchCriteria.organisationProperties = filterData.organisationProperties
      searchCriteria.systemProperties = filterData.systemProperties
    }
		return searchCriteria
  }

	getConformanceStatementsInternal(fullResults: boolean, forExport: boolean) {
    const result = new Observable<ConformanceResultFullList>((subscriber) => {
      let params = this.getCurrentSearchCriteria()
      this.conformanceService.getConformanceOverview(params, fullResults, forExport)
      .subscribe((data: ConformanceResultFullList) => {
        for (let conformanceStatement of data.data) {
          const completedCount = Number(conformanceStatement.completed)
          const failedCount = Number(conformanceStatement.failed)
          const undefinedCount = Number(conformanceStatement.undefined)
          conformanceStatement.status = this.dataService.testStatusText(completedCount, failedCount, undefinedCount)
          conformanceStatement.overallStatus = this.dataService.conformanceStatusForTests(completedCount, failedCount, undefinedCount)
        }
        subscriber.next(data)
      }).add(() => {
        this.dataStatus = {status: Constants.STATUS.FINISHED}
        subscriber.complete()
      })
    })
    return result
  }

	getConformanceStatements() {
    this.filterState.updatePending = true
    this.getConformanceStatementsInternal(false, false)
    .subscribe((data) => {
			this.conformanceStatements = data.data
      this.conformanceStatementsTotalCount = this.conformanceStatements.length
      this.currentPage = 1      
      this.selectPage()
			this.onCollapseAll()
    }).add(() => {
			this.filterState.updatePending = false
    })
  }

  organiseTestSuites(statement: ConformanceResultFullWithTestSuites) {
    if (statement.testCases != undefined) {
      const testSuites: ConformanceResultTestSuite[] = []
      let testSuite: ConformanceResultTestSuite|undefined
      for (let item of statement.testCases) {
        if (testSuite == undefined || testSuite.testSuiteId != item.testSuiteId) {
          if (testSuite != undefined) {
            testSuites.push(testSuite)
          }
          testSuite = {
            testSuiteId: item.testSuiteId!,
            testSuiteName: item.testSuiteName!,
            expanded: true,
            result: item.result!,
            testCases: []
          }
        }
        if (item.result != testSuite.result) {
          if (item.result == Constants.TEST_CASE_RESULT.FAILURE) {
            testSuite.result = Constants.TEST_CASE_RESULT.FAILURE
          } else if (item.result == Constants.TEST_CASE_RESULT.UNDEFINED && testSuite.result == Constants.TEST_CASE_RESULT.SUCCESS) {
            testSuite.result = Constants.TEST_CASE_RESULT.UNDEFINED
          }
        }
        testSuite.testCases.push(item as ConformanceStatusItem)
      }
      if (testSuite != undefined) {
        testSuites.push(testSuite)
      }
      statement.testSuites = testSuites
    }
  }

	onExpand(statement: ConformanceResultFull) {
		if (this.isExpanded(statement)) {
			this.collapse(statement)
    } else {
      this.expand(statement)
      if (statement.testCases == undefined) {
        statement.testCasesLoaded = false
        this.conformanceService.getConformanceStatus(statement.actorId, statement.systemId, true)
        .subscribe((data) => {
          const testCases: Partial<ConformanceStatusItem>[] = []
          for (let result of data) {
            testCases.push({
              id: result.testCaseId,
              sessionId: result.sessionId,
              testSuiteId: result.testSuiteId,
              testSuiteName: result.testSuiteName,
              testCaseId: result.testCaseId,
              testCaseName: result.testCaseName,
              result: result.result,
              outputMessage: result.outputMessage,
              sessionTime: result.sessionTime
            })
          }
          statement.testCases = testCases
          this.organiseTestSuites(statement)
        }).add(() => {
          statement.testCasesLoaded = true
        })
      }
    }
  }

	collapse(statement: ConformanceResultFull) {
		delete this.expandedStatements[statement.systemId+"_"+statement.actorId]
		this.expandedStatements.count -= 1
  }

	expand(statement: ConformanceResultFull) {
		this.expandedStatements[statement.systemId+"_"+statement.actorId] = true
		this.expandedStatements.count += 1
  }

	isExpanded(statement: ConformanceResultFull) {
		return this.expandedStatements[statement.systemId+"_"+statement.actorId] != undefined
  }

	showCollapseAll() {
		return this.expandedStatements.count > 0
  }

	onCollapseAll() {
		this.expandedStatements = { count: 0 }
  }

	showExportTestCase(testCase: Partial<ConformanceStatusItem>) {
		return testCase.sessionId != undefined && testCase.sessionId != ""
  }

	onExportConformanceStatementsAsCsv() {
		this.exportPending = true
		this.getConformanceStatementsInternal(true, true)
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
    }).add(() => {
      this.exportPending = false
    })
  }

	onExportTestCase(statement: ConformanceResultFull, testCase: Partial<ConformanceStatusItem>) {
		testCase.exportPending = true
		this.reportService.exportTestCaseReport(testCase.sessionId!, testCase.id!)
    .subscribe((stepResults) => {
			const blobData = new Blob([stepResults], {type: 'application/pdf'});
			saveAs(blobData, "test_case_report.pdf");
    }).add(() => {
      testCase.exportPending = false
    })
  }

	onExportConformanceStatement(statement?: ConformanceResultFull) {
		let statementToProcess: ConformanceResultFull
		if (statement == undefined) {
			statementToProcess = this.conformanceStatements[0] as ConformanceResultFull
    } else {
      statementToProcess = statement!
    }
		if (this.settings == undefined) {
      statementToProcess.exportPending = true
			this.conformanceService.getConformanceCertificateSettings(statementToProcess.communityId, false)
      .subscribe((settings) => {
				if (settings != undefined && settings.id != undefined) {
					this.settings = settings
        } else {
					this.settings = {}
        }
        this.showSettingsPopup(statementToProcess)
      }).add(() => {
				statementToProcess.exportPending = false
      })
    } else {
      this.showSettingsPopup(statementToProcess)
    }
  }

  private showSettingsPopup(statement: ConformanceResultFull) {
    const modalRef = this.modalService.show(ConformanceCertificateModalComponent, {
      class: 'modal-lg', 
      initialState: {
        settings: JSON.parse(JSON.stringify(this.settings)),
        conformanceStatement: statement
      }
    })
  }

  toOrganisation(statement: ConformanceResultFull) {
    if (statement.organizationId == this.dataService.vendor!.id) {
      // Own organisation
      this.routingService.toOwnOrganisationDetails()
    } else {
      this.routingService.toOrganisationDetails(statement.communityId, statement.organizationId)
    }
  }

  toSystem(statement: ConformanceResultFull) {
    this.routingService.toSystems(statement.organizationId, statement.systemId)
  }

  toStatement(statement: ConformanceResultFull) {
    this.routingService.toConformanceStatement(statement.organizationId, statement.systemId, statement.actorId, statement.specId)
  }

  toSpecification(statement: ConformanceResultFull) {
    this.routingService.toSpecification(statement.domainId, statement.specId)
  }

  toActor(statement: ConformanceResultFull) {
    this.routingService.toActor(statement.domainId, statement.specId, statement.actorId)
  }

  toTestSession(sessionId: string) {
    this.routingService.toSessionDashboard(sessionId)
  }

  doFirstPage() {
    if (!this.prevDisabled) {
      this.currentPage = 1
      this.selectPage()
    }
  }

  doPrevPage() {
    if (!this.prevDisabled) {
      this.currentPage -= 1
      this.selectPage()
    }
  }

  doNextPage() {
    if (!this.nextDisabled) {
      this.currentPage += 1
      this.selectPage()
    }
  }

  doLastPage() {
    if (!this.nextDisabled) {
      this.currentPage = Math.ceil(this.conformanceStatementsTotalCount / Constants.TABLE_PAGE_SIZE)
      this.selectPage()
    }
  }

  selectPage() {
    const startIndex = (this.currentPage - 1) * Constants.TABLE_PAGE_SIZE
    const endIndex = startIndex + Constants.TABLE_PAGE_SIZE
    this.conformanceStatementsPage = this.conformanceStatements.slice(startIndex, endIndex)
    this.updatePagination()
  }

  updatePagination() {
    if (this.currentPage == 1) {
      this.nextDisabled = this.conformanceStatementsTotalCount <= Constants.TABLE_PAGE_SIZE
      this.prevDisabled = true
    } else if (this.currentPage == Math.ceil(this.conformanceStatementsTotalCount / Constants.TABLE_PAGE_SIZE)) {
      this.nextDisabled = true
      this.prevDisabled = false
    } else {
      this.nextDisabled = false
      this.prevDisabled = false
    }
  }

}
