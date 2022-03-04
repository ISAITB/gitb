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
    this.columnCount = this.columns.length
    if (this.checkboxEnabled) this.columnCount += 1
    if (this.actionVisible) this.columnCount += 1
    if (this.operationsVisible) {
      // Session termination
      this.columnCount += 1
      this.deleteVisibleForRow = (row: TestResultForDisplay) => {
        // This is needed because we may have refreshed a session in a table displaying active sessions that has completed.
        return row.endTime == undefined
      }
    }
    if (this.exportVisible) this.columnCount += 1
    if (this.refreshComplete) {
      this.refreshComplete.subscribe(() => {
        this.sessionBeingRefreshed = undefined
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

  goToSystem(row: TestResultForDisplay) {
    if (this.dataService.isVendorUser) {
      this.routingService.toSystemInfo(row.organizationId!, row.systemId!)
    } else {
      this.routingService.toSystems(row.organizationId!, row.systemId!)
    }
  }

  goToStatement(row: TestResultForDisplay) {
    this.routingService.toConformanceStatement(row.organizationId!, row.systemId!, row.actorId!, row.specificationId!)
  }

  goToOrganisation(row: TestResultForDisplay) {
    const targetOrganisationId = row.organizationId!
    if (targetOrganisationId == this.dataService.vendor!.id) {
      // This is the user's own organisation
      this.routingService.toOwnOrganisationDetails()
    } else {
      // Another organisation
      this.routingService.toOrganisationDetails(row.communityId!, targetOrganisationId)
    }
  }

  refresh(row: TestResultForDisplay) {
    if (this.supportRefresh) {
      this.sessionBeingRefreshed = row
      this.onRefresh.emit(row)
    }
  }

}
