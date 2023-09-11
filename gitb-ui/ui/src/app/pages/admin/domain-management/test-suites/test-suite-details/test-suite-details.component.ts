import { AfterViewInit, Component, EventEmitter, OnInit } from '@angular/core';
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
import { saveAs } from 'file-saver'
import { Specification } from 'src/app/types/specification';
import { BsModalService } from 'ngx-bootstrap/modal';
import { LinkSharedTestSuiteModalComponent } from 'src/app/modals/link-shared-test-suite-modal/link-shared-test-suite-modal.component';
import { filter, find } from 'lodash';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-test-suite-details',
  templateUrl: './test-suite-details.component.html'
})
export class TestSuiteDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  testSuite: Partial<TestSuiteWithTestCases> = {}
  domainId!: number
  specificationId?: number
  testSuiteId!: number
  dataStatus = {status: Constants.STATUS.NONE}
  specificationStatus = {status: Constants.STATUS.NONE}
  showDocumentation = false
  testCaseTableColumns: TableColumnDefinition[] = [
    { field: 'identifier', title: 'ID' },
    { field: 'sname', title: 'Name' },
    { field: 'description', title: 'Description'},
    { field: 'optional', title: 'Optional'},
    { field: 'disabled', title: 'Disabled'}
  ]
  specificationTableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Specification' },
    { field: 'description', title: 'Description' }
  ]  
  savePending = false
  deletePending = false
  downloadPending = false
  selectingForUnlink = false
  unlinkPending = false
  linkPending = false
  linkedSpecifications: Specification[] = []
  unlinkedSpecifications: Specification[] = []
  clearLinkedSpecificationsSelection = new EventEmitter<void>()

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

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    const specIdParameter = this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID)
    if (specIdParameter) {
      this.specificationId = Number(specIdParameter)
    }
    this.testSuiteId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID))
    this.loadTestCases()
  }

  loadTestCases() {
    if (this.dataStatus.status == Constants.STATUS.NONE) {
      this.testSuiteService.getTestSuiteWithTestCases(this.testSuiteId)
      .subscribe((data) => {
        this.testSuite = data
      }).add(() => {
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  loadLinkedSpecifications(forceLoad?: boolean) {
    if (this.specificationStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.linkedSpecifications = []
      this.unlinkedSpecifications = []
      const loadLinked = this.testSuiteService.getLinkedSpecifications(this.testSuiteId)
      const loadUnlinked = this.conformanceService.getSpecifications(this.domainId)
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
    this.savePending = true
		this.testSuiteService.updateTestSuiteMetadata(this.testSuite.id!, this.testSuite.sname!, this.testSuite.description, this.testSuite.documentation, this.testSuite.version!)
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

	onTestCaseSelect(testCase: TestCase) {
    if (this.specificationId) {
      this.routingService.toTestCase(this.domainId, this.specificationId!, this.testSuiteId, testCase.id)
    } else {
      this.routingService.toSharedTestCase(this.domainId, this.testSuiteId, testCase.id)
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

}
