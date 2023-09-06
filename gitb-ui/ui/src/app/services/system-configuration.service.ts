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

  getConfigurationValues() {
    return this.restService.get<SystemConfiguration[]>({
      path: ROUTES.controllers.SystemConfigurationService.getConfigurationValues().url,
      authenticate: false
    })    
  }

  updateConfigurationValue(name: string, value?: string) {
    const data: any = {
      name: name
    }
    if (value !== undefined) {
      data.parameter = value
    }
    return this.restService.post<string|undefined>({
      path: ROUTES.controllers.SystemConfigurationService.updateConfigurationValue().url,
      data: data,
      authenticate: true
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
