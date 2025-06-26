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
import { Constants } from 'src/app/common/constants';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Domain } from 'src/app/types/domain';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';

@Component({
    selector: 'app-domain-management',
    templateUrl: './domain-management.component.html',
    styles: [],
    standalone: false
})
export class DomainManagementComponent implements OnInit {

  dataStatus = {status: Constants.STATUS.PENDING}
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description'}
  ]
  domains: Domain[] = []


  constructor(
    public readonly dataService: DataService,
    private readonly conformanceService: ConformanceService,
    private readonly routingService: RoutingService
  ) { }

  ngOnInit(): void {
		this.getDomains()
    this.routingService.domainsBreadcrumbs()
  }

	getDomains() {
		if (this.dataService.isSystemAdmin) {
			this.conformanceService.getDomains()
			.subscribe((data) => {
				this.domains = data
				this.dataStatus.status = Constants.STATUS.FINISHED
      }).add(() => {
				this.dataStatus.status = Constants.STATUS.FINISHED
      })
    } else if (this.dataService.isCommunityAdmin) {
			this.conformanceService.getCommunityDomains(this.dataService.community!.id)
			.subscribe((data) => {
        if (data) {
          this.domains = data.domains
        }
      }).add(() => {
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

	onDomainSelect(domain: Domain) {
    this.routingService.toDomain(domain.id)
  }

  create() {
    this.routingService.toCreateDomain()
  }

}
