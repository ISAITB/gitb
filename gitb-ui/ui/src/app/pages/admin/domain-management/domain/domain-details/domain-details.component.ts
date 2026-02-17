/*
 * Copyright (C) 2026 European Union
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

import {Component, EventEmitter, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {
  CreateEditDomainParameterModalComponent
} from 'src/app/modals/create-edit-domain-parameter-modal/create-edit-domain-parameter-modal.component';
import {TestSuiteUploadModalComponent} from 'src/app/modals/test-suite-upload-modal/test-suite-upload-modal.component';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {Domain} from 'src/app/types/domain';
import {DomainParameter} from 'src/app/types/domain-parameter';
import {Specification} from 'src/app/types/specification';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {saveAs} from 'file-saver';
import {TestSuite} from 'src/app/types/test-suite';
import {BaseTabbedComponent} from 'src/app/pages/base-tabbed-component';
import {SpecificationService} from 'src/app/services/specification.service';
import {finalize, forkJoin, map, Observable, of, ReplaySubject} from 'rxjs';
import {DomainSpecification} from 'src/app/types/domain-specification';
import {SpecificationGroup} from 'src/app/types/specification-group';
import {BreadcrumbType} from 'src/app/types/breadcrumb-type';
import {CdkDragDrop} from '@angular/cdk/drag-drop';
import {TableApi} from '../../../../../components/table/table-api';
import {PagingEvent} from '../../../../../components/paging-controls/paging-event';
import {DomainParameterService} from '../../../../../services/domain-parameter.service';
import {TestServiceRow} from './test-service-row';
import {TestServiceWithParameter} from '../../../../../types/test-service-with-parameter';
import {
  CreateEditTestServiceModalComponent
} from '../../../../../modals/create-edit-test-service-modal/create-edit-test-service-modal.component';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';
import {FilterUpdate} from '../../../../../components/test-filter/filter-update';
import {TestService} from '../../../../../types/test-service';
import {PagingControlsApi} from '../../../../../components/paging-controls/paging-controls-api';
import {PagingPlacement} from '../../../../../components/paging-controls/paging-placement';
import {
  DomainSpecificationDisplayComponentApi
} from '../../../../../components/domain-specification-display/domain-specification-display-component-api';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-domain-details',
    templateUrl: './domain-details.component.html',
    styleUrls: ['./domain-details.component.less'],
    standalone: false
})
export class DomainDetailsComponent extends BaseTabbedComponent implements OnInit {

  private static readonly MESSAGING_SERVICE_ENDPOINT_REGEXP = /^https?:\/\/\S+\/messaging/
  private static readonly VALIDATION_SERVICE_ENDPOINT_REGEXP = /^https?:\/\/\S+\/validation/
  private static readonly PROCESSING_SERVICE_ENDPOINT_REGEXP = /^https?:\/\/\S+\/process/

  @ViewChild("sharedTestSuiteTable") sharedTestSuiteTable?: TableApi
  @ViewChild("servicesTable") servicesTable?: TableApi
  @ViewChild("specificationPagingControls") specificationPagingControls?: PagingControlsApi
  @ViewChild("parameterPagingControls") parameterPagingControls?: PagingControlsApi
  @ViewChildren("specificationDisplayComponent") specificationDisplayComponents?: QueryList<DomainSpecificationDisplayComponentApi>

  domain: Partial<Domain> = {}
  domainSpecifications: DomainSpecification[] = []
  specifications: Specification[] = []
  hasGroups = false
  specificationGroups: SpecificationGroup[] = []
  sharedTestSuites: TestSuite[] = []
  domainParameters: DomainParameter[] = []
  testServices: TestServiceRow[] = []
  domainId!: number
  specificationStatus = {status: Constants.STATUS.NONE}
  sharedTestSuiteStatus = {status: Constants.STATUS.NONE}
  parameterStatus = {status: Constants.STATUS.NONE}
  testServiceStatus = {status: Constants.STATUS.NONE}
  specificationPage = 1
  sharedTestSuitePage = 1
  parameterPage = 1
  testServicePage = 1
  specificationTotal = 0
  sharedTestSuiteTotal = 0
  parameterTotal = 0
  testServiceTotal = 0
  sharedTestSuiteTableColumns: TableColumnDefinition[] = [
    { field: 'identifier', title: 'ID' },
    { field: 'sname', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'version', title: 'Version' }
  ]
  testServiceTableColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'ID' },
    { field: 'endpoint', title: 'Endpoint address' },
    { field: 'description', title: 'Description' },
    { field: 'serviceType', title: 'Service type' }
  ]
  savePending = false
  deletePending = false
  saveOrderPending = false
  dragOngoing = false
  loaded = false
  specificationsRefreshing = false
  sharedTestSuitesRefreshing = false
  parametersRefreshing = false
  servicesRefreshing = false
  sharedTestSuiteFilter?: string
  parameterFilter?: string
  testServiceFilter?: string
  domainParameterColumnCount = 4
  hasTestServices = false
  convertParameterSelectionConfig!: MultiSelectConfig<DomainParameter>
  convertPending = false
  specificationFilter?: string
  specificationGroupsLoaded = false
  managingSpecificationOrder = false
  hasMultipleSpecifications = false
  protected readonly PagingPlacement = PagingPlacement;

  constructor(
    public readonly dataService: DataService,
    private readonly specificationService: SpecificationService,
    private readonly domainParameterService: DomainParameterService,
    private readonly conformanceService: ConformanceService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly modalService: NgbModal,
    private readonly popupService: PopupService,
    private readonly routingService: RoutingService,
    route: ActivatedRoute,
    router: Router
  ) { super(router, route) }

  loadTab(tabIndex: number): void {
    if (tabIndex == Constants.TAB.DOMAIN.PARAMETERS) {
      this.loadDomainParameters()
    } else if (tabIndex == Constants.TAB.DOMAIN.SPECIFICATIONS) {
      this.loadSpecifications()
    } else if (tabIndex == Constants.TAB.DOMAIN.TEST_SERVICES) {
      this.loadTestServices()
    } else {
      this.loadSharedTestSuites()
    }
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.conformanceService.getDomain(this.domainId)
    .subscribe((data) => {
      this.domain = data
      this.routingService.domainBreadcrumbs(this.domainId, this.domain.sname!)
    }).add(() => {
      this.loaded = true
    })
    this.convertParameterSelectionConfig = {
      name: 'convertParameterToService',
      textField: 'name',
      singleSelection: true,
      filterLabel: 'Register from parameter',
      filterLabelIcon: Constants.BUTTON_ICON.NEW,
      noItemsMessage: 'No candidate parameters available.',
      searchPlaceholder: 'Search parameter...',
      clearItems: new EventEmitter(),
      loader: () => this.domainParameterService.getAvailableDomainParametersForTestServiceConversion(this.domainId)
    }
  }

  toggleSpecificationGroupCollapse(collapse: boolean) {
    for (let spec of this.domainSpecifications) {
      if (spec.group) {
        spec.collapsed = collapse
      }
    }
  }

  refreshSharedTestSuites() {
    this.loadSharedTestSuitesInternal({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
  }

  loadSpecifications() {
    if (this.specificationStatus.status == Constants.STATUS.NONE) {
      this.loadSpecificationsInternal({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize })
    } else {
      this.updateSpecificationPaging(this.specificationPage, this.specificationTotal)
    }
  }

  loadSharedTestSuites(forceLoad?: boolean) {
    if (this.sharedTestSuiteStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.refreshSharedTestSuites()
    } else {
      this.updateTestSuitePaging(this.sharedTestSuitePage, this.sharedTestSuiteTotal)
    }
  }

  loadDomainParameters(forceLoad?: boolean) {
    if (this.parameterStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.loadDomainParametersInternal({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize }, forceLoad)
    } else {
      this.updateParameterPaging(this.parameterPage, this.parameterTotal)
    }
  }

  loadTestServices(forceLoad?: boolean) {
    if (this.testServiceStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.loadTestServicesInternal({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize }, forceLoad).subscribe(() => {})
    } else {
      this.updateTestServicePaging(this.testServicePage, this.testServiceTotal)
    }
  }

  private loadSpecificationsInternal(pagingInfo: PagingEvent) {
    if (this.specificationStatus.status == Constants.STATUS.FINISHED) {
      this.specificationsRefreshing = true
    } else {
      this.specificationStatus.status = Constants.STATUS.PENDING
    }
    const specsObservable = this.conformanceService.getDomainSpecificationsPaged(this.domainId, pagingInfo.targetPage, pagingInfo.targetPageSize, this.specificationFilter)
    let specGroupsObservable: Observable<SpecificationGroup[]>
    if (!this.specificationGroupsLoaded) {
      specGroupsObservable = this.specificationService.getDomainSpecificationGroups(this.domainId)
    } else {
      specGroupsObservable = of(this.specificationGroups)
    }
    forkJoin([specsObservable, specGroupsObservable]).subscribe((results) => {
      this.specificationGroups = results[1]
      this.hasGroups = this.specificationGroups.length > 0
      this.specificationGroupsLoaded = true
      this.domainSpecifications = results[0].data
      this.specifications = this.dataService.toSpecifications(this.domainSpecifications)
      if (this.specificationStatus.status == Constants.STATUS.PENDING) {
        // This was the very first load.
        this.hasMultipleSpecifications = results[0].count > 1
      }
      this.toggleSpecificationGroupCollapse(false)
      this.updateSpecificationPaging(pagingInfo.targetPage, results[0].count)
    }).add(() => {
      this.specificationsRefreshing = false
      this.specificationStatus.status = Constants.STATUS.FINISHED
    })
  }

  private loadSharedTestSuitesInternal(pagingInfo: PagingEvent) {
    if (this.sharedTestSuiteStatus.status == Constants.STATUS.FINISHED) {
      this.sharedTestSuitesRefreshing = true
    } else {
      this.sharedTestSuiteStatus.status = Constants.STATUS.PENDING
    }
    this.conformanceService.searchSharedTestSuites(this.domainId, this.sharedTestSuiteFilter, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.sharedTestSuites = data.data
        this.updateTestSuitePaging(pagingInfo.targetPage, data.count)
      }).add(() => {
      this.sharedTestSuitesRefreshing = false
      this.sharedTestSuiteStatus.status = Constants.STATUS.FINISHED
    })
  }

  private loadDomainParametersInternal(pagingInfo: PagingEvent, forceLoad?: boolean) {
    if (this.parameterStatus.status == Constants.STATUS.FINISHED || forceLoad) {
      this.parametersRefreshing = true
    } else {
      this.parameterStatus.status = Constants.STATUS.PENDING
    }
    this.domainParameterService.searchDomainParameters(this.domainId, this.parameterFilter, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe((data) => {
        this.domainParameters = data.data
        for (let parameter of this.domainParameters) {
          if (parameter.kind == 'HIDDEN') {
            parameter.valueToShow = "*****"
          } else if (parameter.kind == 'BINARY') {
            const extension = this.dataService.extensionFromMimeType(parameter.contentType)
            parameter.valueToShow =  parameter.name+extension
          } else {
            parameter.valueToShow = parameter.value
          }
        }
        this.hasTestServices = this.domainParameters.find(p => p.isTestService) != undefined
        if (this.hasTestServices) {
          this.domainParameterColumnCount = 5
        } else {
          this.domainParameterColumnCount = 4
        }
        this.updateParameterPaging(pagingInfo.targetPage, data.count)
      }).add(() => {
        this.parametersRefreshing = false
        this.parameterStatus.status = Constants.STATUS.FINISHED
    })
  }

  private loadTestServicesInternal(pagingInfo: PagingEvent, forceLoad?: boolean): Observable<void> {
    const completed$ = new ReplaySubject<void>(1);
    if (this.testServiceStatus.status == Constants.STATUS.FINISHED || forceLoad) {
      this.servicesRefreshing = true
    } else {
      this.testServiceStatus.status = Constants.STATUS.PENDING
    }
    this.domainParameterService.searchTestServices(this.domainId, this.testServiceFilter, pagingInfo.targetPage, pagingInfo.targetPageSize).pipe(
      map((data) => {
        this.testServices = data.data.map((service) => {
          return {
            serviceType: this.dataService.testServiceTypeLabel(service.service.serviceType),
            apiType: this.dataService.testServiceApiTypeLabel(service.service.apiType),
            name: service.parameter.name,
            endpoint: service.parameter.value!,
            description: service.parameter.description,
            data: service
          }
        })
        this.updateTestServicePaging(pagingInfo.targetPage, data.count)
      }),
      finalize(() => {
        this.servicesRefreshing = false
        this.testServiceStatus.status = Constants.STATUS.FINISHED
        completed$.next()
        completed$.complete()
      })
    ).subscribe()
    return completed$.asObservable()
  }

  private updateSpecificationPaging(page: number, count: number) {
    this.specificationPagingControls?.updateStatus(page, count)
    this.specificationPage = page
    this.specificationTotal = count
  }

  private updateTestSuitePaging(page: number, count: number) {
    this.sharedTestSuiteTable?.getPagingControls()?.updateStatus(page, count)
    this.sharedTestSuitePage = page
    this.sharedTestSuiteTotal = count
  }

  private updateParameterPaging(page: number, count: number) {
    this.parameterPagingControls?.updateStatus(page, count)
    this.parameterPage = page
    this.parameterTotal = count
  }

  private updateTestServicePaging(page: number, count: number) {
    this.servicesTable?.getPagingControls()?.updateStatus(page, count)
    this.testServicePage = page
    this.testServiceTotal = count
  }

  doSpecificationPaging(event: PagingEvent) {
    this.loadSpecificationsInternal(event)
    if (event.pageSizeChanged) {
      this.sharedTestSuiteStatus.status = Constants.STATUS.NONE
      this.testServiceStatus.status = Constants.STATUS.NONE
      this.parameterStatus.status = Constants.STATUS.NONE
    }
  }

  doSharedTestSuitePaging(event: PagingEvent) {
    this.loadSharedTestSuitesInternal(event)
    if (event.pageSizeChanged) {
      this.specificationStatus.status = Constants.STATUS.NONE
      this.parameterStatus.status = Constants.STATUS.NONE
      this.testServiceStatus.status = Constants.STATUS.NONE
    }
  }

  doParameterPaging(event: PagingEvent) {
    this.loadDomainParametersInternal(event)
    if (event.pageSizeChanged) {
      this.specificationStatus.status = Constants.STATUS.NONE
      this.sharedTestSuiteStatus.status = Constants.STATUS.NONE
      this.testServiceStatus.status = Constants.STATUS.NONE
    }
  }

  doServicePaging(event: PagingEvent) {
    this.loadTestServicesInternal(event)
    if (event.pageSizeChanged) {
      this.specificationStatus.status = Constants.STATUS.NONE
      this.sharedTestSuiteStatus.status = Constants.STATUS.NONE
      this.parameterStatus.status = Constants.STATUS.NONE
    }
  }

	downloadParameter(parameter: DomainParameter) {
    this.domainParameterService.downloadDomainParameterFile(this.domainId, parameter.id)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: parameter.contentType})
      const extension = this.dataService.extensionFromMimeType(parameter.contentType)
      saveAs(blobData, parameter.name+extension)
    })
  }

	deleteDomain() {
		this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelDomainLower()+"?", "Delete", "Cancel", Constants.BUTTON_ICON.DELETE)
    .subscribe(() => {
      this.deletePending = true
      this.conformanceService.deleteDomain(this.domainId)
      .subscribe(() => {
        this.popupService.success(this.dataService.labelDomain()+' deleted.')
        this.routingService.toDomains()
      }).add(() => {
        this.deletePending = false
      })
    })
  }

	saveDisabled() {
    return !(this.textProvided(this.domain?.sname) && this.textProvided(this.domain?.fname))
  }

	saveDomainChanges() {
    this.savePending = true
		this.conformanceService.updateDomain(this.domainId, this.domain.sname!, this.domain.fname!, this.domain.description, this.domain.reportMetadata)
    .subscribe(() => {
      this.popupService.success(this.dataService.labelDomain()+' updated.')
      this.dataService.breadcrumbUpdate({id: this.domainId, type: BreadcrumbType.domain, label: this.domain.sname!})
    }).add(() => {
      this.savePending = false
    })
  }

	back() {
    this.routingService.toDomains()
  }

	onSpecificationSelect(specification: DomainSpecification) {
    if (!this.managingSpecificationOrder) {
      this.routingService.toSpecification(this.domainId, specification.id)
    }
  }

	onSpecificationGroupSelect(groupOrOption: DomainSpecification) {
    if (groupOrOption.group) {
      groupOrOption.collapsed = !groupOrOption.collapsed
    } else {
      this.onSpecificationSelect(groupOrOption)
    }
  }

  onSharedTestSuiteSelect(testSuite: TestSuite) {
    this.routingService.toSharedTestSuite(this.domainId, testSuite.id)
  }

	openParameterModal(domainParameter: Partial<DomainParameter>) {
    const modalRef = this.modalService.open(CreateEditDomainParameterModalComponent, { size: 'lg'})
    const modalInstance = modalRef.componentInstance as CreateEditDomainParameterModalComponent
    modalInstance.domainParameter = domainParameter
    modalInstance.domainId = this.domain.id!
    modalRef.closed.subscribe(() => {
      this.loadDomainParameters(true)
    })
  }

  openTestServiceModal(testService: Partial<TestServiceWithParameter>, updateMatching: boolean) {
    const modalRef = this.modalService.open(CreateEditTestServiceModalComponent, { size: 'lg' })
    const modalInstance = modalRef.componentInstance as CreateEditTestServiceModalComponent
    modalInstance.testService = testService
    modalInstance.domainId = this.domain.id!
    modalInstance.updateMatching = updateMatching
    modalRef.closed.subscribe(() => {
      this.loadTestServices(true)
      // Ensure that the next time we navigate to the domain parameters tab it is also updated
      this.parameterStatus.status = Constants.STATUS.NONE
    })
  }

	onDomainParameterSelect(domainParameter: DomainParameter) {
    if (domainParameter.isTestService) {
      // Display the requested test service
      this.loadTestServicesInternal({ targetPage: 1, targetPageSize: this.dataService.defaultPagingTableSize }, true).subscribe(() => {
        const selectedService = this.testServices.find(service => service.data.parameter.id == domainParameter.id)
        if (selectedService) {
          this.onTestServiceSelect(selectedService)
        }
      })
      // Switch to the test services tab
      if (this.tabs) {
        this.tabIdToShow = Constants.TAB.DOMAIN.TEST_SERVICES
      }
    } else {
      this.openParameterModal(domainParameter)
    }
  }

  onTestServiceSelect(serviceRow: TestServiceRow) {
    this.openTestServiceModal(serviceRow.data, false)
  }

	createDomainParameter() {
		this.openParameterModal({})
  }

  createTestService() {
    this.openTestServiceModal({}, false)
  }

	uploadTestSuite() {
    const modal = this.modalService.open(TestSuiteUploadModalComponent, { size: 'lg', backdrop: 'static', keyboard: false })
    const modalInstance = modal.componentInstance as TestSuiteUploadModalComponent
    modalInstance.availableSpecifications = this.specifications
    modalInstance.testSuitesVisible = false
    modalInstance.domainId = this.domainId
  }

  uploadSharedTestSuite() {
    const modal = this.modalService.open(TestSuiteUploadModalComponent, { size: 'lg', backdrop: 'static', keyboard: false })
    const modalInstance = modal.componentInstance as TestSuiteUploadModalComponent
    modalInstance.availableSpecifications = this.specifications
    modalInstance.sharedTestSuite = true
    modalInstance.domainId = this.domainId
    modal.closed.subscribe((testSuitesUpdated?: boolean) => {
      if (testSuitesUpdated) {
        this.loadSharedTestSuites(true)
      }
    })
  }

	createSpecification() {
    this.routingService.toCreateSpecification(this.domainId)
  }

  createSpecificationGroup() {
    this.routingService.toCreateSpecificationGroup(this.domainId)
  }

  private reloadCurrentSpecificationPage() {
    let page = this.specificationPagingControls?.getCurrentStatus().currentPage
    let pageSize = this.specificationPagingControls?.getCurrentStatus().pageSize
    if (page == undefined) page = 1
    if (pageSize == undefined) pageSize = this.dataService.defaultPagingTableSize
    this.loadSpecificationsInternal({ targetPage: page, targetPageSize: pageSize })
  }

  removeSpecificationFromGroup(specificationId: number) {
    this.specificationService.removeSpecificationFromGroup(specificationId).subscribe(() => {
      this.popupService.success(this.dataService.labelSpecificationInGroup() + ' removed.')
      this.reloadCurrentSpecificationPage()
    })
  }

  copySpecificationToGroup(specificationId: number, newGroupId: number) {
    this.specificationService.copySpecificationToGroup(newGroupId, specificationId).subscribe(() => {
      this.popupService.success(this.dataService.labelSpecificationInGroup()+' copied.')
      this.reloadCurrentSpecificationPage()
    })
  }

  moveSpecificationToGroup(specificationId: number, newGroupId: number) {
    this.specificationService.addSpecificationToGroup(newGroupId, specificationId).subscribe(() => {
      this.popupService.success(this.dataService.labelSpecificationInGroup()+' moved.')
      this.reloadCurrentSpecificationPage()
    })
  }

  dropSpecification(event: CdkDragDrop<any>) {
    if (event.currentIndex != event.previousIndex && this.domainSpecifications) {
      this.domainSpecifications.splice(event.currentIndex, 0, this.domainSpecifications.splice(event.previousIndex, 1)[0]);
    }
  }

  saveOrdering() {
    const groupIds: number[] = []
    const groupOrders: number[] = []
    const specIds: number[] = []
    const specOrders: number[] = []
    let i = 0
    for (let spec of this.domainSpecifications) {
      if (spec.group) {
        groupIds.push(spec.id)
        groupOrders.push(i)
      } else {
        specIds.push(spec.id)
        specOrders.push(i)
      }
      if (spec.options) {
        let j = 0
        for (let option of spec.options) {
          specIds.push(option.id)
          specOrders.push(j)
          j += 1
        }
      }
      i += 1
    }
    this.saveOrderPending = true
    this.specificationService.saveSpecificationOrder(this.domainId, groupIds, groupOrders, specIds, specOrders)
    .subscribe(() => {
      this.popupService.success("Ordering saved successfully.")
    }).add(() => {
      this.managingSpecificationOrder = false
      this.saveOrderPending = false
    })
  }

  manageOrdering() {
    this.managingSpecificationOrder = true
    this.specificationFilter = undefined
    this.loadSpecificationsInternal({ targetPage: 1, targetPageSize: 1000000})
  }

  cancelManageOrdering() {
    this.managingSpecificationOrder = false
    this.loadSpecifications()
  }

  resetOrdering() {
    this.saveOrderPending = true
    this.specificationService.resetSpecificationOrder(this.domainId)
    .subscribe(() => {
      this.popupService.success("Ordering reset successfully.")
      this.loadSpecifications()
    }).add(() => {
      this.saveOrderPending = false
    })
  }

  convertParameterToTestService(event: FilterUpdate<DomainParameter>) {
    const parameter = event.values.active[0]
    const guessedServiceType = this.guessTestServiceTypeFromParameter(parameter)
    let service: TestService|undefined
    if (guessedServiceType != undefined) {
      service = {
        id: 0,
        serviceType: guessedServiceType,
        apiType: Constants.TEST_SERVICE_API_TYPE.SOAP,
        monitor: true,
        parameter: parameter.id
      }
    }
    this.openTestServiceModal({
      service: service,
      parameter: parameter
    }, true)
  }

  private guessTestServiceTypeFromParameter(parameter: DomainParameter): number|undefined {
    let guessedServiceType: number|undefined
    let textToCheck = parameter.value!.toLowerCase()
    // Check endpoint address
    if (DomainDetailsComponent.VALIDATION_SERVICE_ENDPOINT_REGEXP.test(textToCheck)) {
      guessedServiceType = Constants.TEST_SERVICE_TYPE.VALIDATION
    } else if (DomainDetailsComponent.MESSAGING_SERVICE_ENDPOINT_REGEXP.test(textToCheck)) {
      guessedServiceType = Constants.TEST_SERVICE_TYPE.MESSAGING
    } else if (DomainDetailsComponent.PROCESSING_SERVICE_ENDPOINT_REGEXP.test(textToCheck)) {
      guessedServiceType = Constants.TEST_SERVICE_TYPE.PROCESSING
    }
    if (guessedServiceType == undefined) {
      // Check parameter name
      textToCheck = parameter.name.toLowerCase()
      if (textToCheck.includes("valid")) {
        guessedServiceType = Constants.TEST_SERVICE_TYPE.VALIDATION
      } else if (textToCheck.includes("message") || textToCheck.includes("messaging")) {
        guessedServiceType = Constants.TEST_SERVICE_TYPE.MESSAGING
      } else if (textToCheck.includes("process")) {
        guessedServiceType = Constants.TEST_SERVICE_TYPE.PROCESSING
      }
    }
    return guessedServiceType
  }

  onSpecificationDisplayControlSelected(selectedId: number) {
    this.specificationDisplayComponents?.forEach((component) => {
      component.otherControlSelected(selectedId)
    })
  }

}
