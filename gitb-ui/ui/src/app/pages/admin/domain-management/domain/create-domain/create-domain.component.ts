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
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Domain } from 'src/app/types/domain';

@Component({
    selector: 'app-create-domain',
    templateUrl: './create-domain.component.html',
    styles: [],
    standalone: false
})
export class CreateDomainComponent extends BaseComponent implements OnInit, AfterViewInit {

  domain: Partial<Domain> = {}
  pending = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('shortName')
  }

  ngOnInit(): void {
  }

	saveDisabled() {
    return !this.textProvided(this.domain.sname) || !this.textProvided(this.domain.fname)
  }

	createDomain() {
		if (!this.saveDisabled()) {
      this.pending = true
			this.conformanceService.createDomain(this.domain.sname!, this.domain.fname!, this.domain.description, this.domain.reportMetadata)
      .subscribe(() => {
        this.popupService.success(this.dataService.labelDomain()+' created.')
        this.routingService.toDomains()
      }).add(() => {
        this.pending = false
      })
    }
  }

	cancel() {
    this.routingService.toDomains()
  }

}
