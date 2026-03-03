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

import {CanActivateFn} from '@angular/router';
import {RoutingService} from '../services/routing.service';
import {inject} from '@angular/core';
import {ProfileResolver} from './profile-resolver';
import {map} from 'rxjs/operators';

export const startRedirectResolver: CanActivateFn = (route, state) => {

  const profileResolver = inject(ProfileResolver);
  const routingService = inject(RoutingService);

  return profileResolver.resolveData(state).pipe(
    map(() => {
      return routingService.resolveStartPage()
    })
  )

}
