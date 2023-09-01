import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { mergeMap, of, share } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ConformanceTestCase } from 'src/app/pages/organisation/conformance-statement/conformance-test-case';
import { ReportService } from 'src/app/services/report.service';
import { saveAs } from 'file-saver'
import { TestCaseTag } from 'src/app/types/test-case-tag';
import { sortBy } from 'lodash';

@Component({
  selector: 'app-test-case-display',
  templateUrl: './test-case-display.component.html',
  styleUrls: [ './test-case-display.component.less' ]
})
export class TestCaseDisplayComponent implements OnInit {

  @Input() testCases!: ConformanceTestCase[]
  @Input() hasOptional? = false
  @Input() hasDisabled? = false
  @Input() showExecute? = true
  @Input() showExport? = false

  @Output() viewTestSession = new EventEmitter<string>()
  @Output() viewDocumentation = new EventEmitter<number>()
  @Output() execute = new EventEmitter<ConformanceTestCase>()

  Constants = Constants
  exportXmlPending: {[key:number]: boolean } = {}
  exportPdfPending: {[key:number]: boolean } = {}

  hasDescriptions = false
  hasDescription: {[key:number]: boolean } = {}
  descriptionVisible: {[key:number]: boolean } = {}

  tagsToDisplay!: TestCaseTag[]

  constructor(
    private reportService: ReportService
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
    this.viewDocumentation.emit(testCase.id)
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
