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

import {AfterViewInit, ChangeDetectionStrategy, Component, EventEmitter, HostListener, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Constants} from '../../common/constants';
import {CustomContentTypes} from '../../common/custom-content-types';
import {TableColumns} from '../../common/table-columns';
import {CustomContentService} from '../../services/custom-content.service';
import {DataService} from '../../services/data.service';
import {RoutingService} from '../../services/routing.service';
import {CustomContent} from '../../types/custom-content';
import {CustomContentState} from '../../types/custom-content-state';
import {NavigationTarget} from '../../types/navigation-target';
import {TableColumnDefinition} from '../../types/table-column-definition.type';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';
import {PagingEvent} from '../paging-controls/paging-event';
import {TableApi} from '../table/table-api';

/**
 * The management tab for the custom content (landing pages, legal notices and error templates) of a community or,
 * for the Test Bed defaults, of the default community. Its state is kept by the parent page to survive tab changes.
 */
@Component({
  selector: 'app-custom-content-tab',
  standalone: false,
  templateUrl: './custom-content-tab.component.html',
  changeDetection: ChangeDetectionStrategy.Eager
})
export class CustomContentTabComponent implements OnInit, AfterViewInit {

  @Input() state!: CustomContentState
  @Input() communityId!: number
  @Output() pageSizeChanged = new EventEmitter<void>()
  @ViewChild("contentTable") contentTable?: TableApi
  @ViewChild("showContentFilter") showContentFilter?: CheckBoxOptionPanelComponentApi

  protected readonly Constants = Constants
  protected readonly CustomContentTypes = CustomContentTypes
  contentRefreshing = false
  typeOptions: CheckboxOption[][] = [
    CustomContentTypes.ALL.map((type) => {
      return { key: String(type), label: CustomContentTypes.label(type)+'s', default: true, iconClass: CustomContentTypes.icon(type) }
    })
  ]
  columns: TableColumnDefinition[] = []
  private readonly baseColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    TableColumns.valuePill('type', 'Type', (type: number) => type ? { text: CustomContentTypes.label(type) } : undefined)
  ]

  constructor(
    private readonly customContentService: CustomContentService,
    private readonly routingService: RoutingService,
    private readonly dataService: DataService
  ) { }

  get testBedDefaultsView() {
    return this.communityId == Constants.DEFAULT_COMMUNITY_ID
  }

  ngOnInit(): void {
    this.columns = [...this.baseColumns, TableColumns.flag('default', Constants.BUTTON_ICON.DEFAULT, this.testBedDefaultsView ? 'Test Bed default.' : 'Community default.')]
  }

  ngAfterViewInit(): void {
    if (this.state.status == Constants.STATUS.NONE) {
      this.queryContent({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
    } else {
      this.updatePagination(this.state.page, this.state.total)
    }
  }

  @HostListener('document:click', ['$event'])
  clickRegistered(event: Event) {
    this.showContentFilter?.documentClick(event)
  }

  @HostListener('document:keyup.escape')
  escapeRegistered() {
    this.showContentFilter?.documentEscape()
  }

  filterByType(choices: CheckboxOptionState) {
    this.state.types = CustomContentTypes.ALL.filter((type) => choices[String(type)] === true)
    this.queryContent({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
  }

  doPageNavigation(event: PagingEvent) {
    this.queryContent(event)
    if (event.pageSizeChanged) {
      this.pageSizeChanged.emit()
    }
  }

  private queryContent(pagingInfo: PagingEvent) {
    if (this.state.status == Constants.STATUS.FINISHED) {
      this.contentRefreshing = true
    } else {
      this.state.status = Constants.STATUS.PENDING
    }
    if (!this.testBedDefaultsView && this.state.testBedDefaults == undefined) {
      // Determine for which types the Test Bed has a default that can be copied.
      this.state.testBedDefaults = {}
      this.customContentService.getTestBedDefaultTypes().subscribe((data) => {
        this.state.testBedDefaults = {
          [Constants.CUSTOM_CONTENT_TYPE.LANDING_PAGE]: data.landingPage,
          [Constants.CUSTOM_CONTENT_TYPE.LEGAL_NOTICE]: data.legalNotice,
          [Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE]: data.errorTemplate
        }
      })
    }
    const types = this.state.types ?? CustomContentTypes.ALL
    if (types.length == 0) {
      // Nothing to show.
      this.state.content = []
      this.updatePagination(1, 0)
      this.contentRefreshing = false
      this.state.status = Constants.STATUS.FINISHED
    } else {
      this.customContentService.searchCustomContentByCommunity(this.communityId, types, pagingInfo.targetPage, pagingInfo.targetPageSize)
        .subscribe((data) => {
          this.state.content = data.data
          this.updatePagination(pagingInfo.targetPage, data.count!)
        }).add(() => {
          this.contentRefreshing = false
          this.state.status = Constants.STATUS.FINISHED
        })
    }
  }

  private updatePagination(page: number, count: number) {
    this.contentTable?.getPagingControls()?.updateStatus(page, count)
    this.state.page = page
    this.state.total = count
  }

  hasTestBedDefault(type: number) {
    return this.state.testBedDefaults?.[type] === true
  }

  noTestBedDefaults() {
    return !CustomContentTypes.ALL.some((type) => this.hasTestBedDefault(type))
  }

  create(type: number) {
    void this.routingService.toCreateCustomContent(type, this.testBedDefaultsView ? undefined : this.communityId, false)
  }

  copyTestBedDefault(type: number) {
    void this.routingService.toCreateCustomContent(type, this.communityId, true)
  }

  rowTarget = (content: CustomContent): NavigationTarget => {
    return this.routingService.linkToCustomContent(content.type, this.testBedDefaultsView ? undefined : this.communityId, content.id)
  }

}
