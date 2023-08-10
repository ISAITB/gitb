import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-menu-item',
  templateUrl: './menu-item.component.html',
  styleUrls: [ './menu-item.component.less' ]
})
export class MenuItemComponent implements OnInit {

  @Input() label!: string
  @Input() icon!: string
  @Input() expanded = false

  constructor() { }

  ngOnInit(): void {
  }

}
