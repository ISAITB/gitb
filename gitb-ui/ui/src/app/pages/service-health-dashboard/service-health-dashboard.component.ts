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
    this.cards.forEach(card => {
      if (card.info) {
        switch (card.info.status) {
          case HealthStatus.ERROR: errorCount += 1; break;
          case HealthStatus.WARNING: warningCount += 1; break;
          case HealthStatus.INFO: infoCount += 1; break;
          case HealthStatus.OK: okCount += 1; break;
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
    } else {
      this.overviewMessage = "All service are working correctly. Click the relevant cards for more details."
      this.overviewHealth = HealthStatus.OK
    }

  }

}
