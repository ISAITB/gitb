import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { EndpointService } from 'src/app/services/endpoint.service';
import { ParameterService } from 'src/app/services/parameter.service';
import { PopupService } from 'src/app/services/popup.service';
import { EndpointData } from './endpoint-data';
import { ParameterData } from './parameter-data';
import { find } from 'lodash'
import { ParameterPresetValue } from 'src/app/types/parameter-preset-value';
import { CreateParameterModalComponent } from 'src/app/components/parameters/create-parameter-modal/create-parameter-modal.component';
import { Parameter } from 'src/app/types/parameter';
import { EndpointParameter } from 'src/app/types/endpoint-parameter';
import { ParameterDetailsModalComponent } from 'src/app/components/parameters/parameter-details-modal/parameter-details-modal.component';
import { ParameterReference } from 'src/app/types/parameter-reference';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
  selector: 'app-endpoint-details',
  templateUrl: './endpoint-details.component.html',
  styles: [
  ]
})
export class EndpointDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  endpointId!: number
  actorId!: number
  domainId!: number
  specificationId!: number
  orderParametersDisabled = true
  orderPending = false
  dataStatus = {status: Constants.STATUS.PENDING}
  endpoint: Partial<EndpointData> = {}
  parameters: ParameterData[] = []
  parameterValues: ParameterReference[] = []
  savePending = false
  deletePending = false

  constructor(
    private conformanceService: ConformanceService,
    private endpointService: EndpointService,
    private parameterService: ParameterService,
    private confirmationDialogService: ConfirmationDialogService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private modalService: BsModalService,
    public dataService: DataService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('name')
  }

  ngOnInit(): void {
		this.endpointId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ENDPOINT_ID))
		this.actorId  = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))
		this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
		this.specificationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID))
    this.loadEndpointData()
  }

  private kindLabel(kind: string) {
    if (kind == 'SIMPLE') { 
      return 'Simple' 
    } else if (kind == 'BINARY') { 
      return 'Binary' 
    } else { 
      return 'Secret '
    }
  }

  private loadEndpointData() {
    this.dataStatus.status = Constants.STATUS.PENDING
    this.parameters = []
		this.conformanceService.getEndpoints([this.endpointId])
    .subscribe((data) => {
			this.endpoint = data[0]
			this.parameterValues = []
			if (this.endpoint.parameters != undefined && this.endpoint.parameters.length > 0) {
				for (let e of this.endpoint.parameters) {
          const p: Partial<ParameterData> = e
          p.kindLabel = this.kindLabel(e.kind)
					p.useLabel = e.use == 'R'
					p.adminOnlyLabel = !e.adminOnly
					p.notForTestsLabel = !e.notForTests
          this.parameters.push(p as ParameterData)
					const itemRef: Partial<ParameterReference> = {id: e.id, name: e.name, key: e.testKey, kind: e.kind}
					itemRef.hasPresetValues = false
					if (p.allowedValues != undefined) {
						itemRef.presetValues = JSON.parse(p.allowedValues)
						itemRef.hasPresetValues = itemRef.presetValues != undefined && itemRef.presetValues.length > 0
          }
					this.parameterValues.push(itemRef as ParameterReference)
        }
      }
    }).add(() => {
			this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

	moveParameterUp(index: number) {
		const item = this.parameters.splice(index, 1)[0]
		this.orderParametersDisabled = false
		this.parameters.splice(index-1, 0, item)
  }

	moveParameterDown(index: number) {
		const item = this.parameters.splice(index, 1)[0]
		this.orderParametersDisabled = false
		this.parameters.splice(index+1, 0, item)
  }

	orderParameters() {
    this.orderPending = true
		const ids: number[] = []
		for (let param of this.parameters) {
			ids.push(param.id)
    }
		this.parameterService.orderParameters(this.endpointId, ids)
		.subscribe(() => {
			this.popupService.success('Parameter ordering saved.')
    }).add(() => {
			this.orderParametersDisabled = true
      this.orderPending = false
    })
  }

	delete() {
		this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelEndpointLower()+"?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.endpointService.deleteEndPoint(this.endpointId)
      .subscribe(() => {
        this.back()
        this.popupService.success(this.dataService.labelEndpoint()+' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

	saveChanges() {
    this.savePending = true
		this.endpointService.updateEndPoint(this.endpointId, this.endpoint.name!, this.endpoint.description, this.actorId)
    .subscribe(() => {
      this.popupService.success(this.dataService.labelEndpoint()+' updated.')
    }).add(() => {
      this.savePending = false
    })
  }

	saveDisabled() {
		return !this.textProvided(this.endpoint?.name)
  }

	back() {
    this.routingService.toActor(this.domainId, this.specificationId, this.actorId)
  }

	preparePresetValues(parameter: Parameter) {
		parameter.allowedValues = undefined
		if (parameter.kind == 'SIMPLE' && parameter.hasPresetValues) {
			const checkedValues: ParameterPresetValue[] = []
			for (let value of parameter.presetValues!) {
				const existingValue = find(checkedValues, (v) => {
          return v.value == value.value
        })
				if (existingValue == undefined) {
					checkedValues.push({value: value.value, label: value.label})
        }
      }
			parameter.allowedValues = JSON.stringify(checkedValues)
    }
  }

	addParameter() {
    const modal = this.modalService.show(CreateParameterModalComponent, {
      class: 'modal-lg',
      initialState: {
        options: {
          hideInExport: true,
          hideInRegistration: true,
          hasKey: true,
          existingValues: this.parameterValues
        }
      }
    })
    modal.content!.created.subscribe((parameter: Parameter) => {
      this.preparePresetValues(parameter)
      this.conformanceService.createParameter(parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue, this.endpointId)
      .subscribe(() => {
        this.loadEndpointData()
        this.popupService.success('Parameter created.')
      })
    })
  }

	onParameterSelect(parameter: EndpointParameter) {
    const modal = this.modalService.show(ParameterDetailsModalComponent, {
      class: 'modal-lg',
      initialState: {
        parameter: parameter,
        options: {
					hideInExport: true,
					hideInRegistration: true,
          hasKey: true,
					existingValues: this.parameterValues
        }
      }
    })
    modal.content!.updated.subscribe((parameter: Parameter) => {
      this.preparePresetValues(parameter)
      this.parameterService.updateParameter(parameter.id, parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue, this.endpointId)
      .subscribe(() => {
        this.loadEndpointData()
        this.popupService.success('Parameter updated.')
      })
    })
    modal.content!.deleted.subscribe((parameter: Parameter) => {
      this.parameterService.deleteParameter(parameter.id)
      .subscribe(() => {
        this.loadEndpointData()
        this.popupService.success('Parameter deleted.')
      })
    })
  }

}
