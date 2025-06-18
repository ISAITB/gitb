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
import {Observable, Subject, timeout} from 'rxjs';
import {HealthInfo} from '../types/health-info';
import {RestService} from './rest.service';
import {ROUTES} from '../common/global';
import {WebSocketService} from './web-socket.service';
import {WebSocketSubject} from 'rxjs/webSocket';

@Injectable({
  providedIn: 'root'
})
export class HealthCheckService {

  constructor(
    private restService: RestService,
    private webSocketService: WebSocketService
  ) {
  }

  checkTestEngineCallbacks(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkTestEngineCallbacks().url,
      authenticate: true,
    })
  }

  checkAntivirusService(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkAntivirusService().url,
      authenticate: true,
    })
  }

  checkEmailService(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkEmailService().url,
      authenticate: true,
    })
  }

  checkTrustedTimestampService(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkTrustedTimestampService().url,
      authenticate: true,
    })
  }

  checkTestEngineCommunication(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkTestEngineCommunication().url,
      authenticate: true,
    })
  }

  checkUserInterfaceCommunicationSuccessDetails(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkUserInterfaceCommunicationSuccessDetails().url,
      authenticate: true,
    })
  }

  checkUserInterfaceCommunicationErrorDetails(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkUserInterfaceCommunicationErrorDetails().url,
      authenticate: true,
    })
  }

  checkUserInterfaceCommunication(): Observable<HealthInfo> {
    try {
      const finished$ = new Subject<HealthInfo>();
      let socket: WebSocketSubject<any>|undefined
      /* The following configuration can be used to test this for errors:
            const testData = {
              webSocketURL: () => "ws://localhost:9001/api/health/ws",
              url: "api/health/ws"
            }
            socket = this.webSocketService.prepareWebSocket(testData,
       */
      socket = this.webSocketService.prepareWebSocket(ROUTES.controllers.HealthCheckService.checkUserInterfaceCommunication(),
        { next: () => {} },
        { next: () => {} }
      )
      socket.subscribe({
        next: (response: any) => {
          let healthStatus$: Observable<HealthInfo>
          if (response?.msg == "OK") {
            healthStatus$ = this.checkUserInterfaceCommunicationSuccessDetails()
          } else {
            healthStatus$ = this.checkUserInterfaceCommunicationErrorDetails()
          }
          healthStatus$.subscribe((msg) => {
            finished$.next(msg)
            finished$.complete()
          })
        },
        error: () => {
          this.checkUserInterfaceCommunicationErrorDetails().subscribe((msg) => {
            finished$.next(msg)
            finished$.complete()
          })
        },
        complete: () => {
          // Do nothing
        }
      })
      /*
       * We send a "test" text and expect to get a response of OK (anything else is considered a failure.
       * There is no need to close the socket (the server closes it immediately after sending a response)
       */
      socket.next({ msg: "test" })
      return finished$.pipe(
        // Give the operation 10 seconds, otherwise complete with an error (retrieved from backend)
        timeout({each: 10000, with: () => this.checkUserInterfaceCommunicationErrorDetails()})
      );
    } catch (error) {
      return this.checkUserInterfaceCommunicationErrorDetails()
    }
  }
}
