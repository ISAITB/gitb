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
  OnChanges, OnDestroy,
  OnInit,
  Output,
  QueryList,
  Renderer2,
  SimpleChanges,
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
import {forkJoin, mergeMap, Observable, of, Subscription} from 'rxjs';
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
import {TestResultCommentsModalComponent} from '../../modals/test-result-comments-modal/test-result-comments-modal.component';
import {TestSessionPresentationComponent} from '../diagram/test-session-presentation/test-session-presentation.component';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {CheckboxOptionPanelComponent} from '../checkbox-option-panel/checkbox-option-panel.component';
import {SessionInfoPanelApi} from '../session-info-panel/session-info-panel-api';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';

@Component({
    selector: '[app-session-table]',
    templateUrl: './session-table.component.html',
    styleUrls: ['./session-table.component.less'],
    standalone: false
})
export class SessionTableComponent extends BaseTableComponent implements OnInit, OnChanges, OnDestroy {

  @Input() sessionTableId = 'session-table'
  @Input() expandedCounter?: { count: number }
  @Input() supportRefresh = false
  @Input() refreshComplete?: EventEmitter<TestResultReport|undefined>
  @Input() copyForOtherRoleOption = false
  @Input() showCheckbox?: EventEmitter<boolean>
  @Input() showNavigationControls = true
  @Input() columnChooserOptions?: CheckboxOption[][]
  @Output() onRefresh = new EventEmitter<TestResultForDisplay>()
  @Output() columnChooserUpdated = new EventEmitter<CheckboxOptionState>()
  @Output() columnChooserClosed = new EventEmitter<void>()
  @ViewChild("pagingControls") pagingControls?: PagingControlsApi
  @ViewChild("columnChooserPanel") columnChooserPanel?: CheckboxOptionPanelComponent
  @ViewChild("tableContainer") tableContainer?: ElementRef
  @ViewChildren("sessionContainer") sessionContainers?: QueryList<ElementRef>
  @ViewChildren("testSessionPresentationComponent") testSessionPresentationComponents?: QueryList<TestSessionPresentationComponent>
  @ViewChildren("sessionInfoPanel") sessionInfoPanels?: QueryList<SessionInfoPanelApi>
  @ViewChildren("sessionFlagControl") sessionFlagControls?: QueryList<CheckBoxOptionPanelComponentApi>

  Constants = Constants
  columnCount = 0
  diagramCollapsedFinished: {[key: string]: boolean} = {}
  viewLogPending: {[key: string]: boolean} = {}
  sessionBeingRefreshed?: TestResultForDisplay
  pageSizeChangeSubscription?: Subscription;
  rowsAnimated = true

  constructor(
    private readonly reportService: ReportService,
    private readonly modalService: NgbModal,
    private readonly testService: TestService,
    public readonly dataService: DataService,
    private readonly popupService: PopupService,
    private renderer: Renderer2
  ) { super() }

  ngOnInit(): void {
    this.processColumns()
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
    this.setRowsAnimated()
    this.pageSizeChangeSubscription = this.dataService.onPageSizeChange$.subscribe((newSize: number) => this.setRowsAnimated())
  }

  ngOnDestroy(): void {
    if (this.pageSizeChangeSubscription) {
      this.pageSizeChangeSubscription.unsubscribe();
    }
  }

  private setRowsAnimated(): void {
    // If we are showing 100 rows per table disable row animations as this results in choppy frame updates.
    this.rowsAnimated = this.dataService.defaultPagingTableSize < 100
  }

  /** Refresh the column chooser panel options (e.g. to update disabled flags after a toggle). */
  refreshColumnChooser(newOptions: CheckboxOption[][]): void {
    this.columnChooserPanel?.refresh(newOptions)
  }

  // The column chooser panel is a single instance owned directly by this component (unlike the
  // per-row option panels forwarded to via tableRowComponents), so it needs to be included in the
  // same top-level document listener forwarding chain rather than adding its own listeners. The
  // per-row session info panels are likewise forwarded to here rather than each registering its own
  // document-level listener, which would not scale well with page sizes of ~100 rows.
  override clickRegistered(event: Event) {
    super.clickRegistered(event)
    this.columnChooserPanel?.documentClick(event)
    this.sessionInfoPanels?.forEach(panel => panel.documentClick(event))
    this.sessionFlagControls?.forEach(panel => panel.documentClick(event))
  }

  override escapeRegistered() {
    super.escapeRegistered()
    this.columnChooserPanel?.documentEscape()
    this.sessionInfoPanels?.forEach(panel => panel.documentEscape())
    this.sessionFlagControls?.forEach(panel => panel.documentEscape())
  }

