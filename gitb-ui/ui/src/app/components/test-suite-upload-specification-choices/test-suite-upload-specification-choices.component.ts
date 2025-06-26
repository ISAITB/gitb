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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { filter, find } from 'lodash';
import { SpecificationChoice } from 'src/app/modals/test-suite-upload-modal/specification-choice';
import { DataService } from 'src/app/services/data.service';

@Component({
    selector: 'app-test-suite-upload-specification-choices',
    templateUrl: './test-suite-upload-specification-choices.component.html',
    styleUrls: ['./test-suite-upload-specification-choices.component.less'],
    standalone: false
})
export class TestSuiteUploadSpecificationChoicesComponent implements OnInit {

  @Input() choices: SpecificationChoice[] = []
  @Input() sharedTestSuite = false
  @Output() pendingChoices = new EventEmitter<boolean>()

  choiceMap: {[key: number]: SpecificationChoice} = {}
  hasChoices = false
  hasChoicesToComplete = false
  hasMultipleChoices = false
  hasMultipleChoicesWithOptions = false
  totalCount = 0
  skipCount = 0

  constructor(
    public readonly dataService: DataService
  ) { }

  ngOnInit(): void {
    if (!this.sharedTestSuite) {
      this.skipCount = filter(this.choices, (choice) => choice.sharedTestSuite).length
    }
    this.hasChoicesToComplete = this.choices.length > this.skipCount
    this.hasChoices = this.choices.length > 0
    this.hasMultipleChoices = this.choices.length > 1
    if (this.sharedTestSuite) {
      this.hasMultipleChoicesWithOptions = filter(this.choices, (choice) => !choice.testSuiteExists).length > 1
    } else {
      this.hasMultipleChoicesWithOptions = filter(this.choices, (choice) => !choice.sharedTestSuite).length > 1
    }
    this.pendingChoices.emit(this.hasChoicesToComplete)
  }

  applyChoiceToAll(reference: SpecificationChoice) {
    for (let choice of this.choices!) {
      if (choice.specification != reference.specification) {
        choice.updateActors = reference.updateActors
        if (reference.testSuiteExists && choice.testSuiteExists) {
          choice.updateTestSuite = reference.updateTestSuite
          for (let referenceTestCase of reference.testCasesInArchiveAndDB) {
            const matchingTestCase = find(choice.testCasesInArchiveAndDB, (testCase) => {
              return testCase.identifier == referenceTestCase.identifier
            })
            if (matchingTestCase) {
              matchingTestCase.updateDefinition = referenceTestCase.updateDefinition
              matchingTestCase.resetTestHistory = referenceTestCase.resetTestHistory
            }
          }
        }
      }
    }
  }

  skipUpdate(choice: SpecificationChoice) {
    choice.skipUpdate = true
    this.skipCount = this.skipCount + 1
    this.hasChoicesToComplete = this.choices.length > this.skipCount
    this.pendingChoices.emit(this.hasChoicesToComplete)
  }

  processUpdate(choice: SpecificationChoice) {
    choice.skipUpdate = false
    this.skipCount = this.skipCount - 1
    this.hasChoicesToComplete = this.choices.length > this.skipCount
    this.pendingChoices.emit(this.hasChoicesToComplete)
  }

}
