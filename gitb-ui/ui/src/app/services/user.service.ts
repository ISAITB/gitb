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
import { Constants } from '../common/constants';
import { ROUTES } from '../common/global';
import { ErrorDescription } from '../types/error-description';
import { User } from '../types/user.type';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private restService: RestService
  ) { }

  getSystemAdministrators() {
    return this.restService.get<User[]>({
      path: ROUTES.controllers.UserService.getSystemAdministrators().url,
      authenticate: true
    })
  }

  getCommunityAdministrators(communityId: number) {
    return this.restService.get<User[]>({
      path: ROUTES.controllers.UserService.getCommunityAdministrators().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })
  }

  getUsersByOrganisation(orgId: number) {
    return this.restService.get<User[]>({
      path: ROUTES.controllers.UserService.getUsersByOrganization(orgId).url,
      authenticate: true
    })
  }

  getBasicUsersByOrganization(orgId: number) {
    return this.restService.get<User[]>({
      path: ROUTES.controllers.UserService.getBasicUsersByOrganization(orgId).url,
      authenticate: true
    })
  }

  getOwnOrganisationUserById(userId: number) {
    return this.restService.get<User>({
      path: ROUTES.controllers.UserService.getOwnOrganisationUserById(userId).url,
      authenticate: true
    })
  }

  getUserById(userId: number) {
    return this.restService.get<User|undefined>({
      path: ROUTES.controllers.UserService.getUserById(userId).url,
      authenticate: true
    })
  }

  updateSystemAdminProfile(userId: number, name: string, password?: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.UserService.updateSystemAdminProfile(userId).url,
      authenticate: true,
      data: {
        user_name: name,
        password: password
      }
    })
  }

  updateCommunityAdminProfile(userId: number, name: string, password?: string) {
    return this.restService.post<void>({
      path: ROUTES.controllers.UserService.updateCommunityAdminProfile(userId).url,
      authenticate: true,
      data: {
        user_name: name,
        password: password
      }
    })
  }

  updateUserProfile(userId: number, name: string|undefined, role: number, password: string|undefined) {
    const data: any = {
      role_id: role
    }
    if (name != undefined) {
      data.user_name = name
    }
    if (password != undefined) {
      data.password = password
    }
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.UserService.updateUserProfile(userId).url,
      data: data,
      authenticate: true
    })
  }

  createSystemAdmin(userName: string|undefined, userEmail: string, userPassword: string|undefined) {
    return this.restService.post<void>({
      path: ROUTES.controllers.UserService.createSystemAdmin().url,
      data: {
        user_name: userName,
        user_email: userEmail,
        password: userPassword,
        community_id: Constants.DEFAULT_COMMUNITY_ID
      },
      authenticate: true
    })
  }

  createCommunityAdmin(userName: string|undefined, userEmail: string, userPassword: string|undefined, communityId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.UserService.createCommunityAdmin().url,
      data: {
        user_name: userName,
        user_email: userEmail,
        password: userPassword,
        community_id: communityId
      },
      authenticate: true
    })
  }

  createVendorUser(userName: string|undefined, userEmail: string, userPassword: string|undefined, orgId: number, roleId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.UserService.createUser(orgId).url,
      data: {
        user_name: userName,
        user_email: userEmail,
        password: userPassword,
        role_id: roleId
      },
      authenticate: true
    })
  }

  deleteAdmin(userId: number) {
    return this.restService.delete<ErrorDescription|undefined>({
      path: ROUTES.controllers.UserService.deleteAdmin(userId).url,
      authenticate: true
    })
  }

  deleteVendorUser(userId: number) {
    return this.restService.delete<ErrorDescription|undefined>({
      path: ROUTES.controllers.UserService.deleteVendorUser(userId).url,
      authenticate: true
    })
  }
}
