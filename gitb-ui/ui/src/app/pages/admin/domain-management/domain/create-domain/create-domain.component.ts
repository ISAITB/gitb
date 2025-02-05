import { AfterViewInit, Component, OnInit } from '@angular/core';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Domain } from 'src/app/types/domain';

@Component({
    selector: 'app-create-domain',
    templateUrl: './create-domain.component.html',
    styles: [],
    standalone: false
})
export class CreateDomainComponent extends BaseComponent implements OnInit, AfterViewInit {

  domain: Partial<Domain> = {}
  pending = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('shortName')
  }

  ngOnInit(): void {
  }

	saveDisabled() {
    return !this.textProvided(this.domain.sname) || !this.textProvided(this.domain.fname)
  }

	createDomain() {
		if (!this.saveDisabled()) {
      this.pending = true
			this.conformanceService.createDomain(this.domain.sname!, this.domain.fname!, this.domain.description, this.domain.reportMetadata)
      .subscribe(() => {
        this.popupService.success(this.dataService.labelDomain()+' created.')
        this.routingService.toDomains()
      }).add(() => {
        this.pending = false
      })
    }
  }

	cancel() {
    this.routingService.toDomains()
  }

}
