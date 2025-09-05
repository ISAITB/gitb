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

import {Component, EventEmitter, Input, OnInit, Output, QueryList, ViewChildren} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {ConformanceTestCase} from 'src/app/pages/organisation/conformance-statement/conformance-test-case';
import {ConformanceTestSuite} from 'src/app/pages/organisation/conformance-statement/conformance-test-suite';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {HtmlService} from 'src/app/services/html.service';
import {TestSuiteDisplayComponentApi} from './test-suite-display-component-api';
import {TestCaseDisplayComponentApi} from '../test-case-display/test-case-display-component-api';

@Component({
    selector: 'app-test-suite-display',
    templateUrl: './test-suite-display.component.html',
    styleUrls: ['./test-suite-display.component.less'],
    standalone: false
})
export class TestSuiteDisplayComponent implements OnInit, TestSuiteDisplayComponentApi {

  @Input() testSuites?: ConformanceTestSuite[] = []
  @Input() showExecute? = true
  @Input() showExport? = false
  @Input() showViewDocumentation? = true
  @Input() shaded = false
  @Input() communityId?: number

  @Output() viewTestSession = new EventEmitter<string>()
  @Output() viewTestCaseDocumentation = new EventEmitter<number>()
  @Output() executeTestCase = new EventEmitter<ConformanceTestCase>()
  @Output() executeTestSuite = new EventEmitter<ConformanceTestSuite>()
  @Output() toggleExpand = new EventEmitter<boolean>()

  @ViewChildren("testCaseDisplayComponent") testCaseDisplayComponents?: QueryList<TestCaseDisplayComponentApi>

  hovering: {[key:number]: boolean } = {}
  viewDocumentationPending: {[key:number]: boolean } = {}
  animated = true

  protected readonly Constants = Constants

  constructor(
    private readonly conformanceService: ConformanceService,
    private readonly htmlService: HtmlService,
    public readonly dataService: DataService
  ) { }

  ngOnInit(): void {
    this.prepareTestCaseGroupMaps()
  }

  refresh() {
    this.animated = false
    setTimeout(() => {
      this.prepareTestCaseGroupMaps()
      this.testCaseDisplayComponents?.forEach((testCaseDisplayComponent) => {
        testCaseDisplayComponent.refresh()
      })
      setTimeout(() => {
        this.animated = true
      })
    })
  }

  private prepareTestCaseGroupMaps(): void {
    if (this.testSuites) {
      for (let testSuite of this.testSuites) {
        if (testSuite.testCaseGroups) {
          if (!testSuite.testCaseGroupMap) {
            testSuite.testCaseGroupMap = this.dataService.toTestCaseGroupMap(testSuite.testCaseGroups)
          }
        }
      }
    }
  }

  onExpand(testSuite: ConformanceTestSuite) {
    testSuite.expanded = !testSuite.expanded
    this.toggleExpand.emit(testSuite.expanded)
  }

  propagateViewTestSession(sessionId: string) {
    this.viewTestSession.emit(sessionId)
  }

  propagateExecuteTestSession(testCase: ConformanceTestCase) {
    this.executeTestCase.emit(testCase)
  }

  showTestSuiteDocumentation(testSuite: ConformanceTestSuite) {
    this.viewDocumentationPending[testSuite.id] = true
    this.dataService.setImplicitCommunity(this.communityId)
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
