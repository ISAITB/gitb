import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ActorService } from 'src/app/services/actor.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Actor } from 'src/app/types/actor';
import { Endpoint } from 'src/app/types/endpoint';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { EndpointRepresentation } from './endpoint-representation';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';

@Component({
  selector: 'app-actor-details',
  templateUrl: './actor-details.component.html',
  styles: [
  ]
})
export class ActorDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  actor: Partial<Actor> = {}
  endpoints: Endpoint[] = []
  endpointRepresentations: EndpointRepresentation[] = []
  dataStatus = {status: Constants.STATUS.PENDING}
  domainId!: number
  specificationId!: number
  actorId!: number
  endpointTableColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'desc', title: 'Description' }, 
    { field: 'parameters', title: 'Parameters' }
  ]
  savePending = false
  deletePending = false

  constructor(
    private conformanceService: ConformanceService,
    private actorService: ActorService,
    private confirmationDialogService: ConfirmationDialogService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private popupService: PopupService,
    public dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('id')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.specificationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID))
    this.actorId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))
    this.conformanceService.getActor(this.actorId, this.specificationId)
    .subscribe((data) => {
      this.actor = data
      if (this.actor.badges) {
        this.actor.badges.specificationId = this.specificationId
        this.actor.badges.actorId = this.actorId
        this.actor.badges.enabled = this.actor.badges.success != undefined && this.actor.badges.success.enabled!
        this.actor.badges.initiallyEnabled = this.actor.badges.enabled
        this.actor.badges.failureBadgeActive = this.actor.badges.failure != undefined && this.actor.badges.failure.enabled!
        this.actor.badges.successBadgeForReportActive = this.actor.badges.successForReport != undefined && this.actor.badges.successForReport.enabled!
        this.actor.badges.otherBadgeForReportActive = this.actor.badges.otherForReport != undefined && this.actor.badges.otherForReport.enabled!
        this.actor.badges.failureBadgeForReportActive = this.actor.badges.failureForReport != undefined && this.actor.badges.failureForReport.enabled!
      }
      this.routingService.actorBreadcrumbs(this.domainId, this.specificationId, this.actorId, this.actor.actorId)
    })
    this.conformanceService.getEndpointsForActor(this.actorId)
    .subscribe((data) => {
      this.endpoints = data
      this.endpointRepresentations = this.endpoints.map((endpoint) => {
        const parameters = endpoint.parameters.map((parameter) => {
          return parameter.name
        })
        return {
          'id': endpoint.id,
          'name': endpoint.name,
          'desc': endpoint.description,
          'parameters': parameters.join(', ')
        }
      })
    }).add(() => {
      this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

  delete() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelActorLower()+"?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.actorService.deleteActor(this.actorId)
      .subscribe(() => {
        this.back()
        this.popupService.success(this.dataService.labelActor()+' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  saveChanges() {
    this.savePending = true
    this.actorService.updateActor(this.actorId, this.actor.actorId!, this.actor.name!, this.actor.description, this.actor.default, this.actor.hidden, this.actor.displayOrder, this.domainId, this.specificationId, this.actor.badges!)
    .subscribe(() => {
      this.popupService.success(this.dataService.labelActor()+' updated.')
      this.dataService.breadcrumbUpdate({id: this.actorId, type: BreadcrumbType.actor, label: this.actor.actorId!})
    }).add(() => {
      this.savePending = false
    })
  }

  back() {
    this.routingService.toSpecification(this.domainId, this.specificationId, Constants.TAB.SPECIFICATION.ACTORS)
  }

  saveDisabled() {
    return !(
      this.textProvided(this.actor?.actorId) && 
      this.textProvided(this.actor?.name) && 
      this.numberOrEmpty(this.actor?.displayOrder) &&
      this.dataService.badgesValid(this.actor?.badges)
    )
  }

  onEndpointSelect(endpoint: EndpointRepresentation) {
    this.routingService.toEndpoint(this.domainId, this.specificationId, this.actorId, endpoint.id)
  }

  createEndpoint() {
    this.routingService.toCreateEndpoint(this.domainId, this.specificationId, this.actorId)
  }

}