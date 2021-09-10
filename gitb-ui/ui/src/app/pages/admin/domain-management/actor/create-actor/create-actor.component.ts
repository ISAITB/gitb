import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Actor } from 'src/app/types/actor';

@Component({
  selector: 'app-create-actor',
  templateUrl: './create-actor.component.html',
  styles: [
  ]
})
export class CreateActorComponent extends BaseComponent implements OnInit, AfterViewInit {

  domainId!: number
  specificationId!:number
  actor: Partial<Actor> = {}
  savePending = false

  constructor(
    public dataService: DataService,
    private popupService: PopupService,
    private conformanceService: ConformanceService,
    private routingService: RoutingService,
    private route: ActivatedRoute
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('id')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
  }

	saveDisabled() {
    return !(this.textProvided(this.actor?.actorId) && this.textProvided(this.actor?.name) && this.numberOrEmpty(this.actor?.displayOrder))
  }

	createActor() {
		if (!this.saveDisabled()) {
      this.savePending = true
      this.conformanceService.createActor(this.actor.actorId!, this.actor.name!, this.actor.description, this.actor.default, this.actor.hidden, this.actor.displayOrder, this.domainId, this.specificationId)
      .subscribe(() => {
        this.routingService.toSpecification(this.domainId, this.specificationId)
        this.popupService.success(this.dataService.labelActor()+' created.')
      }).add(() => {
        this.savePending = false
      })
    }
  }

	cancel() {
    this.routingService.toSpecification(this.domainId, this.specificationId)
  }

}
