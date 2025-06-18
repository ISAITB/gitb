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
import { ROUTES } from '../common/global';
import { ActualUserInfo } from '../types/actual-user-info';
import { ErrorDescription } from '../types/error-description';
import { UserAccount } from '../types/user-account';
import { RestService } from './rest.service'

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private restService: RestService) { }

  checkEmail(email: string) {
    return this.restService.get<{available: boolean}>({
      path: ROUTES.controllers.AuthenticationService.checkEmail().url,
      params: {
        email: email
      }
    })
  }

  checkEmailOfSystemAdmin(email: string) {
    return this.restService.get<{available: boolean}>({
      path: ROUTES.controllers.AuthenticationService.checkEmailOfSystemAdmin().url,
      params: {
        email: email
      }
    })
  }

  checkEmailOfCommunityAdmin(email: string, communityId: number) {
    return this.restService.get<{available: boolean}>({
      path: ROUTES.controllers.AuthenticationService.checkEmailOfCommunityAdmin().url,
      params: {
        email: email,
        community_id: communityId
      }
    })
  }

  checkEmailOfOrganisationUser(email: string, orgId: number, roleId: number) {
    return this.restService.get<{available: boolean}>({
      path: ROUTES.controllers.AuthenticationService.checkEmailOfOrganisationUser().url,
      params: {
        email: email,
        organization_id: orgId,
        role_id: roleId
      }
    })
  }

  checkEmailOfOrganisationMember(email: string, roleId?: number) {
    const params: any = {
      email: email,
    }
    if (roleId != undefined) {
      params.role_id = roleId
    }
    return this.restService.get<{available: boolean}>({
      path: ROUTES.controllers.AuthenticationService.checkEmailOfOrganisationMember().url,
      params: params
   })
  }

  logout(fullLogout: boolean) {
    return this.restService.post<void>({
      path: ROUTES.controllers.AuthenticationService.logout().url,
      data: {
        full: fullLogout
      }
    })
  }

  getUserFunctionalAccounts() {
    return this.restService.get<ActualUserInfo>({
      path: ROUTES.controllers.AuthenticationService.getUserFunctionalAccounts().url
    })
  }

  getUserUnlinkedFunctionalAccounts() {
    return this.restService.get<UserAccount[]>({
      path: ROUTES.controllers.AuthenticationService.getUserUnlinkedFunctionalAccounts().url
    })
  }

  disconnectFunctionalAccount(option: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.AuthenticationService.disconnectFunctionalAccount().url,
      data: {
        type: option
      }
    })
  }

  linkFunctionalAccount(accountId: number) {
    return this.restService.post<ActualUserInfo>({
      path: ROUTES.controllers.AuthenticationService.linkFunctionalAccount().url,
      data : {
        id: accountId
      }
    })
  }

  migrateFunctionalAccount(email: string, password: string) {
    return this.restService.post<ActualUserInfo|ErrorDescription>({
      path: ROUTES.controllers.AuthenticationService.migrateFunctionalAccount().url,
      data : {
        email: email,
        password: password
      }
    })
  }

}
