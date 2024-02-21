import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';
import { find } from 'lodash'
import { RoutingService } from 'src/app/services/routing.service';
import { System } from 'src/app/types/system';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { CheckboxOptionState } from 'src/app/components/checkbox-option-panel/checkbox-option-state';
import { CheckboxOption } from 'src/app/components/checkbox-option-panel/checkbox-option';
import { ConformanceService } from 'src/app/services/conformance.service';
import { ConformanceSnapshot } from 'src/app/types/conformance-snapshot';
import { Observable, forkJoin } from 'rxjs';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-conformance-statements',
  templateUrl: './conformance-statements.component.html',
  styleUrls: [ './conformance-statements.component.less' ]
})
export class ConformanceStatementsComponent implements OnInit {

  system?: System
  communityId?: number
  communityIdForSnapshots!: number
  organisationId!: number
  systems!: System[]
  systemStatus = {status: Constants.STATUS.PENDING}
  dataStatus = {status: Constants.STATUS.NONE}
  statements: ConformanceStatementItem[] = []
  itemsByType?: { groups: ConformanceStatementItem[], specs: ConformanceStatementItem[], actors: ConformanceStatementItem[] }
  visibleItemCount = 0

  animated = true
  showCreate = false
  showDomain = false
  showBack = false
  columnCount = -1
  Constants = Constants
  exportPending = false

