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
import { FileParam } from '../types/file-param.type';

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  updateVendorProfile(vendorFname: string|undefined, vendorSname: string|undefined, processProperties: boolean, properties: CustomProperty[], landingPageId: number|undefined) {
    let data: any = {}
    if (vendorFname != undefined) {
      data.vendor_fname = vendorFname
    }
    if (vendorSname != undefined) {
      data.vendor_sname = vendorSname
    }
    if (landingPageId != undefined) {
      data.landing_page_id = landingPageId
    }
    let files: FileParam[]|undefined
    if (processProperties) {
      const props = this.dataService.customPropertiesForPost(properties)
      data.properties = props.parameterJson
      files = props.files
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.AccountService.updateVendorProfile().url,
      data: data,
      files: files,
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
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.AccountService.updateUserProfile().url,
      data: {
        password: password,
        old_password: oldPassword
      },
      authenticate: true,
      errorHandler: errorHandler
    })
  }

  registerUser(userName: string|undefined, userEmail: string, userPassword: string|undefined, roleId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.AccountService.registerUser().url,
      data: {
        user_name  : userName,
        user_email : userEmail,
        password   : userPassword,
        role_id: roleId
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
    let files: FileParam[]|undefined
    if (messageAttachments.length > 0) {
      files = []
      for (let i=0; i < messageAttachments.length; i++) {
        files.push({param: 'file'+i, data: messageAttachments[i].file!})
      }
    }
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.AccountService.submitFeedback().url,
      data: data,
      files: files,
      authenticate: false
    })
  }

}
