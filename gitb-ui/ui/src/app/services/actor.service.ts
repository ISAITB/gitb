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
import { RestService } from './rest.service';
import { BadgesInfo } from '../components/manage-badges/badges-info';
import { DataService } from './data.service';

@Injectable({
  providedIn: 'root'
})
export class ActorService {

  constructor(
    private readonly restService: RestService,
    private readonly dataService: DataService
  ) { }

  deleteActor(actorId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.ActorService.deleteActor(actorId).url,
      authenticate: true
    })
  }

  updateActor(id: number, actorId: string, name: string, description: string|undefined, reportMetadata: string|undefined, defaultActor: boolean|undefined, hiddenActor: boolean|undefined, displayOrder: number|undefined, domainId: number, specificationId: number, badges: BadgesInfo) {
    if (hiddenActor == undefined) hiddenActor = false
    if (defaultActor == undefined) defaultActor = false
    const data: any = {
      actor_id: actorId,
      name: name,
      description: description,
      metadata: reportMetadata,
      default: defaultActor,
      hidden: hiddenActor,
      domain_id: domainId,
      spec_id: specificationId
    }
    if (displayOrder != undefined) data.displayOrder = Number(displayOrder)
    const files = this.dataService.parametersForBadgeUpdate(badges, data)
    return this.restService.post<void>({
      path: ROUTES.controllers.ActorService.updateActor(id).url,
      authenticate: true,
      data: data,
      files: files
    })
  }

}
