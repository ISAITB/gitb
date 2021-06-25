import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { forkJoin } from 'rxjs';
import { map, share } from 'rxjs/operators';
import { Constants } from 'src/app/common/constants';
import { ReportService } from 'src/app/services/report.service';
import { TestStepResult } from 'src/app/types/test-step-result';
import { ActorInfo } from '../actor-info';
import { DiagramEvents } from '../diagram-events';
import { StepData } from '../step-data';
import { SessionData } from './session-data';

@Component({
  selector: 'app-test-session-presentation',
  templateUrl: './test-session-presentation.component.html'
})
export class TestSessionPresentationComponent implements OnInit {

  @Input() sessionId!: string
  @Input() sessionObject!: SessionData
  @Output() ready = new EventEmitter<SessionData>()

  stepsOfTests: {[key: string]: StepData[]} = {}
  test?: string
  actorInfoOfTests?: {[key: string]: ActorInfo[]}
  outputMessage?: string
  outputMessageType?: string
  result?: string
  events = new DiagramEvents()

  testResultFlat:{[key: string]: TestStepResult} = {}

  constructor(
    private reportService: ReportService
  ) { }

  ngOnInit(): void {
    this.loadTestSessionData()
  }

  loadTestSessionData() {
    // Load test step results
    const obs1 = this.reportService.getTestStepResults(this.sessionId)
    .pipe(
      map((stepResults) => {
        for (let result of stepResults) {
          this.testResultFlat[result.stepId] = result
        }
      }),
      share() // We return a shared observable here otherwise the requests are repeated
    )
    // Load test result details
    const obs2 = this.reportService.getTestResultOfSession(this.sessionId)
    .pipe(
      map((result) => {
        let testcase = JSON.parse(result.tpl!)
        let actorInfoOfTests: {[key: string]: ActorInfo[]} = {}
        let stepsOfTests: {[key: string]: StepData[]} = {}
        let actors: ActorInfo[] = []
        for (let actor of testcase.actors.actor) {
          actors.push({
            id: actor.id,
            name: actor.name,
            role: actor.role
          })
        }
        actorInfoOfTests[this.sessionId] = actors
        stepsOfTests[this.sessionId] = testcase.steps
        this.outputMessage = result.outputMessage
        if (result.result == Constants.TEST_CASE_RESULT.SUCCESS) {
          this.outputMessageType = 'success'
        } else if (result.result == Constants.TEST_CASE_RESULT.FAILURE) {
          this.outputMessageType = 'danger'
        } else {
          this.outputMessageType = 'info'
        }
        this.actorInfoOfTests = actorInfoOfTests
        this.stepsOfTests = stepsOfTests
      }),
      share() // We return a shared observable here otherwise the requests are repeated
    )
    // Process once both requests are complete
    forkJoin([obs1, obs2]).subscribe(() => {
      let parentStatus = Constants.TEST_STATUS.WAITING
      if (this.sessionObject?.endTime) {
        if (this.sessionObject?.result != undefined) {
          if (this.sessionObject.result == 'SUCCESS') {
            parentStatus = Constants.TEST_STATUS.COMPLETED
          } else if (this.sessionObject.result == 'FAILURE') {
            parentStatus = Constants.TEST_STATUS.ERROR
          } else if (this.sessionObject.result == 'UNDEFINED') {
            parentStatus = Constants.TEST_STATUS.SKIPPED
          }
        } else {
          parentStatus = Constants.TEST_STATUS.ERROR
        }
      }
      this.addStatusToSteps(this.stepsOfTests[this.sessionId], parentStatus)
      this.ready.emit(this.sessionObject)
    })
  }

  addStatusToSteps(steps: StepData[]|undefined, parentStatus?: number) {
    if (steps != undefined) {
      for (let step of steps) {
        let statusToSet = undefined
        if (this.testResultFlat[step.id] != undefined) {
          statusToSet = this.testResultFlat[step.id].result
          if (this.testResultFlat[step.id].path !== undefined) {
            step.report = {}
            step.report.tcInstanceId = this.sessionId
            step.report.path = this.testResultFlat[step.id].path
          }
        }
        if (statusToSet == undefined) {
          if (parentStatus == Constants.TEST_STATUS.COMPLETED || parentStatus == Constants.TEST_STATUS.ERROR || parentStatus == Constants.TEST_STATUS.SKIPPED) {
            statusToSet = Constants.TEST_STATUS.SKIPPED
          }
        }
        step.status =  statusToSet
        if (step.type == "decision") {
          this.addStatusToSteps(step.then, statusToSet)
          this.addStatusToSteps(step.else, statusToSet)
        } else if (step.type == "group") {
          this.addStatusToSteps(step.steps, statusToSet)
        } else if (step.type == "flow") {
          if (step.threads != undefined) {
            for (let thread of step.threads) {
              this.addStatusToSteps(thread, statusToSet)
            }
          }
        } else if (step.type == "loop") {
          this.traverseLoops(step)
          if (step.sequences != undefined) {
            for (let sequence of step.sequences) {
              this.addStatusToSteps(sequence.steps, statusToSet)
            }
          }
        }
      }
    }
  }

  traverseLoops(loopStep: StepData) {
    loopStep.sequences = []
    let i = 1
    while (this.testResultFlat[loopStep.id + "[" + i + "]"] != undefined) {
      let loopItemResult = this.testResultFlat[loopStep.id + "[" + i + "]"]
      let loopItem: StepData = {
        id: loopItemResult.stepId,
        report: {
          path: loopItemResult.path,
          tcInstanceId: loopItemResult.sessionId
        },
        status: loopItemResult.result,
        steps: [],
        type: "loop"
      }
      let sequence = JSON.stringify(loopStep.steps)
      if (i > 1) {
        sequence = sequence.split(loopStep.id+"[1]").join(loopStep.id + "[" + i + "]")
      }
      let sequenceObj = JSON.parse(sequence)
      if (sequenceObj.length > 0) {
        for (let seqItem of sequenceObj) {
          loopItem.steps.push(seqItem)
        }
      }
      loopStep.sequences.push(loopItem)
      i = i+1
    }
    loopStep.currentIndex = loopStep.sequences.length - 1
  }

}
