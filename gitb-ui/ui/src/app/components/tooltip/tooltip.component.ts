import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-tooltip',
  templateUrl: './tooltip.component.html',
  styles: [
  ]
})
export class TooltipComponent implements OnInit {

  @Input() tbTooltip = ''
  @Input() inline = false
  @Input() inlineType = 'check'
  @Input() boundariesElement: 'viewport' | 'scrollParent' | 'window' = 'scrollParent'

  constructor() { }

  ngOnInit(): void {
  }

}
