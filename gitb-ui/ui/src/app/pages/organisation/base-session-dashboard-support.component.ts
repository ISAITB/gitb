import {Component, EventEmitter} from '@angular/core';
import {Constants} from '../../common/constants';
import {TestResultForDisplay} from '../../types/test-result-for-display';
import {TestService} from '../../services/test.service';
import {ConfirmationDialogService} from '../../services/confirmation-dialog.service';
import {PopupService} from '../../services/popup.service';
import {TestResultReport} from '../../types/test-result-report';
import {DiagramLoaderService} from '../../components/diagram/test-session-presentation/diagram-loader.service';
import {TestResultSearchCriteria} from '../../types/test-result-search-criteria';

@Component({
  template: '',
  standalone: false
})
export abstract class BaseSessionDashboardSupportComponent {

  currentPage = 1
  isNextPageDisabled = false
  completedTestsTotalCount = 0
  isPreviousPageDisabled = false
  sessionRefreshCompleteEmitter = new EventEmitter<TestResultReport|undefined>()
  sessionIdToShow?: string
  activeSortOrder = "asc"
  activeSortColumn = "startTime"
  completedSortOrder = "desc"
  completedSortColumn = "endTime"

  protected constructor(
    protected testService: TestService,
    protected confirmationDialogService: ConfirmationDialogService,
    protected popupService: PopupService,
    protected diagramLoaderService: DiagramLoaderService
  ) {}

  updatePagination() {
    if (this.currentPage == 1) {
      this.isNextPageDisabled = this.completedTestsTotalCount <= Constants.TABLE_PAGE_SIZE
      this.isPreviousPageDisabled = true
    } else if (this.currentPage == Math.ceil(this.completedTestsTotalCount / Constants.TABLE_PAGE_SIZE)) {
      this.isNextPageDisabled = true
      this.isPreviousPageDisabled = false
    } else {
      this.isNextPageDisabled = false
      this.isPreviousPageDisabled = false
    }
  }

  protected applyCompletedDataToTestSession(displayedResult: TestResultForDisplay, loadedResult: TestResultReport) {
    displayedResult.endTime = loadedResult.result.endTime
    displayedResult.result = loadedResult.result.result
    displayedResult.obsolete = loadedResult.result.obsolete
    if (displayedResult.diagramState && loadedResult.result.outputMessage) {
      displayedResult.diagramState.outputMessage = loadedResult.result.outputMessage
      displayedResult.diagramState.outputMessageType = this.diagramLoaderService.determineOutputMessageType(loadedResult.result.result)
    }
  }

  protected refreshSessionDiagram(session: TestResultForDisplay, result: TestResultReport) {
    this.diagramLoaderService.loadTestStepResults(session.session)
      .subscribe((data) => {
        const currentState = session.diagramState!
        if (result.result.endTime) {
          // Session completed
          this.popupService.info("The test session has completed.")
          this.applyCompletedDataToTestSession(session, result)
        }
        this.diagramLoaderService.updateStatusOfSteps(session, currentState.stepsOfTests[session.session], data)
      }).add(() => {
      this.sessionRefreshCompleteEmitter.emit(result)
    })
  }

  stopSession(session: TestResultForDisplay) {
    this.confirmationDialogService.confirmedDangerous('Confirm termination', 'Are you certain you want to terminate this session?', 'Terminate', 'Cancel')
      .subscribe(() => {
        session.deletePending = true
        this.testService.stop(session.session)
          .subscribe(() => {
            this.queryDatabase()
            this.popupService.success('Test session terminated.')
          }).add(() => {
          session.deletePending = false
        })
      })
  }

  protected addSessionDashboardCriteriaToTestResultSearchCriteria(searchCriteria: TestResultSearchCriteria, filterData:{[key: string]: any}|undefined) {
    if (filterData) {
      searchCriteria.testSuiteIds = filterData[Constants.FILTER_TYPE.TEST_SUITE]
      searchCriteria.testCaseIds = filterData[Constants.FILTER_TYPE.TEST_CASE]
      searchCriteria.results = filterData[Constants.FILTER_TYPE.RESULT]
      searchCriteria.startTimeBeginStr = filterData.startTimeBeginStr
      searchCriteria.startTimeEndStr = filterData.startTimeEndStr
      searchCriteria.endTimeBeginStr = filterData.endTimeBeginStr
      searchCriteria.endTimeEndStr = filterData.endTimeEndStr
      searchCriteria.sessionId = filterData.sessionId
    }
    if (this.sessionIdToShow != undefined) {
      searchCriteria.sessionId = this.sessionIdToShow
    }
    searchCriteria.activeSortColumn = this.activeSortColumn
    searchCriteria.activeSortOrder = this.activeSortOrder
    searchCriteria.completedSortColumn = this.completedSortColumn
    searchCriteria.completedSortOrder = this.completedSortOrder
    searchCriteria.currentPage = this.currentPage
  }

  abstract queryDatabase(): void

}
