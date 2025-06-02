import { Component, Input } from '@angular/core';
import {HealthStatus} from '../../types/health-status';

@Component({
  selector: 'app-service-health-icon',
  standalone: false,
  templateUrl: './service-health-icon.component.html',
  styleUrl: './service-health-icon.component.less'
})
export class ServiceHealthIconComponent {

  @Input() status: HealthStatus|undefined

  protected readonly HealthStatus = HealthStatus;
}
