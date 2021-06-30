import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { TestSuiteUploadModalComponent } from 'src/app/modals/test-suite-upload-modal/test-suite-upload-modal.component';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { Actor } from 'src/app/types/actor';
import { Specification } from 'src/app/types/specification';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { TestSuite } from 'src/app/types/test-suite';

@Component({
  selector: 'app-specification-details',
  templateUrl: './specification-details.component.html',
  styles: [
  ]
})
export class SpecificationDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  specification: Partial<Specification> = {}
  actors: Actor[] = []
  testSuites: TestSuite[] = []
  domainId!: number
  specificationId!: number
  actorStatus = {status: Constants.STATUS.PENDING}
  testSuiteStatus = {status: Constants.STATUS.PENDING}
  testSuiteTableColumns: TableColumnDefinition[] = [
    { field: 'identifier', title: 'ID' },
    { field: 'sname', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'version', title: 'Version' }
  ]
  actorTableColumns: TableColumnDefinition[] = [
    { field: 'actorId', title: 'ID' },
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' },
    { field: 'default', title: 'Default' },
    { field: 'hidden', title: 'Hidden' }			
  ]
  savePending = false
  deletePending = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private confirmationDialogService: ConfirmationDialogService,
    private specificationService: SpecificationService,
    private router: Router,
    private route: ActivatedRoute,
    private popupService: PopupService,
    private modalService: BsModalService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('shortName')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
		this.conformanceService.getSpecificationsWithIds([this.specificationId])
		.subscribe((data) => {
      this.specification = data[0]
    })
    this.loadActors()
    this.loadTestSuites()
  }

  private loadActors() {
    this.actorStatus.status = Constants.STATUS.PENDING
		this.conformanceService.getActorsWithSpecificationId(this.specificationId)
		.subscribe((data) => {
			this.actors = data
    }).add(() => {
			this.actorStatus.status = Constants.STATUS.FINISHED
    })
  }

  private loadTestSuites() {
    this.testSuiteStatus.status = Constants.STATUS.PENDING
		this.conformanceService.getTestSuites(this.specificationId)
		.subscribe((data) => {
			this.testSuites = data
    }).add(() => {
			this.testSuiteStatus.status = Constants.STATUS.FINISHED
    })
  }

  createActor() {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', this.specificationId, 'actors', 'create'])
  }

	uploadTestSuite() {
    const modal = this.modalService.show(TestSuiteUploadModalComponent, {
      class: 'modal-lg',
      keyboard: false,
      backdrop: 'static',
      initialState: {
				availableSpecifications: [this.specification as Specification],
				testSuitesVisible: true
      }
    })
    modal.content!.completed.subscribe((testSuitesUpdated: boolean) => {
      if (testSuitesUpdated) {
        this.loadTestSuites()
        this.loadActors()
      }
    })
  }

	onActorSelect(actor: Actor) {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', this.specificationId, 'actors', actor.id])
  }

	onTestSuiteSelect(testSuite: TestSuite) {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', this.specificationId, 'testsuites', testSuite.id])
  }

	deleteSpecification() {
		this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelSpecificationLower()+"?", "Yes", "No")
		.subscribe(() => {
      this.deletePending = true
      this.specificationService.deleteSpecification(this.specificationId)
      .subscribe(() => {
        this.router.navigate(['admin', 'domains', this.domainId])
        this.popupService.success(this.dataService.labelSpecification()+' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

	saveSpecificationChanges() {
    this.savePending = true
		this.specificationService.updateSpecification(this.specificationId, this.specification.sname!, this.specification.fname!, this.specification.description, this.specification.hidden)
		.subscribe(() => {
			this.popupService.success(this.dataService.labelSpecification()+' updated.')
    }).add(() => {
      this.savePending = false
    })
  }

	saveDisabled() {
    return !(this.textProvided(this.specification?.sname) && this.textProvided(this.specification?.fname))
  }

	back() {
    this.router.navigate(['admin', 'domains', this.domainId])
  }

}
