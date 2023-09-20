import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { ReportService } from 'src/app/services/report.service';
import { RoutingService } from 'src/app/services/routing.service';
import { TestResultForDisplay } from 'src/app/types/test-result-for-display';
import { BaseTableComponent } from '../base-table/base-table.component';
import { SessionData } from '../diagram/test-session-presentation/session-data';
import { SessionPresentationData } from '../diagram/test-session-presentation/session-presentation-data';
import { SessionLogModalComponent } from '../session-log-modal/session-log-modal.component';

@Component({
  selector: '[app-session-table]',
  templateUrl: './session-table.component.html',
  styleUrls: [ './session-table.component.less' ]
})
export class SessionTableComponent extends BaseTableComponent implements OnInit {

  @Input() sessionTableId = 'session-table'
  @Input() expandedCounter?: { count: number }
  @Input() supportRefresh = false
  @Input() refreshComplete?: EventEmitter<void>
  @Input() showCheckbox?: EventEmitter<boolean>
  @Output() onRefresh = new EventEmitter<TestResultForDisplay>()

  Constants = Constants
  columnCount = 0
  diagramCollapsed: {[key: string]: boolean} = {}
  viewLogPending: {[key: string]: boolean} = {}
  stateEmitters: {[key: string]: EventEmitter<void>} = {}
  sessionBeingRefreshed?: TestResultForDisplay
  diagramStateForSessionBeingRefreshed?: SessionPresentationData

  constructor(
    private reportService: ReportService,
    private modalService: BsModalService,
    private routingService: RoutingService,
    public dataService: DataService
  ) { super() }

  ngOnInit(): void {
    for (let column of this.columns) {
      column.headerClass = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      if (column.sortable) {
        column.headerClass = column.headerClass + ' sortable'
      }
    }
    this.splitColumns()
    this.columnCount = this.columns.length + 1 // PLus one for expandable.
    if (this.checkboxEnabled) this.columnCount += 1
    if (this.actionVisible || this.operationsVisible || this.exportVisible) this.columnCount += 1
    if (this.operationsVisible) {
      // Session termination
      this.deleteVisibleForRow = (row: TestResultForDisplay) => {
        // This is needed because we may have refreshed a session in a table displaying active sessions that has completed.
        return row.endTime == undefined
      }
    }
    if (this.refreshComplete) {
      this.refreshComplete.subscribe(() => {
        this.sessionBeingRefreshed = undefined
      })
    }
    if (this.showCheckbox) {
      this.showCheckbox.subscribe((show) => {
        if (show) {
          if (!this.checkboxEnabled) {
            this.checkboxEnabled = true
            this.columnCount += 1
          }
        } else {
          if (this.checkboxEnabled) {
            this.checkboxEnabled = false
            this.columnCount -= 1
          }
        }
      })
    }
  }

  diagramReady(test: SessionData) {
    test.diagramLoaded = true;
    setTimeout(() => {
      test.hideLoadingIcon = true;
      test.diagramExpanded = true;
    }, 200)
  }

  onExpand(data: TestResultForDisplay) {
    data.expanded = data.expanded === undefined || !data.expanded
    if (this.expandedCounter !== undefined) {
      if (data.expanded) {
        this.expandedCounter.count = this.expandedCounter.count + 1
      } else {
        this.expandedCounter.count = this.expandedCounter.count - 1
      }
    }
  }

  rowClass(row: TestResultForDisplay) {
    let rowClass = ''
    if (this.rowStyle) {
      let customClass = this.rowStyle(row)
      if (customClass !== undefined) {
        rowClass = rowClass + ' ' + customClass
      }
    }
    if (this.allowSelect || this.allowMultiSelect || this.onSelect) {
      rowClass = rowClass + ' selectable'
    }
    return rowClass
  }

  showTestSessionLog(row: TestResultForDisplay) {
    const sessionId = row.session
    this.viewLogPending[sessionId] = true
    this.reportService.getTestSessionLog(sessionId)
    .subscribe((logs: string[]) => {
      this.modalService.show(SessionLogModalComponent, {
        class: 'modal-lg',
        initialState: {
          messages: logs
        }
      })
    }).add(() => {
      delete this.viewLogPending[sessionId]
    })
  }

  toSystem(row: TestResultForDisplay) {
    const targetOrganisationId = row.organizationId!
    if (targetOrganisationId == this.dataService.vendor!.id) {
      // This is the user's own organisation
      this.routingService.toOwnSystemDetails(row.systemId!)
    } else {
      this.routingService.toSystemDetails(row.communityId!, row.organizationId!, row.systemId!)
    }
  }

  toStatement(row: TestResultForDisplay) {
    if (row.organizationId! == this.dataService.vendor?.id) {
      this.routingService.toOwnConformanceStatement(row.organizationId!, row.systemId!, row.actorId!)
    } else {
      this.routingService.toConformanceStatement(row.organizationId!, row.systemId!, row.actorId!, row.communityId!)
    }
  }

  toOrganisation(row: TestResultForDisplay) {
    const targetOrganisationId = row.organizationId!
    if (targetOrganisationId == this.dataService.vendor!.id) {
      // This is the user's own organisation
      this.routingService.toOwnOrganisationDetails()
    } else {
      // Another organisation
      this.routingService.toOrganisationDetails(row.communityId!, targetOrganisationId)
    }
  }

  toCommunity(row: TestResultForDisplay) {
    this.routingService.toCommunity(row.communityId!)
  }

  toDomain(row: TestResultForDisplay) {
    this.routingService.toDomain(row.domainId!)
  }

  toSpecification(row: TestResultForDisplay) {
    this.routingService.toSpecification(row.domainId!, row.specificationId!)
  }

  toActor(row: TestResultForDisplay) {
    this.routingService.toActor(row.domainId!, row.specificationId!, row.actorId!)
  }

  showToCommunity(row: TestResultForDisplay) {
    return row.communityId != undefined && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)
  }

  showToOrganisation(row: TestResultForDisplay) {
    return row.organizationId != undefined
  }

  showToSystem(row: TestResultForDisplay) {
    return this.showToOrganisation(row) && row.systemId != undefined
  }

  showToDomain(row: TestResultForDisplay) {
    return row.domainId != undefined && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)
  }

  showToSpecification(row: TestResultForDisplay) {
    return this.showToDomain(row) && row.specificationId != undefined
  }

  showToActor(row: TestResultForDisplay) {
    return this.showToSpecification(row) && row.actorId != undefined
  }

  showToStatement(row: TestResultForDisplay) {
    return row.organizationId != undefined && row.systemId != undefined && row.communityId != undefined && row.actorId != undefined && row.specificationId != undefined
  }

  showPartyNavigation(row: TestResultForDisplay) {
    return this.showToCommunity(row) || this.showToOrganisation(row) || this.showToSystem(row)
  }

  showSpecificationNavigation(row: TestResultForDisplay) {
    return this.showToDomain(row) || this.showToSpecification(row) || this.showToActor(row)
  }

  refresh(row: TestResultForDisplay) {
    if (this.supportRefresh) {
      this.sessionBeingRefreshed = row
      this.onRefresh.emit(row)
    }
  }

}
