import {HealthCardService} from './health-card-service';
import {Observable} from 'rxjs';
import {HealthInfo} from './health-info';

export interface HealthCardInfo {

  type: HealthCardService
  title: string
  info?: HealthInfo
  checkFunction: () => Observable<HealthInfo>

}
