import { HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ROUTES } from '../common/global';
import { StepReport } from '../components/diagram/report/step-report';
import { TestCase } from '../types/test-case';
import { TestResult } from '../types/test-result';
import { TestResultData } from '../types/test-result-data';
import { TestResultReport } from '../types/test-result-report';
import { TestResultSearchCriteria } from '../types/test-result-search-criteria';
import { TestStepResult } from '../types/test-step-result';
import { DataService } from './data.service';
import { RestService } from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  constructor(
    private dataService: DataService,
    private restService: RestService
  ) { }

  getTestCasesForSystem(systemId: number) {
    return this.restService.get<TestCase[]>({
      path: ROUTES.controllers.RepositoryService.getTestCasesForSystem(systemId).url,
      authenticate: true
    })
  }

  getAllTestCases() {
    return this.restService.get<TestCase[]>({
      path: ROUTES.controllers.RepositoryService.getAllTestCases().url,
      authenticate: true
    })
  }

  getTestCasesForCommunity() {
    return this.restService.get<TestCase[]>({
      path: ROUTES.controllers.RepositoryService.getTestCasesForCommunity(this.dataService.community!.id).url,
      authenticate: true
    })
  }

  private criteriaToRequestParams(criteria: TestResultSearchCriteria, activeResults: boolean, systemId?: number) {
    const params: any = {}
    if (criteria.specIds !== undefined && criteria.specIds.length > 0) {
      params.specification_ids = criteria.specIds.join(',')
    }
    if (criteria.actorIds !== undefined && criteria.actorIds.length > 0) {
      params.actor_ids = criteria.actorIds.join(',')
    }
    if (criteria.testSuiteIds !== undefined && criteria.testSuiteIds.length > 0) {
      params.test_suite_ids = criteria.testSuiteIds.join(',')
    }
    if (criteria.testCaseIds !== undefined && criteria.testCaseIds.length > 0) {
      params.test_case_ids = criteria.testCaseIds.join(',')
    }
    if (criteria.domainIds !== undefined && criteria.domainIds.length > 0) {
      params.domain_ids = criteria.domainIds.join(',')
    }
    if (criteria.startTimeBeginStr !== undefined) {
      params.start_time_begin = criteria.startTimeBeginStr
    }
    if (criteria.startTimeEndStr !== undefined) {
      params.start_time_end = criteria.startTimeEndStr
    }
    if (criteria.sessionId !== undefined) {
      params.session_id = criteria.sessionId
    }
    if (systemId !== undefined) {
      // System-specific parameters
      params.system_id = systemId
    } else {
      // General parameters
      if (criteria.communityIds !== undefined && criteria.communityIds.length > 0) {
        params.community_ids = criteria.communityIds.join(',')
      }
      if (criteria.organisationIds !== undefined && criteria.organisationIds.length > 0) {
        params.organization_ids = criteria.organisationIds.join(',')
      }
      if (criteria.systemIds !== undefined && criteria.systemIds.length > 0) {
        params.system_ids = criteria.systemIds.join(',')
      }
      if (criteria.organisationProperties !== undefined && criteria.organisationProperties.length > 0) {
        params.org_params = JSON.stringify(criteria.organisationProperties)
      }
      if (criteria.systemProperties !== undefined && criteria.systemProperties.length > 0) {
        params.sys_params = JSON.stringify(criteria.systemProperties)
      }
    }
    if (activeResults) {
      // Parameters specific for active test sessions
      if (criteria.activeSortColumn !== undefined) {
        params.sort_column = criteria.activeSortColumn
      }
      if (criteria.activeSortOrder !== undefined) {
        params.sort_order = criteria.activeSortOrder
      }
    } else {
      // Parameters specific for completed test sessions
      if (criteria.results !== undefined && criteria.results.length > 0) {
        params.results = criteria.results.join(',')
      }
      if (criteria.endTimeBeginStr !== undefined) {
        params.end_time_begin = criteria.endTimeBeginStr
      }
      if (criteria.endTimeEndStr !== undefined) {
        params.end_time_end = criteria.endTimeEndStr
      }
      if (criteria.completedSortColumn !== undefined) {
        params.sort_column = criteria.completedSortColumn
      }
      if (criteria.completedSortOrder !== undefined) {
        params.sort_order = criteria.completedSortOrder
      }
    }
    return params
  }

  getTestResult(sessionId: string) {
    return this.restService.get<TestResultReport|undefined>({
      path: ROUTES.controllers.ReportService.getTestResult(sessionId).url,
      authenticate: true
    })
  }

  getActiveTestResults(criteria: TestResultSearchCriteria, forExport?: boolean) {
    const params = this.criteriaToRequestParams(criteria, true)
    params.export = forExport != undefined && forExport
    return this.restService.post<TestResultData>({
      path: ROUTES.controllers.ReportService.getActiveTestResults().url,
      authenticate: true,
      data: params
    })
  }

  getSystemActiveTestResults(systemId: number, criteria: TestResultSearchCriteria) {
    const params = this.criteriaToRequestParams(criteria, true, systemId)
    return this.restService.post<TestResultData>({
      path: ROUTES.controllers.ReportService.getSystemActiveTestResults().url,
      authenticate: true,
      data: params
    })
  }

  getCompletedTestResults(page: number, limit: number, criteria: TestResultSearchCriteria, forExport?: boolean) {
    const params = this.criteriaToRequestParams(criteria, false)
    params.page = page
    params.limit = limit
    params.export = forExport !== undefined && forExport
    return this.restService.post<TestResultData>({
      path: ROUTES.controllers.ReportService.getFinishedTestResults().url,
      authenticate: true,
      data: params
    })
  }

  getTestResults(systemId: number, page: number, limit: number, criteria: TestResultSearchCriteria) {
    const params = this.criteriaToRequestParams(criteria, false, systemId)
    params.page = page
    params.limit = limit
    return this.restService.post<TestResultData>({
      path: ROUTES.controllers.ReportService.getTestResults().url,
      authenticate: true,
      data: params
    })
  }

  exportConformanceStatementReport(actorId: number, systemId: number, includeTests: boolean) {
    return this.restService.get<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceStatementReport().url,
      params: {
        actor_id: actorId,
        system_id: systemId,
        tests: includeTests 
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportTestCaseReport(session: string, testCaseId: number) {
    return this.restService.get<ArrayBuffer>(({
      path: ROUTES.controllers.RepositoryService.exportTestCaseReport().url,
      params: {
        session_id: session,
        test_id: testCaseId
      },
      authenticate: true,
      arrayBuffer: true
    }))
  }

  getTestStepResults(sessionId: string) {
    return this.restService.get<TestStepResult[]>({
      path: ROUTES.controllers.ReportService.getTestStepResults(sessionId).url,
      authenticate: true
    })
  }

  getTestResultOfSession(sessionId: string) {
    return this.restService.get<TestResult>({
      path: ROUTES.controllers.ReportService.getTestResultOfSession(sessionId).url,
      authenticate: true
    })
  }

  getTestStepReport(session: string, reportPath?: string) {
    // Paths like 6[2].1.xml must be escaped
    if (reportPath != undefined) {
      reportPath = reportPath.replace(/\[/g, '__SQS__')
      reportPath = reportPath.replace(/\]/g, '__SQE__')
      reportPath = escape(reportPath)
    }
    return this.restService.get<StepReport>({
      path: ROUTES.controllers.RepositoryService.getTestStepReport(session, reportPath).url,
      authenticate: true
    })
  }

  getTestStepReportData(sessionId: string, dataId: string, mimeType?: string) {
    return this.restService.get<HttpResponse<ArrayBuffer>>({
      path: ROUTES.controllers.RepositoryService.getTestStepReportData(sessionId, dataId).url,
      authenticate: true,
      arrayBuffer: true,
      accept: mimeType,
      httpResponse: true
    })
  }

  exportTestStepReport(sessionId: string, reportPath: string) {
    return this.restService.get<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportTestStepReport(sessionId, reportPath).url,
      authenticate: true,
      arrayBuffer: true
    })
  }

  createTestReport(sessionId: string, systemId: number, actorId: number, testId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.ReportService.createTestReport().url,
      data: {
        session_id: sessionId,
        system_id: systemId,
        actor_id: actorId,
        test_id: testId
      },
      authenticate: true
    })
  }

  getTestSessionLog(session: string) {
    return this.restService.get<string[]>({
      path: ROUTES.controllers.RepositoryService.getTestSessionLog(session).url,
      authenticate: true
    })
  }

}
