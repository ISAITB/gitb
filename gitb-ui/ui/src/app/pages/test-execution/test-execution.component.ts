import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { forkJoin, Observable, timer, of, Subscription } from 'rxjs';
import { map, mergeMap, share } from 'rxjs/operators';
import { WebSocketSubject } from 'rxjs/webSocket';
import { Constants } from 'src/app/common/constants';
import { ActorInfo } from 'src/app/components/diagram/actor-info';
import { StepReport } from 'src/app/components/diagram/report/step-report';
import { StepData } from 'src/app/components/diagram/step-data';
import { ProvideInputModalComponent } from 'src/app/modals/provide-input-modal/provide-input-modal.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { HtmlService } from 'src/app/services/html.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { SystemService } from 'src/app/services/system.service';
import { TestService } from 'src/app/services/test.service';
import { WebSocketService } from 'src/app/services/web-socket.service';
import { OrganisationParameterWithValue } from 'src/app/types/organisation-parameter-with-value';
import { Organisation } from 'src/app/types/organisation.type';
import { SUTConfiguration } from 'src/app/types/sutconfiguration';
import { SystemConfigurationEndpoint } from 'src/app/types/system-configuration-endpoint';
import { SystemConfigurationParameter } from 'src/app/types/system-configuration-parameter';
import { SystemParameterWithValue } from 'src/app/types/system-parameter-with-value';
import { WebSocketMessage } from 'src/app/types/web-socket-message';
import { ConformanceTestCase } from '../organisation/conformance-statement/conformance-test-case';
import { cloneDeep, filter, map as lmap } from 'lodash'
import { Configuration } from 'src/app/types/configuration';
import { CodeEditorModalComponent } from 'src/app/components/code-editor-modal/code-editor-modal.component';
import { DiagramEvents } from 'src/app/components/diagram/diagram-events';
import { UserInteraction } from 'src/app/types/user-interaction';
import { UserInteractionInput } from 'src/app/types/user-interaction-input';

@Component({
  selector: 'app-test-execution',
  templateUrl: './test-execution.component.html',
  styles: [
  ]
})
export class TestExecutionComponent implements OnInit, OnDestroy {

  testsToExecute: ConformanceTestCase[] = []
  actorId!: number
  systemId!: number
  specificationId!: number
  isAdmin = false
  documentationExists = false

  started = false
  stopped = false
  firstTestStarted = false
  reload = false
  startAutomatically = false
  wizardStep = 0
  currentTestIndex = 0
  currentTest?: ConformanceTestCase
  visibleTest?: ConformanceTestCase
  intervalSet = false
  progressIcons: {[key: number]: string} = {}
  testCaseStatus: {[key: number]: number} = {}
  testCaseOutput: {[key: number]: string} = {}
  stepsOfTests: {[key: string]: StepData[]} = {}
  actorInfoOfTests: {[key: string]: ActorInfo[]} = {}
  logMessages: {[key: string]: string[]} = {}
  organisationProperties?: OrganisationParameterWithValue[]
  systemProperties?: SystemParameterWithValue[]
  endpointRepresentations?: SystemConfigurationEndpoint[]
  actor?: string
  session?: string
  configurationValid = false
  systemConfigurationValid = false
  organisationConfigurationValid = false
  showOrganisationProperties = false
  showSystemProperties = false
  showStatementProperties = false
  somethingIsVisible = false
  requiredPropertiesAreHidden = false
  simulatedConfigs?: SUTConfiguration[]
  currentSimulatedConfigs?: SUTConfiguration[]
  messagesToProcess?: WebSocketMessage[]
  testEvents: {[key: number]: DiagramEvents} = {}

