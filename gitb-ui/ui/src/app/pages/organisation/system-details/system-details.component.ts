import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemService } from 'src/app/services/system.service';

@Component({
  selector: 'app-system-details',
  templateUrl: './system-details.component.html',
  styles: [
  ]
})
export class SystemDetailsComponent implements OnInit {

  count = 0
  organisationId!: number
  systemId!: number
  private resolvedCount: number|undefined

  constructor(
    public dataService: DataService,
    private systemService: SystemService,
    public routingService: RoutingService,
    private route: ActivatedRoute,
    router: Router
  ) {
		if (this.dataService.isVendorUser) {
      this.resolvedCount = router.getCurrentNavigation()?.extras.state?.systemCount
    }
  }

  ngOnInit(): void {
    this.organisationId = Number(this.route.snapshot.paramMap.get('org_id'))
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
		if (this.dataService.isVendorUser) {
      if (this.resolvedCount == undefined) {
        this.systemService.getSystemsByOrganisation(this.dataService.vendor!.id)
        .subscribe((data) => {
          this.count = data.length
        })
      } else {
        this.count = this.resolvedCount
      }
    }
  }

}
