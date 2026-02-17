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

import {AfterViewInit, Component, OnInit, QueryList, ViewChildren} from '@angular/core';
import {RoutingService} from '../../services/routing.service';
import {HealthCardInfo} from '../../types/health-card-info';
import {HealthStatus} from '../../types/health-status';
import {HealthCardService} from '../../types/health-card-service';
import {HealthCheckService} from '../../services/health-check.service';
import {finalize, forkJoin, Observable} from 'rxjs';
import {ServiceHealthCardComponentApi} from '../../components/service-health-card/service-health-card-component-api';
import {DataService} from '../../services/data.service';
import {MenuItem} from '../../types/menu-item.enum';
import {MenuItemStatus} from '../../types/menu-item-status.enum';
import {Constants} from '../../common/constants';
import {TestServiceBasicInfo} from '../../types/test-service-basic-info';
import {HealthInfo} from '../../types/health-info';
import {map} from 'rxjs/operators';
import {HealthOverview} from '../../components/service-health-card/health-overview';
import {ConformanceService} from '../../services/conformance.service';
import {MultiSelectConfig} from '../../components/multi-select-filter/multi-select-config';
import {Domain} from '../../types/domain';
import {FilterUpdate} from '../../components/test-filter/filter-update';

@Component({
  selector: 'app-service-health-dashboard',
  standalone: false,
  templateUrl: './service-health-dashboard.component.html',
  styleUrl: './service-health-dashboard.component.less'
})
export class ServiceHealthDashboardComponent implements OnInit, AfterViewInit {

  @ViewChildren("serviceCard") serviceCards?: QueryList<ServiceHealthCardComponentApi>;
  @ViewChildren("testServiceCard") testServiceCards?: QueryList<ServiceHealthCardComponentApi>;

  cards: HealthCardInfo[] = []
  checkAllPending = false
  checkTestServicesPending = false
  coreServiceOverview: HealthOverview = { status: HealthStatus.UNKNOWN }
  testServiceOverview: HealthOverview = { status: HealthStatus.UNKNOWN }
  selectedDomainId?: number
  communityId?: number
  testServicesStatus = {status: Constants.STATUS.NONE}
  testServiceCardData: HealthCardInfo[] = []
  protected readonly HealthStatus = HealthStatus;
  protected readonly Constants = Constants;
  domainSelectConfig?: MultiSelectConfig<Domain>

  constructor(
    private readonly routingService: RoutingService,
    private readonly healthCheckService: HealthCheckService,
    private readonly conformanceService: ConformanceService,
    protected readonly dataService: DataService
  ) {
  }

  ngOnInit(): void {
    this.domainSelectConfig = {
      name: 'domains',
      textField: 'fname',
      filterLabel: `All ${this.dataService.labelDomainsLower()}`,
      filterLabelIcon: Constants.BUTTON_ICON.DOMAIN,
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      searchPlaceholder: `Search ${this.dataService.labelDomainsLower()}...`,
      loader: () => this.conformanceService.getDomains()
    }
    /*
     * Core services
     */
    if (this.dataService.isSystemAdmin) {
      this.cards.push({
        type: HealthCardService.SOFTWARE_VERSION,
        title: "Software version updates",
        info: this.emptyHealthInfo(),
        checkFunction: () => this.healthCheckService.checkSoftwareVersion()
      })
      this.cards.push({
        type: HealthCardService.USER_INTERFACE,
        title: "User interface communications",
        info: this.emptyHealthInfo(),
        checkFunction: () => this.healthCheckService.checkUserInterfaceCommunication()
      })
      this.cards.push({
        type: HealthCardService.UI_SERVICE_COMMUNICATION,
        title: "Test engine communications",
        info: this.emptyHealthInfo(),
        checkFunction: () => this.healthCheckService.checkTestEngineCommunication()
      })
      this.cards.push({
        type: HealthCardService.HANDLER_CALLBACKS,
        title: "Test engine callbacks",
        info: this.emptyHealthInfo(),
        checkFunction: () => this.healthCheckService.checkTestEngineCallbacks()
      })
      this.cards.push({
        type: HealthCardService.ANTIVIRUS,
        title: "Antivirus scanning service",
        info: this.emptyHealthInfo(),
        checkFunction: () => this.healthCheckService.checkAntivirusService()
      })
      this.cards.push({
        type: HealthCardService.SMTP_SERVICE,
        title: "Email (SMTP) service",
        info: this.emptyHealthInfo(),
        checkFunction: () => this.healthCheckService.checkEmailService()
      })
      this.cards.push({
        type: HealthCardService.TSA_SERVICE,
        title: "Trusted timestamp service",
        info: this.emptyHealthInfo(),
        checkFunction: () => this.healthCheckService.checkTrustedTimestampService()
      })
      this.checkAllCoreServices()
    }
    /*
     * Community services
     */
    if (this.dataService.isCommunityAdmin) {
      this.communityId = this.dataService.community?.id
    }
    this.getTestServicesForHealthCheck()
    this.routingService.serviceHealthDashboardBreadcrumbs()
  }

  ngAfterViewInit(): void {
    if (this.dataService.isSystemAdmin) {
      this.checkAllCoreServices()
    }
  }

