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
import { Observable, mergeMap, of } from 'rxjs';
import { ProvideInputModalComponent } from 'src/app/modals/provide-input-modal/provide-input-modal.component';
import { TestService } from 'src/app/services/test.service';
import { TestResultReport } from 'src/app/types/test-result-report';
import { LogLevel } from 'src/app/types/log-level';
import { TestInteractionData } from 'src/app/types/test-interaction-data';
import { filter, find, findIndex } from 'lodash';

@Component({
  selector: '[app-session-table]',
  templateUrl: './session-table.component.html',
  styleUrls: [ './session-table.component.less' ]
})
export class SessionTableComponent extends BaseTableComponent implements OnInit {

  @Input() sessionTableId = 'session-table'
  @Input() expandedCounter?: { count: number }
  @Input() supportRefresh = false
  @Input() refreshComplete?: EventEmitter<TestResultReport|undefined>
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
    private testService: TestService,
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
      this.refreshComplete.subscribe((report) => {
        this.refreshTestSession(report)
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
    if (test.diagramState?.interactions) {
      test.diagramState.interactions = this.extractApplicableInteractions(test.diagramState.interactions)
    }
    test.diagramLoaded = true
    this.updateButtonBadges(test)
    setTimeout(() => {
      test.hideLoadingIcon = true
      test.diagramExpanded = true
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

  private updateButtonBadges(sessionData: SessionData) {
    if (sessionData.diagramState) {
      if (sessionData.diagramState.logs) {
        let previousLogs = (sessionData.reviewedLogLines != undefined)?sessionData.reviewedLogLines:0
        let hasErrors = false
        let hasWarnings = false
        let hasMessages = false
        for (let i = previousLogs; i < sessionData.diagramState.logs.length; i++) {
          const logLevel = this.dataService.logMessageLevel(sessionData.diagramState.logs[i], LogLevel.DEBUG)
          if (logLevel == LogLevel.ERROR) {
            hasErrors = true
          } else if (logLevel == LogLevel.WARN) {
            hasWarnings = true
          } else {
            hasMessages = true            
          }
          if (hasErrors) break;
        }
        sessionData.hasUnreadErrorLogs = hasErrors
        sessionData.hasUnreadWarningLogs = hasWarnings
        sessionData.hasUnreadMessageLogs = hasMessages
      }
    }
  }

  labelForPendingInteraction(session: TestResultForDisplay, step: TestInteractionData) {
    if (step?.desc) {
      return step.desc
    } else {
      const index = findIndex(session.diagramState?.interactions, (step) => step.stepId == step.stepId)
      if (index != undefined) {
        return "Interaction " + (index + 1)
      } else {
        return "Interaction"
      }
    }    
  }

  private extractApplicableInteractions(interactions: TestInteractionData[]) {
    return filter(interactions, (interaction) => !interaction.admin || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin)
  }

  private refreshTestSession(testReport: TestResultReport|undefined) {
    if (testReport && this.sessionBeingRefreshed?.diagramState) {
      this.sessionBeingRefreshed.diagramState.logs = testReport.logs
      if (testReport.interactions) {
        this.sessionBeingRefreshed.diagramState.interactions = this.extractApplicableInteractions(testReport.interactions)
      }
      this.updateButtonBadges(this.sessionBeingRefreshed)
    }
    this.sessionBeingRefreshed = undefined
  }

  displayPendingInteraction(row: TestResultForDisplay, stepId?: string) {
    if (row.diagramState?.interactions) {
      const interactionCount = row.diagramState?.interactions.length
      if (interactionCount > 0) {
        let interactionData: TestInteractionData|undefined
        if (stepId == undefined) {
          interactionData = row.diagramState.interactions[0]
        } else {
          interactionData = find(row.diagramState.interactions, (interaction) => interaction.stepId == stepId)
        }
        if (interactionData) {
          const modalRef = this.modalService.show(ProvideInputModalComponent, {
            backdrop: 'static',
            keyboard: false,
            initialState: {
              interactions: interactionData.interactions,
              inputTitle: interactionData.inputTitle,
              sessionId: row.session
            }
          })
          modalRef.content!.result.subscribe((result) => {
            if (result != undefined) {
              this.testService.provideInput(row.session, interactionData!.stepId, result, interactionData!.admin)
              .subscribe(() => {
                this.refresh(row)
              })
            }
          })
        }
      }
    }
  }

  showTestSessionLog(row: TestResultForDisplay) {
    if (row.diagramState) {
      const sessionId = row.session
      row.hasUnreadErrorLogs = false
      row.hasUnreadWarningLogs = false
      row.hasUnreadMessageLogs = false
      row.reviewedLogLines = row.diagramState.logs?.length
      let logsObservable: Observable<string[]>
      if (row.diagramState.logs != undefined) {
        logsObservable = of(row.diagramState.logs)
      } else {
        this.viewLogPending[sessionId] = true
        logsObservable = this.reportService.getTestSessionLog(sessionId)
        .pipe(
          mergeMap((logs) => {
            if (row.diagramState) {
              row.diagramState.logs = logs
            }
            this.viewLogPending[sessionId] = false
            return of(logs)
          })
        )
      }
      logsObservable.subscribe((logs) => {
        this.modalService.show(SessionLogModalComponent, {
          class: 'modal-lg',
          initialState: {
            messages: logs
          }
        })
      })
    }
  }

  toSystem(row: TestResultForDisplay) {
    if (row.organizationId! == this.dataService.vendor!.id) {
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
    if (row.organizationId! == this.dataService.vendor!.id) {
      // This is the user's own organisation
      this.routingService.toOwnOrganisationDetails()
    } else {
      // Another organisation
      this.routingService.toOrganisationDetails(row.communityId!, row.organizationId!)
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

  toTestSuite(row: TestResultForDisplay) {
    this.routingService.toTestSuite(row.domainId!, row.specificationId!, row.testSuiteId!)
  }

  toTestCase(row: TestResultForDisplay) {
    this.routingService.toTestCase(row.domainId!, row.specificationId!, row.testSuiteId!, row.testCaseId!)
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
    return row.domainId != undefined && (
      this.dataService.isSystemAdmin || (
        this.dataService.isCommunityAdmin && this.dataService.community?.domain != undefined
      )
    )
  }

  showToSpecification(row: TestResultForDisplay) {
    return this.showToDomain(row) && row.specificationId != undefined
  }

  showToActor(row: TestResultForDisplay) {
    return this.showToSpecification(row) && row.actorId != undefined
  }

  showToTestSuite(row: TestResultForDisplay) {
    return this.showToSpecification(row) && row.testSuiteId != undefined
  }

  showToTestCase(row: TestResultForDisplay) {
    return this.showToTestSuite(row) && row.testCaseId != undefined
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
