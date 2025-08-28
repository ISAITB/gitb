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

import {Component, EventEmitter, NgZone, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {SystemService} from 'src/app/services/system.service';
import {find} from 'lodash';
import {RoutingService} from 'src/app/services/routing.service';
import {System} from 'src/app/types/system';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {ConformanceService} from 'src/app/services/conformance.service';
import {ConformanceSnapshot} from 'src/app/types/conformance-snapshot';
import {forkJoin, Observable, of} from 'rxjs';
import {ExportReportEvent} from 'src/app/types/export-report-event';
import {ReportSupportService} from 'src/app/services/report-support.service';
import {
  BaseConformanceItemDisplayComponent
} from 'src/app/components/base-conformance-item-display/base-conformance-item-display.component';
import {ConformanceSnapshotList} from 'src/app/types/conformance-snapshot-list';
import {MultiSelectConfig} from '../../../components/multi-select-filter/multi-select-config';
import {FilterUpdate} from '../../../components/test-filter/filter-update';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {CheckboxOptionState} from '../../../components/checkbox-option-panel/checkbox-option-state';

@Component({
    selector: 'app-conformance-statements',
    templateUrl: './conformance-statements.component.html',
    styleUrls: ['./conformance-statements.component.less'],
    standalone: false
})
export class ConformanceStatementsComponent extends BaseConformanceItemDisplayComponent implements OnInit {

  system?: System
  communityIdForSnapshots!: number
  systems!: System[]
  systemStatus = {status: Constants.STATUS.PENDING}
  showCreate = false
  showDomain = false
  showBack = false
  showCreateSystem = false
  conformanceSnapshots?: ConformanceSnapshot[]
  systemSelectionConfig!: MultiSelectConfig<System>

  constructor(
    dataService: DataService,
    zone: NgZone,
    private readonly systemService: SystemService,
    conformanceService: ConformanceService,
    private readonly route: ActivatedRoute,
    public readonly routingService: RoutingService,
    private readonly reportSupportService: ReportSupportService
  ) { super(dataService, zone, conformanceService) }

  ngOnInit(): void {
    this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    let isOwnConformanceStatements = true
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.communityIdForSnapshots = this.communityId
      this.showBack = this.organisationId >= 0
      isOwnConformanceStatements = false
    } else {
      this.communityIdForSnapshots = this.dataService.vendor!.community
    }
    let snapshotId: number|undefined
    if (this.route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID)) {
      snapshotId = Number(this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SNAPSHOT_ID))
    }
    this.showCreate = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowStatementManagement)
    this.showCreateSystem = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || this.dataService.isVendorAdmin
    this.showDomain = this.dataService.isSystemAdmin || this.dataService.community?.domainId == undefined
    this.systemSelectionConfig = {
      name: "system",
      textField: "fname",
      singleSelection: true,
      singleSelectionPersistent: true,
      filterLabel: 'Select ' + this.dataService.labelSystemLower()+ '...',
      noItemsMessage: 'No ' + this.dataService.labelSystemsLower() + ' available.',
      searchPlaceholder: 'Search ' + this.dataService.labelSystemsLower() + "...",
      replaceSelectedItems: new EventEmitter(),
      replaceItems: new EventEmitter()
    }
    const systemsLoaded = this.getSystems(snapshotId)
    let snapshotsLoaded: Observable<ConformanceSnapshotList>
    if (isOwnConformanceStatements && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)) {
      // Administrator organisations are not included in conformance snapshots
      snapshotsLoaded = of({snapshots: []})
    } else {
      snapshotsLoaded = this.conformanceService.getConformanceSnapshots(this.communityIdForSnapshots)
    }
    forkJoin([systemsLoaded, snapshotsLoaded]).subscribe((results) => {
      // Snapshots
      this.conformanceSnapshots = results[1].snapshots
      this.latestSnapshotButtonLabel = results[1].latest
      if (this.latestSnapshotButtonLabel == undefined || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        this.latestSnapshotButtonLabel = Constants.LATEST_CONFORMANCE_STATUS_LABEL
      }
      if (snapshotId != undefined) {
        const referencedSnapshot = find(this.conformanceSnapshots, (snapshot) => snapshot.id == snapshotId)
        if (referencedSnapshot) {
          this.activeConformanceSnapshot = referencedSnapshot
          this.snapshotButtonLabel = this.activeConformanceSnapshot.label
        } else {
          // The snapshot could have been deleted or rendered hidden.
          this.snapshotButtonLabel = this.latestSnapshotButtonLabel
        }
      } else {
        this.snapshotButtonLabel = this.latestSnapshotButtonLabel
      }
      // Systems - this will also load statements and update breadcrumbs.
      this.systemsLoaded(results[0])
    }).add(() => {
      this.systemStatus.status = Constants.STATUS.FINISHED
    })
  }

  private systemsLoaded(systems: System[]) {
    this.systems = systems
    setTimeout(() => {
      this.systemSelectionConfig.replaceItems!.emit(this.systems)
      let systemToSelect: System|undefined
      if (this.systems.length == 1) {
        systemToSelect = systems[0]
      } else if (this.route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID)) {
        const systemId = Number(this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID))
        systemToSelect = find(this.systems, (sys) => {
          return sys.id == systemId
        })
      }
      if (systemToSelect) {
        this.systemSelectionConfig.replaceSelectedItems!.emit([systemToSelect])
      } else {
        this.updateBreadcrumbs()
      }
    })
  }

  private updateBreadcrumbs() {
    if (this.communityId == undefined) {
      this.routingService.ownConformanceStatementsBreadcrumbs(this.organisationId!, this.system?.id, this.system?.sname, this.activeConformanceSnapshot?.id, this.activeConformanceSnapshot?.label)
    } else {
      this.routingService.conformanceStatementsBreadcrumbs(this.communityId, this.organisationId!, undefined, this.system?.id, this.system?.sname, this.activeConformanceSnapshot?.id, this.activeConformanceSnapshot?.label)
    }
  }

  systemSelectionChanged(event: FilterUpdate<System>): void {
    this.system = event.values.active[0]
    this.updateBreadcrumbs()
    this.getConformanceStatements()
    this.updateRouting()
  }

  getConformanceStatements() {
    this.getConformanceStatementsInternal({ targetPage: 1, targetPageSize: 10 })
  }

  filterByStatus(choices: CheckboxOptionState) {
    super.filterByStatus(choices);
    this.getConformanceStatements()
  }

  private getConformanceStatementsInternal(pagingInfo: PagingEvent) {
    if (this.dataStatus.status == Constants.STATUS.FINISHED) {
      this.updatePending = true
    } else {
      this.dataStatus.status = Constants.STATUS.PENDING
    }
    this.conformanceService.getConformanceStatementsForSystem(this.system!.id, this.activeConformanceSnapshot?.id, pagingInfo.targetPage, pagingInfo.targetPageSize, this.searchCriteria)
      .subscribe((data) => {
        this.applyTreeViewSearchResult(data, pagingInfo)
      }).add(() => {
      this.updatePending = false
      this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

  doTreeViewPaging(event: PagingEvent) {
    this.getConformanceStatementsInternal(event)
  }

  onStatementSelect(statement: ConformanceStatementItem) {
    if (this.communityId == undefined) {
      this.routingService.toOwnConformanceStatement(this.organisationId!, this.system!.id, statement.id, this.activeConformanceSnapshot?.id, this.activeConformanceSnapshot?.label)
    } else {
      this.routingService.toConformanceStatement(this.organisationId!, this.system!.id, statement.id, this.communityId, this.activeConformanceSnapshot?.id, this.activeConformanceSnapshot?.label)
    }
  }

  createStatement() {
    this.routingService.toCreateConformanceStatement(this.organisationId!, this.system!.id, this.communityId)
  }

  back() {
    this.routingService.toOrganisationDetails(this.communityId!, this.organisationId!)
  }

  toCreateSystem() {
    if (this.communityId != undefined) {
      this.routingService.toCreateSystem(this.communityId, this.organisationId!)
    } else {
      this.routingService.toCreateOwnSystem()
    }
  }

  private updateRouting() {
    if (this.communityId != undefined) {
      this.routingService.toConformanceStatements(this.communityId, this.organisationId!, this.system?.id, this.activeConformanceSnapshot?.id, true)
    } else {
      this.routingService.toOwnConformanceStatements(this.organisationId!, this.system?.id, this.activeConformanceSnapshot?.id, true)
    }
  }

  snapshotSelected(snapshot?: ConformanceSnapshot) {
    const reloadNeeded = snapshot?.id != this.activeConformanceSnapshot?.id
    if (reloadNeeded) {
      this.activeConformanceSnapshot = snapshot
      this.snapshotButtonLabel = (snapshot == undefined)?this.latestSnapshotButtonLabel:snapshot.label
      setTimeout(() => {
        if (this.listView) {
          this.filterControl?.setToggleState(false)
          this.listViewTable?.snapshotChanged()
          this.updateRouting()
        } else {
          this.systemStatus.status = Constants.STATUS.PENDING
          this.systemService.getSystemsByOrganisation(this.organisationId!, snapshot?.id).subscribe((data) => {
            this.systemsLoaded(data)
          }).add(() => {
            // Update the routing path (to avoid loss of state on refresh).
            this.updateRouting()
            this.systemStatus.status = Constants.STATUS.FINISHED
          })
        }
      })
    }
  }

  onExportConformanceOverview(event: ExportReportEvent) {
    if (event.format == 'xml') {
      event.item.exportXmlPending = true
    } else {
      event.item.exportPdfPending = true
    }
    const reportLevel = this.determineReportLevel(event)
    this.reportSupportService.handleConformanceOverviewReport(this.communityIdForSnapshots, this.system!.id, event.item.id, reportLevel, this.activeConformanceSnapshot?.id, event.format, this.dataService.conformanceStatusForConformanceItem(event.item))
    .subscribe(() => {
      // Do nothing further.
    }).add(() => {
      if (event.format == 'xml') {
        event.item.exportXmlPending = false
      } else {
        event.item.exportPdfPending = false
      }
    })
  }

  exportOverview(format: 'xml'|'pdf') {
    this.exportPending = true
    const overallStatus = this.dataService.conformanceStatusForConformanceItems(this.statements)
    this.reportSupportService.handleConformanceOverviewReport(this.communityIdForSnapshots, this.system!.id, undefined, "all", this.activeConformanceSnapshot?.id, format, overallStatus)
    .subscribe(() => {
      // Do nothing further
    }).add(() => {
      this.exportPending = false
    })
  }

  viewTypeToggled() {
    if (!this.listView) {
      this.systemsLoaded(this.systems)
    }
  }

  getSystems(snapshotId?: number) {
    let snapshotIdToUse = snapshotId
    if (snapshotIdToUse == undefined) {
      snapshotIdToUse = this.activeConformanceSnapshot?.id
    }
    return this.systemService.getSystemsByOrganisation(this.organisationId!, snapshotIdToUse)
  }

}
