import { Injectable } from '@angular/core';
import { ROUTER_CONFIGURATION } from '@angular/router';
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

  getUserById(userId: number) {
    return this.restService.get<User>({
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
      data.name = name
    }
    if (password != undefined) {
      data.password = password
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.UserService.updateUserProfile(userId).url,
      data: data,
      authenticate: true
    })
  }

  createSystemAdmin(userName: string, userEmail: string, userPassword: string) {
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

  createCommunityAdmin(userName: string, userEmail: string, userPassword: string, communityId: number) {
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

  createVendorUser(userName: string, userEmail: string, userPassword: string, orgId: number, roleId: number) {
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
