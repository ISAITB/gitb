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

import {Component, OnInit, ViewChild} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {RoutingService} from 'src/app/services/routing.service';
import {Domain} from 'src/app/types/domain';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {TableApi} from '../../../components/table/table-api';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {Observable, of} from 'rxjs';
import {SearchResult} from '../../../types/search-result';

@Component({
    selector: 'app-domain-management',
    templateUrl: './domain-management.component.html',
    styles: [],
    standalone: false
})
export class DomainManagementComponent implements OnInit {

  @ViewChild("domainTable") domainTable?: TableApi

  domainStatus = {status: Constants.STATUS.PENDING}
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description'}
  ]
  domains: Domain[] = []
  domainFilter?: string
  domainsRefreshing = false

  constructor(
    public readonly dataService: DataService,
    private readonly conformanceService: ConformanceService,
    private readonly routingService: RoutingService
  ) { }

  ngOnInit(): void {
		this.refreshDomains()
    this.routingService.domainsBreadcrumbs()
  }

	onDomainSelect(domain: Domain) {
    this.routingService.toDomain(domain.id)
  }

  create() {
    this.routingService.toCreateDomain()
  }

  applyFilter() {
    this.refreshDomains()
  }

  loadDomains(pagingInfo: PagingEvent) {
    if (this.domainStatus.status == Constants.STATUS.FINISHED) {
      this.domainsRefreshing = true
    } else {
      this.domainStatus.status = Constants.STATUS.PENDING
    }
    let $domains: Observable<SearchResult<Domain>>
    if (this.dataService.isSystemAdmin) {
      $domains = this.conformanceService.searchDomains(this.domainFilter, pagingInfo.targetPage, pagingInfo.targetPageSize)
    } else if (this.dataService.isCommunityAdmin) {
      $domains = this.conformanceService.searchCommunityDomains(this.dataService.community!.id, this.domainFilter, pagingInfo.targetPage, pagingInfo.targetPageSize)
    } else {
      $domains = of({ data: [], count: 0 })
    }
    $domains.subscribe((data: SearchResult<Domain>) => {
      this.domains = data.data
      this.updatePagination(pagingInfo.targetPage, data.count!)
    }).add(() => {
      this.domainsRefreshing = false
      this.domainStatus.status = Constants.STATUS.FINISHED
    })
  }

  refreshDomains() {
    this.loadDomains({ targetPage: 1, targetPageSize: this.domainTable?.getPagingControls()?.getCurrentStatus().pageSize! })
  }

  doDomainPaging(event: PagingEvent) {
    this.loadDomains(event)
  }

  private updatePagination(page: number, count: number) {
    this.domainTable?.getPagingControls()?.updateStatus(page, count)
  }

}
