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

import {Component, Input, OnInit} from '@angular/core';
import {ActorInfo} from '../actor-info';
import {StepData} from '../step-data';
import {ActorRole} from '../../../types/actor-role';
import {Constants} from 'src/app/common/constants';
import {DiagramEvents} from '../diagram-events';
import {TestCaseDefinitionActors} from '../../../types/test-case-definition-actors';
import {Utils} from '../../../common/utils';

@Component({
    selector: 'app-sequence-diagram',
    templateUrl: './sequence-diagram.component.html',
    styles: [],
    standalone: false
})
export class SequenceDiagramComponent implements OnInit {

  @Input() stepsOfTests!: {[key: string]: StepData[]}
  @Input() test!: string
  @Input() actorInfoOfTests!: {[key: string]: TestCaseDefinitionActors}
  @Input() events!: DiagramEvents

  actorInfo?: ActorInfo[]
  sutActor?: ActorInfo
  actors: ActorInfo[] = []
  messages: StepData[] = []
  actorStyleCount!: number

  constructor() { }

  ngOnInit(): void {
    this.events.subscribeToTestLoad((event: { testId: number }) => {
      if (event.testId+'' == this.test) {
        this.updateState()
      }
    })
    this.updateState()
  }

  updateState() {
    this.actorInfo = this.prepareActorInfo()
    let steps = this.stepsOfTests[this.test]
    if (steps != undefined) {
      this.messages = this.extractSteps(steps, this.actorInfo, 0)
      this.actors = this.extractActors(this.messages, this.actorInfo)
      this.setStepIndexes(this.messages)
      this.actorStyleCount = this.actors.length
    }
  }

  private prepareActorInfo(): ActorInfo[] {
    const actorDefinition = this.actorInfoOfTests[this.test]
    actorDefinition.actor.forEach(actor => {
      if (actor.role == "SUT") {
        actor.diagramRole = ActorRole.SystemUnderTest
        // Extract the SUT actor.
        this.sutActor = actor
      } else if (!actor.diagramRole) {
        actor.diagramRole = ActorRole.Simulated
      }
    })
    if (!this.sutActor) {
      throw new Error("Test case without SUT actor")
    }
    // Add built-in actors.
    return actorDefinition.actor.concat([
      {
        id: Constants.TEST_ENGINE_ACTOR_ID,
        name: (actorDefinition.engineName == undefined)?Constants.TEST_ENGINE_ACTOR_NAME:actorDefinition.engineName,
        diagramRole: ActorRole.TestEngine,
        displayOrder: actorDefinition.engineDisplayOrder
      },
      {
        id: Constants.TESTER_ACTOR_ID,
        name: (actorDefinition.userName == undefined)?this.getTesterActorName():actorDefinition.userName,
        diagramRole: ActorRole.User,
        displayOrder: actorDefinition.userDisplayOrder
      },
      {
        id: Constants.ADMINISTRATOR_ACTOR_ID,
        name: (actorDefinition.adminName == undefined)?Constants.ADMINISTRATOR_ACTOR_NAME:actorDefinition.adminName,
        diagramRole: ActorRole.Administrator,
        displayOrder: actorDefinition.adminDisplayOrder
      }
    ])
  }

  private stepFilter(step: StepData): boolean {
    return step.type == 'msg' ||
      step.type == 'verify' ||
      step.type == 'process' ||
      step.type == 'interact' ||
      step.type == 'exit' ||
      (step.type == 'group' && (step.steps.filter((s) => this.stepFilter(s)).length > 0)) ||
      (step.type == 'loop' && (step.steps.filter((s) => this.stepFilter(s)).length > 0)) ||
      (step.type == 'decision' && (((step.then == undefined)?0:step.then.filter((s) => this.stepFilter(s)).length) + ((step.else == undefined)?0:step.else.filter((s) => this.stepFilter(s)).length)) > 0) ||
      (step.type == 'flow' && (((step.threads == undefined)?0:step.threads.filter(((thread) => thread.filter((s) => this.stepFilter(s)).length > 0)).length) > 0))
  }

