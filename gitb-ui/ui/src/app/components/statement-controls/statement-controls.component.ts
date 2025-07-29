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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {DataService} from 'src/app/services/data.service';
import {ConformanceIds} from 'src/app/types/conformance-ids';
import {TestCaseFilterState} from '../test-case-filter/test-case-filter-state';
import {TestCaseFilterOptions} from '../test-case-filter/test-case-filter-options';
import {StatementFilterState} from './statement-filter-state';
import {NavigationControlsConfig} from '../navigation-controls/navigation-controls-config';

@Component({
    selector: 'app-statement-controls',
    templateUrl: './statement-controls.component.html',
    standalone: false
})
export class StatementControlsComponent implements OnInit {

  @Input() conformanceIds!: ConformanceIds
  @Input() communityId!:number
  @Input() organisationId!: number
  @Input() snapshotId!:number|undefined
  @Input() snapshotLabel!:string|undefined
  @Input() hasBadge!: boolean|undefined
  @Input() showCollapseAll = false
  @Output() filter = new EventEmitter<StatementFilterState>()
  @Output() collapseAll = new EventEmitter<void>()

  filtersActive = false
  testCaseFilter?: string
  testSuiteFilter?: string
  defaultTestCaseFilterState: TestCaseFilterState = {
    showSuccessful: true,
    showFailed: true,
    showIncomplete: true,
    showOptional: true,
    showDisabled: true
  }
  testCaseFilterState = this.defaultTestCaseFilterState
  testCaseFilterOptions: TestCaseFilterOptions = {
    showOptional: true,
    showDisabled: true,
    initialState: this.testCaseFilterState
  }
  navigationConfig!: NavigationControlsConfig

  constructor(
    public readonly dataService: DataService
  ) {}

  ngOnInit(): void {
    this.navigationConfig = {
      systemId: this.conformanceIds.systemId,
      organisationId: this.organisationId,
      communityId: this.communityId,
      actorId: this.conformanceIds.actorId,
      specificationId: this.conformanceIds.specificationId,
      domainId: this.conformanceIds.domainId
    }
  }

  applySearchFilters() {
    this.filter.emit({
      showSuccessful: this.testCaseFilterState.showSuccessful,
      showFailed: this.testCaseFilterState.showFailed,
      showIncomplete: this.testCaseFilterState.showIncomplete,
      showOptional: this.testCaseFilterState.showOptional,
      showDisabled: this.testCaseFilterState.showDisabled,
      testCaseFilter: this.testCaseFilter,
      testSuiteFilter: this.testSuiteFilter
    })
  }

  resultFilterUpdated(state: TestCaseFilterState) {
    this.testCaseFilterState = state
    this.applySearchFilters()
  }

  handleCollapseAll() {
    this.collapseAll.emit()
  }

}
