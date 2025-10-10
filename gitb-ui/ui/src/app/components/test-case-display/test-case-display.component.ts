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
import {mergeMap, of, share} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {ConformanceTestCase} from 'src/app/pages/organisation/conformance-statement/conformance-test-case';
import {ReportService} from 'src/app/services/report.service';
import {saveAs} from 'file-saver';
import {sortBy} from 'lodash';
import {ConformanceService} from 'src/app/services/conformance.service';
import {HtmlService} from 'src/app/services/html.service';
import {SpecificationReferenceInfo} from 'src/app/types/specification-reference-info';
import {ConformanceTestCaseGroup} from '../../pages/organisation/conformance-statement/conformance-test-case-group';
import {BaseComponent} from '../../pages/base-component.component';
import {DataService} from '../../services/data.service';
import {CloseEvent} from '../test-result-status-display/close-event';
import {TestCaseDisplayComponentApi} from './test-case-display-component-api';
import {TestResultStatusDisplayComponentApi} from '../test-result-status-display/test-result-status-display-component-api';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';

@Component({
    selector: 'app-test-case-display',
    templateUrl: './test-case-display.component.html',
    styleUrls: ['./test-case-display.component.less'],
    standalone: false
})
export class TestCaseDisplayComponent extends BaseComponent implements TestCaseDisplayComponentApi, OnInit {

  @Input() testCases!: ConformanceTestCase[]
  @Input() testCaseGroups?: Map<number, ConformanceTestCaseGroup>
  @Input() testSuiteSpecificationReference?: SpecificationReferenceInfo
  @Input() hasOptional? = false
  @Input() hasDisabled? = false
  @Input() showExecute? = true
  @Input() showExport? = false
  @Input() showViewDocumentation? = true
  @Input() showResults? = true
  @Input() showEdit? = false
  @Input() shaded = true
  @Input() communityId?: number

  @Output() viewTestSession = new EventEmitter<string>()
  @Output() execute = new EventEmitter<ConformanceTestCase>()
  @Output() edit = new EventEmitter<ConformanceTestCase>()
  @Output() optionsOpened = new EventEmitter<ConformanceTestCase>()

  @ViewChildren("testResultStatusDisplayComponent") testResultStatusDisplayComponents?: QueryList<TestResultStatusDisplayComponentApi>
  @ViewChildren("optionButton") optionButtons?: QueryList<CheckBoxOptionPanelComponentApi>

  protected static EXPORT_XML = '0'
  protected static EXPORT_PDF = '1'
  protected static VIEW_SESSION = '2'

  Constants = Constants
  exportXmlPending: {[key:number]: boolean } = {}
  exportPdfPending: {[key:number]: boolean } = {}
  viewDocumentationPending: {[key:number]: boolean } = {}

  hasDescriptions = false
  hasDescription: {[key:number]: boolean } = {}
  descriptionVisible: {[key:number]: boolean } = {}
  statusCloseEmitter = new EventEmitter<CloseEvent>()
  animated = true

  constructor(
    private readonly reportService: ReportService,
    private readonly htmlService: HtmlService,
    private readonly conformanceService: ConformanceService,
    private readonly dataService: DataService
  ) { super() }

  ngOnInit(): void {
    this.resetPresentation()
  }

  refresh() {
    this.resetPresentation()
    this.testResultStatusDisplayComponents?.forEach((component) => {
      component.refresh()
    })
  }

  getParsedTags(testCase: ConformanceTestCase) {
    if (testCase.tags != undefined && testCase.parsedTags == undefined) {
      testCase.parsedTags = sortBy(JSON.parse(testCase.tags), ['name'])
    }
    return testCase.parsedTags
  }

  closeOptions(source: ConformanceTestCase) {
    this.optionButtons?.forEach((optionButton) => {
      if (optionButton.getReferenceItem() !== source) {
        optionButton.close()
      }
    })
  }

  loadOptions(testCase: ConformanceTestCase) {
    return () => {
      const options: CheckboxOption[][] = []
      if (testCase.sessionId != undefined) {
        options.push([
          { key: TestCaseDisplayComponent.VIEW_SESSION, label: "View test session", default: true, iconClass: "fa-solid fa-search"}
        ])
      }
      if (this.showExportTestCase(testCase)) {
        options.push([
          { key: TestCaseDisplayComponent.EXPORT_PDF, label: "Download report", default: true, iconClass: "fa-solid fa-file-pdf"},
          { key: TestCaseDisplayComponent.EXPORT_XML, label: "Download report as XML", default: true, iconClass: "fa-solid fa-file-lines"}
        ])
      }
      return of(options)
    }
  }

