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

  checkEmailOfOrganisationMember(email: string) {
    return this.restService.get<{available: boolean}>({
      path: ROUTES.controllers.AuthenticationService.checkEmailOfOrganisationMember().url,
      params: {
        email: email
      }
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
      path: ROUTES.controllers.Aut1henticationService.disconnectFunctionalAccount().url,
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