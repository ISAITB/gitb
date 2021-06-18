import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Endpoint } from 'src/app/types/endpoint';

@Component({
  selector: 'app-create-endpoint',
  templateUrl: './create-endpoint.component.html',
  styles: [
  ]
})
export class CreateEndpointComponent extends BaseComponent implements OnInit, AfterViewInit {

  domainId!: number
  specificationId!: number
  actorId!: number
  endpoint: Partial<Endpoint> = {}
  pending = false

  constructor(
    public dataService: DataService,
    private router: Router,
    private route: ActivatedRoute,
    private popupService: PopupService,
    private conformanceService: ConformanceService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.specificationId = Number(this.route.snapshot.paramMap.get('spec_id'))
    this.actorId = Number(this.route.snapshot.paramMap.get('actor_id'))
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
    this.router.navigate(['admin', 'domains', this.domainId, 'specifications', this.specificationId, 'actors', this.actorId])
  }

}
