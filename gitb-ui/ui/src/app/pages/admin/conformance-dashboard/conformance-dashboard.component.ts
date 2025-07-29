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

import {Component, EventEmitter, NgZone, OnInit, ViewChild} from '@angular/core';
import {BsModalService} from 'ngx-bootstrap/modal';
import {Observable, of} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {RoutingService} from 'src/app/services/routing.service';
import {ConformanceCertificateSettings} from 'src/app/types/conformance-certificate-settings';
import {ConformanceResultFull} from 'src/app/types/conformance-result-full';
import {ConformanceResultFullList} from 'src/app/types/conformance-result-full-list';
import {ConformanceResultFullWithTestSuites} from 'src/app/types/conformance-result-full-with-test-suites';
import {FilterState} from 'src/app/types/filter-state';
import {TestResultSearchCriteria} from 'src/app/types/test-result-search-criteria';
import {ConformanceSnapshot} from 'src/app/types/conformance-snapshot';
import {ConformanceSnapshotsModalComponent} from 'src/app/modals/conformance-snapshots-modal/conformance-snapshots-modal.component';
import {Community} from 'src/app/types/community';
import {Organisation} from 'src/app/types/organisation.type';
import {System} from 'src/app/types/system';
import {CommunityService} from 'src/app/services/community.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {SystemService} from 'src/app/services/system.service';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {ExportReportEvent} from 'src/app/types/export-report-event';
import {ReportSupportService} from 'src/app/services/report-support.service';
import {find} from 'lodash';
import {MultiSelectConfig} from 'src/app/components/multi-select-filter/multi-select-config';
import {FilterUpdate} from 'src/app/components/test-filter/filter-update';
import {
  BaseConformanceItemDisplayComponent
} from 'src/app/components/base-conformance-item-display/base-conformance-item-display.component';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {PagingControlsApi} from '../../../components/paging-controls/paging-controls-api';
import {StatementFilterState} from '../../../components/statement-controls/statement-filter-state';
import {ConformanceTestSuite} from '../../organisation/conformance-statement/conformance-test-suite';

@Component({
    selector: 'app-conformance-dashboard',
    templateUrl: './conformance-dashboard.component.html',
    styleUrls: ['./conformance-dashboard.component.less'],
    standalone: false
})
export class ConformanceDashboardComponent extends BaseConformanceItemDisplayComponent implements OnInit {

  @ViewChild("pagingControls") pagingControls?: PagingControlsApi

  exportPending = false
  dataStatus = {status: Constants.STATUS.PENDING}
  filterState: FilterState = {
    filters: [ Constants.FILTER_TYPE.SPECIFICATION, Constants.FILTER_TYPE.SPECIFICATION_GROUP, Constants.FILTER_TYPE.ACTOR, Constants.FILTER_TYPE.ORGANISATION, Constants.FILTER_TYPE.SYSTEM, Constants.FILTER_TYPE.ORGANISATION_PROPERTY, Constants.FILTER_TYPE.SYSTEM_PROPERTY, Constants.FILTER_TYPE.RESULT, Constants.FILTER_TYPE.END_TIME ],
    updatePending: false,
    updateDisabled: false
  }
  communityId?: number
  selectedCommunityId?: number
  selectedOrganisationId?: number
  selectedSystemId?: number
  columnCount!: number
  expandedStatements: { [key: string]: any, count: number } = {
    count: 0
  }
  conformanceStatements: ConformanceResultFullWithTestSuites[] = []
  settings?: Partial<ConformanceCertificateSettings>
  Constants = Constants

  sortOrder = Constants.ORDER.ASC
  sortColumn = Constants.FILTER_TYPE.COMMUNITY

  latestSnapshotButtonLabel = 'Latest conformance status'
  snapshotButtonLabel = this.latestSnapshotButtonLabel
  activeConformanceSnapshot?: ConformanceSnapshot

