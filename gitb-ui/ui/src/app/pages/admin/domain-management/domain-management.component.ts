/*
 * Copyright (C) 2026 European Union
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

import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
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
import {UsageTipService} from '../../../services/usage-tip.service';
import {BaseComponent} from '../../base-component.component';
import {DisplayState} from '../../../types/display-state';

/** Persisted search/paging state for the Domains list - restored when returning here (e.g. via Back
 * from a domain's detail page). */
interface DomainManagementListState {
  filter?: string
}

@Component({
    selector: 'app-domain-management',
    templateUrl: './domain-management.component.html',
    styles: [],
    standalone: false
})
export class DomainManagementComponent extends BaseComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild("domainTable") domainTable?: TableApi

  domainStatus = {status: Constants.STATUS.PENDING}
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description'},
    { field: 'tags', title: '', tagData: true, headerClass: 'th-min', cellClass: 'td-min centered' }
  ]
  domains: Domain[] = []
  domainFilter?: string
  domainsRefreshing = false

  constructor(
    public readonly dataService: DataService,
    private readonly conformanceService: ConformanceService,
    private readonly routingService: RoutingService,
    private readonly usageTipService: UsageTipService
  ) { super() }

  ngOnInit(): void {
    let targetPaging: PagingEvent = { targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize }
    const existingState = this.getDisplayState<DomainManagementListState>(Constants.DISPLAY_STATE_KEY.DOMAINS, true)
    if (existingState) {
      if (existingState.state) {
        this.domainFilter = existingState.state.filter
      }
      if (existingState.paging) {
        targetPaging = { targetPage: existingState.paging.currentPage, targetPageSize: existingState.paging.pageSize }
      }
    }
    this.loadDomains(targetPaging)
    this.routingService.domainsBreadcrumbs()
  }

  ngOnDestroy(): void {
    const state: DisplayState<DomainManagementListState> = {
      key: Constants.DISPLAY_STATE_KEY.DOMAINS,
      state: { filter: this.domainFilter },
      paging: this.domainTable?.getPagingControls()?.getCurrentStatus()
    }
    this.saveDisplayState(Constants.DISPLAY_STATE_KEY.DOMAINS, state)
  }

  ngAfterViewInit(): void {
    if (this.dataService.isSystemAdmin) {
      this.usageTipService.showUsageTip(Constants.USAGE_TIP.TEST_BED_ADMIN_DOMAINS)
    }
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
    this.loadDomains({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
  }

  doDomainPaging(event: PagingEvent) {
    this.loadDomains(event)
  }

  private updatePagination(page: number, count: number) {
    this.domainTable?.getPagingControls()?.updateStatus(page, count)
  }

  protected readonly Constants = Constants;
}
