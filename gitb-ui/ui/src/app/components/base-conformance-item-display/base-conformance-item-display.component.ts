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

import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, ViewChild } from '@angular/core';
import { CheckboxOption } from '../checkbox-option-panel/checkbox-option';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { DataService } from 'src/app/services/data.service';
import { find } from 'lodash';
import { Constants } from 'src/app/common/constants';
import { CheckboxOptionState } from '../checkbox-option-panel/checkbox-option-state';
import {ExportReportEvent} from '../../types/export-report-event';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseConformanceItemDisplayComponent implements AfterViewInit, OnDestroy {

  @ViewChild('searchControls') searchControls?: ElementRef
  @ViewChild('selectorControls') selectorControls?: ElementRef
  @ViewChild('conformanceItemPage') conformanceItemPage?: ElementRef

  protected static SHOW_SUCCEEDED = '0'
  protected static SHOW_FAILED = '1'
  protected static SHOW_INCOMPLETE = '2'

  statementFilter?: string
  showCompleted = true
  showFailed = true
  showIncomplete = true
  statusOptions: CheckboxOption[][] = [
    [
      {key: BaseConformanceItemDisplayComponent.SHOW_SUCCEEDED, label: 'Succeeded statements', default: this.showCompleted, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)},
      {key: BaseConformanceItemDisplayComponent.SHOW_FAILED, label: 'Failed statements', default: this.showFailed, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)},
      {key: BaseConformanceItemDisplayComponent.SHOW_INCOMPLETE, label: 'Incomplete statements', default: this.showIncomplete, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)},
    ]
  ]
  visibleItemCount = 0
  animated = true
  resizeObserver!: ResizeObserver
  searchControlsWrapped = false
  statements: ConformanceStatementItem[] = []
  itemsByType?: { groups: ConformanceStatementItem[], specs: ConformanceStatementItem[], actors: ConformanceStatementItem[] }

  constructor(
    public dataService: DataService,
    private zone: NgZone
  ) {}

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => {
      this.zone.run(() => {
        this.calculateWrapping()
      })
    })
    if (this.conformanceItemPage) {
      this.resizeObserver.observe(this.conformanceItemPage.nativeElement)
    }
  }

  resetStatementFilters() {
    this.statementFilter = undefined
    this.showCompleted = true
    this.showFailed = true
    this.showIncomplete = true
  }

  filterByStatus(choices: CheckboxOptionState) {
    this.showCompleted = choices[BaseConformanceItemDisplayComponent.SHOW_SUCCEEDED]
    this.showFailed = choices[BaseConformanceItemDisplayComponent.SHOW_FAILED]
    this.showIncomplete = choices[BaseConformanceItemDisplayComponent.SHOW_INCOMPLETE]
    this.filterStatements()
  }

  filterStatements() {
    this.animated = false
    setTimeout(() => {
      if (this.itemsByType) {
        const defaultFilteredValue = this.statementFilter == undefined && this.showCompleted && this.showFailed && this.showIncomplete
        this.statements.forEach((item) => { item.filtered = defaultFilteredValue; })
        this.itemsByType.groups.forEach((item) => { item.filtered = defaultFilteredValue; })
        this.itemsByType.specs.forEach((item) => { item.filtered = defaultFilteredValue; })
        this.itemsByType.actors.forEach((item) => { item.filtered = defaultFilteredValue; })
        if (!defaultFilteredValue) {
          // Apply filters per item type - only needed if we are going to mark items as filtered (visible)
          this.filterItems(this.statements)
          this.filterItems(this.itemsByType.groups)
          this.filterItems(this.itemsByType.specs)
          this.filterItems(this.itemsByType.actors)
        }
        // Apply match semantics to hierarchy.
        this.filterParentsWithFilteredChildren(this.itemsByType.specs)
        this.filterParentsWithFilteredChildren(this.itemsByType.groups)
        this.filterParentsWithFilteredChildren(this.statements)
        // Update visible item count.
        this.countVisibleItems()
      }
      this.toggleAnimated(true)
    }, 1)
  }

  private toggleAnimated(animatedValue: boolean) {
    setTimeout(() => {
      this.animated = animatedValue
    }, 1)
  }

  private filterItems(items: ConformanceStatementItem[]|undefined) {
    if (items) {
      for (let item of items) {
        this.filterItem(item)
      }
    }
  }

  private filterItem(item: ConformanceStatementItem) {
    if (!item.hidden) {
      item.filteredByText = false
      // Text filter.
      if (this.statementFilter) {
        const filterToApply = this.statementFilter.trim().toLowerCase()
        if ((item.name.toLowerCase().indexOf(filterToApply)) >= 0) {
          item.filteredByText = true
        }
      }
      // Status filter
      item.filteredByStatus = false
      const statusFilterApplied = !this.showCompleted || !this.showFailed || !this.showIncomplete
      if (statusFilterApplied) {
        if ((this.showCompleted && this.checkItemStatus(item, Constants.TEST_CASE_RESULT.SUCCESS))
            || (this.showFailed && this.checkItemStatus(item, Constants.TEST_CASE_RESULT.FAILURE))
            || (this.showIncomplete && this.checkItemStatus(item, Constants.TEST_CASE_RESULT.UNDEFINED))) {
              item.filteredByStatus = true
        }
      }
      if ((!this.statementFilter || item.filteredByText) && (!statusFilterApplied || item.filteredByStatus)) {
        item.filtered = true
      }
    }
  }

  private checkItemsStatus(items: ConformanceStatementItem[], statusToLookFor: string): boolean {
    if (items) {
      for (let child of items) {
        let childHasStatus = this.checkItemStatus(child, statusToLookFor)
        if (childHasStatus) {
          return true
        }
      }
    }
    return false
  }


  private checkItemStatus(item: ConformanceStatementItem, statusToLookFor: string): boolean {
    if (item.results) {
      return this.dataService.conformanceStatusForTests(item.results.completedToConsider, item.results.failedToConsider, item.results.undefinedToConsider) == statusToLookFor
    } else if (item.items) {
      return this.checkItemsStatus(item.items, statusToLookFor)
    } else {
      return false // We should never reach this case.
    }
  }

  private filterParentsWithFilteredChildren(items: ConformanceStatementItem[]) {
    for (let item of items) {
      const hasChild = item.items != undefined && item.items.length > 0
      const hasVisibleChild = hasChild && (find(item.items, (child) => {
        return child.filtered == true
      }) != undefined)
      if (hasVisibleChild) {
        item.collapsed = false
      }
      if (!item.filtered) {
        item.filtered = hasVisibleChild
      } else {
        // Apply filtering logic to children.
        this.dataService.visitConformanceItems(item.items, (item) => {
          if (item.filteredByStatus) {
            item.filtered = true
          }
        })
      }
    }
  }

  private countVisibleItems() {
    let count = 0
    this.dataService.visitConformanceItems(this.statements, (item) => {
      if (item.filtered == true) {
        count += 1
      }
    })
    this.visibleItemCount = count
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      if (this.conformanceItemPage) {
        this.resizeObserver.unobserve(this.conformanceItemPage.nativeElement)
      }
    }
  }

  protected calculateWrapping() {
    if (this.selectorControls && this.searchControls) {
      this.searchControlsWrapped = this.statements.length > 0 && this.selectorControls.nativeElement.getBoundingClientRect().top != this.searchControls.nativeElement.getBoundingClientRect().top
    }
  }

  protected determineReportLevel(event: ExportReportEvent) {
    let reportLevel: 'all'|'domain'|'specification'|'group'
    if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN) {
      reportLevel = "domain"
    } else if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP) {
      reportLevel = "group"
    } else if (event.item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION) {
      reportLevel = "specification"
    } else {
      reportLevel = "all"
    }
    return reportLevel
  }

}
