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

import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { Specification } from 'src/app/types/specification';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
    selector: 'app-create-specification',
    templateUrl: './create-specification.component.html',
    styles: [],
    standalone: false
})
export class CreateSpecificationComponent extends BaseComponent implements OnInit, AfterViewInit {

  domainId!: number
  specification: Partial<Specification> = {}
  pending = false
  groups?: SpecificationGroup[]

  constructor(
    public readonly dataService: DataService,
    private readonly conformanceService: ConformanceService,
    private readonly popupService: PopupService,
    private readonly route: ActivatedRoute,
    private readonly routingService: RoutingService,
    private readonly specificationService: SpecificationService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('shortName')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    const groupId = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SPECIFICATION_GROUP_ID)
    if (groupId) {
      this.specification.group = Number(groupId)
    }
    this.specificationService.getDomainSpecificationGroups(this.domainId)
    .subscribe((data) => {
      this.specification.groups = data
    })
    this.specification.badges = {
      enabled: false,
      initiallyEnabled: false,
      success: { enabled: false },
      other: { enabled: false },
      failure: { enabled: false },
      successForReport: { enabled: false },
      otherForReport: { enabled: false },
      failureForReport: { enabled: false },
      failureBadgeActive: false,
      successBadgeForReportActive: false,
      otherBadgeForReportActive: false,
      failureBadgeForReportActive: false
    }
  }

	saveDisabled() {
    return !(
      this.textProvided(this.specification?.sname) &&
      this.textProvided(this.specification?.fname) &&
      this.dataService.badgesValid(this.specification?.badges)
    )
  }

	createSpecification() {
		if (!this.saveDisabled()) {
      this.pending = true
      this.conformanceService.createSpecification(this.specification.sname!, this.specification.fname!, this.specification.description, this.specification.reportMetadata, this.specification.hidden, this.domainId, this.specification.group, this.specification.badges!)
      .subscribe(() => {
        this.routingService.toDomain(this.domainId)
        this.popupService.success(this.dataService.labelSpecification()+' created.')
      }).add(() => {
        this.pending = false
      })
    }
  }

	cancel() {
    let domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.routingService.toDomain(domainId)
  }

}
