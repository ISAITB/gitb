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
import {NextObserver} from 'rxjs';
import {webSocket} from 'rxjs/webSocket';
import {ROUTES} from '../common/global';
import {Utils} from '../common/utils';
import {DataService} from './data.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  constructor(private readonly dataService: DataService) { }

  prepareWebSocket<T>(webSocketRoute: any, openObserver: NextObserver<Event>, closeObserver: NextObserver<CloseEvent>) {
    const internalAddressWithServerPart = webSocketRoute.webSocketURL()
    const pathPart = webSocketRoute.url
    const serverPart = internalAddressWithServerPart.substring(0, internalAddressWithServerPart.length - pathPart.length)
    const urlToUse = Utils.webSocketURL(serverPart + this.dataService.completePath(pathPart))
    return webSocket({
      url: urlToUse,
      openObserver: openObserver,
      closeObserver: closeObserver
    })
  }

  connect(openObserver: NextObserver<Event>, closeObserver: NextObserver<CloseEvent>) {
    return this.prepareWebSocket(ROUTES.controllers.WebSocketService.socket("session"), openObserver, closeObserver)
  }
}
