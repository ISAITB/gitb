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
    for (let testCase of this.testCases) {
      if (testCase.tags != undefined && testCase.parsedTags == undefined) {
        testCase.parsedTags = sortBy(JSON.parse(testCase.tags), ['name'])
      }
      this.hasDescription[testCase.id] = testCase.description != undefined && testCase.description != ''
      if (this.hasDescription[testCase.id] && !this.hasDescriptions) {
        this.hasDescriptions = true
      }
      this.descriptionVisible[testCase.id] = false
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
