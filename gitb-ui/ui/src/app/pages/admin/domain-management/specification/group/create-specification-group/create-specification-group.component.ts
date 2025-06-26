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
import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
    selector: 'app-create-specification-group',
    templateUrl: './create-specification-group.component.html',
    styles: [],
    standalone: false
})
export class CreateSpecificationGroupComponent extends BaseComponent implements OnInit {

  group: Partial<SpecificationGroup> = {}
  pending = false

  constructor(
    public readonly dataService: DataService,
    private readonly specificationService: SpecificationService,
    private readonly popupService: PopupService,
    private readonly route: ActivatedRoute,
    private readonly routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
		this.dataService.focus('shortName')
  }

	saveDisabled() {
		return !(this.textProvided(this.group.sname) && this.textProvided(this.group.fname))
  }

  createSpecificationGroup() {
		if (!this.saveDisabled()) {
			let domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
      this.pending = true
      this.specificationService.createSpecificationGroup(this.group.sname!, this.group.fname!, this.group.description, this.group.reportMetadata, domainId)
      .subscribe(() => {
        this.routingService.toDomain(domainId)
        this.popupService.success(this.dataService.labelSpecificationGroup()+' created.')
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
