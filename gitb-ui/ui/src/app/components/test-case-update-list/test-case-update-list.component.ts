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

import {Component, Input} from '@angular/core';
import {TestSuiteUploadTestCaseChoice} from 'src/app/modals/test-suite-upload-modal/test-suite-upload-test-case-choice';

@Component({
    selector: 'app-test-case-update-list',
    templateUrl: './test-case-update-list.component.html',
    styleUrls: ['./test-case-update-list.component.less'],
    standalone: false
})
export class TestCaseUpdateListComponent {

  @Input() title!: string
  @Input() titleTooltip?: string
  @Input() testCases: TestSuiteUploadTestCaseChoice[] = []
  @Input() showUpdateDefinition = false
  @Input() showResetTestHistory = false
  @Input() show = false

  constructor() { }

  toggleTestCaseChoices(selected: boolean) {
    for (let testCase of this.testCases) {
      testCase.updateDefinition = selected
      testCase.resetTestHistory = selected
    }
  }

  toggleTestCaseDataChoice(selected: boolean) {
    for (let testCase of this.testCases) {
      testCase.updateDefinition = selected
    }
  }

  toggleTestCaseHistoryChoice(selected: boolean) {
    for (let testCase of this.testCases) {
      testCase.resetTestHistory = selected
    }
  }

  hasAllTestCaseDataChoice(selected: boolean) {
    for (let testCase of this.testCases) {
      if (testCase.updateDefinition != selected) {
        return false
      }
    }
    return true
  }

  hasAllTestCaseHistoryChoice(selected: boolean) {
    for (let testCase of this.testCases) {
      if (testCase.resetTestHistory != selected) {
        return false
      }
    }
    return true
  }

}
