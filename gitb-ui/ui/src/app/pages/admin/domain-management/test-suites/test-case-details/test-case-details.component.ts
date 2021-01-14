import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';
import { PopupService } from 'src/app/services/popup.service';
import { TestSuiteService } from 'src/app/services/test-suite.service';
import { TestCase } from 'src/app/types/test-case';

@Component({
  selector: 'app-test-case-details',
  templateUrl: './test-case-details.component.html',
  styles: [
  ]
})
export class TestCaseDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  testCase: Partial<TestCase> = {}
  domainId!: number
  specificationId!:number
  testSuiteId!:number
  testCaseId!:number
  showDocumentation = false
  pending = false

  constructor(
    private dataService: DataService,
    private testSuiteService: TestSuiteService,
    private router: Router,
    private route: ActivatedRoute,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private conformanceService: ConformanceService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
    this.testSuiteId = Number(this.route.snapshot.paramMap.get('testsuite_id'))
    this.testCaseId = Number(this.route.snapshot.paramMap.get('testcase_id'))
		this.testSuiteService.getTestCase(this.testCaseId)
    .subscribe((data) => {
			this.testCase = data
    })
  }

	previewDocumentation() {
		this.conformanceService.getDocumentationForPreview(this.testCase.documentation!)
    .subscribe((html) => {
      this.htmlService.showHtml('Test case documentation', html)
    })
  }

	saveChanges() {
    this.pending = true
		this.testSuiteService.updateTestCaseMetadata(this.testCase.id!, this.testCase.sname!, this.testCase.description, this.testCase.documentation)
    .subscribe(() => {
      this.popupService.success('Test case updated.')
    }).add(() => {
      this.pending = false
    })
  }

	back() {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', this.specificationId, 'testsuites', this.testSuiteId])
  }

	saveDisabled() {
		return !this.textProvided(this.testCase?.sname)
  }

}
