import { Component, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styles: [
  ]
})
export class AdminComponent implements OnInit {

  public communityId?: number

  constructor(
    public dataService: DataService,
    public routingService: RoutingService
  ) { }

  ngOnInit(): void {
    this.dataService.setBanner('Administration')
    this.communityId = this.dataService.community?.id
  }

}
