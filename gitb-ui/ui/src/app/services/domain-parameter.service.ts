/*
 * Copyright (C) 2025 European Union
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

import {Injectable} from '@angular/core';
import {ROUTES} from '../common/global';
import {RestService} from './rest.service';
import {DomainParameter} from '../types/domain-parameter';
import {ErrorDescription} from '../types/error-description';
import {FileParam} from '../types/file-param.type';
import {TestServiceWithParameter} from '../types/test-service-with-parameter';
import {Id} from '../types/id';
import {ServiceCallResult} from '../types/service-call-result';

@Injectable({
  providedIn: 'root'
})
export class DomainParameterService {

  constructor(
    private readonly restService: RestService
  ) { }

  getDomainParameters(domainId: number, loadValues?: boolean, onlySimple?: boolean) {
    const params: any = {}
    if (loadValues != undefined) {
      params.values = loadValues
    }
    if (onlySimple != undefined) {
      params.simple = onlySimple
    }
    return this.restService.get<DomainParameter[]>({
      path: ROUTES.controllers.DomainParameterService.getDomainParameters(domainId).url,
      authenticate: true,
      params: params
    })
  }

  getDomainParametersOfCommunity(communityId: number, loadValues?: boolean, onlySimple?: boolean) {
    const params: any = {}
    if (loadValues != undefined) {
      params.values = loadValues
    }
    if (onlySimple != undefined) {
      params.simple = onlySimple
    }
    return this.restService.get<DomainParameter[]>({
      path: ROUTES.controllers.DomainParameterService.getDomainParametersOfCommunity(communityId).url,
      authenticate: true,
      params: params
    })
  }

  downloadDomainParameterFile(domainId: number, domainParameterId: number) {
    return this.restService.get<ArrayBuffer>({
      path: ROUTES.controllers.DomainParameterService.downloadDomainParameterFile(domainId, domainParameterId).url,
      authenticate: true,
      arrayBuffer: true
    })
  }

  updateDomainParameter(domainParameterId: number, domainParameterName: string, domainParameterDescription: string|undefined, domainParameterValue: string|File|undefined, domainParameterKind: string, inTests: boolean|undefined, domainId: number) {
    const params: any = {
      name: domainParameterName,
      kind: domainParameterKind
    }
    if (domainParameterDescription != undefined) {
      params.desc = domainParameterDescription
    }
    if (inTests != undefined) {
      params.inTests = inTests
    } else {
      params.inTests = false
    }
    let files: FileParam[]|undefined
    if (domainParameterKind == 'BINARY') {
      if (domainParameterValue != undefined) {
        params.contentType = (domainParameterValue as File).type
        files = [{param: 'file', data: domainParameterValue as File}]
      }
    } else {
      params.value = domainParameterValue
    }
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.DomainParameterService.updateDomainParameter(domainId, domainParameterId).url,
      authenticate: true,
      data: {
        config: JSON.stringify(params)
      },
      files: files
    })
  }

  createDomainParameter(domainParameterName: string, domainParameterDescription: string|undefined, domainParameterValue: string|File, domainParameterKind: string, inTests: boolean|undefined, domainId: number) {
    const params: any = {
      name: domainParameterName,
      kind: domainParameterKind
    }
    if (domainParameterDescription != undefined) {
      params.desc = domainParameterDescription
    }
    if (inTests != undefined) {
      params.inTests = inTests
    } else {
      params.inTests = false
    }
    let files: FileParam[]|undefined
    if (domainParameterKind == 'BINARY') {
      params.contentType = (domainParameterValue as File).type
      files = [{param: 'file', data: domainParameterValue as File}]
    } else {
      params.value = domainParameterValue as string
    }
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.DomainParameterService.createDomainParameter(domainId).url,
      authenticate: true,
      data: {
        config: JSON.stringify(params)
      },
      files: files
    })
  }

  deleteDomainParameter(domainParameterId: number, domainId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.DomainParameterService.deleteDomainParameter(domainId, domainParameterId).url,
      authenticate: true
    })
  }

  getTestServices(domainId: number) {
    return this.restService.get<TestServiceWithParameter[]>({
      path: ROUTES.controllers.DomainParameterService.getTestServices(domainId).url,
      authenticate: true
    })
  }

  getTestService(domainId: number, serviceId: number) {
    return this.restService.get<TestServiceWithParameter>({
      path: ROUTES.controllers.DomainParameterService.getTestService(domainId, serviceId).url,
      authenticate: true
    })
  }

  createTestService(data: TestServiceWithParameter, domainId: number, updateExistingParameter: boolean) {
    return this.restService.post<ErrorDescription|Id|void>({
      path: ROUTES.controllers.DomainParameterService.createTestService(domainId).url,
      authenticate: true,
      data: this.toTestServicePayload(data, updateExistingParameter)
    })
  }

  private toTestServicePayload(data: TestServiceWithParameter, updateExistingParameter?: boolean) {
    const payload: any = {
      id: data.service.id,
      serviceType: data.service.serviceType,
      apiType: data.service.apiType,
      identifier: data.service.identifier,
      version: data.service.version,
      name: data.parameter.name,
      description: data.parameter.description,
      value: data.parameter.value,
      parameter: data.parameter.id,
      authBasicUsername : data.service.authBasicUsername,
      authBasicPassword : data.service.authBasicPassword,
      authTokenPassword : data.service.authTokenPassword,
      authTokenUsername : data.service.authTokenUsername,
      authTokenPasswordType : data.service.authTokenPasswordType
    }
    if (updateExistingParameter != undefined) {
      payload.update = updateExistingParameter
    }
    return payload
  }

  updateTestService(data: TestServiceWithParameter, domainId: number, updateExistingParameter: boolean) {
    return this.restService.post<ErrorDescription|Id|void>({
      path: ROUTES.controllers.DomainParameterService.updateTestService(domainId, data.service.id).url,
      authenticate: true,
      data: this.toTestServicePayload(data, updateExistingParameter)
    })
  }

  deleteTestService(domainId: number, serviceId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.DomainParameterService.deleteTestService(domainId, serviceId).url,
      authenticate: true
    })
  }

  getAvailableDomainParametersForTestServiceConversion(domainId: number) {
    return this.restService.get<DomainParameter[]>({
      path: ROUTES.controllers.DomainParameterService.getAvailableDomainParametersForTestServiceConversion(domainId).url,
      authenticate: true
    })
  }

  testTestService(data: TestServiceWithParameter, domainId: number) {
    return this.restService.post<ServiceCallResult>({
      path: ROUTES.controllers.DomainParameterService.testTestService(domainId).url,
      authenticate: true,
      data: this.toTestServicePayload(data)
    })
  }

}
