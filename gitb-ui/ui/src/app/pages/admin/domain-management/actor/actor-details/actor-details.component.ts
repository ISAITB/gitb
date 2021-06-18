import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ActorService } from 'src/app/services/actor.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Actor } from 'src/app/types/actor';
import { Endpoint } from 'src/app/types/endpoint';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { EndpointRepresentation } from './endpoint-representation';

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
    private router: Router,
    private route: ActivatedRoute,
    private popupService: PopupService,
    public dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('id')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
    this.actorId = Number(this.route.snapshot.paramMap.get('actor_id'))
    this.conformanceService.getActorsWithIds([this.actorId])
    .subscribe((data) => {
      this.actor = data[0]
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
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this "+this.dataService.labelActorLower()+"?", "Yes", "No")
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
    this.actorService.updateActor(this.actorId, this.actor.actorId!, this.actor.name!, this.actor.description, this.actor.default, this.actor.hidden, this.actor.displayOrder, this.domainId, this.specificationId)
    .subscribe(() => {
      this.popupService.success(this.dataService.labelActor()+' updated.')
    }).add(() => {
      this.savePending = false
    })
  }

  back() {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', this.specificationId])
  }

  saveDisabled() {
    return !(this.textProvided(this.actor?.actorId) && this.textProvided(this.actor?.name) && this.numberOrEmpty(this.actor?.displayOrder))
  }

  onEndpointSelect(endpoint: EndpointRepresentation) {
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', this.specificationId, 'actors', this.actorId, 'endpoints', endpoint.id])
  }

}