  /**
   * (Re)derive the header classes, the left/right column split, and the column count from the
   * current `columns` input. Called on init and whenever the columns input is replaced (e.g. as a
   * result of the user toggling visible columns via the column chooser), so headers, cell data and
   * column count all stay in sync with what is actually displayed.
   */
  private processColumns(): void {
    for (let column of this.columns) {
      if (column.headerClass == undefined) {
        column.headerClass = 'tb-'+column.title.toLowerCase().replace(' ', '-')
      }
      if (column.sortable) {
        column.headerClass = column.headerClass + ' sortable'
      }
    }
    this.columnsLeft = []
    this.columnsRight = []
    this.splitColumns()
    this.columnCount = this.columns.length + 1 // Plus one for expandable.
    if (this.checkboxEnabled) this.columnCount += 1
    if (this.actionVisible || this.operationsVisible || this.exportVisible || this.optionsVisible) this.columnCount += 1
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['columns'] && !changes['columns'].firstChange) {
      this.processColumns()
    }
    // Precompute the row CSS class once per data load instead of evaluating it for every row on
    // every change-detection pass (which is very expensive at large page sizes, especially during
    // ngbCollapse animations that tick change detection at ~60fps).
    if (changes['data'] && this.data) {
      for (const row of this.data as TestResultForDisplay[]) {
        row.rowClass = this.computeRowClass(row)
        row.trackKey = row.session + '|' + row.result
      }
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
    if (test.expansionPending) {
      // The 'ready' event fires once the diagram data is loaded, but the diagram is only rendered into
      // the DOM on the following change-detection cycle. Defer opening the row to a later task so the
      // diagram is fully rendered first; ngbCollapse then measures the final height and the expand
      // animation stays fluid (no mid-animation jump as the diagram appears).
      test.expansionPending = false
      setTimeout(() => {
        this.updateSessionWidths()
        test.expanded = true
        test.expandedOrder = Date.now()
        if (this.expandedCounter !== undefined) {
          this.expandedCounter.count = this.expandedCounter.count + 1
        }
      })
    } else {
      setTimeout(() => this.updateSessionWidths())
    }
  }

  onExpand(data: TestResultForDisplay) {
    if (data.expanded) {
      // Collapse immediately.
      data.expanded = false
      if (this.expandedCounter !== undefined) {
        this.expandedCounter.count = this.expandedCounter.count - 1
      }
    } else if (data.expansionPending) {
      // Expansion already in progress - ignore further clicks until loading completes.
      return
    } else if (data.diagramLoaded) {
      // The diagram was already loaded on a previous expansion: open immediately.
      data.expanded = true
      data.expandedOrder = Date.now()
      if (this.expandedCounter !== undefined) {
        this.expandedCounter.count = this.expandedCounter.count + 1
      }
      this.loadSessionComments(data)
    } else {
      // First expansion: keep the row collapsed and show a spinner on the row's expand icon while the
      // diagram loads. The diagram is rendered (gated on expansionPending in the template) so the load
      // starts, but it stays hidden inside the collapsed row. diagramReady() opens the row, with the
      // diagram already in place, once loading completes.
      data.expansionPending = true
      this.loadSessionComments(data)
    }
  }

  public loadSessionComments(data: TestResultForDisplay) {
    if (!this.supportRefresh && data.commentsLoaded !== true) {
      data.commentsPending = true
      this.testService.getTestSessionComments(data.session).subscribe((comments) => {
        data.hasComments = comments != undefined
      }).add(() => {
        data.commentsPending = false
        data.commentsLoaded = true
      })
    }
  }

  private computeRowClass(row: TestResultForDisplay) {
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
    // Cache the config on the row so the binding yields a stable object reference rather than
    // allocating a new one (and re-triggering the navigation controls) on every change detection.
    if (!row.navigationConfig) {
      row.navigationConfig = {
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
    return row.navigationConfig
  }

  viewComments(row: TestResultForDisplay) {
    row.commentsPending = true
    forkJoin([
      this.testService.getTestSessionComments(row.session),
      this.testService.getTestSessionResultMinimal(row.session)
    ]).subscribe((data) => {
      const modal = this.modalService.open(TestResultCommentsModalComponent, { size: 'lg' })
      const modalInstance = modal.componentInstance as TestResultCommentsModalComponent
      modalInstance.sessionId = row.session
      modalInstance.sessionResult = row.result
      modalInstance.sessionOutputMessage = data[1]?.outputMessage
      modalInstance.sessionOwner = row.organizationId
      modalInstance.sessionOwnerName = row.organization
      modalInstance.comments = data[0]
      modalInstance.commentsEditable = !row.obsolete
      modalInstance.updateResult.subscribe((result) => {
        row.result = result.result;
        row.trackKey = row.session + '|' + row.result
        this.testSessionPresentationComponents?.find((presentation => presentation.sessionId() === row.session))?.updateOutputMessage(result.outputMessage, result.result)
        this.tableRowComponents?.forEach((component) => component.refreshData())
      });
      modalInstance.commentUpdate.subscribe((hasComments) => {
        row.hasComments = hasComments;
      });
    }).add(() => {
      row.commentsPending = false
    })
  }

  flagPending: {[key: string]: boolean} = {}

  onFlagChanged(row: TestResultForDisplay, flagId: number|undefined) {
    this.flagPending[row.session] = true
    this.testService.setTestSessionFlag(row.session, flagId).subscribe(() => {
      row.flagId = flagId
      const flag = this.dataService.getTestFlag(row.communityId, flagId)
      row.flagDisplay = flag ? { colour: flag.colour, name: flag.name } : undefined
      // The row's own Flag column cell data was precomputed when the row was (re)rendered - refresh it
      // now so the (collapsed) table row's icon reflects the just-changed flag immediately.
      this.tableRowComponents?.forEach((component) => component.refreshData())
    }).add(() => {
      this.flagPending[row.session] = false
    })
  }

}