  private static SHOW_SUCCEEDED = '0'
  private static SHOW_FAILED = '1'
  private static SHOW_INCOMPLETE = '2'
  statementFilter?: string
  showCompleted = true
  showFailed = true
  showIncomplete = true
  statusOptions: CheckboxOption[][] = [
    [
      {key: ConformanceStatementsComponent.SHOW_SUCCEEDED, label: 'Succeeded statements', default: this.showCompleted, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)},
      {key: ConformanceStatementsComponent.SHOW_FAILED, label: 'Failed statements', default: this.showFailed, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)},
      {key: ConformanceStatementsComponent.SHOW_INCOMPLETE, label: 'Incomplete statements', default: this.showIncomplete, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)},
    ]
  ]
  showCreateSystem = false
  latestSnapshotButtonLabel?: string
  conformanceSnapshots?: ConformanceSnapshot[]
  snapshotButtonLabel?: string
  currentlySelectedSnapshot?: ConformanceSnapshot

  constructor(
    private systemService: SystemService,
    private conformanceService: ConformanceService,
    public dataService: DataService,
    private route: ActivatedRoute,
    public routingService: RoutingService
  ) { }

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
      this.itemsByType = this.getItemsByType(data)
      this.statements = this.processItems(data)
      this.filterStatements()
    }).add(() => {
      this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

  private getItemsByType(items: ConformanceStatementItem[]): { groups: ConformanceStatementItem[], specs: ConformanceStatementItem[], actors: ConformanceStatementItem[] } {
    let groups: ConformanceStatementItem[] = []
    let specs: ConformanceStatementItem[] = []
    let actors: ConformanceStatementItem[] = []
    for (let domain of items) {
      if (domain.items) {
        for (let specOrGroup of domain.items) {
          if (specOrGroup.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP) {
            groups.push(specOrGroup)
            // Specifications in group
            if (specOrGroup.items) {
              specOrGroup.items.forEach((item) => specs.push(item))
            }
          } else {
            // Specification in domain
            specs.push(specOrGroup)
          }
        }
      }
    }
    for (let spec of specs) {
      if (spec.items) {
        spec.items.forEach((item) => actors.push(item))
      }
    }
    return {
      groups: groups,
      specs: specs,
      actors: actors
    }
  }

  private processItems(items: ConformanceStatementItem[]) {
    if (items.length == 1) {
      // We only have one domain. Hide it unless the user has access to any domain.
      items[0].hidden = this.dataService.community?.domain != undefined
    }
    // Initialise item state.
    this.visit(items, (item) => {
      if (item.collapsed == undefined) {
        item.collapsed = false
      }
      if (item.items && item.items.length == 1) {
        item.items[0].collapsed = false
      }
      if (item.hidden == undefined) {
        item.hidden = false
      }
      item.filtered = true
    })
    return items
  }

  private visit(items: ConformanceStatementItem[]|undefined, visitor: (item: ConformanceStatementItem) => any) {
    if (items) {
      for (let item of items) {
        visitor(item)
        this.visit(item.items, visitor)
      }
    }
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

  filterByStatus(choices: CheckboxOptionState) {
    this.showCompleted = choices[ConformanceStatementsComponent.SHOW_SUCCEEDED]
    this.showFailed = choices[ConformanceStatementsComponent.SHOW_FAILED]
    this.showIncomplete = choices[ConformanceStatementsComponent.SHOW_INCOMPLETE]
    this.filterStatements()
  }

  private toggleAnimated(animatedValue: boolean) {
    setTimeout(() => {
      this.animated = animatedValue
    }, 1)
  }

  filterStatements() {
    this.animated = false
    setTimeout(() => {
      if (this.itemsByType) {
        const defaultFilteredValue = this.statementFilter == undefined && this.showCompleted && this.showFailed && this.showIncomplete
        this.statements.forEach((item) => { item.filtered = defaultFilteredValue; })
        this.itemsByType.groups.forEach((item) => { item.filtered = defaultFilteredValue; })
        this.itemsByType.specs.forEach((item) => { item.filtered = defaultFilteredValue; })
        this.itemsByType.actors.forEach((item) => { item.filtered = defaultFilteredValue; })
        if (!defaultFilteredValue) {
          // Apply filters per item type - only needed if we are going to mark items as filtered (visible)
          this.filterItems(this.statements)
          this.filterItems(this.itemsByType.groups)
          this.filterItems(this.itemsByType.specs)
          this.filterItems(this.itemsByType.actors)
        }
        // Apply match semantics to hierarchy.
        this.filterParentsWithFilteredChildren(this.itemsByType.specs)
        this.filterParentsWithFilteredChildren(this.itemsByType.groups)
        this.filterParentsWithFilteredChildren(this.statements)
        // Update visible item count.
        this.countVisibleItems()
      }
      this.toggleAnimated(true)
    }, 1)
  }

  private filterItems(items: ConformanceStatementItem[]|undefined) {
    if (items) {
      for (let item of items) {
        this.filterItem(item)
      }
    }
  }

  private filterItem(item: ConformanceStatementItem) {
    if (!item.hidden) {
      item.filteredByText = false
      // Text filter.
      if (this.statementFilter) {
        const filterToApply = this.statementFilter.trim().toLowerCase()
        if ((item.name.toLowerCase().indexOf(filterToApply)) >= 0) {
          item.filteredByText = true
        }
      }
      // Status filter
      item.filteredByStatus = false
      const statusFilterApplied = !this.showCompleted || !this.showFailed || !this.showIncomplete
      if (statusFilterApplied) {
        if ((this.showCompleted && this.checkItemStatus(item, Constants.TEST_CASE_RESULT.SUCCESS))
            || (this.showFailed && this.checkItemStatus(item, Constants.TEST_CASE_RESULT.FAILURE))
            || (this.showIncomplete && this.checkItemStatus(item, Constants.TEST_CASE_RESULT.UNDEFINED))) {
              item.filteredByStatus = true
        }
      }
      if ((!this.statementFilter || item.filteredByText) && (!statusFilterApplied || item.filteredByStatus)) {
        item.filtered = true
      }
    }
  }

  private checkItemsStatus(items: ConformanceStatementItem[], statusToLookFor: string): boolean {
    if (items) {
      for (let child of items) {
        let childHasStatus = this.checkItemStatus(child, statusToLookFor)
        if (childHasStatus) {
          return true
        }
      }
    }
    return false
  }


  private checkItemStatus(item: ConformanceStatementItem, statusToLookFor: string): boolean {
    if (item.results) {
      return this.dataService.conformanceStatusForTests(item.results.completed, item.results.failed, item.results.undefined) == statusToLookFor
    } else if (item.items) {
      return this.checkItemsStatus(item.items, statusToLookFor)
    } else {
      return false // We should never reach this case.
    }
  }

  private filterParentsWithFilteredChildren(items: ConformanceStatementItem[]) {
    for (let item of items) {
      const hasChild = item.items != undefined && item.items.length > 0
      const hasVisibleChild = hasChild && (find(item.items, (child) => {
        return child.filtered == true
      }) != undefined)
      if (hasVisibleChild) {
        item.collapsed = false
      }
      if (!item.filtered) {
        item.filtered = hasVisibleChild
      } else {
        // Apply filtering logic to children.
        this.visit(item.items, (item) => {
          if (item.filteredByStatus) {
            item.filtered = true
          }
        })
      }
    }
  }

  private countVisibleItems() {
    let count = 0
    this.visit(this.statements, (item) => {
      if (item.filtered == true) {
        count += 1
      }
    })
    this.visibleItemCount = count
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

  onExportConformanceOverview(item: ConformanceStatementItem) {
    let domainId: number|undefined
    let groupId: number|undefined
    let specId: number|undefined
    if (item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN) domainId = item.id
    if (item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP) groupId = item.id
    if (item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION) specId = item.id
    item.exportPending = true
    this.conformanceService.exportConformanceOverviewReport(this.system!.id, domainId, groupId, specId, this.currentlySelectedSnapshot?.id)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "conformance_overview_report.pdf");
    }).add(() => {
      item.exportPending = false
    })
  }

  exportOverview() {
    this.exportPending = true
    this.conformanceService.exportConformanceOverviewReport(this.system!.id, undefined, undefined, undefined, this.currentlySelectedSnapshot?.id)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "conformance_overview_report.pdf");
    }).add(() => {
      this.exportPending = false
    })
  }

}
