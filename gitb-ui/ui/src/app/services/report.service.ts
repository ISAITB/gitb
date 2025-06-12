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
import { RestService } from './rest.service';
import { FileReference } from '../types/file-reference';
import { DataService } from './data.service';
import { TestInteractionData } from '../types/test-interaction-data';
import { FileData } from '../types/file-data.type';
import { FileParam } from '../types/file-param.type';
import { Constants } from '../common/constants';
import { ConformanceCertificateSettings } from '../types/conformance-certificate-settings';
import { ConformanceOverviewCertificateSettings } from '../types/conformance-overview-certificate-settings';
import { CommunityReportSettings } from '../types/community-report-settings';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  constructor(
    private restService: RestService,
    private dataService: DataService
  ) { }

  searchTestCasesInDomain(domainId: number, specificationIds: number[]|undefined, specificationGroupIds: number[]|undefined, actorIds: number[]|undefined, testSuiteIds: number[]|undefined) {
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
		if (testSuiteIds && testSuiteIds.length > 0) {
			data["test_suite_ids"] = testSuiteIds.join(',')
		}
		return this.restService.post<TestCase[]>({
			path: ROUTES.controllers.RepositoryService.searchTestCasesInDomain().url,
			authenticate: true,
			data: data
		})
  }

  searchTestCases(domainIds: number[]|undefined, specificationIds: number[]|undefined, specificationGroupIds: number[]|undefined, actorIds: number[]|undefined, testSuiteIds: number[]|undefined) {
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
		if (testSuiteIds && testSuiteIds.length > 0) {
			data["test_suite_ids"] = testSuiteIds.join(',')
		}
		return this.restService.post<TestCase[]>({
			path: ROUTES.controllers.RepositoryService.searchTestCases().url,
			authenticate: true,
			data: data
		})
  }

  getAllTestCases() {
    return this.restService.get<TestCase[]>({
      path: ROUTES.controllers.RepositoryService.getAllTestCases().url,
      authenticate: true
    })
  }

  private criteriaToRequestParams(criteria: TestResultSearchCriteria, activeResults: boolean, organisationId?: number) {
    const params: any = {}
    if (criteria.specIds !== undefined && criteria.specIds.length > 0) {
      params.specification_ids = criteria.specIds.join(',')
    }
    if (criteria.specGroupIds !== undefined && criteria.specGroupIds.length > 0) {
      params.group_ids = criteria.specGroupIds.join(',')
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
    if (criteria.systemIds !== undefined && criteria.systemIds.length > 0) {
      params.system_ids = criteria.systemIds.join(',')
    }
    if (organisationId !== undefined) {
      // Organisation-specific parameters
      params.organization_id = organisationId
    } else {
      // General parameters
      if (criteria.communityIds !== undefined && criteria.communityIds.length > 0) {
        params.community_ids = criteria.communityIds.join(',')
      }
      if (criteria.organisationIds !== undefined && criteria.organisationIds.length > 0) {
        params.organization_ids = criteria.organisationIds.join(',')
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
    let path: string
    if (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) {
      path = ROUTES.controllers.RepositoryService.getTestResultAdmin(sessionId).url
    } else {
      path = ROUTES.controllers.RepositoryService.getTestResult(sessionId).url
    }
    return this.restService.get<TestResultReport|undefined>({
      path: path,
      authenticate: true
    })
  }

  getActiveTestResults(page: number, limit: number, criteria: TestResultSearchCriteria, pendingAdminInteraction: boolean, forExport?: boolean) {
    const params = this.criteriaToRequestParams(criteria, true)
    params.page = page
    params.limit = limit
    params.export = forExport != undefined && forExport
    params.pending_admin_interaction = pendingAdminInteraction
    return this.restService.post<TestResultData>({
      path: ROUTES.controllers.ReportService.getActiveTestResults().url,
      authenticate: true,
      data: params
    })
  }

  getSystemActiveTestResults(page: number, limit: number, organisationId: number, criteria: TestResultSearchCriteria) {
    const params = this.criteriaToRequestParams(criteria, true, organisationId)
    params.page = page
    params.limit = limit
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

  getTestResults(organisationId: number, page: number, limit: number, criteria: TestResultSearchCriteria) {
    const params = this.criteriaToRequestParams(criteria, false, organisationId)
    params.page = page
    params.limit = limit
    return this.restService.post<TestResultData>({
      path: ROUTES.controllers.ReportService.getTestResults().url,
      authenticate: true,
      data: params
    })
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

  getTestStepReportDataAsDataUrl(sessionId: string, dataId: string, mimeType: string|undefined) {
    return this.restService.get<FileReference>({
      path: ROUTES.controllers.RepositoryService.getTestStepReportDataAsDataUrl(sessionId, dataId).url,
      authenticate: true,
      accept: mimeType
    })
  }

  getTestStepReportData(sessionId: string, dataId: string, mimeType: string|undefined) {
    return this.restService.get<HttpResponse<ArrayBuffer>>({
      path: ROUTES.controllers.RepositoryService.getTestStepReportData(sessionId, dataId).url,
      authenticate: true,
      arrayBuffer: true,
      accept: mimeType,
      httpResponse: true
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

  getPendingTestSessionInteractions(session: string) {
    let path: string
    if (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) {
      path = ROUTES.controllers.RepositoryService.getPendingTestSessionInteractionsAdmin(session).url
    } else {
      path = ROUTES.controllers.RepositoryService.getPendingTestSessionInteractions(session).url
    }
    return this.restService.get<TestInteractionData[]>({
      path: path,
      authenticate: true
    })
  }

  loadReportSettings(communityId: number, reportType: number) {
    return this.restService.get<CommunityReportSettings>({
      path: ROUTES.controllers.RepositoryService.loadReportSettings(communityId).url,
      authenticate: true,
      params: {
        type: reportType
      }
    })
  }

  getReportStylesheet(communityId: number, reportType: number) {
    return this.restService.get<string>({
      path: ROUTES.controllers.RepositoryService.getReportStylesheet(communityId).url,
      authenticate: true,
      text: true,
      params: {
        type: reportType
      }
    })
  }

  private reportSettingsToData(reportType: number, useStylesheet: boolean, settings: CommunityReportSettings): any {
    let data: any = {
      type: reportType,
      useStylesheet: useStylesheet,
      signPdfReports: settings.signPdfs,
      useCustomPdfReports: settings.customPdfs,
      useCustomPdfReportsWithCustomXml: settings.customPdfsWithCustomXml,
    }
    if (settings.customPdfService != undefined) {
      data.customPdfService = settings.customPdfService
    }
    return data
  }

  updateReportSettings(communityId: number, reportType: number, useStylesheet: boolean, stylesheet: FileData|undefined, settings: CommunityReportSettings) {
    let files: FileParam[]|undefined
    const data = this.reportSettingsToData(reportType, useStylesheet, settings)
    if (useStylesheet && stylesheet?.file) {
      files = [{ param: "file", data: stylesheet.file}]
    }
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.updateReportSettings(communityId).url,
      authenticate: true,
      files: files,
      data: data
    })
  }

  exportDemoReportPdf(communityId: number, reportType: number, reportSettings: CommunityReportSettings, useStyleSheet: boolean, file?: FileData, data?: {[key: string]: any}) {
    let path: string
    if (reportType == Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_REPORT) {
      path = ROUTES.controllers.RepositoryService.exportDemoConformanceOverviewReport(communityId).url
    } else if (reportType == Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT) {
      path = ROUTES.controllers.RepositoryService.exportDemoConformanceStatementReport(communityId).url
    } else if (reportType == Constants.REPORT_TYPE.TEST_CASE_REPORT) {
      path = ROUTES.controllers.RepositoryService.exportDemoTestCaseReport(communityId).url
    } else {
      path = ROUTES.controllers.RepositoryService.exportDemoTestStepReport(communityId).url
    }
    let files: FileParam[]|undefined
    if (useStyleSheet && file?.file) {
      files = [{ param: "file", data: file.file}]
    }
    let dataToUse = this.reportSettingsToData(reportType, useStyleSheet, reportSettings)
    if (data != undefined) {
      for (let dataItem in data) {
        // Add extra data items for specific report types.
        dataToUse[dataItem] = data[dataItem]
      }
    }
    return this.restService.post<HttpResponse<ArrayBuffer>>({
      path: path,
      authenticate: true,
      files: files,
      data: dataToUse,
      arrayBuffer: true,
      httpResponse: true
    })
  }

  exportDemoReportXml(communityId: number, reportType: number, enabled: boolean, file?: FileData, data?: {[key: string]: any}) {
    let path: string
    if (reportType == Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_REPORT) {
      path = ROUTES.controllers.RepositoryService.exportDemoConformanceOverviewReportInXML(communityId).url
    } else if (reportType == Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT) {
      path = ROUTES.controllers.RepositoryService.exportDemoConformanceStatementReportInXML(communityId).url
    } else if (reportType == Constants.REPORT_TYPE.TEST_CASE_REPORT) {
      path = ROUTES.controllers.RepositoryService.exportDemoTestCaseReportInXML(communityId).url
    } else {
      path = ROUTES.controllers.RepositoryService.exportDemoTestStepReportInXML(communityId).url
    }
    let files: FileParam[]|undefined
    if (enabled && file?.file) {
      files = [{ param: "file", data: file.file}]
    }
    let dataToUse = data
    if (dataToUse == undefined) {
      dataToUse = {}
    }
    dataToUse.enable = enabled
    dataToUse.type = reportType
    return this.restService.post<string>({
      path: path,
      authenticate: true,
      files: files,
      text: true,
      data: dataToUse
    })
  }

  updateConformanceCertificateSettings(communityId: number, reportSettings: CommunityReportSettings, certificateSettings: ConformanceCertificateSettings, stylesheet?: FileData) {
    let files: FileParam[]|undefined
    if (reportSettings.customPdfsWithCustomXml && stylesheet?.file) {
      files = [{ param: "file", data: stylesheet.file}]
    }
    let dataToUse = this.reportSettingsToData(Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_CERTIFICATE, reportSettings.customPdfsWithCustomXml, reportSettings)
    dataToUse["settings"] = JSON.stringify(certificateSettings)
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.updateConformanceCertificateSettings(communityId).url,
      authenticate: true,
      data: dataToUse,
      files: files
    })
  }

  updateConformanceOverviewCertificateSettings(communityId: number, reportSettings: CommunityReportSettings, certificateSettings: ConformanceOverviewCertificateSettings, stylesheet?: FileData) {
    let files: FileParam[]|undefined
    if (reportSettings.customPdfsWithCustomXml && stylesheet?.file) {
      files = [{ param: "file", data: stylesheet.file}]
    }
    let dataToUse = this.reportSettingsToData(Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_CERTIFICATE, reportSettings.customPdfsWithCustomXml, reportSettings)
    dataToUse["settings"] = JSON.stringify(certificateSettings)
    return this.restService.post<void>({
      path: ROUTES.controllers.RepositoryService.updateConformanceOverviewCertificateSettings(communityId).url,
      authenticate: true,
      data: dataToUse,
      files: files
    })
  }

  exportDemoConformanceCertificateReport(communityId: number, reportSettings: CommunityReportSettings, certificateSettings: ConformanceCertificateSettings, stylesheet?: FileData) {
    let files: FileParam[]|undefined
    if (reportSettings.customPdfsWithCustomXml && stylesheet?.file) {
      files = [{ param: "file", data: stylesheet.file}]
    }
    let dataToUse = this.reportSettingsToData(Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_CERTIFICATE, reportSettings.customPdfsWithCustomXml, reportSettings)
    dataToUse["settings"] = JSON.stringify(certificateSettings)
    return this.restService.post<HttpResponse<ArrayBuffer>>({
      path: ROUTES.controllers.RepositoryService.exportDemoConformanceCertificateReport(communityId).url,
      data: dataToUse,
      authenticate: true,
      arrayBuffer: true,
      httpResponse: true,
      files: files
    })
  }

  exportDemoConformanceOverviewCertificateReport(communityId: number, reportSettings: CommunityReportSettings, certificateSettings: ConformanceOverviewCertificateSettings, reportLevel: string, reportLevelIdentifier?: number, stylesheet?: FileData) {
    let files: FileParam[]|undefined
    if (reportSettings.customPdfsWithCustomXml && stylesheet?.file) {
      files = [{ param: "file", data: stylesheet.file}]
    }
    let dataToUse = this.reportSettingsToData(Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_CERTIFICATE, reportSettings.customPdfsWithCustomXml, reportSettings)
    dataToUse["settings"] = JSON.stringify(certificateSettings)
    dataToUse["level"] = reportLevel
    dataToUse["id"] = reportLevelIdentifier
    return this.restService.post<HttpResponse<ArrayBuffer>>({
      path: ROUTES.controllers.RepositoryService.exportDemoConformanceOverviewCertificateReport(communityId).url,
      data: dataToUse,
      files: files,
      authenticate: true,
      arrayBuffer: true,
      httpResponse: true
    })
  }

  exportOwnConformanceOverviewCertificateReport(systemId: number, domainId: number|undefined, groupId: number|undefined, specificationId: number|undefined, snapshotId: number|undefined) {
    let data: any = {
      system_id: systemId
    }
    if (domainId != undefined) data.domain_id = domainId
    if (groupId != undefined) data.group_id = groupId
    if (specificationId != undefined) data.spec_id = specificationId
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportOwnConformanceOverviewCertificateReport().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportOwnConformanceCertificateReport(actorId: number, systemId: number, snapshotId?: number) {
    let data: any = {
      actor_id: actorId,
      system_id: systemId
    }
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportOwnConformanceCertificateReport().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportConformanceOverviewCertificate(communityId: number, systemId: number, domainId: number|undefined, groupId: number|undefined, specId: number|undefined, settings: ConformanceOverviewCertificateSettings|undefined, snapshotId: number|undefined) {
    let data: any = {
      system_id: systemId,
      community_id: communityId,
    }
    if (settings != undefined) {
      let settingsData: any = {
        title: settings.title,
        includeTitle: settings.includeTitle == true,
        includeMessage: settings.includeMessage == true,
        includeTestStatus: settings.includeTestStatus == true,
        includeTestCases: settings.includeTestCases == true,
        includeTestCaseDetails: settings.includeTestCaseDetails == true,
        includeDetails: settings.includeDetails == true,
        includeSignature: settings.includeSignature == true,
        includePageNumbers: settings.includePageNumbers == true
      }
      if (settingsData.includeMessage) {
        settingsData.messages = settings.messages
      }
      data.settings = JSON.stringify(settingsData)
    }
    if (domainId != undefined) data.domain_id = domainId
    if (groupId != undefined) data.group_id = groupId
    if (specId != undefined) data.spec_id = specId
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceOverviewCertificateReport().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportConformanceOverviewReport(systemId: number, domainId: number|undefined, groupId: number|undefined, specId: number|undefined, snapshotId: number|undefined) {
    let data: any = {
      system_id: systemId
    }
    if (domainId != undefined) data.domain_id = domainId
    if (groupId != undefined) data.group_id = groupId
    if (specId != undefined) data.spec_id = specId
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceOverviewReport().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportConformanceCertificate(communityId: number, actorId: number, systemId: number, settings: ConformanceCertificateSettings|undefined, snapshotId?: number) {
    let data: any = {
      community_id: communityId,
      actor_id: actorId,
      system_id: systemId
    }
    if (settings != undefined) {
      let settingsData: any = {
        title: settings.title,
        includeTitle: settings.includeTitle == true,
        includeMessage: settings.includeMessage == true,
        includeTestStatus: settings.includeTestStatus == true,
        includeTestCases: settings.includeTestCases == true,
        includeDetails: settings.includeDetails == true,
        includeSignature: settings.includeSignature == true,
        includePageNumbers: settings.includePageNumbers == true
      }
      if (settingsData.includeMessage) {
        settingsData.message = settings.message
      }
      data.settings = JSON.stringify(settingsData)
    }
    if (snapshotId != undefined) {
      data.snapshot = snapshotId
    }
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceCertificateReport().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportTestStepReport(sessionId: string, reportPath: string, reportContentType: string) {
    return this.restService.get<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportTestStepReport(sessionId, reportPath).url,
      authenticate: true,
      arrayBuffer: true,
      accept: reportContentType
    })
  }

  exportTestCaseDocumentationPreviewReport(documentation: string) {
    return this.restService.post<ArrayBuffer>(({
      path: ROUTES.controllers.TestSuiteService.previewTestCaseDocumentationInReports().url,
      data: {
        documentation: documentation
      },
      authenticate: true,
      arrayBuffer: true
    }))
  }

  exportConformanceOverviewReportInXML(communityId: number, systemId: number, domainId: number|undefined, groupId: number|undefined, specId: number|undefined, snapshotId: number|undefined) {
    let data:any = {
      community_id: communityId,
      system_id: systemId
    }
    if (domainId != undefined) data.domain_id = domainId
    if (groupId != undefined) data.group_id = groupId
    if (specId != undefined) data.spec_id = specId
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceOverviewReportInXML().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportOwnConformanceOverviewReportInXML(systemId: number, domainId: number|undefined, groupId: number|undefined, specId: number|undefined, snapshotId: number|undefined) {
    let data:any = {
      system_id: systemId,
    }
    if (domainId != undefined) data.domain_id = domainId
    if (groupId != undefined) data.group_id = groupId
    if (specId != undefined) data.spec_id = specId
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportOwnConformanceOverviewReportInXML().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportConformanceStatementReportInXML(actorId: number, systemId: number, communityId: number, includeTests: boolean, snapshotId?: number) {
    let data:any = {
      community_id: communityId,
      actor_id: actorId,
      system_id: systemId,
      tests: includeTests
    }
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceStatementReportInXML().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportOwnConformanceStatementReportInXML(actorId: number, systemId: number, includeTests: boolean, snapshotId: number|undefined) {
    let data:any = {
      actor_id: actorId,
      system_id: systemId,
      tests: includeTests
    }
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportOwnConformanceStatementReportInXML().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportConformanceStatementReport(actorId: number, systemId: number, includeTests: boolean, snapshotId?: number) {
    let data:any = {
      actor_id: actorId,
      system_id: systemId,
      tests: includeTests
    }
    if (snapshotId != undefined) data.snapshot = snapshotId
    return this.restService.post<ArrayBuffer>({
      path: ROUTES.controllers.RepositoryService.exportConformanceStatementReport().url,
      data: data,
      authenticate: true,
      arrayBuffer: true
    })
  }

  exportTestCaseReport(session: string, testCaseId: number, contentType: string) {
    return this.restService.get<ArrayBuffer>(({
      path: ROUTES.controllers.RepositoryService.exportTestCaseReport().url,
      params: {
        session_id: session,
        test_id: testCaseId
      },
      authenticate: true,
      arrayBuffer: true,
      accept: contentType
    }))
  }

}
