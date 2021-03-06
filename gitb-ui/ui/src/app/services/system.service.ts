import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { ConformanceConfiguration } from '../pages/organisation/conformance-statement/conformance-configuration';
import { BinaryMetadata } from '../types/binary-metadata';
import { ConformanceStatement } from '../types/conformance-statement';
import { ErrorDescription } from '../types/error-description';
import { System } from '../types/system';
import { SystemParameter } from '../types/system-parameter';
import { SystemParameterWithValue } from '../types/system-parameter-with-value';
import { DataService } from './data.service';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class SystemService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  getSystemsByOrganisation(orgId: number, checkIfHasTests?: boolean) {
    let params: any = {
      organization_id: orgId
    }
    if (checkIfHasTests !== undefined) {
      params.check_has_tests = checkIfHasTests
    }
    return this.restService.get<System[]>({
      path: ROUTES.controllers.SystemService.getSystemsByOrganization().url,
      authenticate: true,
      params: params
    })
  }

  getSystem(systemId: number) {
    return this.restService.get<System>({
      path: ROUTES.controllers.SystemService.getSystemProfile(systemId).url,
      authenticate: true
    })
  }

  getSystems(systemIds?: number[]) {
    let params: any = {}
    if (systemIds !== undefined && systemIds.length > 0) {
      params.ids = systemIds.join(',')
    }
    return this.restService.get<System[]>({
      path: ROUTES.controllers.SystemService.getSystems().url,
      authenticate: true,
      params: params
    })
  }

  getSystemsByCommunity() {
    return this.restService.get<System[]>({
      path: ROUTES.controllers.SystemService.getSystemsByCommunity(this.dataService.community!.id).url,
      authenticate: true
    })
  }

  getSystemParameterValues(systemId: number) {
    return this.restService.get<SystemParameter[]>({
      path: ROUTES.controllers.SystemService.getSystemParameterValues(systemId).url,
      authenticate: true
    })
  }

  updateSystem(systemId: number, sname: string, fname: string, description: string|undefined, version: string, organisationId: number, otherSystem: number|undefined, processProperties: boolean, properties: SystemParameter[], copySystemParameters: boolean, copyStatementParameters: boolean) {
    const data: any = {
      system_sname: sname,
      system_fname: fname,
      system_version: version
    }
    if (description != undefined) {
      data.system_description = description
    }
    if (otherSystem != undefined) {
      data.other_system = otherSystem
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }
    if (processProperties) {
      data.properties = this.dataService.customPropertiesForPost(properties)
    }
    data.organization_id = organisationId
    return this.restService.post<void>({
      path: ROUTES.controllers.SystemService.updateSystemProfile(systemId).url,
      data: data,
      authenticate: true
    })
  }

  registerSystemWithOrganisation(sname: string, fname: string, description: string|undefined, version: string, orgId: number, otherSystem: number|undefined, processProperties: boolean, properties: SystemParameter[], copySystemParameters: boolean, copyStatementParameters: boolean) {
    const data: any = {
      system_sname: sname,
      system_fname: fname,
      system_version: version,
      organization_id: orgId
    }
    if (otherSystem != undefined) {
      data.other_system = otherSystem
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }
    if (description != undefined) {
      data.system_description = description
    }
    if (processProperties) {
      data.properties = this.dataService.customPropertiesForPost(properties)
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.SystemService.registerSystemWithOrganization().url,
      data: data,
      authenticate: true
    })
  }

  deleteSystem(systemId: number, organisationId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.SystemService.deleteSystem(systemId).url,
      params: {
        organization_id: organisationId
      },
      authenticate: true
    })
  }

  getConformanceStatements(system: number, specId?: number, actorId?: number) {
    if (actorId != undefined && specId != undefined) {
      return this.restService.get<ConformanceStatement[]>({
        path: ROUTES.controllers.SystemService.getConformanceStatements(system).url,
        authenticate: true,
        params: {
          spec: specId,
          actor: actorId
        }
      })
    } else {
      return this.restService.get<ConformanceStatement[]>({
        path: ROUTES.controllers.SystemService.getConformanceStatements(system).url,
        authenticate: true
      })
    }
  }

  defineConformanceStatement(system: number, spec: number, actor: number) {
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.SystemService.defineConformanceStatement(system).url,
      data: {
        spec: spec,
        actor: actor
      },
      authenticate: true
    })
  }

  checkSystemParameterValues(systemId: number) {
    return this.restService.get<SystemParameterWithValue[]>({
      path: ROUTES.controllers.SystemService.checkSystemParameterValues(systemId).url,
      authenticate: true
    })
  }

  deleteConformanceStatement(systemId: number, actorIds: number[]) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.SystemService.deleteConformanceStatement(systemId).url,
      authenticate: true,
      params: {
        ids: actorIds.join(',')
      }
    })
  }

  deleteEndpointConfiguration(systemId: number, parameterId: number, endpointId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.SystemService.deleteEndpointConfiguration(endpointId).url,
      authenticate: true,
      params: {
        system_id: systemId,
        parameter_id: parameterId
      }
    })
  }

  saveEndpointConfiguration(endpoint: number, config: ConformanceConfiguration, isBinary?: boolean) {
    const configToSend: any = {
      system: config.system,
      parameter: config.parameter,
      endpoint: config.endpoint
    }
    if (isBinary != undefined && isBinary) {
      configToSend.valueBinary = config.value
    } else {
      configToSend.value = config.value
    }
    return this.restService.post<BinaryMetadata|undefined>({
      path: ROUTES.controllers.SystemService.saveEndpointConfiguration(endpoint).url,
      authenticate: true,
      data: {
        config: JSON.stringify(configToSend)
      }
    })
  }

}
