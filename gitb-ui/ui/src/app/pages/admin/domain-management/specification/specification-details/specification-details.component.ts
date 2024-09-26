import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, find } from 'lodash';
import { BsModalService } from 'ngx-bootstrap/modal';
import { forkJoin, map, mergeMap, of, share } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { LinkSharedTestSuiteModalComponent } from 'src/app/modals/link-shared-test-suite-modal/link-shared-test-suite-modal.component';
import { TestSuiteUploadModalComponent } from 'src/app/modals/test-suite-upload-modal/test-suite-upload-modal.component';
import { TestSuiteUploadResult } from 'src/app/modals/test-suite-upload-modal/test-suite-upload-result';
import { BaseTabbedComponent } from 'src/app/pages/base-tabbed-component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { Actor } from 'src/app/types/actor';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { Specification } from 'src/app/types/specification';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { TestSuite } from 'src/app/types/test-suite';

@Component({
  selector: 'app-specification-details',
  templateUrl: './specification-details.component.html'
})
export class SpecificationDetailsComponent extends BaseTabbedComponent implements OnInit, AfterViewInit {

  sharedTestSuiteId?: number
  specification: Partial<Specification> = {}
  actors: Actor[] = []
  testSuites: TestSuite[] = []
  sharedTestSuites: TestSuite[] = []
  availableSharedTestSuites: TestSuite[] = []
  availableSharedTestSuitesLoaded = false
  domainId!: number
  specificationId!: number
  actorStatus = {status: Constants.STATUS.NONE}
  testSuiteStatus = {status: Constants.STATUS.NONE}
  testSuiteTableColumns: TableColumnDefinition[] = [
    { field: 'identifier', title: 'ID' },
    { field: 'sname', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'version', title: 'Version' },
    { field: 'shared', title: 'Shared' }
  ]
  actorTableColumns: TableColumnDefinition[] = [
    { field: 'actorId', title: 'ID' },
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default' },
    { field: 'hidden', title: '', atEnd: true, isHiddenFlag: true, headerClass: 'th-min-centered' }
  ]
  savePending = false
  deletePending = false
  linkPending = false
  unlinkPending = false
  loaded = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private confirmationDialogService: ConfirmationDialogService,
    private specificationService: SpecificationService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    router: Router,
    private popupService: PopupService,
    private modalService: BsModalService
  ) { super(router) }

  loadTab(tabIndex: number): void {
    if (tabIndex == Constants.TAB.SPECIFICATION.TEST_SUITES) {
      this.loadTestSuites()
    } else {
      this.loadActors()
    }
  }

  ngAfterViewInit(): void {
    this.showTab()
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.specificationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID))
    const groupObservable = this.specificationService.getSpecificationGroups(this.domainId)
    .pipe(
      mergeMap((data) => {
        return of(data)
      }),
      share()
    )
		const specObservable = this.conformanceService.getSpecification(this.specificationId)
    .pipe(
      map((data) => {
        this.specification = data
        if (!this.specification.group) {
          // Set to undefined to make sure the "undefined" option in the group select is pre-selected.
          this.specification.group = undefined
        }
        if (this.specification.badges) {
          this.specification.badges.specificationId = this.specificationId
          this.specification.badges.enabled = this.specification.badges.success != undefined && this.specification.badges.success.enabled!
          this.specification.badges.initiallyEnabled = this.specification.badges.enabled
          this.specification.badges.failureBadgeActive = this.specification.badges.failure != undefined && this.specification.badges.failure.enabled!
          this.specification.badges.successBadgeForReportActive = this.specification.badges.successForReport != undefined && this.specification.badges.successForReport.enabled!
          this.specification.badges.otherBadgeForReportActive = this.specification.badges.otherForReport != undefined && this.specification.badges.otherForReport.enabled!
          this.specification.badges.failureBadgeForReportActive = this.specification.badges.failureForReport != undefined && this.specification.badges.failureForReport.enabled!
        }
      }),
      share()
    )
    forkJoin([groupObservable, specObservable]).subscribe((data) => {
      this.specification.groups = data[0]
      this.routingService.specificationBreadcrumbs(this.domainId, this.specificationId, this.breadcrumbLabel())
    }).add(() => {
      this.loaded = true
    })
  }

  private breadcrumbLabel() {
    let label = ''
    if (this.specification.group != undefined) {
      label += find(this.specification.groups, (group) => group.id == this.specification.group)?.sname + " - "
    }
    label += this.specification.sname!
    return label
  }

  loadActors(forceLoad?: boolean) {
    if (this.actorStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.actorStatus.status = Constants.STATUS.PENDING
      this.actors = []
      this.conformanceService.getActorsWithSpecificationId(this.specificationId)
      .subscribe((data) => {
        this.actors = data
      }).add(() => {
        this.actorStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  loadTestSuites(forceLoad?: boolean) {
    if (this.testSuiteStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.testSuiteStatus.status = Constants.STATUS.PENDING
      this.testSuites = []
      this.sharedTestSuites = []
      this.conformanceService.getTestSuites(this.specificationId)
      .subscribe((data) => {
        this.testSuites = data
        this.sharedTestSuites = filter(this.testSuites, (ts) => ts.shared)
      }).add(() => {
        this.availableSharedTestSuitesLoaded = false
        this.testSuiteStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  createActor() {
    this.routingService.toCreateActor(this.domainId, this.specificationId)
  }

	uploadTestSuite() {
    const modal = this.modalService.show(TestSuiteUploadModalComponent, {
      class: 'modal-lg',
      keyboard: false,
      backdrop: 'static',
      initialState: {
				availableSpecifications: [this.specification as Specification],
				testSuitesVisible: true,
        domainId: this.domainId
      }
    })
    modal.content!.completed.subscribe((testSuitesUpdated: boolean) => {
      if (testSuitesUpdated) {
        this.loadTestSuites(true)
        this.loadActors(true)
      }
    })
  }

  loadAvailableSharedTestSuites() {
    if (!this.availableSharedTestSuitesLoaded) {
      this.conformanceService.getSharedTestSuites(this.domainId)
      .subscribe((data) => {
        this.availableSharedTestSuites = filter(data, (testSuite) => {
          const foundTestSuite = find(this.sharedTestSuites, (linkedTestSuite) => {
            return linkedTestSuite.id == testSuite.id
          })
          return foundTestSuite == undefined
        })
      }).add(() => {
        this.availableSharedTestSuitesLoaded = true
      })
    }
  }

  linkTestSuite(testSuite: TestSuite) {
    this.linkPending = true
    this.clearAlerts()
    this.conformanceService.linkSharedTestSuite(testSuite.id, [this.specificationId]).pipe(
      mergeMap((result) => {
        if (result.needsConfirmation) {
          const modalRef = this.modalService.show(LinkSharedTestSuiteModalComponent, {
            class: 'modal-lg',
            keyboard: false,
            backdrop: 'static',
            initialState: {
              testSuiteId: testSuite.id,
              domainId: this.domainId,
              step: 'confirm',
              uploadResult: result,
              availableSpecifications: [this.specification as Specification]
            }
          })
          return modalRef.content!.completed
        } else if (result.success) {
          this.popupService.success('Test suite linked to '+this.dataService.labelSpecificationLower()+'.')
          this.linkPending = false
          return of(true)
        } else {
          this.showErrorMessage(result)
          this.linkPending = false
          return of(false)
        }
      }),
      share()
    ).subscribe((reloadData) => {
      if (reloadData) {
        this.loadTestSuites(true)
        // Actors may have been updated through the linking process
        this.actorStatus.status = Constants.STATUS.NONE
      }
      this.linkPending = false
    })
  }

  showErrorMessage(uploadResult: TestSuiteUploadResult) {
    let msg: string
    if (uploadResult != undefined) {
      if (uploadResult.errorInformation != undefined) {
        msg = 'An error occurred while processing the test suite: '+uploadResult.errorInformation
      } else {
        msg = 'An error occurred while processing the test suite'
      }
    } else {
      msg = 'An error occurred while processing the test suite: Response was empty'
    }
    this.addAlertError(msg)
  }

  unlinkTestSuite(testSuite: TestSuite) {
    this.unlinkPending = true
    this.conformanceService.unlinkSharedTestSuite(testSuite.id, [this.specificationId]).subscribe(() => {
      this.popupService.success('Test suite unlinked from '+this.dataService.labelSpecificationLower()+'.')
    }).add(() => {
      this.loadTestSuites(true)
      this.unlinkPending = false
    })
  }

	onActorSelect(actor: Actor) {
    this.routingService.toActor(this.domainId, this.specificationId, actor.id)
  }

	onTestSuiteSelect(testSuite: TestSuite) {
    this.routingService.toTestSuite(this.domainId, this.specificationId, testSuite.id)
  }

	deleteSpecification() {
		this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelSpecificationLower()+"?", "Delete", "Cancel")
		.subscribe(() => {
      this.deletePending = true
      this.specificationService.deleteSpecification(this.specificationId)
      .subscribe(() => {
        this.routingService.toDomain(this.domainId)
        this.popupService.success(this.dataService.labelSpecification()+' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

	saveSpecificationChanges() {
    this.savePending = true
		this.specificationService.updateSpecification(this.specificationId, this.specification.sname!, this.specification.fname!, this.specification.description, this.specification.reportMetadata, this.specification.hidden, this.specification.group, this.specification.badges!)
		.subscribe(() => {
			this.popupService.success(this.dataService.labelSpecification()+' updated.')
      this.dataService.breadcrumbUpdate({id: this.specificationId, type: BreadcrumbType.specification, label: this.breadcrumbLabel()})
    }).add(() => {
      this.savePending = false
    })
  }

	saveDisabled() {
    return !(
      this.textProvided(this.specification?.sname) && 
      this.textProvided(this.specification?.fname) &&
      this.dataService.badgesValid(this.specification?.badges)
    )
  }

	back() {
    if (this.sharedTestSuiteId) {
      this.routingService.toSharedTestSuite(this.domainId, this.sharedTestSuiteId)
    } else {
      this.routingService.toDomain(this.domainId)
    }
  }

}
