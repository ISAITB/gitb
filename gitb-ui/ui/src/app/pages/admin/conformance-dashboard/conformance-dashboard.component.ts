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

import {Component, NgZone, OnInit, ViewChild} from '@angular/core';
import {BsModalService} from 'ngx-bootstrap/modal';
import {Observable, of} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {RoutingService} from 'src/app/services/routing.service';
import {ConformanceCertificateSettings} from 'src/app/types/conformance-certificate-settings';
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
import {MultiSelectConfig} from 'src/app/components/multi-select-filter/multi-select-config';
import {FilterUpdate} from 'src/app/components/test-filter/filter-update';
import {
  BaseConformanceItemDisplayComponent
} from 'src/app/components/base-conformance-item-display/base-conformance-item-display.component';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {CheckboxOptionState} from '../../../components/checkbox-option-panel/checkbox-option-state';
import {ConformanceResultFullWithTestSuites} from '../../../types/conformance-result-full-with-test-suites';
import {ActivatedRoute} from '@angular/router';
import {MultiSelectFilterComponentApi} from '../../../components/multi-select-filter/multi-select-filter-component-api';

@Component({
    selector: 'app-conformance-dashboard',
    templateUrl: './conformance-dashboard.component.html',
    styleUrls: ['./conformance-dashboard.component.less'],
    standalone: false
})
export class ConformanceDashboardComponent extends BaseConformanceItemDisplayComponent implements OnInit {

  @ViewChild("communityDropdown") communityDropdown?: MultiSelectFilterComponentApi<Community>
  @ViewChild("organisationDropdown") organisationDropdown?: MultiSelectFilterComponentApi<Organisation>
  @ViewChild("systemDropdown") systemDropdown?: MultiSelectFilterComponentApi<System>

  selectedCommunityId?: number
  selectedOrganisationId?: number
  selectedSystemId?: number
  settings?: Partial<ConformanceCertificateSettings>
  availableCommunities?: Community[]
  availableOrganisations?: Organisation[]
  availableSystems?: System[]
  exportOverviewPending = false
  communitySelectConfig?: MultiSelectConfig<Community>
  organisationSelectConfig?: MultiSelectConfig<Organisation>
  systemSelectConfig?: MultiSelectConfig<System>
  initialCommunityId?: number
  initialisingSnapshots = false

  constructor(
    dataService: DataService,
    zone: NgZone,
    conformanceService: ConformanceService,
    private readonly modalService: BsModalService,
    private readonly routingService: RoutingService,
    private readonly communityService: CommunityService,
    private readonly organisationService: OrganisationService,
    private readonly systemService: SystemService,
    private readonly reportSupportService: ReportSupportService,
    private readonly route: ActivatedRoute
  ) { super(dataService, zone, conformanceService) }

