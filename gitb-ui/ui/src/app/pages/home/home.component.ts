import { Component, OnInit } from '@angular/core';
import { mergeMap, Observable, of } from 'rxjs';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    standalone: false
})
export class HomeComponent implements OnInit {

  pageContent?: string

  constructor(
    private accountService: AccountService,
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    let page$: Observable<string>
    if (this.dataService.currentLandingPageContent == undefined) {
      // Reload.
      page$ = this.accountService.getVendorProfile().pipe(
        mergeMap((vendor) => {
          if (vendor.landingPages) {
            this.dataService.currentLandingPageContent = this.orEmptyString(vendor.landingPages.content)
          } else {
            this.dataService.currentLandingPageContent = ''
          }
          return of(this.dataService.currentLandingPageContent)
        })
      )
    } else {
      page$ = of(this.dataService.currentLandingPageContent)
    }
    page$.subscribe((data) => {
      this.pageContent = data
    })
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
