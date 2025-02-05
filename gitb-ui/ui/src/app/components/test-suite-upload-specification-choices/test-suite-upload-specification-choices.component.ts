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
    public dataService: DataService
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
