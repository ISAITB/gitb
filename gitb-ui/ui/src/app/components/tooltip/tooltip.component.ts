import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-tooltip',
  templateUrl: './tooltip.component.html',
  styleUrls: [ './tooltip.component.less' ]
})
export class TooltipComponent implements OnInit {

  @Input() tbTooltip = ''
  @Input() inline = false
  @Input() inlineType = 'check'
  @Input() boundariesElement: 'viewport' | 'scrollParent' | 'window' = 'scrollParent'
  @Input() withMargin = false

  constructor() { }

  ngOnInit(): void {
  }

}
