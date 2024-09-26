import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ImportItemState } from '../import-item-state';
import { ImportItemStateGroup } from '../import-item-state-group';

@Component({
  selector: '[app-import-item-group-preview]',
  templateUrl: './import-item-group-preview.component.html',
  styles: [
  ]
})
export class ImportItemGroupPreviewComponent implements OnInit {

  @Input() tbImportItemGroup!: ImportItemStateGroup
  @Input() importType!: string
  @Output() processStatusChange = new EventEmitter<void>()
  Constants = Constants
  group!: ImportItemStateGroup
  skipped = false
  showSkipAll = false
  showProceedAll = false
  showExpandAll = false
  collapsed = false
  showCount = true

  constructor() { }

  calculateFlags() {
    this.skipped = !this.isNotSkipped()
    this.showProceedAll = this.evaluateShowProceedAll()
    this.showSkipAll = this.evaluateShowSkipAll()
    this.showExpandAll = this.evaluateShowExpandAll()
    this.showCount = this.tbImportItemGroup.type != Constants.IMPORT_ITEM_TYPE.SYSTEM_SETTINGS
  }

  ngOnInit(): void {
    this.group = this.tbImportItemGroup
    this.calculateFlags()
  }

  toggleGroup(group: ImportItemStateGroup) {
    if (group.open) {
      this.closeGroup(group)
    } else {
      group.open = true
    }
  }

  closeGroup(group: ImportItemStateGroup) {
    group.open = false
    for (let item of group.items) {
      this.closeItem(item)
    }
  }

  childrenCollapsed() {
    setTimeout(() => {
      this.collapsed = true      
    }, 1)
  }

  childrenExpanding() {
    setTimeout(() => {
      this.collapsed = false
    }, 1)
  }

  closeItem(item: ImportItemState) {
    if (item.groups != undefined) {
      item.open = false
      for (let group of item.groups) {
        this.closeGroup(group)
      }
    }
  }

  private notifyImportItemListener(item: ImportItemState) {
    item.selectedProcessOptionUpdated?.emit()
  }

  skipAll() {
    for (let item of this.group.items) {
      item.selectedProcessOption = Constants.IMPORT_ITEM_CHOICE.SKIP
      this.notifyImportItemListener(item)
    }
  }

  proceedAll() {
    for (let item of this.group.items) {
      item.selectedProcessOption = Constants.IMPORT_ITEM_CHOICE.PROCEED
      this.notifyImportItemListener(item)
    }
  }

  isNotSkipped() {
    for (let item of this.group.items) {
      if (!this.isFullySkipped(item)) {
        return true
      }
    }
    return false
  }

  isFullySkipped(item: ImportItemState) {
    if (item.process == Constants.IMPORT_ITEM_CHOICE.SKIP || item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT) {
      return true
    } else if (item.process == Constants.IMPORT_ITEM_CHOICE.PROCEED) {
      return false
    } else { // SKIP_PROCESS_CHILDREN
      if (item.children != undefined && item.children.length > 0) {
        for (let child of item.children) {
          if (!this.isFullySkipped(child)) {
            return false
          }
        }
        return true
      } else {
        return false
      }
    }
  }

  expandAll() {
    this.group.open = true
    for (let item of this.group.items) {
      this.expandItem(item)
    }
  }

  expandItem(item: ImportItemState) {
    item.open = true
    if (item.groups != undefined) {
      for (let group of item.groups) {
        group.open = true
        for (let groupItem of group.items) {
          this.expandItem(groupItem)
        }
      }
    }
  }

  evaluateShowExpandAll() {
    if (!this.group.open) {
      return true
    } else {
      for (let groupItem of this.group.items) {
        if (this.hasClosedChild(groupItem)) {
          return true
        }
      }
    }
    return false
  }

  hasClosedChild(item: ImportItemState) {
    if (!item.open) {
      return true
    } else if (item.groups != undefined) {
      for (let group of item.groups) {
        if (!group.open) {
          return true
        } else {
          for (let groupItem of group.items) {
            if (this.hasClosedChild(groupItem)) {
              return true
            }
          }
        }
      }
    }
    return false
  }

  childProcessStatusUpdated() {
    this.calculateFlags()
    this.processStatusChange.emit()
  }


  evaluateShowSkipAll() {
    if (this.group.items.length > 0) {
      for (let item of this.group.items) {
        if (item.process == Constants.IMPORT_ITEM_CHOICE.PROCEED || item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN) {
          return true
        }
      }
    }
    return false
  }

  evaluateShowProceedAll() {
    if (this.group.items.length > 0) {
      for (let item of this.group.items) {
        if (item.process == Constants.IMPORT_ITEM_CHOICE.SKIP || item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN) {
          return true
        }
      }
    }
    return false
  }


}
