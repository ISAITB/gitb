import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { mergeMap, Observable, share, map, of } from 'rxjs';
import { ActorInfo } from 'src/app/components/diagram/actor-info';
import { DiagramEvents } from 'src/app/components/diagram/diagram-events';
import { StepData } from 'src/app/components/diagram/step-data';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { TestSuiteService } from 'src/app/services/test-suite.service';
import { TestService } from 'src/app/services/test.service';
import { TestCase } from 'src/app/types/test-case';

@Component({
  selector: 'app-test-case-details',
  templateUrl: './test-case-details.component.html',
  styleUrls: [ './test-case-details.component.less' ]
})
export class TestCaseDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  testCase: Partial<TestCase> = {}
  domainId!: number
  specificationId?:number
  testSuiteId!:number
  testCaseId!:number
  showDocumentation = false
  pending = false
  diagramLoaded = false
  steps: {[key: string]: StepData[]} = {}
  actorInfo: {[key: string]: ActorInfo[]} = {}
  testEvents: {[key: number]: DiagramEvents} = {}

  constructor(
    private dataService: DataService,
    private testSuiteService: TestSuiteService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private conformanceService: ConformanceService,
    private testService: TestService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    const specificationIdParam = this.route.snapshot.paramMap.get('spec_id')
    if (specificationIdParam) {
      this.specificationId = Number(specificationIdParam)
    }
    this.testSuiteId = Number(this.route.snapshot.paramMap.get('testsuite_id'))
    this.testCaseId = Number(this.route.snapshot.paramMap.get('testcase_id'))
		this.testSuiteService.getTestCase(this.testCaseId)
    .subscribe((data) => {
			this.testCase = data
    })
    this.testEvents[this.testCaseId] = new DiagramEvents()    
    this.getTestCaseDefinition(this.testCaseId).subscribe(() => {}).add(() => {
      this.diagramLoaded = true
    })
  }

	previewDocumentation() {
		this.conformanceService.getDocumentationForPreview(this.testCase.documentation!)
    .subscribe((html) => {
      this.htmlService.showHtml('Test case documentation', html)
    })
  }

  copyDocumentation() {
    this.dataService.copyToClipboard(this.testCase.documentation!).subscribe(() => {
      this.popupService.success('HTML source copied to clipboard.')
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
    if (this.specificationId != undefined) {
      this.routingService.toTestSuite(this.domainId, this.specificationId, this.testSuiteId)
    } else {
      // Shared test suite.
      this.routingService.toSharedTestSuite(this.domainId, this.testSuiteId)
    }
  }

	saveDisabled() {
		return !this.textProvided(this.testCase?.sname)
  }

  getTestCaseDefinition(testCaseToLookup: number): Observable<void> {
    return this.testService.getTestCaseDefinition(testCaseToLookup).pipe(
      mergeMap((testCase) => {
        return this.testService.prepareTestCaseDisplayActors(testCase, this.specificationId).pipe(
          map((actorData) => {
            this.actorInfo[testCaseToLookup] = actorData
            this.steps[testCaseToLookup] = testCase.steps
            this.testEvents[testCaseToLookup].signalTestLoad({ testId: testCaseToLookup })
          })
        )
      }), share()
    )
  }

}
