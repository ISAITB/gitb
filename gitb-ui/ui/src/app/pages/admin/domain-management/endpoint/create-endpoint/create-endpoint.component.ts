import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Endpoint } from 'src/app/types/endpoint';

@Component({
    selector: 'app-create-endpoint',
    templateUrl: './create-endpoint.component.html',
    styles: [],
    standalone: false
})
export class CreateEndpointComponent extends BaseComponent implements OnInit, AfterViewInit {

  domainId!: number
  specificationId!: number
  actorId!: number
  endpoint: Partial<Endpoint> = {}
  pending = false

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private popupService: PopupService,
    private conformanceService: ConformanceService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.specificationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID))
    this.actorId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))
  }

	saveDisabled() {
    return !this.textProvided(this.endpoint?.name)
  }

	createEndpoint() {
		if (!this.saveDisabled()) {
      this.pending = true
			this.conformanceService.createEndpoint(this.endpoint.name!, this.endpoint.description, this.actorId)
      .subscribe(() => {
        this.cancel()
        this.popupService.success(this.dataService.labelEndpoint()+' created.')
      }).add(() => {
        this.pending = false
      })
    }
  }

	cancel() {
    this.routingService.toActor(this.domainId, this.specificationId, this.actorId)
  }

}
