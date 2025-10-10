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

import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { FileParam } from '../types/file-param.type';
import { System } from '../types/system';
import { SystemParameter } from '../types/system-parameter';
import { DataService } from './data.service';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class SystemService {

  constructor(
    private readonly restService: RestService,
    private readonly dataService: DataService
  ) { }

  getSystemsByOrganisation(orgId: number, snapshotId?: number, checkIfHasTests?: boolean) {
    let params: any = {
      organization_id: orgId
    }
    if (snapshotId !== undefined) {
      params.snapshot = snapshotId
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

  getSystemById(systemId: number) {
    return this.restService.get<System>({
      path: ROUTES.controllers.SystemService.getSystemById(systemId).url,
      authenticate: true
    })
  }

  getSystem(systemId: number) {
    return this.restService.get<System>({
      path: ROUTES.controllers.SystemService.getSystemProfile(systemId).url,
      authenticate: true
    })
  }

  searchSystems(communityIds: number[]|undefined, organisationIds: number[]|undefined, snapshotId?: number) {
		const data: any = {}
		if (communityIds && communityIds.length > 0) {
		  data["community_ids"] = communityIds.join(',')
		}
		if (organisationIds && organisationIds.length > 0) {
		  data["organization_ids"] = organisationIds.join(',')
		}
    if (snapshotId != undefined) {
      data["snapshot"] = snapshotId
    }
    return this.restService.post<System[]>({
      path: ROUTES.controllers.SystemService.searchSystems().url,
      authenticate: true,
      data: data
    })
  }

  searchSystemsInCommunity(communityId: number, organisationIds: number[]|undefined, snapshotId?: number) {
		const data: any = {
      community_id: communityId
    }
		if (organisationIds && organisationIds.length > 0) {
		  data["organization_ids"] = organisationIds.join(',')
		}
    if (snapshotId != undefined) {
      data['snapshot'] = snapshotId
    }
    return this.restService.post<System[]>({
      path: ROUTES.controllers.SystemService.searchSystemsInCommunity().url,
      authenticate: true,
      data: data
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

  getSystemParameterValues(systemId: number, onlySimple?: boolean) {
    let params = undefined
    if (onlySimple != undefined) {
      params = {
        simple: onlySimple
      }
    }
    return this.restService.get<SystemParameter[]>({
      path: ROUTES.controllers.SystemService.getSystemParameterValues(systemId).url,
      authenticate: true,
      params: params
    })
  }

  downloadSystemParameterFile(systemId: number, parameterId: number) {
		return this.restService.get<ArrayBuffer>({
			path: ROUTES.controllers.SystemService.downloadSystemParameterFile(systemId, parameterId).url,
			authenticate: true,
			arrayBuffer: true
		})
  }

  updateSystem(systemId: number, sname: string, fname: string, description: string|undefined, version: string|undefined, organisationId: number, otherSystem: number|undefined, processProperties: boolean, properties: SystemParameter[], copySystemParameters: boolean, copyStatementParameters: boolean) {
    const data: any = {
      system_sname: sname,
      system_fname: fname
    }
    if (version != undefined) {
      data.system_version = version
    }
    if (description != undefined) {
      data.system_description = description
    }
    if (otherSystem) {
      data.other_system = otherSystem
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }
    let files: FileParam[]|undefined
    if (processProperties) {
      const props = this.dataService.customPropertiesForPost(properties)
      data.properties = props.parameterJson
      files = props.files
    }
    data.organization_id = organisationId
    return this.restService.post<void>({
      path: ROUTES.controllers.SystemService.updateSystemProfile(systemId).url,
      data: data,
      files: files,
      authenticate: true
    })
  }

  registerSystemWithOrganisation(sname: string, fname: string, description: string|undefined, version: string|undefined, orgId: number, otherSystem: number|undefined, processProperties: boolean, properties: SystemParameter[], copySystemParameters: boolean, copyStatementParameters: boolean) {
    const data: any = {
      system_sname: sname,
      system_fname: fname,
      organization_id: orgId
    }
    if (version != undefined) {
      data.system_version = version
    }
    if (otherSystem) {
      data.other_system = otherSystem
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }
    if (description != undefined) {
      data.system_description = description
    }
    let files: FileParam[]|undefined
    if (processProperties) {
      const props = this.dataService.customPropertiesForPost(properties)
      data.properties = props.parameterJson
      files = props.files
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.SystemService.registerSystemWithOrganization().url,
      data: data,
      files: files,
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

  defineConformanceStatements(system: number, actorIds: Set<number>) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SystemService.defineConformanceStatements(system).url,
      data: {
        ids: [...actorIds].join(',')
      },
      authenticate: true
    })
  }

  checkSystemParameterValues(systemId: number) {
    return this.restService.get<SystemParameter[]>({
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

  downloadEndpointConfigurationFile(systemId: number, parameterId: number) {
		return this.restService.get<ArrayBuffer>({
			path: ROUTES.controllers.SystemService.downloadEndpointConfigurationFile().url,
			authenticate: true,
      params: {
        system_id: systemId,
        parameter_id: parameterId
      },
			arrayBuffer: true
		})
  }

  updateSystemApiKey(systemId: number) {
    return this.restService.post<string>({
      path: ROUTES.controllers.SystemService.updateSystemApiKey(systemId).url,
      authenticate: true,
      text: true
    })
  }

  ownSystemHasTests(systemId: number) {
    return this.restService.get<{hasTests: boolean}>({
      path: ROUTES.controllers.SystemService.ownSystemHasTests(systemId).url,
      authenticate: true
    })
  }

}
