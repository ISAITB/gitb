import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';

@Component({
  selector: 'app-system-details',
  templateUrl: './system-details.component.html',
  styles: [
  ]
})
export class SystemDetailsComponent implements OnInit {

  count = 0
  private resolvedCount: number|undefined

  constructor(
    public dataService: DataService,
    private systemService: SystemService,
    router: Router
  ) {
		if (this.dataService.isVendorUser) {
      this.resolvedCount = router.getCurrentNavigation()?.extras.state?.systemCount
    }
  }

  ngOnInit(): void {
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
