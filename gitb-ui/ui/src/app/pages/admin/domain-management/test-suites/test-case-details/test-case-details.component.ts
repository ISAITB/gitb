import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { mergeMap, Observable, share, map } from 'rxjs';
import { find, remove, sortBy } from 'lodash';
import { ActorInfo } from 'src/app/components/diagram/actor-info';
import { DiagramEvents } from 'src/app/components/diagram/diagram-events';
import { StepData } from 'src/app/components/diagram/step-data';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { RoutingService } from 'src/app/services/routing.service';
import { TestSuiteService } from 'src/app/services/test-suite.service';
import { TestService } from 'src/app/services/test.service';
import { TestCase } from 'src/app/types/test-case';
import { saveAs } from 'file-saver'
import { Constants } from 'src/app/common/constants';
import { BsModalService } from 'ngx-bootstrap/modal';
import { CreateEditTagComponent } from 'src/app/modals/create-edit-tag/create-edit-tag.component';
import { TestCaseTag } from 'src/app/types/test-case-tag';

@Component({
  selector: 'app-test-case-details',
  templateUrl: './test-case-details.component.html',
  styleUrls: [ './test-case-details.component.less' ]
})
export class TestCaseDetailsComponent extends BaseComponent implements OnInit {

  testCase: Partial<TestCase> = {}
  domainId!: number
  specificationId?:number
  testSuiteId!:number
  testCaseId!:number
  loaded = false
  pending = false
  diagramLoaded = false
  steps: {[key: string]: StepData[]} = {}
  actorInfo: {[key: string]: ActorInfo[]} = {}
  testEvents: {[key: number]: DiagramEvents} = {}
  previewPending = false
  private tagCounter = 0
  communityId?: number

  constructor(
    public dataService: DataService,
    private testSuiteService: TestSuiteService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private conformanceService: ConformanceService,
    private testService: TestService,
    private reportService: ReportService,
    private modalService: BsModalService
  ) { super() }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    const specificationIdParam = this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID)
    if (specificationIdParam) {
      this.specificationId = Number(specificationIdParam)
    }
    this.testSuiteId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID))
    this.testCaseId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.TEST_CASE_ID))
    if (this.dataService.isCommunityAdmin) {
      this.communityId = this.dataService.vendor?.community
    } else {
      this.communityId = this.route.snapshot.data[Constants.NAVIGATION_DATA.IMPLICIT_COMMUNITY_ID] as number|undefined
    }
		this.testSuiteService.getTestCase(this.testCaseId)
    .subscribe((data) => {
			this.testCase = data
      if (data.tags) {
        this.testCase.parsedTags = sortBy(JSON.parse(data.tags), ['name'])
        for (let tag of this.testCase.parsedTags!) {
          tag.id = this.tagCounter++
        }
      }
      if (this.specificationId) {
        this.routingService.testCaseBreadcrumbs(this.domainId, this.specificationId, this.testSuiteId, this.testCaseId, this.testCase.identifier!)
      } else {
        this.routingService.sharedTestCaseBreadcrumbs(this.domainId, this.testSuiteId, this.testCaseId, this.testCase.identifier!)
      }
    }).add(() => {
      this.loaded = true
    })
    this.testEvents[this.testCaseId] = new DiagramEvents()
    this.getTestCaseDefinition(this.testCaseId).subscribe(() => {}).add(() => {
      this.diagramLoaded = true
    })
  }

	previewDocumentationPopup() {
    this.previewPending = true
		this.conformanceService.getDocumentationForPreview(this.testCase.documentation!)
    .subscribe((html) => {
      this.htmlService.showHtml('Test case documentation', html)
    }).add(() => {
      this.previewPending = false
    })
  }

	previewDocumentationPdf() {
    this.previewPending = true
		this.reportService.exportTestCaseDocumentationPreviewReport(this.testCase.documentation!)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "report_preview.pdf");
    }).add(() => {
      this.previewPending = false
    })
  }

  copyDocumentation() {
    this.dataService.copyToClipboard(this.testCase.documentation!).subscribe(() => {
      this.popupService.success('HTML source copied to clipboard.')
    })
  }

  private serialiseTags() {
    if (this.testCase.parsedTags && this.testCase.parsedTags.length > 0) {
      return JSON.stringify(this.testCase.parsedTags)
    } else {
      return undefined
    }
  }

	saveChanges() {
    this.pending = true
		this.testSuiteService.updateTestCaseMetadata(this.testCase.id!, this.testCase.sname!, this.testCase.description, this.testCase.documentation, this.testCase.optional, this.testCase.disabled, this.serialiseTags(), this.testCase.specReference, this.testCase.specDescription, this.testCase.specLink)
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
		return !this.loaded || !this.textProvided(this.testCase?.sname)
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

  private openTagModal(tag?: TestCaseTag) {
    const modal = this.modalService.show(CreateEditTagComponent, {
      class: 'modal-lg',
      initialState: {
        tag: tag
      }
    })
    modal.content!.createdTag.subscribe((createdTag) => {
      if (this.testCase.parsedTags == undefined) {
        this.testCase.parsedTags = []
      }
      createdTag.id = this.tagCounter++
      this.testCase.parsedTags.push(createdTag)
      this.testCase.parsedTags = sortBy(this.testCase.parsedTags, ['name'])
    })
    modal.content!.updatedTag.subscribe((updatedTag) => {
      if (this.testCase.parsedTags) {
        remove(this.testCase.parsedTags, (tag) => tag.id == updatedTag.id)
        this.testCase.parsedTags.push(updatedTag)
        this.testCase.parsedTags = sortBy(this.testCase.parsedTags, ['name'])
      }
    })
  }

  tagEdited(tagId: number) {
    if (this.testCase.parsedTags) {
      const selectedTag = find(this.testCase.parsedTags, (tag) => tag.id == tagId)
      if (selectedTag) {
        this.openTagModal(selectedTag)
      }
    }
  }

  tagDeleted(tagId: number) {
    if (this.testCase.parsedTags) {
      remove(this.testCase.parsedTags, (tag) => tag.id == tagId)
    }
  }

  createTag() {
    this.openTagModal()
  }
}
