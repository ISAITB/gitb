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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { CreateParameterModalComponent } from 'src/app/components/parameters/create-parameter-modal/create-parameter-modal.component';
import { CommunityService } from 'src/app/services/community.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { Parameter } from 'src/app/types/parameter';
import { ParameterPresetValue } from 'src/app/types/parameter-preset-value';
import { ParameterReference } from 'src/app/types/parameter-reference';
import { SystemParameter } from 'src/app/types/system-parameter';
import { find } from 'lodash'
import { ParameterDetailsModalComponent } from 'src/app/components/parameters/parameter-details-modal/parameter-details-modal.component';
import { ActionMethods } from './action-methods';
import { PreviewParametersModalComponent } from 'src/app/modals/preview-parameters-modal/preview-parameters-modal.component';
import { RoutingService } from 'src/app/services/routing.service';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
    selector: 'app-community-properties',
    templateUrl: './community-properties.component.html',
    standalone: false
})
export class CommunityPropertiesComponent implements OnInit {

  organisationParameterStatus = {status: Constants.STATUS.PENDING}
  systemParameterStatus = {status: Constants.STATUS.PENDING}
  communityId!:number
  orderOrganisationParametersDisabled = true
  orderSystemParametersDisabled = true
  organisationReservedKeys = ['fullName', 'shortName']
  systemReservedKeys = ['fullName', 'shortName', 'version']
  organisationParameters: OrganisationParameter[] = []
  organisationParameterValues: ParameterReference[] = []
  systemParameters: SystemParameter[] = []
  systemParameterValues: ParameterReference[] = []
  orderOrganisationParametersPending = false
  orderSystemParametersPending = false
  draggingOrganisationParameter = false
  draggingSystemParameter = false

  organisationPropertiesCollapsed = false
  organisationPropertiesCollapseFinished = false
  systemPropertiesCollapsed = false
  systemPropertiesCollapseFinished = false

  Constants = Constants

  organisationPropertyMethods: ActionMethods = {
    create: this.communityService.createOrganisationParameter.bind(this.communityService),
    update: this.communityService.updateOrganisationParameter.bind(this.communityService),
    delete: this.communityService.deleteOrganisationParameter.bind(this.communityService),
    reload: this.loadOrganisationParameters.bind(this)
  }
  systemPropertyMethods: ActionMethods = {
    create: this.communityService.createSystemParameter.bind(this.communityService),
    update: this.communityService.updateSystemParameter.bind(this.communityService),
    delete: this.communityService.deleteSystemParameter.bind(this.communityService),
    reload: this.loadSystemParameters.bind(this)
  }

