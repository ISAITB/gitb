import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ConformanceTestCase } from 'src/app/pages/organisation/conformance-statement/conformance-test-case';
import { ConformanceTestSuite } from 'src/app/pages/organisation/conformance-statement/conformance-test-suite';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-test-suite-display',
  templateUrl: './test-suite-display.component.html',
  styleUrls: [ './test-suite-display.component.less' ]
})
export class TestSuiteDisplayComponent implements OnInit {

  @Input() testSuites?: ConformanceTestSuite[] = []
  @Input() showExecute? = true
  @Input() showExport? = false

  @Output() viewTestSession = new EventEmitter<string>()
  @Output() viewTestCaseDocumentation = new EventEmitter<number>()
  @Output() executeTestCase = new EventEmitter<ConformanceTestCase>()
  @Output() viewTestSuiteDocumentation = new EventEmitter<number>()
  @Output() executeTestSuite = new EventEmitter<ConformanceTestSuite>()

  hovering: {[key:number]: boolean } = {}
  
  Constants = Constants

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

  onExpand(testSuite: ConformanceTestSuite) {
    testSuite.expanded = !testSuite.expanded
  }

  propagateViewTestSession(sessionId: string) {
    this.viewTestSession.emit(sessionId)
  }

  propagateViewTestCaseDocumentation(testCaseId: number) {
    this.viewTestCaseDocumentation.emit(testCaseId)
  }

  propagateExecuteTestSession(testCase: ConformanceTestCase) {
    this.executeTestCase.emit(testCase)
  }

  showTestSuiteDocumentation(testSuite: ConformanceTestSuite) {
    this.viewTestSuiteDocumentation.emit(testSuite.id)
  }

  onTestSuiteSelect(testSuite: ConformanceTestSuite) {
    this.executeTestSuite.emit(testSuite)
  }
}
