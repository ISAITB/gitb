import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { MenuItem } from 'src/app/types/menu-item.enum';

@Component({
  selector: 'app-menu-item',
  templateUrl: './menu-item.component.html',
  styleUrls: [ './menu-item.component.less' ]
})
export class MenuItemComponent implements OnInit {

  @Input() label!: string
  @Input() icon!: string
  @Input() expanded = false
  @Input() type!: MenuItem
  active = false

  constructor(private dataService: DataService) { }

  ngOnInit(): void {
    this.dataService.onPageChange$.subscribe((event) => {
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

}
