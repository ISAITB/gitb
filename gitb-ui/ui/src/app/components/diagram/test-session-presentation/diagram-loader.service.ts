import { share, forkJoin, mergeMap, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { ReportService } from 'src/app/services/report.service';
import { TestCaseDefinition } from 'src/app/types/test-case-definition';
import { TestService } from 'src/app/services/test.service';
import { ActorInfo } from '../actor-info';
import { StepData } from '../step-data';
import { Constants } from 'src/app/common/constants';
import { SessionData } from './session-data';
import { SessionPresentationData } from './session-presentation-data';
import { TestStepResult } from 'src/app/types/test-step-result';
import { DiagramEvents } from '../diagram-events';
import { TestInteractionData } from 'src/app/types/test-interaction-data';

@Injectable({
  providedIn: 'root'
})
export class DiagramLoaderService {

  constructor(
    private reportService: ReportService,
    private testService: TestService
  ) { }

  findNodeWithStepId(steps: StepData[]|undefined, stepId: string): StepData|undefined {
    if (steps != undefined) {
      for (let step of steps) {
        if (step != undefined) {
          const parentOrCurrentNode = this.filterStep(step, stepId)
          if (parentOrCurrentNode != undefined) {
            return parentOrCurrentNode
          }
        }
      }
    }
    return undefined
  }

  private filterStep(step: StepData, id: string): StepData|undefined {
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

  private isParent(id: string, parentId: string) {
    const periodIndex = id.indexOf('.', parentId.length)
    const parenthesesIndex = id.indexOf('[', parentId.length)
    return id.indexOf(parentId) == 0 && (periodIndex == parentId.length || parenthesesIndex == parentId.length)
  }

  loadTestStepResults(session: string) {
    return this.reportService.getTestStepResults(session)
    .pipe(
      mergeMap((stepResults) => {
        const testResultFlat:{[key: string]: TestStepResult} = {}
        for (let result of stepResults) {
          testResultFlat[result.stepId] = result
        }
        return of(testResultFlat)
      }),
      share() // We return a shared observable here otherwise the requests are repeated
    )
  }

  determineOutputMessageType(result: "SUCCESS"|"FAILURE"|"UNDEFINED") {
    let outputMessageType: string
    if (result == Constants.TEST_CASE_RESULT.SUCCESS) {
      outputMessageType = 'success'
    } else if (result == Constants.TEST_CASE_RESULT.FAILURE) {
      outputMessageType = 'danger'
    } else {
      outputMessageType = 'info'
    }
    return outputMessageType
  }

  loadTestSessionData(session: SessionData): Observable<SessionPresentationData> {
    // Load test step results
    const obs1 = this.loadTestStepResults(session.session)
    // Load log entries (only for an active test session).
    let obs2: Observable<string[]|undefined> = of(undefined)
    if (session.endTime == undefined) obs2 = this.reportService.getTestSessionLog(session.session)
    // Load pending interactions (only for an active test session).
    let obs3: Observable<TestInteractionData[]|undefined> = of(undefined)
    if (session.endTime == undefined) obs3 = this.reportService.getPendingTestSessionInteractions(session.session)
    // Load test result details
    const obs4 = this.reportService.getTestResultOfSession(session.session)
    .pipe(
      mergeMap((result) => {
        let testcase = JSON.parse(result.tpl!) as TestCaseDefinition
        return this.testService.prepareTestCaseDisplayActors(testcase, result.specificationId).pipe(
          mergeMap((actorDataToUse) => {
            let actorInfoOfTests: {[key: string]: ActorInfo[]} = {}
            let stepsOfTests: {[key: string]: StepData[]} = {}
            actorInfoOfTests[session.session] = actorDataToUse
            stepsOfTests[session.session] = testcase.steps
            return of({
              outputMessage: result.outputMessage,
              outputMessageType: this.determineOutputMessageType(result.result),
              actorInfoOfTests: actorInfoOfTests,
              stepsOfTests: stepsOfTests
            })
          })
        )
      }),
      share() // We return a shared observable here otherwise the requests are repeated
    )
    // Process once both requests are complete
    return forkJoin([obs1, obs2, obs3, obs4]).pipe(
      mergeMap((result) => {
        this.updateStatusOfSteps(session, result[3].stepsOfTests[session.session], result[0])
        const state: SessionPresentationData = {
          stepsOfTests: result[3].stepsOfTests,
          actorInfoOfTests: result[3].actorInfoOfTests,
          outputMessage: result[3].outputMessage,
          outputMessageType: result[3].outputMessageType,
          events: new DiagramEvents(),
          testResultFlat: result[0],
          logs: result[1],
          interactions: result[2]
        }
        return of(state)
      })
    )
  }

  updateStatusOfSteps(session: SessionData, steps: StepData[], testResultFlat:{[key: string]: TestStepResult}) {
    let parentStatus = this.statusForSession(session)
    this.addStatusToSteps(session.session, steps, testResultFlat, parentStatus)
  }

  private statusForSession(session: SessionData): number {
    let parentStatus = Constants.TEST_STATUS.WAITING
    if (session.endTime) {
      if (session.result != undefined) {
        if (session.result == 'SUCCESS') {
          parentStatus = Constants.TEST_STATUS.COMPLETED
        } else if (session.result == 'FAILURE') {
          parentStatus = Constants.TEST_STATUS.ERROR
        } else if (session.result == 'UNDEFINED') {
          parentStatus = Constants.TEST_STATUS.SKIPPED
        }
      } else {
        parentStatus = Constants.TEST_STATUS.ERROR
      }
    }
    return parentStatus
  }

  private addStatusToSteps(sessionId: string, steps: StepData[]|undefined, testResultFlat:{[key: string]: TestStepResult}, parentStatus?: number) {
    if (steps != undefined) {
      for (let step of steps) {
        let statusToSet = undefined
        if (testResultFlat[step.id] != undefined) {
          statusToSet = testResultFlat[step.id].result
          if (testResultFlat[step.id].path !== undefined) {
            step.report = {}
            step.report.tcInstanceId = sessionId
            step.report.path = testResultFlat[step.id].path
          }
        }
        if (statusToSet == undefined) {
          if (parentStatus == Constants.TEST_STATUS.COMPLETED || parentStatus == Constants.TEST_STATUS.ERROR || parentStatus == Constants.TEST_STATUS.SKIPPED) {
            statusToSet = Constants.TEST_STATUS.SKIPPED
          }
        }
        step.status =  statusToSet
        if (step.type == "decision") {
          this.addStatusToSteps(sessionId, step.then, testResultFlat, statusToSet)
          this.addStatusToSteps(sessionId, step.else, testResultFlat, statusToSet)
        } else if (step.type == "group") {
          this.addStatusToSteps(sessionId, step.steps, testResultFlat, statusToSet)
        } else if (step.type == "flow") {
          if (step.threads != undefined) {
            for (let thread of step.threads) {
              this.addStatusToSteps(sessionId, thread, testResultFlat, statusToSet)
            }
          }
        } else if (step.type == "loop") {
          // By default set the steps of the loop as skipped. Normal status values for executed iterations will be looked up via the sequences.
          this.addStatusToSteps(sessionId, step.steps, testResultFlat, Constants.TEST_STATUS.SKIPPED)
          this.traverseLoops(testResultFlat, step)
          if (step.sequences != undefined) {
            for (let sequence of step.sequences) {
              this.addStatusToSteps(sessionId, sequence.steps, testResultFlat, statusToSet)
            }
          }
        }
      }
    }
  }

  private traverseLoops(testResultFlat:{[key: string]: TestStepResult}, loopStep: StepData) {
    loopStep.sequences = []
    let i = 1
    while (testResultFlat[loopStep.id + "[" + i + "]"] != undefined) {
      let loopItemResult = testResultFlat[loopStep.id + "[" + i + "]"]
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
