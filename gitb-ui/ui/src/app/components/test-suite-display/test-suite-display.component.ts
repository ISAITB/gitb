import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ConformanceTestCase } from 'src/app/pages/organisation/conformance-statement/conformance-test-case';
import { ConformanceTestSuite } from 'src/app/pages/organisation/conformance-statement/conformance-test-suite';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';

@Component({
  selector: 'app-test-suite-display',
  templateUrl: './test-suite-display.component.html',
  styleUrls: [ './test-suite-display.component.less' ]
})
export class TestSuiteDisplayComponent implements OnInit {

  @Input() testSuites?: ConformanceTestSuite[] = []
  @Input() showExecute? = true
  @Input() showExport? = false
  @Input() showViewDocumentation? = true
  @Input() shaded = false

  @Output() viewTestSession = new EventEmitter<string>()
  @Output() viewTestCaseDocumentation = new EventEmitter<number>()
  @Output() executeTestCase = new EventEmitter<ConformanceTestCase>()
  @Output() executeTestSuite = new EventEmitter<ConformanceTestSuite>()

  hovering: {[key:number]: boolean } = {}
  viewDocumentationPending: {[key:number]: boolean } = {}
  
  Constants = Constants

  constructor(
    private conformanceService: ConformanceService,
    private htmlService: HtmlService,
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

  propagateExecuteTestSession(testCase: ConformanceTestCase) {
    this.executeTestCase.emit(testCase)
  }

  showTestSuiteDocumentation(testSuite: ConformanceTestSuite) {
    this.viewDocumentationPending[testSuite.id] = true
    this.conformanceService.getTestSuiteDocumentation(testSuite.id)
    .subscribe((data) => {
      this.htmlService.showHtml("Test suite documentation", data)
    }).add(() => {
      this.viewDocumentationPending[testSuite.id] = false
    })
  }

  onTestSuiteSelect(testSuite: ConformanceTestSuite) {
    this.executeTestSuite.emit(testSuite)
  }
}
