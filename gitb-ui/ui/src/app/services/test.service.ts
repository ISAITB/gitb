import { Injectable } from '@angular/core';
import { find } from 'lodash';
import { map, mergeMap, Observable, of, share } from 'rxjs';
import { ROUTES } from '../common/global';
import { ActorInfo } from '../components/diagram/actor-info';
import { Actor } from '../types/actor';
import { ConfigureResponse } from '../types/configure-response';
import { FileParam } from '../types/file-param.type';
import { TestCaseDefinition } from '../types/test-case-definition';
import { UserInteractionInput } from '../types/user-interaction-input';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class TestService {

  constructor(
    private restService: RestService
  ) { }

  stop(session: string) {
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.stop(session).url,
        authenticate: true
    })
  }

  stopAll() {
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.stopAll().url,
        authenticate: true
    })
  }

  stopAllCommunitySessions(communityId: number) {
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.stopAllCommunitySessions(communityId).url,
        authenticate: true
    })
  }

  stopAllOrganisationSessions(organisationId: number) {
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.stopAllOrganisationSessions(organisationId).url,
        authenticate: true
    })
  }

  getBinaryMetadata(data: string, isBase64: boolean) {
    return this.restService.post<{mimeType: string, extension: string}>({
        path: ROUTES.controllers.TestResultService.getBinaryMetadata().url,
        authenticate: true,
        data: {
            data: data,
            is_base64: isBase64
        }
    })
  }

  startHeadlessTestSessions(testCaseIds: number[], specId: number, systemId: number, actorId: number, sequential: boolean) {
    const data: any = {
      spec_id: specId,
      system_id: systemId,
      actor_id: actorId,
      sequential: sequential
    }
    if (testCaseIds != undefined && testCaseIds.length > 0) {
        data.test_case_ids = testCaseIds.join(',')
    }
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.startHeadlessTestSessions().url,
        authenticate: true,
        data: data            
    })
  }

  getTestCaseDefinition(testCase: number) {
    return this.restService.get<TestCaseDefinition>({
        path: ROUTES.controllers.TestService.getTestCaseDefinition(testCase).url,
        authenticate: true
    })
  }

  getActorDefinitions(specificationId: number) {
    return this.restService.get<Actor[]>({
        path: ROUTES.controllers.TestService.getActorDefinitions().url,
        authenticate: true,
        params: {
          spec_id: specificationId
        }
    })
  }

  initiate(testCase: number) {
    return this.restService.post<string>({
        path: ROUTES.controllers.TestService.initiate(testCase).url,
        authenticate: true,
        text: true
    })
  }

  configure(specId: number, session: string, systemId: number, actorId: number) {
    return this.restService.post<ConfigureResponse>({
        path: ROUTES.controllers.TestService.configure(session).url,
        params: {
            spec_id: specId,
            system_id: systemId,
            actor_id: actorId
        },
        authenticate: true
    })
  }

  initiatePreliminary(session: string) {
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.initiatePreliminary(session).url,
        authenticate: true
    })
  }

  start(session: string) {
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.start(session).url,
        authenticate: true
    })
  }

  restart(session: string) {
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.restart(session).url,
        authenticate: true
    })
  }

  provideInput(session: string, step: string, inputs: UserInteractionInput[]) {
    const inputsToSend: any[] = []
    let files: FileParam[] = []
    for (let input of inputs) {
      const inputToSend: any = {
        id: input.id,
        name: input.name,
        type: input.type,
        embeddingMethod: input.embeddingMethod
      }
      if (input.file) {
        files.push({param: 'file_'+input.id, data: input.file})
      } else {
        inputToSend.value = input.value
      }
      inputsToSend.push(inputToSend)
    }
    return this.restService.post<void>({
        path: ROUTES.controllers.TestService.provideInput(session).url,
        data: {
            teststep: step,
            inputs: JSON.stringify(inputsToSend)
        },
        files: files,
        authenticate: true
    })
  }

  prepareTestCaseDisplayActors(testCase: TestCaseDefinition, specificationId: number|undefined): Observable<ActorInfo[]> {
    let actorData: Observable<ActorInfo[]>
    if (specificationId == undefined) {
      actorData = of(testCase.actors.actor)
    } else {
      actorData = this.getActorDefinitions(specificationId).pipe(map((domainActors) => {
        for (let testCaseActor of testCase.actors.actor) {
          if (testCaseActor.name == undefined || testCaseActor.displayOrder == undefined) {
            // Lookup name and display order from domain data.
            const relevantDomainActor = find(domainActors, (actorDef) => actorDef.actorId == testCaseActor.id)
            if (relevantDomainActor != undefined) {
              if (testCaseActor.name == undefined) {
                testCaseActor.name = relevantDomainActor.name
              }
              if (testCaseActor.displayOrder == undefined && relevantDomainActor.displayOrder != undefined) {
                testCaseActor.displayOrder = relevantDomainActor.displayOrder
              }
            }
          }
        }
        return testCase.actors.actor
      }), share())
    }
    return actorData.pipe(
      mergeMap((actorDataToUse) => {
        actorDataToUse = actorDataToUse.sort((a, b) => {
          if (a.displayOrder == undefined && b.displayOrder == undefined) return 0
          else if (a.displayOrder != undefined && b.displayOrder == undefined) return -1
          else if (a.displayOrder == undefined && b.displayOrder != undefined) return 1
          else return Number(a.displayOrder) - Number(b.displayOrder)
        })
        return of(actorDataToUse)
      }), share()
    )
  }

}
