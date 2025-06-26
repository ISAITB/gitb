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
import { Actor } from 'src/app/types/actor';

@Component({
    selector: 'app-create-actor',
    templateUrl: './create-actor.component.html',
    styles: [],
    standalone: false
})
export class CreateActorComponent extends BaseComponent implements OnInit, AfterViewInit {

  domainId!: number
  specificationId!:number
  actor: Partial<Actor> = {}
  savePending = false

  constructor(
    public readonly dataService: DataService,
    private readonly popupService: PopupService,
    private readonly conformanceService: ConformanceService,
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('id')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.specificationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID))
    this.actor.badges = {
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
      this.textProvided(this.actor?.actorId) &&
      this.textProvided(this.actor?.name) &&
      this.numberOrEmpty(this.actor?.displayOrder) &&
      this.dataService.badgesValid(this.actor?.badges)
    )
  }

	createActor() {
		if (!this.saveDisabled()) {
      this.savePending = true
      this.conformanceService.createActor(this.actor.actorId!, this.actor.name!, this.actor.description, this.actor.reportMetadata, this.actor.default, this.actor.hidden, this.actor.displayOrder, this.domainId, this.specificationId, this.actor.badges!)
      .subscribe(() => {
        this.cancel()
        this.popupService.success(this.dataService.labelActor()+' created.')
      }).add(() => {
        this.savePending = false
      })
    }
  }

	cancel() {
    this.routingService.toSpecification(this.domainId, this.specificationId, Constants.TAB.SPECIFICATION.ACTORS)
  }

}
