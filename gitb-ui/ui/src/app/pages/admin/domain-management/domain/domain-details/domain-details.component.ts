import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { CreateEditDomainParameterModalComponent } from 'src/app/modals/create-edit-domain-parameter-modal/create-edit-domain-parameter-modal.component';
import { TestSuiteUploadModalComponent } from 'src/app/modals/test-suite-upload-modal/test-suite-upload-modal.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Domain } from 'src/app/types/domain';
import { DomainParameter } from 'src/app/types/domain-parameter';
import { Specification } from 'src/app/types/specification';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { saveAs } from 'file-saver'
import { TestSuite } from 'src/app/types/test-suite';
import { BaseTabbedComponent } from 'src/app/pages/base-tabbed-component';
import { SpecificationService } from 'src/app/services/specification.service';
import { forkJoin } from 'rxjs';
import { DomainSpecification } from 'src/app/types/domain-specification';
import { SpecificationGroup } from 'src/app/types/specification-group';
import { find, remove } from 'lodash';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
    selector: 'app-domain-details',
    templateUrl: './domain-details.component.html',
    styleUrls: ['./domain-details.component.less'],
    standalone: false
})
export class DomainDetailsComponent extends BaseTabbedComponent implements OnInit, AfterViewInit {

  domain: Partial<Domain> = {}