  checkAllCoreServices() {
    setTimeout(() => {
      if (this.serviceCards) {
        const tasks = this.serviceCards.map(serviceCard => {
          return serviceCard.checkStatus()
        })
        this.checkAllPending = true
        forkJoin(tasks).subscribe(() => {}).add(() => {
          this.updateStatus()
          this.checkAllPending = false
        })
      }
    })
  }

  checkAllTestServices() {
    setTimeout(() => {
      if (this.testServiceCards) {
        const tasks = this.testServiceCards.map(serviceCard => {
          return serviceCard.checkStatus()
        })
        this.checkTestServicesPending = true
        forkJoin(tasks).subscribe(() => {}).add(() => {
          this.updateTestServiceStatus()
          this.checkTestServicesPending = false
        })
      }
    })
  }

  private serviceTypeCardIcon(serviceType: number): string {
    switch (serviceType) {
      case Constants.TEST_SERVICE_TYPE.MESSAGING: return 'fa-solid fa-paper-plane';
      case Constants.TEST_SERVICE_TYPE.VALIDATION: return 'fa-solid fa-award';
      default: return 'fa-solid fa-gear';
    }
  }

  private getTestServicesForHealthCheck() {
    let services$: Observable<TestServiceBasicInfo[]>
    if (this.communityId != undefined) {
      services$ = this.healthCheckService.getCommunityTestServicesForHealthCheck(this.communityId, this.selectedDomainId)
    } else {
      services$ = this.healthCheckService.getTestServicesForHealthCheck(this.selectedDomainId)
    }
    this.testServicesStatus.status = Constants.STATUS.PENDING
    services$.pipe(
      map(data => {
        this.testServiceCardData = data.map(service => {
          return {
            id: service.id,
            type: HealthCardService.CUSTOM_TEST_SERVICE,
            icon: this.serviceTypeCardIcon(service.serviceType),
            info: this.emptyHealthInfo(),
            title: service.serviceName,
            subtitle: service.domainName,
            checkFunction: () => this.checkTestService(service.id, service.domainId)
          }
        })
      }),
      finalize(() => {
        this.testServicesStatus.status = Constants.STATUS.FINISHED
      })
    ).subscribe(() => {})
  }

  checkTestService(serviceId: number, domainId: number): Observable<HealthInfo> {
    return this.healthCheckService.testTestServiceById(serviceId, domainId)
  }

  cardUpdated() {
    this.updateStatus()
  }

  testServiceCardUpdated() {
    this.updateTestServiceStatus()
  }

  private updateOverview(cards: HealthCardInfo[], overview: HealthOverview, serviceLabel: string): HealthStatus {
    let errorCount = 0
    let warningCount = 0
    let infoCount = 0
    let okCount = 0
    let unknownCount = 0
    cards.forEach((card) => {
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
      overview.message = `One or more ${serviceLabel} service checks reported failures. Click the relevant cards for more details.`
      overview.status = HealthStatus.ERROR
    } else if (warningCount > 0) {
      overview.message = `One or more ${serviceLabel} service checks reported warnings. Click the relevant cards for more details.`
      overview.status = HealthStatus.WARNING
    } else if (infoCount > 0) {
      overview.message = `All ${serviceLabel} services are working correctly with additional information available. Click the relevant cards for more details.`
      overview.status = HealthStatus.INFO
    } else if (okCount == cards.length) {
      overview.message = `All ${serviceLabel} services are working correctly. Click the relevant cards for more details.`
      overview.status = HealthStatus.OK
    } else {
      overview.message = `It was not possible to determine the ${serviceLabel} service health status.`
      overview.status = HealthStatus.INFO
    }
    return overview.status
  }

  private updateMenuIndicator() {
    if (this.coreServiceOverview.status == HealthStatus.ERROR) {
      this.dataService.updateMenuItemStatus(MenuItem.serviceHealthDashboard, MenuItemStatus.Error)
    } else if (this.coreServiceOverview.status == HealthStatus.WARNING) {
      this.dataService.updateMenuItemStatus(MenuItem.serviceHealthDashboard, MenuItemStatus.Warning)
    } else {
      this.dataService.updateMenuItemStatus(MenuItem.serviceHealthDashboard, MenuItemStatus.None)
    }
  }

  private updateStatus() {
    this.updateOverview(this.cards, this.coreServiceOverview, "core")
    this.updateMenuIndicator()
  }

  private updateTestServiceStatus() {
    this.updateOverview(this.testServiceCardData, this.testServiceOverview, "custom test")
    this.updateMenuIndicator()
  }

  domainChanged(event?: FilterUpdate<Domain>) {
    if (event?.values.active.length == 1) {
      this.selectedDomainId = event.values.active[0].id
    } else {
      this.selectedDomainId = undefined
    }
    this.emptyHealthOverview(this.testServiceOverview)
    this.getTestServicesForHealthCheck()
  }

  private emptyHealthOverview(overview: HealthOverview): void {
    overview.status = HealthStatus.UNKNOWN
    overview.message = undefined
  }

  private emptyHealthInfo(): HealthInfo {
    return {
      status: HealthStatus.UNKNOWN,
      summary: 'The custom test service has not been tested.',
      details: ''
    }
  }

}
