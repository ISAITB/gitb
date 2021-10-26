import { Component, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AccountService } from 'src/app/services/account.service';
import { LandingPageService } from 'src/app/services/landing-page.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styles: [
  ]
})
export class HomeComponent implements OnInit {

  html = ''

  constructor(
    private accountService: AccountService,
    private landingPageService: LandingPageService,
    public sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
    this.accountService.getVendorProfile().subscribe((vendor) => {
      if (vendor.landingPages) {
        this.html = vendor.landingPages.content!
      } else {
        let communityId = vendor.community
        this.landingPageService.getCommunityDefaultLandingPage(communityId).subscribe((data) => {
          if (data.exists == true) {
            this.html = data.content!
          }
        })
      }
    })
  }

}