  ngOnInit(): void {
    this.latestSnapshotButtonLabel = 'Latest conformance status'
    this.snapshotButtonLabel = this.latestSnapshotButtonLabel
		if (this.dataService.isCommunityAdmin) {
			this.communityId = this.dataService.community!.id
      this.selectedCommunityId = this.communityId
      this.initialCommunityId = this.selectedCommunityId
    } else {
      if (this.route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.COMMUNITY_ID)) {
        this.initialCommunityId = Number(this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.COMMUNITY_ID))
      }
    }
    if (this.route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.ORGANISATION_ID)) {
      this.selectedOrganisationId = Number(this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.ORGANISATION_ID))
    }
    if (this.route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID)) {
      this.selectedSystemId = Number(this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID))
    }
    // Tree view selection configs - start
    if (this.dataService.isSystemAdmin) {
      this.communitySelectConfig = {
        name: 'availableCommunities',
        textField: 'fname',
        filterLabel: 'Select community',
        filterLabelIcon: Constants.BUTTON_ICON.COMMUNITY,
        singleSelection: true,
        singleSelectionPersistent: true,
      }
    }
    this.organisationSelectConfig = {
      name: 'availableOrganisations',
      textField: 'fname',
      filterLabel: `Select ${this.dataService.labelOrganisationLower()}`,
      filterLabelIcon: Constants.BUTTON_ICON.ORGANISATION,
      singleSelection: true,
      singleSelectionPersistent: true,
    }
    this.systemSelectConfig = {
      name: 'availableSystems',
      textField: 'fname',
      filterLabel: `Select ${this.dataService.labelSystemLower()}`,
      filterLabelIcon: Constants.BUTTON_ICON.SYSTEM,
      singleSelection: true,
      singleSelectionPersistent: true,
    }
    this.createStatusOptions()
    // Tree view select configs - end
    this.routingService.conformanceDashboardBreadcrumbs()
    if (this.route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID)) {
      const initialSnapshotId = Number(this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID))
      this.activeConformanceSnapshot = {
        id: initialSnapshotId
      }
      // Initialise snapshots if needed.
      this.initialisingSnapshots = true
      this.conformanceService.getConformanceSnapshot(initialSnapshotId).subscribe((selectedSnapshot) => {
        this.snapshotButtonLabel = selectedSnapshot.label
        this.activeConformanceSnapshot = selectedSnapshot
      }).add(() => {
        this.initialisingSnapshots = false
      })
    }
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
    this.viewTypeToggled(true)
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
      reportObservable = this.reportSupportService.handleConformanceStatementReport(this.selectedCommunityId!, event.actorId!, this.selectedSystemId!, this.snapshotIdToUse(), event.format, true, testCaseCount)
    } else {
      const reportLevel = this.determineReportLevel(event)
      reportObservable = this.reportSupportService.handleConformanceOverviewReport(this.selectedCommunityId!, this.selectedSystemId!, event.item.id, reportLevel, this.snapshotIdToUse(), event.format, this.dataService.conformanceStatusForConformanceItem(event.item))
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

  manageConformanceSnapshots() {
    if (this.selectedCommunityId != undefined) {
      const modalRef = this.modalService.show(ConformanceSnapshotsModalComponent, {
        class: 'modal-lg',
        initialState: {
          communityId: this.selectedCommunityId!,
          currentlySelectedSnapshot: this.snapshotIdToUse()
        }
      })
      modalRef.content!.select.subscribe((selectedSnapshot) => {
        if (selectedSnapshot) {
          this.snapshotButtonLabel = selectedSnapshot.label
          if (this.activeConformanceSnapshot == undefined || this.activeConformanceSnapshot.id != selectedSnapshot.id) {
            this.activeConformanceSnapshot = selectedSnapshot
            this.updateRouting()
            if (this.listView) {
              this.listViewTable?.reloadData()
            } else {
              this.updateTreeViewForSnapshotChange()
            }
          }
          this.activeConformanceSnapshot = selectedSnapshot
        } else {
          this.updateRouting()
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
        this.listViewTable?.reloadData()
      } else {
        this.updateTreeViewForSnapshotChange()
      }
    }
    this.snapshotButtonLabel = this.latestSnapshotButtonLabel
  }

  private updateTreeViewForSnapshotChange() {
    this.communityChanged(true)
  }

  viewTypeToggled(fromPageInit?: boolean) {
    if (this.listView) {
      this.selectedCommunityId = this.communityId
      this.selectedOrganisationId = undefined
      this.selectedSystemId = undefined
      this.availableOrganisations = undefined
      this.availableSystems = undefined
      this.statements = []
      if (this.dataService.isSystemAdmin) {
        // Force a switch back to the latest snapshot and refresh
        this.viewLatestConformanceSnapshot(true)
      }
    } else {
      if (!fromPageInit) {
        this.resetStatementFilters()
      }
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
          } else if (this.initialCommunityId != undefined) {
            communityToApply = data.find((c) => c.id == this.initialCommunityId)
            this.initialCommunityId = undefined
          }
          this.selectedCommunityId = communityToApply?.id
          if (this.communitySelectConfig) {
            setTimeout(() => {
              this.communityDropdown?.replaceItems(this.availableCommunities!)
              this.communityDropdown?.replaceSelectedItems((communityToApply == undefined)?[]:[communityToApply], true)
            }, 1)
          }
          if (this.dataService.isSystemAdmin) {
            if (fromPageInit === true) {
              this.communityChanged(true)
            } else {
              // Force a switch back to the latest snapshot and refresh
              this.viewLatestConformanceSnapshot(true)
            }
          } else {
            this.communityChanged(true)
          }
        })
      })
    }
  }

  private updateRouting() {
    let communityId: number|undefined
    if (this.dataService.isSystemAdmin) {
      communityId = this.selectedCommunityId
    }
    this.routingService.toConformanceDashboard(communityId, this.selectedOrganisationId, this.selectedSystemId, this.snapshotIdToUse(), true)
  }

  private snapshotIdToUse() {
    return this.activeConformanceSnapshot?.id
  }

  communityChanged(fromSnapshotChange: boolean, community?: FilterUpdate<Community>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    this.pagingControls?.hide()
    if (community && community.values.active.length > 0) {
      this.selectedCommunityId = community.values.active[0].id
      this.updateRouting()
    }
    if (!fromSnapshotChange) {
      this.activeConformanceSnapshot = undefined
    }
    let loadObservable: Observable<Organisation[]>
    if (this.selectedCommunityId == undefined) {
      loadObservable = of([])
    } else {
      loadObservable = this.organisationService.getOrganisationsByCommunity(this.selectedCommunityId, false, this.snapshotIdToUse())
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
          organisationToApply = data.find((org) => org.id == this.selectedOrganisationId)
          if (organisationToApply == undefined) {
            this.selectedOrganisationId = undefined
          }
        } else {
          this.selectedOrganisationId = undefined
        }
        setTimeout(() => {
          this.organisationDropdown?.replaceItems(this.availableOrganisations!)
          this.organisationDropdown?.replaceSelectedItems((organisationToApply == undefined)?[]:[organisationToApply], true)
          this.organisationChanged()
        })
      })
    })
  }

  organisationChanged(organisation?: FilterUpdate<Organisation>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    this.pagingControls?.hide()
    if (organisation && organisation.values.active.length > 0) {
      this.selectedOrganisationId = organisation.values.active[0].id
      this.updateRouting()
    }
    let loadObservable: Observable<System[]>
    if (this.selectedOrganisationId == undefined) {
      loadObservable = of([])
    } else {
      loadObservable = this.systemService.getSystemsByOrganisation(this.selectedOrganisationId, this.snapshotIdToUse())
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
          systemToApply = data.find((sys) => sys.id == this.selectedSystemId)
          if (systemToApply == undefined) {
            this.selectedSystemId = undefined
          }
        } else {
          this.selectedSystemId = undefined
        }
        setTimeout(() => {
          this.systemDropdown?.replaceItems(this.availableSystems!)
          this.systemDropdown?.replaceSelectedItems((systemToApply == undefined)?[]:[systemToApply], true)
          this.systemChanged()
        }, 1)
      })
    })
  }

  systemChanged(system?: FilterUpdate<System>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    this.pagingControls?.hide()
    if (system && system.values.active.length > 0) {
      this.selectedSystemId = system.values.active[0].id
      this.updateRouting()
    }
    setTimeout(() => {
      if (this.selectedSystemId == undefined) {
        this.dataStatus.status = Constants.STATUS.FINISHED
        this.statements = []
      } else {
        this.getConformanceStatementsForTreeView()
      }
    })
  }

  filterByStatus(choices: CheckboxOptionState) {
    super.filterByStatus(choices);
    this.getConformanceStatementsForTreeView()
  }

  doTreeViewPaging(event: PagingEvent) {
    this.getConformanceStatementsForTreeViewInternal(event)
  }

  getConformanceStatementsForTreeView() {
    this.restoreState()
    let pagingEvent: PagingEvent = { targetPage: 1, targetPageSize: Constants.TABLE_PAGE_SIZE }
    if (this.initialPagingStatus != undefined) {
      pagingEvent = { targetPage: this.initialPagingStatus.currentPage, targetPageSize: this.initialPagingStatus.pageSize }
      this.initialPagingStatus = undefined
    }
    this.getConformanceStatementsForTreeViewInternal(pagingEvent)
  }

  getConformanceStatementsForTreeViewInternal(pagingInfo: PagingEvent) {
    if (this.selectedSystemId != undefined) {
      if (this.dataStatus.status == Constants.STATUS.FINISHED) {
        this.updatePending = true
      } else {
        this.dataStatus.status = Constants.STATUS.PENDING
        this.pagingControls?.hide()
      }
      this.conformanceService.getConformanceStatementsForSystem(this.selectedSystemId, this.snapshotIdToUse(), pagingInfo.targetPage, pagingInfo.targetPageSize, this.searchCriteria)
      .subscribe((data) => {
        this.applyTreeViewSearchResult(data, pagingInfo)
      }).add(() => {
        this.updatePending = false
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  exportOverview(format: 'xml'|'pdf') {
    this.exportOverviewPending = true
    const overallStatus = this.dataService.conformanceStatusForConformanceItems(this.statements)
    this.reportSupportService.handleConformanceOverviewReport(this.selectedCommunityId!, this.selectedSystemId!, undefined, "all", this.snapshotIdToUse(), format, overallStatus)
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

  listViewCommunityChange(newCommunityId: number|undefined) {
    if (this.communityId == undefined) {
      if (newCommunityId != undefined) {
        if (newCommunityId != this.selectedCommunityId) {
          this.snapshotButtonLabel = this.latestSnapshotButtonLabel
          this.activeConformanceSnapshot = undefined
          this.selectedCommunityId = newCommunityId
        }
      } else {
        this.snapshotButtonLabel = this.latestSnapshotButtonLabel
        this.activeConformanceSnapshot = undefined
        this.selectedCommunityId = undefined
      }
    }
  }

  onStatementSelect(statement: ConformanceStatementItem) {
    if (sessionStorage) sessionStorage.setItem(Constants.SESSION_DATA.FROM_DASHBOARD, "true")
    this.routingService.toConformanceStatement(this.selectedOrganisationId!, this.selectedSystemId!, statement.id, this.selectedCommunityId!, this.snapshotIdToUse(), this.activeConformanceSnapshot?.label)
  }

  onStatementSelectFromListView(statement: ConformanceResultFullWithTestSuites) {
    if (sessionStorage) sessionStorage.setItem(Constants.SESSION_DATA.FROM_DASHBOARD, "true")
    this.routingService.toConformanceStatement(statement.organizationId, statement.systemId, statement.actorId, statement.communityId, this.snapshotIdToUse(), this.activeConformanceSnapshot?.label)
  }

  protected displayStateKey(): string {
    return Constants.DISPLAY_STATE_KEY.CONFORMANCE_DASHBOARD
  }

  protected displayStateDataKey(): string {
    return `${this.selectedSystemId}|${this.snapshotIdToUse()}`
  }

}
