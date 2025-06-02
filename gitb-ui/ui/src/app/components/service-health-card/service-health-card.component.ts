import {Component, EventEmitter, Input, Output} from '@angular/core';
import {HealthCardInfo} from '../../types/health-card-info';
import {HealthStatus} from '../../types/health-status';
import {ServiceHealthCardComponentApi} from './service-health-card-component-api';
import {map, Subject} from 'rxjs';
import {BsModalService} from 'ngx-bootstrap/modal';
import {ServiceHealthModalComponent} from '../../modals/service-health-modal/service-health-modal.component';

@Component({
  selector: 'app-service-health-card',
  standalone: false,
  templateUrl: './service-health-card.component.html',
  styleUrl: './service-health-card.component.less'
})
export class ServiceHealthCardComponent implements ServiceHealthCardComponentApi{

  @Input() info!: HealthCardInfo
  @Output() updated = new EventEmitter<void>();

  hovering = false

  constructor(private modalService: BsModalService) {
  }

  showDetails() {
    if (this.info.info != undefined) {
      const modal = this.modalService.show(ServiceHealthModalComponent, {
        class: 'modal-lg',
        initialState: {
          serviceInfo: this.info
        }
      })
      modal.content?.updated.subscribe(() => {
        this.updated.emit()
      })
    }
  }

  checkStatus() {
    this.info.info = undefined
    const finished$ = new Subject<void>();
    this.info.checkFunction().pipe(
      map((result) => {
        this.info.info = result;
      })
    ).subscribe({
      next: () => finished$.next(),
      error: (error) => finished$.error(error),
      complete: () => finished$.complete()
    }).add(() => {
      if (this.info.info == undefined) {
        this.info.info = {
          status: HealthStatus.UNKNOWN,
          summary: "Unable to complete service healthcheck.",
          details: "An unexpected error occurred while trying to determine the service's health status."
        }
      }
    })
    return finished$.asObservable()
  }

}
