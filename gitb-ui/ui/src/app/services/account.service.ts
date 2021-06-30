import { Injectable } from '@angular/core';
import { RestService } from './rest.service'
import { Observable } from 'rxjs';
import { ROUTES } from '../common/global';
import { DataService } from './data.service';
import { User } from '../types/user.type';
import { Organisation } from '../types/organisation.type';
import { AppConfigurationProperties } from '../types/app-configuration-properties';
import { ErrorDescription } from '../types/error-description';
import { FileData } from '../types/file-data.type';
import { CustomProperty } from '../types/custom-property.type';

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  updateVendorProfile(vendorFname: string|undefined, vendorSname: string|undefined, processProperties: boolean, properties: CustomProperty[]) {
    let data: any = {}
    if (vendorFname != undefined) {
      data.vendor_fname = vendorFname
    }
    if (vendorSname != undefined) {
      data.vendor_sname = vendorSname
    }
    if (processProperties) {
      data.properties = this.dataService.customPropertiesForPost(properties)
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.AccountService.updateVendorProfile().url,
      data: data,
      authenticate: true
    })
  }

  getVendorProfile() {
    return this.restService.get<Organisation>({
      path: ROUTES.controllers.AccountService.getVendorProfile().url,
      authenticate: true
    })
  }

  getVendorUsers() {
    return this.restService.get<User[]>({
      path: ROUTES.controllers.AccountService.getVendorUsers().url,
      authenticate: true
    })
  }

  getConfiguration() {
    return this.restService.get<AppConfigurationProperties>({
      path: ROUTES.controllers.AccountService.getConfiguration().url,
      authenticate: false
    })
  }

  getUserProfile() {
    return this.restService.get<User>({
      path: ROUTES.controllers.AccountService.getUserProfile().url,
      authenticate: true
    })
  }

  updateOnetimePassword(username: string, password: string, oldPassword: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.AccountService.updateUserProfile().url,
      data: {
        user_name: username,
        password: password,
        old_password: oldPassword
      }
    })
  }

  updatePassword(password: string, oldPassword: string, errorHandler?: (_:any) => Observable<any>) {
    return this.restService.post<void>({
      path: ROUTES.controllers.AccountService.updateUserProfile().url,
      data: {
        password: password,
        old_password: oldPassword
      },
      authenticate: true,
      errorHandler: errorHandler
    })
  }

  registerUser(userName: string, userEmail: string, userPassword: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.AccountService.registerUser().url,
      data: {
        user_name  : userName,
        user_email : userEmail,
        password   : userPassword
      },
      authenticate: true
    })
  }

  updateUserProfile(name: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.AccountService.updateUserProfile().url,
      data: {
        user_name: name
      },
      authenticate: true
    })
  }

  submitFeedback(userEmail: string, messageTypeId: number, messageTypeDescription: string, messageContent: string, messageAttachments: FileData[]) {
    let data:any = {
      user_email: userEmail,
      msg_type_id: messageTypeId,
      msg_type_description: messageTypeDescription,
      msg_content: messageContent
    }
    if (messageAttachments.length > 0) {
      data['msg_attachments'] = JSON.stringify(messageAttachments)
    }
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.AccountService.submitFeedback().url,
      data: data,
      authenticate: this.dataService.user !== undefined
    })
  }

}
