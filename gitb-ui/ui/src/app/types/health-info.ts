import {HealthStatus} from './health-status';

export interface HealthInfo {

  status: HealthStatus
  summary: string
  details: string

}
