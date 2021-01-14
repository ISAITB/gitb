import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { CreateEditDomainParameterModalComponent } from 'src/app/modals/create-edit-domain-parameter-modal/create-edit-domain-parameter-modal.component';
import { TestSuiteUploadModalComponent } from 'src/app/modals/test-suite-upload-modal/test-suite-upload-modal.component';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Domain } from 'src/app/types/domain';
import { DomainParameter } from 'src/app/types/domain-parameter';
import { Specification } from 'src/app/types/specification';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';

@Component({
  selector: 'app-domain-details',
  templateUrl: './domain-details.component.html',
  styles: [
  ]
})
export class DomainDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  domain: Partial<Domain> = {}
  specifications: Specification[] = []
  domainParameters: DomainParameter[] = []
  domainId!: number
  specificationStatus = {status: Constants.STATUS.PENDING}
  parameterStatus = {status: Constants.STATUS.PENDING}
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description' },
    { field: 'hidden', title: 'Hidden' }
  ]
  savePending = false
  deletePending = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private confirmationDialogService: ConfirmationDialogService,
    private modalService: BsModalService,
    private popupService: PopupService,
    private router: Router,
    private route: ActivatedRoute
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('shortName')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.conformanceService.getDomains([this.domainId])
    .subscribe((data) => {
      this.domain = data[0]
    })
    this.conformanceService.getSpecifications(this.domainId)
    .subscribe((data) => {
      this.specifications = data
    }).add(() => {
      this.specificationStatus.status = Constants.STATUS.FINISHED
    })
    this.loadDomainParameters()
  }

  private loadDomainParameters() {
    this.parameterStatus.status = Constants.STATUS.PENDING
    this.conformanceService.getDomainParameters(this.domainId)
    .subscribe((data) => {
      this.domainParameters = []
      for (let parameter of data) {
        if (parameter.kind == 'HIDDEN') {
          parameter.valueToShow = "*****"
        } else if (parameter.kind == 'BINARY') {
          const mimeType = this.dataService.mimeTypeFromDataURL(parameter.value!)
          const extension = this.dataService.extensionFromMimeType(mimeType)
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

	downloadParameter(parameter: DomainParameter) {
		const mimeType = this.dataService.mimeTypeFromDataURL(parameter.value!)
		const blob = this.dataService.b64toBlob(this.dataService.base64FromDataURL(parameter.value!), mimeType)
		const extension = this.dataService.extensionFromMimeType(mimeType)
		saveAs(blob, parameter.name+extension)
  }

	deleteDomain() {
		this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelDomainLower()+"?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.conformanceService.deleteDomain(this.domainId)
      .subscribe(() => {
        this.popupService.success(this.dataService.labelDomain()+' deleted.')
        this.router.navigate(['admin', 'domains'])
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
		this.conformanceService.updateDomain(this.domainId, this.domain.sname!, this.domain.fname!, this.domain.description)
    .subscribe(() => {
      this.popupService.success(this.dataService.labelDomain()+' updated.')
    }).add(() => {
      this.savePending = false
    })
  }

	back() {
    this.router.navigate(['admin', 'domains'])
  }

	onSpecificationSelect(specification: Specification) {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', specification.id ])
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
        this.loadDomainParameters()
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
        testSuitesVisible: false
      }
    })
  }

	createSpecification() {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', 'create'])
  }
  
}
