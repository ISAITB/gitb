import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { filter } from 'lodash';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { ConformanceStatementResult } from 'src/app/types/conformance-statement-result';
import { Counters } from '../test-status-icons/counters';
import { DataService } from 'src/app/services/data.service';
import { Constants } from 'src/app/common/constants';

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
  @Input() expandable = true
  @Input() wrapDescriptions = false
  @Input() withCheck = true
  @Input() withExport = false
  @Input() withResults = false
  @Input() filtering = true

  @Output() selectionChanged = new EventEmitter<ConformanceStatementItem>()
  @Output() export = new EventEmitter<ConformanceStatementItem>()
  hasChildren = false
  allChildrenHidden = false
  showCheck = false
  showResults = false
  results?: ConformanceStatementResult
  counters?: Counters
  status?: string
  updateTime?: string
  Constants = Constants

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    this.hasChildren = this.item.items != undefined && this.item.items.length > 0
    if (this.hasChildren) {
      const hiddenChildren = filter(this.item.items, (item) => {
        return item.hidden == true
      })
      this.allChildrenHidden = this.item.items!.length == hiddenChildren.length
    } else {
      this.allChildrenHidden = true
    }
    this.showCheck = this.withCheck && (!this.hasChildren || this.allChildrenHidden)
    if (this.withResults && this.allChildrenHidden) {
      this.results = this.findResults(this.item)
      if (this.results) {
        this.showResults = true
        this.counters = {
          completed: this.results.completed,
          failed: this.results.failed,
          other: this.results.undefined,
          completedOptional: this.results.completedOptional,
          failedOptional: this.results.failedOptional,
          otherOptional: this.results.undefinedOptional
        }
        this.updateTime = this.results.updateTime
        this.status = this.dataService.conformanceStatusForTests(this.results.completed, this.results.failed, this.results.undefined)
      }
    }
  }

  findResults(item: ConformanceStatementItem):ConformanceStatementResult|undefined  {
    if (item.results) {
      return item.results
    } else if (item.items) {
      for (let child of item.items) {
        if (child.results) {
          return child.results
        }
      }
    }
    return undefined
  }

  clickHeader() {
    if (this.hasChildren && !this.allChildrenHidden) {
      this.item.collapsed = this.expandable && !this.item.collapsed
    } else {
      if (this.showCheck) {
        this.item.checked = !this.item.checked
      }
      if (this.hasChildren) {
        this.notifyForSelectionChange(this.item.items![0])
      } else {
        this.notifyForSelectionChange()
      }
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

  childExported(item: ConformanceStatementItem) {
    this.export.emit(item)
  }

  onExport() {
    this.export.emit(this.item)
  }
}
