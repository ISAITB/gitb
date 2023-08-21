import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';

@Component({
  selector: 'app-conformance-statement-items-display',
  templateUrl: './conformance-statement-items-display.component.html',
  styles: [
  ]
})
export class ConformanceStatementItemsDisplayComponent implements OnInit {

  @Input() items: ConformanceStatementItem[] = []
  @Input() shade = false
  @Input() animated = false
  @Input() withCheck = true
  @Input() withResults = false
  @Output() selectionChanged = new EventEmitter<ConformanceStatementItem>()
  hidden = false

  constructor() { }

  ngOnInit(): void {
    if (this.items.length == 1 && this.items[0].itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN) {
      // If we have only one domain then we don't show it.
      this.hidden = true
    }
  }

  childSelectionChanged(childItem: ConformanceStatementItem) {
    this.selectionChanged.emit(childItem)
  }
}
