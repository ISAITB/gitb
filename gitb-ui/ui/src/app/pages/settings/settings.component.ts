import { Component, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styles: [
  ]
})
export class SettingsComponent implements OnInit {

  constructor(
    private routingService: RoutingService,
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

  toProfile() {
    this.routingService.toProfile()
  }

  toOrganisation() {
    this.routingService.toOwnOrganisationDetails()
  }

  toChangePassword() {
    this.routingService.toChangePassword()
  }
}
