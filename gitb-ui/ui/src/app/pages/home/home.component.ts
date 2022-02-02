import { Component, OnInit } from '@angular/core';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';
import { LandingPageService } from 'src/app/services/landing-page.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styles: [
  ]
})
export class HomeComponent implements OnInit {

  constructor(
    private accountService: AccountService,
    private landingPageService: LandingPageService,
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    if (this.dataService.currentLandingPageContent == undefined) {
      this.accountService.getVendorProfile().subscribe((vendor) => {
        if (vendor.landingPages) {
          this.dataService.currentLandingPageContent = this.orEmptyString(vendor.landingPages.content)
        } else {
          let communityId = vendor.community
          this.landingPageService.getCommunityDefaultLandingPage(communityId).subscribe((data) => {
            if (data.exists == true) {
              this.dataService.currentLandingPageContent = this.orEmptyString(data.content)
            } else {
              this.dataService.currentLandingPageContent = ''
            }
          })
        }
      })
    }
  }

  private orEmptyString(content: string|undefined) {
    if (content != undefined) {
      return content
    } else {
      return ''
    }
  }

}
