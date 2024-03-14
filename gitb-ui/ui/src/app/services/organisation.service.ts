import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { ApiKeyInfo } from '../types/api-key-info';
import { CustomProperty } from '../types/custom-property.type';
import { ErrorDescription } from '../types/error-description';
import { FileParam } from '../types/file-param.type';
import { OrganisationParameterWithValue } from '../types/organisation-parameter-with-value';
import { OrganisationSearchResult } from '../types/organisation-search-result.type';
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

  searchOrganizations(communityIds: number[]|undefined) {
		const data: any = {}
		if (communityIds && communityIds.length > 0) {
		  data["community_ids"] = communityIds.join(',')
		}
    return this.restService.post<Organisation[]>({
      path: ROUTES.controllers.OrganizationService.searchOrganizations().url,
      authenticate: true,
      data: data
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

  getOrganisationsByCommunity(communityId: number, includeAdminOrganisation?: boolean, snapshotId?: number) {
    const params: any = {}
    if (includeAdminOrganisation || snapshotId != undefined) {
      params.admin = true
      params.snapshot = snapshotId
    }
    return this.restService.get<Organisation[]>({
      path: ROUTES.controllers.OrganizationService.getOrganizationsByCommunity(communityId).url,
      authenticate: true,
      params: params
    })
  }

  searchOrganisationsByCommunity(communityId: number, filter: string|undefined, sortOrder: string|undefined, sortColumn: string|undefined, page: number|undefined, limit: number|undefined, creationOrderSort: string) {
    return this.restService.get<OrganisationSearchResult>({
      path: ROUTES.controllers.OrganizationService.searchOrganizationsByCommunity(communityId).url,
      authenticate: true,
      params: {
        filter: filter, 
        sort_order: sortOrder,
        sort_column: sortColumn,
        page: page,
        limit: limit,
        creationOrderSort: creationOrderSort
      }
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
    if (otherOrganisation) {
      data.other_organisation = otherOrganisation
      data.org_params = copyOrganisationParameters
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }

    let files: FileParam[]|undefined
    if (processProperties) {
      const props = this.dataService.customPropertiesForPost(properties)
      data.properties = props.parameterJson
      files = props.files
    }

    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.OrganizationService.createOrganization().url,
      data: data,
      files: files
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
    if (otherOrganisation) {
      data.other_organisation = otherOrganisation
      data.org_params = copyOrganisationParameters
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    }
    let files: FileParam[]|undefined
    if (processProperties) {
      const props = this.dataService.customPropertiesForPost(properties)
      data.properties = props.parameterJson
      files = props.files
    }

    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.OrganizationService.updateOrganization(orgId).url,
      data: data,
      files: files,
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

  getOrganisationParameterValues(orgId: number, onlySimple?: boolean) {
    let params = undefined
    if (onlySimple != undefined) {
      params = {
        simple: onlySimple
      }
    }
    return this.restService.get<OrganisationParameterWithValue[]>({
      path: ROUTES.controllers.OrganizationService.getOrganisationParameterValues(orgId).url,
      authenticate: true,
      params: params
    })
  }

  ownOrganisationHasTests() {
    return this.restService.get<{hasTests: boolean}>({
      path: ROUTES.controllers.OrganizationService.ownOrganisationHasTests().url,
      authenticate: true
    })  
  }

  downloadOrganisationParameterFile(organisationId: number, parameterId: number) {
		return this.restService.get<ArrayBuffer>({
			path: ROUTES.controllers.OrganizationService.downloadOrganisationParameterFile(organisationId, parameterId).url,
			authenticate: true,
			arrayBuffer: true
		})
  }

  getAutomationKeysForOrganisation(organisationId: number) {
    return this.restService.get<ApiKeyInfo>({
      path: ROUTES.controllers.OrganizationService.getAutomationKeysForOrganisation(organisationId).url,
      authenticate: true
    })
  }

  updateOrganisationApiKey(organisationId: number) {
    return this.restService.post<string>({
      path: ROUTES.controllers.OrganizationService.updateOrganisationApiKey(organisationId).url,
      authenticate: true,
      text: true
    })
  }

  deleteOrganisationApiKey(organisationId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.OrganizationService.deleteOrganisationApiKey(organisationId).url,
      authenticate: true
    })
  }

  

}
