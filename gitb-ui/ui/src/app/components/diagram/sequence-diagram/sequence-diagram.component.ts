import {Component, Input, OnInit} from '@angular/core';
import {ActorInfo} from '../actor-info';
import {StepData} from '../step-data';
import {filter, find, flatten, forEach, indexOf, map, maxBy, minBy, sortBy, uniq} from 'lodash';
import {Constants} from 'src/app/common/constants';
import {DiagramEvents} from '../diagram-events';

@Component({
    selector: 'app-sequence-diagram',
    templateUrl: './sequence-diagram.component.html',
    styles: [],
    standalone: false
})
export class SequenceDiagramComponent implements OnInit {

  @Input() stepsOfTests!: {[key: string]: StepData[]}
  @Input() test!: string
  @Input() actorInfoOfTests!: {[key: string]: ActorInfo[]}
  @Input() events!: DiagramEvents

  actorInfo?: ActorInfo[]
  sutActor?: ActorInfo
  actors: string[] = []
  messages: StepData[] = []
  includesNonSpecificationActor = true

  constructor() { }

  ngOnInit(): void {
    this.events.subscribeToTestLoad((event: { testId: number }) => {
      if (event.testId+'' == this.test) {
        this.updateState()
      }
    })
    this.updateState()
  }

  classValue() {
    return 'sequence-diagram actor-diagram-' + (this.includesNonSpecificationActor?this.actors.length:(this.actors.length-1))
  }

  updateState() {
    this.sutActor = this.getSutActor(this.actorInfoOfTests[this.test])
    let steps = this.stepsOfTests[this.test]
    this.actorInfo = this.actorInfoOfTests[this.test].concat([
      {id: Constants.TEST_ENGINE_ACTOR_ID, name: Constants.TEST_ENGINE_ACTOR_NAME},
      {id: Constants.TESTER_ACTOR_ID, name: this.getTesterActorName()},
      {id: Constants.ADMINISTRATOR_ACTOR_ID, name: Constants.ADMINISTRATOR_ACTOR_NAME}
    ])
    if (steps != undefined) {
      this.messages = this.extractSteps(steps, this.actorInfo, 0)
      this.actors = this.extractActors(this.messages, this.actorInfo)
      this.setStepIndexes(this.messages)
    }
  }

  stepFilter(this: SequenceDiagramComponent, step: StepData): boolean {
    return step.type == 'msg' ||
      step.type == 'verify' ||
      step.type == 'process' ||
      step.type == 'interact' ||
      step.type == 'exit' ||
      (step.type == 'group' && (filter(step.steps, this.stepFilter.bind(this))).length > 0) ||
      (step.type == 'loop' && (filter(step.steps, this.stepFilter.bind(this))).length > 0) ||
      (step.type == 'decision' && (filter(step.then, this.stepFilter.bind(this))).length + (filter(step.else, this.stepFilter.bind(this))).length > 0) ||
      (step.type == 'flow' && (filter(step.threads, ((thread) => (filter(thread, this.stepFilter.bind(this))).length > 0))).length > 0)
  }

  processStep(step: StepData, actorInfo: ActorInfo[], currentLevel: number) {
    step.level = currentLevel
    if (step.type == 'verify' || step.type == 'process' || step.type == 'exit') {
      step.from = Constants.TEST_ENGINE_ACTOR_ID
      step.to = Constants.TEST_ENGINE_ACTOR_ID
    } else if (step.type == 'group') {
      step.steps = this.extractSteps(step.steps, actorInfo, currentLevel)
    } else if (step.type == 'loop') {
      step.steps = this.extractSteps(step.steps, actorInfo, currentLevel)
      if (step.sequences != undefined) {
        for (let sequence of step.sequences) {
          this.processStep(sequence, actorInfo, currentLevel + 1)
        }
      }
    } else if (step.type == 'decision') {
      step.then = this.extractSteps(step.then, actorInfo, currentLevel)
      if (step.else) {
        step.else = this.extractSteps(step.else, actorInfo, currentLevel)
      }
    } else if (step.type == 'flow') {
      for (let thread of step.threads!) {
        this.extractSteps(thread, actorInfo, currentLevel)
      }
    } else if (step.type == 'interact') {
      const hasRequests = this.hasRequests(step.interactions)
      if (hasRequests && step.admin) {
        step.from = Constants.ADMINISTRATOR_ACTOR_ID
        step.to = Constants.TEST_ENGINE_ACTOR_ID
      } else if (hasRequests && !step.admin) {
        step.from = Constants.TESTER_ACTOR_ID
        step.to = Constants.TEST_ENGINE_ACTOR_ID
      } else if (!hasRequests && step.admin) {
        step.from = Constants.TEST_ENGINE_ACTOR_ID
        step.to = Constants.ADMINISTRATOR_ACTOR_ID
      } else {
        step.from = Constants.TEST_ENGINE_ACTOR_ID
        step.to = Constants.TESTER_ACTOR_ID
      }
    }
    return step
  }

