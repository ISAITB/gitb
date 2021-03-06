import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { CustomProperty } from '../types/custom-property.type';
import { ErrorDescription } from '../types/error-description';
import { OrganisationParameterWithValue } from '../types/organisation-parameter-with-value';
import { Organisation } from '../types/organisation.type';
import { DataService } from './data.service';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class OrganisationService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  getOrganisations() {
    return this.restService.get<Organisation[]>({
      path: ROUTES.controllers.OrganizationService.getOrganizations().url,
      authenticate: true
    })
  }

  getOrganisationById(orgId: number) {
    return this.restService.get<Organisation>({
      path: ROUTES.controllers.OrganizationService.getOrganizationById(orgId).url,
      authenticate: true
    })
  }

  getOrganisationBySystemId(systemId: number) {
    return this.restService.get<Organisation>({
      path: ROUTES.controllers.OrganizationService.getOrganizationBySystemId(systemId).url,
      authenticate: true
    })
  }

  getOrganisationsByCommunity(communityId: number) {
    return this.restService.get<Organisation[]>({
      path: ROUTES.controllers.OrganizationService.getOrganizationsByCommunity(communityId).url,
      authenticate: true
    })
  }

  createOrganisation(shortName: string, fullName: string, landingPage: number|undefined, legalNotice: number|undefined, errorTemplate: number|undefined, otherOrganisation: number|undefined, communityId: number, template: boolean, templateName: string|undefined, processProperties: boolean, properties: CustomProperty[], copyOrganisationParameters: boolean, copySystemParameters: boolean, copyStatementParameters: boolean) {
    let data: any = {
      vendor_sname: shortName,
      vendor_fname: fullName,
      community_id: communityId
    }
    if (this.dataService.configuration.registrationEnabled) {
      data.template =  template
      if (template && templateName != undefined) {
        data.template_name = templateName
      }
    }
    if (landingPage != undefined) {
      data.landing_page_id = landingPage
    }
    if (legalNotice != undefined) {
      data.legal_notice_id = legalNotice
    }
    if (errorTemplate != undefined) {
      data.error_template_id = errorTemplate
    }
    if (otherOrganisation != undefined) {
      data.other_organisation = otherOrganisation
      data.org_params = copyOrganisationParameters
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }

    if (processProperties) {
      data.properties = this.dataService.customPropertiesForPost(properties)
    }

    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.OrganizationService.createOrganization().url,
      data: data
    })
  }

  deleteOrganisation(orgId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.OrganizationService.deleteOrganization(orgId).url
    })
  }

  updateOrganisation(orgId: number, shortName: string, fullName: string, landingPage: number|undefined, legalNotice: number|undefined, errorTemplate: number|undefined, otherOrganisation: number|undefined, template: boolean, templateName: string|undefined, processProperties: boolean, properties: CustomProperty[], copyOrganisationParameters: boolean, copySystemParameters: boolean, copyStatementParameters: boolean) {
    let data: any = {
      vendor_sname: shortName,
      vendor_fname: fullName
    }
    if (this.dataService.configuration.registrationEnabled) {
      data.template =  template
      if (template && templateName != undefined) {
        data.template_name = templateName
      }
    }

    if (landingPage != undefined) {
      data.landing_page_id = landingPage
    }
    if (legalNotice != undefined) {
      data.legal_notice_id = legalNotice
    }
    if (errorTemplate != undefined) {
      data.error_template_id = errorTemplate
    }
    if (otherOrganisation != undefined) {
      data.other_organisation = otherOrganisation
      data.org_params = copyOrganisationParameters
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }
    if (processProperties) {
      data.properties = this.dataService.customPropertiesForPost(properties)
    }

    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.OrganizationService.updateOrganization(orgId).url,
      data: data,
      authenticate: true
    })
  }
  
  getOwnOrganisationParameterValues() {
    return this.restService.get<OrganisationParameterWithValue[]>({
      path: ROUTES.controllers.OrganizationService.getOwnOrganisationParameterValues().url,
      authenticate: true
    })
  }

  checkOrganisationParameterValues(orgId: number) {
    return this.restService.get<OrganisationParameterWithValue[]>({
      path: ROUTES.controllers.OrganizationService.checkOrganisationParameterValues(orgId).url,
      authenticate: true
    })
  }

  getOrganisationParameterValues(orgId: number) {
    return this.restService.get<OrganisationParameterWithValue[]>({
      path: ROUTES.controllers.OrganizationService.getOrganisationParameterValues(orgId).url,
      authenticate: true
    })
  }

  ownOrganisationHasTests() {
    return this.restService.get<{hasTests: boolean}>({
      path: ROUTES.controllers.OrganizationService.ownOrganisationHasTests().url,
      authenticate: true
    })  
  }

}
