import {Component, EventEmitter, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {HtmlService} from 'src/app/services/html.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {TestSuiteService} from 'src/app/services/test-suite.service';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {TestCase} from 'src/app/types/test-case';
import {TestSuiteWithTestCases} from 'src/app/types/test-suite-with-test-cases';
import {saveAs} from 'file-saver';
import {Specification} from 'src/app/types/specification';
import {BsModalService} from 'ngx-bootstrap/modal';
import {LinkSharedTestSuiteModalComponent} from 'src/app/modals/link-shared-test-suite-modal/link-shared-test-suite-modal.component';
import {filter, find} from 'lodash';
import {forkJoin} from 'rxjs';
import {ConformanceTestCase} from '../../../../organisation/conformance-statement/conformance-test-case';
import {ConformanceTestCaseGroup} from '../../../../organisation/conformance-statement/conformance-test-case-group';
import {FilterUpdate} from '../../../../../components/test-filter/filter-update';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';

@Component({
    selector: 'app-test-suite-details',
    templateUrl: './test-suite-details.component.html',
    styleUrls: ['./test-suite-details.component.less'],
    standalone: false
})
export class TestSuiteDetailsComponent extends BaseComponent implements OnInit {

  testSuite: Partial<TestSuiteWithTestCases> = {}
  domainId!: number
  specificationId?: number
  testSuiteId!: number
  dataStatus = {status: Constants.STATUS.NONE}
  specificationStatus = {status: Constants.STATUS.NONE}
  specificationTableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Specification' },
    { field: 'description', title: 'Description' }
  ]
  loaded = false
  savePending = false
  deletePending = false
  downloadPending = false
  selectingForUnlink = false
  unlinkPending = false
  linkPending = false
  linkedSpecifications: Specification[] = []
  unlinkedSpecifications: Specification[] = []
  clearLinkedSpecificationsSelection = new EventEmitter<void>()

  testCasesToShow?: ConformanceTestCase[]
  hasDisabledTestCases = false
  hasOptionalTestCases = false
  testCaseGroupMap?: Map<number, ConformanceTestCaseGroup>
  communityId?: number

  movePending = false
  moveSelectionConfig!: MultiSelectConfig<Specification>

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private testSuiteService: TestSuiteService,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private conformanceService: ConformanceService,
    private confirmationDialogService: ConfirmationDialogService,
    private modalService: BsModalService
  ) { super() }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    const specIdParameter = this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID)
    if (specIdParameter) {
      this.specificationId = Number(specIdParameter)
    }
    if (this.dataService.isCommunityAdmin) {
      this.communityId = this.dataService.vendor?.community
    } else {
      this.communityId = this.route.snapshot.data[Constants.NAVIGATION_DATA.IMPLICIT_COMMUNITY_ID] as number|undefined
    }
    this.testSuiteId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID))
    this.moveSelectionConfig = {
      name: "specification",
      textField: "sname",
      singleSelection: true,
      supportPending: true,
      filterLabel: 'Move to ' + this.dataService.labelSpecificationLower(),
      noItemsMessage: 'No target ' + this.dataService.labelSpecificationsLower() + ' available.',
      searchPlaceholder: 'Search ' + this.dataService.labelSpecificationsLower() + "...",
      loader: () => this.loadAvailableSpecificationsForMove()
    }
    this.loadTestCases()
  }

  loadTestCases() {
    if (this.dataStatus.status == Constants.STATUS.NONE) {
      this.testSuiteService.getTestSuiteWithTestCases(this.testSuiteId)
      .subscribe((data) => {
        this.testSuite = data
        this.testCaseGroupMap = this.dataService.toTestCaseGroupMap(data.testCaseGroups)
        this.testCasesToShow = this.toConformanceTestCases(data.testCases)
        if (this.specificationId) {
          this.routingService.testSuiteBreadcrumbs(this.domainId, this.specificationId, this.testSuiteId, this.testSuite.identifier!)
        } else {
          this.routingService.sharedTestSuiteBreadcrumbs(this.domainId, this.testSuiteId, this.testSuite.identifier!)
        }
      }).add(() => {
        this.dataStatus.status = Constants.STATUS.FINISHED
        this.loaded = true
      })
    }
  }

  loadLinkedSpecifications(forceLoad?: boolean) {
    if (this.specificationStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.linkedSpecifications = []
      this.unlinkedSpecifications = []
      const loadLinked = this.testSuiteService.getLinkedSpecifications(this.testSuiteId)
      const loadUnlinked = this.conformanceService.getDomainSpecifications(this.domainId)
      forkJoin([loadLinked, loadUnlinked]).subscribe((results) => {
        this.linkedSpecifications = results[0]
        const currentIds = this.dataService.asSet(this.linkedSpecifications.map((x) => x.id))
        const specs: Specification[] = []
        for (let spec of results[1]) {
          if (!currentIds[spec.id]) {
            specs.push(spec)
          }
        }
        this.unlinkedSpecifications = specs
      }).add(() => {
        this.specificationStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

	previewDocumentation() {
		this.conformanceService.getDocumentationForPreview(this.testSuite.documentation!)
    .subscribe((html) => {
      this.htmlService.showHtml('Test suite documentation', html)
    })
  }

  copyDocumentation() {
    this.dataService.copyToClipboard(this.testSuite.documentation!).subscribe(() => {
      this.popupService.success('HTML source copied to clipboard.')
    })
  }

	download() {
    this.clearAlerts()
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
    this.clearAlerts()
    let message: string
    if (this.testSuite.shared) {
      message = "Deleting this test suite will remove it from all linked " + this.dataService.labelSpecificationsLower() + ". Are you sure you want to proceed?"
    } else {
      message = "Are you sure you want to delete this test suite?"
    }
		this.confirmationDialogService.confirmedDangerous("Confirm delete", message, "Delete", "Cancel")
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
    this.clearAlerts()
    this.savePending = true
		this.testSuiteService.updateTestSuiteMetadata(this.testSuite.id!, this.testSuite.sname!, this.testSuite.description, this.testSuite.documentation, this.testSuite.version!, this.testSuite.specReference, this.testSuite.specDescription, this.testSuite.specLink)
    .subscribe(() => {
      this.popupService.success('Test suite updated.')
    }).add(() => {
      this.savePending = false
    })
  }

	back() {
    if (this.specificationId) {
      this.routingService.toSpecification(this.domainId, this.specificationId!, Constants.TAB.SPECIFICATION.TEST_SUITES)
    } else {
      this.routingService.toDomain(this.domainId, Constants.TAB.DOMAIN.TEST_SUITES)
    }
  }

	saveDisabled() {
    return !this.textProvided(this.testSuite?.sname) || !this.textProvided(this.testSuite?.version)
  }

	onTestCaseSelect(testCaseId: number) {
    if (this.specificationId) {
      this.routingService.toTestCase(this.domainId, this.specificationId!, this.testSuiteId, testCaseId)
    } else {
      this.routingService.toSharedTestCase(this.domainId, this.testSuiteId, testCaseId)
    }
  }

  onSpecificationSelect(specification: Specification) {
    this.routingService.toSpecification(this.domainId, specification.id)
  }

  linkSpecifications() {
    this.linkPending = true
    const modalRef = this.modalService.show(LinkSharedTestSuiteModalComponent, {
      class: 'modal-lg',
      keyboard: false,
      backdrop: 'static',
      initialState: {
        testSuiteId: this.testSuite.id,
        domainId: this.domainId,
        availableSpecifications: this.unlinkedSpecifications
      }
    })
    modalRef.onHidden!.subscribe(() => {
      this.linkPending = false
    })
    modalRef.content!.completed.subscribe((refreshNeeded: boolean) => {
      if (refreshNeeded) {
        this.loadLinkedSpecifications(true)
      }
    })
  }

  checkedSpecifications() {
    return filter(this.linkedSpecifications, (spec) => {
      return spec.checked != undefined && spec.checked
    })
  }

  specificationsChecked() {
    return find(this.linkedSpecifications, (spec) => spec.checked != undefined && spec.checked) != undefined
  }

  selectUnlinkSpecifications() {
    this.selectingForUnlink = true
  }

  confirmUnlinkSpecifications() {
    this.unlinkPending = true
    const checkedSpecifications = this.checkedSpecifications()
    this.conformanceService.unlinkSharedTestSuite(this.testSuiteId, checkedSpecifications.map((x) => x.id)).subscribe(() => {
      if (checkedSpecifications.length == 1) {
        this.popupService.success('Test suite unlinked from '+this.dataService.labelSpecificationLower()+'.')
      } else {
        this.popupService.success('Test suite unlinked from '+this.dataService.labelSpecificationsLower()+'.')
      }
    }).add(() => {
      this.unlinkPending = false
      this.cancelUnlinkSpecifications()
      this.loadLinkedSpecifications(true)
    })
  }

  cancelUnlinkSpecifications() {
    for (let spec of this.linkedSpecifications) {
      spec.checked = false
    }
    this.clearLinkedSpecificationsSelection.emit()
    this.selectingForUnlink = false
  }

  private toConformanceTestCases(testCases: TestCase[]) {
    const testCaseToReturn: ConformanceTestCase[] = []
    testCases.forEach(testCase => {
      if (testCase.optional) this.hasOptionalTestCases = true
      if (testCase.disabled) this.hasDisabledTestCases = true
      testCaseToReturn.push({
        optional: testCase.optional,
        disabled: testCase.disabled,
        result: Constants.TEST_CASE_RESULT.UNDEFINED,
        resultToShow: Constants.TEST_CASE_RESULT.UNDEFINED,
        tags: testCase.tags,
        sname: testCase.sname,
        description: testCase.description,
        id: testCase.id,
        hasDocumentation: testCase.hasDocumentation == true,
        group: testCase.group,
        specReference: testCase.specReference,
        specDescription: testCase.specDescription,
        specLink: testCase.specLink
      })
    })
    return testCaseToReturn;
  }

  loadAvailableSpecificationsForMove() {
    return this.testSuiteService.getAvailableSpecificationsForMove(this.testSuiteId)
  }

  moveSelectionChanged(event: FilterUpdate<Specification>) {
    this.clearAlerts()
    const specificationId = event.values.active[0].id
    this.movePending = true
    this.testSuiteService.moveTestSuiteToSpecification(this.testSuiteId, specificationId).subscribe((result) => {
      if (this.isErrorDescription(result)) {
        this.addAlertError(result.error_description)
      } else {
        this.popupService.success('Test suite moved successfully.')
        this.routingService.toTestSuite(this.domainId, specificationId, this.testSuiteId).then(() => {
          // Reset breadcrumbs and loaded IDs
          this.specificationId = specificationId
          this.routingService.testSuiteBreadcrumbs(this.domainId, this.specificationId, this.testSuiteId, this.testSuite.identifier!)
        })
      }
    }).add(() => {
      this.movePending = false
    })
  }
}