  processStep(step: StepData, actorInfo: ActorInfo[], currentLevel: number) {
    step.level = currentLevel
    if (step.type == 'verify' || step.type == 'process' || step.type == 'exit') {
      if (step.from == undefined) {
        if (step.to == undefined) {
          step.from = Constants.TEST_ENGINE_ACTOR_ID
        } else {
          step.from = step.to
        }
      }
      if (step.to == undefined) {
        if (step.from == undefined) {
          step.to = Constants.TEST_ENGINE_ACTOR_ID
        } else {
          step.to = step.from
        }
      }
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
        if (step.to == undefined) {
          step.to = Constants.TEST_ENGINE_ACTOR_ID
        }
      } else if (hasRequests && !step.admin) {
        step.from = Constants.TESTER_ACTOR_ID
        if (step.to == undefined) {
          step.to = Constants.TEST_ENGINE_ACTOR_ID
        }
      } else if (!hasRequests && step.admin) {
        if (step.from == undefined) {
          step.from = Constants.TEST_ENGINE_ACTOR_ID
        }
        step.from = Constants.TEST_ENGINE_ACTOR_ID
        step.to = Constants.ADMINISTRATOR_ACTOR_ID
      } else {
        if (step.from == undefined) {
          step.from = Constants.TEST_ENGINE_ACTOR_ID
        }
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

  extractSteps(stepsToProcess: StepData[]|undefined, actorInfo: ActorInfo[], currentLevel: number) {
    let results: StepData[] = []
    if (stepsToProcess != undefined) {
      let steps = stepsToProcess.filter((step) => this.stepFilter(step))
      for (let step of steps) {
        results = results.concat(this.processStep(step, actorInfo, currentLevel + 1))
      }
    }
    return results
  }

  getTesterActorName() {
    if (this.sutActor?.name != undefined) {
      return this.sutActor?.name
    } else {
      return this.sutActor?.id
    }
  }

  extractActors(messages: StepData[]|undefined, actorInfo: ActorInfo[]): ActorInfo[] {
    // Get actor identifiers from steps.
    const actorIdentifiers = this.extractActorsInternal(messages, actorInfo)
    // Determine sorted list of actors.
    let userActorToAppend: ActorInfo|undefined
    let administratorActorToAppend: ActorInfo|undefined
    let testEngineActorToAppend: ActorInfo|undefined
    const specificationActors: ActorInfo[] = []
    let actorsNeedSorting = false
    // Map actors to actor identifiers.
    actorIdentifiers.forEach((actorIdentifier: string) => {
      const matchedActor = actorInfo.find(actor => actor.id == actorIdentifier)
      if (!matchedActor) {
        throw new Error(`Unable to retrieve actor definition based on identifier [${actorIdentifier}]`)
      }
      if (matchedActor.diagramRole === ActorRole.User) {
        if (matchedActor.displayOrder == undefined) {
          userActorToAppend = matchedActor
        } else {
          specificationActors.push(matchedActor)
          actorsNeedSorting = true
        }
      } else if (matchedActor.diagramRole === ActorRole.Administrator) {
        if (matchedActor.displayOrder == undefined) {
          administratorActorToAppend = matchedActor
        } else {
          specificationActors.push(matchedActor)
          actorsNeedSorting = true
        }
      } else if (matchedActor.diagramRole === ActorRole.TestEngine) {
        if (matchedActor.displayOrder == undefined) {
          testEngineActorToAppend = matchedActor
        } else {
          specificationActors.push(matchedActor)
          actorsNeedSorting = true
        }
      } else {
        if (matchedActor.displayOrder != undefined) {
          actorsNeedSorting = true
        }
        specificationActors.push(matchedActor)
      }
    })
    // Sort (if needed) the test case's declared actors.
    if (actorsNeedSorting) {
      specificationActors.sort((actor1, actor2) => {
        if (actor1.displayOrder != undefined && actor2.displayOrder != undefined) {
          return actor1.displayOrder - actor2.displayOrder
        } else if (actor1.displayOrder != undefined) {
          return -1
        } else if (actor2.displayOrder != undefined) {
          return 1
        } else {
          return 0
        }
      })
    }
    // If in use, add the special built-in actors at the end.
    let allActors = specificationActors
    if (userActorToAppend) allActors.push(userActorToAppend)
    if (administratorActorToAppend) allActors.push(administratorActorToAppend)
    if (testEngineActorToAppend) allActors.push(testEngineActorToAppend)
    return allActors
  }

  extractActorsInternal(messages: StepData[]|undefined, actorInfo: ActorInfo[]): string[] {
    if (messages == undefined) {
      return []
    } else {
      let collection: string[][] = messages.map((message): string[] => {
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
          return instructionActors.concat(requestActors).flat()
        } else {
          return []
        }
      })
      return Utils.uniqueValues(collection.flat())
    }
  }

  setStepIndexes(this: SequenceDiagramComponent, messages: StepData[]) {
    messages.forEach((message, i) => {
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
    message.fromIndex = this.actors.findIndex(actor => actor.id == message.from)
    message.toIndex = this.actors.findIndex(actor => actor.id == message.to)
    message.span = Math.abs(message.fromIndex - message.toIndex)
  }

  private isRightToLeft(step: StepData) {
    return step.fromIndex! > step.toIndex!
  }

  private stepTypeExpandsRightContainerBound(step: StepData) {
    // These are the types of steps that can display a report or other information in their own swimlane.
    return step.type != "interact" && (step.type != "msg" || (step.from == step.to))
  }

  private setMessageSpan(message: StepData, firstChild: StepData, lastChild: StepData) {
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
    if (this.stepTypeExpandsRightContainerBound(lastChild)) {
      span += 1
    }
    if (lastChild.span != undefined && lastChild.span > span) {
      span = lastChild.span
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

  private leftMostStep(steps: StepData[]) {
    if (steps.length == 0) {
      return undefined
    } else {
      return steps.reduce((min, item) => (this.leftMostStepActorIndex(item) < this.leftMostStepActorIndex(min))?item:min)
    }
  }

  private rightMostStep(steps: StepData[]) {
    return steps.reduce((best: StepData|null, step: StepData): StepData => {
      if (!best) return step;
      const bestIndex = this.rightMostStepActorIndex(best)
      const currentIndex = this.rightMostStepActorIndex(step)
      if (currentIndex > bestIndex) {
        return step
      } else if (currentIndex < bestIndex) {
        return best
      } else {
        // Prefer steps with a report on the actor's lifeline.
        if (this.stepTypeExpandsRightContainerBound(step)) {
          return step
        } else {
          return best
        }
      }
    }, null);
  }

  setGroupStepChildIndexes(message: StepData) {
    let childSteps = message.steps
    this.setStepIndexes(childSteps)
    let firstChild = this.leftMostStep(childSteps)
    let lastChild = this.rightMostStep(childSteps)
    this.setMessageSpan(message, firstChild!, lastChild!)
  }

  setDecisionStepChildIndexes(message: StepData) {
    let childSteps = message.then!
    if (message.else) {
      childSteps = childSteps.concat(message.else)
    }
    this.setStepIndexes(childSteps)
    let firstChild = this.leftMostStep(childSteps)
    let lastChild = this.rightMostStep(childSteps)
    this.setMessageSpan(message, firstChild!, lastChild!)
  }

  setFlowStepChildIndexes(message: StepData) {
    message.threads?.forEach(thread => this.setStepIndexes(thread))
    const childSteps = (message.threads == undefined)?[]:message.threads.flat()
    let firstChild = this.leftMostStep(childSteps)
    let lastChild = this.rightMostStep(childSteps)
    this.setMessageSpan(message, firstChild!, lastChild!)
  }

  setLoopStepChildIndexes(message: StepData) {
    this.setStepIndexes(message.steps)
    let firstChild = this.leftMostStep(message.steps)
    let lastChild = this.rightMostStep(message.steps)
    this.setMessageSpan(message, firstChild!, lastChild!)
    if (message.sequences != undefined) {
      for (let sequence of message.sequences) {
        this.setLoopStepChildIndexes(sequence)
      }
    }
  }

}
