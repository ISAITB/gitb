import { Component, NgZone, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';
import { find } from 'lodash'
import { RoutingService } from 'src/app/services/routing.service';
import { System } from 'src/app/types/system';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { ConformanceService } from 'src/app/services/conformance.service';
import { ConformanceSnapshot } from 'src/app/types/conformance-snapshot';
import { Observable, forkJoin } from 'rxjs';
import { ExportReportEvent } from 'src/app/types/export-report-event';
import { ReportSupportService } from 'src/app/services/report-support.service';
import { BaseConformanceItemDisplayComponent } from 'src/app/components/base-conformance-item-display/base-conformance-item-display.component';

@Component({
  selector: 'app-conformance-statements',
  templateUrl: './conformance-statements.component.html',
  styleUrls: [ './conformance-statements.component.less' ]
})
export class ConformanceStatementsComponent extends BaseConformanceItemDisplayComponent implements OnInit {

  system?: System
  communityId?: number
  communityIdForSnapshots!: number
  organisationId!: number
  systems!: System[]
  systemStatus = {status: Constants.STATUS.PENDING}
  dataStatus = {status: Constants.STATUS.NONE}

  showCreate = false
  showDomain = false
  showBack = false
  columnCount = -1
  Constants = Constants
  exportPending = false

  showCreateSystem = false
  latestSnapshotButtonLabel?: string
  conformanceSnapshots?: ConformanceSnapshot[]
  snapshotButtonLabel?: string
  currentlySelectedSnapshot?: ConformanceSnapshot

  constructor(
    dataService: DataService,
    zone: NgZone,
    private systemService: SystemService,
    private conformanceService: ConformanceService,
    private route: ActivatedRoute,
    public routingService: RoutingService,
    private reportSupportService: ReportSupportService
  ) { super(dataService, zone) }

  ngOnInit(): void {
    this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.communityIdForSnapshots = this.communityId
      this.showBack = this.organisationId >= 0
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
    this.columnCount = this.showDomain?6:5
    const systemsLoaded = this.systemService.getSystemsByOrganisation(this.organisationId, snapshotId)
    const snapshotsLoaded = this.conformanceService.getConformanceSnapshots(this.communityIdForSnapshots)
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
          this.currentlySelectedSnapshot = referencedSnapshot
          this.snapshotButtonLabel = this.currentlySelectedSnapshot.label
        } else {
          // The snapshot could have been deleted or rendered hidden.
          this.snapshotButtonLabel = this.latestSnapshotButtonLabel
        }
      } else {
        this.snapshotButtonLabel = this.latestSnapshotButtonLabel
      }
      // Systems - this will also load statements and update breadcrumbs.
      this.systemsLoaded(results[0], false)
    }).add(() => {
      this.systemStatus.status = Constants.STATUS.FINISHED
    })
  }

  private systemsLoaded(systems: System[], updateRoutePath: boolean) {
    this.systems = systems
    if (this.systems.length == 1) {
      this.system = systems[0]
    } else if (this.route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID)) {
      const systemId = Number(this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SYSTEM_ID))
      this.system = find(this.systems, (sys) => {
        return sys.id == systemId
      })
    }
    if (this.system) {
      this.systemChanged(updateRoutePath)
    } else {
      this.updateBreadcrumbs()
    }
  }

  private updateBreadcrumbs() {
    if (this.communityId == undefined) {
      this.routingService.ownConformanceStatementsBreadcrumbs(this.organisationId, this.system?.id, this.system?.sname, this.currentlySelectedSnapshot?.id, this.currentlySelectedSnapshot?.label)
    } else {
      this.routingService.conformanceStatementsBreadcrumbs(this.communityId, this.organisationId, undefined, this.system?.id, this.system?.sname, this.currentlySelectedSnapshot?.id, this.currentlySelectedSnapshot?.label)
    }
  }

  systemChanged(updateRoutePath: boolean) {
    this.updateBreadcrumbs()
    this.getConformanceStatements()
    if (updateRoutePath) {
      this.updateRouting()
    }
  }

  getConformanceStatements() {
    this.dataStatus.status = Constants.STATUS.PENDING
    this.conformanceService.getConformanceStatementsForSystem(this.system!.id, this.currentlySelectedSnapshot?.id)
    .subscribe((data) => {
      this.itemsByType = this.dataService.organiseConformanceItemsByType(data)
      this.statements = this.dataService.prepareConformanceStatementItemsForDisplay(data)
      this.filterStatements()
    }).add(() => {
      this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

  onStatementSelect(statement: ConformanceStatementItem) {
    if (this.communityId == undefined) {
      this.routingService.toOwnConformanceStatement(this.organisationId, this.system!.id, statement.id, this.currentlySelectedSnapshot?.id, this.currentlySelectedSnapshot?.label)
    } else {
      this.routingService.toConformanceStatement(this.organisationId, this.system!.id, statement.id, this.communityId, this.currentlySelectedSnapshot?.id, this.currentlySelectedSnapshot?.label)
    }
  }

  createStatement() {
    this.routingService.toCreateConformanceStatement(this.organisationId, this.system!.id, this.communityId)
  }

  back() {
    this.routingService.toOrganisationDetails(this.communityId!, this.organisationId)
  }

  toCreateSystem() {
    if (this.communityId != undefined) {
      this.routingService.toCreateSystem(this.communityId, this.organisationId)
    } else {
      this.routingService.toCreateOwnSystem()
    }
  }

  private updateRouting() {
    if (this.communityId != undefined) {
      this.routingService.toConformanceStatements(this.communityId, this.organisationId, this.system?.id, this.currentlySelectedSnapshot?.id, true)
    } else {
      this.routingService.toOwnConformanceStatements(this.organisationId, this.system?.id, this.currentlySelectedSnapshot?.id, true)
    }
  }

  snapshotSelected(snapshot?: ConformanceSnapshot) {
    let systemsLoaded: Observable<System[]>|undefined
    if (snapshot && snapshot.id != this.currentlySelectedSnapshot?.id) {
      // Selected a non-latest snapshot that differs from the (possibly) currently selected one.
      systemsLoaded = this.systemService.getSystemsByOrganisation(this.organisationId, snapshot.id)
    } else if (snapshot == undefined && this.currentlySelectedSnapshot) {
      // Latest snapshot selected while a previous non-latest one was selected.
      systemsLoaded = this.systemService.getSystemsByOrganisation(this.organisationId)
    }
    if (systemsLoaded) {
      this.currentlySelectedSnapshot = snapshot
      this.snapshotButtonLabel = (snapshot == undefined)?this.latestSnapshotButtonLabel:snapshot.label
      this.systemStatus.status = Constants.STATUS.PENDING
      systemsLoaded.subscribe((data) => {
        this.systemsLoaded(data, false)
      }).add(() => {
        // Update the routing path (to avoid loss of state on refresh).
        this.updateRouting()
        this.systemStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  onExportConformanceOverview(event: ExportReportEvent) {
    if (event.format == 'xml') {
      event.item.exportXmlPending = true
    } else {
      event.item.exportPdfPending = true
    }
    let reportLevel: 'all'|'domain'|'specification'|'group'
    if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN) {
      reportLevel = "domain"
    } else if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP) {
      reportLevel = "group"
    } else if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION) {
      reportLevel = "specification"
    } else {
      reportLevel = "all"
    }
    this.reportSupportService.handleConformanceOverviewReport(this.communityIdForSnapshots, this.system!.id, event.item.id, reportLevel, this.currentlySelectedSnapshot?.id, event.format, this.dataService.conformanceStatusForConformanceItem(event.item))
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
    this.reportSupportService.handleConformanceOverviewReport(this.communityIdForSnapshots, this.system!.id, undefined, "all", this.currentlySelectedSnapshot?.id, format, overallStatus)
    .subscribe(() => {
      // Do nothing further
    }).add(() => {
      this.exportPending = false
    })
  }

}