  private ws?: WebSocketSubject<any>
  private heartbeat?: Subscription
  private messageProcessing?: Subscription

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private modalService: BsModalService,
    private testService: TestService,
    private systemService: SystemService,
    private conformanceService: ConformanceService,
    private reportService: ReportService,
    public dataService: DataService,
    private organisationService: OrganisationService,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private webSocketService: WebSocketService,
    private errorService: ErrorService
  ) { }

  private queryParamToNumber(paramName: string): number|undefined {
    const value = this.route.snapshot.queryParamMap.get(paramName)
    if (value == undefined) {
      return undefined
    } else {
      return Number(value)
    }
  }

  ngOnInit(): void {
    this.actorId = Number(this.route.snapshot.paramMap.get('actor_id'))
    this.systemId = Number(this.route.snapshot.paramMap.get('system_id'))
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
    this.isAdmin = this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin
    this.documentationExists = this.testCasesHaveDocumentation()
    if (this.dataService.tests != undefined) {
      this.testsToExecute = this.dataService.tests
      this.initialiseEvents()
      this.initialiseState()
    } else {
      // We lost our state following a refresh - recreate state.
      const  testSuiteId = this.queryParamToNumber('ts')
      if (testSuiteId != undefined) {
        this.conformanceService.getConformanceStatusForTestSuite(this.actorId, this.systemId, testSuiteId)
        .subscribe((data) => {
          // There will always be one test suite returned.
          const tests: ConformanceTestCase[] = []
          for (let result of data) {
            tests.push({
              id: result.testCaseId!,
              sname: result.testCaseName,
              description: result.testCaseDescription,
              hasDocumentation: result.testCaseHasDocumentation,
              result: Constants.TEST_CASE_RESULT.UNDEFINED
            })
          }
          this.testsToExecute = tests
          this.initialiseEvents()
          this.initialiseState()
        })
      } else {
        const testCaseId = this.queryParamToNumber('tc')
        this.conformanceService.getTestSuiteTestCase(testCaseId!)
        .subscribe((data) => {
          // There will always be one test case returned.
          this.testsToExecute = [{
            id: data.id,
            sname: data.sname,
            description: data.description,
            hasDocumentation: data.hasDocumentation!,
            result: Constants.TEST_CASE_RESULT.UNDEFINED
          }]
          this.initialiseEvents()
          this.initialiseState()
        })
      }
    }
  }

  initialiseEvents() {
    for (let test of this.testsToExecute) {
      this.testEvents[test.id] = new DiagramEvents()
    }
  }

  ngOnDestroy(): void {
    this.leavingTestExecutionPage()
  }

  initialiseState() {
    this.currentTestIndex = 0
    this.currentTest = this.testsToExecute[this.currentTestIndex]
    this.visibleTest = this.currentTest
    // Set all flags, indexes, counters to their original state.
    this.started = false
    this.stopped = false
    this.firstTestStarted = false
    this.reload = false
    this.startAutomatically = false
    this.wizardStep = 0
    this.intervalSet = false
    this.progressIcons = {}
    this.testCaseStatus = {}
    this.testCaseOutput = {}
    this.stepsOfTests = {}
    this.actorInfoOfTests = {}
    this.logMessages = {}
    this.organisationProperties = undefined
    this.systemProperties = undefined
    this.endpointRepresentations = undefined
    this.actor = undefined
    this.session = undefined
    this.configurationValid = false
    this.systemConfigurationValid = false
    this.organisationConfigurationValid = false
    this.showOrganisationProperties = false
    this.showSystemProperties = false
    this.showStatementProperties = false
    this.somethingIsVisible = false
    this.requiredPropertiesAreHidden = false
    this.simulatedConfigs = undefined
    this.currentSimulatedConfigs = undefined
    this.messagesToProcess = undefined
    // Start initialisation
    for (let test of this.testsToExecute) {
      this.updateTestCaseStatus(test.id, Constants.TEST_CASE_STATUS.PENDING)
      this.stepsOfTests[test.id] = []
      this.actorInfoOfTests[test.id] = []
    }
    const checkConfigurations = this.checkConfigurations()
    const testCaseLoaded = this.getTestCaseDefinition(this.currentTest.id)
    forkJoin([checkConfigurations, testCaseLoaded]).subscribe(() => {
      this.nextStep()
    })
  }

  getOrganisation(): Organisation {
    let organisation = this.dataService.vendor
    if (this.isAdmin) {
      organisation = JSON.parse(localStorage[Constants.LOCAL_DATA.ORGANISATION])
    }
    return organisation!
  }

  checkConfigurations() {
    const organisationParameterCheck = this.organisationService.checkOrganisationParameterValues(this.getOrganisation().id)
    const systemParameterCheck = this.systemService.checkSystemParameterValues(this.systemId)
    const statementParameterCheck = this.conformanceService.checkConfigurations(this.actorId, this.systemId)
    return forkJoin([organisationParameterCheck, systemParameterCheck, statementParameterCheck]).pipe(
      map((data) => {
        this.organisationProperties = data[0]
        this.systemProperties = data[1]
        this.endpointRepresentations = data[2]
      }, share())
    )    
  }

  getTestCaseDefinition(testCaseToLookup: number): Observable<void> {
    return this.testService.getTestCaseDefinition(testCaseToLookup).pipe(
      mergeMap((testCase) => {
        if (testCase.preliminary != undefined) {
          this.currentTest!.preliminary = testCase.preliminary
        }
        return this.testService.getActorDefinitions(this.specificationId).pipe(
          map((data) => {
            let tempActors = testCase.actors.actor
            for (let domainActorData of data) {
              if (domainActorData.id == this.actorId) {
                this.actor = domainActorData.actorId
              }
              for (let testCaseActorData of tempActors) {
                if (testCaseActorData.id == domainActorData.actorId) {
                  if (testCaseActorData.name == undefined) {
                    testCaseActorData.name = domainActorData.name
                  }
                  if (testCaseActorData.displayOrder == undefined && domainActorData.displayOrder != undefined) {
                    testCaseActorData.displayOrder = domainActorData.displayOrder
                  }
                  break
                }
              }
            }
            tempActors = tempActors.sort((a, b) => {
              if (a.displayOrder == undefined && b.displayOrder == undefined) return 0
              else if (a.displayOrder != undefined && b.displayOrder == undefined) return -1
              else if (a.displayOrder == undefined && b.displayOrder != undefined) return 1
              else return Number(a.displayOrder) - Number(b.displayOrder)
            })
            this.actorInfoOfTests[testCaseToLookup] = tempActors as ActorInfo[]
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
    this.stopped = true
    if (this.session != undefined) {
      this.stop(this.session)
    }
    this.started = false 
    this.startAutomatically = false
    this.reload = true  
  }

  updateTestCaseStatus(testId: number, status: number) {
    this.testCaseStatus[testId] = status
    if (status == Constants.TEST_CASE_STATUS.PROCESSING) this.progressIcons[testId] = "fa-gear fa-spin test-case-running"
    else if (status == Constants.TEST_CASE_STATUS.READY) this.progressIcons[testId] = "fa-gear test-case-ready"
    else if (status == Constants.TEST_CASE_STATUS.PENDING) this.progressIcons[testId] = "fa-gear test-case-pending"
    else if (status == Constants.TEST_CASE_STATUS.ERROR) this.progressIcons[testId] = "fa-times-circle test-case-error"
    else if (status == Constants.TEST_CASE_STATUS.COMPLETED) this.progressIcons[testId] = "fa-check-circle test-case-success"
    else if (status == Constants.TEST_CASE_STATUS.STOPPED) this.progressIcons[testId] = "fa-gear test-case-stopped"
    else this.progressIcons[testId] = "fa-gear test-case-pending"  
  }

  progressIcon(testCaseId: number) {
    return this.progressIcons[testCaseId]
  }

  nextStep(stepToConsider?: number) {
    if (!this.stopped) {
      if (stepToConsider == undefined)
        stepToConsider = this.wizardStep
      if (stepToConsider == 0) {
        // Before starting
        this.configurationValid = this.dataService.isConfigurationValid(this.endpointRepresentations!)
        this.systemConfigurationValid = this.dataService.isMemberConfigurationValid(this.systemProperties!)
        this.organisationConfigurationValid = this.dataService.isMemberConfigurationValid(this.organisationProperties!)
        if (!this.configurationValid || !this.systemConfigurationValid || !this.organisationConfigurationValid) {
          // Missing configuration.
          let statementProperties: SystemConfigurationParameter[] = []
          if (this.endpointRepresentations != undefined && this.endpointRepresentations.length > 0) {
            statementProperties = this.endpointRepresentations[0].parameters
          }
          const organisationPropertyVisibility = this.dataService.checkPropertyVisibility(this.organisationProperties!)
          const systemPropertyVisibility = this.dataService.checkPropertyVisibility(this.systemProperties!)
          const statementPropertyVisibility = this.dataService.checkPropertyVisibility(statementProperties)
          this.showOrganisationProperties = organisationPropertyVisibility.hasVisibleMissingRequiredProperties || organisationPropertyVisibility.hasVisibleMissingOptionalProperties
          this.showSystemProperties = systemPropertyVisibility.hasVisibleMissingRequiredProperties || systemPropertyVisibility.hasVisibleMissingOptionalProperties
          this.showStatementProperties = statementPropertyVisibility.hasVisibleMissingRequiredProperties || statementPropertyVisibility.hasVisibleMissingOptionalProperties
          this.somethingIsVisible = this.showOrganisationProperties || this.showSystemProperties || this.showStatementProperties
          this.requiredPropertiesAreHidden = organisationPropertyVisibility.hasNonVisibleMissingRequiredProperties || systemPropertyVisibility.hasNonVisibleMissingRequiredProperties || statementPropertyVisibility.hasNonVisibleMissingRequiredProperties
          this.wizardStep = 1
        } else {
          this.runInitiateStep()
        }
      } else if (stepToConsider == 1) {
        this.runInitiateStep()
      } else if (stepToConsider == 2) {
        this.wizardStep = 3
        if (this.startAutomatically) {
          this.start(this.session!)
        }
      } else if (this.wizardStep == 3) {
        // We are running the next test case
        this.startNextTest()
      }
    }
  }
 
  startNextTest() {
    if (this.currentTestIndex + 1 < this.testsToExecute.length) {
      this.currentTestIndex += 1
      this.currentTest = this.testsToExecute[this.currentTestIndex]
      this.visibleTest = this.currentTest
      this.startAutomatically = true
      this.getTestCaseDefinition(this.currentTest.id)
      .subscribe(() => {
        this.nextStep(1)
      })
    } else {
      this.startAutomatically = false
      this.started = false 
      this.reload = true
    }
  }
  
  runInitiateStep() {
    this.updateTestCaseStatus(this.currentTest!.id, Constants.TEST_CASE_STATUS.READY)
    this.initiate(this.currentTest!.id).subscribe(() => {
      let configsDiffer = true
      if (this.currentSimulatedConfigs != undefined) {
        configsDiffer = this.configsDifferent(this.currentSimulatedConfigs, this.simulatedConfigs)        
      } else {
        this.currentSimulatedConfigs = this.simulatedConfigs
      }
      if (this.simulatedConfigs && this.simulatedConfigs.length > 0 && configsDiffer) {
        this.wizardStep = 2
      } else {
        this.wizardStep = 3
        if (this.startAutomatically) {
          this.start(this.session!)
        }
      }
    })
  }

  configsDifferent(previous: any, current: any) {
    for (let c1 of previous) {
      delete c1["$$hashKey"]
      for (let c2 of c1.configs) {
        delete c2["$$hashKey"]
        for (let c3 of c2.config) {
          delete c3["$$hashKey"]
        }
      }
    }
    const configStr1 = JSON.stringify(previous)
    const configStr2 = JSON.stringify(current)
    return !(configStr1 == configStr2)
  }

  initiate(testCase: number): Observable<void> {
    return this.testService.initiate(testCase).pipe(
      mergeMap((data) => {
        this.session = data
        this.currentTest!.sessionId = this.session
        // Create WebSocket
        this.ws = this.webSocketService.connect(
          { next: () => { this.onOpen() } },
          { next: () => { this.onClose() } }
        )
        this.ws.subscribe(
          msg => this.onMessage(msg),
          err => this.onError(err),
          () => this.onClose()
        )
        return this.configure(this.session)
      })
    )
  }

  onOpen() {
    // Register client
    this.ws!.next({
      command: Constants.WEB_SOCKET_COMMAND.REGISTER,
      sessionId: this.session!,
      actorId: this.actor!
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
      if (this.currentTest?.id != undefined) {
        if (this.logMessages[this.currentTest.id] == undefined) {
          this.logMessages[this.currentTest.id] = []
        }
      }
      if (response.report?.context?.value != undefined) {
        this.logMessages[this.currentTest!.id].push(response.report.context.value)
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

  processMessage(response: WebSocketMessage) {
    const stepId = response.stepId
    if (response.interactions != undefined) { // interactWithUsers
      this.interact(response.interactions, response.inputTitle, stepId)
    } else if (response.notify != undefined) {
      if (response.notify.simulatedConfigs != undefined) {
        this.simulatedConfigs = response.notify.simulatedConfigs
      }
    } else { // updateStatus
      if (stepId == Constants.END_OF_TEST_STEP) {
        this.started = false
        this.testCaseFinished(response.status, response?.outputMessage)
      } else {
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
      if (current != undefined && current.id == stepId && current.status != status) {
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
      }
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

  interact(interactions: UserInteraction[], inputTitle: string|undefined, stepId: string) {
    const modalRef = this.modalService.show(ProvideInputModalComponent, {
      backdrop: 'static',
      keyboard: false,
      initialState: {
        interactions: interactions,
        inputTitle: inputTitle
      }
    })
    modalRef.content!.result.subscribe((result: UserInteractionInput[]) => {
      this.testService.provideInput(this.session!, stepId, result).subscribe(() => {
        // Do nothing.
      })
    })
  }

  configure(session: string): Observable<void> {
    return this.testService.configure(this.specificationId, session, this.systemId, this.actorId).pipe(
      mergeMap((data) => {
        this.simulatedConfigs = data.configs
        if (this.currentTest?.preliminary != undefined) {
          this.startAutomatically = false
          return this.initiatePreliminary(this.session!)
        } else {
          return of(void 0)
        }
      })
    )
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
    if (result == Constants.TEST_STATUS.COMPLETED) {
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
    this.setPendingStepsToSkipped()
    this.closeWebSocket()
    if (this.currentTestIndex + 1 < this.testsToExecute.length) {
      timer(1000).subscribe(() => {
        this.nextStep()
      })
    } else {
      this.nextStep()
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
    this.firstTestStarted = true
    this.startAutomatically = true
    this.reportService.createTestReport(session, this.systemId, this.actorId, this.currentTest!.id)
    .subscribe(() => {
      this.testService.start(session).subscribe(() => {})
    })
  }

  stop(session: string) {
    this.started = false
    this.testService.stop(session).subscribe(() => {
      this.closeWebSocket()      
      this.session = undefined
      this.testCaseFinished()
    })
  }

  back() {
    this.router.navigate(['organisation', 'systems', this.systemId, 'conformance', 'detail', this.actorId, this.specificationId])
  }

  reinitialise() {
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
  }

  viewTestCase(testCase: ConformanceTestCase) {
    if (this.isTestCaseClickable(testCase)) {
      this.visibleTest = testCase
    }
  }

  isTestCaseClickable(testCase: ConformanceTestCase) {
    return testCase.id != this.visibleTest?.id && this.testCaseStatus[testCase.id] != Constants.TEST_CASE_STATUS.PENDING
  }

  tableRowClass(testCase: ConformanceTestCase) {
    if (this.isTestCaseClickable(testCase)) return "clickable-row"
    else if (testCase.id == this.visibleTest?.id && this.testsToExecute.length > 1) return "selected-row"
    else return ""
  }

  showTestCaseDocumentation(testCaseId: number) {
    this.conformanceService.getTestCaseDocumentation(testCaseId)
    .subscribe((data) => {
      this.htmlService.showHtml("Test case documentation", data)
    })
  }

  toOrganisationProperties() {
    if (this.dataService.isVendorUser || this.dataService.isVendorAdmin) {
      this.router.navigate(['settings', 'organisation'], { queryParams: { 'viewProperties': true } })
    } else {
      const organisation = this.getOrganisation()
      if (this.dataService.vendor!.id == organisation.id) {
        this.router.navigate(['settings', 'organisation'], { queryParams: { 'viewProperties': true } })
      } else {
        this.organisationService.getOrganisationBySystemId(this.systemId)
        .subscribe((data) => {
          this.router.navigate(['admin', 'users', 'community', data.community, 'organisation', data.id], { queryParams: { 'viewProperties': true } })
        })
      }
    }
  }

  toSystemProperties() {
    if (this.dataService.isVendorUser) {
      this.router.navigate(['organisation', 'systems', this.systemId, 'info'], { queryParams: { 'viewProperties': true } })
    } else {
      this.router.navigate(['organisation', 'systems'], { queryParams: { 'id': this.systemId, 'viewProperties': true } })
    }
  }

  toConfigurationProperties() {
    this.router.navigate(['organisation', 'systems', this.systemId, 'conformance', 'detail', this.actorId, this.specificationId], { queryParams: { 'viewProperties': true } })
  }

  getActorName(actorId: string) {
    let actorName = actorId
    for (let info of this.actorInfoOfTests[this.currentTest!.id]) {
      if (actorId == info.id) {
        if (info.name != undefined) {
          actorName = info.name
        } else {
          actorName = info.id
        }
        break
      }
    }
    return actorName
  }

  download(binaryParameter: Configuration) {
    const mimeType = this.dataService.mimeTypeFromDataURL(binaryParameter.value!)
    const extension = this.dataService.extensionFromMimeType(mimeType)
    let name = binaryParameter.name
    if (extension) {
      name += '.' + extension
    }
    const blob = this.dataService.b64toBlob(this.dataService.base64FromDataURL(binaryParameter.value!))
    saveAs(blob, name)
  }

  viewLog() {
    let value: string
    if (this.logMessages[this.visibleTest!.id] != undefined) {
      value = this.logMessages[this.visibleTest!.id].join('')
    } else {
      value = ''
    }
    this.modalService.show(CodeEditorModalComponent, {
      class: 'modal-lg',
      initialState: {
        documentName: 'Test session log',
        editorOptions: {
          value: value,
          readOnly: true,
          lineNumbers: true,
          smartIndent: false,
          electricChars: false,
          mode: 'text/plain',
          download: {
            fileName: 'log.txt',
            mimeType: 'text/plain'
          }
        }
      }
    })
  }

  alertTypeForStatus(status: number) {
    if (status == Constants.TEST_CASE_STATUS.COMPLETED) return 'success'
    else return 'danger'
  }

  leavingTestExecutionPage() {
    if (this.firstTestStarted && !this.stopped) {
      this.closeWebSocket()
      const pendingTests = filter(this.testsToExecute, (test) => {
        return this.testCaseStatus[test.id] == Constants.TEST_CASE_STATUS.READY || this.testCaseStatus[test.id] == Constants.TEST_CASE_STATUS.PENDING
      })
      const pendingTestIds = lmap(pendingTests, (test) => { return test.id } )
      if (pendingTestIds.length > 0) {
        this.testService.startHeadlessTestSessions(pendingTestIds, this.specificationId, this.systemId, this.actorId).subscribe(() => {})
        this.popupService.success('Continuing test sessions in background. Check <b>Test Sessions</b> for progress.')
      } else {
        if (this.testCaseStatus[this.currentTest!.id] == Constants.TEST_CASE_STATUS.PROCESSING) {
          this.popupService.success('Continuing test session in background. Check <b>Test Sessions</b> for progress.')
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

}
