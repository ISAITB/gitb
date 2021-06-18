import { Injectable } from '@angular/core';
import { RestService } from './rest.service'
import { ROUTES } from '../common/global';
import { SystemConfiguration } from '../types/system-configuration';
import { ErrorDescription } from '../types/error-description';

@Injectable({
  providedIn: 'root'
})
export class SystemConfigurationService {


  constructor(private restService: RestService) { }

  getSessionAliveTime() {
    return this.restService.get<SystemConfiguration>({
      path: ROUTES.controllers.SystemConfigurationService.getSessionAliveTime().url,
      authenticate: true
    })
  }

  getLogo() {
    return this.restService.get<string>({
      path: ROUTES.controllers.SystemConfigurationService.getLogo().url,
      authenticate: false,
      text: true
    })
  }

  getFooterLogo() {
    return this.restService.get<string>({
      path: ROUTES.controllers.SystemConfigurationService.getFooterLogo().url,
      authenticate: false,
      text: true
    })
  }

  updateSessionAliveTime(value?: number) {
    const data: any = {}
    if (value !== undefined) {
      data.parameter = value
    }
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.SystemConfigurationService.updateSessionAliveTime().url,
      data: data,
      authenticate: true
    })
  }

}
