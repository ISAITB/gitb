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
import {TestCaseFilterApi} from './test-case-filter-api';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {Constants} from '../../common/constants';
import {DataService} from '../../services/data.service';
import {TestCaseFilterState} from './test-case-filter-state';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {TestCaseFilterOptions} from './test-case-filter-options';

@Component({
  selector: 'app-test-case-filter',
  standalone: false,
  templateUrl: './test-case-filter.component.html'
})
export class TestCaseFilterComponent implements TestCaseFilterApi, OnInit {

  @Input() options?: TestCaseFilterOptions
  @Output() apply = new EventEmitter<TestCaseFilterState>()

  testDisplayOptions!: CheckboxOption[][]
  refreshDisplayOptions = new EventEmitter<CheckboxOption[][]>()

  constructor(private readonly dataService: DataService) {
  }

  ngOnInit(): void {
    this.updateOptions(this.options)
  }

  refreshOptions(options?: TestCaseFilterOptions): void {
    this.updateOptions(options)
  }

  private updateOptions(options?: TestCaseFilterOptions): void {
    const showSuccessfulDefault = options == undefined || options.initialState == undefined || options.initialState.showSuccessful
    const showFailedDefault = options == undefined || options.initialState == undefined || options.initialState.showFailed
    const showIncompleteDefault = options == undefined || options.initialState == undefined || options.initialState.showIncomplete
    this.testDisplayOptions = [[
      {key: Constants.TEST_FILTER.SUCCEEDED, label: 'Succeeded tests', default: showSuccessfulDefault, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)},
      {key: Constants.TEST_FILTER.FAILED, label: 'Failed tests', default: showFailedDefault, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)},
      {key: Constants.TEST_FILTER.INCOMPLETE, label: 'Incomplete tests', default: showIncompleteDefault, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)}
    ]]
    const otherOptions: CheckboxOption[] = []
    if (options?.showOptional) {
      const showOptionalDefault = options.initialState == undefined || options.initialState.showOptional
      otherOptions.push({key: Constants.TEST_FILTER.OPTIONAL, label: 'Optional tests', default: showOptionalDefault})
    }
    if (options?.showDisabled) {
      const showDisabledDefault = options.initialState != undefined && options.initialState.showDisabled
      otherOptions.push({key: Constants.TEST_FILTER.DISABLED, label: 'Disabled tests', default: showDisabledDefault})
    }
    if (otherOptions.length > 1) {
      this.testDisplayOptions.push(otherOptions)
    }
    this.refreshDisplayOptions.emit(this.testDisplayOptions)
  }

  resultFilterUpdated(choices: CheckboxOptionState) {
    this.apply.emit({
      showSuccessful: choices[Constants.TEST_FILTER.SUCCEEDED],
      showFailed: choices[Constants.TEST_FILTER.FAILED],
      showIncomplete: choices[Constants.TEST_FILTER.INCOMPLETE],
      showOptional: choices[Constants.TEST_FILTER.OPTIONAL],
      showDisabled: choices[Constants.TEST_FILTER.DISABLED]
    })
  }

}