  handleOption(event: CheckboxOptionState, testCase: ConformanceTestCase) {
    if (event[TestCaseDisplayComponent.EXPORT_PDF]) {
      this.onExportTestCasePdf(testCase)
    } else if (event[TestCaseDisplayComponent.EXPORT_XML]) {
      this.onExportTestCaseXml(testCase)
    } else if (event[TestCaseDisplayComponent.VIEW_SESSION]) {
      this.viewTestCase(testCase)
    }
  }

  optionsOpening(testCase: ConformanceTestCase) {
    this.optionsOpened.emit(testCase)
  }

  private resetPresentation() {
    this.animated = false
    setTimeout(() => {
      for (let testCase of this.testCases) {
        this.hasDescription[testCase.id] = testCase.description != undefined && testCase.description != ''
        if (this.hasDescription[testCase.id] && !this.hasDescriptions) {
          this.hasDescriptions = true
        }
        this.descriptionVisible[testCase.id] = false
      }
      setTimeout(() => {
        this.dataService.prepareTestCaseGroupPresentation(this.testCases, this.testCaseGroups)
        this.animated = true
      })
    })
  }

  viewTestCase(testCase: ConformanceTestCase) {
    this.viewTestSession.emit(testCase.sessionId!)
  }

  showTestCaseDocumentation(testCase: ConformanceTestCase) {
    this.viewDocumentationPending[testCase.id] = true
    this.dataService.setImplicitCommunity(this.communityId)
    this.conformanceService.getTestCaseDocumentation(testCase.id)
    .subscribe((data) => {
      this.htmlService.showHtml("Test case documentation", data)
    }).add(() => {
      this.viewDocumentationPending[testCase.id] = false
    })
  }

  onTestSelect(testCase: ConformanceTestCase) {
    this.execute.emit(testCase)
  }

  showExportTestCase(testCase: ConformanceTestCase) {
    return this.showExport && testCase.sessionId != undefined && testCase.sessionId != ""
  }

	onExportTestCaseXml(testCase: ConformanceTestCase) {
    this.exportXmlPending[testCase.id] = true
    this.onExportTestCase(testCase, 'application/xml', 'test_case_report.xml')
    .subscribe(() => {
      this.exportXmlPending[testCase.id] = false
    })
  }

	onExportTestCasePdf(testCase: ConformanceTestCase) {
    this.exportPdfPending[testCase.id] = true
    this.onExportTestCase(testCase, 'application/pdf', 'test_case_report.pdf')
    .subscribe(() => {
      this.exportPdfPending[testCase.id] = false
    })
  }

	onExportTestCase(testCase: Partial<ConformanceTestCase>, contentType: string, fileName: string) {
    return this.reportService.exportTestCaseReport(testCase.sessionId!, testCase.id!, contentType)
    .pipe(
      mergeMap((data) => {
        const blobData = new Blob([data], {type: contentType});
        saveAs(blobData, fileName);
        return of(data)
      }),
      share()
    )
  }

  groupTooltip(resultToShow: string | undefined): string {
    let tooltip
    if (resultToShow == Constants.TEST_CASE_RESULT.SUCCESS) {
      tooltip = 'Group success (at least one required test is successful)'
    } else if (resultToShow == Constants.TEST_CASE_RESULT.FAILURE) {
      tooltip = 'Group failure (no successful required tests and at least one failure)'
    } else if (resultToShow == Constants.TEST_CASE_RESULT.WARNING) {
      tooltip = 'Group warning (no successful required tests and at least one warning)'
    } else if (resultToShow == Constants.TEST_CASE_RESULT.UNDEFINED) {
      tooltip = 'Group incomplete (all required tests are incomplete)'
    } else {
      tooltip = 'Group ignored (all tests are ignored)'
    }
    return tooltip;
  }

  editTestcase(testCase: ConformanceTestCase) {
    if (this.showEdit) {
      this.edit.emit(testCase)
    }
  }

  testCaseClicked(testCaseId: number) {
    this.descriptionVisible[testCaseId] = !this.descriptionVisible[testCaseId]
    // Make sure that any status display messages are closed
    this.statusCloseEmitter.emit({})
  }

  statusPopupOpened(openedTestCaseId: number) {
    this.statusCloseEmitter.emit({idToSkip: openedTestCaseId})
  }
}
