import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { mergeMap, Observable, share, map } from 'rxjs';
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
  specificationId!:number
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
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
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
    this.routingService.toTestSuite(this.domainId, this.specificationId, this.testSuiteId)
  }

	saveDisabled() {
		return !this.textProvided(this.testCase?.sname)
  }

  getTestCaseDefinition(testCaseToLookup: number): Observable<void> {
    return this.testService.getTestCaseDefinition(testCaseToLookup).pipe(
      mergeMap((testCase) => {
        return this.testService.getActorDefinitions(this.specificationId).pipe(
          map((data) => {
            let tempActors = testCase.actors.actor
            for (let domainActorData of data) {
              for (let testCaseActorData of tempActors) {
                if (testCaseActorData.id == domainActorData.actorId) {
                  if (testCaseActorData.name == undefined) {
                    testCaseActorData.name = domainActorData.name
                  }
                  if (testCaseActorData.displayOrder == undefined && domainActorData.displayOrder != undefined) {
                    testCaseActorData.displayOrder = domainActorData.displayOrder
                  }
                  break
                }
              }
            }
            tempActors = tempActors.sort((a, b) => {
              if (a.displayOrder == undefined && b.displayOrder == undefined) return 0
              else if (a.displayOrder != undefined && b.displayOrder == undefined) return -1
              else if (a.displayOrder == undefined && b.displayOrder != undefined) return 1
              else return Number(a.displayOrder) - Number(b.displayOrder)
            })
            this.actorInfo[testCaseToLookup] = tempActors as ActorInfo[]
            this.steps[testCaseToLookup] = testCase.steps
            this.testEvents[testCaseToLookup].signalTestLoad({ testId: testCaseToLookup })
          })
        )
      }), share()
    )
  }

}
