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

import {Injectable} from '@angular/core';
import {Observable, Subject, timeout} from 'rxjs';
import {HealthInfo} from '../types/health-info';
import {RestService} from './rest.service';
import {ROUTES} from '../common/global';
import {DataService} from './data.service';
import {HealthStatus} from '../types/health-status';
import {TestServiceBasicInfo} from '../types/test-service-basic-info';

@Injectable({
  providedIn: 'root'
})
export class HealthCheckService {

  // The minimum delay (in ms) between the health check's two server-sent events for the response to be considered as streamed.
  private static readonly MIN_STREAMING_DELAY = 1500

  constructor(
    private readonly restService: RestService,
    private readonly dataService: DataService
  ) { }

  getTestServicesForHealthCheck(domainId: number|undefined): Observable<TestServiceBasicInfo[]> {
    return this.restService.get<TestServiceBasicInfo[]>({
      path: ROUTES.controllers.DomainParameterService.getTestServicesForHealthCheck().url,
      authenticate: true,
      params: {
        domain: domainId
      }
    })
  }

  getCommunityTestServicesForHealthCheck(communityId: number, domainId: number|undefined): Observable<TestServiceBasicInfo[]> {
    return this.restService.get<TestServiceBasicInfo[]>({
      path: ROUTES.controllers.DomainParameterService.getCommunityTestServicesForHealthCheck(communityId).url,
      authenticate: true,
      params: {
        domain: domainId
      }
    })
  }

  runPostLoginChecks(): Observable<HealthStatus> {
    return this.restService.get<HealthStatus>({
      path: ROUTES.controllers.HealthCheckService.runPostLoginChecks().url,
      authenticate: true,
    })
  }

  checkTestEngineCallbacks(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkTestEngineCallbacks().url,
      authenticate: true,
    })
  }

  checkTestServiceCallbacks(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkTestServiceCallbacks().url,
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

  checkSoftwareVersion(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkSoftwareVersion().url,
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

  checkUserInterfaceCommunicationBufferedDetails(): Observable<HealthInfo> {
    return this.restService.get<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.checkUserInterfaceCommunicationBufferedDetails().url,
      authenticate: true,
    })
  }

  checkUserInterfaceCommunication(): Observable<HealthInfo> {
    try {
      const finished$ = new Subject<HealthInfo>()
      let finished = false
      let checkTime: number|undefined
      const eventSource = new EventSource(this.dataService.completePath(ROUTES.controllers.HealthCheckService.checkUserInterfaceCommunication().url))
      const finish = (healthStatus$: Observable<HealthInfo>) => {
        if (!finished) {
          finished = true
          eventSource.close()
          healthStatus$.subscribe((msg) => {
            finished$.next(msg)
            finished$.complete()
          })
        }
      }
      /*
       * The server sends a "check" event immediately and a "done" event a few seconds later, keeping the stream open in between.
       * If the events arrive with a delay between them the communication is working. If they arrive together, an intermediate
       * component (e.g. a reverse proxy) is buffering the response, which would also delay the updates of running test sessions.
       * Any error (or the lack of a response) is considered a failure.
       */
      eventSource.addEventListener('check', () => {
        checkTime = Date.now()
      })
      eventSource.addEventListener('done', () => {
        if (checkTime != undefined && (Date.now() - checkTime) >= HealthCheckService.MIN_STREAMING_DELAY) {
          finish(this.checkUserInterfaceCommunicationSuccessDetails())
        } else {
          finish(this.checkUserInterfaceCommunicationBufferedDetails())
        }
      })
      eventSource.onerror = () => {
        finish(this.checkUserInterfaceCommunicationErrorDetails())
      }
      return finished$.pipe(
        // Give the operation 10 seconds, otherwise complete with an error (retrieved from backend)
        timeout({each: 10000, with: () => {
          finished = true
          eventSource.close()
          return this.checkUserInterfaceCommunicationErrorDetails()
        }})
      );
    } catch (error) {
      return this.checkUserInterfaceCommunicationErrorDetails()
    }
  }

  testTestServiceById(serviceId: number, domainId: number) {
    return this.restService.post<HealthInfo>({
      path: ROUTES.controllers.HealthCheckService.testTestServiceById(domainId).url,
      authenticate: true,
      data: {
        id: serviceId
      }
    })
  }

}
