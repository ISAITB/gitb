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
    private conformanceService: ConformanceService,
    private endpointService: EndpointService,
    private confirmationDialogService: ConfirmationDialogService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    public dataService: DataService,
    private popupService: PopupService
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
