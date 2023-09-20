import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Specification } from 'src/app/types/specification';

@Component({
  selector: 'app-create-specification',
  templateUrl: './create-specification.component.html',
  styles: [
  ]
})
export class CreateSpecificationComponent extends BaseComponent implements OnInit, AfterViewInit {

  specification: Partial<Specification> = {}
  pending = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private route: ActivatedRoute,
    private routingService: RoutingService
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('shortName')
  }

  ngOnInit(): void {
    const groupId = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.SPECIFICATION_GROUP_ID)
    if (groupId) {
      this.specification.group = Number(groupId)
    }
    this.specification.badges = {
      enabled: false,
      initiallyEnabled: false,
      failureBadgeActive: false,
      success: { enabled: false },
      other: { enabled: false },
      failure: { enabled: false }
    }
  }

	saveDisabled() {
    return !(
      this.textProvided(this.specification?.sname) && 
      this.textProvided(this.specification?.fname) &&
      (
        !this.specification.badges!.enabled || 
        (
          this.specification.badges!.success.enabled && 
          this.specification.badges!.other.enabled && 
          (
            !this.specification.badges!.failureBadgeActive ||
            this.specification.badges!.failure.enabled
          )
        )
      )
    )
  }

	createSpecification() {
		if (!this.saveDisabled()) {
			let domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
      this.pending = true
      this.conformanceService.createSpecification(this.specification.sname!, this.specification.fname!, this.specification.description, this.specification.hidden, domainId, this.specification.group, this.specification.badges!)
      .subscribe(() => {
        this.routingService.toDomain(domainId)
        this.popupService.success(this.dataService.labelSpecification()+' created.')
      }).add(() => {
        this.pending = false
      })
    }
  }

	cancel() {
    let domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.routingService.toDomain(domainId)
  }

}
