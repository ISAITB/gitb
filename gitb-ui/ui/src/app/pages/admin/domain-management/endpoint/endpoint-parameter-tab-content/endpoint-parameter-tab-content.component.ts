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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ParameterData } from '../endpoint-details/parameter-data';
import { ParameterReference } from 'src/app/types/parameter-reference';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { Parameter } from 'src/app/types/parameter';
import { ParameterPresetValue } from 'src/app/types/parameter-preset-value';
import { find } from 'lodash';
import { EndpointParameter } from 'src/app/types/endpoint-parameter';
import { ParameterDetailsModalComponent } from 'src/app/components/parameters/parameter-details-modal/parameter-details-modal.component';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ParameterService } from 'src/app/services/parameter.service';
import { PopupService } from 'src/app/services/popup.service';
import { Constants } from 'src/app/common/constants';
import { CreateParameterModalComponent } from 'src/app/components/parameters/create-parameter-modal/create-parameter-modal.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';

@Component({
    selector: 'app-endpoint-parameter-tab-content',
    templateUrl: './endpoint-parameter-tab-content.component.html',
    standalone: false
})
export class EndpointParameterTabContentComponent implements OnInit {

  @Input() actorId!: number
  @Input() endpointId?: number
  @Input() parametersLoaded!: EventEmitter<EndpointParameter[]|undefined>
  @Input() manageEndpoints = false

  @Output() createEndpoint = new EventEmitter<any>()

  dataStatus = {status: Constants.STATUS.PENDING}
  orderParametersDisabled = true
  orderPending = false
  draggingParameter = false
  parameters: ParameterData[] = []
  parameterValues: ParameterReference[] = []
  Constants = Constants

  constructor(
    private readonly modalService: BsModalService,
    private readonly parameterService: ParameterService,
    private readonly popupService: PopupService,
    private readonly conformanceService: ConformanceService,
    public readonly dataService: DataService
  ) {
  }

  ngOnInit(): void {
    this.parametersLoaded.subscribe((endpointParameters) => {
      this.prepareParameters(endpointParameters)
    })
  }

  private prepareParameters(endpointParameters: EndpointParameter[]|undefined) {
    this.dataStatus.status = Constants.STATUS.PENDING
    this.parameters = []
    this.parameterValues = []
    if (endpointParameters != undefined && endpointParameters.length > 0) {
      for (let e of endpointParameters) {
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
    this.dataStatus.status = Constants.STATUS.FINISHED
  }

  private loadEndpointData() {
    if (this.endpointId) {
      this.conformanceService.getEndpoints([this.endpointId])
      .subscribe((data) => {
        if (data && data.length > 0) {
          this.prepareParameters(data[0].parameters)
        }
      })
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
      this.conformanceService.createParameter(parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue, this.endpointId, this.actorId)
      .subscribe((result) => {
        if (this.endpointId == undefined) {
          this.endpointId = result.endpoint
        }
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
    if (this.endpointId) {
      modal.content!.updated.subscribe((parameter: Parameter) => {
        this.preparePresetValues(parameter)
        this.parameterService.updateParameter(parameter.id, parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue, this.endpointId!)
        .subscribe(() => {
          this.loadEndpointData()
          this.popupService.success('Parameter updated.')
        })
      })
    }
    modal.content!.deleted.subscribe((parameter: Parameter) => {
      this.parameterService.deleteParameter(parameter.id)
      .subscribe(() => {
        this.loadEndpointData()
        this.popupService.success('Parameter deleted.')
      })
    })
  }

	orderParameters() {
    if (this.endpointId) {
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
  }

  dropParameter(event: CdkDragDrop<any>) {
    if (event.currentIndex != event.previousIndex) {
      this.parameters.splice(event.currentIndex, 0, this.parameters.splice(event.previousIndex, 1)[0]);
      this.orderParametersDisabled = false
    }
  }

	private preparePresetValues(parameter: Parameter) {
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

  private kindLabel(kind: string) {
    if (kind == 'SIMPLE') {
      return 'Simple'
    } else if (kind == 'BINARY') {
      return 'Binary'
    } else {
      return 'Secret '
    }
  }

  addEndpoint() {
    if (this.manageEndpoints) {
      this.createEndpoint.emit()
    }
  }

}
