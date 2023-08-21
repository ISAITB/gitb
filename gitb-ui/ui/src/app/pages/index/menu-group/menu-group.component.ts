import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-menu-group',
  templateUrl: './menu-group.component.html',
  styleUrls: [ './menu-group.component.less' ]
})
export class MenuGroupComponent implements OnInit {

  @Input() label?: string
  @Input() expanded = false

  constructor() { }

  ngOnInit(): void {
    if (this.label) {
      this.label = this.label.toUpperCase()
    }
  }

}
