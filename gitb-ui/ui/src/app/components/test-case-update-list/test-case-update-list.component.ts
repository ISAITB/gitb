import { Component, Input, OnInit } from '@angular/core';
import { TestSuiteUploadTestCaseChoice } from 'src/app/modals/test-suite-upload-modal/test-suite-upload-test-case-choice';

@Component({
  selector: 'app-test-case-update-list',
  templateUrl: './test-case-update-list.component.html',
  styleUrls: [ './test-case-update-list.component.less' ]
})
export class TestCaseUpdateListComponent implements OnInit {

  @Input() title!: string
  @Input() titleTooltip?: string
  @Input() testCases: TestSuiteUploadTestCaseChoice[] = []
  @Input() showUpdateDefinition = false
  @Input() showResetTestHistory = false
  @Input() show = false

  constructor() { }

  ngOnInit(): void {
  }

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
