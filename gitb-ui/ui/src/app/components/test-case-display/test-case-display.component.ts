import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { mergeMap, of, share } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ConformanceTestCase } from 'src/app/pages/organisation/conformance-statement/conformance-test-case';
import { ReportService } from 'src/app/services/report.service';
import { saveAs } from 'file-saver'
import { TestCaseTag } from 'src/app/types/test-case-tag';
import { sortBy } from 'lodash';
import { ConformanceService } from 'src/app/services/conformance.service';
import { HtmlService } from 'src/app/services/html.service';
import { SpecificationReferenceInfo } from 'src/app/types/specification-reference-info';

@Component({
  selector: 'app-test-case-display',
  templateUrl: './test-case-display.component.html',
  styleUrls: [ './test-case-display.component.less' ]
})
export class TestCaseDisplayComponent implements OnInit {

  @Input() testCases!: ConformanceTestCase[]
  @Input() testSuiteSpecificationReference?: SpecificationReferenceInfo
  @Input() hasOptional? = false
  @Input() hasDisabled? = false
  @Input() showExecute? = true
  @Input() showExport? = false
  @Input() showViewDocumentation? = true
  @Input() shaded = true

  @Output() viewTestSession = new EventEmitter<string>()
  @Output() execute = new EventEmitter<ConformanceTestCase>()

  Constants = Constants
  exportXmlPending: {[key:number]: boolean } = {}
  exportPdfPending: {[key:number]: boolean } = {}
  viewDocumentationPending: {[key:number]: boolean } = {}

  hasDescriptions = false
  hasDescription: {[key:number]: boolean } = {}
  descriptionVisible: {[key:number]: boolean } = {}

  tagsToDisplay!: TestCaseTag[]

  constructor(
    private reportService: ReportService,
    private htmlService: HtmlService,
    private conformanceService: ConformanceService
  ) { }

  ngOnInit(): void {
    let testCasesInGroup: ConformanceTestCase[] = []
    for (let testCase of this.testCases) {
      if (testCase.tags != undefined && testCase.parsedTags == undefined) {
        testCase.parsedTags = sortBy(JSON.parse(testCase.tags), ['name'])
      }
      this.hasDescription[testCase.id] = testCase.description != undefined && testCase.description != ''
      if (this.hasDescription[testCase.id] && !this.hasDescriptions) {
        this.hasDescriptions = true
      }
      this.descriptionVisible[testCase.id] = false
      this.manageGroup(testCasesInGroup, testCase)
    }
    this.closeGroup(testCasesInGroup)
    if (this.testCases[this.testCases.length-1].group != undefined) {
      this.testCases[this.testCases.length-1].groupLast = true
    }
  }

  private manageGroup(currentGroup: ConformanceTestCase[], testCase: ConformanceTestCase) {
    if (testCase.group == undefined) {
      testCase.resultToShow = testCase.result
      if (currentGroup.length > 0) {
        this.closeGroup(currentGroup)
      }
    } else {
      if (currentGroup.length > 0 && currentGroup[0].group != testCase.group) {
        this.closeGroup(currentGroup)
      }
      currentGroup.push(testCase)
    }
  }

  private closeGroup(currentGroup: ConformanceTestCase[]) {
    if (currentGroup.length > 0) {
      currentGroup[0].groupFirst = true
      currentGroup[currentGroup.length - 1].groupLast = true
      let successCount = 0
      let warningCount = 0
      let failureCount = 0
      let incompleteCount = 0
      currentGroup.forEach((group) => {
        switch (group.result) {
          case Constants.TEST_CASE_RESULT.SUCCESS: successCount++; break
          case Constants.TEST_CASE_RESULT.WARNING: warningCount++; break
          case Constants.TEST_CASE_RESULT.FAILURE: failureCount++; break
          default: incompleteCount++; break
        }
      })
      let groupStatus = Constants.TEST_CASE_RESULT.UNDEFINED
      if (successCount > 0) {
        groupStatus = Constants.TEST_CASE_RESULT.SUCCESS
      } else if (warningCount > 0) {
        groupStatus = Constants.TEST_CASE_RESULT.WARNING
      } else if (failureCount > 0) {
        groupStatus = Constants.TEST_CASE_RESULT.FAILURE
      }
      currentGroup.forEach((group) => {
        group.resultToShow = groupStatus
      })
      currentGroup.splice(0, currentGroup.length)
    }
  }

  viewTestCase(testCase: ConformanceTestCase) {
    this.viewTestSession.emit(testCase.sessionId!)
  }

  showTestCaseDocumentation(testCase: ConformanceTestCase) {
    this.viewDocumentationPending[testCase.id] = true
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

}
