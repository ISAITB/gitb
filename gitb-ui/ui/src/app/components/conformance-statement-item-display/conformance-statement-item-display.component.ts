import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';

@Component({
  selector: 'app-conformance-statement-item-display',
  templateUrl: './conformance-statement-item-display.component.html',
  styleUrls: [ './conformance-statement-item-display.component.less' ]
})
export class ConformanceStatementItemDisplayComponent implements OnInit {

  @Input() item!: ConformanceStatementItem
  @Input() shade = false
  @Input() hideSelf = false
  @Input() animated = true
  @Output() selectionChanged = new EventEmitter<ConformanceStatementItem>()
  hasChildren = false
  showCheck = false

  constructor() { }

  ngOnInit(): void {
    this.hasChildren = this.item.items != undefined && this.item.items.length > 0
    this.showCheck = !this.hasChildren
  }

  clickHeader() {
    if (this.hasChildren) {
      this.item.collapsed = !this.item.collapsed
    } else {
      this.item.checked = !this.item.checked
      this.notifyForSelectionChange()
    }
  }

  notifyForSelectionChange(otherItem?: ConformanceStatementItem) {
    if (otherItem) {
      this.selectionChanged.emit(otherItem)
    } else {
      this.selectionChanged.emit(this.item)
    }
  }

  updateChecked() {
    this.notifyForSelectionChange()
  }

  expanded() {
    if (this.item.items && this.item.items.length == 1) {
      this.item.items[0].collapsed = false
    }
  }

  collapsed() {
    if (this.item.items) {
      for (let child of this.item.items) {
        child.collapsed = true
      }
    }
  }

  childSelectionChanged(item: ConformanceStatementItem) {
    this.notifyForSelectionChange(item)
  }
}
