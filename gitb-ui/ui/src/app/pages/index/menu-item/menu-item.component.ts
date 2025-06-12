import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { DataService } from 'src/app/services/data.service';
import { MenuItem } from 'src/app/types/menu-item.enum';

@Component({
    selector: 'app-menu-item',
    templateUrl: './menu-item.component.html',
    styleUrls: ['./menu-item.component.less'],
    standalone: false
})
export class MenuItemComponent implements OnInit, OnDestroy {

  @Input() label!: string
  @Input() icon!: string
  @Input() expanded = false
  @Input() type!: MenuItem
  active = false
  pageChangeSubscription?: Subscription

  constructor(private dataService: DataService) { }

  ngOnInit(): void {
    if (this.dataService.latestPageChange && this.dataService.latestPageChange.menuItem == this.type) {
      this.active = true
      this.dataService.changeBanner(this.label)
    }
    this.pageChangeSubscription = this.dataService.onPageChange$.subscribe((event) => {
      if (event.menuItem != undefined) {
        setTimeout(() => {
          this.active = event.menuItem  == this.type
          if (this.active) {
            this.dataService.changeBanner(this.label)
          }
        }, 1)
      }
    })
  }

  ngOnDestroy(): void {
    if (this.pageChangeSubscription) this.pageChangeSubscription.unsubscribe()
  }

}
