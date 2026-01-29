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

import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
  QueryList,
  Renderer2,
  ViewChild,
  ViewChildren
} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {ReportService} from 'src/app/services/report.service';
import {TestResultForDisplay} from 'src/app/types/test-result-for-display';
import {BaseTableComponent} from '../base-table/base-table.component';
import {SessionData} from '../diagram/test-session-presentation/session-data';
import {SessionLogModalComponent} from '../session-log-modal/session-log-modal.component';
import {mergeMap, Observable, of} from 'rxjs';
import {ProvideInputModalComponent} from 'src/app/modals/provide-input-modal/provide-input-modal.component';
import {TestService} from 'src/app/services/test.service';
import {TestResultReport} from 'src/app/types/test-result-report';
import {LogLevel} from 'src/app/types/log-level';
import {TestInteractionData} from 'src/app/types/test-interaction-data';
import {PopupService} from 'src/app/services/popup.service';
import {PagingControlsApi} from '../paging-controls/paging-controls-api';
import {NavigationControlsConfig} from '../navigation-controls/navigation-controls-config';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {UserInteractionInput} from '../../types/user-interaction-input';

@Component({
    selector: '[app-session-table]',
    templateUrl: './session-table.component.html',
    styleUrls: ['./session-table.component.less'],
    standalone: false
})
export class SessionTableComponent extends BaseTableComponent implements OnInit {

  @Input() sessionTableId = 'session-table'
  @Input() expandedCounter?: { count: number }
  @Input() supportRefresh = false
  @Input() refreshComplete?: EventEmitter<TestResultReport|undefined>
  @Input() copyForOtherRoleOption = false
  @Input() showCheckbox?: EventEmitter<boolean>
  @Input() showNavigationControls = true
  @Output() onRefresh = new EventEmitter<TestResultForDisplay>()
  @ViewChild("pagingControls") pagingControls?: PagingControlsApi
  @ViewChild("tableContainer") tableContainer?: ElementRef
  @ViewChildren("sessionContainer") sessionContainers?: QueryList<ElementRef>

  Constants = Constants
  columnCount = 0
  diagramCollapsed: {[key: string]: boolean} = {}
  diagramCollapsedFinished: {[key: string]: boolean} = {}
  viewLogPending: {[key: string]: boolean} = {}
  sessionBeingRefreshed?: TestResultForDisplay

  constructor(
    private readonly reportService: ReportService,
    private readonly modalService: NgbModal,
    private readonly testService: TestService,
    public readonly dataService: DataService,
    private readonly popupService: PopupService,
    private renderer: Renderer2
  ) { super() }

  ngOnInit(): void {
    for (let column of this.columns) {
      if (column.headerClass == undefined) {
        column.headerClass = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      }
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

  @HostListener('window:resize')
  onWindowResize() {
    this.updateSessionWidths();
  }

  updateSessionWidthsWrapper() {
    setTimeout(() => {
      this.updateSessionWidths()
    }, 1)
  }

  private updateSessionWidths() {
    if (!this.tableContainer || !this.sessionContainers) return;

    const tableWidth = this.tableContainer.nativeElement.offsetWidth;
    const padding = 16; // 2 * 8px
    const targetWidth = tableWidth - padding;

    this.sessionContainers.forEach(sessionEl => {
      this.renderer.setStyle(sessionEl.nativeElement, 'width', `${targetWidth}px`);
    });
  }

  diagramReady(test: SessionData) {
    if (test.diagramState?.interactions) {
      test.diagramState.interactions = this.extractApplicableInteractions(test.diagramState.interactions)
    }
    test.diagramLoaded = true
    this.updateButtonBadges(test)
    setTimeout(() => {
      this.updateSessionWidths()
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

  labelForPendingInteraction(step: TestInteractionData, index: number) {
    if (step?.desc) {
      return step.desc
    } else {
      return "Interaction " + (index + 1)
    }
  }

  private extractApplicableInteractions(interactions: TestInteractionData[]) {
    return interactions.filter((interaction) => !interaction.admin || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin)
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
          interactionData = row.diagramState.interactions.find((interaction) => interaction.stepId == stepId)
        }
        if (interactionData) {
          const modalRef = this.modalService.open(ProvideInputModalComponent, { size: 'lg' })
          const modalInstance = modalRef.componentInstance as ProvideInputModalComponent
          modalInstance.interactions = interactionData.interactions
          modalInstance.inputTitle = interactionData.inputTitle!
          modalInstance.sessionId = row.session
          modalRef.closed.subscribe((result: UserInteractionInput[]) => {
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
        const modal = this.modalService.open(SessionLogModalComponent, { size: 'lg' })
        const modalInstance = modal.componentInstance as SessionLogModalComponent
        modalInstance.messages = logs
      })
    }
  }

  refresh(row: TestResultForDisplay) {
    if (this.supportRefresh) {
      this.sessionBeingRefreshed = row
      this.onRefresh.emit(row)
    }
  }

  toggleDiagramCollapsedFinished(session: string, value: boolean) {
    setTimeout(() => {
      this.diagramCollapsedFinished[session] = value
      this.updateSessionWidths()
    }, 1)
  }

  copyLink(row: TestResultForDisplay, forOtherRole?: boolean) {
    const params: Record<string, string> = {}
    params[Constants.NAVIGATION_QUERY_PARAM.TEST_SESSION_ID] = row.session
    let routePath: string|undefined
    if (forOtherRole) {
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        routePath = `/organisation/tests/${row.organizationId}`
      } else {
        routePath = '/admin/sessions'
      }
    }
    this.dataService.copyExternalLink(params, routePath).subscribe((value) => {
      if (value) {
        this.popupService.success("Link copied to clipboard.")
      }
    })
  }

  toNavigationConfig(row: TestResultForDisplay): NavigationControlsConfig {
    return {
      systemId: row.systemId,
      organisationId: row.organizationId,
      communityId: row.communityId,
      actorId: row.actorId,
      specificationId: row.specificationId,
      domainId: row.domainId,
      testCaseId: row.testCaseId,
      testSuiteId: row.testSuiteId,
    }
  }

}
