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

import {AfterViewInit, Component, OnInit, QueryList, ViewChildren} from '@angular/core';
import {RoutingService} from '../../services/routing.service';
import {HealthCardInfo} from '../../types/health-card-info';
import {HealthStatus} from '../../types/health-status';
import {HealthCardService} from '../../types/health-card-service';
import {HealthCheckService} from '../../services/health-check.service';
import {forkJoin} from 'rxjs';
import {ServiceHealthCardComponentApi} from '../../components/service-health-card/service-health-card-component-api';

@Component({
  selector: 'app-service-health-dashboard',
  standalone: false,
  templateUrl: './service-health-dashboard.component.html',
  styleUrl: './service-health-dashboard.component.less'
})
export class ServiceHealthDashboardComponent implements OnInit, AfterViewInit {

  @ViewChildren("serviceCard") serviceCards?: QueryList<ServiceHealthCardComponentApi>;

  cards!: HealthCardInfo[]
  checkAllPending = false
  overviewMessage?: string
  overviewHealth?: HealthStatus
  protected readonly HealthStatus = HealthStatus;

  constructor(
    private routingService: RoutingService,
    private healthCheckService: HealthCheckService) {
  }

  ngOnInit(): void {
    this.cards = []
    this.cards.push({
      type: HealthCardService.USER_INTERFACE,
      title: "User interface communications",
      checkFunction: () => this.healthCheckService.checkUserInterfaceCommunication()
    })
    this.cards.push({
      type: HealthCardService.UI_SERVICE_COMMUNICATION,
      title: "Test engine communications",
      checkFunction: () => this.healthCheckService.checkTestEngineCommunication()
    })
    this.cards.push({
      type: HealthCardService.HANDLER_CALLBACKS,
      title: "Test engine callbacks",
      checkFunction: () => this.healthCheckService.checkTestEngineCallbacks()
    })
    this.cards.push({
      type: HealthCardService.ANTIVIRUS,
      title: "Antivirus scanning service",
      checkFunction: () => this.healthCheckService.checkAntivirusService()
    })
    this.cards.push({
      type: HealthCardService.SMTP_SERVICE,
      title: "Email (SMTP) service",
      checkFunction: () => this.healthCheckService.checkEmailService()
    })
    this.cards.push({
      type: HealthCardService.TSA_SERVICE,
      title: "Trusted timestamp service",
      checkFunction: () => this.healthCheckService.checkTrustedTimestampService()
    })
    this.checkAllServices()
    this.routingService.serviceHealthDashboardBreadcrumbs()
  }

  ngAfterViewInit(): void {
    this.checkAllServices()
  }

  checkAllServices() {
    setTimeout(() => {
      if (this.serviceCards) {
        const tasks = this.serviceCards.map(serviceCard => {
          return serviceCard.checkStatus()
        })
        this.checkAllPending = true
        forkJoin(tasks).subscribe(() => {
          this.updateBanner()
        }).add(() => {
          this.checkAllPending = false
        })
      }
    })
  }

  cardUpdated() {
    this.updateBanner()
  }

  updateBanner() {
    let errorCount = 0
    let warningCount = 0
    let infoCount = 0
    let okCount = 0
    let unknownCount = 0
    this.cards.forEach(card => {
      if (card.info) {
        switch (card.info.status) {
          case HealthStatus.ERROR: errorCount += 1; break;
          case HealthStatus.WARNING: warningCount += 1; break;
          case HealthStatus.INFO: infoCount += 1; break;
          case HealthStatus.OK: okCount += 1; break;
          default: unknownCount += 1; break;
        }
      }
    })
    if (errorCount > 0) {
      this.overviewMessage = "One or more service checks reported failures. Click the relevant cards for more details."
      this.overviewHealth = HealthStatus.ERROR
    } else if (warningCount > 0) {
      this.overviewMessage = "One or more service checks reported warnings. Click the relevant cards for more details."
      this.overviewHealth = HealthStatus.WARNING
    } else if (infoCount > 0) {
      this.overviewMessage = "All services are working correctly with additional information available. Click the relevant cards for more details."
      this.overviewHealth = HealthStatus.INFO
    } else if (okCount == this.cards.length) {
      this.overviewMessage = "All services are working correctly. Click the relevant cards for more details."
      this.overviewHealth = HealthStatus.OK
    } else {
      this.overviewMessage = "It was not possible to determine the status of internal services."
      this.overviewHealth = HealthStatus.INFO
    }

  }

}
