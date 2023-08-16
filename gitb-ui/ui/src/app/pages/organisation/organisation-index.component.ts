import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { Organisation } from 'src/app/types/organisation.type';

@Component({
  selector: 'app-organisation-index',
  templateUrl: './organisation-index.component.html',
  styles: [
  ]
})
export class OrganisationIndexComponent implements OnInit {

  sub?: Subscription
  
  constructor(
    public dataService: DataService,
    route: ActivatedRoute
  ) {
    // this.sub = route.params.subscribe(() => {
    //   this.initialise()
    // })
  }

  // ngOnDestroy(): void {
  //   if (this.sub) {
  //     this.sub.unsubscribe()
  //   }
  // }

  initialise() {
    // this.dataService.setBanner('Tests'+((this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin)?' - '+this.organisation.sname:''))
  }

  ngOnInit(): void {
    // Initialisation takes place in the initialise method because we want to reload for parameter changes (observed via event).
    // this.initialise()
    this.dataService.setBanner('Tests')
  }

}
