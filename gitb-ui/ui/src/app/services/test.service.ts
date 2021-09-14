import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { Actor } from '../types/actor';
import { ConfigureResponse } from '../types/configure-response';
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

  startHeadlessTestSessions(testCaseIds: number[], specId: number, systemId: number, actorId: number) {
    const data: any = {
      spec_id: specId,
      system_id: systemId,
      actor_id: actorId
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
    for (let input of inputs) {
      const inputToSend: any = {
        id: input.id,
        name: input.name,
        type: input.type,
        embeddingMethod: input.embeddingMethod
      }
      if (inputToSend.embeddingMethod == 'BASE64') {
        inputToSend.valueBinary = input.value
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
        authenticate: true
    })
  }

}
