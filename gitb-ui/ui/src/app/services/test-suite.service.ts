import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { Specification } from '../types/specification';
import { TestCase } from '../types/test-case';
import { TestSuiteWithTestCases } from '../types/test-suite-with-test-cases';
import { RestService } from './rest.service';
import { TestCaseTag } from '../types/test-case-tag';

@Injectable({
  providedIn: 'root'
})
export class TestSuiteService {

  constructor(
    private restService: RestService
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

}