  private hasRequests(interactions?: StepData[]) {
    if (interactions) {
      for (let interaction of interactions) {
        if (interaction.type == 'request') {
          return true
        }
      }
    }
    return false
  }

  extractSteps(s: StepData[]|undefined, actorInfo: ActorInfo[], currentLevel: number) {
    let results: StepData[] = []
    if (s != undefined) {
      let steps = filter(s, this.stepFilter.bind(this))
      for (let step of steps) {
        results = results.concat(this.processStep(step, actorInfo, currentLevel + 1))
      }
    }
    return results
  }

  getSutActor(actorInfo: ActorInfo[]) {
    for (let actor of actorInfo) {
      if (actor.role == 'SUT') {
        return actor
      }
    }
    throw Error("Test case without SUT actor")
  }

  getTesterActorName() {
    if (this.sutActor?.name != undefined) {
      return this.sutActor?.name + ' - ' + Constants.TESTER_ACTOR_NAME
    } else {
      return this.sutActor?.id + ' - ' + Constants.TESTER_ACTOR_NAME
    }
  }

  extractActors(messages: StepData[]|undefined, actorInfo: ActorInfo[]) {
    let actors = this.extractActorsInternal(messages, actorInfo)
    let hasOrdering = find(actorInfo, (actor) => {
      return actor.displayOrder != undefined
    })
    let actorsToReturn: string[]
    if (hasOrdering != undefined) {
      actorsToReturn = this.sortActors(actors, actorInfo)
    } else {
      actorsToReturn = actors
    }
    this.includesNonSpecificationActor = find(actorsToReturn, (actor) => {
      return actor == Constants.TESTER_ACTOR_ID || actor == Constants.TEST_ENGINE_ACTOR_ID || actor == Constants.ADMINISTRATOR_ACTOR_ID
    }) != undefined
    if (this.includesNonSpecificationActor) {
      actorsToReturn = this.moveActorToTheEnd(actorsToReturn, Constants.TESTER_ACTOR_ID)
      actorsToReturn = this.moveActorToTheEnd(actorsToReturn, Constants.ADMINISTRATOR_ACTOR_ID)
      actorsToReturn = this.moveActorToTheEnd(actorsToReturn, Constants.TEST_ENGINE_ACTOR_ID)
    }
    return actorsToReturn
  }

  extractActorsInternal(messages: StepData[]|undefined, actorInfo: ActorInfo[]): string[] {
    if (messages == undefined) {
      return []
    } else {
      let collection: string[][] = map(messages, (message): string[] => {
        if (message.from != undefined && message.to != undefined) {
          return [message.from, message.to]
        } else if (message.type == 'group') {
          return this.extractActorsInternal(message.steps, actorInfo)
        } else if (message.type == 'loop') {
          return this.extractActorsInternal(message.steps, actorInfo)
        } else if (message.type == 'decision') {
          let _then = this.extractActorsInternal(message.then, actorInfo)
          let _else = this.extractActorsInternal(message.else, actorInfo)
          return _then.concat(_else)
        } else if (message.type == 'flow') {
          let threadData: string[] = []
          for (let thread of message.threads!) {
            threadData = threadData.concat(this.extractActorsInternal(thread, actorInfo))
          }
          return threadData
        } else if (message.type == 'exit') {
          return [Constants.TEST_ENGINE_ACTOR_ID, Constants.TEST_ENGINE_ACTOR_ID]
        } else if (message.type == 'interact') {
          let instructionActors: string[]
          let requestActors: string[]
          if (message.admin) {
            instructionActors = [Constants.TEST_ENGINE_ACTOR_ID, Constants.ADMINISTRATOR_ACTOR_ID]
            requestActors = [Constants.ADMINISTRATOR_ACTOR_ID, Constants.TEST_ENGINE_ACTOR_ID]
          } else {
            instructionActors = [Constants.TEST_ENGINE_ACTOR_ID, Constants.TESTER_ACTOR_ID]
            requestActors = [Constants.TESTER_ACTOR_ID, Constants.TEST_ENGINE_ACTOR_ID]
          }
          return flatten(instructionActors.concat(requestActors))
        } else {
          return []
        }
      })
      return uniq(flatten(collection))
    }
  }

  moveActorToTheEnd(actors: string[], actorId: string) {
    let testEngineIndex = actors.indexOf(actorId)
    if (testEngineIndex != -1) {
      actors.splice(testEngineIndex, 1)
      actors.push(actorId)
    }
    return actors
  }