  constructor(
    public readonly dataService: DataService,
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly communityService: CommunityService,
    private readonly modalService: BsModalService,
    private readonly popupService: PopupService
  ) { }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    this.loadOrganisationParameters()
    this.loadSystemParameters()
    this.routingService.communityParametersBreadcrumbs(this.communityId)
  }

  loadOrganisationParameters() {
    this.organisationParameters = []
    this.organisationParameterStatus.status = Constants.STATUS.PENDING
    this.communityService.getOrganisationParameters(this.communityId)
    .subscribe((data) => {
      this.organisationParameters = data
      this.organisationParameterValues = this.extractParameterReferences(data)
    }).add(() => {
      this.organisationParameterStatus.status = Constants.STATUS.FINISHED
    })
  }

  loadSystemParameters() {
    this.systemParameters = []
    this.systemParameterStatus.status = Constants.STATUS.PENDING
    this.communityService.getSystemParameters(this.communityId)
    .subscribe((data) => {
      this.systemParameters = data
      this.systemParameterValues = this.extractParameterReferences(data)
    }).add(() => {
      this.systemParameterStatus.status = Constants.STATUS.FINISHED
    })
  }

  kindLabel(property: CustomProperty) {
    if (property.kind == 'SIMPLE') {
      return 'Simple'
    } else if (property.kind == 'BINARY') {
      return 'Binary'
    } else {
      return 'Secret'
    }
  }

  extractParameterReferences<T extends CustomProperty>(properties: T[]): ParameterReference[] {
    const references: ParameterReference[] = []
    for (let item of properties) {
      const itemRef: Partial<ParameterReference> = {
        id: item.id, name: item.name, key: item.testKey, kind: item.kind
      }
      itemRef.hasPresetValues = false
      if (item.allowedValues != undefined) {
        itemRef.presetValues = JSON.parse(item.allowedValues)
        itemRef.hasPresetValues = itemRef.presetValues != undefined && itemRef.presetValues.length > 0
      }
      references.push(itemRef as ParameterReference)
    }
    return references
  }

  previewParameters<T extends CustomProperty>(title: string, parameters: T[], hasRegistrationCase: boolean, parameterType: 'organisation'|'system') {
    this.modalService.show(PreviewParametersModalComponent, {
      class: 'modal-lg',
      initialState: {
        modalTitle: title,
        parameters: parameters,
        hasRegistrationCase: hasRegistrationCase,
        parameterType: parameterType
      }
    })
  }

  previewOrganisationParameters() {
    this.previewParameters(this.dataService.labelOrganisation()+" property form preview", this.organisationParameters, true, 'organisation')
  }

  previewSystemParameters() {
    this.previewParameters(this.dataService.labelSystem()+" property form preview", this.systemParameters, false, 'system')
  }

  orderOrganisationParameters() {
    const ids: number[] = []
    for (let param of this.organisationParameters) {
      ids.push(param.id)
    }
    this.orderOrganisationParametersPending = true
    this.communityService.orderOrganisationParameters(this.communityId, ids)
    .subscribe(() => {
      this.popupService.success('Property ordering saved.')
    }).add(() => {
      this.orderOrganisationParametersDisabled = true
      this.orderOrganisationParametersPending = false
    })
  }

  orderSystemParameters() {
    const ids: number[] = []
    for (let param of this.systemParameters) {
      ids.push(param.id)
    }
    this.orderSystemParametersPending = true
    this.communityService.orderSystemParameters(this.communityId, ids)
    .subscribe(() => {
      this.popupService.success('Property ordering saved.')
    }).add(() => {
      this.orderSystemParametersDisabled = true
      this.orderSystemParametersPending = false
    })
  }

  addParameter(modalTitle: string, existingValues: ParameterReference[], reservedKeys: string[], methods: ActionMethods, propertyLabel: string, hideInRegistration: boolean) {
    const modalRef = this.modalService.show(CreateParameterModalComponent, {
      class: 'modal-lg',
      initialState: {
        options: {
          nameLabel: 'Label',
          notForTests: true,
          adminOnly: false,
          hasKey: true,
          hideInRegistration: hideInRegistration,
          modalTitle: modalTitle,
          confirmMessage: 'Are you sure you want to delete this property?',
          existingValues: existingValues,
          reservedKeys: reservedKeys
        }
      }
    })
    modalRef.content?.created.subscribe((parameter: Parameter) => {
      this.preparePresetValues(parameter)
      methods.create(parameter, this.communityId).subscribe(() => {
        methods.reload()
        this.popupService.success(propertyLabel + ' property created.')
      })
    })
  }

  preparePresetValues(parameter: Parameter) {
    parameter.allowedValues = undefined
    if (parameter.kind == 'SIMPLE' && parameter.hasPresetValues) {
      const checkedValues: ParameterPresetValue[] = []
      if (parameter.presetValues != undefined) {
        for (let value of parameter.presetValues) {
          const existingValue = find(checkedValues, (v) => v.value == value.value)
          if (existingValue == undefined) {
            checkedValues.push({value: value.value, label: value.label})
          }
        }
      }
      if (checkedValues.length > 0) {
        parameter.allowedValues = JSON.stringify(checkedValues)
      }
    }
  }

  addOrganisationParameter() {
    this.addParameter('Create '+this.dataService.labelOrganisationLower()+' property', this.organisationParameterValues, this.organisationReservedKeys, this.organisationPropertyMethods, this.dataService.labelOrganisation(), false)
  }

  addSystemParameter() {
    this.addParameter('Create '+this.dataService.labelSystemLower()+' property', this.systemParameterValues, this.systemReservedKeys, this.systemPropertyMethods, this.dataService.labelSystem(), true)
  }

  onParameterSelect(parameter: Parameter, existingValues: ParameterReference[], reservedKeys: string[], methods: ActionMethods, propertyLabel: string, hideInRegistration: boolean) {
    const modalRef = this.modalService.show(ParameterDetailsModalComponent, {
      class: 'modal-lg',
      initialState: {
        parameter: parameter,
        options: {
          nameLabel: 'Label',
          hasKey: true,
          hideInRegistration: hideInRegistration,
          modalTitle: propertyLabel + ' property details',
          confirmMessage: 'Are you sure you want to delete this property?',
          existingValues: existingValues,
          reservedKeys: reservedKeys
        }
      }
    })
    modalRef.content?.deleted.subscribe((parameter: Parameter) => {
      methods.delete(parameter.id).subscribe(() => {
        methods.reload()
        this.popupService.success(propertyLabel + ' property deleted.')
      })
    })
    modalRef.content?.updated.subscribe((parameter: Parameter) => {
      this.preparePresetValues(parameter)
      methods.update(parameter, this.communityId).subscribe(() => {
        methods.reload()
        this.popupService.success(propertyLabel + ' property updated.')
      })
    })
  }

  onOrganisationParameterSelect(parameter: OrganisationParameter) {
    this.onParameterSelect(parameter, this.organisationParameterValues, this.organisationReservedKeys, this.organisationPropertyMethods, this.dataService.labelOrganisation(), false)
  }

  onSystemParameterSelect(parameter: SystemParameter)  {
    this.onParameterSelect(parameter, this.systemParameterValues, this.systemReservedKeys, this.systemPropertyMethods, this.dataService.labelSystem(), true)
  }

  cancel() {
    this.routingService.toCommunity(this.communityId)
  }

  dropOrganisationParameter(event: CdkDragDrop<any>) {
    if (event.currentIndex != event.previousIndex) {
      this.organisationParameters.splice(event.currentIndex, 0, this.organisationParameters.splice(event.previousIndex, 1)[0]);
      this.orderOrganisationParametersDisabled = false
    }
  }

  dropSystemParameter(event: CdkDragDrop<any>) {
    if (event.currentIndex != event.previousIndex) {
      this.systemParameters.splice(event.currentIndex, 0, this.systemParameters.splice(event.previousIndex, 1)[0]);
      this.orderSystemParametersDisabled = false
    }
  }

  toggleOrganisationPropertiesCollapsed(value: boolean) {
    setTimeout(() => {
      this.organisationPropertiesCollapseFinished = value
    }, 1)
  }

  toggleSystemPropertiesCollapsed(value: boolean) {
    setTimeout(() => {
      this.systemPropertiesCollapseFinished = value
    }, 1)
  }

}
