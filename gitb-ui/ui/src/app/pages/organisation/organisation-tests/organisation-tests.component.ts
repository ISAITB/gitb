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

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {DiagramLoaderService} from 'src/app/components/diagram/test-session-presentation/diagram-loader.service';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {ReportService} from 'src/app/services/report.service';
import {TestService} from 'src/app/services/test.service';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {TestResultReport} from 'src/app/types/test-result-report';
import {TestResultSearchCriteria} from 'src/app/types/test-result-search-criteria';
import {Observable} from 'rxjs';
import {RoutingService} from 'src/app/services/routing.service';
import {FieldInfo} from 'src/app/types/field-info';
import {BaseSessionDashboardComponent} from '../../sessions/base-session-dashboard.component';
import {TestResultData} from '../../../types/test-result-data';
import {TestResultForExport} from '../../admin/session-dashboard/test-result-for-export';

@Component({
    selector: 'app-organisation-tests',
    templateUrl: './../../sessions/base-session-dashboard.component.html',
    styles: [],
    standalone: false
})
export class OrganisationTestsComponent extends BaseSessionDashboardComponent implements OnInit {

  domainId?: number

  constructor(
    route: ActivatedRoute,
    reportService: ReportService,
    conformanceService: ConformanceService,
    dataService: DataService,
    confirmationDialogService: ConfirmationDialogService,
    testService: TestService,
    popupService: PopupService,
    diagramLoaderService: DiagramLoaderService,
    routingService: RoutingService
  ) { super(dataService, conformanceService, reportService, confirmationDialogService, testService, popupService, route, diagramLoaderService, routingService) }

  ngOnInit(): void {
    this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    if (!this.dataService.isSystemAdmin && this.dataService.community?.domainId != undefined) {
      this.domainId = this.dataService.community.domainId
    }
    if (this.domainId == undefined) {
      this.filterState.filters.push(Constants.FILTER_TYPE.DOMAIN)
    }
    super.ngOnInit()
  }

  protected getActiveTestsColumns(): TableColumnDefinition[] {
    return [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true, order: 'asc' }
    ]
  }

  protected getCompletedTestsColumns(): TableColumnDefinition[] {
    return [
      { field: 'specification', title: this.dataService.labelSpecification(), sortable: true },
      { field: 'actor', title: this.dataService.labelActor(), sortable: true },
      { field: 'testCase', title: 'Test case', sortable: true },
      { field: 'system', title: this.dataService.labelSystem(), sortable: true },
      { field: 'startTime', title: 'Start time', sortable: true },
      { field: 'endTime', title: 'End time', sortable: true, order: 'desc' },
      { field: 'result', title: 'Result', sortable: true, iconFn: this.dataService.iconForTestResult, iconTooltipFn: this.dataService.tooltipForTestResult }
    ]
  }

  protected setBreadcrumbs() {
    this.routingService.testHistoryBreadcrumbs(this.organisationId!)
  }

  protected showCopyForOtherRoleOption(): boolean {
    return this.dataService.isVendorUser || this.dataService.isVendorAdmin
  }

  protected showTogglePendingAdminInteractionControl(): boolean {
    return false
  }

  protected addExtraSearchCriteria(searchCriteria: TestResultSearchCriteria, filterData: { [p: string]: any } | undefined) {
    if (this.domainId != undefined) {
      searchCriteria.domainIds = [this.domainId]
    } else if (filterData) {
      searchCriteria.domainIds = filterData[Constants.FILTER_TYPE.DOMAIN]
    }
  }

  protected loadActiveTests(page: number, pageSize: number, params: TestResultSearchCriteria, forExport?: boolean): Observable<TestResultData> {
    return this.reportService.getSystemActiveTestResults(page, pageSize, this.organisationId!, params)
  }

  protected loadCompletedTests(page: number, pageSize: number, params: TestResultSearchCriteria, forExport?: boolean): Observable<TestResultData> {
    return this.reportService.getTestResults(this.organisationId!, page, pageSize, params);
  }

  protected addExtraExportData(data: TestResultData, fields: FieldInfo[]) {
    // Nothing.
  }

  protected mapExtraDataToResult(result: TestResultForExport, originalResult: TestResultReport, data: TestResultData) {
    // Nothing.
  }

  protected getExportFieldInfoForCompletedTests(): FieldInfo[] {
    return [
      { header: 'Session', field: 'session'},
      { header: this.dataService.labelDomain(), field: 'domain'},
      { header: this.dataService.labelSpecification(), field: 'specification'},
      { header: this.dataService.labelActor(), field: 'actor'},
      { header: 'Test suite', field: 'testSuite'},
      { header: 'Test case', field: 'testCase'},
      { header: this.dataService.labelSystem(), field: 'system'},
      { header: 'Start time', field: 'startTime'},
      { header: 'End time', field: 'endTime'},
      { header: 'Result', field: 'result'},
      { header: 'Obsolete', field: 'obsolete'}
    ]
  }

  protected getExportFieldInfoForActiveTests(): FieldInfo[] {
    return [
      { header: 'Session', field: 'session'},
      { header: this.dataService.labelDomain(), field: 'domain'},
      { header: this.dataService.labelSpecification(), field: 'specification'},
      { header: this.dataService.labelActor(), field: 'actor'},
      { header: 'Test suite', field: 'testSuite'},
      { header: 'Test case', field: 'testCase'},
      { header: this.dataService.labelSystem(), field: 'system'},
      { header: 'Start time', field: 'startTime'},
    ]
  }

  protected stopAllOperation(): Observable<void> {
    return this.testService.stopAllOrganisationSessions(this.organisationId!)
  }

  protected deleteObsoleteOperation(): Observable<void> {
    return this.conformanceService.deleteObsoleteTestResultsForOrganisation(this.organisationId!)
  }

}
