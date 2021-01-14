import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { TestCase } from '../types/test-case';
import { TestSuiteWithTestCases } from '../types/test-suite-with-test-cases';
import { DataService } from './data.service';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class TestSuiteService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

	getAllTestSuitesWithTestCases() {
		return this.restService.get<TestSuiteWithTestCases[]>({
			path: ROUTES.controllers.TestSuiteService.getAllTestSuitesWithTestCases().url,
			authenticate: true
		})
	}

	getTestSuitesWithTestCasesForCommunity() {
		return this.restService.get<TestSuiteWithTestCases[]>({
			path: ROUTES.controllers.TestSuiteService.getTestSuitesWithTestCasesForCommunity(this.dataService.community!.id).url,
			authenticate: true
		})
	}

	getTestSuitesWithTestCasesForSystem(systemId: number) {
		return this.restService.get<TestSuiteWithTestCases[]>({
			path: ROUTES.controllers.TestSuiteService.getTestSuitesWithTestCasesForSystem(systemId).url,
			authenticate: true
		})
	}

	getTestSuiteWithTestCases(testSuiteId: number) {
		return this.restService.get<TestSuiteWithTestCases>({
			path: ROUTES.controllers.TestSuiteService.getTestSuiteWithTestCases(testSuiteId).url,
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

	updateTestCaseMetadata(testCaseId: number, name: string, description?: string, documentation?: string) {
		return this.restService.post<void>({
			path: ROUTES.controllers.TestSuiteService.updateTestCaseMetadata(testCaseId).url,
			data: {
				name: name,
				description: description,
				documentation: documentation
			},
			authenticate: true
		})
	}

	updateTestSuiteMetadata(testSuiteId: number, name: string, description: string|undefined, documentation:string|undefined, version: string) {
		return this.restService.post<void>({
			path: ROUTES.controllers.TestSuiteService.updateTestSuiteMetadata(testSuiteId).url,
			data: {
				name: name,
				description: description,
				documentation: documentation,
				version: version
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