  listView = false
  availableCommunities?: Community[]
  availableOrganisations?: Organisation[]
  availableSystems?: System[]
  filterCommands = new EventEmitter<number>()
  exportOverviewPending = false
  communitySelectConfig?: MultiSelectConfig<Community>
  organisationSelectConfig?: MultiSelectConfig<Organisation>
  systemSelectConfig?: MultiSelectConfig<System>

  constructor(
    dataService: DataService,
    zone: NgZone,
    private readonly conformanceService: ConformanceService,
    private readonly modalService: BsModalService,
    private readonly routingService: RoutingService,
    private readonly communityService: CommunityService,
    private readonly organisationService: OrganisationService,
    private readonly systemService: SystemService,
    private readonly reportSupportService: ReportSupportService,
  ) { super(dataService, zone) }

  ngOnInit(): void {
    this.filterState.names = {}
    this.filterState.names[Constants.FILTER_TYPE.RESULT] = 'Status'
    this.filterState.names[Constants.FILTER_TYPE.END_TIME] = 'Last update time'
		if (this.dataService.isSystemAdmin) {
			this.columnCount = 11
			this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
			this.filterState.filters.push(Constants.FILTER_TYPE.COMMUNITY)
    } else if (this.dataService.isCommunityAdmin) {
      this.sortColumn = Constants.FILTER_TYPE.ORGANISATION
			this.communityId = this.dataService.community!.id
      this.selectedCommunityId = this.communityId
			if (this.dataService.community!.domain == undefined) {
				this.columnCount = 10
				this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
      } else {
				this.columnCount = 9
      }
    }
    // Tree view select configs - start
    if (this.dataService.isSystemAdmin) {
      this.communitySelectConfig = {
        name: 'availableCommunities',
        textField: 'fname',
        filterLabel: 'Select community',
        singleSelection: true,
        singleSelectionPersistent: true,
        clearItems: new EventEmitter<void>(),
        replaceItems: new EventEmitter<Community[]>(),
        replaceSelectedItems: new EventEmitter<Community[]>()
      }
    }
    this.organisationSelectConfig = {
      name: 'availableOrganisations',
      textField: 'fname',
      filterLabel: `Select ${this.dataService.labelOrganisationLower()}`,
      singleSelection: true,
      singleSelectionPersistent: true,
      clearItems: new EventEmitter<void>(),
      replaceItems: new EventEmitter<Organisation[]>(),
      replaceSelectedItems: new EventEmitter<Organisation[]>()
    }
    this.systemSelectConfig = {
      name: 'availableSystems',
      textField: 'fname',
      filterLabel: `Select ${this.dataService.labelSystemLower()}`,
      singleSelection: true,
      singleSelectionPersistent: true,
      clearItems: new EventEmitter<void>(),
      replaceItems: new EventEmitter<System[]>(),
      replaceSelectedItems: new EventEmitter<System[]>()
    }
    // Tree view select configs - end
    this.routingService.conformanceDashboardBreadcrumbs()
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
    this.viewTypeToggled()
  }

  toggleFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.TOGGLE)
  }

  clearFilters() {
    console.log("CLEAR")
    this.filterCommands.emit(Constants.FILTER_COMMAND.CLEAR)
  }

  refreshFilters() {
    this.filterCommands.emit(Constants.FILTER_COMMAND.REFRESH)
  }

	getCurrentSearchCriteria() {
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
        const communityIdFromFilters = searchCriteria.communityIds[0]
        if (communityIdFromFilters != this.selectedCommunityId) {
          this.snapshotButtonLabel = this.latestSnapshotButtonLabel
          this.activeConformanceSnapshot = undefined
          this.selectedCommunityId = communityIdFromFilters
        }
      } else {
        this.snapshotButtonLabel = this.latestSnapshotButtonLabel
        this.activeConformanceSnapshot = undefined
        this.selectedCommunityId = undefined
      }
    }
		return searchCriteria
  }

	private getConformanceStatementsInternal(pagingInfo: PagingEvent, fullResults: boolean, forExport: boolean) {
    return new Observable<ConformanceResultFullList>((subscriber) => {
      let params = this.getCurrentSearchCriteria()
      this.conformanceService.getConformanceOverview(params, this.activeConformanceSnapshot?.id, fullResults, forExport, this.sortColumn, this.sortOrder, pagingInfo.targetPage, pagingInfo.targetPageSize)
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

	getConformanceStatements() {
    this.selectPage({ targetPage: 1, targetPageSize: this.pagingControls?.getCurrentStatus().pageSize! })
  }

  testSuiteLoader() {
    return ((item: ConformanceStatementItem) => {
      return this.conformanceService.getConformanceStatus(item.id, this.selectedSystemId!, this.activeConformanceSnapshot?.id)
    })
  }

	onExpand(statement: ConformanceResultFullWithTestSuites) {
		if (this.isExpanded(statement)) {
			this.collapse(statement)
    } else {
      this.expand(statement)
      if (statement.testSuites == undefined) {
        statement.testSuitesLoaded = false
        this.conformanceService.getConformanceStatus(statement.actorId, statement.systemId, this.activeConformanceSnapshot?.id)
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

	onExportConformanceStatementsAsCsv() {
		this.exportPending = true
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
    }).add(() => {
      this.exportPending = false
    })
  }

  private countTestCases(item: ConformanceStatementItem): number {
    if (item.results) {
      return item.results.completed + item.results.failed + item.results.undefined
    } else if (item.items) {
      let total = 0
      item.items.forEach(item => {
        total += this.countTestCases(item)
      })
      return total
    } else {
      return 0
    }
  }

  onExportConformanceItem(event: ExportReportEvent) {
    if (event.format == 'xml') {
      event.item.exportXmlPending = true
    } else {
      event.item.exportPdfPending = true
    }
    let reportObservable: Observable<any>
    if (event.statementReport) {
      const testCaseCount = this.countTestCases(event.item)
      reportObservable = this.reportSupportService.handleConformanceStatementReport(this.selectedCommunityId!, event.actorId!, this.selectedSystemId!, this.activeConformanceSnapshot?.id, event.format, true, testCaseCount)
    } else {
      const reportLevel = this.determineReportLevel(event)
      reportObservable = this.reportSupportService.handleConformanceOverviewReport(this.selectedCommunityId!, this.selectedSystemId!, event.item.id, reportLevel, this.activeConformanceSnapshot?.id, event.format, this.dataService.conformanceStatusForConformanceItem(event.item))
    }
    reportObservable.subscribe(() => {
      // Do nothing further
    }).add(() => {
      if (event.format == 'xml') {
        event.item.exportXmlPending = false
      } else {
        event.item.exportPdfPending = false
      }
    })
  }

	onExportConformanceStatement(statement: ConformanceResultFull, format: 'xml'|'pdf') {
    if (format == 'xml') {
      statement.exportXmlPending = true
    } else {
      statement.exportPdfPending = true
    }
    const testCaseCount = statement.completed + statement.failed + statement.undefined
    this.reportSupportService.handleConformanceStatementReport(statement.communityId, statement.actorId, statement.systemId, this.activeConformanceSnapshot?.id, format, true, testCaseCount)
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

  doPageNavigation(event: PagingEvent) {
    this.selectPage(event)
  }

  private selectPage(pagingInfo: PagingEvent) {
    this.filterState.updatePending = true
    this.getConformanceStatementsInternal(pagingInfo, false, false)
    .subscribe((data) => {
      this.conformanceStatements = data.data
      this.pagingControls?.updateStatus(pagingInfo.targetPage, data.count)
			this.onCollapseAll()
    }).add(() => {
			this.filterState.updatePending = false
    })
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

  manageConformanceSnapshots() {
    if (this.selectedCommunityId != undefined) {
      const modalRef = this.modalService.show(ConformanceSnapshotsModalComponent, {
        class: 'modal-lg',
        initialState: {
          communityId: this.selectedCommunityId!,
          currentlySelectedSnapshot: this.activeConformanceSnapshot?.id
        }
      })
      modalRef.content!.select.subscribe((selectedSnapshot) => {
        if (selectedSnapshot) {
          this.snapshotButtonLabel = selectedSnapshot.label
          if (this.activeConformanceSnapshot == undefined || this.activeConformanceSnapshot.id != selectedSnapshot.id) {
            this.activeConformanceSnapshot = selectedSnapshot
            if (this.listView) {
              this.getConformanceStatements()
            } else {
              this.updateTreeViewForSnapshotChange()
            }
          }
          this.activeConformanceSnapshot = selectedSnapshot
        } else {
          this.viewLatestConformanceSnapshot()
        }
      })
    }
  }

  viewLatestConformanceSnapshot(forceReload?: boolean) {
    let reload = forceReload == true
    if (this.activeConformanceSnapshot != undefined) {
      this.activeConformanceSnapshot = undefined
      reload = true
    }
    if (reload) {
      if (this.listView) {
        this.getConformanceStatements()
      } else {
        this.updateTreeViewForSnapshotChange()
      }
    }
    this.snapshotButtonLabel = this.latestSnapshotButtonLabel
  }

  private updateTreeViewForSnapshotChange() {
    this.communityChanged(true)
  }

  viewTypeToggled() {
    if (this.listView) {
      this.selectedCommunityId = this.communityId
      this.selectedOrganisationId = undefined
      this.selectedSystemId = undefined
      this.itemsByType = undefined
      this.availableOrganisations = undefined
      this.availableSystems = undefined
      this.statements = []
      if (this.dataService.isSystemAdmin) {
        // Force a switch back to the latest snapshot and refresh
        this.viewLatestConformanceSnapshot(true)
      } else {
        // Refresh search results
        this.getConformanceStatements()
      }
    } else {
      this.resetStatementFilters()
      let observable: Observable<Community[]>
      if (this.availableCommunities == undefined) {
        if (this.dataService.isSystemAdmin) {
          observable = this.communityService.getCommunities(undefined, true)
        } else if (this.dataService.community) {
          observable = of([this.dataService.community])
        } else {
          observable = of([])
        }
      } else {
        observable = of(this.availableCommunities)
      }
      observable.subscribe((data) => {
        setTimeout(() => {
          this.availableCommunities = data
          let communityToApply: Community|undefined
          if (data.length == 1) {
            communityToApply = data[0]
            this.selectedCommunityId = communityToApply.id
          } else {
            this.selectedCommunityId = undefined
          }
          if (this.communitySelectConfig) {
            setTimeout(() => {
              this.communitySelectConfig!.replaceItems!.emit(this.availableCommunities)
              this.communitySelectConfig!.replaceSelectedItems!.emit((communityToApply == undefined)?[]:[communityToApply])
            }, 1)
          }
          if (this.dataService.isSystemAdmin) {
            // Force a switch back to the latest snapshot and refresh
            this.viewLatestConformanceSnapshot(true)
          } else {
            this.communityChanged(true)
          }
        })
      })
    }
  }

  communityChanged(fromSnapshotChange: boolean, community?: FilterUpdate<Community>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    if (community && community.values.active.length > 0) {
      this.selectedCommunityId = community.values.active[0].id
    }
    if (!fromSnapshotChange) {
      this.activeConformanceSnapshot = undefined
    }
    let loadObservable: Observable<Organisation[]>
    if (this.selectedCommunityId == undefined) {
      loadObservable = of([])
    } else {
      loadObservable = this.organisationService.getOrganisationsByCommunity(this.selectedCommunityId, false, this.activeConformanceSnapshot?.id)
    }
    setTimeout(() => {
      this.availableOrganisations = undefined
      loadObservable.subscribe((data) => {
        this.availableOrganisations = data
        let organisationToApply: Organisation|undefined
        if (data.length == 1) {
          organisationToApply = data[0]
          this.selectedOrganisationId = organisationToApply.id
        } else if (data.length > 1 && this.selectedOrganisationId != undefined) {
          organisationToApply = find(data, (org) => org.id == this.selectedOrganisationId)
          if (organisationToApply == undefined) {
            this.selectedOrganisationId = undefined
          }
        } else {
          this.selectedOrganisationId = undefined
        }
        setTimeout(() => {
          this.organisationSelectConfig!.replaceItems!.emit(this.availableOrganisations)
          this.organisationSelectConfig!.replaceSelectedItems!.emit((organisationToApply == undefined)?[]:[organisationToApply])
          this.organisationChanged()
        })
      })
    })
  }

  organisationChanged(organisation?: FilterUpdate<Organisation>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    if (organisation && organisation.values.active.length > 0) {
      this.selectedOrganisationId = organisation.values.active[0].id
    }
    let loadObservable: Observable<System[]>
    if (this.selectedOrganisationId == undefined) {
      loadObservable = of([])
    } else {
      loadObservable = this.systemService.getSystemsByOrganisation(this.selectedOrganisationId, this.activeConformanceSnapshot?.id)
    }
    setTimeout(() => {
      this.availableSystems = undefined
      loadObservable.subscribe((data) => {
        this.availableSystems = data
        let systemToApply: System|undefined
        if (data.length == 1) {
          systemToApply = data[0]
          this.selectedSystemId = systemToApply.id
        } else if (data.length > 1 && this.selectedSystemId != undefined) {
          systemToApply = find(data, (sys) => sys.id == this.selectedSystemId)
          if (systemToApply == undefined) {
            this.selectedSystemId = undefined
          }
        } else {
          this.selectedSystemId = undefined
        }
        setTimeout(() => {
          this.systemSelectConfig!.replaceItems!.emit(this.availableSystems)
          this.systemSelectConfig!.replaceSelectedItems!.emit((systemToApply == undefined)?[]:[systemToApply])
          this.systemChanged()
        }, 1)
      })
    })
  }

  systemChanged(system?: FilterUpdate<System>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    if (system && system.values.active.length > 0) {
      this.selectedSystemId = system.values.active[0].id
    }
    setTimeout(() => {
      if (this.selectedSystemId == undefined) {
        this.dataStatus.status = Constants.STATUS.FINISHED
        this.itemsByType = undefined
        this.statements = []
      } else {
        this.getConformanceStatementsForTreeView()
      }
    })
  }

  getConformanceStatementsForTreeView() {
    if (this.selectedSystemId != undefined) {
      this.dataStatus.status = Constants.STATUS.PENDING
      this.conformanceService.getConformanceStatementsForSystem(this.selectedSystemId, this.activeConformanceSnapshot?.id)
      .subscribe((data) => {
        this.itemsByType = this.dataService.organiseConformanceItemsByType(data)
        this.statements = this.dataService.prepareConformanceStatementItemsForDisplay(data)
        this.filterStatements()
      }).add(() => {
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  exportOverview(format: 'xml'|'pdf') {
    this.exportOverviewPending = true
    const overallStatus = this.dataService.conformanceStatusForConformanceItems(this.statements)
    this.reportSupportService.handleConformanceOverviewReport(this.selectedCommunityId!, this.selectedSystemId!, undefined, "all", this.activeConformanceSnapshot?.id, format, overallStatus)
    .subscribe(() => {
      // Do nothing further
    }).add(() => {
      this.exportOverviewPending = false
    })
  }

  treeControlsExist() {
    return (this.communityId == undefined && (this.availableCommunities == undefined || this.availableCommunities.length > 0)) ||
      ((this.selectedCommunityId != undefined && this.availableCommunities) && (this.availableOrganisations == undefined || this.availableOrganisations.length > 0)) ||
      ((this.selectedOrganisationId != undefined && this.availableOrganisations) && (this.availableSystems == undefined || this.availableSystems.length > 0)) ||
      (this.statements.length > 0)
  }

  trackStatement(index: number, statement: ConformanceResultFullWithTestSuites): string {
    return `${statement.actorId}_${statement.systemId}`;
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

  collapseAllTestSuites(statement: ConformanceResultFullWithTestSuites) {
    if (statement.displayedTestSuites) {
      statement.displayedTestSuites.forEach((testSuite: ConformanceTestSuite) => {
        testSuite.expanded = false
      })
      this.setCollapseAllStatus(statement)
    }
  }

}
