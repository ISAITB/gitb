import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { TestSuiteService } from 'src/app/services/test-suite.service';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { TestCase } from 'src/app/types/test-case';
import { TestSuiteWithTestCases } from 'src/app/types/test-suite-with-test-cases';

@Component({
  selector: 'app-test-suite-details',
  templateUrl: './test-suite-details.component.html',
  styleUrls: [ './test-suite-details.component.less' ]
})
export class TestSuiteDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  testSuite: Partial<TestSuiteWithTestCases> = {}
  domainId!: number
  specificationId!: number
  testSuiteId!: number
  dataStatus = {status: Constants.STATUS.PENDING}
  showDocumentation = false
  testCaseTableColumns: TableColumnDefinition[] = [
    { field: 'identifier', title: 'ID' },
    { field: 'sname', title: 'Name' },
    { field: 'description', title: 'Description'}
  ]
  savePending = false
  deletePending = false
  downloadPending = false

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private testSuiteService: TestSuiteService,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private conformanceService: ConformanceService,
    private confirmationDialogService: ConfirmationDialogService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
    this.testSuiteId = Number(this.route.snapshot.paramMap.get('testsuite_id'))

		this.testSuiteService.getTestSuiteWithTestCases(this.testSuiteId)
    .subscribe((data) => {
			this.testSuite = data
    }).add(() => {
			this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

	previewDocumentation() {
		this.conformanceService.getDocumentationForPreview(this.testSuite.documentation!)
    .subscribe((html) => {
      this.htmlService.showHtml('Test suite documentation', html)
    })
  }

	download() {
    this.downloadPending = true
		this.testSuiteService.downloadTestSuite(this.testSuite.id!)
    .subscribe((data) => {
			const blobData = new Blob([data], {type: 'application/zip'})
			saveAs(blobData, "test_suite.zip")
    }).add(() => {
      this.downloadPending = false
    })
  }

	delete() {
		this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this test suite?", "Yes", "No")
		.subscribe(() => {
      this.deletePending = true
      this.testSuiteService.undeployTestSuite(this.testSuite.id!)
      .subscribe(() => {
        this.back()
        this.popupService.success('Test suite deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

	saveChanges() {
    this.savePending = true
		this.testSuiteService.updateTestSuiteMetadata(this.testSuite.id!, this.testSuite.sname!, this.testSuite.description, this.testSuite.documentation, this.testSuite.version!)
    .subscribe(() => {
      this.popupService.success('Test suite updated.')
    }).add(() => {
      this.savePending = false
    })
  }

	back() {
    this.routingService.toSpecification(this.domainId, this.specificationId, Constants.TAB.SPECIFICATION.TEST_SUITES)
  }

	saveDisabled() {
    return !this.textProvided(this.testSuite?.sname) || !this.textProvided(this.testSuite?.version)
  }

	onTestCaseSelect(testCase: TestCase) {
    this.routingService.toTestCase(this.domainId, this.specificationId, this.testSuiteId, testCase.id)
  }

}
