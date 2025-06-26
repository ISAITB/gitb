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

import { Component, EventEmitter, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { EndpointService } from 'src/app/services/endpoint.service';
import { PopupService } from 'src/app/services/popup.service';
import { EndpointData } from './endpoint-data';
import { EndpointParameter } from 'src/app/types/endpoint-parameter';
import { RoutingService } from 'src/app/services/routing.service';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';

@Component({
    selector: 'app-endpoint-details',
    templateUrl: './endpoint-details.component.html',
    styles: [],
    standalone: false
})
export class EndpointDetailsComponent extends BaseComponent implements OnInit {

  endpointId!: number
  actorId!: number
  domainId!: number
  specificationId!: number
  endpoint: Partial<EndpointData> = {}
  loaded = false
  savePending = false
  deletePending = false
  parametersLoaded = new EventEmitter<EndpointParameter[]|undefined>

  constructor(
    private readonly conformanceService: ConformanceService,
    private readonly endpointService: EndpointService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    public readonly dataService: DataService,
    private readonly popupService: PopupService
  ) { super() }

  ngOnInit(): void {
		this.endpointId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ENDPOINT_ID))
		this.actorId  = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))
		this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
		this.specificationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID))
    this.loadEndpointData()
  }

  private loadEndpointData() {
		this.conformanceService.getEndpoints([this.endpointId])
    .subscribe((data) => {
			this.endpoint = data[0]
      this.parametersLoaded.emit(this.endpoint.parameters)
      this.routingService.endpointBreadcrumbs(this.domainId, this.specificationId, this.actorId, this.endpointId, this.endpoint.name!)
    }).add(() => {
      this.loaded = true
    })
  }

	delete() {
		this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelEndpointLower()+"?", "Delete", "Cancel")
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
      this.dataService.breadcrumbUpdate({id: this.endpointId, type: BreadcrumbType.endpoint, label: this.endpoint.name!})
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

}
