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

import {Injectable} from '@angular/core';
import {ROUTES} from '../common/global';
import {Specification} from '../types/specification';
import {TestCase} from '../types/test-case';
import {TestSuiteWithTestCases} from '../types/test-suite-with-test-cases';
import {RestService} from './rest.service';
import {ErrorDescription} from '../types/error-description';
import {Id} from '../types/id';
import {SearchResult} from '../types/search-result';

@Injectable({
  providedIn: 'root'
})
export class TestSuiteService {

  constructor(
    private readonly restService: RestService
  ) { }

	searchTestSuites(domainIds: number[]|undefined, specificationIds: number[]|undefined, specificationGroupIds: number[]|undefined, actorIds: number[]|undefined) {
		const data: any = {}
		if (domainIds && domainIds.length > 0) {
		  data["domain_ids"] = domainIds.join(',')
		}
		if (specificationIds && specificationIds.length > 0) {
		  data["specification_ids"] = specificationIds.join(',')
		}
		if (specificationGroupIds != undefined && specificationGroupIds.length > 0) {
			data['group_ids'] = specificationGroupIds.join(',')
		}
		if (actorIds && actorIds.length > 0) {
			data["actor_ids"] = actorIds.join(',')
		}
		return this.restService.post<TestSuiteWithTestCases[]>({
			path: ROUTES.controllers.TestSuiteService.searchTestSuites().url,
			authenticate: true,
			data: data
		})
	}

	searchTestSuitesInDomain(domainId: number, specificationIds: number[]|undefined, specificationGroupIds: number[]|undefined, actorIds: number[]|undefined) {
		const data: any = {
			domain_id: domainId
		}
		if (specificationIds && specificationIds.length > 0) {
		  data["specification_ids"] = specificationIds.join(',')
		}
		if (specificationGroupIds != undefined && specificationGroupIds.length > 0) {
			data['group_ids'] = specificationGroupIds.join(',')
		}
		if (actorIds && actorIds.length > 0) {
			data["actor_ids"] = actorIds.join(',')
		}
		return this.restService.post<TestSuiteWithTestCases[]>({
			path: ROUTES.controllers.TestSuiteService.searchTestSuitesInDomain().url,
			authenticate: true,
			data: data
		})
	}

	getTestSuiteWithTestCases(testSuiteId: number) {
		return this.restService.get<TestSuiteWithTestCases>({
			path: ROUTES.controllers.TestSuiteService.getTestSuiteWithTestCases(testSuiteId).url,
			authenticate: true
	    })
	}

  getTestSuiteTestCasesWithPaging(testSuiteId: number, filter: string|undefined, page: number, limit:  number) {
    let params: any = {
      page: page,
      limit: limit
    }
    if (filter != undefined) params.filter = filter
    return this.restService.get<SearchResult<TestCase>>({
      path: ROUTES.controllers.TestSuiteService.getTestSuiteTestCasesWithPaging(testSuiteId).url,
      authenticate: true,
      params: params
    })
  }

	getLinkedSpecifications(testSuiteId: number) {
		return this.restService.get<Specification[]>({
			path: ROUTES.controllers.TestSuiteService.getLinkedSpecifications(testSuiteId).url,
			authenticate: true
    })
	}

	downloadTestSuite(testSuiteId: number) {
		return this.restService.get<ArrayBuffer>({
			path: ROUTES.controllers.TestSuiteService.downloadTestSuite(testSuiteId).url,
			authenticate: true,
			arrayBuffer: true
		})
	}

	undeployTestSuite(testSuiteId: number) {
		return this.restService.delete<void>({
			path: ROUTES.controllers.TestSuiteService.undeployTestSuite(testSuiteId).url,
			authenticate: true
		})
	}

	updateTestCaseMetadata(testCaseId: number, name: string, description?: string, documentation?: string, optional?:boolean, disabled?:boolean, tags?: string, specReference?: string, specDescription?: string, specLink?:string) {
		return this.restService.post<void>({
			path: ROUTES.controllers.TestSuiteService.updateTestCaseMetadata(testCaseId).url,
			data: {
				name: name,
				description: description,
				documentation: documentation,
				optional: optional,
				disabled: disabled,
				tags: tags,
				specReference: specReference,
				specDescription: specDescription,
				specLink: specLink
			},
			authenticate: true
		})
	}

	updateTestSuiteMetadata(testSuiteId: number, name: string, description: string|undefined, documentation:string|undefined, version: string, specReference?: string, specDescription?: string, specLink?:string) {
		return this.restService.post<void>({
			path: ROUTES.controllers.TestSuiteService.updateTestSuiteMetadata(testSuiteId).url,
			data: {
				name: name,
				description: description,
				documentation: documentation,
				version: version,
				specReference: specReference,
				specDescription: specDescription,
				specLink: specLink
			},
			authenticate: true
		})
	}

	getTestCase(testCaseId: number) {
		return this.restService.get<TestCase>({
			path: ROUTES.controllers.TestSuiteService.getTestCase(testCaseId).url,
			authenticate: true
		})
	}

  getAvailableSpecificationsForMove(testSuiteId: number) {
    return this.restService.get<Specification[]>({
      path: ROUTES.controllers.TestSuiteService.getAvailableSpecificationsForMove(testSuiteId).url,
      authenticate: true
    })
  }

  moveTestSuiteToSpecification(testSuiteId: number, specificationId: number) {
    const data: any = {
      spec_id: specificationId
    }
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.TestSuiteService.moveTestSuiteToSpecification(testSuiteId).url,
      authenticate: true,
      data: data
    })
  }

  convertNonSharedTestSuiteToShared(testSuiteId: number) {
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.TestSuiteService.convertNonSharedTestSuiteToShared(testSuiteId).url,
      authenticate: true
    })
  }

  convertSharedTestSuiteToNonShared(testSuiteId: number) {
    return this.restService.post<ErrorDescription|Id>({
      path: ROUTES.controllers.TestSuiteService.convertSharedTestSuiteToNonShared(testSuiteId).url,
      authenticate: true
    })
  }

}
