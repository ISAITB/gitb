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

import {Component, EventEmitter, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Constants} from 'src/app/common/constants';
import {ActorService} from 'src/app/services/actor.service';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {Actor} from 'src/app/types/actor';
import {Endpoint} from 'src/app/types/endpoint';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {EndpointRepresentation} from './endpoint-representation';
import {BreadcrumbType} from 'src/app/types/breadcrumb-type';
import {EndpointParameter} from 'src/app/types/endpoint-parameter';
import {BaseTabbedComponent} from '../../../../base-tabbed-component';

@Component({
    selector: 'app-actor-details',
    templateUrl: './actor-details.component.html',
    styles: [],
    standalone: false
})
export class ActorDetailsComponent extends BaseTabbedComponent implements OnInit {

  actor: Partial<Actor> = {}
  endpoints: Endpoint[] = []
  endpointRepresentations: EndpointRepresentation[] = []
  dataStatus = {status: Constants.STATUS.PENDING}
  parameterStatus = {status: Constants.STATUS.NONE}
  domainId!: number
  specificationId!: number
  actorId!: number
  endpointTableColumns: TableColumnDefinition[] = [
    { field: 'name', title: 'Name' },
    { field: 'desc', title: 'Description' },
    { field: 'parameters', title: 'Parameters' }
  ]
  loaded = false
  savePending = false
  deletePending = false
  parametersLoaded = new EventEmitter<EndpointParameter[]|undefined>()

  constructor(
    private readonly conformanceService: ConformanceService,
    private readonly actorService: ActorService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly routingService: RoutingService,
    private readonly popupService: PopupService,
    public readonly dataService: DataService,
    router: Router,
    route: ActivatedRoute,
  ) { super(router, route) }

  loadTab(tabIndex: number) {
    if (tabIndex == Constants.TAB.ACTOR.PARAMETERS) {
      this.loadParameters()
    }
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.specificationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID))
    this.actorId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))
    this.conformanceService.getActor(this.actorId, this.specificationId).subscribe((data) => {
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
    }).add(() => {
      this.dataStatus.status = Constants.STATUS.FINISHED
      this.loaded = true
    })
  }

  private loadParameters() {
    if (this.parameterStatus.status == Constants.STATUS.NONE) {
      this.parameterStatus.status = Constants.STATUS.PENDING
      this.conformanceService.getEndpointsForActor(this.actorId).subscribe((data) => {
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
        if (this.endpoints.length == 0) {
          setTimeout(() => {
            this.parametersLoaded.emit([])
          }, 1)
        } else if (this.endpoints.length == 1) {
          setTimeout(() => {
            this.parametersLoaded.emit(this.endpoints[0].parameters)
          }, 1)
        }
      }).add(() => {
        this.parameterStatus.status = Constants.STATUS.FINISHED
      })
    }
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
    this.actorService.updateActor(this.actorId, this.actor.actorId!, this.actor.name!, this.actor.description, this.actor.reportMetadata, this.actor.default, this.actor.hidden, this.actor.displayOrder, this.domainId, this.specificationId, this.actor.badges!)
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
      this.loaded &&
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
