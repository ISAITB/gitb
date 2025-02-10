import { Component, EventEmitter, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { saveAs } from 'file-saver';
import { cloneDeep, filter, find, map as lmap, remove } from 'lodash';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { Observable, of, Subscription, throwError, timer } from 'rxjs';
import { catchError, map, mergeMap, share } from 'rxjs/operators';
import { WebSocketSubject } from 'rxjs/webSocket';
import { Constants } from 'src/app/common/constants';
import { CheckboxOption } from 'src/app/components/checkbox-option-panel/checkbox-option';
import { CheckboxOptionState } from 'src/app/components/checkbox-option-panel/checkbox-option-state';
import { ActorInfo } from 'src/app/components/diagram/actor-info';
import { DiagramEvents } from 'src/app/components/diagram/diagram-events';
import { StepReport } from 'src/app/components/diagram/report/step-report';
import { StepData } from 'src/app/components/diagram/step-data';
import { SessionLogModalComponent } from 'src/app/components/session-log-modal/session-log-modal.component';
import { SimulatedConfigurationDisplayModalComponent } from 'src/app/components/simulated-configuration-display-modal/simulated-configuration-display-modal.component';
import { ProvideInputModalComponent } from 'src/app/modals/provide-input-modal/provide-input-modal.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { HtmlService } from 'src/app/services/html.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { TestService } from 'src/app/services/test.service';
import { WebSocketService } from 'src/app/services/web-socket.service';
import { LoadingStatus } from 'src/app/types/loading-status.type';
import { LogLevel } from 'src/app/types/log-level';
import { SUTConfiguration } from 'src/app/types/sutconfiguration';
import { TestInteractionData } from 'src/app/types/test-interaction-data';
import { UserInteraction } from 'src/app/types/user-interaction';
import { WebSocketMessage } from 'src/app/types/web-socket-message';
import { ConformanceTestCase } from '../organisation/conformance-statement/conformance-test-case';

@Component({
    selector: 'app-test-execution',
    templateUrl: './test-execution.component.html',
    styleUrls: ['./test-execution.component.less'],
    standalone: false
})
export class TestExecutionComponent implements OnInit, OnDestroy {

  testsToExecute: ConformanceTestCase[] = []
  actorId!: number
  systemId!: number
  communityId?: number
  specificationId?: number
  organisationId!: number
  isAdmin = false
  documentationExists = false

  showCompleted = false
  showPending = true
  startAutomatically = true

  started = false
  nextWaitingToStart = false
  stopped = false
  allStopped = false
  firstTestStarted = false
  reload = false
  startAfterConfigurationComplete = false
  currentTestIndex = -1
  currentTest?: ConformanceTestCase

  progressIcons: {[key: number]: string} = {}
  testCaseStatus: {[key: number]: number} = {}
  testCaseOutput: {[key: number]: string} = {}
  testCaseExpanded: {[key: number]: boolean} = {}
  testCaseVisible: {[key: number]: boolean} = {}
  testCaseCounter: {[key: number]: number} = {}
  stepsOfTests: {[key: number]: StepData[]} = {}
  actorInfoOfTests: {[key: string]: ActorInfo[]} = {}
  interactionStepsOfTests: {[key: number]: TestInteractionData[]} = {}
  logMessages: {[key: number]: string[]} = {}
  logMessageEventEmitters: {[key: number]: EventEmitter<string>} = {}
  unreadLogMessages: {[key: number]: boolean} = {}
  unreadLogErrors: {[key: number]: boolean} = {}
  unreadLogWarnings: {[key: number]: boolean} = {}
  testCaseWithOpenLogView?: number
  exportXmlPending: {[key: number]: boolean} = {}
  exportPdfPending: {[key: number]: boolean} = {}

  actor?: string
  session?: string
  testCaseLoadStatus: LoadingStatus = {status: Constants.STATUS.NONE}
  testPreparationStatus: LoadingStatus = {status: Constants.STATUS.NONE}
  simulatedConfigs?: SUTConfiguration[]
  currentSimulatedConfigs?: SUTConfiguration[]
  messagesToProcess?: WebSocketMessage[]
  testEvents: {[key: number]: DiagramEvents} = {}
  columnCount = 4
  currentInteractionStepId?: string
  currentInteractionModal?: BsModalRef<ProvideInputModalComponent>

  private ws?: WebSocketSubject<any>
  private heartbeat?: Subscription
  private messageProcessing?: Subscription
  Constants = Constants

  private static SHOW_COMPLETED = '0'
  private static SHOW_PENDING = '1'
  private static CONTINUE_AUTOMATICALLY = '2'
  testOptions: CheckboxOption[][] = [
    [
      {key: TestExecutionComponent.SHOW_COMPLETED, label: 'Show completed tests', default: false },
      {key: TestExecutionComponent.SHOW_PENDING, label: 'Show pending tests', default: true }
    ],
    [
      {key: TestExecutionComponent.CONTINUE_AUTOMATICALLY, label: 'Continue automatically', default: true }
    ]
  ]

  constructor(
    private route: ActivatedRoute,
    private modalService: BsModalService,
    private testService: TestService,
    private conformanceService: ConformanceService,
    private reportService: ReportService,
    public dataService: DataService,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private webSocketService: WebSocketService,
    private errorService: ErrorService,
    private routingService: RoutingService,
    private specificationService: SpecificationService
  ) { }

  private queryParamToNumber(paramName: string): number|undefined {
    const value = this.route.snapshot.queryParamMap.get(paramName)
    if (value == undefined) {
      return undefined
    } else {
      return Number(value)
    }
  }

  private setupTests(tests: ConformanceTestCase[]) {
    this.testsToExecute = filter(tests, (tc) => !tc.disabled) // Sanity check
    this.documentationExists = this.testCasesHaveDocumentation()
    if (this.documentationExists) {
      this.columnCount = 5
    } else {
      this.columnCount = 4
    }
    let counter = 1
    for (let test of tests) {
      test.sessionId = undefined
      this.testCaseCounter[test.id] = counter
      counter += 1
    }
    this.initialiseTestMaps()
    // Start initialisation
    for (let test of this.testsToExecute) {
      this.updateTestCaseStatus(test.id, Constants.TEST_CASE_STATUS.PENDING)
      this.actorInfoOfTests[test.id] = []
    }
    this.updateTestCaseVisibility()
  }

  ngOnInit(): void {
    this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    this.actorId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))
    this.systemId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID))
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    }
    this.isAdmin = this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin
    this.specificationService.getSpecificationIdOfActor(this.actorId)
    .subscribe((data) => {
      this.specificationId = data.id
      this.initialiseState()
      this.initialiseTestCases()
    })
  }

  private initialiseTestMaps() {
    for (let test of this.testsToExecute) {
      this.testEvents[test.id] = new DiagramEvents()
      this.logMessages[test.id] = []
      this.logMessageEventEmitters[test.id] = new EventEmitter<string>()
      this.unreadLogMessages[test.id] = false
      this.unreadLogErrors[test.id] = false
      this.unreadLogWarnings[test.id] = false
    }
  }

  ngOnDestroy(): void {
    this.leavingTestExecutionPage()
  }

  private initialiseState() {
    this.started = false
    this.nextWaitingToStart = false
    this.stopped = false
    this.allStopped = false
    this.firstTestStarted = false
    this.reload = false
    this.startAfterConfigurationComplete = false
    this.progressIcons = {}
    this.testCaseStatus = {}
    this.testCaseOutput = {}
    this.testCaseExpanded = {}
    this.testCaseVisible = {}
    this.testCaseCounter = {}
    this.stepsOfTests = {}
    this.interactionStepsOfTests = {}
    this.actorInfoOfTests = {}
    this.actor = undefined
    this.session = undefined
    this.simulatedConfigs = undefined
    this.currentSimulatedConfigs = undefined
    this.messagesToProcess = undefined
    this.currentTest = undefined
    this.currentTestIndex = -1
    this.testCaseLoadStatus.status = Constants.STATUS.NONE
    this.testPreparationStatus.status = Constants.STATUS.NONE
  }

  private initialiseTestCases() {
    // Get and prepare test cases
    this.testCaseLoadStatus.status = Constants.STATUS.PENDING
    this.loadTestCases()
    .subscribe((data) => {
      this.setupTests(data)
      this.prepareNextTest(false)
    }).add(() => {
      this.testCaseLoadStatus.status = Constants.STATUS.FINISHED
    })
  }

  private loadTestCases(): Observable<ConformanceTestCase[]> {
    const testsToExecute = this.dataService.getTestsToExecute()
    if (testsToExecute != undefined) {
      return of(testsToExecute)
    } else {
      // We lost our state following a refresh - recreate state.
      const testSuiteId = this.queryParamToNumber('ts')
      if (testSuiteId != undefined) {
        return this.conformanceService.getConformanceStatusForTestSuiteExecution(this.actorId, this.systemId, testSuiteId)
        .pipe(
          map((data) => {
            // There will always be one test suite returned.
            if (data.testSuites.length > 0) {
              return data.testSuites[0].testCases
            } else {
              return []
            }
          })
        )
      } else {
        const testCaseId = this.queryParamToNumber('tc')
        return this.conformanceService.getTestSuiteTestCaseForExecution(testCaseId!)
        .pipe(
          map((data) => {
            // There will always be one test case returned.
            return [{
              id: data.id,
              sname: data.sname,
              description: data.description,
              hasDocumentation: data.hasDocumentation!,
              result: Constants.TEST_CASE_RESULT.UNDEFINED,
              optional: data.optional,
              disabled: data.disabled
            }]
          })
        )
      }
    }
  }

  getTestCaseDefinition(testCaseToLookup: number): Observable<void> {
    return this.testService.getTestCaseDefinitionByStatement(testCaseToLookup, this.actorId, this.systemId).pipe(
      mergeMap((testCase) => {
        if (testCase.preliminary != undefined) {
          this.currentTest!.preliminary = testCase.preliminary
        }
        return this.testService.prepareTestCaseDisplayActors(testCase, this.specificationId).pipe(
          map((actorData) => {
            this.actorInfoOfTests[testCaseToLookup] = actorData
            this.stepsOfTests[testCaseToLookup] = testCase.steps
            this.testEvents[this.currentTest!.id].signalTestLoad({ testId: testCaseToLookup })
          })
        )
      }), share()
    )
  }

  testCasesHaveDocumentation() {
    if (this.testsToExecute != undefined) {
      for (let test of this.testsToExecute) {
        if (test.hasDocumentation) {
          return true
        }
      }
    }
    return false
  }

  stopAll() {
    this.allStopped = true
    this.nextWaitingToStart = false
    this.stopped = true
    this.started = false
    this.reload = true
    if (this.session != undefined) {
      this.stop(this.session, true)
    }
  }

  updateTestCaseStatus(testId: number, status: number) {
    this.testCaseStatus[testId] = status
    if (status == Constants.TEST_CASE_STATUS.PROCESSING) this.progressIcons[testId] = "fa-gear fa-spin-override test-case-running"
    else if (status == Constants.TEST_CASE_STATUS.READY) this.progressIcons[testId] = "fa-gear test-case-ready"
    else if (status == Constants.TEST_CASE_STATUS.PENDING) this.progressIcons[testId] = "fa-clock-o test-case-pending"
    else if (status == Constants.TEST_CASE_STATUS.ERROR) this.progressIcons[testId] = "fa-times-circle test-case-error"
    else if (status == Constants.TEST_CASE_STATUS.COMPLETED) this.progressIcons[testId] = "fa-check-circle test-case-success"
    else if (status == Constants.TEST_CASE_STATUS.STOPPED) this.progressIcons[testId] = "fa-ban test-case-stopped"
    else if (status == Constants.TEST_CASE_STATUS.CONFIGURING) this.progressIcons[testId] = "fa-spinner fa-spin-override fa-lg"
    else this.progressIcons[testId] = "fa-gear test-case-pending"
  }

  progressIcon(testCaseId: number) {
    return this.progressIcons[testCaseId]
  }

  private prepareNextTest(start: boolean) {
    let previousTestId: number|undefined
    if (this.currentTest == undefined) {
      this.currentTestIndex = -1
    } else {
      previousTestId = this.currentTest.id
    }
    if (this.currentTestIndex + 1 < this.testsToExecute.length) {
      this.currentTestIndex += 1
      this.currentTest = this.testsToExecute[this.currentTestIndex]
      this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.CONFIGURING)
      this.testCaseExpanded[this.currentTest.id] = true
      this.testCaseVisible[this.currentTest.id] = true
      this.stopped = false
      if (previousTestId != undefined) {
        this.testCaseExpanded[previousTestId] = false
        this.testCaseVisible[previousTestId] = this.showCompleted
      }
      this.getTestCaseDefinition(this.currentTest.id).pipe(
        catchError((error: Error) => {
          // Stop pending state of controls.
          if (this.currentTest) {
            this.currentTest.sessionId = ''
            this.stepsOfTests[this.currentTest.id] = []
            this.updateTestCaseStatus(this.currentTest.id, Constants.TEST_CASE_STATUS.STOPPED)
          }
          return throwError(() => error)
        })
      ).subscribe(() => {
        this.startAfterConfigurationComplete = start
        this.initiate(this.currentTest!.id)
      })
    } else {
      this.started = false
      this.reload = true
    }
  }

  configsDifferent(previous: SUTConfiguration[], current: SUTConfiguration[]) {
    const configStr1 = JSON.stringify(previous)
    const configStr2 = JSON.stringify(current)
    return !(configStr1 == configStr2)
  }

  private initiate(testCase: number): void {
    this.testPreparationStatus.status = Constants.STATUS.PENDING
    this.testService.initiate(testCase)
    .subscribe((data) => {
      this.session = data
      this.currentTest!.sessionId = this.session
      // Create WebSocket
      this.ws = this.webSocketService.connect(
        { next: () => { this.onOpen() } },
        { next: () => { this.onClose() } }
      )
      this.ws.subscribe({
        next: (msg) => this.onMessage(msg),
        error: (error) => this.onError(error),
        complete: () => this.onClose()
      })
      // Send the configuration request. We will be notified via WS when ready.
      this.testService.configure(this.specificationId!, this.session, this.systemId, this.actorId).subscribe(() => {})
    })
  }

  private configurationFailed() {
    this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.READY)
    let message: string
    if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
      message = "An error occurred during the test session's configuration. Details on the cause are included in the test session log."
    } else {
      message = "An error occurred during the test session's configuration. Please contact your community administrator to resolve this."
    }
    this.errorService.showSimpleErrorMessage("Configuration error", message)
  }

  private configurationReady(configs: SUTConfiguration[]) {
    // We're ready to start the test session
    this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.READY)
    this.simulatedConfigs = configs
    let configsDiffer = true
    if (this.currentSimulatedConfigs != undefined) {
      configsDiffer = this.simulatedConfigs == undefined || this.configsDifferent(this.currentSimulatedConfigs, this.simulatedConfigs)
    } else {
      this.currentSimulatedConfigs = this.simulatedConfigs
    }
    if (this.simulatedConfigs && this.simulatedConfigs.length > 0 && configsDiffer) {
      const modalRef = this.modalService.show(SimulatedConfigurationDisplayModalComponent, {
        initialState: {
          configurations: this.simulatedConfigs,
          actorInfo: this.actorInfoOfTests[this.currentTest!.id]
        }
      })
      if (modalRef.onHidden) {
        modalRef.onHidden.subscribe(() => {
          this.nextWaitingToStart = true
          this.runPreliminaryStep(false)
        })
      }
    } else {
      this.runPreliminaryStep(true)
    }
  }

  private runPreliminaryStep(doStartCheck: boolean) {
    let preliminaryCall: Observable<void>
    if (this.currentTest?.preliminary != undefined) {
      preliminaryCall = this.initiatePreliminary(this.session!)
    } else {
      preliminaryCall = of(void 0)
    }
    preliminaryCall.subscribe(() => {
      this.testPreparationStatus.status = Constants.STATUS.FINISHED
      if (doStartCheck) {
        if (this.startAfterConfigurationComplete) {
          this.start(this.session!)
        } else {
          this.nextWaitingToStart = !this.startAutomatically || !this.firstTestStarted
        }
      }
    })
  }

  onOpen() {
    // Register client
    this.ws!.next({
      command: Constants.WEB_SOCKET_COMMAND.REGISTER,
      sessionId: this.session!
    })
    // Keep alive heartbeat
    if (this.heartbeat == undefined) {
      this.heartbeat = timer(1, 5000).subscribe(() => {
        this.ws!.next({command: Constants.WEB_SOCKET_COMMAND.PING})
      })
    }
    if (this.messageProcessing == undefined) {
      this.messageProcessing = timer(1, 100).subscribe(() => {
        this.processNextMessage()
      })
    }
  }

  onError(msg: any) {
    if (msg != undefined) {
      console.error(JSON.stringify(msg))
    }
  }

  onClose() {
    this.ws = undefined
    this.closeWebSocket()
  }

  onMessage(response: WebSocketMessage) {
    const stepId = response.stepId
    if (stepId == Constants.LOG_EVENT_TEST_STEP) {
      // Process log messages immediately
      if (response.report?.context?.value != undefined) {
        const logMessage = response.report.context.value
        this.logMessages[this.currentTest!.id].push(logMessage)
        this.logMessageEventEmitters[this.currentTest!.id].emit(logMessage)
        if (this.currentTest!.id != this.testCaseWithOpenLogView) {
          const messageLevel = this.dataService.logMessageLevel(logMessage, LogLevel.DEBUG)
          if (messageLevel == LogLevel.ERROR) {
            this.unreadLogErrors[this.currentTest!.id] = true
          } else if (messageLevel == LogLevel.WARN) {
            this.unreadLogWarnings[this.currentTest!.id] = true
          } else {
            this.unreadLogMessages[this.currentTest!.id] = true
          }
        }
      }
    } else if (response.configs != undefined) {
      if (response.errorCode != undefined) {
        this.configurationFailed()
      } else {
        this.configurationReady(response.configs)
      }
    } else {
      if (this.messagesToProcess == undefined) {
        this.messagesToProcess = []
      }
      this.messagesToProcess.push(response)
    }
  }

  processNextMessage() {
    if (this.messagesToProcess != undefined && this.messagesToProcess.length > 0) {
      const msg = this.messagesToProcess.shift()
      this.processMessage(msg!)
    }
  }

  private recordInteraction(stepId: string, interactions: UserInteraction[], inputTitle: string|undefined, admin: boolean|undefined, desc: string|undefined) {
    if (this.interactionStepsOfTests[this.currentTest!.id] == undefined) {
      this.interactionStepsOfTests[this.currentTest!.id] = []
    }
    if (!find(this.interactionStepsOfTests[this.currentTest!.id], (interaction) => interaction.stepId == stepId)) {
      this.interactionStepsOfTests[this.currentTest!.id].push({
        stepId: stepId,
        interactions: interactions,
        inputTitle: inputTitle,
        admin: admin,
        desc: desc
      })
    }
  }

  private removeInteraction(stepId: string) {
    remove(this.interactionStepsOfTests[this.currentTest!.id], (step) => step.stepId == stepId)
  }

  labelForPendingInteraction(step: TestInteractionData, index: number) {
    if (step?.desc) {
      return step.desc
    } else {
      return "Interaction " + (index + 1)
    }
  }

  displayPendingInteraction(stepId?: string) {
    const testInteractions = this.interactionStepsOfTests[this.currentTest!.id]
    let interaction: TestInteractionData|undefined
    if (testInteractions) {
      if (stepId == undefined) {
        if (testInteractions.length > 0) {
          interaction = testInteractions[0]
        }
      } else {
        interaction = find(testInteractions, (step) => step.stepId == stepId)
      }
      if (interaction) {
        this.interact(interaction.interactions, interaction.inputTitle, interaction.stepId, interaction.admin, interaction.desc)
      }
    }
  }

  private handleInteractions(response: WebSocketMessage) {
    const isInteraction = response.interactions != undefined
    if (isInteraction) {
      // Prompt for an interaction.
      if (this.currentInteractionModal) {
        // We already have an interaction open - park the new one.
        if (!response.admin || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
          this.recordInteraction(response.stepId, response.interactions!, response.inputTitle, response.admin, response.desc)
        }
      } else {
        this.interact(response.interactions!, response.inputTitle, response.stepId, response.admin, response.desc)
      }
    } else {
      // Status update.
      if (response.status == Constants.TEST_STATUS.COMPLETED || response.status == Constants.TEST_STATUS.ERROR || response.status == Constants.TEST_STATUS.WARNING || response.status == Constants.TEST_STATUS.SKIPPED) {
        this.removeInteraction(response.stepId)
        if (this.currentInteractionModal && this.currentInteractionStepId == response.stepId) {
          this.popupService.closeAll()
          this.popupService.warning("The interaction step was completed by another user or process.", true)
          this.currentInteractionModal.hide()
          this.currentInteractionModal = undefined
          this.currentInteractionStepId = undefined
        }
      }
    }
  }

  processMessage(response: WebSocketMessage) {
    const stepId = response.stepId
    if (response.interactions != undefined) { // interactWithUsers
      this.handleInteractions(response)
    } else if (response.notify != undefined) {
      if (response.notify.simulatedConfigs != undefined) {
        this.simulatedConfigs = response.notify.simulatedConfigs
      }
    } else { // updateStatus
      if (stepId == Constants.END_OF_TEST_STEP || stepId == Constants.END_OF_TEST_STEP_EXTERNAL) {
        if (stepId == Constants.END_OF_TEST_STEP_EXTERNAL && !this.stopped && !this.allStopped) {
          // Stopped by other user or API call.
          this.popupService.closeAll()
          if (this.dataService.configuration.automationApiEnabled) {
            this.popupService.warning('The test session was terminated by another user or an external process.', true)
          } else {
            this.popupService.warning('The test session was terminated by another user.', true)
          }
        }
        this.started = false
        this.testCaseFinished(response.status, response?.outputMessage)
      } else {
        this.handleInteractions(response)
        if (response.stepHistory != undefined) {
          this.updateStepHistory(response.tcInstanceId, stepId, response.stepHistory)
        }
        const status = response.status
        const report = response.report
        const step = this.findNodeWithStepId(this.stepsOfTests[this.currentTest!.id], stepId)
        if (report != undefined) {
          report.tcInstanceId = response.tcInstanceId
        }
        this.updateStatus(step, stepId, status, report)
        if (stepId+'' == '0' && report?.result == "FAILURE") {
          // stepId is 0 for the preliminary step
          let msg = ''
          if (report?.reports?.assertionReports != undefined &&
                report.reports.assertionReports.length > 0 &&
                report.reports.assertionReports[0].value?.description != undefined) {
            msg = report.reports.assertionReports[0].value.description
          }
          this.errorService.showSimpleErrorMessage('Preliminary step error', msg)
        }
      }
    }
  }

  isParent(id: string, parentId: string) {
    const periodIndex = id.indexOf('.', parentId.length)
    const parenthesesIndex = id.indexOf('[', parentId.length)
    return id.indexOf(parentId) == 0 && (periodIndex == parentId.length || parenthesesIndex == parentId.length)
  }

  findChildrenByParentId(step: StepData, id: string): StepData[] {
    let foundSteps: StepData[] = []
    if (step != undefined) {
      if (step.id.startsWith(id)) {
        foundSteps.push(step)
      } else {
        if (step.type == 'loop') {
          if (step.steps != undefined) {
            for (let childStep of step.steps) {
              foundSteps = foundSteps.concat(this.findChildrenByParentId(childStep, id))
            }
          }
        } else if (step.type == 'group') {
          for (let childStep of step.steps) {
            foundSteps = foundSteps.concat(this.findChildrenByParentId(childStep, id))
          }
        } else if (step.type == 'decision') {
          if (step.then != undefined) {
            for (let childStep of step.then) {
              foundSteps = foundSteps.concat(this.findChildrenByParentId(childStep, id))
            }
          }
          if (step.else != undefined) {
            for (let childStep of step.else) {
              foundSteps = foundSteps.concat(this.findChildrenByParentId(childStep, id))
            }
          }
        } else if (step.type == 'flow') {
          if (step.threads != undefined) {
            for (let thread of step.threads) {
              for (let childStep of thread) {
                foundSteps = foundSteps.concat(this.findChildrenByParentId(childStep, id))
              }
            }
          }
        }
      }
    }
    return foundSteps
  }

  filterStep(step: StepData, id: string): StepData|undefined {
    if (step.id == id) {
      return step
    } else if (this.isParent(id, step.id)) {
      const parent = step
      let s = undefined
      if (parent.type == 'loop') {
        if (parent.sequences != undefined) {
          for (let sequence of parent.sequences) {
            if (sequence != undefined) {
              if (sequence.id == id) {
                s = sequence
              } else if (this.isParent(id, sequence.id)) {
                s = this.findNodeWithStepId(sequence.steps, id)
                if (s != undefined) {
                  break
                }
              }
            }
          }
        }
      } else if (parent.type == 'group') {
        s = this.findNodeWithStepId(parent.steps, id)
      } else if (parent.type == 'decision') {
        s = this.findNodeWithStepId(parent.then, id)
        if (s == undefined) {
          s = this.findNodeWithStepId(parent.else, id)
        }
      } else if (parent.type == 'flow') {
        for (let thread of parent.threads!) {
          s = this.findNodeWithStepId(thread, id)
          if (s != undefined) {
            break
          }
        }
      }
      if (s != undefined) {
        return s
      } else {
        return parent
      }
    } else {
      return undefined
    }
  }

  findNodeWithStepId(steps: StepData[]|undefined, id: string) {
    if (steps != undefined) {
      for (let step of steps) {
        if (step != undefined) {
          const parentOrCurrentNode = this.filterStep(step, id)
          if (parentOrCurrentNode != undefined) {
            return parentOrCurrentNode
          }
        }
      }
    }
    return undefined
  }

  setIds(steps: StepData[]|undefined, str: string, replacement: string) {
    if (steps != undefined) {
      for (let step of steps) {
        step.id = step.id.replace(str, replacement)
        if (step.type == 'loop') {
          this.setIds(step.steps, str, replacement)
          if (step.sequences != undefined) {
            this.setIds(step.sequences, str, replacement)
          }
        } else if (step.type == 'group') {
          this.setIds(step.steps, str, replacement)
        } else if (step.type == 'flow') {
          for (let thread of step.threads!) {
            this.setIds(thread, str, replacement)
          }
        } else if (step.type == 'decision') {
          this.setIds(step.then, str, replacement)
          this.setIds(step.else, str, replacement)
        }
      }
    }
  }

  clearStatusesAndReports(steps: StepData[]|undefined) {
    if (steps != undefined) {
      for (let step of steps) {
        delete step.status
        delete step.report
        if (step.type == 'loop') {
          this.clearStatusesAndReports(step.steps)
          this.clearStatusesAndReports(step.sequences)
        } else if (step.type == 'group') {
          this.clearStatusesAndReports(step.steps)
        } else if (step.type == 'decision') {
          this.clearStatusesAndReports(step.then)
          this.clearStatusesAndReports(step.else)
        } else if (step.type == 'flow') {
          for (let thread of step.threads!) {
            this.clearStatusesAndReports(thread)
          }
        }
      }
    }
  }

  updateStatus(step: StepData|undefined, stepId: string, status: number, report: StepReport) {
    if (step != undefined) {
      let current: StepData|undefined
      if (step.id != stepId) {
        current = step
        while (current != undefined && current.id != stepId) {
          if (current.type == 'loop' && status == Constants.TEST_STATUS.PROCESSING) {
            const copySteps = cloneDeep(current.steps)
            this.clearStatusesAndReports(copySteps)
            const index = Number(stepId.substring(((stepId.indexOf('[', current.id.length))+1), (stepId.indexOf(']', current.id.length))))
            const oldId = (current.id + '[1]')
            const newId = (current.id + '[' + index + ']')
            this.setIds(copySteps, oldId, newId)
            if (current.sequences == undefined || current.sequences[index - 1] == undefined) {
              const sequence: StepData = {
                  id: newId,
                  type: current.type,
                  steps: copySteps
              }
              if (current.sequences == undefined) {
                current.sequences = [sequence]
              } else {
                current.sequences.push(sequence)
              }
              this.testEvents[this.currentTest!.id].signalLoopSequenceUpdate({ stepId: current.id })
            }
            current = this.findNodeWithStepId(current.sequences[index - 1].steps, stepId)
          } else {
            break
          }
        }
      } else {
        current = step
      }
      if (current != undefined) {
        if (current.id == stepId && current.status != status) {
          if ((status == Constants.TEST_STATUS.COMPLETED) ||
          (status == Constants.TEST_STATUS.ERROR) ||
          (status == Constants.TEST_STATUS.WARNING) ||
          (status == Constants.TEST_STATUS.SKIPPED && (current.status != Constants.TEST_STATUS.COMPLETED && current.status != Constants.TEST_STATUS.ERROR && current.status != Constants.TEST_STATUS.WARNING)) ||
          (status == Constants.TEST_STATUS.WAITING && (this.started && current.status != Constants.TEST_STATUS.SKIPPED && current.status != Constants.TEST_STATUS.COMPLETED && current.status != Constants.TEST_STATUS.ERROR && current.status != Constants.TEST_STATUS.WARNING)) ||
          (status == Constants.TEST_STATUS.PROCESSING && (this.started && current.status != Constants.TEST_STATUS.WAITING && current.status != Constants.TEST_STATUS.SKIPPED && current.status != Constants.TEST_STATUS.COMPLETED && current.status != Constants.TEST_STATUS.ERROR && current.status != Constants.TEST_STATUS.WARNING))) {
            current.status = status
            current.report = report
            // If skipped, marked all children as skipped.
            if (status == Constants.TEST_STATUS.SKIPPED) {
              this.setChildrenAsSkipped(current, stepId, stepId)
            }
          }
        } else if (current.type == 'decision' && status == Constants.TEST_STATUS.SKIPPED) {
          // We do this to immediately mark as skipped decision branches that were not taken.
          if (current.id + '[T]' == stepId) {
            this.setChildrenSequenceAsSkipped(current.then, current.id)
          } else if (current.id + '[F]' == stepId) {
            this.setChildrenSequenceAsSkipped(current.else, current.id)
          }
        }
      }
    } else if ((stepId.endsWith('[T]') || stepId.endsWith('[F]')) && status == Constants.TEST_STATUS.SKIPPED) {
      // This scenario can come up if we have a decision step that is hidden but has visible children.
      // In this case we want to immediately illustrate skip decision branches.
      let childSteps: StepData[] = []
      for (let step of this.stepsOfTests[this.currentTest!.id]) {
        childSteps = childSteps.concat(this.findChildrenByParentId(step, stepId))
      }
      this.setChildrenSequenceAsSkipped(childSteps, stepId)
    }
  }

  setChildrenSequenceAsSkipped(sequence: StepData[]|undefined, parentStepId: string) {
    if (sequence != undefined) {
      for (let childStep of sequence) {
        if (Array.isArray(childStep)) {
          for (let childStepItem of childStep) {
            this.setChildrenAsSkipped(childStepItem, childStepItem.id, parentStepId)
          }
        } else {
          this.setChildrenAsSkipped(childStep, childStep.id, parentStepId)
        }
      }
    }
  }

  setChildrenAsSkipped(step: StepData|undefined, idToCheck: string|undefined, parentStepId: string) {
    const regex = new RegExp(this.escapeRegExp(parentStepId)+"(\\[.+\\])?\\.?", "g")
    if (step != undefined && idToCheck != undefined && (idToCheck == parentStepId || idToCheck.match(regex) != null)) {
      if (step.type == 'loop') {
        this.setChildrenSequenceAsSkipped(step.steps, idToCheck)
      } else if (step.type == 'group') {
        this.setChildrenSequenceAsSkipped(step.steps, idToCheck)
      } else if (step.type == 'decision') {
        this.setChildrenSequenceAsSkipped(step.then, idToCheck)
        this.setChildrenSequenceAsSkipped(step.else, idToCheck)
      } else if (step.type == 'flow') {
        for (let thread of step.threads!) {
          this.setChildrenSequenceAsSkipped(thread, idToCheck)
        }
      }
      step.status = Constants.TEST_STATUS.SKIPPED
    }
  }

  escapeRegExp(text: string) {
    return text.replace(/\./g, "\\.").replace(/\[/g, "\\[").replace(/\]/g, "\\]")
  }

  private interact(interactions: UserInteraction[], inputTitle: string|undefined, stepId: string, admin: boolean|undefined, desc: string|undefined) {
    if (!admin || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
      this.currentInteractionStepId = stepId
      this.currentInteractionModal = this.modalService.show(ProvideInputModalComponent, {
        class: 'modal-lg',
        initialState: {
          interactions: interactions,
          inputTitle: inputTitle,
          sessionId: this.session!
        }
      })
      this.currentInteractionModal.onHide?.subscribe((dismissReason) => {
        // Make sure that we treat ESC or backdrop click as a "minimise" click.
        if (dismissReason == "backdrop-click" || dismissReason == "esc") {
          this.recordInteraction(stepId, interactions, inputTitle, admin, desc)
        }
      })
      this.currentInteractionModal.content!.result.subscribe((result) => {
        this.currentInteractionStepId = undefined
        this.currentInteractionModal = undefined
        if (result == undefined) {
          this.recordInteraction(stepId, interactions, inputTitle, admin, desc)
        } else {
          this.testService.provideInput(this.session!, stepId, result, admin)
          .subscribe(() => {
            this.removeInteraction(stepId)
          })
        }
      }).add(() => {
        this.currentInteractionStepId = undefined
        this.currentInteractionModal = undefined
      })
    }
  }

  initiatePreliminary(session: string): Observable<void> {
    return this.testService.initiatePreliminary(session)
  }

  updateStepHistory(testSessionId: string, currentStepId: string, stepReports: {stepId: string, status: number, path: string|undefined}[]) {
    for (let stepReport of stepReports) {
      if (currentStepId != stepReport.stepId) {
        const step = this.findNodeWithStepId(this.stepsOfTests[this.currentTest!.id], stepReport.stepId)
        if (step && step.status != stepReport.status) {
          let reportToSet:Partial<StepReport>|undefined = undefined
          if (stepReport.path) {
            reportToSet = {
              tcInstanceId: testSessionId,
              path: stepReport.path
            }
          }
          this.updateStatus(step, stepReport.stepId, stepReport.status, reportToSet as StepReport)
        }
      }
    }
  }

  testCaseFinished(result?: number, outputMessage?: string) {
    if (result == Constants.TEST_STATUS.COMPLETED || result == Constants.TEST_STATUS.WARNING) {
      this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.COMPLETED)
    } else if (result == Constants.TEST_STATUS.ERROR) {
      this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.ERROR)
    } else {
      this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.STOPPED)
    }
    if (outputMessage != undefined) {
      this.testCaseOutput[this.currentTest!.id] = outputMessage
    }
    // Make sure steps still marked as pending or in progress are set as skipped.
    this.interactionStepsOfTests = {}
    if (this.currentInteractionModal) {
      this.currentInteractionModal.hide()
    }
    this.currentInteractionModal = undefined
    this.currentInteractionStepId = undefined
    this.setPendingStepsToSkipped()
    this.closeWebSocket()
    if (!this.allStopped && this.currentTestIndex + 1 < this.testsToExecute.length) {
      timer(1000).subscribe(() => {
        this.prepareNextTest(this.startAutomatically)
      })
    } else {
      this.allStopped = true
      this.reload = true
    }
  }

  setPendingStepsToSkipped() {
    for (let step of this.stepsOfTests[this.currentTest!.id]) {
      this.skipPendingSteps(step)
    }
  }

  skipPendingStepSequence(steps: StepData[]|undefined) {
    if (steps != undefined) {
      for (let step of steps) {
        this.skipPendingSteps(step)
      }
    }
  }

  skipPendingSteps(step: StepData) {
    if (step.status == undefined || step.status == Constants.TEST_STATUS.PROCESSING || step.status == Constants.TEST_STATUS.WAITING) {
      step.status = Constants.TEST_STATUS.SKIPPED
    }
    if (step.type == 'loop') {
      this.skipPendingStepSequence(step.steps)
      if (step.sequences) {
        for (let sequence of step.sequences) {
          this.skipPendingStepSequence(sequence.steps)
        }
      }
    } else if (step.type == 'group') {
      this.skipPendingStepSequence(step.steps)
    } else if (step.type == 'decision') {
      this.skipPendingStepSequence(step.then)
      this.skipPendingStepSequence(step.else)
    } else if (step.type == 'flow') {
      if (step.threads) {
        for (let thread of step.threads) {
          this.skipPendingStepSequence(thread)
        }
      }
    }
  }

  start(session: string) {
    this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.PROCESSING)
    this.started = true
    this.nextWaitingToStart = false
    this.firstTestStarted = true
    this.reportService.createTestReport(session, this.systemId, this.actorId, this.currentTest!.id)
    .subscribe(() => {
      this.testService.start(session).subscribe(() => {})
    })
  }

  stop(session: string, force?: boolean) {
    let signalStop = force == true
    if (this.started && !this.stopped) {
      this.stopped = true
      if (this.testsToExecute.length == 1) {
        this.allStopped = true
      }
      this.started = false
      signalStop = true
    }
    if (signalStop) {
      this.testService.stop(session).subscribe(() => {
        this.closeWebSocket()
        this.session = undefined
        this.testCaseFinished()
      })
    }
  }

  back() {
    if (this.communityId == undefined) {
      this.routingService.toOwnConformanceStatement(this.organisationId, this.systemId, this.actorId)
    } else {
      this.routingService.toConformanceStatement(this.organisationId, this.systemId, this.actorId, this.communityId)
    }
  }

  reinitialise() {
    if (!this.allStopped) {
      this.stopAll()
    }
    this.popupService.closeAll()
    if (this.heartbeat) {
      this.heartbeat.unsubscribe()
      this.heartbeat = undefined
    }
    if (this.messageProcessing) {
      this.messageProcessing.unsubscribe()
      this.messageProcessing = undefined
    }
    this.closeWebSocket()
    this.initialiseState()
    this.initialiseTestCases()
  }

  viewTestCase(testCase: ConformanceTestCase) {
    if (this.isTestCaseClickable(testCase)) {
      this.testCaseExpanded[testCase.id] = !this.testCaseExpanded[testCase.id]
    }
  }

  isTestCaseClickable(testCase: ConformanceTestCase) {
    return this.testsToExecute.length > 1 &&
      (this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.COMPLETED ||
       this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.ERROR ||
       this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.STOPPED)
  }

  showTestCaseDocumentation(testCaseId: number) {
    this.conformanceService.getTestCaseDocumentation(testCaseId)
    .subscribe((data) => {
      this.htmlService.showHtml("Test case documentation", data)
    })
  }

  // handleMissingConfigurationAction(action: MissingConfigurationAction) {
  //   if (action == MissingConfigurationAction.viewOrganisation) {
  //     if (this.dataService.isVendorUser || this.dataService.isVendorAdmin) {
  //       this.routingService.toOwnOrganisationDetails(undefined, true)
  //     } else {
  //       if (this.dataService.vendor!.id == this.organisationId) {
  //         this.routingService.toOwnOrganisationDetails(undefined, true)
  //       } else {
  //         this.organisationService.getOrganisationBySystemId(this.systemId)
  //         .subscribe((data) => {
  //           this.routingService.toOrganisationDetails(data.community, data.id, undefined, true)
  //         })
  //       }
  //     }
  //   } else if (action == MissingConfigurationAction.viewSystem) {
  //     if (this.dataService.isVendorUser || this.dataService.isVendorAdmin) {
  //       this.routingService.toOwnSystemDetails(this.systemId, true)
  //     } else {
  //       if (this.dataService.vendor!.id == this.organisationId) {
  //         this.routingService.toOwnSystemDetails(this.systemId, true)
  //       } else {
  //         this.routingService.toSystemDetails(this.communityId!, this.organisationId, this.systemId, true)
  //       }
  //     }
  //   } else { // viewStatement
  //     if (this.communityId == undefined) {
  //       this.routingService.toOwnConformanceStatement(this.organisationId, this.systemId, this.actorId, undefined, undefined, ConformanceStatementTab.configuration)
  //     } else {
  //       this.routingService.toConformanceStatement(this.organisationId, this.systemId, this.actorId, this.communityId, undefined, undefined, ConformanceStatementTab.configuration)
  //     }
  //   }
  // }

  viewLog(test: ConformanceTestCase) {
    this.testCaseWithOpenLogView = test.id
    this.unreadLogMessages[test.id] = false
    this.unreadLogErrors[test.id] = false
    this.unreadLogWarnings[test.id] = false
    const modalRef = this.modalService.show(SessionLogModalComponent, {
      class: 'modal-lg',
      initialState: {
        messages: this.logMessages[test.id].slice(), // Use slice to make a copy of the log messages.
        messageEmitter: this.logMessageEventEmitters[test.id]
      }
    })
    modalRef.onHide?.subscribe(() => {
      this.testCaseWithOpenLogView = undefined
    })
  }

  alertTypeForStatus(status: number) {
    let resultType: string
    if (status == Constants.TEST_CASE_STATUS.COMPLETED) {
      resultType = Constants.TEST_CASE_RESULT.SUCCESS
    } else if (status == Constants.TEST_CASE_STATUS.ERROR) {
      resultType = Constants.TEST_CASE_RESULT.FAILURE
    } else {
      resultType = Constants.TEST_CASE_RESULT.UNDEFINED
    }
    return this.dataService.determineOutputMessageType(resultType)
  }

  leavingTestExecutionPage() {
    this.popupService.closeAll()
    this.dataService.clearTestsToExecute()
    if (this.firstTestStarted && !this.allStopped) {
      this.closeWebSocket()
      const pendingTests = filter(this.testsToExecute, (test) => {
        return this.testCaseStatus[test.id] == Constants.TEST_CASE_STATUS.READY || this.testCaseStatus[test.id] == Constants.TEST_CASE_STATUS.PENDING || this.testCaseStatus[test.id] == Constants.TEST_CASE_STATUS.CONFIGURING
      })
      const pendingTestIds = lmap(pendingTests, (test) => { return test.id } )
      if (pendingTestIds.length > 0) {
        this.testService.startHeadlessTestSessions(pendingTestIds, this.specificationId!, this.systemId, this.actorId, false).subscribe(() => {})
        this.popupService.success('Continuing execution in background. Check <b>Test Sessions</b> for progress.')
      } else {
        if (this.testCaseStatus[this.currentTest!.id] == Constants.TEST_CASE_STATUS.PROCESSING) {
          this.popupService.success('Continuing execution in background. Check <b>Test Sessions</b> for progress.')
        }
      }
    } else {
      if (this.ws != undefined && this.session != undefined) {
        this.stopAll()
      }
    }
    if (this.messageProcessing) this.messageProcessing.unsubscribe()
    if (this.heartbeat) this.heartbeat.unsubscribe()
  }

  private closeWebSocket() {
    if (this.heartbeat) {
      this.heartbeat.unsubscribe()
      this.heartbeat = undefined
    }
    if (this.ws) {
      this.ws.complete()
      this.ws = undefined
    }
  }

  private isVisible(testCase: ConformanceTestCase) {
    return (this.currentTest?.id == testCase.id) ||
      (this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.CONFIGURING) ||
      (this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.READY) ||
      (this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.PROCESSING) ||
      (this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.PENDING && this.showPending) ||
      (this.testCaseStatus[testCase.id] != Constants.TEST_CASE_STATUS.PENDING && this.showCompleted)
  }

  testOptionsUpdated(choices: CheckboxOptionState) {
    const oldShowCompleted = this.showCompleted
    const oldShowPending = this.showPending
    this.showCompleted = choices[TestExecutionComponent.SHOW_COMPLETED]
    this.showPending = choices[TestExecutionComponent.SHOW_PENDING]
    this.startAutomatically = choices[TestExecutionComponent.CONTINUE_AUTOMATICALLY]
    if (oldShowCompleted != this.showCompleted || oldShowPending != this.showPending) {
      this.updateTestCaseVisibility()
    }
  }

  updateTestCaseVisibility() {
    for (let test of this.testsToExecute) {
      if (this.isVisible(test)) {
        this.testCaseVisible[test.id] = true
      } else {
        this.testCaseVisible[test.id] = false
        this.testCaseExpanded[test.id] = false
      }
    }
  }

  exportEnabled(testCase: ConformanceTestCase) {
    return this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.ERROR
      || this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.COMPLETED
      || this.testCaseStatus[testCase.id] == Constants.TEST_CASE_STATUS.STOPPED
  }

  exportPdf(testCase: ConformanceTestCase) {
    this.exportPdfPending[testCase.id] = true
    this.onExportTestCase(testCase, 'application/pdf', 'test_case_report.pdf')
    .subscribe(() => {}).add(() => {
      this.exportPdfPending[testCase.id] = false
    })
  }

  exportXml(testCase: ConformanceTestCase) {
    this.exportXmlPending[testCase.id] = true
    this.onExportTestCase(testCase, 'application/xml', 'test_case_report.xml')
    .subscribe(() => {}).add(() => {
      this.exportXmlPending[testCase.id] = false
    })
  }

	private onExportTestCase(testCase: Partial<ConformanceTestCase>, contentType: string, fileName: string) {
    return this.reportService.exportTestCaseReport(testCase.sessionId!, testCase.id!, contentType)
    .pipe(
      mergeMap((data) => {
        const blobData = new Blob([data], {type: contentType});
        saveAs(blobData, fileName);
        return of(data)
      }),
      share()
    )
  }

}
