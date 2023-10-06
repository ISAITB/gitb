import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-collapsing-icon',
  templateUrl: './collapsing-icon.component.html',
  styles: [
  ]
})
export class CollapsingIconComponent implements OnInit {

  @Input() isCollapsed = false
  @Input() hidden = false
  @Input() padded = true
  @Input() asDiv = false

  constructor() { }

  ngOnInit(): void {
  }

}
