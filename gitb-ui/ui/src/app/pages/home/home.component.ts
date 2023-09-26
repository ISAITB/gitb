import { Component, OnInit } from '@angular/core';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {

  constructor(
    private accountService: AccountService,
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    if (this.dataService.currentLandingPageContent == undefined) {
      // Reload.
      this.accountService.getVendorProfile().subscribe((vendor) => {
        if (vendor.landingPages) {
          this.dataService.currentLandingPageContent = this.orEmptyString(vendor.landingPages.content)
        } else {
          this.dataService.currentLandingPageContent = ''
        }
      })
    }
    this.dataService.breadcrumbUpdate({breadcrumbs: []})
  }

  private orEmptyString(content: string|undefined) {
    if (content != undefined) {
      return content
    } else {
      return ''
    }
  }

}
