import { Component, Input, OnInit } from '@angular/core';
import { ActorInfo } from '../actor-info';
import { StepData } from '../step-data';
import { filter, find, map, flatten, uniq, forEach, indexOf, minBy, maxBy } from 'lodash'
import { Constants } from 'src/app/common/constants';
import { DiagramEvents } from '../diagram-events';

@Component({
  selector: 'app-sequence-diagram',
  templateUrl: './sequence-diagram.component.html',
  styles: [
  ]
})
export class SequenceDiagramComponent implements OnInit {

  @Input() stepsOfTests!: {[key: string]: StepData[]}
  @Input() test!: string
  @Input() actorInfoOfTests!: {[key: string]: ActorInfo[]}
  @Input() events!: DiagramEvents

  actorInfo?: ActorInfo[]
  actors: string[] = []
  messages: StepData[] = []

  constructor() { }

  ngOnInit(): void {
    this.events.subscribeToTestLoad(((event: { testId: number }) => {
      if (event.testId+'' == this.test) {
        this.updateState()
      }
    }).bind(this))
    this.updateState()
  }

  classValue() {
    return 'sequence-diagram actor-diagram-' + this.actors.length
  }

  updateState() {
    let steps = this.stepsOfTests[this.test]
    this.actorInfo = this.actorInfoOfTests[this.test]
    if (steps != undefined) {
      this.messages = this.extractSteps(steps, this.actorInfo)
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

  processStep(step: StepData, actorInfo: ActorInfo[]) {
    step.level = (step.id.split('.')).length
    if (step.type == 'verify' || step.type == 'process' || step.type == 'exit') {
      step.from = Constants.TEST_ENGINE_ACTOR
      step.to = Constants.TEST_ENGINE_ACTOR
    } else if (step.type == 'group') {
      step.steps = this.extractSteps(step.steps, actorInfo)
    } else if (step.type == 'loop') {
      step.steps = this.extractSteps(step.steps, actorInfo)
      if (step.sequences != undefined) {
        for (let sequence of step.sequences) {
          this.processStep(sequence, actorInfo)
        }
      }
    } else if (step.type == 'decision') {
      step.then = this.extractSteps(step.then, actorInfo)
      if (step.else) {
        step.else = this.extractSteps(step.else, actorInfo)
      }
    } else if (step.type == 'flow') {
      for (let thread of step.threads!) {
        this.extractSteps(thread, actorInfo)
      }
    } else if (step.type == 'interact') {
      for (let interaction of step.interactions!) {
        if (interaction.type == 'request') {
          interaction.from = this.getTesterNameForActor(interaction.with, actorInfo)
          interaction.to = Constants.TEST_ENGINE_ACTOR
        } else {
          interaction.from = this.getTesterNameForActor(interaction.with, actorInfo)
          interaction.to = this.getSutActorIfMissing(interaction.with, actorInfo)
        }
      }
    }
    return step
  }

  extractSteps(s: StepData[]|undefined, actorInfo: ActorInfo[]) {
    let results: StepData[] = []
    if (s != undefined) {
      let steps = filter(s, this.stepFilter.bind(this))
      for (let step of steps) {
        results = results.concat(this.processStep(step, actorInfo))
      }
    }
    return results
  }

  getSutActorIfMissing(actor: string|undefined, actorInfo: ActorInfo[]) {
    let result = actor
    if (result == undefined) {
      let sutActor = find(actorInfo, (a) =>
        a.role == 'SUT'
      )
      result = sutActor!.id
    }
    return result
  }

  getTesterNameForActor(actor: string|undefined, actorInfo: ActorInfo[]) {
    let actorId = this.getSutActorIfMissing(actor, actorInfo)
    let actorName = actorId + ' - ' + Constants.TESTER_ACTOR
    for (let info of actorInfo) {
      if (actor == info.id) {
        if (info.name != undefined) {
          actorName = info.name + ' - ' + Constants.TESTER_ACTOR
        } else {
          actorName = info.id + ' - ' + Constants.TESTER_ACTOR
        }
        break
      }
    }
    return actorName
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
    return this.moveTestEngineToTheEnd(actorsToReturn)
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
          return [Constants.TEST_ENGINE_ACTOR, Constants.TEST_ENGINE_ACTOR]
        } else if (message.type == 'interact') {
          let instructions = filter(message.interactions, (interaction) => interaction.type == 'instruction')
          let requests = filter(message.interactions, (interaction) => interaction.type == 'request')
          let instructionActors = map(instructions, (instruction) => { return [this.getTesterNameForActor(instruction.with, actorInfo), this.getSutActorIfMissing(instruction.with, actorInfo)] })
          let requestActors = map(requests, (request) => { return [this.getTesterNameForActor(request.with, actorInfo), Constants.TEST_ENGINE_ACTOR] })
          return flatten(instructionActors.concat(requestActors))
        } else {
          return []
        }
      })
      let flattened = uniq(flatten(collection))
      return flattened
    }
  }

  moveTestEngineToTheEnd(actors: string[]) {
    let testEngineIndex = actors.indexOf(Constants.TEST_ENGINE_ACTOR)
    if (testEngineIndex != -1) {
      actors.splice(testEngineIndex, 1)
      actors.push(Constants.TEST_ENGINE_ACTOR)
    }
    return actors
  }

  sortActors(actors: string[], actorInfo: ActorInfo[]) {
    let sortedArray: (string|undefined)[] = []
    for (let actor of actors) {
      let actorDef = find(actorInfo, (actorDef) => { return actor == actorDef.id })
      if (actorDef != undefined) {
        sortedArray.push(undefined)
      } else {
        sortedArray.push(actor)
      }
    }
    for (let actor of actorInfo) {
      for (let sortedIndex = 0; sortedIndex < sortedArray.length; sortedIndex++) {
        let sortedActor = sortedArray[sortedIndex]
        if (sortedActor == undefined) {
          sortedArray[sortedIndex] = actor.id
          break
        }
      }
    }
    return sortedArray as string[]
  }

  setStepIndexes(this: SequenceDiagramComponent, messages: StepData[]) {
    forEach(messages, (message, i) => {
      message.order = i
      if (message.type == 'verify' || message.type == 'process' || message.type == 'msg' || message.type == 'exit') {
        this.setIndexes(message)
      } else if (message.type == 'group') {
        this.setLoopStepChildIndexes(message)
      } else if (message.type == 'loop') {
        this.setLoopStepChildIndexes(message)
      } else if (message.type == 'decision') {
        this.setDecisionStepChildIndexes(message)
      } else if (message.type == 'flow') {
        this.setFlowStepChildIndexes(message)
      } else if (message.type == 'interact') {
        this.setInteractionStepChildIndexes(message)
      }
    })
  }

  setIndexes(message: StepData) {
    message.fromIndex = indexOf(this.actors, message.from)
    message.toIndex = indexOf(this.actors, message.to)
    message.span = Math.abs(message.fromIndex - message.toIndex)
  }

  setLoopStepChildIndexes(message: StepData) {
    this.setStepIndexes(message.steps)
    let firstChild = minBy(message.steps, (childStep) => { return childStep.fromIndex })
    let lastChild = maxBy(message.steps, (childStep) => { return childStep.toIndex })
    message.from = firstChild!.from
    message.to = lastChild!.to
    message.fromIndex = firstChild!.fromIndex
    message.toIndex = lastChild!.toIndex
    message.span = Math.abs(message.fromIndex! - message.toIndex!)+1
    if (message.sequences != undefined) {
      for (let sequence of message.sequences) {
        this.setLoopStepChildIndexes(sequence)
      }
    }
  }

  setGroupStepChildIndexes(message: StepData) {
    let childSteps = message.steps
    this.setStepIndexes(childSteps)
    let firstChild = minBy(childSteps, (childStep) => {return childStep.fromIndex})
    let lastChild = maxBy(childSteps, (childStep) => {return childStep.toIndex})
    message.from = firstChild!.from
    message.to = lastChild!.to
    message.fromIndex = firstChild!.fromIndex
    message.toIndex = lastChild!.toIndex
    message.span = (Math.abs (message.fromIndex! - message.toIndex!))+1
  }

  setDecisionStepChildIndexes(message: StepData) {
    let childSteps = message.then!
    if (message.else) {
      childSteps = childSteps.concat(message.else)
    }
    this.setStepIndexes(childSteps)
    let firstChild = minBy(childSteps, (childStep) => {return childStep.fromIndex})
    let lastChild = maxBy(childSteps, (childStep) => {return childStep.toIndex})
    message.from = firstChild!.from
    message.to = lastChild!.to
    message.fromIndex = firstChild!.fromIndex
    message.toIndex = lastChild!.toIndex
    message.span = (Math.abs (message.fromIndex! - message.toIndex!))+1
  }

  setFlowStepChildIndexes(message: StepData) {
    forEach(message.threads, this.setStepIndexes.bind(this))
    let firstChild = minBy(flatten(message.threads), (childStep) => {return childStep.fromIndex})
    let lastChild = maxBy(flatten(message.threads), (childStep) => { return childStep.fromIndex})
    message.from = firstChild!.from
    message.to = lastChild!.to
    message.fromIndex = firstChild!.fromIndex
    message.toIndex = lastChild!.toIndex
    message.span = (Math.abs (message.fromIndex! - message.toIndex!))+1
  }

  setInteractionStepChildIndexes(message: StepData) {
    forEach(message.interactions, this.setIndexes.bind(this))
    let firstChild = minBy(message.interactions, (interaction) => {return Math.min(interaction.fromIndex!, interaction.toIndex!)})
    let lastChild = maxBy(message.interactions, (interaction) => {return Math.max(interaction.fromIndex!, interaction.toIndex!)})
    message.fromIndex = Math.min(firstChild!.fromIndex!, firstChild!.toIndex!)
    message.toIndex = Math.max(lastChild!.fromIndex!, lastChild!.toIndex!)
    message.from = this.actors![message.fromIndex]
    message.to = this.actors![message.toIndex]
    message.span = (Math.abs(message.fromIndex - message.toIndex))
  }

}
