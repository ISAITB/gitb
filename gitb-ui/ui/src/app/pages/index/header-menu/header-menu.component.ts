import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { AuthProviderService } from 'src/app/services/auth-provider.service';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
  selector: 'app-header-menu',
  templateUrl: './header-menu.component.html',
  styleUrls: [ './header-menu.component.less' ]
})
export class HeaderMenuComponent implements OnInit {

  @Input() logoutInProgress!: boolean
  expanded = false
  isOverPopup = false
  isOverHeader = false

  constructor(
    public dataService: DataService,
    private authProviderService: AuthProviderService,
    public routingService: RoutingService
  ) { }

  ngOnInit(): void {
  }

	userLoaded(): boolean {
		return this.dataService.user != undefined && this.dataService.user.name != undefined
  }

	userFullyLoaded(): boolean {
    return this.userLoaded() && this.dataService.vendor != undefined
  }  

  isAuthenticated(): boolean {
    return this.authProviderService.isAuthenticated()
  }

	logout() {
    this.authProviderService.signalLogout({full: true})
  }

	switchAccount() {
    this.dataService.recordLoginOption(Constants.LOGIN_OPTION.FORCE_CHOICE)
    this.authProviderService.signalLogout({full: false, keepLoginOption: true})
  }

  overHeader() {
    this.isOverHeader = true
    this.expanded = true
  }

  leftHeader() {
    this.isOverHeader = false
    setTimeout(() => {
      if (!this.isOverPopup) {
        this.expanded = false
      }
    }, 5)
  }

  overPopup() {
    this.isOverPopup = true
    this.expanded = true
  }

  leftPopup() {
    this.isOverPopup = false
    setTimeout(() => {
      if (!this.isOverHeader) {
        this.expanded = false
      }
    }, 5)
  }

}
