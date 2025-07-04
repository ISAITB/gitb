/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import {AfterViewInit, Component, EventEmitter, OnInit} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, find } from 'lodash';
import { BsModalService } from 'ngx-bootstrap/modal';
import {forkJoin, map, mergeMap, Observable, of, share, tap} from 'rxjs';
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
import {FilterUpdate} from '../../../../../components/test-filter/filter-update';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';

@Component({
    selector: 'app-specification-details',
    templateUrl: './specification-details.component.html',
    standalone: false
})
export class SpecificationDetailsComponent extends BaseTabbedComponent implements OnInit, AfterViewInit {

  sharedTestSuiteId?: number
  specification: Partial<Specification> = {}
  actors: Actor[] = []
  testSuites: TestSuite[] = []
  sharedTestSuites: TestSuite[] = []
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

  linkSharedSelectionConfig!: MultiSelectConfig<TestSuite>
  unlinkSharedSelectionConfig!: MultiSelectConfig<TestSuite>

  constructor(
    public readonly dataService: DataService,
    private readonly conformanceService: ConformanceService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly specificationService: SpecificationService,
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    router: Router,
    private readonly popupService: PopupService,
    private readonly modalService: BsModalService
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
    this.linkSharedSelectionConfig = {
      name: 'sharedTestSuitesAvailable',
      textField: 'identifier',
      singleSelection: true,
      filterLabel: 'Link shared test suite',
      noItemsMessage: 'No shared test suites available to link.',
      searchPlaceholder: 'Search test suite...',
      clearItems: new EventEmitter(),
      loader: () => this.loadAvailableSharedTestSuites()
    }
    this.unlinkSharedSelectionConfig = {
      name: 'sharedTestSuitesLinked',
      textField: 'identifier',
      singleSelection: true,
      filterLabel: 'Unlink shared test suite',
      searchPlaceholder: 'Search test suite...',
      clearItems: new EventEmitter(),
      loader: () => this.loadLinkedSharedTestSuites()
    }
    const groupObservable = this.specificationService.getDomainSpecificationGroups(this.domainId)
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
          this.specification.groupObject = undefined
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
      if (this.specification.groups && this.specification.groups.length > 0 && this.specification.group != undefined) {
        this.specification.groupObject = this.specification.groups.find(x => x.id == this.specification.group)
      }
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
    this.linkSharedSelectionConfig.clearItems?.emit()
    this.unlinkSharedSelectionConfig.clearItems?.emit()
    this.loadTestSuitesInternal(forceLoad).subscribe(() => {})
  }

  loadTestSuitesInternal(forceLoad?: boolean): Observable<{all: TestSuite[], shared: TestSuite[]}> {
    if (this.testSuiteStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.testSuiteStatus.status = Constants.STATUS.PENDING
      this.testSuites = []
      this.sharedTestSuites = []
      return this.conformanceService.getTestSuites(this.specificationId).pipe(
        mergeMap((data) => {
          this.testSuites = data
          this.sharedTestSuites = filter(this.testSuites, (ts) => ts.shared)
          return of({
            all: this.testSuites,
            shared: this.sharedTestSuites
          })
        }),
        tap(() => {
          this.availableSharedTestSuitesLoaded = false
          this.testSuiteStatus.status = Constants.STATUS.FINISHED
        }),
        share()
      )
    } else {
      return of({
        all: this.testSuites,
        shared: this.sharedTestSuites
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

  loadLinkedSharedTestSuites(): Observable<TestSuite[]> {
    return this.loadTestSuitesInternal().pipe(
      map((data) => {
        return data.shared
      })
    )
  }

  loadAvailableSharedTestSuites(): Observable<TestSuite[]> {
    const sharedTestSuitesInDomain$ = this.conformanceService.getSharedTestSuites(this.domainId)
    const specificationTestSuites$ = this.loadTestSuitesInternal()
    return forkJoin([sharedTestSuitesInDomain$, specificationTestSuites$]).pipe(
      map((results) => {
        const sharedTestSuitesInDomain = results[0]
        const sharedTestSuitesInSpecification = results[1].shared
        return filter(sharedTestSuitesInDomain, (testSuite) => {
          const foundTestSuite = find(sharedTestSuitesInSpecification, (linkedTestSuite) => {
            return linkedTestSuite.id == testSuite.id
          })
          return foundTestSuite == undefined
        })
      })
    )
  }

  linkTestSuite(event: FilterUpdate<TestSuite>) {
    const testSuite = event.values.active[0]
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

  unlinkTestSuite(event: FilterUpdate<TestSuite>) {
    const testSuite = event.values.active[0]
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
