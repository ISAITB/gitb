import {Component, EventEmitter, HostListener, Input, OnInit, Output} from '@angular/core';
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

@Component({
    selector: 'app-test-case-display',
    templateUrl: './test-case-display.component.html',
    styleUrls: ['./test-case-display.component.less'],
    standalone: false
})
export class TestCaseDisplayComponent extends BaseComponent implements OnInit {

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
  @Input() refresh?: EventEmitter<void>

  @Output() viewTestSession = new EventEmitter<string>()
  @Output() execute = new EventEmitter<ConformanceTestCase>()
  @Output() edit = new EventEmitter<ConformanceTestCase>()

  Constants = Constants
  exportXmlPending: {[key:number]: boolean } = {}
  exportPdfPending: {[key:number]: boolean } = {}
  viewDocumentationPending: {[key:number]: boolean } = {}

  hasDescriptions = false
  hasDescription: {[key:number]: boolean } = {}
  descriptionVisible: {[key:number]: boolean } = {}
  statusCloseEmitter = new EventEmitter<CloseEvent>()

  constructor(
    private reportService: ReportService,
    private htmlService: HtmlService,
    private conformanceService: ConformanceService,
    private dataService: DataService
  ) { super() }

  ngOnInit(): void {
    if (this.refresh) {
      this.refresh.subscribe(() => {
        this.resetPresentation()
      })
    } else {
      this.resetPresentation()
      this.dataService.prepareTestCaseGroupPresentation(this.testCases, this.testCaseGroups)
    }
  }

  private resetPresentation() {
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
