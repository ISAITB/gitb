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