  domainSpecifications: DomainSpecification[] = []
  specifications: Specification[] = []
  hasGroups = false
  specificationGroups: SpecificationGroup[] = []
  sharedTestSuites: TestSuite[] = []
  domainParameters: DomainParameter[] = []
  domainId!: number
  specificationStatus = {status: Constants.STATUS.NONE}
  sharedTestSuiteStatus = {status: Constants.STATUS.NONE}
  parameterStatus = {status: Constants.STATUS.NONE}
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description' },
    { field: 'hidden', title: 'Hidden' }
  ]
  sharedTestSuiteTableColumns: TableColumnDefinition[] = [
    { field: 'identifier', title: 'ID' },
    { field: 'sname', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'version', title: 'Version' }
  ]
  savePending = false
  deletePending = false
  saveOrderPending = false
  dragOngoing = false
  loaded = false

  constructor(
    public dataService: DataService,
    private specificationService: SpecificationService,
    private conformanceService: ConformanceService,
    private confirmationDialogService: ConfirmationDialogService,
    private modalService: BsModalService,
    private popupService: PopupService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    router: Router
  ) { super(router) }

  loadTab(tabIndex: number): void {
    if (tabIndex == Constants.TAB.DOMAIN.PARAMETERS) {
      this.loadDomainParameters()
    } else if (tabIndex == Constants.TAB.DOMAIN.SPECIFICATIONS) {
      this.loadSpecifications()
    } else {
      this.loadSharedTestSuites()
    }
  }

  ngAfterViewInit(): void {
    this.showTab()
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
    this.loadSpecifications()
  }

  toggleSpecificationGroupCollapse(collapse: boolean) {
    for (let spec of this.domainSpecifications) {
      if (spec.group) {
        spec.collapsed = collapse
      }
    }
  }

  loadSpecifications(force?: boolean) {
    if (this.specificationStatus.status == Constants.STATUS.NONE || force) {
      this.specificationStatus.status = Constants.STATUS.PENDING
      this.domainSpecifications = []
      const specsObservable = this.conformanceService.getDomainSpecifications(this.domainId, false)
      const specGroupsObservable = this.specificationService.getDomainSpecificationGroups(this.domainId)
      forkJoin([specsObservable, specGroupsObservable]).subscribe((results) => {
        this.specificationGroups = results[1]
        this.hasGroups = this.specificationGroups.length > 0
        this.domainSpecifications = this.dataService.toDomainSpecifications(this.specificationGroups, results[0])
        this.specifications = this.dataService.toSpecifications(this.domainSpecifications)
      }).add(() => {
        this.specificationStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  loadSharedTestSuites(forceLoad?: boolean) {
    if (this.sharedTestSuiteStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.sharedTestSuiteStatus.status = Constants.STATUS.PENDING
      this.sharedTestSuites = []
      this.conformanceService.getSharedTestSuites(this.domainId)
      .subscribe((data) => {
        this.sharedTestSuites = data
      }).add(() => {
        this.sharedTestSuiteStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  loadDomainParameters(forceLoad?: boolean) {
    if (this.parameterStatus.status == Constants.STATUS.NONE || forceLoad) {
      this.domainParameters = []
      this.parameterStatus.status = Constants.STATUS.PENDING
      this.conformanceService.getDomainParameters(this.domainId, true, false)
      .subscribe((data) => {
        for (let parameter of data) {
          if (parameter.kind == 'HIDDEN') {
            parameter.valueToShow = "*****"
          } else if (parameter.kind == 'BINARY') {
            const extension = this.dataService.extensionFromMimeType(parameter.contentType)
            parameter.valueToShow =  parameter.name+extension
          } else {
            parameter.valueToShow = parameter.value
          }
          this.domainParameters.push(parameter)
        }
      }).add(() => {
        this.parameterStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

	downloadParameter(parameter: DomainParameter) {
    this.conformanceService.downloadDomainParameterFile(this.domainId, parameter.id)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: parameter.contentType})
      const extension = this.dataService.extensionFromMimeType(parameter.contentType)
      saveAs(blobData, parameter.name+extension)
    })
  }

	deleteDomain() {
		this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelDomainLower()+"?", "Delete", "Cancel")
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
    this.routingService.toSpecification(this.domainId, specification.id)
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
    const modalRef = this.modalService.show(CreateEditDomainParameterModalComponent, {
      class: 'modal-lg',
      initialState: {
        domainParameter: domainParameter,
        domainId: this.domain.id
      }
    })
    modalRef.content!.parametersUpdated.subscribe((updated: boolean) => {
      if (updated) {
        this.loadDomainParameters(true)
      }
    })
  }

	onDomainParameterSelect(domainParameter: DomainParameter) {
		this.openParameterModal(domainParameter)
  }

	createDomainParameter() {
		this.openParameterModal({})
  }

	uploadTestSuite() {
    this.modalService.show(TestSuiteUploadModalComponent, {
      class: 'modal-lg',
      backdrop: 'static',
      keyboard: false,
      initialState: {
        availableSpecifications: this.specifications,
        testSuitesVisible: false,
        domainId: this.domainId
      }
    })
  }

  uploadSharedTestSuite() {
    const modal = this.modalService.show(TestSuiteUploadModalComponent, {
      class: 'modal-lg',
      backdrop: 'static',
      keyboard: false,
      initialState: {
        availableSpecifications: this.specifications,
        sharedTestSuite: true,
        domainId: this.domainId
      }
    })
    modal.content!.completed.subscribe((testSuitesUpdated: boolean) => {
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

  removeSpecificationFromGroup(specificationId: number, currentGroupId: number) {
    this.specificationService.removeSpecificationFromGroup(specificationId)
    .subscribe(() => {
      const currentGroup = find(this.domainSpecifications, (ds) => ds.id == currentGroupId)
      if (currentGroup && currentGroup.options) {
        const specifications = remove(currentGroup.options, (ds) => ds.id == specificationId)
        if (specifications && specifications.length > 0) {
          specifications[0].groupId = undefined
          specifications[0].option = false
          specifications[0].removePending = false
          this.domainSpecifications.push(specifications[0])
        }
        this.dataService.setSpecificationGroupVisibility(currentGroup)
      }
      this.dataService.sortDomainSpecifications(this.domainSpecifications)
      this.specifications = this.dataService.toSpecifications(this.domainSpecifications)
      this.popupService.success(this.dataService.labelSpecificationInGroup()+' removed.')
    })
  }

  copySpecificationToGroup(specificationId: number, currentGroupId: number|undefined, newGroupId: number) {
    this.specificationService.copySpecificationToGroup(newGroupId, specificationId)
    .subscribe((result) => {
      const newSpecificationId = result.id
      let specification: DomainSpecification|undefined
      if (currentGroupId) {
        const currentGroup = find(this.domainSpecifications, (ds) => ds.id == currentGroupId)
        if (currentGroup && currentGroup.options) {
          specification = find(currentGroup.options, (ds) => ds.id == specificationId)
        }
      } else {
        specification = find(this.domainSpecifications, (ds) => ds.id == specificationId)
      }
      const newGroup = find(this.domainSpecifications, (ds) => ds.id == newGroupId)
      if (specification && newGroup && newGroup.options) {
        const newSpecification: DomainSpecification = {
          id: newSpecificationId,
          sname: specification.sname,
          fname: specification.fname,
          description: specification.description,
          domain: specification.domain,
          hidden: specification.hidden,
          groupId: newGroupId,
          group: false,
          option: true,
          collapsed: false,
          order: specification.order
        }
        specification.copyPending = false
        newGroup.options.push(newSpecification)
        this.dataService.sortDomainSpecifications(newGroup.options)
        this.dataService.setSpecificationGroupVisibility(newGroup)
        this.specifications = this.dataService.toSpecifications(this.domainSpecifications)
        this.popupService.success(this.dataService.labelSpecificationInGroup()+' copied.')
      }
    })
  }

  moveSpecificationToGroup(specificationId: number, currentGroupId: number|undefined, newGroupId: number) {
    this.specificationService.addSpecificationToGroup(newGroupId, specificationId)
    .subscribe(() => {
      let specifications: DomainSpecification[]|undefined
      if (currentGroupId) {
        const currentGroup = find(this.domainSpecifications, (ds) => ds.id == currentGroupId)
        if (currentGroup && currentGroup.options) {
          specifications = remove(currentGroup.options, (ds) => ds.id == specificationId)
          this.dataService.setSpecificationGroupVisibility(currentGroup)
        }
      } else {
        specifications = remove(this.domainSpecifications, (ds) => ds.id == specificationId)
      }
      if (specifications && specifications.length > 0) {
        specifications[0].movePending = false
        specifications[0].groupId = newGroupId;
        const group = find(this.domainSpecifications, (ds) => ds.id == newGroupId)
        if (group && group.options) {
          specifications[0].option = true
          group.options.push(specifications[0])
          this.dataService.setSpecificationGroupVisibility(group)
          this.dataService.sortDomainSpecifications(group.options)
        }
      }
      this.specifications = this.dataService.toSpecifications(this.domainSpecifications)
      this.popupService.success(this.dataService.labelSpecificationInGroup()+' moved.')
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
      this.saveOrderPending = false
    })
  }

  resetOrdering() {
    this.saveOrderPending = true
    this.specificationService.resetSpecificationOrder(this.domainId)
    .subscribe(() => {
      this.domainSpecifications.forEach((item) => {
        item.order = 0
        if (item.options) {
          item.options.forEach((option) => option.order = 0)
        }
      })
      this.dataService.sortDomainSpecifications(this.domainSpecifications)
      this.popupService.success("Ordering reset successfully.")
    }).add(() => {
      this.saveOrderPending = false
    })    
  }

}