  sortActors(actors: string[], actorInfo: ActorInfo[]) {
    let actorsWithOrder = map(actors, (actorId) => {
      const match = find(actorInfo, (info) => {
        return info.id == actorId
      })
      if (match == undefined || match.displayOrder == undefined) {
        return { id: actorId }
      } else {
        return { id: actorId, order: match.displayOrder }
      }
    })
    const sortedArray = sortBy(actorsWithOrder, ['order'])
    return map(sortedArray, (item) => {
      return item.id
    })
  }

  setStepIndexes(this: SequenceDiagramComponent, messages: StepData[]) {
    forEach(messages, (message, i) => {
      message.order = i
      if (message.type == 'verify' || message.type == 'process' || message.type == 'msg' || message.type == 'exit' || message.type == 'interact') {
        this.setIndexes(message)
      } else if (message.type == 'group') {
        this.setGroupStepChildIndexes(message)
      } else if (message.type == 'loop') {
        this.setLoopStepChildIndexes(message)
      } else if (message.type == 'decision') {
        this.setDecisionStepChildIndexes(message)
      } else if (message.type == 'flow') {
        this.setFlowStepChildIndexes(message)
      }
    })
  }

  setIndexes(message: StepData) {
    message.fromIndex = indexOf(this.actors, message.from)
    message.toIndex = indexOf(this.actors, message.to)
    message.span = Math.abs(message.fromIndex - message.toIndex)
  }

  private isRightToLeft(step: StepData) {
    return step.fromIndex! > step.toIndex!
  }

  private setMessageSpan(message: StepData, firstChild: StepData, lastChild: StepData, extend: boolean) {
    if (this.isRightToLeft(firstChild)) {
      message.from = firstChild.to
      message.fromIndex = firstChild.toIndex
    } else {
      message.from = firstChild.from
      message.fromIndex = firstChild.fromIndex
    }
    if (this.isRightToLeft(lastChild)) {
      message.to = lastChild.from
      message.toIndex = lastChild.fromIndex
    } else {
      message.to = lastChild.to
      message.toIndex = lastChild.toIndex
    }
    let span = Math.abs(message.fromIndex! - message.toIndex!)
    if (extend) {
      if (message.from == Constants.TEST_ENGINE_ACTOR_ID || message.to == Constants.TEST_ENGINE_ACTOR_ID) {
        // Extend the span because we may have here validation or processing with a message and a report.
        span += 1
      }
    }
    if (span == 0) {
      span = 1
    }
    message.span = span
  }

  private leftMostStepActorIndex(step: StepData) {
    if (this.isRightToLeft(step)) {
      // Right-to-left message
      return step.toIndex!
    } else {
      return step.fromIndex!
    }
  }

  private rightMostStepActorIndex(step: StepData) {
    if (this.isRightToLeft(step)) {
      // Right-to-left message
      return step.fromIndex!
    } else {
      return step.toIndex!
    }
  }

  setLoopStepChildIndexes(message: StepData) {
    this.setStepIndexes(message.steps)
    let firstChild = minBy(message.steps, this.leftMostStepActorIndex.bind(this))
    let lastChild = maxBy(message.steps, this.rightMostStepActorIndex.bind(this))
    this.setMessageSpan(message, firstChild!, lastChild!, true)
    if (message.sequences != undefined) {
      for (let sequence of message.sequences) {
        this.setLoopStepChildIndexes(sequence)
      }
    }
  }

  setGroupStepChildIndexes(message: StepData) {
    let childSteps = message.steps
    this.setStepIndexes(childSteps)
    let firstChild = minBy(childSteps, this.leftMostStepActorIndex.bind(this))
    let lastChild = maxBy(childSteps, this.rightMostStepActorIndex.bind(this))
    this.setMessageSpan(message, firstChild!, lastChild!, true)
  }

  setDecisionStepChildIndexes(message: StepData) {
    let childSteps = message.then!
    if (message.else) {
      childSteps = childSteps.concat(message.else)
    }
    this.setStepIndexes(childSteps)
    let firstChild = minBy(childSteps, this.leftMostStepActorIndex.bind(this))
    let lastChild = maxBy(childSteps, this.rightMostStepActorIndex.bind(this))
    this.setMessageSpan(message, firstChild!, lastChild!, true)
  }

  setFlowStepChildIndexes(message: StepData) {
    forEach(message.threads, this.setStepIndexes.bind(this))
    let firstChild = minBy(flatten(message.threads), this.leftMostStepActorIndex.bind(this))
    let lastChild = maxBy(flatten(message.threads), this.rightMostStepActorIndex.bind(this))
    this.setMessageSpan(message, firstChild!, lastChild!, true)
  }

}
