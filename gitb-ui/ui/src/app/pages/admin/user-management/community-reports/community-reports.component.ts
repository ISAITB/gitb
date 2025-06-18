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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { RoutingService } from 'src/app/services/routing.service';
import { ReportSettings } from './report-settings';

@Component({
    selector: 'app-community-reports',
    templateUrl: './community-reports.component.html',
    styleUrl: './community-reports.component.less',
    standalone: false
})
export class CommunityReportsComponent implements OnInit {

  communityId!: number

  conformanceStatementCertificateSettings = new ReportSettings()
  conformanceStatementReportSettings = new ReportSettings()
  conformanceOverviewCertificateSettings = new ReportSettings()
  conformanceOverviewReportSettings = new ReportSettings()
  testCaseReportSettings = new ReportSettings()
  testStepReportSettings = new ReportSettings()

  constructor(
    private route: ActivatedRoute,
    private routingService: RoutingService
  ) {}

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    this.routingService.communityReportSettingsBreadcrumbs(this.communityId)
  }

  back() {
    this.routingService.toCommunity(this.communityId)
  }

}
